package com.yourorg.servershop.importer;

import be.seeseemelk.mockbukkit.MockBukkit;
import com.yourorg.servershop.ServerShopPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ShopImporterTest {
    private ServerShopPlugin plugin;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        plugin = MockBukkit.load(ServerShopPlugin.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void badMaterialNamesIgnored() throws Exception {
        File imports = createImportDir("NO_SUCH_MATERIAL");
        int result = new ShopImporter(plugin).importFrom(imports);

        assertTrue(result > 0, "Importer should process valid materials");
        YamlConfiguration out = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "shop.yml"));
        assertTrue(out.contains("categories.test.STONE.buy"), "Valid material should be imported");
        assertFalse(out.contains("categories.test.NO_SUCH_MATERIAL"), "Invalid material should be ignored");
    }

    @Test
    void disallowedMaterialsExcluded() throws Exception {
        File imports = createImportDir("DIAMOND_SWORD");
        int result = new ShopImporter(plugin).importFrom(imports);

        assertTrue(result > 0, "Importer should process valid materials");
        YamlConfiguration out = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "shop.yml"));
        assertTrue(out.contains("categories.test.STONE.buy"), "Valid material should be imported");
        assertFalse(out.contains("categories.test.DIAMOND_SWORD"), "Disallowed material should be excluded");
    }

    private File createImportDir(String secondMaterial) throws IOException {
        Path root = Files.createTempDirectory("imports");
        Path shops = root.resolve("shops");
        Files.createDirectory(shops);

        String yaml = ""
                + "items:\n"
                + "  entry:\n"
                + "    buy-prices:\n"
                + "      '1':\n"
                + "        amount: '$10'\n"
                + "    sell-prices:\n"
                + "      '1':\n"
                + "        amount: '$5'\n"
                + "    products:\n"
                + "      '1':\n"
                + "        material: STONE\n"
                + "      '2':\n"
                + "        material: " + secondMaterial + "\n";
        Files.writeString(shops.resolve("test.yml"), yaml);
        return root.toFile();
    }
}
