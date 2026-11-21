package com.xiying.shop.service;

import com.xiying.shop.XiYingShopPlugin;
import com.xiying.shop.config.PluginConfig;
import com.xiying.shop.data.ShopStore;
import com.xiying.shop.model.Currency;
import com.xiying.shop.model.Listing;
import com.xiying.shop.model.Shop;
import com.xiying.shop.model.Tx;
import com.xiying.shop.util.EconomyHook;
import com.xiying.shop.util.ItemUtil;
import com.xiying.shop.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ShopService {
    public record Result(boolean ok, String message) {
        public static Result ok(String msg) {
            return new Result(true, msg);
        }

        public static Result fail(String msg) {
            return new Result(false, msg);
        }
    }

    private final XiYingShopPlugin plugin;
    private final ShopStore store;
    private final EconomyHook econ;
    private final PluginConfig cfg;

    public ShopService(XiYingShopPlugin plugin, ShopStore store, EconomyHook econ, PluginConfig cfg) {
        this.plugin = plugin;
        this.store = store;
        this.econ = econ;
        this.cfg = cfg;
    }

    public Shop shopOf(Player player) {
        return store.shopOf(player.getUniqueId());
    }

    public Result createShop(Player player, String rawName) {
        if (shopOf(player) != null) return Result.fail("你已经有一家店铺了");
        String name = rawName == null ? "" : rawName.trim();
        if (name.length() < cfg.shopNameMin || name.length() > cfg.shopNameMax) {
            return Result.fail("店名长度需要在 " + cfg.shopNameMin + "~" + cfg.shopNameMax + " 个字之间");
        }
        if (name.contains("§") || name.contains(" ")) return Result.fail("店名不能包含空格或颜色符号");
        if (store.shopByName(name) != null) return Result.fail("这个店名已经被占用了");
        if (cfg.createCost > 0) {
            if (!econ.moneyReady()) return Result.fail("经济系统还没加载好，请稍后再试");
            if (!econ.has(player, Currency.MONEY, cfg.createCost)) {
                return Result.fail("金币不足，创建店铺需要 " + Msg.money(cfg.createCost) + " 金币");
            }
            if (!econ.take(player, Currency.MONEY, cfg.createCost)) return Result.fail("扣款失败，请稍后再试");
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        String icon = hand != null && !hand.getType().isAir() ? hand.getType().name() : "CHEST";
        Shop shop = new Shop(player.getUniqueId(), player.getName(), name, icon);
        store.putShop(shop);
        store.saveAsync();
        return Result.ok("店铺 §e" + name + " §f创建成功，输入 §e/pshop §f开始上架商品");
    }

    public Result toggleOpen(Player player) {
        Shop shop = shopOf(player);
        if (shop == null) return Result.fail("你还没有店铺");
        shop.open = !shop.open;
        store.saveAsync();
        return Result.ok(shop.open ? "店铺已开张营业" : "店铺已打烊，商品暂不展示");
    }

    public void claim(Player player, Currency currency) {
        Shop shop = shopOf(player);
        if (shop == null) {
            Msg.send(player, "你还没有店铺");
            return;
        }
        if (currency == Currency.MONEY) {
            if (shop.pendingMoney <= 0.0001) {
                Msg.send(player, "暂时没有待领取的金币收益");
                return;
            }
            double amount = shop.pendingMoney;
            if (!econ.give(player.getUniqueId(), player.getName(), Currency.MONEY, amount)) {
                Msg.send(player, "领取失败：经济系统暂时不可用");
                return;
            }
            shop.pendingMoney = 0;
            store.saveNow();
            Msg.send(player, "已领取 §6" + Msg.money(amount) + " 金币 §f收益");
        } else {
            if (shop.pendingPoints <= 0) {
                Msg.send(player, "暂时没有待领取的曦荧币收益");
                return;
            }
            int amount = shop.pendingPoints;
            if (!econ.give(player.getUniqueId(), player.getName(), Currency.POINTS, amount)) {
                Msg.send(player, "领取失败：曦荧币系统暂时不可用");
                return;
            }
            shop.pendingPoints = 0;
            store.saveNow();
            Msg.send(player, "已领取 §d" + amount + " 曦荧币 §f收益");
        }
    }

    public Result listItem(Player player, ItemStack template, int amount, double unitPrice, Currency currency) {
        Shop shop = shopOf(player);
        if (shop == null) return Result.fail("你还没有店铺，先创建一家吧");
        if (!shop.open) return Result.fail("店铺正在打烊，先开张再上架");
        if (template == null || template.getType().isAir()) return Result.fail("请选择要上架的物品");
        if (cfg.blacklisted(template.getType())) return Result.fail("这个物品不允许上架");
        if (amount < 1) return Result.fail("数量至少为 1");
        if (amount > cfg.maxAmountPerListing) return Result.fail("单条最多上架 " + cfg.maxAmountPerListing + " 个");
        if (currency == Currency.POINTS && !cfg.pointsEnabled) return Result.fail("曦荧币结算暂未开启");
        if (currency == Currency.MONEY && !cfg.moneyEnabled) return Result.fail("金币结算暂未开启");
        String priceError = priceError(unitPrice, currency);
        if (priceError != null) return Result.fail(priceError);
        int current = store.listingsOf(player.getUniqueId()).size();
        if (current >= cfg.maxListings) return Result.fail("商品位已满（" + cfg.maxListings + " 条），先下架一些吧");
        int available = countSimilar(player, template);
        if (available < amount) return Result.fail("背包里可用的该物品不够（当前 " + available + " 个）");
        if (!removeSimilar(player, template, amount)) return Result.fail("扣除物品失败，请重试");

        String encoded = ItemUtil.encode(single(template));
        Listing listing = new Listing(UUID.randomUUID().toString(), player.getUniqueId(), player.getName(),
                shop.name, encoded, amount, unitPrice, currency);
        store.putListing(listing);
        store.saveNow();
        return Result.ok("已上架 §e" + ItemUtil.describe(template) + " §f× " + amount
                + "，单价 " + econ.format(currency, unitPrice));
    }

    public Result purchase(Player buyer, Listing listing, int requested) {
        if (listing == null) return Result.fail("这件商品已经不存在了");
        Listing live = store.listing(listing.id);
        if (live == null) return Result.fail("这件商品已经不存在了");
        if (live.owner.equals(buyer.getUniqueId())) return Result.fail("不能购买自己店铺的商品");
        if (!Double.isFinite(live.unitPrice) || live.unitPrice <= 0) return Result.fail("商品价格异常，请联系管理员");
        Shop shop = store.shopOf(live.owner);
        if (shop == null || !shop.open) return Result.fail("这家店铺已经打烊了");
        int qty = Math.max(1, Math.min(requested, live.amount));
        if (qty > live.amount) return Result.fail("库存不足，只剩 " + live.amount + " 个");
        double unit = live.unitPrice;
        double total = unit * qty;
        if (live.currency == Currency.POINTS) total = Math.ceil(total);
        if (!Double.isFinite(total) || total <= 0) return Result.fail("商品价格异常，请联系管理员");
        if (!econ.has(buyer, live.currency, total)) {
            return Result.fail("余额不足，需要 " + econ.format(live.currency, total));
        }
        ItemStack item = ItemUtil.decode(live.item);
        if (item == null) return Result.fail("商品数据异常，请联系管理员");
        if (!econ.take(buyer, live.currency, total)) return Result.fail("扣款失败，请稍后再试");

        item.setAmount(Math.min(qty, ItemUtil.maxStack(item)));
        int left = qty;
        List<ItemStack> stacks = new ArrayList<>();
        while (left > 0) {
            ItemStack part = item.clone();
            int take = Math.min(left, ItemUtil.maxStack(item));
            part.setAmount(take);
            stacks.add(part);
            left -= take;
        }
        PlayerInventory inv = buyer.getInventory();
        for (ItemStack part : stacks) {
            var leftover = inv.addItem(part);
            for (ItemStack drop : leftover.values()) {
                buyer.getWorld().dropItemNaturally(buyer.getLocation(), drop);
            }
        }

        live.amount -= qty;
        if (live.amount <= 0) store.removeListing(live.id);
        double tax = live.currency == Currency.POINTS ? cfg.taxPoints : cfg.taxMoney;
        double sellerGain = total * (1.0 - tax);
        if (live.currency == Currency.POINTS) {
            shop.pendingPoints += (int) Math.floor(sellerGain);
        } else {
            shop.pendingMoney += Math.round(sellerGain * 100.0) / 100.0;
        }
        shop.sales += qty;
        store.addTx(new Tx(buyer.getName(), live.ownerName, ItemUtil.describe(item), qty, total, live.currency));
        store.saveNow();

        Player seller = Bukkit.getPlayer(live.owner);
        if (seller != null && seller.isOnline()) {
            Msg.send(seller, "§a你卖出了 §f" + ItemUtil.describe(item) + " §a× " + qty
                    + "，收益 " + econ.format(live.currency, sellerGain) + " 已存入店铺待领取");
        }
        return Result.ok("购买成功：§e" + ItemUtil.describe(item) + " §f× " + qty
                + "，花费 " + econ.format(live.currency, total));
    }

    public Result cancelListing(Player owner, Listing listing) {
        if (listing == null) return Result.fail("商品已经不存在了");
        Listing live = store.listing(listing.id);
        if (live == null) return Result.fail("这件商品已经不存在了（可能已售完或已下架）");
        if (!live.owner.equals(owner.getUniqueId())) return Result.fail("这不是你的商品");
        ItemStack template = ItemUtil.decode(live.item);
        if (template != null) {
            int left = live.amount;
            while (left > 0) {
                ItemStack part = template.clone();
                int take = Math.min(left, ItemUtil.maxStack(template));
                part.setAmount(take);
                var leftover = owner.getInventory().addItem(part);
                for (ItemStack drop : leftover.values()) {
                    owner.getWorld().dropItemNaturally(owner.getLocation(), drop);
                }
                left -= take;
            }
        }
        store.removeListing(live.id);
        store.saveNow();
        return Result.ok("已下架 §e" + (template != null ? ItemUtil.describe(template) : "商品")
                + " §f，物品已放回背包");
    }

    public Result changePrice(Player owner, Listing listing, double price) {
        if (listing == null) return Result.fail("商品已经不存在了");
        Listing live = store.listing(listing.id);
        if (live == null) return Result.fail("这件商品已经不存在了");
        if (!live.owner.equals(owner.getUniqueId())) return Result.fail("这不是你的商品");
        String error = priceError(price, live.currency);
        if (error != null) return Result.fail(error);
        live.unitPrice = price;
        store.saveNow();
        return Result.ok("单价已改为 " + econ.format(live.currency, price));
    }

    /**
     * 校验单价：必须是有限数字，且在允许区间内。NaN / Infinity 会被 Double.parseDouble 正常接受，
     * 若不拦住可以 0 元买走商品。
     */
    private String priceError(double price, Currency currency) {
        if (!Double.isFinite(price)) return "单价必须是有效数字";
        if (price <= 0) return "单价必须大于 0";
        if (price < cfg.minUnitPrice) return "单价不能低于 " + Msg.money(cfg.minUnitPrice);
        if (price > 100_000_000D) return "单价上限为 100000000";
        if (currency == Currency.POINTS && price != Math.rint(price)) return "曦荧币单价必须是整数";
        return null;
    }

    public List<Listing> browse() {
        List<Listing> list = new ArrayList<>();
        for (Listing l : store.listings()) {
            if (l.amount <= 0) continue;
            Shop shop = store.shopOf(l.owner);
            if (shop == null || !shop.open) continue;
            list.add(l);
        }
        list.sort((a, b) -> Long.compare(b.createdAt, a.createdAt));
        return list;
    }

    public List<Listing> browseShop(UUID owner) {
        List<Listing> list = new ArrayList<>();
        Shop shop = store.shopOf(owner);
        if (shop == null || !shop.open) return list;
        for (Listing l : store.listingsOf(owner)) {
            if (l.amount > 0) list.add(l);
        }
        return list;
    }

    public List<Listing> search(String keyword) {
        String key = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        if (key.isEmpty()) return browse();
        List<Listing> out = new ArrayList<>();
        for (Listing l : browse()) {
            ItemStack item = ItemUtil.decode(l.item);
            String name = item == null ? "" : ItemUtil.describe(item).toLowerCase(Locale.ROOT);
            if (name.contains(key) || l.shopName.toLowerCase(Locale.ROOT).contains(key)
                    || l.ownerName.toLowerCase(Locale.ROOT).contains(key)) {
                out.add(l);
            }
        }
        return out;
    }

    public List<Shop> topShops(int limit) {
        List<Shop> list = store.shops();
        list.sort((a, b) -> Integer.compare(b.sales, a.sales));
        return list.subList(0, Math.min(limit, list.size()));
    }

    public List<Tx> recentTx(Player player, int limit) {
        List<Tx> out = new ArrayList<>();
        for (Tx tx : store.txs()) {
            if (tx.buyer.equalsIgnoreCase(player.getName()) || tx.seller.equalsIgnoreCase(player.getName())) {
                out.add(tx);
            }
            if (out.size() >= limit) break;
        }
        return out;
    }

    public int countSimilar(Player player, ItemStack template) {
        int total = 0;
        for (ItemStack s : player.getInventory().getContents()) {
            if (s != null && !s.getType().isAir() && s.isSimilar(template)) total += s.getAmount();
        }
        return total;
    }

    private boolean removeSimilar(Player player, ItemStack template, int amount) {
        PlayerInventory inv = player.getInventory();
        int remaining = amount;
        for (int i = 0; i < inv.getSize() && remaining > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s == null || s.getType().isAir() || !s.isSimilar(template)) continue;
            int take = Math.min(remaining, s.getAmount());
            if (take >= s.getAmount()) inv.setItem(i, null);
            else s.setAmount(s.getAmount() - take);
            remaining -= take;
        }
        return remaining == 0;
    }

    private ItemStack single(ItemStack template) {
        ItemStack copy = template.clone();
        copy.setAmount(1);
        return copy;
    }

    public boolean isBlacklisted(Material material) {
        return cfg.blacklisted(material);
    }
}
