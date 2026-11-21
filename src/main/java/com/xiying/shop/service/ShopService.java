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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class ShopService {
   private final XiYingShopPlugin plugin;
   private final ShopStore store;
   private final EconomyHook econ;
   private final PluginConfig cfg;
   private final Map<String, String> searchIndex = new ConcurrentHashMap<>();

   public ShopService(XiYingShopPlugin var1, ShopStore var2, EconomyHook var3, PluginConfig var4) {
      this.plugin = var1;
      this.store = var2;
      this.econ = var3;
      this.cfg = var4;
   }

   public Shop shopOf(Player var1) {
      return this.store.shopOf(var1.getUniqueId());
   }

   public ShopService.Result createShop(Player var1, String var2) {
      if (this.shopOf(var1) != null) {
         return ShopService.Result.fail("你已经有一家店铺了");
      } else {
         String var4 = var2 == null ? "" : var2.trim();
         if (var4.length() < this.cfg.shopNameMin || var4.length() > this.cfg.shopNameMax) {
            return ShopService.Result.fail("店名长度需要在 " + this.cfg.shopNameMin + "~" + this.cfg.shopNameMax + " 个字之间");
         } else if (var4.contains("§") || var4.contains(" ")) {
            return ShopService.Result.fail("店名不能包含空格或颜色符号");
         } else if (this.store.shopByName(var4) != null) {
            return ShopService.Result.fail("这个店名已经被占用了");
         } else {
            if (this.cfg.createCost > 0.0) {
               if (!this.econ.moneyReady()) {
                  return ShopService.Result.fail("经济系统还没加载好，请稍后再试");
               }

               if (!this.econ.has(var1, Currency.MONEY, this.cfg.createCost)) {
                  return ShopService.Result.fail("金币不足，创建店铺需要 " + Msg.money(this.cfg.createCost) + " 金币");
               }

               if (!this.econ.take(var1, Currency.MONEY, this.cfg.createCost)) {
                  return ShopService.Result.fail("扣款失败，请稍后再试");
               }
            }

            ItemStack var3;
            String var6 = (var3 = var1.getInventory().getItemInMainHand()) != null && !var3.getType().isAir() ? var3.getType().name() : "CHEST";
            Shop var7 = new Shop(var1.getUniqueId(), var1.getName(), var4, var6);
            this.store.putShop(var7);
            if (!this.store.saveNow()) {
               this.store.removeShop(var1.getUniqueId());
               if (this.cfg.createCost > 0.0
                  && this.econ.moneyReady()
                  && !this.econ.give(var1.getUniqueId(), var1.getName(), Currency.MONEY, this.cfg.createCost)) {
                  this.plugin.getLogger().severe("[XiYingShop] shop creation rolled back but refund failed: " + var4);
               }

               return ShopService.Result.fail("保存数据失败，创建已取消，金币已退回");
            } else {
               return ShopService.Result.ok("店铺 §e" + var4 + " §f创建成功，输入 §e/pshop §f开始上架商品");
            }
         }
      }
   }

   public ShopService.Result toggleOpen(Player var1) {
      Shop var2 = this.shopOf(var1);
      if (var2 == null) {
         return ShopService.Result.fail("你还没有店铺");
      } else {
         boolean var3 = var2.open;
         var2.open = !var3;
         if (!this.store.saveNow()) {
            var2.open = var3;
            return ShopService.Result.fail("保存数据失败，开关店未生效，请稍后再试");
         } else {
            return ShopService.Result.ok(var2.open ? "店铺已开张营业" : "店铺已打烊，商品暂不展示");
         }
      }
   }

   public void claim(Player var1, Currency var2) {
      Shop var3 = this.shopOf(var1);
      if (var3 == null) {
         Msg.send(var1, "你还没有店铺");
      } else {
         if (var2 == Currency.MONEY) {
            if (var3.pendingMoney <= 1.0E-4) {
               Msg.send(var1, "暂时没有待领取的金币收益");
               return;
            }

            double var4 = var3.pendingMoney;
            var3.pendingMoney = 0.0;
            if (!this.store.saveNow()) {
               var3.pendingMoney = var4;
               Msg.send(var1, "保存数据失败，领取已取消，请稍后再试");
               return;
            }

            if (!this.econ.give(var1.getUniqueId(), var1.getName(), Currency.MONEY, var4)) {
               var3.pendingMoney = var4;
               if (!this.store.saveNow()) {
                  this.plugin.getLogger().severe("[XiYingShop] 金币收益回滚落盘失败，可能丢失：shop=" + var3.name + " amount=" + var4);
               }

               Msg.send(var1, "领取失败：经济系统暂时不可用，收益已退回待领取");
               return;
            }

            Msg.send(var1, "已领取 §6" + Msg.money(var4) + " 金币 §f收益");
         } else {
            if (var3.pendingPoints <= 0) {
               Msg.send(var1, "暂时没有待领取的曦荧币收益");
               return;
            }

            int var6 = var3.pendingPoints;
            var3.pendingPoints = 0;
            if (!this.store.saveNow()) {
               var3.pendingPoints = var6;
               Msg.send(var1, "保存数据失败，领取已取消，请稍后再试");
               return;
            }

            if (!this.econ.give(var1.getUniqueId(), var1.getName(), Currency.POINTS, (double)var6)) {
               var3.pendingPoints = var6;
               if (!this.store.saveNow()) {
                  this.plugin.getLogger().severe("[XiYingShop] 曦荧币收益回滚落盘失败，可能丢失：shop=" + var3.name + " amount=" + var6);
               }

               Msg.send(var1, "领取失败：曦荧币系统暂时不可用，收益已退回待领取");
               return;
            }

            Msg.send(var1, "已领取 §d" + var6 + " 曦荧币 §f收益");
         }
      }
   }

   public ShopService.Result listItem(Player var1, ItemStack var2, int var3, double var4, Currency var6) {
      Shop var7 = this.shopOf(var1);
      if (var7 == null) {
         return ShopService.Result.fail("你还没有店铺，先创建一家吧");
      } else if (!var7.open) {
         return ShopService.Result.fail("店铺正在打烊，先开张再上架");
      } else if (var2 != null && !var2.getType().isAir()) {
         if (this.cfg.blacklisted(var2.getType())) {
            return ShopService.Result.fail("这个物品不允许上架");
         } else if (var3 < 1) {
            return ShopService.Result.fail("数量至少为 1");
         } else if (var3 > this.cfg.maxAmountPerListing) {
            return ShopService.Result.fail("单条最多上架 " + this.cfg.maxAmountPerListing + " 个");
         } else if (var6 == Currency.POINTS && !this.cfg.pointsEnabled) {
            return ShopService.Result.fail("曦荧币结算暂未开启");
         } else if (var6 == Currency.MONEY && !this.cfg.moneyEnabled) {
            return ShopService.Result.fail("金币结算暂未开启");
         } else {
            String var8 = this.priceError(var4, var6);
            if (var8 != null) {
               return ShopService.Result.fail(var8);
            } else {
               int var9 = this.store.listingsOf(var1.getUniqueId()).size();
               if (var9 >= this.cfg.maxListings) {
                  return ShopService.Result.fail("商品位已满（" + this.cfg.maxListings + " 条），先下架一些吧");
               } else {
                  int var10 = this.countSimilar(var1, var2);
                  if (var10 < var3) {
                     return ShopService.Result.fail("背包里可用的该物品不够（当前 " + var10 + " 个）");
                  } else {
                     String var11;
                     try {
                        var11 = ItemUtil.encode(this.single(var2));
                     } catch (RuntimeException var14) {
                        this.plugin.getLogger().warning("上架物品序列化失败：" + ItemUtil.describe(var2) + " / " + var14);
                        return ShopService.Result.fail("这个物品无法上架（数据异常），请换个物品试试");
                     }

                     if (var11 == null || var11.isEmpty()) {
                        return ShopService.Result.fail("这个物品无法上架（数据异常），请换个物品试试");
                     } else if (!this.removeSimilar(var1, var2, var3)) {
                        return ShopService.Result.fail("扣除物品失败，请重试");
                     } else {
                        Listing var12 = new Listing(UUID.randomUUID().toString(), var1.getUniqueId(), var1.getName(), var7.name, var11, var3, var4, var6);

                        try {
                           this.store.putListing(var12);
                           if (!this.store.saveNow()) {
                              throw new IllegalStateException("写盘失败");
                           }
                        } catch (RuntimeException var15) {
                           this.store.removeListing(var12.id);
                           this.refund(var1, var2, var3);
                           this.plugin.getLogger().warning("上架写盘失败，物品已退回：" + var15);
                           return ShopService.Result.fail("上架失败，物品已退回背包");
                        }

                        return ShopService.Result.ok("已上架 §e" + ItemUtil.describe(var2) + " §f× " + var3 + "，单价 " + this.econ.format(var6, var4));
                     }
                  }
               }
            }
         }
      } else {
         return ShopService.Result.fail("请选择要上架的物品");
      }
   }

   public ShopService.Result purchase(Player var1, Listing var2, int var3) {
      if (var2 == null) {
         return ShopService.Result.fail("这件商品已经不存在了");
      } else {
         Listing var5 = this.store.listing(var2.id);
         if (var5 == null) {
            return ShopService.Result.fail("这件商品已经不存在了");
         } else if (var5.owner.equals(var1.getUniqueId())) {
            return ShopService.Result.fail("不能购买自己店铺的商品");
         } else if (Double.isFinite(var5.unitPrice) && !(var5.unitPrice <= 0.0)) {
            Shop var6 = this.store.shopOf(var5.owner);
            if (var6 == null || !var6.open) {
               return ShopService.Result.fail("这家店铺已经打烊了");
            } else if (var3 <= 0) {
               return ShopService.Result.fail("购买数量无效");
            } else if (var5.amount <= 0) {
               return ShopService.Result.fail("这件商品已经售罄了");
            } else {
               int var7 = Math.min(var3, var5.amount);
               double var8 = var5.unitPrice;
               double var10 = var8 * (double)var7;
               if (var5.currency == Currency.POINTS) {
                  var10 = Math.ceil(var10);
               }

               if (Double.isFinite(var10) && !(var10 <= 0.0)) {
                  if (!this.econ.has(var1, var5.currency, var10)) {
                     return ShopService.Result.fail("余额不足，需要 " + this.econ.format(var5.currency, var10));
                  } else {
                     ItemStack var12 = ItemUtil.decode(var5.item);
                     if (var12 == null) {
                        return ShopService.Result.fail("商品数据异常，请联系管理员");
                     } else if (this.freeSpaceFor(var1, var12, var7) < var7) {
                        return ShopService.Result.fail("背包空间不足，请先清理背包（本次要买 " + var7 + " 个）");
                     } else {
                        double var13 = var5.currency == Currency.POINTS ? this.cfg.taxPoints : this.cfg.taxMoney;
                        double var15 = var10 * (1.0 - var13);
                        int var17 = var5.amount;
                        double var18 = var6.pendingMoney;
                        int var20 = var6.pendingPoints;
                        int var21 = var6.sales;
                        Tx var22 = new Tx(
                           var1.getUniqueId().toString(),
                           var1.getName(),
                           var5.owner.toString(),
                           var5.ownerName,
                           ItemUtil.describe(var12),
                           var7,
                           var10,
                           var5.currency
                        );
                        boolean var23 = false;
                        var5.amount -= var7;
                        if (var5.amount <= 0) {
                           this.store.removeListing(var5.id);
                           this.searchIndex.remove(var5.id);
                           var23 = true;
                        }

                        if (var5.currency == Currency.POINTS) {
                           var6.pendingPoints = var6.pendingPoints + (int)Math.floor(var15);
                        } else {
                           var6.pendingMoney = var6.pendingMoney + (double)Math.round(var15 * 100.0) / 100.0;
                        }

                        var6.sales += var7;
                        this.store.addTx(var22);
                        if (!this.store.saveNow()) {
                           this.restoreAfterFailedPurchase(var6, var5, var17, var18, var20, var21, var22, var23);
                           this.store.saveAsync();
                           this.plugin.getLogger().warning("购买预留写盘失败，交易已取消：" + var5.id);
                           return ShopService.Result.fail("保存交易数据失败，购买已取消，请稍后再试");
                        } else if (!this.econ.take(var1, var5.currency, var10)) {
                           this.restoreAfterFailedPurchase(var6, var5, var17, var18, var20, var21, var22, var23);
                           if (!this.store.saveNow()) {
                              this.store.saveAsync();
                              this.plugin.getLogger().severe("[XiYingShop] 扣款失败后库存回滚未能落盘：listing=" + var5.id + " qty=" + var7);
                           }

                           return ShopService.Result.fail("扣款失败，请稍后再试");
                        } else {
                           var12.setAmount(Math.min(var7, ItemUtil.maxStack(var12)));
                           ArrayList<ItemStack> var24 = new ArrayList<>();
                           int var25 = var7;

                           while (var25 > 0) {
                              ItemStack var26 = var12.clone();
                              int var4 = Math.min(var25, ItemUtil.maxStack(var12));
                              var26.setAmount(var4);
                              var24.add(var26);
                              var25 -= var4;
                           }

                           PlayerInventory var31 = var1.getInventory();

                           for (ItemStack var27 : var24) {
                              HashMap<Integer, ItemStack> var28 = var31.addItem(new ItemStack[]{var27});

                              for (ItemStack var30 : var28.values()) {
                                 var1.getWorld().dropItemNaturally(var1.getLocation(), var30);
                              }
                           }

                           Player var33 = Bukkit.getPlayer(var5.owner);
                           if (var33 != null && var33.isOnline()) {
                              Msg.send(
                                 var33, "§a你卖出了 §f" + ItemUtil.describe(var12) + " §a× " + var7 + "，收益 " + this.econ.format(var5.currency, var15) + " 已存入店铺待领取"
                              );
                           }

                           String var34 = var7 < var3 ? "（库存不足，实际买到 " + var7 + " 个）" : "";
                           return ShopService.Result.ok(
                              "购买成功：§e" + ItemUtil.describe(var12) + " §f× " + var7 + "，花费 " + this.econ.format(var5.currency, var10) + var34
                           );
                        }
                     }
                  }
               } else {
                  return ShopService.Result.fail("商品价格异常，请联系管理员");
               }
            }
         } else {
            return ShopService.Result.fail("商品价格异常，请联系管理员");
         }
      }
   }

   private void restoreAfterFailedPurchase(Shop var1, Listing var2, int var3, double var4, int var6, int var7, Tx var8, boolean var9) {
      var2.amount = var3;
      if (var9) {
         this.store.putListing(var2);
      }

      var1.pendingMoney = var4;
      var1.pendingPoints = var6;
      var1.sales = var7;
      this.store.removeTx(var8);
   }

   public ShopService.Result cancelListing(Player var1, Listing var2) {
      if (var2 == null) {
         return ShopService.Result.fail("商品已经不存在了");
      } else {
         Listing var3 = this.store.listing(var2.id);
         if (var3 == null) {
            return ShopService.Result.fail("这件商品已经不存在了（可能已售完或已下架）");
         } else if (!var3.owner.equals(var1.getUniqueId())) {
            return ShopService.Result.fail("这不是你的商品");
         } else {
            ItemStack var4 = ItemUtil.decode(var3.item);
            if (var4 == null) {
               this.store.removeListing(var3.id);
               this.searchIndex.remove(var3.id);
               this.store.saveNow();
               return ShopService.Result.ok("商品数据已损坏，已直接从货架移除");
            } else {
               int var5 = this.freeSpaceFor(var1, var4, var3.amount);
               if (var5 < var3.amount) {
                  return ShopService.Result.fail("背包放不下这 " + var3.amount + " 个（缺 " + (var3.amount - var5) + " 个空位），请先清理背包再下架");
               } else {
                  this.store.removeListing(var3.id);
                  this.searchIndex.remove(var3.id);
                  if (!this.store.saveNow()) {
                     this.store.putListing(var3);
                     return ShopService.Result.fail("保存失败，下架已取消，请稍后再试");
                  } else {
                     this.refund(var1, var4, var3.amount);
                     return ShopService.Result.ok("已下架 §e" + ItemUtil.describe(var4) + " §f，物品已放回背包");
                  }
               }
            }
         }
      }
   }

   public ShopService.Result changePrice(Player var1, Listing var2, double var3) {
      if (var2 == null) {
         return ShopService.Result.fail("商品已经不存在了");
      } else {
         Listing var5 = this.store.listing(var2.id);
         if (var5 == null) {
            return ShopService.Result.fail("这件商品已经不存在了");
         } else if (!var5.owner.equals(var1.getUniqueId())) {
            return ShopService.Result.fail("这不是你的商品");
         } else {
            String var6 = this.priceError(var3, var5.currency);
            if (var6 != null) {
               return ShopService.Result.fail(var6);
            } else {
               double var7 = var5.unitPrice;
               var5.unitPrice = var3;
               if (!this.store.saveNow()) {
                  var5.unitPrice = var7;
                  return ShopService.Result.fail("保存数据失败，改价已取消，请稍后再试");
               } else {
                  return ShopService.Result.ok("单价已改为 " + this.econ.format(var5.currency, var3));
               }
            }
         }
      }
   }

   private String priceError(double var1, Currency var3) {
      if (!Double.isFinite(var1)) {
         return "单价必须是有效数字";
      } else if (var1 <= 0.0) {
         return "单价必须大于 0";
      } else if (var1 < this.cfg.minUnitPrice) {
         return "单价不能低于 " + Msg.money(this.cfg.minUnitPrice);
      } else if (var1 > 1.0E8) {
         return "单价上限为 100000000";
      } else {
         return var3 == Currency.POINTS && var1 != Math.rint(var1) ? "曦荧币单价必须是整数" : null;
      }
   }

   public ShopService.Result deleteShop(Player var1, String var2) {
      Shop var3 = this.shopOf(var1);
      if (var3 == null) {
         return ShopService.Result.fail("你还没有店铺");
      } else if (var2 != null && var3.name.equalsIgnoreCase(var2.trim())) {
         List var4 = this.store.listingsOf(var1.getUniqueId());
         if (!var4.isEmpty()) {
            return ShopService.Result.fail("店铺里还有 " + var4.size() + " 条商品，请先全部下架再删店");
         } else if (!(var3.pendingMoney > 1.0E-4) && var3.pendingPoints <= 0) {
            this.store.removeShop(var1.getUniqueId());
            boolean var5 = false;
            if (this.cfg.createCost > 0.0 && this.econ.moneyReady()) {
               var5 = this.econ.give(var1.getUniqueId(), var1.getName(), Currency.MONEY, this.cfg.createCost);
               if (!var5) {
                  this.plugin
                     .getLogger()
                     .severe("[XiYingShop] delshop 创建费退回失败：shop=" + var3.name + " cost=" + this.cfg.createCost + " owner=" + var1.getName());
               }
            }

            if (!this.store.saveNow()) {
               if (!this.econ.take(var1, Currency.MONEY, this.cfg.createCost)) {
                  this.plugin.getLogger().severe("[XiYingShop] delshop rolled back but creation cost could not be reclaimed: " + var3.name);
               }

               this.store.putShop(var3);
               this.plugin.getLogger().warning("删除店铺写盘失败，已回滚：" + var3.name);
               return ShopService.Result.fail("保存数据失败，删店已取消，请稍后再试");
            } else {
               String var6 = "";
               if (this.cfg.createCost > 0.0) {
                  if (var5) {
                     var6 = "，创建费 " + Msg.money(this.cfg.createCost) + " 金币已退回";
                  } else if (this.econ.moneyReady()) {
                     var6 = "（创建费退回失败，请联系管理员）";
                  } else {
                     var6 = "（经济系统不可用，创建费未退回，请联系管理员）";
                  }
               }

               return ShopService.Result.ok("店铺 §e" + var3.name + " §f已删除" + var6);
            }
         } else {
            return ShopService.Result.fail("店铺里还有未领取的收益，请先领取再删店");
         }
      } else {
         return ShopService.Result.fail("请用 /pshop delshop <你的店名> 精确输入店名来删店");
      }
   }

   public ShopService.Result transferShop(Player var1, String var2, String var3) {
      Shop var4 = this.shopOf(var1);
      if (var4 == null) {
         return ShopService.Result.fail("你还没有店铺");
      } else if (var2 == null || !var4.name.equalsIgnoreCase(var2.trim())) {
         return ShopService.Result.fail("请用 /pshop transfer <你的店名> <接收人> 精确输入店名来转让");
      } else if (var3 != null && !var3.trim().isEmpty()) {
         Player var5 = Bukkit.getPlayerExact(var3.trim());
         if (var5 == null) {
            return ShopService.Result.fail("接收人 " + var3.trim() + " 不在线，请让对方先上线再转让");
         } else if (var5.getUniqueId().equals(var1.getUniqueId())) {
            return ShopService.Result.fail("不能把店铺转让给自己");
         } else if (!var5.hasPermission("xiyingshop.use")) {
            return ShopService.Result.fail("对方没有使用玩家商店的权限，无法接手店铺");
         } else if (this.shopOf(var5) != null) {
            return ShopService.Result.fail("对方已经有一家店铺了，需要对方先删店才能接手");
         } else {
            UUID var6 = var4.owner;
            String var7 = var4.ownerName;
            UUID var8 = var5.getUniqueId();
            String var9 = var5.getName();
            List<Listing> var10 = this.store.listingsOf(var6);
            var4.owner = var8;
            var4.ownerName = var9;

            for (Listing var12 : var10) {
               var12.owner = var8;
               var12.ownerName = var9;
               this.searchIndex.remove(var12.id);
            }

            this.store.removeShop(var6);
            this.store.putShop(var4);
            if (this.store.saveNow()) {
               Msg.send(var5, "§a玩家 §f" + var1.getName() + " §a把店铺 §e" + var4.name + " §a转让给你了，输入 §f/pshop §a开始管理");
               return ShopService.Result.ok("店铺 §e" + var4.name + " §f已转让给 §e" + var5.getName() + " §f（" + var10.size() + " 条在售商品、待领收益随店转移）");
            } else {
               var4.owner = var6;
               var4.ownerName = var7;

               for (Listing var14 : var10) {
                  var14.owner = var6;
                  var14.ownerName = var7;
               }

               this.store.removeShop(var8);
               this.store.putShop(var4);
               this.store.saveAsync();
               return ShopService.Result.fail("保存数据失败，转让已取消，请稍后再试");
            }
         }
      } else {
         return ShopService.Result.fail("请指定接收人");
      }
   }

   private int freeSpaceFor(Player var1, ItemStack var2, int var3) {
      int var4 = ItemUtil.maxStack(var2);
      int var5 = 0;

      for (ItemStack var9 : var1.getInventory().getStorageContents()) {
         if (var9 == null || var9.getType().isAir()) {
            var5 += var4;
         } else if (var9.isSimilar(var2)) {
            var5 += Math.max(0, var4 - var9.getAmount());
         }

         if (var5 >= var3) {
            return var3;
         }
      }

      ItemStack var10 = var1.getInventory().getItemInOffHand();
      if (var10 == null || var10.getType().isAir()) {
         var5 += var4;
      } else if (var10.isSimilar(var2)) {
         var5 += Math.max(0, var4 - var10.getAmount());
      }

      return var5;
   }

   public List<Listing> browse() {
      ArrayList<Listing> var1 = new ArrayList<>();

      for (Listing var3 : this.store.listings()) {
         Shop var4;
         if (var3.amount > 0 && (var4 = this.store.shopOf(var3.owner)) != null && var4.open) {
            var1.add(var3);
         }
      }

      var1.sort((var0, var1x) -> Long.compare(var1x.createdAt, var0.createdAt));
      return var1;
   }

   public List<Listing> browseShop(UUID var1) {
      ArrayList var2 = new ArrayList();
      Shop var3 = this.store.shopOf(var1);
      if (var3 != null && var3.open) {
         for (Listing var5 : this.store.listingsOf(var1)) {
            if (var5.amount > 0) {
               var2.add(var5);
            }
         }

         return var2;
      } else {
         return var2;
      }
   }

   public List<Listing> search(String var1) {
      String var2 = var1 == null ? "" : var1.trim().toLowerCase(Locale.ROOT);
      if (var2.isEmpty()) {
         return this.browse();
      } else {
         ArrayList var3 = new ArrayList();

         for (Listing var5 : this.browse()) {
            String var6 = this.searchIndex.get(var5.id);
            if (var6 == null) {
               ItemStack var7 = ItemUtil.decode(var5.item);
               var6 = (var7 == null ? "" : ItemUtil.describe(var7).toLowerCase(Locale.ROOT))
                  + "|"
                  + var5.shopName.toLowerCase(Locale.ROOT)
                  + "|"
                  + var5.ownerName.toLowerCase(Locale.ROOT);
               this.searchIndex.put(var5.id, var6);
            }

            if (var6.contains(var2)) {
               var3.add(var5);
            }
         }

         return var3;
      }
   }

   public List<Shop> topShops(int var1) {
      ArrayList<Shop> var2 = new ArrayList<>();

      for (Shop var4 : this.store.shops()) {
         if (var4 != null && var4.open) {
            var2.add(var4);
         }
      }

      var2.sort((var0, var1x) -> Integer.compare(var1x.sales, var0.sales));
      return var2.subList(0, Math.min(var1, var2.size()));
   }

   public List<Tx> recentTx(Player var1, int var2) {
      ArrayList var3 = new ArrayList();
      String var4 = var1.getUniqueId().toString();
      String var5 = var1.getName();

      for (Tx var7 : this.store.txs()) {
         boolean var8 = var4.equals(var7.buyerId) || var4.equals(var7.sellerId);
         boolean var9 = (var7.buyerId == null || var7.buyerId.isEmpty()) && (var7.sellerId == null || var7.sellerId.isEmpty());
         if (!var8 && var9) {
            var8 = var7.buyer != null && var7.buyer.equalsIgnoreCase(var5) || var7.seller != null && var7.seller.equalsIgnoreCase(var5);
         }

         if (var8) {
            var3.add(var7);
         }

         if (var3.size() >= var2) {
            break;
         }
      }

      return var3;
   }

   public static boolean txIsMine(Tx var0, Player var1, boolean var2) {
      if (var0 != null && var1 != null) {
         String var3 = var1.getUniqueId().toString();
         String var4 = var1.getName();
         String var5 = var2 ? var0.sellerId : var0.buyerId;
         String var6 = var2 ? var0.seller : var0.buyer;
         return var5 != null && !var5.isEmpty() ? var5.equals(var3) : var6 != null && var6.equalsIgnoreCase(var4);
      } else {
         return false;
      }
   }

   public int countSimilar(Player var1, ItemStack var2) {
      int var3 = 0;

      for (ItemStack var7 : var1.getInventory().getStorageContents()) {
         if (var7 != null && !var7.getType().isAir() && var7.isSimilar(var2)) {
            var3 += var7.getAmount();
         }
      }

      ItemStack var8 = var1.getInventory().getItemInOffHand();
      if (var8 != null && !var8.getType().isAir() && var8.isSimilar(var2)) {
         var3 += var8.getAmount();
      }

      return var3;
   }

   private boolean removeSimilar(Player var1, ItemStack var2, int var3) {
      PlayerInventory var4 = var1.getInventory();
      ItemStack[] var5 = var4.getStorageContents();
      int var6 = var3;

      for (int var7 = 0; var7 < var5.length && var6 > 0; var7++) {
         ItemStack var8 = var5[var7];
         if (var8 != null && !var8.getType().isAir() && var8.isSimilar(var2)) {
            int var9 = Math.min(var6, var8.getAmount());
            if (var9 >= var8.getAmount()) {
               var4.setItem(var7, null);
            } else {
               var8.setAmount(var8.getAmount() - var9);
            }

            var6 -= var9;
         }
      }

      if (var6 > 0) {
         ItemStack var10 = var4.getItemInOffHand();
         if (var10 != null && !var10.getType().isAir() && var10.isSimilar(var2)) {
            int var11 = Math.min(var6, var10.getAmount());
            if (var11 >= var10.getAmount()) {
               var4.setItemInOffHand(null);
            } else {
               var10.setAmount(var10.getAmount() - var11);
               var4.setItemInOffHand(var10);
            }

            var6 -= var11;
         }
      }

      return var6 == 0;
   }

   private void refund(Player var1, ItemStack var2, int var3) {
      int var4 = var3;

      while (var4 > 0) {
         int var5 = Math.min(var4, ItemUtil.maxStack(var2));
         ItemStack var6 = var2.clone();
         var6.setAmount(var5);
         HashMap<Integer, ItemStack> var7 = var1.getInventory().addItem(new ItemStack[]{var6});

         for (ItemStack var9 : var7.values()) {
            var1.getWorld().dropItemNaturally(var1.getLocation(), var9);
         }

         var4 -= var5;
      }
   }

   private ItemStack single(ItemStack var1) {
      ItemStack var2 = var1.clone();
      var2.setAmount(1);
      return var2;
   }

   public boolean isBlacklisted(Material var1) {
      return this.cfg.blacklisted(var1);
   }

   public static record Result(boolean ok, String message) {
      public static ShopService.Result ok(String var0) {
         return new ShopService.Result(true, var0);
      }

      public static ShopService.Result fail(String var0) {
         return new ShopService.Result(false, var0);
      }
   }
}
