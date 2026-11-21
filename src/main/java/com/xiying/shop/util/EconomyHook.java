package com.xiying.shop.util;

import com.xiying.shop.XiYingShopPlugin;
import com.xiying.shop.model.Currency;
import java.util.UUID;
import net.milkbowl.vault.economy.Economy;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyHook {
   private final XiYingShopPlugin plugin;
   private Economy economy;
   private PlayerPointsAPI points;

   public EconomyHook(XiYingShopPlugin var1) {
      this.plugin = var1;
   }

   public void setup() {
      this.economy = null;
      this.points = null;

      try {
         RegisteredServiceProvider var1;
         if (Bukkit.getPluginManager().isPluginEnabled("Vault") && (var1 = Bukkit.getServicesManager().getRegistration(Economy.class)) != null) {
            this.economy = (Economy)var1.getProvider();
         }
      } catch (Throwable var3) {
         this.plugin.getLogger().warning("Vault 经济挂钩失败：" + var3.getMessage());
      }

      try {
         PlayerPoints var4;
         if (Bukkit.getPluginManager().isPluginEnabled("PlayerPoints") && (var4 = PlayerPoints.getInstance()) != null) {
            this.points = var4.getAPI();
         }
      } catch (Throwable var2) {
         this.plugin.getLogger().warning("PlayerPoints 挂钩失败：" + var2.getMessage());
      }

      this.plugin
         .getLogger()
         .info("经济挂钩：Vault=" + (this.economy != null ? this.economy.getName() : "无") + "，PlayerPoints=" + (this.points != null ? "已连接" : "无"));
   }

   public boolean moneyReady() {
      return this.economy != null;
   }

   public boolean pointsReady() {
      return this.points != null;
   }

   public double money(OfflinePlayer var1) {
      return this.economy == null ? 0.0 : this.economy.getBalance(var1);
   }

   public int points(UUID var1) {
      return this.points == null ? 0 : Math.max(0, this.points.look(var1));
   }

   public boolean take(Player var1, Currency var2, double var3) {
      if (var2 == Currency.POINTS) {
         if (this.points == null) {
            return false;
         } else {
            int var5 = (int)Math.ceil(var3);
            return var5 <= 0 ? true : this.points.take(var1.getUniqueId(), var5);
         }
      } else if (this.economy == null) {
         return false;
      } else {
         return !this.economy.has(var1, var3) ? false : this.economy.withdrawPlayer(var1, var3).transactionSuccess();
      }
   }

   public boolean give(UUID var1, String var2, Currency var3, double var4) {
      if (var3 == Currency.POINTS) {
         if (this.points == null) {
            return false;
         } else {
            int var7 = (int)Math.floor(var4);
            return var7 <= 0 || this.points.give(var1, var7);
         }
      } else if (this.economy == null) {
         return false;
      } else {
         OfflinePlayer var6 = Bukkit.getOfflinePlayer(var1);
         return this.economy.depositPlayer(var6, var4).transactionSuccess();
      }
   }

   public boolean has(Player var1, Currency var2, double var3) {
      return var2 == Currency.POINTS
         ? this.points != null && this.points.look(var1.getUniqueId()) >= (int)Math.ceil(var3)
         : this.economy != null && this.economy.has(var1, var3);
   }

   public String format(Currency var1, double var2) {
      return var1 == Currency.POINTS ? (int)Math.ceil(var2) + " 曦荧币" : Msg.money(var2) + " 金币";
   }
}
