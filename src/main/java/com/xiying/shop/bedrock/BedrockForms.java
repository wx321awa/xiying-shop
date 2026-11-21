package com.xiying.shop.bedrock;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.geysermc.cumulus.component.util.ComponentType;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.form.SimpleForm.Builder;
import org.geysermc.cumulus.form.util.FormBuilder;
import org.geysermc.cumulus.response.CustomFormResponse;
import org.geysermc.floodgate.api.FloodgateApi;

public class BedrockForms {
   private final XiYingShopPlugin plugin;
   private final ShopService service;
   private final ShopStore store;
   private final EconomyHook econ;
   private final PluginConfig cfg;
   private static final SimpleDateFormat TIME = new SimpleDateFormat("MM-dd HH:mm");
   private static final int PICK_PAGE_SIZE = 18;

   public BedrockForms(XiYingShopPlugin var1, ShopService var2, ShopStore var3, EconomyHook var4, PluginConfig var5) {
      this.plugin = var1;
      this.service = var2;
      this.store = var3;
      this.econ = var4;
      this.cfg = var5;
   }

   public boolean isBedrock(Player var1) {
      try {
         return Bukkit.getPluginManager().isPluginEnabled("floodgate") && FloodgateApi.getInstance().isFloodgatePlayer(var1.getUniqueId());
      } catch (Throwable var3) {
         return false;
      }
   }

   private void send(Player var1, Object var2) {
      try {
         boolean var3 = FloodgateApi.getInstance().sendForm(var1.getUniqueId(), (FormBuilder)var2);
         if (!var3) {
            Msg.send(var1, "表单发送失败，请重试");
         }
      } catch (Throwable var4) {
         this.plugin.getLogger().warning("基岩表单发送失败：" + var4.getMessage());
         Msg.send(var1, "表单发送失败：" + var4.getMessage());
      }
   }

   private void sync(Runnable var1) {
      Bukkit.getScheduler().runTask(this.plugin, var1);
   }

   public void openMain(Player var1) {
      Shop var2 = this.service.shopOf(var1);
      int var3 = this.service.browse().size();
      int var4 = this.store.shops().size();
      String var5 = "§7玩家开设的小商铺\n\n§f在售商品：§e"
         + var3
         + " §f件\n§f店铺数量：§e"
         + var4
         + " §f家\n§f我的金币：§6"
         + Msg.money(this.econ.money(var1))
         + "\n§f我的曦荧币：§d"
         + this.econ.points(var1.getUniqueId())
         + "\n\n"
         + (var2 == null ? "§7你还没有店铺，点「我的店铺」创建" : "§7我的店铺：§b" + var2.name + (var2.open ? " §a营业中" : " §c已打烊"));
      Builder var6 = (Builder)((Builder)((Builder)SimpleForm.builder().title("§b曦荧玩家商店"))
            .content(var5)
            .button("§a全站商品\n§7浏览所有玩家上架的商品")
            .button("§e搜索商品\n§7按物品 / 店名 / 卖家搜索")
            .button("§b我的店铺\n§7开店、上架、改价、领收益")
            .button("§6热门店铺\n§7按销量排行")
            .button("§d交易记录\n§7最近 20 条成交")
            .button("§7玩法帮助")
            .button("§c关闭")
            .validResultHandler(var2x -> this.sync(() -> {
                  switch (var2x.clickedButtonId()) {
                     case 0:
                        this.openBrowse(var1, this.allIds(), 0, "§8全站商品");
                        break;
                     case 1:
                        this.openSearch(var1);
                        break;
                     case 2:
                        this.openMyShop(var1);
                        break;
                     case 3:
                        this.openRanking(var1);
                        break;
                     case 4:
                        this.openTx(var1);
                        break;
                     case 5:
                        this.openHelp(var1);
                  }
               })))
         .closedOrInvalidResultHandler(() -> {
         });
      this.send(var1, var6);
   }

