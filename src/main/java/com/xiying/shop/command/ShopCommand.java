package com.xiying.shop.command;

import com.xiying.shop.XiYingShopPlugin;
import com.xiying.shop.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ShopCommand implements CommandExecutor, TabCompleter {
    private final XiYingShopPlugin plugin;

    public ShopCommand(XiYingShopPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("admin")) {
            if (!sender.hasPermission("xiyingshop.admin")) {
                Msg.send(sender, "§c你没有权限");
                return true;
            }
            if (args.length > 1 && args[1].equalsIgnoreCase("reload")) {
                plugin.reloadAll();
                Msg.send(sender, "配置已重载");
                return true;
            }
            Msg.send(sender, "用法：/pshop admin reload");
            return true;
        }
        if (!(sender instanceof Player player)) {
            Msg.send(sender, "这个命令只能玩家使用");
            return true;
        }
        if (!player.hasPermission("xiyingshop.use")) {
            Msg.send(player, "§c你没有使用玩家商店的权限");
            return true;
        }
        if (plugin.getBedrockForms().isBedrock(player)) {
            plugin.getBedrockForms().openMain(player);
        } else {
            plugin.getGui().openMain(player);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (String option : List.of("admin")) {
                if (option.startsWith(prefix)) out.add(option);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            if ("reload".startsWith(args[1].toLowerCase(Locale.ROOT))) out.add("reload");
        }
        return out;
    }
}
