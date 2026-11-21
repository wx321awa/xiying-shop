package com.xiying.shop;

import com.xiying.shop.bedrock.BedrockForms;
import com.xiying.shop.command.ShopCommand;
import com.xiying.shop.config.PluginConfig;
import com.xiying.shop.data.ShopStore;
import com.xiying.shop.gui.ChatInput;
import com.xiying.shop.gui.ChatListener;
import com.xiying.shop.gui.GuiListener;
import com.xiying.shop.gui.GuiManager;
import com.xiying.shop.service.ShopService;
import com.xiying.shop.util.EconomyHook;
import com.xiying.shop.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class XiYingShopPlugin extends JavaPlugin {
   private PluginConfig pluginConfig;
   private ShopStore store;
   private EconomyHook econ;
   private ShopService service;
   private GuiManager gui;
   private ChatInput chatInput;
   private BedrockForms bedrockForms;

   public void onEnable() {
      this.saveDefaultConfig();
      this.pluginConfig = PluginConfig.load(this);
      ItemUtil.load(this);
      this.store = new ShopStore(this);
      this.store.load();
      this.econ = new EconomyHook(this);
      this.econ.setup();
      this.buildComponents();
      ShopCommand var1 = new ShopCommand(this);
      if (this.getCommand("pshop") != null) {
         this.getCommand("pshop").setExecutor(var1);
         this.getCommand("pshop").setTabCompleter(var1);
      }

      Bukkit.getPluginManager().registerEvents(new GuiListener(this), this);
      Bukkit.getPluginManager().registerEvents(new ChatListener(this), this);
      Bukkit.getScheduler().runTaskLater(this, () -> {
         if (!this.econ.moneyReady()) {
            this.econ.setup();
            if (!this.econ.moneyReady()) {
               this.getLogger().warning("Vault 经济仍未就绪，金币相关功能会提示不可用");
            }
         }
      }, 60L);
      this.getLogger().info("曦荧玩家商店已启用（Java GUI + 基岩表单）");
   }

   private void buildComponents() {
      this.service = new ShopService(this, this.store, this.econ, this.pluginConfig);
      this.gui = new GuiManager(this, this.service, this.store, this.econ, this.pluginConfig);
      if (this.chatInput == null) {
         this.chatInput = new ChatInput(this, this.service, this.gui);
      } else {
         this.chatInput.rebind(this.service, this.gui);
      }

      this.bedrockForms = new BedrockForms(this, this.service, this.store, this.econ, this.pluginConfig);
   }

   public void reloadAll() {
      this.pluginConfig = PluginConfig.load(this);
      ItemUtil.load(this);
      this.econ.setup();
      this.buildComponents();
   }

   public void onDisable() {
      if (this.store != null) {
         this.store.shutdown();
      }
   }

   public PluginConfig getPluginConfig() {
      return this.pluginConfig;
   }

   public ShopStore getStore() {
      return this.store;
   }

   public EconomyHook getEcon() {
      return this.econ;
   }

   public ShopService getService() {
      return this.service;
   }

   public GuiManager getGui() {
      return this.gui;
   }

   public ChatInput getChatInput() {
      return this.chatInput;
   }

   public BedrockForms getBedrockForms() {
      return this.bedrockForms;
   }
}