   private List<String> allIds() {
      ArrayList var1 = new ArrayList();

      for (Listing var3 : this.service.browse()) {
         var1.add(var3.id);
      }

      return var1;
   }

   public void openBrowse(Player var1, List<String> var2, int var3, String var4) {
      int var5 = this.cfg.bedrockPageSize;
      ArrayList var6 = new ArrayList();

      for (String var8 : var2) {
         if (this.store.listing(var8) != null) {
            var6.add(var8);
         }
      }

      ArrayList var24 = var6;
      int var25 = Math.max(1, (var6.size() + var5 - 1) / var5);
      int var9 = Math.max(0, Math.min(var3, var25 - 1));
      int var10 = var9 * var5;
      int var11 = Math.max(0, Math.min(var5, var6.size() - var10));
      Builder var12 = ((Builder)SimpleForm.builder().title(var4)).content("§7第 §e" + (var9 + 1) + "§7 / §e" + var25 + " §7页 · 共 §e" + var6.size() + " §7件");
      ArrayList var13 = new ArrayList();

      for (int var14 = 0; var14 < var11; var14++) {
         Listing var15 = this.store.listing((String)var24.get(var10 + var14));
         if (var15 != null) {
            ItemStack var16 = ItemUtil.decode(var15.item);
            String var17 = var16 == null ? "数据异常" : ItemUtil.describe(var16);
            var12.button("§f" + var17 + " §7× " + var15.amount + "\n§6" + this.econ.format(var15.currency, var15.unitPrice) + " §7· " + var15.shopName);
            var13.add(var15.id);
         }
      }

      boolean var26 = var9 > 0;
      boolean var27 = var9 + 1 < var25;
      int var28 = var13.size();
      int var29 = -1;
      int var18 = -1;
      if (var26) {
         var29 = var28++;
         var12.button("§e上一页");
      }

      if (var27) {
         var18 = var28++;
         var12.button("§e下一页");
      }

      int var19 = var28;
      var12.button("§c返回主菜单");
      int var21 = var29;
      int var22 = var18;
      var12.validResultHandler(var9x -> this.sync(() -> {
            int var10x = var9x.clickedButtonId();
            if (var10x >= 0 && var10x < var13.size()) {
               this.openDetail(var1, (String)var13.get(var10x), var6, var9, var4);
            } else if (var10x == var21) {
               this.openBrowse(var1, var6, var9 - 1, var4);
            } else if (var10x == var22) {
               this.openBrowse(var1, var6, var9 + 1, var4);
            } else if (var10x == var19) {
               this.openMain(var1);
            }
         }));
      this.send(var1, var12);
   }

   public void openDetail(Player var1, String var2, List<String> var3, int var4, String var5) {
      Listing var6 = this.store.listing(var2);
      if (var6 == null) {
         Msg.send(var1, "这件商品已经不存在了");
         this.openMain(var1);
      } else {
         ItemStack var7 = ItemUtil.decode(var6.item);
         String var8 = var7 == null ? "数据异常" : ItemUtil.describe(var7);
         String var9 = "§f物品：§e"
            + var8
            + "\n§f单价：§6"
            + this.econ.format(var6.currency, var6.unitPrice)
            + "\n§f库存：§e"
            + var6.amount
            + "\n§f店铺：§b"
            + var6.shopName
            + " §7("
            + var6.ownerName
            + ")\n\n§7购买后物品会直接放进背包";
         Builder var10 = ((Builder)SimpleForm.builder().title("§8" + var8)).content(var9);
         ArrayList var11 = new ArrayList();

         for (int var15 : new int[]{1, 8, 64, var6.amount}) {
            int var16 = Math.min(var15, var6.amount);
            if (var16 > 0 && !var11.contains(var16)) {
               var11.add(var16);
               var10.button("§a购买 " + var16 + " 个\n§7" + this.econ.format(var6.currency, var6.unitPrice * (double)var16));
            }
         }

         var10.button("§e返回");
         var10.validResultHandler(var8x -> this.sync(() -> {
               int var9x = var8x.clickedButtonId();
               if (var9x >= 0 && var9x < var11.size()) {
                  ShopService.Result var10x = this.service.purchase(var1, var6, (Integer)var11.get(var9x));
                  Msg.send(var1, var10x.message());
                  if (this.store.listing(var2) != null) {
                     this.openDetail(var1, var2, var3, var4, var5);
                  } else {
                     this.openBrowse(var1, var3, var4, var5);
                  }
               } else {
                  this.openBrowse(var1, var3, var4, var5);
               }
            }));
         this.send(var1, var10);
      }
   }

