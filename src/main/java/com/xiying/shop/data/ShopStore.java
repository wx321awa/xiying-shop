package com.xiying.shop.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.xiying.shop.XiYingShopPlugin;
import com.xiying.shop.model.Currency;
import com.xiying.shop.model.Listing;
import com.xiying.shop.model.Shop;
import com.xiying.shop.model.Tx;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ShopStore {
   private final XiYingShopPlugin plugin;
   private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
   private final File file;
   private final Map<UUID, Shop> shops = new ConcurrentHashMap<>();
   private final Map<String, Listing> listings = new LinkedHashMap<>();
   private final List<Tx> txs = new ArrayList<>();
   private final Object writeLock = new Object();
   private final AtomicBoolean saveScheduled = new AtomicBoolean(false);
   private final ExecutorService io = Executors.newSingleThreadExecutor(var0 -> {
      Thread var1x = new Thread(var0, "XiYingShop-IO");
      var1x.setDaemon(true);
      return var1x;
   });

   public ShopStore(XiYingShopPlugin var1) {
      this.plugin = var1;
      this.file = new File(var1.getDataFolder(), "data.json");
   }

   public synchronized void load() {
      this.shops.clear();
      this.listings.clear();
      this.txs.clear();
      if (this.file.exists()) {
         try {
            String var1 = Files.readString(this.file.toPath(), StandardCharsets.UTF_8);
            ShopStore.Data var7 = (ShopStore.Data)this.gson.fromJson(var1, ShopStore.Data.class);
            if (var7 == null) {
               return;
            }

            if (var7.shops != null) {
               for (Shop var4 : var7.shops) {
                  if (var4 != null && var4.owner != null) {
                     this.shops.put(var4.owner, var4);
                  }
               }
            }

            if (var7.listings != null) {
               for (Listing var9 : var7.listings) {
                  if (var9 != null && var9.id != null && var9.owner != null) {
                     if (var9.currency == null) {
                        var9.currency = Currency.MONEY;
                     }

                     this.listings.put(var9.id, var9);
                  }
               }
            }

            if (var7.txs != null) {
               this.txs.addAll(var7.txs);
            }

            this.plugin.getLogger().info("已载入 " + this.shops.size() + " 家店铺、" + this.listings.size() + " 条商品");
         } catch (Exception var6) {
            this.plugin.getLogger().warning("读取 data.json 失败：" + var6.getMessage());
            File var2 = new File(this.plugin.getDataFolder(), "data.json.broken");

            try {
               Files.copy(this.file.toPath(), var2.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception var5) {
            }
         }
      }
   }

   private ShopStore.Data snapshot() {
      ShopStore.Data var1 = new ShopStore.Data();
      var1.shops = new ArrayList<>(this.shops.values());
      var1.listings = new ArrayList<>(this.listings.values());
      var1.txs = new ArrayList<>(this.txs);
      return var1;
   }

   public void saveAsync() {
      if (this.saveScheduled.compareAndSet(false, true)) {
         this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            this.saveScheduled.set(false);
            String var1 = this.gson.toJson(this.snapshot());

            try {
               this.io.submit(() -> this.write(var1));
            } catch (Exception var3) {
               this.plugin.getLogger().warning("安排异步写盘失败：" + var3.getMessage());
            }
         }, 20L);
      }
   }

   public boolean saveNow() {
      String var1;
      try {
         var1 = this.gson.toJson(this.snapshot());
      } catch (Exception var4) {
         this.plugin.getLogger().warning("序列化 data.json 失败：" + var4.getMessage());
         return false;
      }

      try {
         Future var2 = this.io.submit(() -> this.write(var1));
         Boolean var3 = (Boolean)var2.get(5L, TimeUnit.SECONDS);
         return var3 != null && var3;
      } catch (Exception var5) {
         this.plugin.getLogger().warning("等待 data.json 写盘失败：" + var5.getMessage());
         return false;
      }
   }

   public void shutdown() {
      try {
         this.saveNow();
      } finally {
         this.io.shutdown();

         try {
            this.io.awaitTermination(5L, TimeUnit.SECONDS);
         } catch (InterruptedException var7) {
            Thread.currentThread().interrupt();
         }
      }
   }

   private boolean write(String var1) {
      Object var2 = this.writeLock;
      synchronized (var2) {
         boolean var10000;
         try {
            if (!this.file.getParentFile().exists() && !this.file.getParentFile().mkdirs()) {
               this.plugin.getLogger().warning("无法创建数据目录");
               return false;
            }

            File var4 = new File(this.file.getParentFile(), "data.json.tmp");
            Files.writeString(var4.toPath(), var1, StandardCharsets.UTF_8);

            try {
               Files.move(var4.toPath(), this.file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception var7) {
               Files.move(var4.toPath(), this.file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            var10000 = true;
         } catch (Exception var8) {
            this.plugin.getLogger().warning("保存 data.json 失败：" + var8.getMessage());
            return false;
         }

         return var10000;
      }
   }

   public Shop shopOf(UUID var1) {
      return var1 == null ? null : this.shops.get(var1);
   }

   public Shop shopByName(String var1) {
      if (var1 == null) {
         return null;
      } else {
         for (Shop var3 : this.shops.values()) {
            if (var3.name != null && var3.name.equalsIgnoreCase(var1)) {
               return var3;
            }
         }

         return null;
      }
   }

   public List<Shop> shops() {
      return new ArrayList<>(this.shops.values());
   }

   public void putShop(Shop var1) {
      this.shops.put(var1.owner, var1);
   }

   public void removeShop(UUID var1) {
      this.shops.remove(var1);
   }

   public Listing listing(String var1) {
      return var1 == null ? null : this.listings.get(var1);
   }

   public List<Listing> listings() {
      return new ArrayList<>(this.listings.values());
   }

   public List<Listing> listingsOf(UUID var1) {
      ArrayList<Listing> var2 = new ArrayList<>();

      for (Listing var4 : this.listings.values()) {
         if (var4.owner != null && var4.owner.equals(var1)) {
            var2.add(var4);
         }
      }

      var2.sort((var0, var1x) -> Long.compare(var1x.createdAt, var0.createdAt));
      return var2;
   }

   public void putListing(Listing var1) {
      this.listings.put(var1.id, var1);
   }

   public void removeListing(String var1) {
      this.listings.remove(var1);
   }

   public void addTx(Tx var1) {
      this.txs.add(0, var1);

      while (this.txs.size() > 300) {
         this.txs.remove(this.txs.size() - 1);
      }
   }

   public void removeTx(Tx var1) {
      this.txs.remove(var1);
   }

   public List<Tx> txs() {
      return new ArrayList<>(this.txs);
   }

   public static class Data {
      public List<Shop> shops = new ArrayList<>();
      public List<Listing> listings = new ArrayList<>();
      public List<Tx> txs = new ArrayList<>();
   }
}
