package com.xiying.shop.model;

import java.util.UUID;

public class Shop {
    public UUID owner;
    public String ownerName = "";
    public String name = "";
    public String icon = "CHEST";
    public boolean open = true;
    public double pendingMoney = 0.0;
    public int pendingPoints = 0;
    public int sales = 0;
    public long createdAt = 0L;

    public Shop() {
    }

    public Shop(UUID owner, String ownerName, String name, String icon) {
        this.owner = owner;
        this.ownerName = ownerName;
        this.name = name;
        this.icon = icon;
        this.createdAt = System.currentTimeMillis();
    }
}
