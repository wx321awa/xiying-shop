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

    @Override
    public void onEnable() {
        saveDefaultConfig();
        pluginConfig = PluginConfig.load(this);
        ItemUtil.load(this);
        store = new ShopStore(this);
        store.load();
        econ = new EconomyHook(this);
        econ.setup();
        buildComponents();

        ShopCommand command = new ShopCommand(this);
        if (getCommand("pshop") != null) {
            getCommand("pshop").setExecutor(command);
            getCommand("pshop").setTabCompleter(command);
        }
        Bukkit.getPluginManager().registerEvents(new GuiListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(this), this);

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!econ.moneyReady()) {
                econ.setup();
                if (!econ.moneyReady()) {
                    getLogger().warning("Vault 经济仍未就绪，金币相关功能会提示不可用");
                }
            }
        }, 60L);

        getLogger().info("曦荧玩家商店已启用（Java GUI + 基岩表单）");
    }

    private void buildComponents() {
        service = new ShopService(this, store, econ, pluginConfig);
        gui = new GuiManager(this, service, store, econ, pluginConfig);
        chatInput = new ChatInput(this, service, gui);
        bedrockForms = new BedrockForms(this, service, store, econ, pluginConfig);
    }

    public void reloadAll() {
        pluginConfig = PluginConfig.load(this);
        ItemUtil.load(this);
        econ.setup();
        buildComponents();
    }

    @Override
    public void onDisable() {
        if (store != null) store.saveNow();
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public ShopStore getStore() {
        return store;
    }

    public EconomyHook getEcon() {
        return econ;
    }

    public ShopService getService() {
        return service;
    }

    public GuiManager getGui() {
        return gui;
    }

    public ChatInput getChatInput() {
        return chatInput;
    }

    public BedrockForms getBedrockForms() {
        return bedrockForms;
    }
}
