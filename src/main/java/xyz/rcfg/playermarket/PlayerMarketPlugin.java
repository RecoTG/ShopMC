package xyz.rcfg.playermarket;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.rcfg.playermarket.commands.PMAdminCommand;
import xyz.rcfg.playermarket.gui.MenuListener;
import xyz.rcfg.playermarket.service.ShopService;

public class PlayerMarketPlugin extends JavaPlugin {
    private static PlayerMarketPlugin instance;
    private Economy economy;
    private ShopService shop;
    public static PlayerMarketPlugin getInstance(){ return instance; }
    public Economy economy(){ return economy; }
    public ShopService shop(){ return shop; }
    @Override public void onEnable() {
        instance = this;
        saveDefaultConfig();
        if (!setupEconomy()) getLogger().warning("Vault economy not found. Buying/selling will fail.");
        shop = new ShopService(this);
        Bukkit.getPluginManager().registerEvents(new MenuListener(shop), this);
        getCommand("shop").setExecutor((sender, cmd, label, args) -> {
            if (!(sender instanceof org.bukkit.entity.Player p)) { sender.sendMessage("Players only."); return true; }
            shop.openRoot(p); return true; });
        getCommand("sellall").setExecutor((sender, cmd, label, args) -> {
            if (!(sender instanceof org.bukkit.entity.Player p)) { sender.sendMessage("Players only."); return true; }
            shop.openSellAll(p); return true; });
        getCommand("pm").setExecutor(new PMAdminCommand(this));
    }
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }
}