   public void openMyShop(Player var1) {
      Shop var2 = this.service.shopOf(var1);
      if (var2 == null) {
         org.geysermc.cumulus.form.ModalForm.Builder var7 = (org.geysermc.cumulus.form.ModalForm.Builder)((org.geysermc.cumulus.form.ModalForm.Builder)ModalForm.builder()
               .title("§b创建店铺"))
            .content("§f创建费用：§6" + Msg.money(this.cfg.createCost) + " 金币\n\n§7店铺可以上架商品、设置价格，\n§7买家购买后收益会存进店铺待领取。\n\n§7确定要开店吗？")
            .button1("§a创建店铺")
            .button2("§c再想想")
            .validResultHandler(var2x -> this.sync(() -> {
                  if (var2x.clickedFirst()) {
                     this.openCreateName(var1);
                  } else {
                     this.openMain(var1);
                  }
               }));
         this.send(var1, var7);
      } else {
         List<Listing> var3 = this.store.listingsOf(var1.getUniqueId());
         int var4 = 0;

         for (Listing var6 : var3) {
            var4 += var6.amount;
         }

         String var8 = "§f店名：§b"
            + var2.name
            + "\n§f状态："
            + (var2.open ? "§a营业中" : "§c已打烊")
            + "\n§f商品：§e"
            + var3.size()
            + " §7/ "
            + this.cfg.maxListings
            + " 条，共 §e"
            + var4
            + " §7个\n§f累计售出：§e"
            + var2.sales
            + " §7个\n§f待领取：§6"
            + Msg.money(var2.pendingMoney)
            + " 金币 §7/ §d"
            + var2.pendingPoints
            + " 曦荧币";
         Builder var9 = (Builder)((Builder)SimpleForm.builder().title("§8我的店铺"))
            .content(var8)
            .button("§a上架物品\n§7从背包选择物品，填数量与单价")
            .button("§b管理商品\n§7改价 / 下架")
            .button("§6领取金币收益\n§7" + Msg.money(var2.pendingMoney) + " 金币")
            .button("§d领取曦荧币收益\n§7" + var2.pendingPoints + " 曦荧币")
            .button(var2.open ? "§c打烊\n§7商品暂时不展示" : "§a开张\n§7让商品重新上架")
            .button("§e返回主菜单")
            .validResultHandler(var2x -> this.sync(() -> {
                  switch (var2x.clickedButtonId()) {
                     case 0:
                        this.openListPick(var1);
                        break;
                     case 1:
                        this.openManage(var1);
                        break;
                     case 2:
                        this.service.claim(var1, Currency.MONEY);
                        this.openMyShop(var1);
                        break;
                     case 3:
                        this.service.claim(var1, Currency.POINTS);
                        this.openMyShop(var1);
                        break;
                     case 4:
                        Msg.send(var1, this.service.toggleOpen(var1).message());
                        this.openMyShop(var1);
                        break;
                     default:
                        this.openMain(var1);
                  }
               }));
         this.send(var1, var9);
      }
   }

