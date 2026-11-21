package com.xiying.shop.gui;

import com.xiying.shop.XiYingShopPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class GuiListener implements Listener {
   private final XiYingShopPlugin plugin;

   public GuiListener(XiYingShopPlugin var1) {
      this.plugin = var1;
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onClick(InventoryClickEvent var1) {
      if (var1.getWhoClicked() instanceof Player var3) {
         if (var1.getView().getTopInventory().getHolder() instanceof GuiHolder var5) {
            var1.setCancelled(true);
            if (var1.getClickedInventory() != null) {
               int var6 = var1.getRawSlot();
               if (var1.getClickedInventory().equals(var1.getView().getTopInventory())) {
                  this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
                     if (var3.isOnline()) {
                        this.plugin.getGui().handleClick(var3, var5, var6);
                     }
                  });
               } else if (var5.type == GuiType.LIST_ITEM) {
                  int var7 = var1.getRawSlot();
                  this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
                     if (var3.isOnline()) {
                        this.plugin.getGui().handlePlayerInvClick(var3, var5, var7);
                     }
                  });
               }
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onDrag(InventoryDragEvent var1) {
      if (var1.getView().getTopInventory().getHolder() instanceof GuiHolder) {
         var1.setCancelled(true);
      }
   }
}
