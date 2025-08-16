package com.yourorg.servershop.dynamic;

import com.yourorg.servershop.ServerShopPlugin;
import com.yourorg.servershop.shop.Catalog;
import com.yourorg.servershop.shop.PriceModel;
import com.yourorg.servershop.config.CategorySettings;
import com.yourorg.servershop.weekly.WeeklyShopManager;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DynamicPricingManagerTest {
    @Test
    public void decaysMultiplierOverTime() throws Exception {
        Path dataDir = Files.createTempDirectory("data");

        ServerShopPlugin plugin = Mockito.mock(ServerShopPlugin.class);
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("dynamicPricing.enabled", true);
        cfg.set("dynamicPricing.initialMultiplier", 1.0);
        cfg.set("dynamicPricing.minMultiplier", 0.5);
        cfg.set("dynamicPricing.maxMultiplier", 2.0);
        cfg.set("dynamicPricing.buyStep", 0.005);
        cfg.set("dynamicPricing.sellStep", 0.005);
        cfg.set("dynamicPricing.decay.enabled", true);
        cfg.set("dynamicPricing.decay.perHourTowards1", 0.5);
        when(plugin.getConfig()).thenReturn(cfg);
        when(plugin.getDataFolder()).thenReturn(dataDir.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));

        Catalog catalog = Mockito.mock(Catalog.class);
        when(catalog.allMaterials()).thenReturn(Set.of(Material.STONE));
        when(catalog.categoryOf(any(Material.class))).thenReturn("blocks");
        PriceModel pm = Mockito.mock(PriceModel.class);
        when(pm.clampToBounds(anyDouble(), anyDouble())).thenAnswer(i -> i.getArgument(0));
        when(catalog.priceModel()).thenReturn(pm);
        when(plugin.catalog()).thenReturn(catalog);

        WeeklyShopManager weekly = Mockito.mock(WeeklyShopManager.class);
        when(weekly.isWeekly(any(Material.class))).thenReturn(false);
        when(plugin.weekly()).thenReturn(weekly);

        CategorySettings catSettings = Mockito.mock(CategorySettings.class);
        when(catSettings.multiplier(anyString())).thenReturn(1.0);
        when(plugin.categorySettings()).thenReturn(catSettings);

        DynamicPricingManager mgr = new DynamicPricingManager(plugin);

        Field mapField = DynamicPricingManager.class.getDeclaredField("map");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Material, PriceState> map = (Map<Material, PriceState>) mapField.get(mgr);
        PriceState st = map.get(Material.STONE);
        st.multiplier = 2.0;
        st.lastUpdateMs = System.currentTimeMillis() - 2 * 3600_000L;

        double price = mgr.buyPrice(Material.STONE, 10.0);

        assertEquals(12.5, price, 0.01);
        assertEquals(1.25, st.multiplier, 0.01);
    }
}
