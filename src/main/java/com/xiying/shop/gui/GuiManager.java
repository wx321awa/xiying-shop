package com.xiying.shop.gui;

import com.xiying.shop.XiYingShopPlugin;
import com.xiying.shop.config.PluginConfig;
import com.xiying.shop.data.ShopStore;
import com.xiying.shop.model.Currency;
import com.xiying.shop.model.Listing;
import com.xiying.shop.model.Shop;
import com.xiying.shop.model.Tx;
import com.xiying.shop.service.ShopService;
import com.xiying.shop.util.EconomyHook;
import com.xiying.shop.util.ItemUtil;
import com.xiying.shop.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class GuiManager {
    private final XiYingShopPlugin plugin;
    private final ShopService service;
    private final ShopStore store;
    private final EconomyHook econ;
    private final PluginConfig cfg;

    public GuiManager(XiYingShopPlugin plugin, ShopService service, ShopStore store, EconomyHook econ, PluginConfig cfg) {
        this.plugin = plugin;
        this.service = service;
        this.store = store;
        this.econ = econ;
        this.cfg = cfg;
    }

    private static final SimpleDateFormat TIME = new SimpleDateFormat("MM-dd HH:mm");

    public void openMain(Player player) {
        GuiHolder holder = new GuiHolder(GuiType.MAIN);
        Inventory inv = Bukkit.createInventory(holder, 54, Msg.c("§8曦荧玩家商店"));
        holder.setInventory(inv);
        fill(inv);
        Shop shop = service.shopOf(player);
        inv.setItem(4, icon(shop == null ? Material.CHEST : material(shop.icon), "§b我的店铺",
                shop == null ? List.of("§7还没有店铺", "§7点击创建（" + Msg.money(cfg.createCost) + " 金币）")
                        : List.of("§7店名：§f" + shop.name, "§7状态：" + (shop.open ? "§a营业中" : "§c已打烊"),
                        "§7商品：" + store.listingsOf(player.getUniqueId()).size() + " 条",
                        "", "§e点击管理")));
        inv.setItem(19, icon(Material.EMERALD, "§a全站商品", List.of("§7浏览所有玩家上架的商品")));
        inv.setItem(21, icon(Material.COMPASS, "§e搜索商品", List.of("§7按物品名 / 店名 / 卖家搜索")));
        inv.setItem(23, icon(Material.GOLD_INGOT, "§6热门店铺", List.of("§7按销量排行")));
        inv.setItem(25, icon(Material.PAPER, "§d交易记录", List.of("§7查看你最近的成交记录")));
        inv.setItem(40, icon(Material.BOOK, "§f帮助", List.of("§7怎么开店、怎么上架、怎么买东西")));
        inv.setItem(49, icon(Material.BARRIER, "§c关闭", List.of()));
        player.openInventory(inv);
    }

    public void openBrowseAll(Player player) {
        List<String> ids = new ArrayList<>();
        for (Listing l : service.browse()) ids.add(l.id);
        openByIds(player, ids, 0, "§8全站商品");
    }

    public void openByIds(Player player, List<String> ids, int page, String title) {
        GuiHolder holder = new GuiHolder(GuiType.BROWSE);
        holder.ids = ids;
        holder.title = title;
        int size = 54;
        Inventory inv = Bukkit.createInventory(holder, size, Msg.c(title));
        holder.setInventory(inv);
        fill(inv);
        int perPage = 45;
        int pages = Math.max(1, (ids.size() + perPage - 1) / perPage);
        int p = Math.max(0, Math.min(page, pages - 1));
        holder.page = p;
        int start = p * perPage;
        for (int i = 0; i < perPage && start + i < ids.size(); i++) {
            Listing l = store.listing(ids.get(start + i));
            if (l == null) continue;
            inv.setItem(i, listingIcon(l));
        }
        if (ids.isEmpty()) {
            inv.setItem(22, icon(Material.BARRIER, "§7暂时没有商品", List.of("§7换个关键词或者晚点再来")));
        }
        inv.setItem(45, p > 0 ? icon(Material.ARROW, "§e上一页", List.of("§7第 " + p + " / " + pages + " 页")) : null);
        inv.setItem(49, icon(Material.CHEST, "§b返回主菜单", List.of()));
        inv.setItem(53, p + 1 < pages ? icon(Material.ARROW, "§e下一页", List.of("§7第 " + (p + 2) + " / " + pages + " 页")) : null);
        inv.setItem(48, icon(Material.PAPER, "§7第 " + (p + 1) + " / " + pages + " 页", List.of("§7共 " + ids.size() + " 条商品")));
        player.openInventory(inv);
    }

    private ItemStack listingIcon(Listing l) {
        ItemStack item = ItemUtil.decode(l.item);
        if (item == null) return icon(Material.BARRIER, "§c商品数据异常", List.of("§7联系管理员处理"));
        item = item.clone();
        item.setAmount(Math.min(Math.max(1, l.amount), ItemUtil.maxStack(item)));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            if (meta.lore() != null) {
                for (var line : meta.lore()) lore.add(Msg.plain(line));
            }
            lore.add("§8────────────");
            lore.add("§7单价：§6" + econ.format(l.currency, l.unitPrice));
            lore.add("§7库存：§f" + l.amount);
            lore.add("§7店铺：§f" + l.shopName);
            lore.add("§7卖家：§f" + l.ownerName);
            lore.add("");
            lore.add("§e点击查看 / 购买");
            meta.lore(lore.stream().map(Msg::c).toList());
            item.setItemMeta(meta);
        }
        return item;
    }

    public void openDetail(Player player, String listingId, String backTitle, List<String> backIds, int backPage) {
        Listing l = store.listing(listingId);
        if (l == null) {
            Msg.send(player, "这件商品已经不存在了");
            openMain(player);
            return;
        }
        GuiHolder holder = new GuiHolder(GuiType.DETAIL);
        holder.listingId = listingId;
        holder.ids = backIds;
        holder.page = backPage;
        Inventory inv = Bukkit.createInventory(holder, 27, Msg.c("§8商品详情"));
        holder.setInventory(inv);
        fill(inv);
        ItemStack preview = ItemUtil.decode(l.item);
        if (preview != null) {
            preview = preview.clone();
            preview.setAmount(Math.min(Math.max(1, l.amount), ItemUtil.maxStack(preview)));
            ItemMeta meta = preview.getItemMeta();
            if (meta != null) {
                List<String> lore = new ArrayList<>();
                lore.add("§8────────────");
                lore.add("§7单价：§6" + econ.format(l.currency, l.unitPrice));
                lore.add("§7库存：§f" + l.amount);
                lore.add("§7店铺：§f" + l.shopName + " §7(§f" + l.ownerName + "§7)");
                meta.lore(lore.stream().map(Msg::c).toList());
                preview.setItemMeta(meta);
            }
            inv.setItem(13, preview);
        }
        inv.setItem(10, icon(Material.LIME_DYE, "§a购买 1 个", List.of("§7花费 " + econ.format(l.currency, l.unitPrice))));
        inv.setItem(12, icon(Material.LIME_DYE, "§a购买 8 个", List.of("§7花费 " + econ.format(l.currency, l.unitPrice * 8))));
        inv.setItem(14, icon(Material.LIME_DYE, "§a购买 64 个", List.of("§7花费 " + econ.format(l.currency, l.unitPrice * 64))));
        inv.setItem(16, icon(Material.EMERALD, "§a全部买下", List.of("§7花费 " + econ.format(l.currency, l.unitPrice * l.amount))));
        inv.setItem(22, icon(Material.ARROW, "§e返回", List.of()));
        player.openInventory(inv);
    }

    public void openMyShop(Player player) {
        Shop shop = service.shopOf(player);
        if (shop == null) {
            openMain(player);
            return;
        }
        GuiHolder holder = new GuiHolder(GuiType.MY_SHOP);
        Inventory inv = Bukkit.createInventory(holder, 27, Msg.c("§8我的店铺 · " + shop.name));
        holder.setInventory(inv);
        fill(inv);
        List<Listing> mine = store.listingsOf(player.getUniqueId());
        int total = 0;
        for (Listing l : mine) total += l.amount;
        inv.setItem(4, icon(material(shop.icon), "§b" + shop.name, List.of(
                "§7状态：" + (shop.open ? "§a营业中" : "§c已打烊"),
                "§7商品：§f" + mine.size() + " §7/ " + cfg.maxListings + " 条",
                "§7总数量：§f" + total,
                "§7累计售出：§f" + shop.sales + " 个",
                "§7待领取：§6" + Msg.money(shop.pendingMoney) + " 金币 §7/ §d" + shop.pendingPoints + " 曦荧币")));
        inv.setItem(19, icon(Material.HOPPER, "§a上架物品", List.of("§7点自己背包里的物品", "§7再输入单价和数量")));
        inv.setItem(20, icon(Material.CHEST, "§b管理商品", List.of("§7改价 / 下架")));
        inv.setItem(21, icon(Material.GOLD_INGOT, "§6领取金币收益", List.of("§7待领取：§f" + Msg.money(shop.pendingMoney))));
        inv.setItem(22, icon(Material.AMETHYST_SHARD, "§d领取曦荧币收益", List.of("§7待领取：§f" + shop.pendingPoints)));
        inv.setItem(23, icon(Material.LEVER, shop.open ? "§a打烊" : "§a开张", List.of("§7当前：" + (shop.open ? "营业中" : "已打烊"))));
        inv.setItem(26, icon(Material.ARROW, "§e返回", List.of()));
        player.openInventory(inv);
    }

    public void openManage(Player player, int page) {
        List<Listing> mine = store.listingsOf(player.getUniqueId());
        GuiHolder holder = new GuiHolder(GuiType.MANAGE);
        Inventory inv = Bukkit.createInventory(holder, 54, Msg.c("§8管理商品"));
        holder.setInventory(inv);
        fill(inv);
        int perPage = 45;
        int pages = Math.max(1, (mine.size() + perPage - 1) / perPage);
        int p = Math.max(0, Math.min(page, pages - 1));
        holder.page = p;
        for (int i = 0; i < perPage && p * perPage + i < mine.size(); i++) {
            Listing l = mine.get(p * perPage + i);
            ItemStack icon = listingIcon(l);
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.lore() == null ? new ArrayList<>()
                        : new ArrayList<>(meta.lore().stream().map(Msg::plain).toList());
                lore.add("§e点击改价 / 下架");
                meta.lore(lore.stream().map(Msg::c).toList());
                icon.setItemMeta(meta);
            }
            inv.setItem(i, icon);
        }
        inv.setItem(45, p > 0 ? icon(Material.ARROW, "§e上一页", List.of()) : null);
        inv.setItem(49, icon(Material.CHEST, "§b返回店铺", List.of()));
        inv.setItem(53, p + 1 < pages ? icon(Material.ARROW, "§e下一页", List.of()) : null);
        player.openInventory(inv);
    }

    public void openListItem(Player player) {
        GuiHolder holder = new GuiHolder(GuiType.LIST_ITEM);
        Inventory inv = Bukkit.createInventory(holder, 27, Msg.c("§8选择要上架的物品"));
        holder.setInventory(inv);
        fill(inv);
        inv.setItem(4, icon(Material.BOOK, "§e怎么上架？", List.of(
                "§7直接点你背包里的物品",
                "§7然后在聊天栏输入：§f单价 数量",
                "§7例如：§f100 16 §7= 单价 100，上架 16 个")));
        inv.setItem(22, icon(Material.ARROW, "§e返回店铺", List.of()));
        player.openInventory(inv);
    }

    public void openCurrency(Player player, ItemStack item) {
        GuiHolder holder = new GuiHolder(GuiType.CURRENCY);
        holder.pendingItem = item == null ? null : item.clone();
        Inventory inv = Bukkit.createInventory(holder, 27, Msg.c("§8选择结算货币"));
        holder.setInventory(inv);
        fill(inv);
        ItemStack preview = item == null ? icon(Material.BARRIER, "§c未选择物品", List.of()) : item.clone();
        if (preview != null && preview.getAmount() > 1) preview.setAmount(Math.min(preview.getAmount(), 64));
        inv.setItem(13, preview);
        inv.setItem(11, icon(Material.GOLD_INGOT, "§6金币", List.of("§7按 Vault / CMI 金币结算")));
        inv.setItem(15, icon(Material.AMETHYST_SHARD, "§d曦荧币", List.of("§7按 PlayerPoints 结算")));
        inv.setItem(22, icon(Material.ARROW, "§e返回", List.of()));
        player.openInventory(inv);
    }

    public void openEdit(Player player, String listingId) {
        Listing l = store.listing(listingId);
        if (l == null) {
            Msg.send(player, "这件商品已经不存在了");
            openManage(player, 0);
            return;
        }
        GuiHolder holder = new GuiHolder(GuiType.EDIT);
        holder.listingId = listingId;
        Inventory inv = Bukkit.createInventory(holder, 27, Msg.c("§8编辑商品"));
        holder.setInventory(inv);
        fill(inv);
        inv.setItem(13, listingIcon(l));
        inv.setItem(11, icon(Material.NAME_TAG, "§e修改单价", List.of("§7当前：§f" + econ.format(l.currency, l.unitPrice), "§7点击后聊天栏输入新单价")));
        inv.setItem(15, icon(Material.BARRIER, "§c下架商品", List.of("§7物品会放回你的背包")));
        inv.setItem(22, icon(Material.ARROW, "§e返回", List.of()));
        player.openInventory(inv);
    }

    public void openRanking(Player player) {
        GuiHolder holder = new GuiHolder(GuiType.RANKING);
        Inventory inv = Bukkit.createInventory(holder, 27, Msg.c("§8热门店铺"));
        holder.setInventory(inv);
        fill(inv);
        List<Shop> top = service.topShops(10);
        int slot = 0;
        for (Shop shop : top) {
            List<Listing> listings = service.browseShop(shop.owner);
            inv.setItem(slot++, icon(material(shop.icon), "§6#" + (slot) + " §b" + shop.name, List.of(
                    "§7店主：§f" + shop.ownerName,
                    "§7销量：§e" + shop.sales,
                    "§7在售：§f" + listings.size() + " 种",
                    "", "§e点击查看这家店")));
        }
        if (top.isEmpty()) inv.setItem(13, icon(Material.BARRIER, "§7还没有店铺", List.of()));
        inv.setItem(26, icon(Material.ARROW, "§e返回", List.of()));
        player.openInventory(inv);
    }

    public void openTx(Player player) {
        GuiHolder holder = new GuiHolder(GuiType.TX);
        Inventory inv = Bukkit.createInventory(holder, 54, Msg.c("§8我的交易记录"));
        holder.setInventory(inv);
        fill(inv);
        List<Tx> list = service.recentTx(player, 45);
        for (int i = 0; i < list.size(); i++) {
            Tx tx = list.get(i);
            boolean sold = tx.seller.equalsIgnoreCase(player.getName());
            inv.setItem(i, icon(Material.PAPER,
                    (sold ? "§a售出 §f" : "§b买入 §f") + tx.item + " §7× " + tx.amount,
                    List.of("§7时间：§f" + TIME.format(new Date(tx.time)),
                            "§7对方：§f" + (sold ? tx.buyer : tx.seller),
                            "§7金额：§6" + econ.format(tx.currency, tx.total))));
        }
        if (list.isEmpty()) inv.setItem(22, icon(Material.BARRIER, "§7还没有交易记录", List.of()));
        inv.setItem(49, icon(Material.ARROW, "§e返回", List.of()));
        player.openInventory(inv);
    }

    public void openHelp(Player player) {
        GuiHolder holder = new GuiHolder(GuiType.HELP);
        Inventory inv = Bukkit.createInventory(holder, 27, Msg.c("§8玩法帮助"));
        holder.setInventory(inv);
        fill(inv);
        inv.setItem(11, icon(Material.CHEST, "§b开店", List.of("§7主菜单点「我的店铺」", "§7创建费用：" + Msg.money(cfg.createCost) + " 金币", "§7店名 2~12 个字")));
        inv.setItem(13, icon(Material.HOPPER, "§a上架", List.of("§7店铺 › 上架物品", "§7点背包物品后输入「单价 数量」", "§7可选择金币或曦荧币结算", "§7税费：" + taxText())));
        inv.setItem(15, icon(Material.EMERALD, "§e购买", List.of("§7全站商品或搜索", "§7点开商品选择购买数量", "§7物品直接进背包")));
        inv.setItem(26, icon(Material.ARROW, "§e返回", List.of()));
        player.openInventory(inv);
    }

    private String taxText() {
        if (cfg.taxMoney <= 0 && cfg.taxPoints <= 0) return "§a无（成交全额到账）";
        return "§7金币 " + (int) (cfg.taxMoney * 100) + "% / 曦荧币 " + (int) (cfg.taxPoints * 100) + "%";
    }

    public void handleClick(Player player, GuiHolder holder, int rawSlot) {
        switch (holder.type) {
            case MAIN -> {
                switch (rawSlot) {
                    case 4 -> {
                        if (service.shopOf(player) == null) {
                            plugin.getChatInput().startCreateShop(player);
                        } else {
                            openMyShop(player);
                        }
                    }
                    case 19 -> openBrowseAll(player);
                    case 21 -> plugin.getChatInput().startSearch(player);
                    case 23 -> openRanking(player);
                    case 25 -> openTx(player);
                    case 40 -> openHelp(player);
                    case 49 -> player.closeInventory();
                    default -> {
                    }
                }
            }
            case BROWSE -> {
                if (rawSlot == 49) {
                    openMain(player);
                } else if (rawSlot == 45 && holder.page > 0) {
                    openByIds(player, holder.ids, holder.page - 1, holder.title);
                } else if (rawSlot == 53) {
                    openByIds(player, holder.ids, holder.page + 1, holder.title);
                } else if (rawSlot >= 0 && rawSlot < 45) {
                    Listing l = null;
                    if (rawSlot < holder.ids.size()) l = store.listing(holder.ids.get(rawSlot));
                    if (l != null) openDetail(player, l.id, holder.title, holder.ids, holder.page);
                }
            }
            case DETAIL -> {
                Listing l = store.listing(holder.listingId);
                if (l == null) {
                    openMain(player);
                    return;
                }
                int qty = switch (rawSlot) {
                    case 10 -> 1;
                    case 12 -> 8;
                    case 14 -> 64;
                    case 16 -> l.amount;
                    default -> 0;
                };
                if (rawSlot == 22) {
                    openByIds(player, holder.ids, holder.page, holder.title);
                } else if (qty > 0) {
                    ShopService.Result r = service.purchase(player, l, qty);
                    Msg.send(player, r.message());
                    if (r.ok() && store.listing(l.id) != null) {
                        openDetail(player, l.id, holder.title, holder.ids, holder.page);
                    } else if (r.ok()) {
                        openByIds(player, holder.ids, holder.page, holder.title);
                    }
                }
            }
            case MY_SHOP -> {
                switch (rawSlot) {
                    case 19 -> openListItem(player);
                    case 20 -> openManage(player, 0);
                    case 21 -> service.claim(player, Currency.MONEY);
                    case 22 -> service.claim(player, Currency.POINTS);
                    case 23 -> {
                        Msg.send(player, service.toggleOpen(player).message());
                        openMyShop(player);
                    }
                    case 26 -> openMain(player);
                    default -> {
                    }
                }
            }
            case MANAGE -> {
                if (rawSlot == 49) {
                    openMyShop(player);
                } else if (rawSlot == 45 && holder.page > 0) {
                    openManage(player, holder.page - 1);
                } else if (rawSlot == 53) {
                    openManage(player, holder.page + 1);
                } else if (rawSlot >= 0 && rawSlot < 45) {
                    List<Listing> mine = store.listingsOf(player.getUniqueId());
                    int index = holder.page * 45 + rawSlot;
                    if (index < mine.size()) openEdit(player, mine.get(index).id);
                }
            }
            case EDIT -> {
                if (rawSlot == 11) {
                    plugin.getChatInput().startEditPrice(player, holder.listingId);
                } else if (rawSlot == 15) {
                    Listing l = store.listing(holder.listingId);
                    Msg.send(player, service.cancelListing(player, l).message());
                    openManage(player, 0);
                } else if (rawSlot == 22) {
                    openManage(player, 0);
                }
            }
            case LIST_ITEM -> {
                if (rawSlot == 22) {
                    openMyShop(player);
                }
            }
            case CURRENCY -> {
                if (rawSlot == 11 || rawSlot == 15) {
                    ItemStack item = holder.pendingItem;
                    if (item == null) {
                        openListItem(player);
                        return;
                    }
                    Currency currency = rawSlot == 11 ? Currency.MONEY : Currency.POINTS;
                    plugin.getChatInput().startListItem(player, item, currency);
                } else if (rawSlot == 22) {
                    openListItem(player);
                }
            }
            case RANKING -> {
                if (rawSlot == 26) {
                    openMain(player);
                } else if (rawSlot >= 0 && rawSlot < 10) {
                    List<Shop> top = service.topShops(10);
                    if (rawSlot < top.size()) {
                        Shop shop = top.get(rawSlot);
                        List<String> ids = new ArrayList<>();
                        for (Listing l : service.browseShop(shop.owner)) ids.add(l.id);
                        openByIds(player, ids, 0, "§8" + shop.name);
                    }
                }
            }
            case TX -> {
                if (rawSlot == 49) openMain(player);
            }
            case HELP -> {
                if (rawSlot == 26) openMain(player);
            }
        }
    }

    public void handlePlayerInvClick(Player player, GuiHolder holder, int slot) {
        if (holder.type != GuiType.LIST_ITEM) return;
        ItemStack item = player.getInventory().getItem(slot);
        if (item == null || item.getType().isAir()) return;
        openCurrency(player, item);
    }

    private void fill(Inventory inv) {
        ItemStack filler = icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, filler);
        }
    }

    public static ItemStack icon(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Msg.c(name));
            if (!lore.isEmpty()) meta.lore(lore.stream().map(Msg::c).toList());
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static Material material(String raw) {
        Material m = Material.matchMaterial(raw == null ? "" : raw);
        return m == null ? Material.CHEST : m;
    }
}
