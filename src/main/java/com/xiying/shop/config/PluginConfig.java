package com.xiying.shop.config;

import com.xiying.shop.XiYingShopPlugin;
import com.xiying.shop.util.Msg;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

public class PluginConfig {
   public String prefix;
   public double createCost;
   public double taxMoney;
   public double taxPoints;
   public int maxListings;
   public int maxAmountPerListing;
   public double minUnitPrice;
   public boolean moneyEnabled;
   public boolean pointsEnabled;
   public int browsePageSize;
   public int bedrockPageSize;
   public int shopNameMin;
   public int shopNameMax;
   public final Set<Material> blacklist = new HashSet<>();

   public static PluginConfig load(XiYingShopPlugin var0) {
      var0.saveDefaultConfig();
      var0.reloadConfig();
      FileConfiguration var1 = var0.getConfig();
      PluginConfig var2 = new PluginConfig();
      var2.prefix = var1.getString("prefix", "§8[§b曦荧商店§8] §f");
      var2.createCost = var1.getDouble("create-cost", 50.0);
      var2.taxMoney = clamp(var1.getDouble("tax-money", 0.0), 0.0, 0.9);
      var2.taxPoints = clamp(var1.getDouble("tax-points", 0.0), 0.0, 0.9);
      var2.maxListings = Math.max(1, var1.getInt("max-listings", 27));
      var2.maxAmountPerListing = Math.max(1, var1.getInt("max-amount-per-listing", 2304));
      var2.minUnitPrice = Math.max(0.01, var1.getDouble("min-unit-price", 1.0));
      var2.moneyEnabled = var1.getBoolean("money-enabled", true);
      var2.pointsEnabled = var1.getBoolean("points-enabled", true);
      var2.browsePageSize = Math.max(9, Math.min(45, var1.getInt("browse-page-size", 45)));
      var2.bedrockPageSize = Math.max(4, Math.min(20, var1.getInt("bedrock-page-size", 9)));
      var2.shopNameMin = Math.max(1, var1.getInt("shop-name-min", 2));
      var2.shopNameMax = Math.max(var2.shopNameMin, var1.getInt("shop-name-max", 12));

      for (String var5 : var1.getStringList("blacklist")) {
         Material var6 = Material.matchMaterial(var5);
         if (var6 != null) {
            var2.blacklist.add(var6);
         }
      }

      Msg.setPrefix(var2.prefix);
      return var2;
   }

   private static double clamp(double var0, double var2, double var4) {
      return Math.max(var2, Math.min(var4, var0));
   }

   public boolean blacklisted(Material var1) {
      return this.blacklist.contains(var1);
   }
}
