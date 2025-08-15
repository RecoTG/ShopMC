package com.yourorg.servershop;

import com.yourorg.servershop.commands.*;
import com.yourorg.servershop.gui.MenuManager;
import com.yourorg.servershop.logging.*;
import com.yourorg.servershop.shop.*;
import com.yourorg.servershop.weekly.*;
import com.yourorg.servershop.dynamic.*;
import com.yourorg.servershop.config.*;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class ServerShopPlugin extends JavaPlugin {
    private Economy economy;
    private Catalog catalog;
    private LoggerManager logger;
    private WeeklyShopManager weekly;
    private MenuManager menus;
    private ShopService shopService;
    private DynamicPricingManager dynamic;
    private CategorySettings categorySettings;

    @Override public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);
        saveResource("shop.yml", false);
        if (!setupEconomy()) {
            getLogger().severe("Vault economy not found. Disabling.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        this.categorySettings = new CategorySettings(this);
        this.catalog = new Catalog(this); catalog.reload();
        this.weekly = new WeeklyShopManager(this);
        this.logger = new LoggerManager(this);
        this.dynamic = new DynamicPricingManager(this);
        this.shopService = new ShopService(this);
        this.menus = new MenuManager(this);
        Bukkit.getPluginManager().registerEvents(menus, this);

        int saveEvery = Math.max(1, getConfig().getInt("dynamicPricing.decay.saveEveryMinutes", 5));
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, dynamic::tickSaveAll, 20L * 60L * saveEvery, 20L * 60L * saveEvery);

        getCommand("shop").setExecutor(new ShopCommand(this));
        getCommand("sell").setExecutor(new SellCommand(this));
        getCommand("sellall").setExecutor(new SellAllCommand(this));
        getCommand("shoplog").setExecutor(new ShopLogCommand(this));
        getCommand("weeklyshop").setExecutor(new WeeklyShopCommand(this));
        getLogger().info("DynamicServerShop enabled (Importer + Admin + Category multipliers + Fuzzy Search).");
    }

    @Override public void onDisable() {
        if (logger != null) logger.close();
        if (dynamic != null) dynamic.close();
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    public String prefixed(String msg) {
        String raw = getConfig().getString("messages.prefix", "&6[Shop] &7");
        String prefix = ChatColor.translateAlternateColorCodes('&', raw);
        return prefix + msg;
    }

    public Economy economy() { return economy; }
    public Catalog catalog() { return catalog; }
    public LoggerManager logger() { return logger; }
    public WeeklyShopManager weekly() { return weekly; }
    public MenuManager menus() { return menus; }
    public ShopService shop() { return shopService; }
    public DynamicPricingManager dynamic() { return dynamic; }
    public CategorySettings categorySettings() { return categorySettings; }
}