   private void openCreateName(Player var1) {
      org.geysermc.cumulus.form.CustomForm.Builder var2 = (org.geysermc.cumulus.form.CustomForm.Builder)((org.geysermc.cumulus.form.CustomForm.Builder)CustomForm.builder()
            .title("§b给店铺起个名字"))
         .label("§7店名 " + this.cfg.shopNameMin + "~" + this.cfg.shopNameMax + " 个字，不能带空格")
         .input("店名", "例如：曦荧杂货铺")
         .validResultHandler(var2x -> this.sync(() -> {
               String var3 = BedrockForms.FormValues.of(var2x).input(0);
               ShopService.Result var4 = this.service.createShop(var1, var3);
               Msg.send(var1, var4.message());
               if (var4.ok()) {
                  this.openMyShop(var1);
               } else {
                  this.openMain(var1);
               }
            }));
      this.send(var1, var2);
   }

   private List<BedrockForms.InvOption> inventoryOptions(Player var1) {
      ArrayList<BedrockForms.InvOption> var2 = new ArrayList<>();
      ArrayList<ItemStack> var3 = new ArrayList<>();

      for (ItemStack var7 : var1.getInventory().getStorageContents()) {
         var3.add(var7);
      }

      var3.add(var1.getInventory().getItemInOffHand());

      for (ItemStack var10 : var3) {
         if (var10 != null && !var10.getType().isAir() && !this.cfg.blacklisted(var10.getType())) {
            boolean var11 = false;

            for (int var12 = 0; var12 < var2.size(); var12++) {
               BedrockForms.InvOption var8 = (BedrockForms.InvOption)var2.get(var12);
               if (var8.template().isSimilar(var10)) {
                  var2.set(var12, new BedrockForms.InvOption(var8.template(), var8.count() + var10.getAmount()));
                  var11 = true;
                  break;
               }
            }

            if (!var11) {
               ItemStack var13 = var10.clone();
               var13.setAmount(1);
               var2.add(new BedrockForms.InvOption(var13, var10.getAmount()));
            }
         }
      }

      return var2;
   }

   public void openListPick(Player var1) {
      this.openListPick(var1, 0);
   }

   public void openListPick(Player var1, int var2) {
      List var3 = this.inventoryOptions(var1);
      if (var3.isEmpty()) {
         Msg.send(var1, "背包里没有可以上架的物品");
         this.openMyShop(var1);
      } else {
         int var4 = Math.max(1, (var3.size() + 18 - 1) / 18);
         int var5 = Math.max(0, Math.min(var2, var4 - 1));
         int var6 = var5 * 18;
         int var7 = Math.max(0, Math.min(18, var3.size() - var6));
         Builder var8 = ((Builder)SimpleForm.builder().title("§b选择上架物品"))
            .content("§7共 §e" + var3.size() + " §7种物品 · 第 §e" + (var5 + 1) + "§7 / §e" + var4 + " §7页\n§7选一件，下一步填数量和单价");

         for (int var9 = 0; var9 < var7; var9++) {
            BedrockForms.InvOption var10 = (BedrockForms.InvOption)var3.get(var6 + var9);
            var8.button("§f" + ItemUtil.describe(var10.template()) + "\n§7背包里有 §f×" + var10.count());
         }

         int var16 = var7;
         int var18 = -1;
         int var11 = -1;
         if (var5 > 0) {
            var16 = var7 + 1;
            var18 = var7;
            var8.button("§e上一页");
         }

         if (var5 + 1 < var4) {
            var11 = var16++;
            var8.button("§e下一页");
         }

         var8.button("§c取消");
         int var14 = var18;
         int var15 = var11;
         var8.validResultHandler(var8x -> this.sync(() -> {
               int var9x = var8x.clickedButtonId();
               if (var9x >= 0 && var9x < var7) {
                  this.openListForm(var1, ((BedrockForms.InvOption)var3.get(var6 + var9x)).template());
               } else if (var9x == var14) {
                  this.openListPick(var1, var5 - 1);
               } else if (var9x == var15) {
                  this.openListPick(var1, var5 + 1);
               } else {
                  this.openMyShop(var1);
               }
            }));
         this.send(var1, var8);
      }
   }

