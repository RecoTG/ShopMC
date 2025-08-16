package com.yourorg.servershop.importer;

import com.yourorg.servershop.ServerShopPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class ShopImporterTest {
    @Test
    public void importsValidItems() throws Exception {
        Path dataDir = Files.createTempDirectory("data");
        Path importDir = Files.createTempDirectory("import");
        Path shopsDir = importDir.resolve("shops");
        Files.createDirectories(shopsDir);

        String yaml = "items:\n" +
                "  sample:\n" +
                "    buy-prices:\n" +
                "      '1':\n" +
                "        amount: $10\n" +
                "    sell-prices:\n" +
                "      '1':\n" +
                "        amount: $5\n" +
                "    products:\n" +
                "      '1':\n" +
                "        material: STONE\n" +
                "      '2':\n" +
                "        material: IRON_SWORD\n";
        Files.writeString(shopsDir.resolve("Blocks.yml"), yaml);

        ServerShopPlugin plugin = Mockito.mock(ServerShopPlugin.class, Mockito.RETURNS_DEEP_STUBS);
        when(plugin.getDataFolder()).thenReturn(dataDir.toFile());

        ShopImporter importer = new ShopImporter(plugin);
        int count = importer.importFrom(importDir.toFile());

        assertEquals(5, count);
        File outFile = dataDir.resolve("shop.yml").toFile();
        assertTrue(outFile.isFile());
        YamlConfiguration out = YamlConfiguration.loadConfiguration(outFile);
        assertTrue(out.contains("categories.blocks.STONE.buy"));
        assertEquals(10.0, out.getDouble("categories.blocks.STONE.buy"), 0.001);
        assertFalse(out.contains("categories.blocks.IRON_SWORD"));
    }
}
