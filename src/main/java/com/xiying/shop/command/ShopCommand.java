package com.xiying.shop.command;

import com.xiying.shop.XiYingShopPlugin;
import com.xiying.shop.util.Msg;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ShopCommand implements CommandExecutor, TabCompleter {
   private final XiYingShopPlugin plugin;

   public ShopCommand(XiYingShopPlugin var1) {
      this.plugin = var1;
   }

   public boolean onCommand(@NotNull CommandSender var1, @NotNull Command var2, @NotNull String var3, @NotNull String[] var4) {
      if (var4.length > 0 && var4[0].equalsIgnoreCase("admin")) {
         if (!var1.hasPermission("xiyingshop.admin")) {
            Msg.send(var1, "§c你没有权限");
            return true;
         } else if (var4.length > 1 && var4[1].equalsIgnoreCase("reload")) {
            this.plugin.reloadAll();
            Msg.send(var1, "配置已重载");
            return true;
         } else {
            Msg.send(var1, "用法：/pshop admin reload");
            return true;
         }
      } else if (!(var1 instanceof Player var5)) {
         Msg.send(var1, "这个命令只能玩家使用");
         return true;
      } else if (!var5.hasPermission("xiyingshop.use")) {
         Msg.send(var5, "§c你没有使用玩家商店的权限");
         return true;
      } else if (var4.length > 0 && var4[0].equalsIgnoreCase("delshop")) {
         Msg.send(var5, this.plugin.getService().deleteShop(var5, var4.length > 1 ? var4[1] : "").message());
         return true;
      } else if (var4.length > 0 && var4[0].equalsIgnoreCase("transfer")) {
         String var6 = var4.length > 1 ? var4[1] : "";
         String var7 = var4.length > 2 ? var4[2] : "";
         Msg.send(var5, this.plugin.getService().transferShop(var5, var6, var7).message());
         return true;
      } else {
         if (this.plugin.getBedrockForms().isBedrock(var5)) {
            this.plugin.getBedrockForms().openMain(var5);
         } else {
            this.plugin.getGui().openMain(var5);
         }

         return true;
      }
   }

   public List<String> onTabComplete(@NotNull CommandSender var1, @NotNull Command var2, @NotNull String var3, @NotNull String[] var4) {
      ArrayList var5 = new ArrayList();
      if (var4.length == 1) {
         String var6 = var4[0].toLowerCase(Locale.ROOT);

         for (String var8 : List.of("admin", "delshop", "transfer")) {
            if (var8.startsWith(var6)) {
               var5.add(var8);
            }
         }
      } else if (var4.length == 2 && var4[0].equalsIgnoreCase("admin") && "reload".startsWith(var4[1].toLowerCase(Locale.ROOT))) {
         var5.add("reload");
      } else if (var4.length == 2 && var4[0].equalsIgnoreCase("delshop") && var1 instanceof Player && this.plugin.getService().shopOf((Player)var1) != null) {
         String var11 = this.plugin.getService().shopOf((Player)var1).name;
         if (var11.toLowerCase(Locale.ROOT).startsWith(var4[1].toLowerCase(Locale.ROOT))) {
            var5.add(var11);
         }
      } else if (var4.length == 2 && var4[0].equalsIgnoreCase("transfer") && var1 instanceof Player && this.plugin.getService().shopOf((Player)var1) != null) {
         String var10 = this.plugin.getService().shopOf((Player)var1).name;
         if (var10.toLowerCase(Locale.ROOT).startsWith(var4[1].toLowerCase(Locale.ROOT))) {
            var5.add(var10);
         }
      } else if (var4.length == 3 && var4[0].equalsIgnoreCase("transfer")) {
         String var9 = var4[2].toLowerCase(Locale.ROOT);

         for (Player var13 : Bukkit.getOnlinePlayers()) {
            if (var13.getName().toLowerCase(Locale.ROOT).startsWith(var9)) {
               var5.add(var13.getName());
            }
         }
      }

      return var5;
   }
}