   public void openListForm(Player var1, ItemStack var2) {
      if (var2 != null && !var2.getType().isAir()) {
         ItemStack var3 = var2.clone();
         var3.setAmount(1);
         int var4 = this.service.countSimilar(var1, var3);
         if (var4 <= 0) {
            Msg.send(var1, "背包里已经没有这件物品了");
            this.openMyShop(var1);
         } else {
            org.geysermc.cumulus.form.CustomForm.Builder var5 = ((org.geysermc.cumulus.form.CustomForm.Builder)CustomForm.builder().title("§b上架物品"))
               .label("§f" + ItemUtil.describe(var3) + " §7背包里有 §f×" + var4 + "\n§7数量留空按 1 个，§e单价必须填写")
               .input("数量（留空=1）", "1")
               .input("单价（每个，必填）", "10")
               .dropdown("结算货币", List.of("金币", "曦荧币"));
            var5.validResultHandler(var3x -> this.sync(() -> {
                  BedrockForms.FormValues var7 = BedrockForms.FormValues.of(var3x);
                  String var8 = var7.input(0).trim();
                  String var9 = var7.input(1).trim();

                  double var4x;
                  int var6;
                  try {
                     var6 = var8.isEmpty() ? 1 : Integer.parseInt(var8);
                     var4x = var9.isEmpty() ? 0.0 : Double.parseDouble(var9);
                  } catch (Exception var12) {
                     this.plugin.getLogger().warning("上架表单解析失败：" + var3x.getResponses() + " / " + var3x.getComponentTypes());
                     Msg.send(var1, "数量或单价不是有效数字");
                     this.openMyShop(var1);
                     return;
                  }

                  if (var4x <= 0.0) {
                     Msg.send(var1, "请填写单价（每个）后再提交");
                     this.openListForm(var1, var3);
                  } else {
                     Currency var10 = var7.dropdown(0) == 1 ? Currency.POINTS : Currency.MONEY;
                     ShopService.Result var11 = this.service.listItem(var1, var3, var6, var4x, var10);
                     Msg.send(var1, var11.message());
                     this.openMyShop(var1);
                  }
               }));
            this.send(var1, var5);
         }
      } else {
         Msg.send(var1, "背包里已经没有这件物品了");
         this.openMyShop(var1);
      }
   }

   public void openManage(Player var1) {
      List var2 = this.store.listingsOf(var1.getUniqueId());
      if (var2.isEmpty()) {
         Msg.send(var1, "你还没有上架任何商品");
         this.openMyShop(var1);
      } else {
         int var3 = var2.size();
         Builder var4 = ((Builder)SimpleForm.builder().title("§b管理商品")).content("§7共 §e" + var2.size() + " §7条，点一条进行改价或下架");

         for (int var5 = 0; var5 < var3; var5++) {
            Listing var6 = (Listing)var2.get(var5);
            ItemStack var7 = ItemUtil.decode(var6.item);
            String var8 = var7 == null ? "数据异常" : ItemUtil.describe(var7);
            var4.button("§f" + var8 + " §7× " + var6.amount + "\n§6" + this.econ.format(var6.currency, var6.unitPrice));
         }

         var4.button("§e返回店铺");
         var4.validResultHandler(var4x -> this.sync(() -> {
               int var5x = var4x.clickedButtonId();
               if (var5x >= 0 && var5x < var3) {
                  this.openEdit(var1, ((Listing)var2.get(var5x)).id);
               } else {
                  this.openMyShop(var1);
               }
            }));
         this.send(var1, var4);
      }
   }

