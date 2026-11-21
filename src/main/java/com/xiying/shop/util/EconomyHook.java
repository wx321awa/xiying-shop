package com.xiying.shop.util;

import com.xiying.shop.XiYingShopPlugin;
import com.xiying.shop.model.Currency;
import net.milkbowl.vault.economy.Economy;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.UUID;

public class EconomyHook {
    private final XiYingShopPlugin plugin;
    private Economy economy;
    private PlayerPointsAPI points;

    public EconomyHook(XiYingShopPlugin plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        economy = null;
        points = null;
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
                RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
                if (rsp != null) economy = rsp.getProvider();
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Vault 经济挂钩失败：" + t.getMessage());
        }
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("PlayerPoints")) {
                PlayerPoints pp = PlayerPoints.getInstance();
                if (pp != null) points = pp.getAPI();
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("PlayerPoints 挂钩失败：" + t.getMessage());
        }
        plugin.getLogger().info("经济挂钩：Vault=" + (economy != null ? economy.getName() : "无")
                + "，PlayerPoints=" + (points != null ? "已连接" : "无"));
    }

    public boolean moneyReady() {
        return economy != null;
    }

    public boolean pointsReady() {
        return points != null;
    }

    public double money(OfflinePlayer player) {
        return economy == null ? 0 : economy.getBalance(player);
    }

    public int points(UUID uuid) {
        return points == null ? 0 : Math.max(0, points.look(uuid));
    }

    public boolean take(Player player, Currency currency, double amount) {
        if (currency == Currency.POINTS) {
            if (points == null) return false;
            int cost = (int) Math.ceil(amount);
            if (cost <= 0) return true;
            return points.take(player.getUniqueId(), cost);
        }
        if (economy == null) return false;
        if (!economy.has(player, amount)) return false;
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean give(UUID uuid, String name, Currency currency, double amount) {
        if (currency == Currency.POINTS) {
            if (points == null) return false;
            int value = (int) Math.floor(amount);
            return value <= 0 || points.give(uuid, value);
        }
        if (economy == null) return false;
        OfflinePlayer off = Bukkit.getOfflinePlayer(uuid);
        return economy.depositPlayer(off, amount).transactionSuccess();
    }

    public boolean has(Player player, Currency currency, double amount) {
        if (currency == Currency.POINTS) {
            return points != null && points.look(player.getUniqueId()) >= (int) Math.ceil(amount);
        }
        return economy != null && economy.has(player, amount);
    }

    public String format(Currency currency, double amount) {
        if (currency == Currency.POINTS) {
            return ((int) Math.ceil(amount)) + " 曦荧币";
        }
        return Msg.money(amount) + " 金币";
    }
}
