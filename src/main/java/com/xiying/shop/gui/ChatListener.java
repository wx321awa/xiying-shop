package com.xiying.shop.gui;

import com.xiying.shop.XiYingShopPlugin;
import com.xiying.shop.util.Msg;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {
    private final XiYingShopPlugin plugin;

    public ChatListener(XiYingShopPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        ChatInput input = plugin.getChatInput();
        if (!input.isWaiting(player)) return;
        String text = Msg.plain(event.message());
        event.setCancelled(true);
        Bukkit.getScheduler().runTask(plugin, () -> input.handle(player, text));
    }

    /**
     * 有些聊天/跨服插件会在更高优先级里把事件重新放行，这里兜底再取消一次，顺便把情况记进日志。
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onChatMonitor(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getChatInput().isWaiting(player)) return;
        if (!event.isCancelled()) {
            plugin.getLogger().warning("聊天输入被其他插件重新放行，已再次拦截：" + player.getName()
                    + " -> " + Msg.plain(event.message()));
            event.setCancelled(true);
        }
    }

    /**
     * 兼容仍走旧版聊天事件（AsyncPlayerChatEvent）广播的插件。
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onLegacyChat(AsyncPlayerChatEvent event) {
        if (!plugin.getChatInput().isWaiting(event.getPlayer())) return;
        event.setCancelled(true);
    }
}
