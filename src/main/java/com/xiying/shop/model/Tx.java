package com.xiying.shop.model;

public class Tx {
    public long time;
    public String buyer = "";
    public String seller = "";
    public String item = "";
    public int amount;
    public double total;
    public Currency currency = Currency.MONEY;

    public Tx() {
    }

    public Tx(String buyer, String seller, String item, int amount, double total, Currency currency) {
        this.time = System.currentTimeMillis();
        this.buyer = buyer;
        this.seller = seller;
        this.item = item;
        this.amount = amount;
        this.total = total;
        this.currency = currency;
    }
}
