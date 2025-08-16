package com.yourorg.servershop.dynamic;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import net.jqwik.api.*;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Assertions;

import com.yourorg.servershop.ServerShopPlugin;
import com.yourorg.servershop.shop.ItemEntry;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

import org.mockito.Mockito;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;

import java.lang.reflect.Field;
import java.util.Map;

public class PricingPropertiesTest {
    static ServerMock server;
    static ServerShopPlugin plugin;

    @BeforeAll
    static void init() {
        server = MockBukkit.mock();
        Plugin vault = MockBukkit.createMockPlugin("Vault");
        Economy econ = Mockito.mock(Economy.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(econ.withdrawPlayer(any(OfflinePlayer.class), anyDouble()))
                .thenReturn(new EconomyResponse(0, 0, EconomyResponse.ResponseType.SUCCESS, ""));
        Mockito.when(econ.depositPlayer(any(OfflinePlayer.class), anyDouble()))
                .thenReturn(new EconomyResponse(0, 0, EconomyResponse.ResponseType.SUCCESS, ""));
        Mockito.when(econ.getBalance(any(OfflinePlayer.class))).thenReturn(1_000_000.0);
        server.getServicesManager().register(Economy.class, econ, vault, ServicePriority.Normal);
        plugin = MockBukkit.load(ServerShopPlugin.class);
    }

    @AfterAll
    static void shutdown() {
        MockBukkit.unmock();
    }

    @Provide
    Arbitrary<Material> materials() {
        return Arbitraries.of(plugin.catalog().allMaterials());
    }

    @Provide
    Arbitrary<Integer> counts() {
        return Arbitraries.integers().between(1, 10_000);
    }

    @Property
    void priceWithinBounds(@ForAll("materials") Material mat) {
        ItemEntry entry = plugin.catalog().get(mat).orElseThrow();
        DynamicPricingManager mgr = new DynamicPricingManager(plugin);
        double buy = mgr.buyPrice(mat, entry.buyPrice());
        double sell = mgr.sellPrice(mat, entry.sellPrice());

        double minBuy = Math.max(0.01, entry.buyPrice() * plugin.getConfig().getDouble("priceModel.minFactor"));
        double maxBuy = Math.max(minBuy, entry.buyPrice() * plugin.getConfig().getDouble("priceModel.maxFactor"));
        Assertions.assertTrue(buy >= minBuy && buy <= maxBuy);

        double minSell = Math.max(0.01, entry.sellPrice() * plugin.getConfig().getDouble("priceModel.minFactor"));
        double maxSell = Math.max(minSell, entry.sellPrice() * plugin.getConfig().getDouble("priceModel.maxFactor"));
        Assertions.assertTrue(sell >= minSell && sell <= maxSell);
    }

    @Property
    void spreadRespected(@ForAll("materials") Material mat) {
        ItemEntry entry = plugin.catalog().get(mat).orElseThrow();
        DynamicPricingManager mgr = new DynamicPricingManager(plugin);
        double buy = mgr.buyPrice(mat, entry.buyPrice());
        double sell = mgr.sellPrice(mat, entry.sellPrice());
        Assertions.assertTrue(buy >= sell);
    }

    @Property
    void adjustmentsCapped(@ForAll("materials") Material mat,
                           @ForAll("counts") int buyQty,
                           @ForAll("counts") int sellQty) throws Exception {
        DynamicPricingManager mgr = new DynamicPricingManager(plugin);
        Map<Material, PriceState> map = stateMap(mgr);
        mgr.adjustOnBuy(mat, buyQty);
        double maxMult = plugin.getConfig().getDouble("dynamicPricing.maxMultiplier");
        double minMult = plugin.getConfig().getDouble("dynamicPricing.minMultiplier");
        double afterBuy = map.get(mat).multiplier;
        Assertions.assertTrue(afterBuy <= maxMult);
        mgr.adjustOnSell(mat, sellQty);
        double afterSell = map.get(mat).multiplier;
        Assertions.assertTrue(afterSell >= minMult && afterSell <= maxMult);
    }

    @SuppressWarnings("unchecked")
    private static Map<Material, PriceState> stateMap(DynamicPricingManager mgr) throws Exception {
        Field f = DynamicPricingManager.class.getDeclaredField("map");
        f.setAccessible(true);
        return (Map<Material, PriceState>) f.get(mgr);
    }
}
