package com.xiying.shop.gui;

import com.xiying.shop.XiYingShopPlugin;
import com.xiying.shop.util.Msg;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ChatListener implements Listener {
   private final XiYingShopPlugin plugin;
   private final Map<UUID, ChatListener.LastMsg> lastDeliver = new ConcurrentHashMap<>();

   public ChatListener(XiYingShopPlugin var1) {
      this.plugin = var1;
   }

   @EventHandler(
      priority = EventPriority.LOWEST,
      ignoreCancelled = false
   )
   public void onChat(AsyncChatEvent var1) {
      Player var2 = var1.getPlayer();
      ChatInput var3 = this.plugin.getChatInput();
      if (!var3.isWaiting(var2)) {
         this.notifyExpired(var2, var3);
      } else {
         String var4 = Msg.plain(var1.message());
         var1.setCancelled(true);
         this.deliver(var2, var4);
      }
   }

   private void notifyExpired(Player var1, ChatInput var2) {
      if (var2.consumeExpired(var1)) {
         Bukkit.getScheduler().runTask(this.plugin, () -> var1.sendMessage(Msg.c("§8[§b曦荧商店§8] §e商店输入已超时取消（2 分钟未完成），这句话会正常发出，需要时重新点一次按钮就好")));
      }
   }

   private void deliver(Player var1, String var2) {
      long var3 = System.currentTimeMillis();
      ChatListener.LastMsg var5 = this.lastDeliver.get(var1.getUniqueId());
      if (var5 == null || var3 - var5.time() >= 750L || !var5.text().equals(var2)) {
         this.lastDeliver.put(var1.getUniqueId(), new ChatListener.LastMsg(var2, var3));
         Bukkit.getScheduler().runTask(this.plugin, () -> this.plugin.getChatInput().handle(var1, var2));
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = false
   )
   public void onChatMonitor(AsyncChatEvent var1) {
      Player var2 = var1.getPlayer();
      if (this.plugin.getChatInput().isWaiting(var2)) {
         if (!var1.isCancelled()) {
            this.plugin.getLogger().warning("聊天输入被其他插件重新放行，已再次拦截：" + var2.getName() + " -> " + Msg.plain(var1.message()));
            var1.setCancelled(true);
         }
      }
   }

   @EventHandler(
      priority = EventPriority.LOWEST,
      ignoreCancelled = false
   )
   public void onLegacyChat(AsyncPlayerChatEvent var1) {
      if (this.plugin.getChatInput().isWaiting(var1.getPlayer())) {
         var1.setCancelled(true);
         this.deliver(var1.getPlayer(), var1.getMessage());
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR
   )
   public void onQuit(PlayerQuitEvent var1) {
      this.plugin.getChatInput().cancel(var1.getPlayer());
   }

   private static record LastMsg(String text, long time) {
   }
}
