package com.xiying.shop.model;

import java.util.UUID;

public class Listing {
    public String id;
    public UUID owner;
    public String ownerName = "";
    public String shopName = "";
    public String item = "";
    public int amount = 0;
    public double unitPrice = 0.0;
    public Currency currency = Currency.MONEY;
    public long createdAt = 0L;

    public Listing() {
    }

    public Listing(String id, UUID owner, String ownerName, String shopName, String item,
                   int amount, double unitPrice, Currency currency) {
        this.id = id;
        this.owner = owner;
        this.ownerName = ownerName;
        this.shopName = shopName;
        this.item = item;
        this.amount = amount;
        this.unitPrice = unitPrice;
        this.currency = currency;
        this.createdAt = System.currentTimeMillis();
    }
}
