package com.xiying.shop.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class GuiHolder implements InventoryHolder {
   public final GuiType type;
   public int page;
   public int perPage = 45;
   public String listingId;
   public String title = "";
   public UUID ownerFilter;
   public ItemStack pendingItem;
   public int pendingSlot = -1;
   public List<String> ids = new ArrayList<>();
   private Inventory inventory;

   public GuiHolder(GuiType var1) {
      this.type = var1;
   }

   public Inventory getInventory() {
      return this.inventory;
   }

   public void setInventory(Inventory var1) {
      this.inventory = var1;
   }
}
