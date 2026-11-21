package com.xiying.shop.model;

public enum Currency {
   MONEY("金币"),
   POINTS("曦荧币");

   private final String label;

   private Currency(String nullxx) {
      this.label = nullxx;
   }

   public String label() {
      return this.label;
   }

   public static Currency parse(String var0) {
      if (var0 == null) {
         return MONEY;
      } else {
         return var0.equalsIgnoreCase("POINTS") ? POINTS : MONEY;
      }
   }
}
