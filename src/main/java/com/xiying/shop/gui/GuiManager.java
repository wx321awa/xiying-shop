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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GuiManager {
   private final XiYingShopPlugin plugin;
   private final ShopService service;
   private final ShopStore store;
   private final EconomyHook econ;
   private final PluginConfig cfg;
   private static final SimpleDateFormat TIME = new SimpleDateFormat("MM-dd HH:mm");

   public GuiManager(XiYingShopPlugin var1, ShopService var2, ShopStore var3, EconomyHook var4, PluginConfig var5) {
      this.plugin = var1;
      this.service = var2;
      this.store = var3;
      this.econ = var4;
      this.cfg = var5;
   }

   public void openMain(Player var1) {
      GuiHolder var2 = new GuiHolder(GuiType.MAIN);
      Inventory var3 = Bukkit.createInventory(var2, 54, Msg.c("§8曦荧玩家商店"));
      var2.setInventory(var3);
      this.fill(var3);
      Shop var4 = this.service.shopOf(var1);
      var3.setItem(
         4,
         icon(
            var4 == null ? Material.CHEST : material(var4.icon),
            "§b我的店铺",
            var4 == null
               ? List.of("§7还没有店铺", "§7点击创建（" + Msg.money(this.cfg.createCost) + " 金币）")
               : List.of(
                  "§7店名：§f" + var4.name,
                  "§7状态：" + (var4.open ? "§a营业中" : "§c已打烊"),
                  "§7商品：" + this.store.listingsOf(var1.getUniqueId()).size() + " 条",
                  "",
                  "§e点击管理"
               )
         )
      );
      var3.setItem(19, icon(Material.EMERALD, "§a全站商品", List.of("§7浏览所有玩家上架的商品")));
      var3.setItem(21, icon(Material.COMPASS, "§e搜索商品", List.of("§7按物品名 / 店名 / 卖家搜索")));
      var3.setItem(23, icon(Material.GOLD_INGOT, "§6热门店铺", List.of("§7按销量排行")));
      var3.setItem(25, icon(Material.PAPER, "§d交易记录", List.of("§7查看你最近的成交记录")));
      var3.setItem(40, icon(Material.BOOK, "§f帮助", List.of("§7怎么开店、怎么上架、怎么买东西")));
      var3.setItem(49, icon(Material.BARRIER, "§c关闭", List.of()));
      var1.openInventory(var3);
   }

   public void openBrowseAll(Player var1) {
      ArrayList var2 = new ArrayList();

      for (Listing var4 : this.service.browse()) {
         var2.add(var4.id);
      }

      this.openByIds(var1, var2, 0, "§8全站商品");
   }

   public void openByIds(Player var1, List<String> var2, int var3, String var4) {
      GuiHolder var6 = new GuiHolder(GuiType.BROWSE);
      var6.ids = var2;
      var6.title = var4;
      byte var7 = 54;
      Inventory var8 = Bukkit.createInventory(var6, var7, Msg.c(var4));
      var6.setInventory(var8);
      this.fill(var8);
      int var9 = this.cfg.browsePageSize;
      var6.perPage = var9;
      int var10 = Math.max(1, (var2.size() + var9 - 1) / var9);
      int var5;
      var6.page = var5 = Math.max(0, Math.min(var3, var10 - 1));
      int var11 = var5 * var9;

      for (int var12 = 0; var12 < var9 && var11 + var12 < var2.size(); var12++) {
         Listing var13 = this.store.listing((String)var2.get(var11 + var12));
         if (var13 != null) {
            var8.setItem(var12, this.listingIcon(var13));
         }
      }

      if (var2.isEmpty()) {
         var8.setItem(22, icon(Material.BARRIER, "§7暂时没有商品", List.of("§7换个关键词或者晚点再来")));
      }

      var8.setItem(45, var5 > 0 ? icon(Material.ARROW, "§e上一页", List.of("§7第 " + var5 + " / " + var10 + " 页")) : null);
      var8.setItem(49, icon(Material.CHEST, "§b返回主菜单", List.of()));
      var8.setItem(53, var5 + 1 < var10 ? icon(Material.ARROW, "§e下一页", List.of("§7第 " + (var5 + 2) + " / " + var10 + " 页")) : null);
      var8.setItem(48, icon(Material.PAPER, "§7第 " + (var5 + 1) + " / " + var10 + " 页", List.of("§7共 " + var2.size() + " 条商品")));
      var1.openInventory(var8);
   }

   private ItemStack listingIcon(Listing var1) {
      ItemStack var2 = ItemUtil.decode(var1.item);
      if (var2 == null) {
         return icon(Material.BARRIER, "§c商品数据异常", List.of("§7联系管理员处理"));
      } else {
         var2 = var2.clone();
         var2.setAmount(Math.min(Math.max(1, var1.amount), ItemUtil.maxStack(var2)));
         ItemMeta var3 = var2.getItemMeta();
         if (var3 != null) {
            ArrayList<String> var4 = new ArrayList<>();
            if (var3.lore() != null) {
               for (Component var6 : var3.lore()) {
                  var4.add(Msg.plain(var6));
               }
            }

            var4.add("§8────────────");
            var4.add("§7单价：§6" + this.econ.format(var1.currency, var1.unitPrice));
            var4.add("§7库存：§f" + var1.amount);
            var4.add("§7店铺：§f" + var1.shopName);
            var4.add("§7卖家：§f" + var1.ownerName);
            var4.add("");
            var4.add("§e点击查看 / 购买");
            var3.lore(var4.stream().map(Msg::c).toList());
            var2.setItemMeta(var3);
         }

         return var2;
      }
   }

   public void openDetail(Player var1, String var2, String var3, List<String> var4, int var5) {
      Listing var6 = this.store.listing(var2);
      if (var6 == null) {
         Msg.send(var1, "这件商品已经不存在了");
         this.openMain(var1);
      } else {
         GuiHolder var7 = new GuiHolder(GuiType.DETAIL);
         var7.listingId = var2;
         var7.ids = var4;
         var7.page = var5;
         Inventory var8 = Bukkit.createInventory(var7, 27, Msg.c("§8商品详情"));
         var7.setInventory(var8);
         this.fill(var8);
         ItemStack var9 = ItemUtil.decode(var6.item);
         if (var9 != null) {
            var9 = var9.clone();
            var9.setAmount(Math.min(Math.max(1, var6.amount), ItemUtil.maxStack(var9)));
            ItemMeta var10 = var9.getItemMeta();
            if (var10 != null) {
               ArrayList<String> var11 = new ArrayList<>();
               var11.add("§8────────────");
               var11.add("§7单价：§6" + this.econ.format(var6.currency, var6.unitPrice));
               var11.add("§7库存：§f" + var6.amount);
               var11.add("§7店铺：§f" + var6.shopName + " §7(§f" + var6.ownerName + "§7)");
               var10.lore(var11.stream().map(Msg::c).toList());
               var9.setItemMeta(var10);
            }

            var8.setItem(13, var9);
         }

         int var13 = Math.min(8, Math.max(1, var6.amount));
         int var14 = Math.min(64, Math.max(1, var6.amount));
         var8.setItem(10, icon(Material.LIME_DYE, "§a购买 1 个", List.of("§7花费 " + this.econ.format(var6.currency, var6.unitPrice))));
         var8.setItem(12, icon(Material.LIME_DYE, "§a购买 " + var13 + " 个", List.of("§7花费 " + this.econ.format(var6.currency, var6.unitPrice * (double)var13))));
         var8.setItem(14, icon(Material.LIME_DYE, "§a购买 " + var14 + " 个", List.of("§7花费 " + this.econ.format(var6.currency, var6.unitPrice * (double)var14))));
         var8.setItem(
            16,
            icon(Material.EMERALD, "§a全部买下 (" + var6.amount + " 个)", List.of("§7花费 " + this.econ.format(var6.currency, var6.unitPrice * (double)var6.amount)))
         );
         var8.setItem(22, icon(Material.ARROW, "§e返回", List.of()));
         var1.openInventory(var8);
      }
   }

   public void openMyShop(Player var1) {
      Shop var2 = this.service.shopOf(var1);
      if (var2 == null) {
         this.openMain(var1);
      } else {
         GuiHolder var3 = new GuiHolder(GuiType.MY_SHOP);
         Inventory var4 = Bukkit.createInventory(var3, 27, Msg.c("§8我的店铺 · " + var2.name));
         var3.setInventory(var4);
         this.fill(var4);
         List<Listing> var5 = this.store.listingsOf(var1.getUniqueId());
         int var6 = 0;

         for (Listing var8 : var5) {
            var6 += var8.amount;
         }

         var4.setItem(
            4,
            icon(
               material(var2.icon),
               "§b" + var2.name,
               List.of(
                  "§7状态：" + (var2.open ? "§a营业中" : "§c已打烊"),
                  "§7商品：§f" + var5.size() + " §7/ " + this.cfg.maxListings + " 条",
                  "§7总数量：§f" + var6,
                  "§7累计售出：§f" + var2.sales + " 个",
                  "§7待领取：§6" + Msg.money(var2.pendingMoney) + " 金币 §7/ §d" + var2.pendingPoints + " 曦荧币"
               )
            )
         );
         var4.setItem(19, icon(Material.HOPPER, "§a上架物品", List.of("§7点自己背包里的物品", "§7再输入单价和数量")));
         var4.setItem(20, icon(Material.CHEST, "§b管理商品", List.of("§7改价 / 下架")));
         var4.setItem(21, icon(Material.GOLD_INGOT, "§6领取金币收益", List.of("§7待领取：§f" + Msg.money(var2.pendingMoney))));
         var4.setItem(22, icon(Material.AMETHYST_SHARD, "§d领取曦荧币收益", List.of("§7待领取：§f" + var2.pendingPoints)));
         var4.setItem(23, icon(Material.LEVER, var2.open ? "§a打烊" : "§a开张", List.of("§7当前：" + (var2.open ? "营业中" : "已打烊"))));
         var4.setItem(26, icon(Material.ARROW, "§e返回", List.of()));
         var1.openInventory(var4);
      }
   }

   public void openManage(Player var1, int var2) {
      List var4 = this.store.listingsOf(var1.getUniqueId());
      GuiHolder var5 = new GuiHolder(GuiType.MANAGE);
      Inventory var6 = Bukkit.createInventory(var5, 54, Msg.c("§8管理商品"));
      var5.setInventory(var6);
      this.fill(var6);
      byte var7 = 45;
      int var8 = Math.max(1, (var4.size() + var7 - 1) / var7);
      int var3;
      var5.page = var3 = Math.max(0, Math.min(var2, var8 - 1));

      for (int var9 = 0; var9 < var7 && var3 * var7 + var9 < var4.size(); var9++) {
         Listing var10 = (Listing)var4.get(var3 * var7 + var9);
         ItemStack var11 = this.listingIcon(var10);
         ItemMeta var12 = var11.getItemMeta();
         if (var12 != null) {
            ArrayList<String> var13 = var12.lore() == null
               ? new ArrayList<String>()
               : new ArrayList<String>(var12.lore().stream().map(Msg::plain).toList());
            var13.add("§e点击改价 / 下架");
            var12.lore(var13.stream().map(Msg::c).toList());
            var11.setItemMeta(var12);
         }

         var6.setItem(var9, var11);
      }

      var6.setItem(45, var3 > 0 ? icon(Material.ARROW, "§e上一页", List.of()) : null);
      var6.setItem(49, icon(Material.CHEST, "§b返回店铺", List.of()));
      var6.setItem(53, var3 + 1 < var8 ? icon(Material.ARROW, "§e下一页", List.of()) : null);
      var1.openInventory(var6);
   }

   public void openListItem(Player var1) {
      GuiHolder var2 = new GuiHolder(GuiType.LIST_ITEM);
      Inventory var3 = Bukkit.createInventory(var2, 27, Msg.c("§8选择要上架的物品"));
      var2.setInventory(var3);
      this.fill(var3);
      var3.setItem(4, icon(Material.BOOK, "§e怎么上架？", List.of("§7直接点你背包里的物品", "§7然后在聊天栏输入：§f单价 数量", "§7例如：§f100 16 §7= 单价 100，上架 16 个")));
      var3.setItem(22, icon(Material.ARROW, "§e返回店铺", List.of()));
      var1.openInventory(var3);
   }

   public void openCurrency(Player var1, ItemStack var2) {
      GuiHolder var4 = new GuiHolder(GuiType.CURRENCY);
      var4.pendingItem = var2 == null ? null : var2.clone();
      Inventory var5 = Bukkit.createInventory(var4, 27, Msg.c("§8选择结算货币"));
      var4.setInventory(var5);
      this.fill(var5);
      ItemStack var3 = var2 == null ? icon(Material.BARRIER, "§c未选择物品", List.of()) : var2.clone();
      if (var3 != null && var3.getAmount() > 1) {
         var3.setAmount(Math.min(var3.getAmount(), 64));
      }

      var5.setItem(13, var3);
      var5.setItem(11, icon(Material.GOLD_INGOT, "§6金币", List.of("§7按 Vault / CMI 金币结算")));
      var5.setItem(15, icon(Material.AMETHYST_SHARD, "§d曦荧币", List.of("§7按 PlayerPoints 结算")));
      var5.setItem(22, icon(Material.ARROW, "§e返回", List.of()));
      var1.openInventory(var5);
   }

   public void openEdit(Player var1, String var2) {
      Listing var3 = this.store.listing(var2);
      if (var3 == null) {
         Msg.send(var1, "这件商品已经不存在了");
         this.openManage(var1, 0);
      } else {
         GuiHolder var4 = new GuiHolder(GuiType.EDIT);
         var4.listingId = var2;
         Inventory var5 = Bukkit.createInventory(var4, 27, Msg.c("§8编辑商品"));
         var4.setInventory(var5);
         this.fill(var5);
         var5.setItem(13, this.listingIcon(var3));
         var5.setItem(11, icon(Material.NAME_TAG, "§e修改单价", List.of("§7当前：§f" + this.econ.format(var3.currency, var3.unitPrice), "§7点击后聊天栏输入新单价")));
         var5.setItem(15, icon(Material.BARRIER, "§c下架商品", List.of("§7物品会放回你的背包")));
         var5.setItem(22, icon(Material.ARROW, "§e返回", List.of()));
         var1.openInventory(var5);
      }
   }

   public void openRanking(Player var1) {
      GuiHolder var2 = new GuiHolder(GuiType.RANKING);
      Inventory var3 = Bukkit.createInventory(var2, 27, Msg.c("§8热门店铺"));
      var2.setInventory(var3);
      this.fill(var3);
      List<Shop> var4 = this.service.topShops(10);
      int var5 = 0;

      for (Shop var7 : var4) {
         List var8 = this.service.browseShop(var7.owner);
         var3.setItem(
            var5++,
            icon(
               material(var7.icon),
               "§6#" + var5 + " §b" + var7.name,
               List.of("§7店主：§f" + var7.ownerName, "§7销量：§e" + var7.sales, "§7在售：§f" + var8.size() + " 种", "", "§e点击查看这家店")
            )
         );
      }

      if (var4.isEmpty()) {
         var3.setItem(13, icon(Material.BARRIER, "§7还没有店铺", List.of()));
      }

      var3.setItem(26, icon(Material.ARROW, "§e返回", List.of()));
      var1.openInventory(var3);
   }

   public void openTx(Player var1) {
      GuiHolder var2 = new GuiHolder(GuiType.TX);
      Inventory var3 = Bukkit.createInventory(var2, 54, Msg.c("§8我的交易记录"));
      var2.setInventory(var3);
      this.fill(var3);
      List var4 = this.service.recentTx(var1, 45);

      for (int var5 = 0; var5 < var4.size(); var5++) {
         Tx var6 = (Tx)var4.get(var5);
         boolean var7 = ShopService.txIsMine(var6, var1, true);
         var3.setItem(
            var5,
            icon(
               Material.PAPER,
               (var7 ? "§a售出 §f" : "§b买入 §f") + var6.item + " §7× " + var6.amount,
               List.of(
                  "§7时间：§f" + TIME.format(new Date(var6.time)),
                  "§7对方：§f" + (var7 ? var6.buyer : var6.seller),
                  "§7金额：§6" + this.econ.format(var6.currency, var6.total)
               )
            )
         );
      }

      if (var4.isEmpty()) {
         var3.setItem(22, icon(Material.BARRIER, "§7还没有交易记录", List.of()));
      }

      var3.setItem(49, icon(Material.ARROW, "§e返回", List.of()));
      var1.openInventory(var3);
   }

   public void openHelp(Player var1) {
      GuiHolder var2 = new GuiHolder(GuiType.HELP);
      Inventory var3 = Bukkit.createInventory(var2, 27, Msg.c("§8玩法帮助"));
      var2.setInventory(var3);
      this.fill(var3);
      var3.setItem(11, icon(Material.CHEST, "§b开店", List.of("§7主菜单点「我的店铺」", "§7创建费用：" + Msg.money(this.cfg.createCost) + " 金币", "§7店名 2~12 个字")));
      var3.setItem(13, icon(Material.HOPPER, "§a上架", List.of("§7店铺 › 上架物品", "§7点背包物品后输入「单价 数量」", "§7可选择金币或曦荧币结算", "§7税费：" + this.taxText())));
      var3.setItem(15, icon(Material.EMERALD, "§e购买", List.of("§7全站商品或搜索", "§7点开商品选择购买数量", "§7物品直接进背包")));
      var3.setItem(26, icon(Material.ARROW, "§e返回", List.of()));
      var1.openInventory(var3);
   }

   private String taxText() {
      return this.cfg.taxMoney <= 0.0 && this.cfg.taxPoints <= 0.0
         ? "§a无（成交全额到账）"
         : "§7金币 " + (int)(this.cfg.taxMoney * 100.0) + "% / 曦荧币 " + (int)(this.cfg.taxPoints * 100.0) + "%";
   }

   public void handleClick(Player var1, GuiHolder var2, int var3) {
      switch (var2.type) {
         case MAIN:
            switch (var3) {
               case 4:
                  if (this.service.shopOf(var1) == null) {
                     this.plugin.getChatInput().startCreateShop(var1);
                  } else {
                     this.openMyShop(var1);
                  }

                  return;
               case 19:
                  this.openBrowseAll(var1);
                  return;
               case 21:
                  this.plugin.getChatInput().startSearch(var1);
                  return;
               case 23:
                  this.openRanking(var1);
                  return;
               case 25:
                  this.openTx(var1);
                  return;
               case 40:
                  this.openHelp(var1);
                  return;
               case 49:
                  var1.closeInventory();
                  return;
               default:
                  return;
            }
         case BROWSE:
            if (var3 == 49) {
               this.openMain(var1);
            } else if (var3 == 45 && var2.page > 0) {
               this.openByIds(var1, var2.ids, var2.page - 1, var2.title);
            } else if (var3 == 53) {
               this.openByIds(var1, var2.ids, var2.page + 1, var2.title);
            } else if (var3 >= 0 && var3 < var2.perPage) {
               int var13 = var2.page * var2.perPage + var3;
               Listing var17 = null;
               if (var13 < var2.ids.size()) {
                  var17 = this.store.listing(var2.ids.get(var13));
               }

               if (var17 != null) {
                  this.openDetail(var1, var17.id, var2.title, var2.ids, var2.page);
               }
            }
            break;
         case DETAIL:
            int var12 = 0;
            Listing var16 = this.store.listing(var2.listingId);
            if (var16 == null) {
               this.openMain(var1);
               return;
            }

            switch (var3) {
               case 10:
                  var12 = 1;
               case 11:
               case 13:
               case 15:
               default:
                  break;
               case 12:
                  var12 = 8;
                  break;
               case 14:
                  var12 = 64;
                  break;
               case 16:
                  var12 = var16.amount;
            }

            if (var3 == 22) {
               this.openByIds(var1, var2.ids, var2.page, var2.title);
            } else {
               if (var12 > var16.amount) {
                  var12 = var16.amount;
               }

               if (var12 > 0) {
                  ShopService.Result var18 = this.service.purchase(var1, var16, var12);
                  Msg.send(var1, var18.message());
                  if (var18.ok() && this.store.listing(var16.id) != null) {
                     this.openDetail(var1, var16.id, var2.title, var2.ids, var2.page);
                  } else if (var18.ok()) {
                     this.openByIds(var1, var2.ids, var2.page, var2.title);
                  }
               }
            }
            break;
         case MY_SHOP:
            switch (var3) {
               case 19:
                  this.openListItem(var1);
                  return;
               case 20:
                  this.openManage(var1, 0);
                  return;
               case 21:
                  this.service.claim(var1, Currency.MONEY);
                  this.openMyShop(var1);
                  return;
               case 22:
                  this.service.claim(var1, Currency.POINTS);
                  this.openMyShop(var1);
                  return;
               case 23:
                  Msg.send(var1, this.service.toggleOpen(var1).message());
                  this.openMyShop(var1);
                  return;
               case 24:
               case 25:
               default:
                  return;
               case 26:
                  this.openMain(var1);
                  return;
            }
         case MANAGE:
            if (var3 == 49) {
               this.openMyShop(var1);
            } else if (var3 == 45 && var2.page > 0) {
               this.openManage(var1, var2.page - 1);
            } else if (var3 == 53) {
               this.openManage(var1, var2.page + 1);
            } else {
               List var11;
               int var15;
               if (var3 >= 0 && var3 < 45 && (var15 = var2.page * 45 + var3) < (var11 = this.store.listingsOf(var1.getUniqueId())).size()) {
                  this.openEdit(var1, ((Listing)var11.get(var15)).id);
               }
            }
            break;
         case EDIT:
            if (var3 == 11) {
               this.plugin.getChatInput().startEditPrice(var1, var2.listingId);
            } else if (var3 == 15) {
               Listing var10 = this.store.listing(var2.listingId);
               Msg.send(var1, this.service.cancelListing(var1, var10).message());
               this.openManage(var1, 0);
            } else if (var3 == 22) {
               this.openManage(var1, 0);
            }
            break;
         case LIST_ITEM:
            if (var3 == 22) {
               this.openMyShop(var1);
            }
            break;
         case CURRENCY:
            if (var3 == 11 || var3 == 15) {
               ItemStack var9 = var2.pendingItem;
               if (var9 == null) {
                  this.openListItem(var1);
                  return;
               }

               Currency var14 = var3 == 11 ? Currency.MONEY : Currency.POINTS;
               this.plugin.getChatInput().startListItem(var1, var9, var14);
            } else if (var3 == 22) {
               this.openListItem(var1);
            }
            break;
         case RANKING:
            if (var3 == 26) {
               this.openMain(var1);
            } else {
               List var4;
               if (var3 >= 0 && var3 < 10 && var3 < (var4 = this.service.topShops(10)).size()) {
                  Shop var5 = (Shop)var4.get(var3);
                  ArrayList var6 = new ArrayList();

                  for (Listing var8 : this.service.browseShop(var5.owner)) {
                     var6.add(var8.id);
                  }

                  this.openByIds(var1, var6, 0, "§8" + var5.name);
               }
            }
            break;
         case TX:
            if (var3 == 49) {
               this.openMain(var1);
            }
            break;
         case HELP:
            if (var3 == 26) {
               this.openMain(var1);
            }
      }
   }

   public void handlePlayerInvClick(Player var1, GuiHolder var2, int var3) {
      if (var2.type == GuiType.LIST_ITEM) {
         if (var3 >= 27 && var3 <= 62) {
            int var4 = var3 - 27;
            int var5 = var4 < 27 ? var4 + 9 : var4 - 27;
            ItemStack var6 = var1.getInventory().getItem(var5);
            if (var6 != null && !var6.getType().isAir()) {
               this.openCurrency(var1, var6);
            }
         }
      }
   }

   private void fill(Inventory var1) {
      ItemStack var2 = icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());

      for (int var3 = 0; var3 < var1.getSize(); var3++) {
         if (var1.getItem(var3) == null) {
            var1.setItem(var3, var2);
         }
      }
   }

   public static ItemStack icon(Material var0, String var1, List<String> var2) {
      ItemStack var3 = new ItemStack(var0);
      ItemMeta var4 = var3.getItemMeta();
      if (var4 != null) {
         var4.displayName(Msg.c(var1));
         if (!var2.isEmpty()) {
            var4.lore(var2.stream().map(Msg::c).toList());
         }

         var3.setItemMeta(var4);
      }

      return var3;
   }

   private static Material material(String var0) {
      Material var1 = Material.matchMaterial(var0 == null ? "" : var0);
      return var1 == null ? Material.CHEST : var1;
   }
}
