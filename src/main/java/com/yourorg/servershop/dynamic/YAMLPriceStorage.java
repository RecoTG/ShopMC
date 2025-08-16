package com.yourorg.servershop.dynamic;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import java.math.BigDecimal;

import java.io.File;

public final class YAMLPriceStorage implements PriceStorage {
    private final File file;

    public YAMLPriceStorage(File dataFolder) { this.file = new File(dataFolder, "prices.yml"); }

    @Override public synchronized java.util.Map<Material, PriceState> loadAll() {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        java.util.Map<Material, PriceState> map = new java.util.EnumMap<>(Material.class);
        ConfigurationSection sec = y.getConfigurationSection("prices");
        if (sec == null) return map;
        for (String key : sec.getKeys(false)) {
            Material m = Material.matchMaterial(key); if (m == null) continue;
            Object mObj = sec.getConfigurationSection(key).get("multiplier");
            double mult = mObj == null ? 1.0 : new BigDecimal(String.valueOf(mObj)).doubleValue();
            long last = sec.getConfigurationSection(key).getLong("lastUpdate", System.currentTimeMillis());
            map.put(m, new PriceState(mult, last));
        }
        return map;
    }

    @Override public synchronized void save(Material mat, PriceState st) throws Exception {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        y.set("prices."+mat.name()+".multiplier", BigDecimal.valueOf(st.multiplier).toPlainString());
        y.set("prices."+mat.name()+".lastUpdate", st.lastUpdateMs);
        y.save(file);
    }

    @Override public synchronized void saveAll(java.util.Map<Material, PriceState> map) throws Exception {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        for (var e : map.entrySet()) {
            y.set("prices."+e.getKey().name()+".multiplier", BigDecimal.valueOf(e.getValue().multiplier).toPlainString());
            y.set("prices."+e.getKey().name()+".lastUpdate", e.getValue().lastUpdateMs);
        }
        y.save(file);
    }

    @Override public void close() { }
}
