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

    public static void setPrefix(String raw) {
        if (raw != null && !raw.isEmpty()) prefix = raw;
    }

    public static Component c(String text) {
        return LEGACY.deserialize(text == null ? "" : text);
    }

    public static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    public static void send(CommandSender sender, String text) {
        sender.sendMessage(c(prefix + text));
    }

    public static void raw(CommandSender sender, String text) {
        sender.sendMessage(c(text));
    }

    public static String money(double value) {
        return String.format("%.2f", value);
    }

    public static String intFmt(int value) {
        return String.valueOf(value);
    }
}