   public void openEdit(Player var1, String var2) {
      Listing var3 = this.store.listing(var2);
      if (var3 == null) {
         Msg.send(var1, "商品已经不存在了");
         this.openMyShop(var1);
      } else {
         ItemStack var4 = ItemUtil.decode(var3.item);
         String var5 = var4 == null ? "数据异常" : ItemUtil.describe(var4);
         org.geysermc.cumulus.form.CustomForm.Builder var6 = (org.geysermc.cumulus.form.CustomForm.Builder)((org.geysermc.cumulus.form.CustomForm.Builder)CustomForm.builder()
               .title("§b编辑商品"))
            .label("§f" + var5 + " §7× " + var3.amount + "\n§7当前单价：§6" + this.econ.format(var3.currency, var3.unitPrice))
            .input("新单价", Msg.money(var3.unitPrice))
            .dropdown("操作", List.of("保存改价", "下架商品"))
            .validResultHandler(var4x -> this.sync(() -> {
                  BedrockForms.FormValues var7 = BedrockForms.FormValues.of(var4x);
                  if (var7.dropdown(0) == 1) {
                     Msg.send(var1, this.service.cancelListing(var1, var3).message());
                     this.openMyShop(var1);
                  } else {
                     String var8 = var7.input(0).trim();

                     double var5x;
                     try {
                        var5x = var8.isEmpty() ? var3.unitPrice : Double.parseDouble(var8);
                     } catch (Exception var10) {
                        this.plugin.getLogger().warning("改价表单解析失败：" + var4x.getResponses() + " / " + var4x.getComponentTypes());
                        Msg.send(var1, "单价不是有效数字");
                        this.openEdit(var1, var2);
                        return;
                     }

                     Msg.send(var1, this.service.changePrice(var1, var3, var5x).message());
                     this.openMyShop(var1);
                  }
               }));
         this.send(var1, var6);
      }
   }

   public void openSearch(Player var1) {
      org.geysermc.cumulus.form.CustomForm.Builder var2 = (org.geysermc.cumulus.form.CustomForm.Builder)((org.geysermc.cumulus.form.CustomForm.Builder)CustomForm.builder()
            .title("§b搜索商品"))
         .label("§7按物品名 / 店名 / 卖家名搜索")
         .input("关键词", "例如：钻石")
         .validResultHandler(var2x -> this.sync(() -> {
               String var3 = BedrockForms.FormValues.of(var2x).input(0);
               ArrayList var4 = new ArrayList();

               for (Listing var6 : this.service.search(var3)) {
                  var4.add(var6.id);
               }

               this.openBrowse(var1, var4, 0, "§8搜索：" + var3);
            }));
      this.send(var1, var2);
   }

   public void openRanking(Player var1) {
      List var2 = this.service.topShops(10);
      Builder var3 = ((Builder)SimpleForm.builder().title("§6热门店铺")).content("§7按累计售出数量排行");
      int var4 = var2.size();

      for (int var5 = 0; var5 < var4; var5++) {
         Shop var6 = (Shop)var2.get(var5);
         int var7 = this.service.browseShop(var6.owner).size();
         var3.button("§6#" + (var5 + 1) + " §b" + var6.name + "\n§7店主 " + var6.ownerName + " · 售出 " + var6.sales + " · 在售 " + var7);
      }

      var3.button("§c返回主菜单");
      var3.validResultHandler(var4x -> this.sync(() -> {
            int var5x = var4x.clickedButtonId();
            if (var5x >= 0 && var5x < var4) {
               Shop var6x = (Shop)var2.get(var5x);
               ArrayList var7x = new ArrayList();

               for (Listing var9 : this.service.browseShop(var6x.owner)) {
                  var7x.add(var9.id);
               }

               this.openBrowse(var1, var7x, 0, "§8" + var6x.name);
            } else {
               this.openMain(var1);
            }
         }));
      this.send(var1, var3);
   }

