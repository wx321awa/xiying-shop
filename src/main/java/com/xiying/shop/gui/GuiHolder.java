package com.xiying.shop.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GuiHolder implements InventoryHolder {
    public final GuiType type;
    public int page;
    public String listingId;
    public String title = "";
    public UUID ownerFilter;
    public ItemStack pendingItem;
    public int pendingSlot = -1;
    public List<String> ids = new ArrayList<>();
    private Inventory inventory;

    public GuiHolder(GuiType type) {
        this.type = type;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
