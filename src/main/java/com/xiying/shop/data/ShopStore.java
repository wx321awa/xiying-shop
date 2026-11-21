package com.xiying.shop.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.xiying.shop.XiYingShopPlugin;
import com.xiying.shop.model.Listing;
import com.xiying.shop.model.Shop;
import com.xiying.shop.model.Tx;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ShopStore {
    public static class Data {
        public List<Shop> shops = new ArrayList<>();
        public List<Listing> listings = new ArrayList<>();
        public List<Tx> txs = new ArrayList<>();
    }

    private final XiYingShopPlugin plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final File file;
    private final Map<UUID, Shop> shops = new ConcurrentHashMap<>();
    private final Map<String, Listing> listings = new LinkedHashMap<>();
    private final List<Tx> txs = new ArrayList<>();
    private final Object writeLock = new Object();
    private boolean saveScheduled = false;

    public ShopStore(XiYingShopPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.json");
    }

    public synchronized void load() {
        shops.clear();
        listings.clear();
        txs.clear();
        if (!file.exists()) return;
        try {
            String json = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            Data data = gson.fromJson(json, Data.class);
            if (data == null) return;
            if (data.shops != null) {
                for (Shop s : data.shops) {
                    if (s != null && s.owner != null) shops.put(s.owner, s);
                }
            }
            if (data.listings != null) {
                for (Listing l : data.listings) {
                    if (l != null && l.id != null && l.owner != null) {
                        if (l.currency == null) l.currency = com.xiying.shop.model.Currency.MONEY;
                        listings.put(l.id, l);
                    }
                }
            }
            if (data.txs != null) txs.addAll(data.txs);
            plugin.getLogger().info("已载入 " + shops.size() + " 家店铺、" + listings.size() + " 条商品");
        } catch (Exception e) {
            plugin.getLogger().warning("读取 data.json 失败：" + e.getMessage());
            File broken = new File(plugin.getDataFolder(), "data.json.broken-" + System.currentTimeMillis());
            try {
                Files.copy(file.toPath(), broken.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception ignored) {
            }
        }
    }

    private Data snapshot() {
        Data data = new Data();
        data.shops = new ArrayList<>(shops.values());
        data.listings = new ArrayList<>(listings.values());
        data.txs = new ArrayList<>(txs);
        return data;
    }

    public void saveAsync() {
        if (saveScheduled) return;
        saveScheduled = true;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            saveScheduled = false;
            String json = gson.toJson(snapshot());
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> write(json));
        }, 20L);
    }

    public void saveNow() {
        String json = gson.toJson(snapshot());
        write(json);
    }

    private void write(String json) {
        synchronized (writeLock) {
            try {
                if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
                    plugin.getLogger().warning("无法创建数据目录");
                    return;
                }
                File tmp = new File(file.getParentFile(), "data.json.tmp");
                Files.writeString(tmp.toPath(), json, StandardCharsets.UTF_8);
                try {
                    Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE);
                } catch (Exception atomicFailed) {
                    Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("保存 data.json 失败：" + e.getMessage());
            }
        }
    }

    public Shop shopOf(UUID owner) {
        return owner == null ? null : shops.get(owner);
    }

    public Shop shopByName(String name) {
        if (name == null) return null;
        for (Shop s : shops.values()) {
            if (s.name != null && s.name.equalsIgnoreCase(name)) return s;
        }
        return null;
    }

    public List<Shop> shops() {
        return new ArrayList<>(shops.values());
    }

    public void putShop(Shop shop) {
        shops.put(shop.owner, shop);
    }

    public void removeShop(UUID owner) {
        shops.remove(owner);
    }

    public Listing listing(String id) {
        return id == null ? null : listings.get(id);
    }

    public List<Listing> listings() {
        return new ArrayList<>(listings.values());
    }

    public List<Listing> listingsOf(UUID owner) {
        List<Listing> list = new ArrayList<>();
        for (Listing l : listings.values()) {
            if (l.owner != null && l.owner.equals(owner)) list.add(l);
        }
        list.sort(Comparator.comparingLong((Listing l) -> l.createdAt).reversed());
        return list;
    }

    public void putListing(Listing listing) {
        listings.put(listing.id, listing);
    }

    public void removeListing(String id) {
        listings.remove(id);
    }

    public void addTx(Tx tx) {
        txs.add(0, tx);
        while (txs.size() > 300) txs.remove(txs.size() - 1);
    }

    public List<Tx> txs() {
        return new ArrayList<>(txs);
    }
}
