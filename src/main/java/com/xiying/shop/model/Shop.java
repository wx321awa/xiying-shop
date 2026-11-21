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

   public Shop(UUID var1, String var2, String var3, String var4) {
      this.owner = var1;
      this.ownerName = var2;
      this.name = var3;
      this.icon = var4;
      this.createdAt = System.currentTimeMillis();
   }
}
