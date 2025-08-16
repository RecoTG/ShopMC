package com.yourorg.servershop.dynamic;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

import java.util.EnumMap;
import java.util.Map;

public final class YAMLPriceStorage implements PriceStorage {
    private final File file;
    private final Map<Material, PriceState> pending = new EnumMap<>(Material.class);

    public YAMLPriceStorage(File dataFolder) { this.file = new File(dataFolder, "prices.yml"); }

    @Override public synchronized java.util.Map<Material, PriceState> loadAll() {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        java.util.Map<Material, PriceState> map = new java.util.EnumMap<>(Material.class);
        ConfigurationSection sec = y.getConfigurationSection("prices");
        if (sec == null) return map;
        for (String key : sec.getKeys(false)) {
            Material m = Material.matchMaterial(key); if (m == null) continue;
            double mult = sec.getConfigurationSection(key).getDouble("multiplier", 1.0);
            long last = sec.getConfigurationSection(key).getLong("lastUpdate", System.currentTimeMillis());
            map.put(m, new PriceState(mult, last));
        }
        return map;
    }

    @Override public synchronized void save(Material mat, PriceState st) {
        pending.put(mat, st);
    }

    @Override public synchronized void saveAll(java.util.Map<Material, PriceState> map) {
        pending.putAll(map);
    }

    @Override public synchronized void flush() throws Exception {
        if (pending.isEmpty()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        for (var e : pending.entrySet()) {
            y.set("prices."+e.getKey().name()+".multiplier", e.getValue().multiplier);
            y.set("prices."+e.getKey().name()+".lastUpdate", e.getValue().lastUpdateMs);
        }
        y.save(file);
        pending.clear();
    }

    @Override public void close() {
        try { flush(); } catch (Exception ignored) { }
    }
}
