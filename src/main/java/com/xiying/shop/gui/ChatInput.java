package com.xiying.shop.gui;

import com.xiying.shop.XiYingShopPlugin;
import com.xiying.shop.model.Currency;
import com.xiying.shop.model.Listing;
import com.xiying.shop.service.ShopService;
import com.xiying.shop.util.Msg;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatInput {
    private enum Kind {CREATE_SHOP, SEARCH, LIST_ITEM, EDIT_PRICE}

    private static class Pending {
        Kind kind;
        ItemStack item;
        Currency currency = Currency.MONEY;
        String listingId;
    }

    private final XiYingShopPlugin plugin;
    private final ShopService service;
    private final GuiManager gui;
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

    public ChatInput(XiYingShopPlugin plugin, ShopService service, GuiManager gui) {
        this.plugin = plugin;
        this.service = service;
        this.gui = gui;
    }

    public void startCreateShop(Player player) {
        player.closeInventory();
        Pending p = new Pending();
        p.kind = Kind.CREATE_SHOP;
        pending.put(player.getUniqueId(), p);
        Msg.send(player, "请在聊天栏输入店名（" + plugin.getPluginConfig().shopNameMin + "~"
                + plugin.getPluginConfig().shopNameMax + " 个字），输入 §eq §f取消。创建费用 §6"
                + Msg.money(plugin.getPluginConfig().createCost) + " 金币");
    }

    public void startSearch(Player player) {
        player.closeInventory();
        Pending p = new Pending();
        p.kind = Kind.SEARCH;
        pending.put(player.getUniqueId(), p);
        Msg.send(player, "请在聊天栏输入要搜索的物品名 / 店名 / 卖家名，输入 §eq §f取消");
    }

    public void startListItem(Player player, ItemStack item, Currency currency) {
        player.closeInventory();
        Pending p = new Pending();
        p.kind = Kind.LIST_ITEM;
        p.item = item.clone();
        p.currency = currency;
        pending.put(player.getUniqueId(), p);
        Msg.send(player, "上架 §e" + com.xiying.shop.util.ItemUtil.describe(item) + "§f（"
                + currency.label() + "结算），请在聊天栏输入：§e单价 数量§f，例如 §e100 16§f，输入 §eq §f取消");
    }

    public void startEditPrice(Player player, String listingId) {
        player.closeInventory();
        Pending p = new Pending();
        p.kind = Kind.EDIT_PRICE;
        p.listingId = listingId;
        pending.put(player.getUniqueId(), p);
        Msg.send(player, "请在聊天栏输入新的单价，输入 §eq §f取消");
    }

    public boolean isWaiting(Player player) {
        return pending.containsKey(player.getUniqueId());
    }

    public void cancel(Player player) {
        pending.remove(player.getUniqueId());
    }

    public void handle(Player player, String raw) {
        Pending p = pending.remove(player.getUniqueId());
        if (p == null) return;
        String text = raw == null ? "" : raw.trim();
        if (text.equalsIgnoreCase("q") || text.equalsIgnoreCase("cancel")) {
            Msg.send(player, "已取消");
            return;
        }
        switch (p.kind) {
            case CREATE_SHOP -> {
                ShopService.Result r = service.createShop(player, text);
                Msg.send(player, r.message());
                if (r.ok()) gui.openMyShop(player);
            }
            case SEARCH -> {
                List<String> ids = new ArrayList<>();
                for (Listing l : service.search(text)) ids.add(l.id);
                gui.openByIds(player, ids, 0, "§8搜索：" + text);
            }
            case LIST_ITEM -> {
                String[] parts = text.split("\\s+");
                if (parts.length < 2) {
                    Msg.send(player, "格式不对，应该是：§e单价 数量§f，例如 §e100 16");
                    return;
                }
                double price;
                int amount;
                try {
                    price = Double.parseDouble(parts[0]);
                    amount = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    Msg.send(player, "单价或数量不是有效数字");
                    return;
                }
                ShopService.Result r = service.listItem(player, p.item, amount, price, p.currency);
                Msg.send(player, r.message());
                if (r.ok()) gui.openMyShop(player);
            }
            case EDIT_PRICE -> {
                double price;
                try {
                    price = Double.parseDouble(text);
                } catch (NumberFormatException e) {
                    Msg.send(player, "单价不是有效数字");
                    return;
                }
                Listing l = plugin.getStore().listing(p.listingId);
                ShopService.Result r = service.changePrice(player, l, price);
                Msg.send(player, r.message());
                if (r.ok()) gui.openManage(player, 0);
            }
        }
    }
}
