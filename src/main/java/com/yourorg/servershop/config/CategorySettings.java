package com.yourorg.servershop.config;

import com.yourorg.servershop.ServerShopPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class CategorySettings {
    private final ServerShopPlugin plugin;
    private final File file;
    private final Map<String, Entry> map = new LinkedHashMap<>();

    public static final class Entry {
        public boolean enabled = true;
        public double multiplier = 1.0;
    }

    public CategorySettings(ServerShopPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "categories.yml");
        load();
    }

    public synchronized void ensureCategory(String name) { map.computeIfAbsent(name, k -> new Entry()); }
    public synchronized boolean isEnabled(String name) { Entry e = map.get(name); return e == null ? true : e.enabled; }
    public synchronized double multiplier(String name) { Entry e = map.get(name); return e == null ? 1.0 : e.multiplier; }
    public synchronized void setEnabled(String name, boolean on) { ensureCategory(name); map.get(name).enabled = on; save(); }
    public synchronized void setMultiplier(String name, double m) { ensureCategory(name); map.get(name).multiplier = Math.max(0.0, m); save(); }
    public synchronized Set<String> categories() { return new LinkedHashSet<>(map.keySet()); }

    public synchronized void load() {
        map.clear();
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        if (!y.isConfigurationSection("categories")) return;
        for (String k : y.getConfigurationSection("categories").getKeys(false)) {
            Entry e = new Entry();
            e.enabled = y.getBoolean("categories."+k+".enabled", true);
            e.multiplier = y.getDouble("categories."+k+".multiplier", 1.0);
            map.put(k, e);
        }
    }

    public synchronized void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (String k : map.keySet()) {
            y.set("categories."+k+".enabled", map.get(k).enabled);
            y.set("categories."+k+".multiplier", map.get(k).multiplier);
        }
        try { y.save(file); } catch (IOException e) { plugin.getLogger().warning("Failed to save categories.yml: "+e.getMessage()); }
    }
}
