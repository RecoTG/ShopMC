package com.yourorg.servershop.shop;

import com.yourorg.servershop.ServerShopPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public final class Catalog {
    private final ServerShopPlugin plugin;
    private final Map<Material, ItemEntry> items = new EnumMap<>(Material.class);
    private final Map<String, List<Material>> categories = new LinkedHashMap<>();
    private final Map<Material, String> catByMat = new EnumMap<>(Material.class);
    private PriceModel priceModel;

    public Catalog(ServerShopPlugin plugin) { this.plugin = plugin; }

    public void reload() {
        items.clear(); categories.clear(); catByMat.clear();
        this.priceModel = new PriceModel(plugin.getConfig());
        File f = new File(plugin.getDataFolder(), "shop.yml");
        var yml = YamlConfiguration.loadConfiguration(f);
        ConfigurationSection cats = yml.getConfigurationSection("categories");
        if (cats == null) return;
        for (String cat : cats.getKeys(false)) {
            ConfigurationSection sec = cats.getConfigurationSection(cat);
            List<Material> mats = new ArrayList<>();
            for (String key : sec.getKeys(false)) {
                Material m = Material.matchMaterial(key);
                if (m == null) continue;
                var isec = sec.getConfigurationSection(key);
                double buy = isec.getDouble("buy", 0.0);
                double sell = isec.getDouble("sell", 0.0);
                items.put(m, new ItemEntry(m, buy, sell));
                mats.add(m);
                catByMat.put(m, cat);
            }
            categories.put(cat, Collections.unmodifiableList(mats));
            plugin.categorySettings().ensureCategory(cat);
        }
        plugin.categorySettings().save();
    }

    public java.util.Optional<ItemEntry> get(Material m) { return java.util.Optional.ofNullable(items.get(m)); }
    public java.util.Set<Material> allMaterials() { return items.keySet(); }
    public java.util.Map<String, java.util.List<Material>> categories() { return categories; }
    public PriceModel priceModel() { return priceModel; }
    public String categoryOf(Material m) { return catByMat.getOrDefault(m, ""); }
}
