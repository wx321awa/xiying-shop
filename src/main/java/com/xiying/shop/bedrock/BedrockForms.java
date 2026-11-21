package com.xiying.shop.bedrock;

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
import org.bukkit.inventory.ItemStack;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.geysermc.cumulus.component.util.ComponentType;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.response.CustomFormResponse;
import org.geysermc.floodgate.api.FloodgateApi;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class BedrockForms {
    private final XiYingShopPlugin plugin;
    private final ShopService service;
    private final ShopStore store;
    private final EconomyHook econ;
    private final PluginConfig cfg;
    private static final SimpleDateFormat TIME = new SimpleDateFormat("MM-dd HH:mm");

    public BedrockForms(XiYingShopPlugin plugin, ShopService service, ShopStore store, EconomyHook econ, PluginConfig cfg) {
        this.plugin = plugin;
        this.service = service;
        this.store = store;
        this.econ = econ;
        this.cfg = cfg;
    }

    public boolean isBedrock(Player player) {
        try {
            return Bukkit.getPluginManager().isPluginEnabled("floodgate")
                    && FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
        } catch (Throwable t) {
            return false;
        }
    }

    private void send(Player player, Object builder) {
        try {
            boolean ok = FloodgateApi.getInstance().sendForm(player.getUniqueId(),
                    (org.geysermc.cumulus.form.util.FormBuilder<?, ?, ?>) builder);
            if (!ok) Msg.send(player, "表单发送失败，请重试");
        } catch (Throwable t) {
            plugin.getLogger().warning("基岩表单发送失败：" + t.getMessage());
            Msg.send(player, "表单发送失败：" + t.getMessage());
        }
    }

    private void sync(Runnable runnable) {
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    /**
     * 按组件类型顺序读取表单结果。
     * 不同 Cumulus 版本对 asInput(i)/asDropdown(i) 的索引口径不一致（有的含 label 占位、有的只算同类组件），
     * 直接取原始响应数组更稳妥；响应数组与组件一一对应时（label 为 null）按位置对齐，否则跳过 label 顺序消费。
     */
    private static final class FormValues {
        private final List<String> inputs = new ArrayList<>();
        private final List<Integer> dropdowns = new ArrayList<>();

        static FormValues of(CustomFormResponse response) {
            FormValues values = new FormValues();
            List<ComponentType> types = response.getComponentTypes();
            JsonArray raw = response.getResponses();
            boolean aligned = raw != null && raw.size() == types.size();
            int ri = 0;
            for (ComponentType type : types) {
                if (!aligned && type == ComponentType.LABEL) continue;
                JsonElement element = raw != null && ri < raw.size() ? raw.get(ri++) : null;
                switch (type) {
                    case INPUT -> values.inputs.add(element == null || element.isJsonNull() ? "" : element.getAsString());
                    case DROPDOWN -> values.dropdowns.add(element == null || element.isJsonNull() ? 0 : element.getAsInt());
                    default -> {
                    }
                }
            }
            return values;
        }

        String input(int index) {
            return index >= 0 && index < inputs.size() ? inputs.get(index) : "";
        }

        int dropdown(int index) {
            return index >= 0 && index < dropdowns.size() ? dropdowns.get(index) : 0;
        }
    }

    public void openMain(Player player) {
        Shop shop = service.shopOf(player);
        int listings = service.browse().size();
        int shops = store.shops().size();
        String content = "§7玩家开设的小商铺\n\n"
                + "§f在售商品：§e" + listings + " §f件\n"
                + "§f店铺数量：§e" + shops + " §f家\n"
                + "§f我的金币：§6" + Msg.money(econ.money(player)) + "\n"
                + "§f我的曦荧币：§d" + econ.points(player.getUniqueId()) + "\n\n"
                + (shop == null ? "§7你还没有店铺，点「我的店铺」创建" : "§7我的店铺：§b" + shop.name + (shop.open ? " §a营业中" : " §c已打烊"));
        SimpleForm.Builder form = SimpleForm.builder()
                .title("§b曦荧玩家商店")
                .content(content)
                .button("§a全站商品\n§7浏览所有玩家上架的商品")
                .button("§e搜索商品\n§7按物品 / 店名 / 卖家搜索")
                .button("§b我的店铺\n§7开店、上架、改价、领收益")
                .button("§6热门店铺\n§7按销量排行")
                .button("§d交易记录\n§7最近 20 条成交")
                .button("§7玩法帮助")
                .button("§c关闭")
                .validResultHandler(r -> sync(() -> {
                    switch (r.clickedButtonId()) {
                        case 0 -> openBrowse(player, allIds(), 0, "§8全站商品");
                        case 1 -> openSearch(player);
                        case 2 -> openMyShop(player);
                        case 3 -> openRanking(player);
                        case 4 -> openTx(player);
                        case 5 -> openHelp(player);
                        default -> {
                        }
                    }
                }))
                .closedOrInvalidResultHandler(() -> {
                });
        send(player, form);
    }

    private List<String> allIds() {
        List<String> ids = new ArrayList<>();
        for (Listing l : service.browse()) ids.add(l.id);
        return ids;
    }

    public void openBrowse(Player player, List<String> ids, int page, String title) {
        int perPage = cfg.bedrockPageSize;
        int pages = Math.max(1, (ids.size() + perPage - 1) / perPage);
        int p = Math.max(0, Math.min(page, pages - 1));
        int start = p * perPage;
        int count = Math.max(0, Math.min(perPage, ids.size() - start));
        SimpleForm.Builder form = SimpleForm.builder()
                .title(title)
                .content("§7第 §e" + (p + 1) + "§7 / §e" + pages + " §7页 · 共 §e" + ids.size() + " §7件");
        for (int i = 0; i < count; i++) {
            Listing l = store.listing(ids.get(start + i));
            if (l == null) continue;
            ItemStack item = ItemUtil.decode(l.item);
            String name = item == null ? "数据异常" : ItemUtil.describe(item);
            form.button("§f" + name + " §7× " + l.amount + "\n§6" + econ.format(l.currency, l.unitPrice)
                    + " §7· " + l.shopName);
        }
        boolean prev = p > 0;
        boolean next = p + 1 < pages;
        int idx = count;
        int prevIdx = -1;
        int nextIdx = -1;
        if (prev) {
            prevIdx = idx++;
            form.button("§e上一页");
        }
        if (next) {
            nextIdx = idx++;
            form.button("§e下一页");
        }
        int backIdx = idx;
        form.button("§c返回主菜单");
        int finalP = p;
        int finalPrev = prevIdx;
        int finalNext = nextIdx;
        int finalBack = backIdx;
        form.validResultHandler(r -> sync(() -> {
            int id = r.clickedButtonId();
            if (id >= 0 && id < count) {
                openDetail(player, ids.get(start + id), ids, finalP, title);
            } else if (id == finalPrev) {
                openBrowse(player, ids, finalP - 1, title);
            } else if (id == finalNext) {
                openBrowse(player, ids, finalP + 1, title);
            } else if (id == finalBack) {
                openMain(player);
            }
        }));
        send(player, form);
    }

    public void openDetail(Player player, String listingId, List<String> backIds, int backPage, String backTitle) {
        Listing l = store.listing(listingId);
        if (l == null) {
            Msg.send(player, "这件商品已经不存在了");
            openMain(player);
            return;
        }
        ItemStack item = ItemUtil.decode(l.item);
        String name = item == null ? "数据异常" : ItemUtil.describe(item);
        String content = "§f物品：§e" + name + "\n"
                + "§f单价：§6" + econ.format(l.currency, l.unitPrice) + "\n"
                + "§f库存：§e" + l.amount + "\n"
                + "§f店铺：§b" + l.shopName + " §7(" + l.ownerName + ")\n\n"
                + "§7购买后物品会直接放进背包";
        SimpleForm.Builder form = SimpleForm.builder().title("§8" + name).content(content);
        List<Integer> quantities = new ArrayList<>();
        for (int q : new int[]{1, 8, 64}) {
            if (l.amount >= q) {
                quantities.add(q);
                form.button("§a购买 " + q + " 个\n§7" + econ.format(l.currency, l.unitPrice * q));
            }
        }
        if (l.amount > 64) {
            quantities.add(l.amount);
            form.button("§a全部买下\n§7" + econ.format(l.currency, l.unitPrice * l.amount));
        }
        form.button("§e返回");
        form.validResultHandler(r -> sync(() -> {
            int id = r.clickedButtonId();
            if (id >= 0 && id < quantities.size()) {
                ShopService.Result result = service.purchase(player, l, quantities.get(id));
                Msg.send(player, result.message());
                if (store.listing(listingId) != null) {
                    openDetail(player, listingId, backIds, backPage, backTitle);
                } else {
                    openBrowse(player, backIds, backPage, backTitle);
                }
            } else {
                openBrowse(player, backIds, backPage, backTitle);
            }
        }));
        send(player, form);
    }

    public void openMyShop(Player player) {
        Shop shop = service.shopOf(player);
        if (shop == null) {
            ModalForm.Builder modal = ModalForm.builder()
                    .title("§b创建店铺")
                    .content("§f创建费用：§6" + Msg.money(cfg.createCost) + " 金币\n\n"
                            + "§7店铺可以上架商品、设置价格，\n§7买家购买后收益会存进店铺待领取。\n\n§7确定要开店吗？")
                    .button1("§a创建店铺")
                    .button2("§c再想想")
                    .validResultHandler(r -> sync(() -> {
                        if (r.clickedFirst()) openCreateName(player);
                        else openMain(player);
                    }));
            send(player, modal);
            return;
        }
        List<Listing> mine = store.listingsOf(player.getUniqueId());
        int total = 0;
        for (Listing l : mine) total += l.amount;
        String content = "§f店名：§b" + shop.name + "\n"
                + "§f状态：" + (shop.open ? "§a营业中" : "§c已打烊") + "\n"
                + "§f商品：§e" + mine.size() + " §7/ " + cfg.maxListings + " 条，共 §e" + total + " §7个\n"
                + "§f累计售出：§e" + shop.sales + " §7个\n"
                + "§f待领取：§6" + Msg.money(shop.pendingMoney) + " 金币 §7/ §d" + shop.pendingPoints + " 曦荧币";
        SimpleForm.Builder form = SimpleForm.builder()
                .title("§8我的店铺")
                .content(content)
                .button("§a上架物品\n§7从背包选择物品，填数量与单价")
                .button("§b管理商品\n§7改价 / 下架")
                .button("§6领取金币收益\n§7" + Msg.money(shop.pendingMoney) + " 金币")
                .button("§d领取曦荧币收益\n§7" + shop.pendingPoints + " 曦荧币")
                .button(shop.open ? "§c打烊\n§7商品暂时不展示" : "§a开张\n§7让商品重新上架")
                .button("§e返回主菜单")
                .validResultHandler(r -> sync(() -> {
                    switch (r.clickedButtonId()) {
                        case 0 -> openListPick(player);
                        case 1 -> openManage(player);
                        case 2 -> {
                            service.claim(player, Currency.MONEY);
                            openMyShop(player);
                        }
                        case 3 -> {
                            service.claim(player, Currency.POINTS);
                            openMyShop(player);
                        }
                        case 4 -> {
                            Msg.send(player, service.toggleOpen(player).message());
                            openMyShop(player);
                        }
                        default -> openMain(player);
                    }
                }));
        send(player, form);
    }

    private void openCreateName(Player player) {
        CustomForm.Builder form = CustomForm.builder()
                .title("§b给店铺起个名字")
                .label("§7店名 " + cfg.shopNameMin + "~" + cfg.shopNameMax + " 个字，不能带空格")
                .input("店名", "例如：曦荧杂货铺")
                .validResultHandler(r -> sync(() -> {
                    String name = FormValues.of(r).input(0);
                    ShopService.Result result = service.createShop(player, name);
                    Msg.send(player, result.message());
                    if (result.ok()) openMyShop(player);
                    else openMain(player);
                }));
        send(player, form);
    }

    private record InvOption(ItemStack template, int count) {
    }

    private List<InvOption> inventoryOptions(Player player) {
        List<InvOption> options = new ArrayList<>();
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack == null || stack.getType().isAir()) continue;
            if (cfg.blacklisted(stack.getType())) continue;
            boolean merged = false;
            for (int i = 0; i < options.size(); i++) {
                InvOption option = options.get(i);
                if (option.template().isSimilar(stack)) {
                    options.set(i, new InvOption(option.template(), option.count() + stack.getAmount()));
                    merged = true;
                    break;
                }
            }
            if (!merged) {
                ItemStack template = stack.clone();
                template.setAmount(1);
                options.add(new InvOption(template, stack.getAmount()));
            }
        }
        return options;
    }

    private static final int PICK_PAGE_SIZE = 18;

    public void openListPick(Player player) {
        openListPick(player, 0);
    }

    public void openListPick(Player player, int page) {
        List<InvOption> options = inventoryOptions(player);
        if (options.isEmpty()) {
            Msg.send(player, "背包里没有可以上架的物品");
            openMyShop(player);
            return;
        }
        int pages = Math.max(1, (options.size() + PICK_PAGE_SIZE - 1) / PICK_PAGE_SIZE);
        int p = Math.max(0, Math.min(page, pages - 1));
        int start = p * PICK_PAGE_SIZE;
        int count = Math.max(0, Math.min(PICK_PAGE_SIZE, options.size() - start));
        SimpleForm.Builder form = SimpleForm.builder()
                .title("§b选择上架物品")
                .content("§7共 §e" + options.size() + " §7种物品 · 第 §e" + (p + 1) + "§7 / §e" + pages
                        + " §7页\n§7选一件，下一步填数量和单价");
        for (int i = 0; i < count; i++) {
            InvOption option = options.get(start + i);
            form.button("§f" + ItemUtil.describe(option.template()) + "\n§7背包里有 §f×" + option.count());
        }
        int idx = count;
        int prevIdx = -1;
        int nextIdx = -1;
        if (p > 0) {
            prevIdx = idx++;
            form.button("§e上一页");
        }
        if (p + 1 < pages) {
            nextIdx = idx++;
            form.button("§e下一页");
        }
        form.button("§c取消");
        int finalStart = start;
        int finalPage = p;
        int finalPrev = prevIdx;
        int finalNext = nextIdx;
        form.validResultHandler(r -> sync(() -> {
            int id = r.clickedButtonId();
            if (id >= 0 && id < count) openListForm(player, finalStart + id);
            else if (id == finalPrev) openListPick(player, finalPage - 1);
            else if (id == finalNext) openListPick(player, finalPage + 1);
            else openMyShop(player);
        }));
        send(player, form);
    }

    public void openListForm(Player player, int itemIndex) {
        List<InvOption> options = inventoryOptions(player);
        if (itemIndex < 0 || itemIndex >= options.size()) {
            Msg.send(player, "背包里已经没有这件物品了");
            openMyShop(player);
            return;
        }
        InvOption chosen = options.get(itemIndex);
        CustomForm.Builder form = CustomForm.builder()
                .title("§b上架物品")
                .label("§f" + ItemUtil.describe(chosen.template()) + " §7背包里有 §f×" + chosen.count()
                        + "\n§7数量留空按 1 个，§e单价必须填写")
                .input("数量（留空=1）", "1")
                .input("单价（每个，必填）", "10")
                .dropdown("结算货币", List.of("金币", "曦荧币"));
        form.validResultHandler(r -> sync(() -> {
            FormValues values = FormValues.of(r);
            String amountText = values.input(0).trim();
            String priceText = values.input(1).trim();
            int amount;
            double price;
            try {
                amount = amountText.isEmpty() ? 1 : Integer.parseInt(amountText);
                price = priceText.isEmpty() ? 0 : Double.parseDouble(priceText);
            } catch (Exception e) {
                plugin.getLogger().warning("上架表单解析失败：" + r.getResponses() + " / " + r.getComponentTypes());
                Msg.send(player, "数量或单价不是有效数字");
                openMyShop(player);
                return;
            }
            if (price <= 0) {
                Msg.send(player, "请填写单价（每个）后再提交");
                openListForm(player, itemIndex);
                return;
            }
            Currency currency = values.dropdown(0) == 1 ? Currency.POINTS : Currency.MONEY;
            ShopService.Result result = service.listItem(player, chosen.template(), amount, price, currency);
            Msg.send(player, result.message());
            openMyShop(player);
        }));
        send(player, form);
    }

    public void openManage(Player player) {
        List<Listing> mine = store.listingsOf(player.getUniqueId());
        if (mine.isEmpty()) {
            Msg.send(player, "你还没有上架任何商品");
            openMyShop(player);
            return;
        }
        int limit = Math.min(20, mine.size());
        SimpleForm.Builder form = SimpleForm.builder()
                .title("§b管理商品")
                .content("§7共 §e" + mine.size() + " §7条，点一条进行改价或下架");
        for (int i = 0; i < limit; i++) {
            Listing l = mine.get(i);
            ItemStack item = ItemUtil.decode(l.item);
            String name = item == null ? "数据异常" : ItemUtil.describe(item);
            form.button("§f" + name + " §7× " + l.amount + "\n§6" + econ.format(l.currency, l.unitPrice));
        }
        int backIdx = limit;
        form.button("§e返回店铺");
        form.validResultHandler(r -> sync(() -> {
            int id = r.clickedButtonId();
            if (id >= 0 && id < limit) openEdit(player, mine.get(id).id);
            else openMyShop(player);
        }));
        send(player, form);
    }

    public void openEdit(Player player, String listingId) {
        Listing l = store.listing(listingId);
        if (l == null) {
            Msg.send(player, "商品已经不存在了");
            openMyShop(player);
            return;
        }
        ItemStack item = ItemUtil.decode(l.item);
        String name = item == null ? "数据异常" : ItemUtil.describe(item);
        CustomForm.Builder form = CustomForm.builder()
                .title("§b编辑商品")
                .label("§f" + name + " §7× " + l.amount + "\n§7当前单价：§6" + econ.format(l.currency, l.unitPrice))
                .input("新单价", Msg.money(l.unitPrice))
                .dropdown("操作", List.of("保存改价", "下架商品"))
                .validResultHandler(r -> sync(() -> {
                    FormValues values = FormValues.of(r);
                    if (values.dropdown(0) == 1) {
                        Msg.send(player, service.cancelListing(player, l).message());
                        openMyShop(player);
                        return;
                    }
                    double price;
                    String priceText = values.input(0).trim();
                    try {
                        price = priceText.isEmpty() ? l.unitPrice : Double.parseDouble(priceText);
                    } catch (Exception e) {
                        plugin.getLogger().warning("改价表单解析失败：" + r.getResponses() + " / " + r.getComponentTypes());
                        Msg.send(player, "单价不是有效数字");
                        openEdit(player, listingId);
                        return;
                    }
                    Msg.send(player, service.changePrice(player, l, price).message());
                    openMyShop(player);
                }));
        send(player, form);
    }

    public void openSearch(Player player) {
        CustomForm.Builder form = CustomForm.builder()
                .title("§b搜索商品")
                .label("§7按物品名 / 店名 / 卖家名搜索")
                .input("关键词", "例如：钻石")
                .validResultHandler(r -> sync(() -> {
                    String key = FormValues.of(r).input(0);
                    List<String> ids = new ArrayList<>();
                    for (Listing l : service.search(key)) ids.add(l.id);
                    openBrowse(player, ids, 0, "§8搜索：" + key);
                }));
        send(player, form);
    }

    public void openRanking(Player player) {
        List<Shop> top = service.topShops(10);
        SimpleForm.Builder form = SimpleForm.builder()
                .title("§6热门店铺")
                .content("§7按累计售出数量排行");
        int count = top.size();
        for (int i = 0; i < count; i++) {
            Shop shop = top.get(i);
            int listings = service.browseShop(shop.owner).size();
            form.button("§6#" + (i + 1) + " §b" + shop.name + "\n§7店主 " + shop.ownerName
                    + " · 售出 " + shop.sales + " · 在售 " + listings);
        }
        int backIdx = count;
        form.button("§c返回主菜单");
        form.validResultHandler(r -> sync(() -> {
            int id = r.clickedButtonId();
            if (id >= 0 && id < count) {
                Shop shop = top.get(id);
                List<String> ids = new ArrayList<>();
                for (Listing l : service.browseShop(shop.owner)) ids.add(l.id);
                openBrowse(player, ids, 0, "§8" + shop.name);
            } else {
                openMain(player);
            }
        }));
        send(player, form);
    }

    public void openTx(Player player) {
        List<Tx> list = service.recentTx(player, 20);
        CustomForm.Builder form = CustomForm.builder().title("§d我的交易记录");
        if (list.isEmpty()) {
            form.label("§7还没有交易记录");
        } else {
            for (Tx tx : list) {
                boolean sold = tx.seller.equalsIgnoreCase(player.getName());
                form.label((sold ? "§a售出 §f" : "§b买入 §f") + tx.item + " §7× " + tx.amount
                        + "\n§7" + TIME.format(new Date(tx.time)) + " · §6" + econ.format(tx.currency, tx.total)
                        + " §7· " + (sold ? tx.buyer : tx.seller));
            }
        }
        form.validResultHandler(r -> sync(() -> openMain(player)));
        send(player, form);
    }

    public void openHelp(Player player) {
        CustomForm.Builder form = CustomForm.builder()
                .title("§b玩法帮助")
                .label("§e开店\n§7主菜单 → 我的店铺 → 创建，费用 " + Msg.money(cfg.createCost) + " 金币，店名 "
                        + cfg.shopNameMin + "~" + cfg.shopNameMax + " 个字。")
                .label("§e上架\n§7我的店铺 → 上架物品 → 选物品、填数量、填单价、选货币。\n§7金币走 CMI 经济，曦荧币走 PlayerPoints。")
                .label("§e税费\n§7" + (cfg.taxMoney <= 0 && cfg.taxPoints <= 0
                        ? "目前免税，成交金额全额到账，收益在店铺里领取。"
                        : "成交后卖家到账：金币扣 " + (int) (cfg.taxMoney * 100) + "% ，曦荧币扣 "
                        + (int) (cfg.taxPoints * 100) + "% ，收益在店铺里领取。"))
                .label("§e购买\n§7全站商品或搜索 → 点商品 → 选数量，物品直接进背包。");
        form.validResultHandler(r -> sync(() -> openMain(player)));
        send(player, form);
    }

    public void openShopByIds(Player player, UUID owner, String title) {
        List<String> ids = new ArrayList<>();
        for (Listing l : service.browseShop(owner)) ids.add(l.id);
        openBrowse(player, ids, 0, title);
    }
}