   public void openTx(Player var1) {
      List<Tx> var2 = this.service.recentTx(var1, 20);
      org.geysermc.cumulus.form.CustomForm.Builder var3 = (org.geysermc.cumulus.form.CustomForm.Builder)CustomForm.builder().title("§d我的交易记录");
      if (var2.isEmpty()) {
         var3.label("§7还没有交易记录");
      } else {
         for (Tx var5 : var2) {
            boolean var6 = ShopService.txIsMine(var5, var1, true);
            var3.label(
               (var6 ? "§a售出 §f" : "§b买入 §f")
                  + var5.item
                  + " §7× "
                  + var5.amount
                  + "\n§7"
                  + TIME.format(new Date(var5.time))
                  + " · §6"
                  + this.econ.format(var5.currency, var5.total)
                  + " §7· "
                  + (var6 ? var5.buyer : var5.seller)
            );
         }
      }

      var3.validResultHandler(var2x -> this.sync(() -> this.openMain(var1)));
      this.send(var1, var3);
   }

   public void openHelp(Player var1) {
      org.geysermc.cumulus.form.CustomForm.Builder var2 = ((org.geysermc.cumulus.form.CustomForm.Builder)CustomForm.builder().title("§b玩法帮助"))
         .label("§e开店\n§7主菜单 → 我的店铺 → 创建，费用 " + Msg.money(this.cfg.createCost) + " 金币，店名 " + this.cfg.shopNameMin + "~" + this.cfg.shopNameMax + " 个字。")
         .label("§e上架\n§7我的店铺 → 上架物品 → 选物品、填数量、填单价、选货币。\n§7金币走 CMI 经济，曦荧币走 PlayerPoints。")
         .label(
            "§e税费\n§7"
               + (
                  this.cfg.taxMoney <= 0.0 && this.cfg.taxPoints <= 0.0
                     ? "目前免税，成交金额全额到账，收益在店铺里领取。"
                     : "成交后卖家到账：金币扣 " + (int)(this.cfg.taxMoney * 100.0) + "% ，曦荧币扣 " + (int)(this.cfg.taxPoints * 100.0) + "% ，收益在店铺里领取。"
               )
         )
         .label("§e购买\n§7全站商品或搜索 → 点商品 → 选数量，物品直接进背包。");
      var2.validResultHandler(var2x -> this.sync(() -> this.openMain(var1)));
      this.send(var1, var2);
   }

   public void openShopByIds(Player var1, UUID var2, String var3) {
      ArrayList var4 = new ArrayList();

      for (Listing var6 : this.service.browseShop(var2)) {
         var4.add(var6.id);
      }

      this.openBrowse(var1, var4, 0, var3);
   }

   private static final class FormValues {
      private final List<String> inputs = new ArrayList<>();
      private final List<Integer> dropdowns = new ArrayList<>();

      static BedrockForms.FormValues of(CustomFormResponse var0) {
         BedrockForms.FormValues var1 = new BedrockForms.FormValues();
         List<ComponentType> var2 = var0.getComponentTypes();
         JsonArray var3 = var0.getResponses();
         boolean var4 = var3 != null && var3.size() == var2.size();
         int var5 = 0;

         for (ComponentType var7 : var2) {
            if (var4 || var7 != ComponentType.LABEL) {
               JsonElement var8 = var3 != null && var5 < var3.size() ? var3.get(var5++) : null;
               switch (var7) {
                  case INPUT:
                     var1.inputs.add(var8 != null && !var8.isJsonNull() ? var8.getAsString() : "");
                     break;
                  case DROPDOWN:
                     var1.dropdowns.add(var8 != null && !var8.isJsonNull() ? var8.getAsInt() : 0);
               }
            }
         }

         return var1;
      }

      String input(int var1) {
         return var1 >= 0 && var1 < this.inputs.size() ? this.inputs.get(var1) : "";
      }

      int dropdown(int var1) {
         return var1 >= 0 && var1 < this.dropdowns.size() ? this.dropdowns.get(var1) : 0;
      }
   }

   private static record InvOption(ItemStack template, int count) {
   }
}
