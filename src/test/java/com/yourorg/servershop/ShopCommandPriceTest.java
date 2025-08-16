package com.yourorg.servershop;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import be.seeseemelk.mockbukkit.plugin.MockPlugin;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.ServicePriority;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ShopCommandPriceTest {
    private ServerMock server;
    private ServerShopPlugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        MockPlugin vault = MockBukkit.createMockPlugin("Vault");
        server.getServicesManager().register(Economy.class, new DummyEconomy(), vault, ServicePriority.Normal);
        plugin = MockBukkit.load(ServerShopPlugin.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void priceBuyMatchesConfig() {
        double price = plugin.shop().priceBuy(Material.DIAMOND);
        Assertions.assertEquals(160.0, price);
    }

    @Test
    void priceCommandShowsValue() {
        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin).setPermission("servershop.use", true);
        player.performCommand("shop price DIAMOND");
        String msg = player.nextMessage();
        Assertions.assertEquals("[Shop] DIAMOND: $160.00", ChatColor.stripColor(msg));
    }

    static class DummyEconomy implements Economy {
        private final Map<String, Double> balances = new HashMap<>();

        private double get(String name) { return balances.getOrDefault(name, 0.0); }
        private void set(String name, double amount) { balances.put(name, amount); }

        @Override public boolean isEnabled() { return true; }
        @Override public String getName() { return "Dummy"; }
        @Override public boolean hasBankSupport() { return false; }
        @Override public int fractionalDigits() { return 2; }
        @Override public String format(double amount) { return String.format("$%.2f", amount); }
        @Override public String currencyNamePlural() { return "Dollars"; }
        @Override public String currencyNameSingular() { return "Dollar"; }
        @Override public boolean hasAccount(String playerName) { return true; }
        @Override public boolean hasAccount(OfflinePlayer player) { return true; }
        @Override public boolean hasAccount(String playerName, String worldName) { return true; }
        @Override public boolean hasAccount(OfflinePlayer player, String worldName) { return true; }
        @Override public double getBalance(String playerName) { return get(playerName); }
        @Override public double getBalance(OfflinePlayer player) { return get(player.getName()); }
        @Override public double getBalance(String playerName, String world) { return get(playerName); }
        @Override public double getBalance(OfflinePlayer player, String world) { return get(player.getName()); }
        @Override public boolean has(String playerName, double amount) { return get(playerName) >= amount; }
        @Override public boolean has(OfflinePlayer player, double amount) { return has(player.getName(), amount); }
        @Override public boolean has(String playerName, String worldName, double amount) { return has(playerName, amount); }
        @Override public boolean has(OfflinePlayer player, String worldName, double amount) { return has(player.getName(), amount); }
        @Override public EconomyResponse withdrawPlayer(String playerName, double amount) { set(playerName, get(playerName) - amount); return new EconomyResponse(amount, get(playerName), EconomyResponse.ResponseType.SUCCESS, ""); }
        @Override public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) { return withdrawPlayer(player.getName(), amount); }
        @Override public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) { return withdrawPlayer(playerName, amount); }
        @Override public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) { return withdrawPlayer(player.getName(), amount); }
        @Override public EconomyResponse depositPlayer(String playerName, double amount) { set(playerName, get(playerName) + amount); return new EconomyResponse(amount, get(playerName), EconomyResponse.ResponseType.SUCCESS, ""); }
        @Override public EconomyResponse depositPlayer(OfflinePlayer player, double amount) { return depositPlayer(player.getName(), amount); }
        @Override public EconomyResponse depositPlayer(String playerName, String worldName, double amount) { return depositPlayer(playerName, amount); }
        @Override public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) { return depositPlayer(player.getName(), amount); }
        @Override public EconomyResponse createBank(String name, String player) { return new EconomyResponse(0,0,EconomyResponse.ResponseType.NOT_IMPLEMENTED,null); }
        @Override public EconomyResponse createBank(String name, OfflinePlayer player) { return new EconomyResponse(0,0,EconomyResponse.ResponseType.NOT_IMPLEMENTED,null); }
        @Override public EconomyResponse deleteBank(String name) { return new EconomyResponse(0,0,EconomyResponse.ResponseType.NOT_IMPLEMENTED,null); }
        @Override public EconomyResponse bankBalance(String name) { return new EconomyResponse(0,0,EconomyResponse.ResponseType.NOT_IMPLEMENTED,null); }
        @Override public EconomyResponse bankHas(String name, double amount) { return new EconomyResponse(0,0,EconomyResponse.ResponseType.NOT_IMPLEMENTED,null); }
        @Override public EconomyResponse bankWithdraw(String name, double amount) { return new EconomyResponse(0,0,EconomyResponse.ResponseType.NOT_IMPLEMENTED,null); }
        @Override public EconomyResponse bankDeposit(String name, double amount) { return new EconomyResponse(0,0,EconomyResponse.ResponseType.NOT_IMPLEMENTED,null); }
        @Override public boolean isBankOwner(String name, String playerName) { return false; }
        @Override public boolean isBankOwner(String name, OfflinePlayer player) { return false; }
        @Override public boolean isBankMember(String name, String playerName) { return false; }
        @Override public boolean isBankMember(String name, OfflinePlayer player) { return false; }
        @Override public List<String> getBanks() { return Collections.emptyList(); }
        @Override public boolean createPlayerAccount(String playerName) { set(playerName, 0.0); return true; }
        @Override public boolean createPlayerAccount(OfflinePlayer player) { return createPlayerAccount(player.getName()); }
        @Override public boolean createPlayerAccount(String playerName, String worldName) { return createPlayerAccount(playerName); }
        @Override public boolean createPlayerAccount(OfflinePlayer player, String worldName) { return createPlayerAccount(player.getName()); }
    }
}
