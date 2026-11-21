package com.xiying.shop.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;

public final class Msg {
   public static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
   private static String prefix = "§8[§b曦荧商店§8] §f";

   private Msg() {
   }

   public static void setPrefix(String var0) {
      if (var0 != null && !var0.isEmpty()) {
         prefix = var0;
      }
   }

   public static Component c(String var0) {
      return LEGACY.deserialize(var0 == null ? "" : var0);
   }

   public static String plain(Component var0) {
      return PlainTextComponentSerializer.plainText().serialize(var0);
   }

   public static void send(CommandSender var0, String var1) {
      var0.sendMessage(c(prefix + var1));
   }

   public static void raw(CommandSender var0, String var1) {
      var0.sendMessage(c(var1));
   }

   public static String money(double var0) {
      return String.format("%.2f", var0);
   }

   public static String intFmt(int var0) {
      return String.valueOf(var0);
   }
}
