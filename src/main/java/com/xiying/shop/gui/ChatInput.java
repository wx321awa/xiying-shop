package com.xiying.shop.gui;

import com.xiying.shop.XiYingShopPlugin;
import com.xiying.shop.model.Currency;
import com.xiying.shop.model.Listing;
import com.xiying.shop.service.ShopService;
import com.xiying.shop.util.ItemUtil;
import com.xiying.shop.util.Msg;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ChatInput {
   private final XiYingShopPlugin plugin;
   private ShopService service;
   private GuiManager gui;
   private static final long TIMEOUT_MS = 120000L;
   private final Map<UUID, ChatInput.Pending> pending = new ConcurrentHashMap<>();

   public ChatInput(XiYingShopPlugin var1, ShopService var2, GuiManager var3) {
      this.plugin = var1;
      this.service = var2;
      this.gui = var3;
   }

   public void rebind(ShopService var1, GuiManager var2) {
      this.service = var1;
      this.gui = var2;
   }

   public void startCreateShop(Player var1) {
      var1.closeInventory();
      ChatInput.Pending var2 = new ChatInput.Pending();
      var2.kind = ChatInput.Kind.CREATE_SHOP;
      this.arm(var1, var2);
      Msg.send(
         var1,
         "请在聊天栏输入店名（"
            + this.plugin.getPluginConfig().shopNameMin
            + "~"
            + this.plugin.getPluginConfig().shopNameMax
            + " 个字），输入 §eq §f取消。创建费用 §6"
            + Msg.money(this.plugin.getPluginConfig().createCost)
            + " 金币"
      );
   }

   public void startSearch(Player var1) {
      var1.closeInventory();
      ChatInput.Pending var2 = new ChatInput.Pending();
      var2.kind = ChatInput.Kind.SEARCH;
      this.arm(var1, var2);
      Msg.send(var1, "请在聊天栏输入要搜索的物品名 / 店名 / 卖家名，输入 §eq §f取消");
   }

   public void startListItem(Player var1, ItemStack var2, Currency var3) {
      var1.closeInventory();
      ChatInput.Pending var4 = new ChatInput.Pending();
      var4.kind = ChatInput.Kind.LIST_ITEM;
      var4.item = var2.clone();
      var4.currency = var3;
      this.arm(var1, var4);
      Msg.send(var1, "上架 §e" + ItemUtil.describe(var2) + "§f（" + var3.label() + "结算），请在聊天栏输入：§e单价 数量§f，例如 §e100 16§f，输入 §eq §f取消");
   }

   public void startEditPrice(Player var1, String var2) {
      var1.closeInventory();
      ChatInput.Pending var3 = new ChatInput.Pending();
      var3.kind = ChatInput.Kind.EDIT_PRICE;
      var3.listingId = var2;
      this.arm(var1, var3);
      Msg.send(var1, "请在聊天栏输入新的单价，输入 §eq §f取消");
   }

   private void arm(Player var1, ChatInput.Pending var2) {
      var2.deadline = System.currentTimeMillis() + 120000L;
      this.pending.put(var1.getUniqueId(), var2);
   }

   public boolean isWaiting(Player var1) {
      ChatInput.Pending var2 = this.pending.get(var1.getUniqueId());
      return var2 != null && System.currentTimeMillis() <= var2.deadline;
   }

   public boolean consumeExpired(Player var1) {
      ChatInput.Pending var2 = this.pending.get(var1.getUniqueId());
      if (var2 != null && System.currentTimeMillis() > var2.deadline) {
         this.pending.remove(var1.getUniqueId(), var2);
         return true;
      } else {
         return false;
      }
   }

   public void cancel(Player var1) {
      this.pending.remove(var1.getUniqueId());
   }

   public void handle(Player var1, String var2) {
      ChatInput.Pending var4 = this.pending.get(var1.getUniqueId());
      if (var4 != null) {
         if (System.currentTimeMillis() > var4.deadline) {
            this.pending.remove(var1.getUniqueId(), var4);
            Msg.send(var1, "§e输入等待已超时（2 分钟），本次操作已取消；你现在说的这句话不会被拦截");
         } else {
            String var3 = var2 == null ? "" : var2.trim();
            if (!var3.equalsIgnoreCase("q") && !var3.equalsIgnoreCase("cancel")) {
               switch (var4.kind) {
                  case CREATE_SHOP:
                     ShopService.Result var15 = this.service.createShop(var1, var3);
                     if (var15.ok()) {
                        this.pending.remove(var1.getUniqueId());
                     }

                     Msg.send(var1, var15.message());
                     if (var15.ok()) {
                        this.gui.openMyShop(var1);
                     }
                     break;
                  case SEARCH:
                     this.pending.remove(var1.getUniqueId());
                     ArrayList var14 = new ArrayList();

                     for (Listing var17 : this.service.search(var3)) {
                        var14.add(var17.id);
                     }

                     this.gui.openByIds(var1, var14, 0, "§8搜索：" + var3);
                     break;
                  case LIST_ITEM:
                     String[] var18 = var3.split("\\s+");
                     if (var18.length < 2) {
                        Msg.send(var1, "格式不对，应该是：§e单价 数量§f，例如 §e100 16§f（输入 q 取消）");
                        return;
                     }

                     double var7;
                     int var13;
                     try {
                        var7 = Double.parseDouble(var18[0]);
                        var13 = Integer.parseInt(var18[1]);
                     } catch (NumberFormatException var12) {
                        Msg.send(var1, "单价或数量不是有效数字（输入 q 取消）");
                        return;
                     }

                     this.pending.remove(var1.getUniqueId());
                     ShopService.Result var10 = this.service.listItem(var1, var4.item, var13, var7, var4.currency);
                     Msg.send(var1, var10.message());
                     if (var10.ok()) {
                        this.gui.openMyShop(var1);
                     }
                     break;
                  case EDIT_PRICE:
                     double var6;
                     try {
                        var6 = Double.parseDouble(var3);
                     } catch (NumberFormatException var11) {
                        Msg.send(var1, "单价不是有效数字（输入 q 取消）");
                        return;
                     }

                     this.pending.remove(var1.getUniqueId());
                     Listing var8 = this.plugin.getStore().listing(var4.listingId);
                     ShopService.Result var9 = this.service.changePrice(var1, var8, var6);
                     Msg.send(var1, var9.message());
                     if (var9.ok()) {
                        this.gui.openManage(var1, 0);
                     }
               }
            } else {
               this.pending.remove(var1.getUniqueId());
               Msg.send(var1, "已取消");
            }
         }
      }
   }

   private static enum Kind {
      CREATE_SHOP,
      SEARCH,
      LIST_ITEM,
      EDIT_PRICE;
   }

   private static class Pending {
      volatile long deadline;
      ChatInput.Kind kind;
      ItemStack item;
      Currency currency = Currency.MONEY;
      String listingId;
   }
}
