package com.xiying.shop.model;

public class Tx {
   public long time;
   public String buyerId = "";
   public String sellerId = "";
   public String buyer = "";
   public String seller = "";
   public String item = "";
   public int amount;
   public double total;
   public Currency currency = Currency.MONEY;

   public Tx() {
   }

   public Tx(String var1, String var2, String var3, String var4, String var5, int var6, double var7, Currency var9) {
      this.time = System.currentTimeMillis();
      this.buyerId = var1 == null ? "" : var1;
      this.sellerId = var3 == null ? "" : var3;
      this.buyer = var2 == null ? "" : var2;
      this.seller = var4 == null ? "" : var4;
      this.item = var5;
      this.amount = var6;
      this.total = var7;
      this.currency = var9;
   }
}
