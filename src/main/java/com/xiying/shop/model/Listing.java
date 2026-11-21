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

   public Listing(String var1, UUID var2, String var3, String var4, String var5, int var6, double var7, Currency var9) {
      this.id = var1;
      this.owner = var2;
      this.ownerName = var3;
      this.shopName = var4;
      this.item = var5;
      this.amount = var6;
      this.unitPrice = var7;
      this.currency = var9;
      this.createdAt = System.currentTimeMillis();
   }
}
