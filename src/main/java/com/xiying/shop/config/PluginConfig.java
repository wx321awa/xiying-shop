package com.xiying.shop.config;

import com.xiying.shop.XiYingShopPlugin;
import com.xiying.shop.util.Msg;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    public static PluginConfig load(XiYingShopPlugin plugin) {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();
        PluginConfig cfg = new PluginConfig();
        cfg.prefix = c.getString("prefix", "§8[§b曦荧商店§8] §f");
        cfg.createCost = c.getDouble("create-cost", 50.0);
        cfg.taxMoney = clamp(c.getDouble("tax-money", 0.0), 0.0, 0.9);
        cfg.taxPoints = clamp(c.getDouble("tax-points", 0.0), 0.0, 0.9);
        cfg.maxListings = Math.max(1, c.getInt("max-listings", 27));
        cfg.maxAmountPerListing = Math.max(1, c.getInt("max-amount-per-listing", 2304));
        cfg.minUnitPrice = Math.max(0.01, c.getDouble("min-unit-price", 1.0));
        cfg.moneyEnabled = c.getBoolean("money-enabled", true);
        cfg.pointsEnabled = c.getBoolean("points-enabled", true);
        cfg.browsePageSize = Math.max(9, Math.min(45, c.getInt("browse-page-size", 45)));
        cfg.bedrockPageSize = Math.max(4, Math.min(20, c.getInt("bedrock-page-size", 9)));
        cfg.shopNameMin = Math.max(1, c.getInt("shop-name-min", 2));
        cfg.shopNameMax = Math.max(cfg.shopNameMin, c.getInt("shop-name-max", 12));
        List<String> list = c.getStringList("blacklist");
        for (String raw : list) {
            Material m = Material.matchMaterial(raw);
            if (m != null) cfg.blacklist.add(m);
        }
        Msg.setPrefix(cfg.prefix);
        return cfg;
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    public boolean blacklisted(Material material) {
        return blacklist.contains(material);
    }
}
