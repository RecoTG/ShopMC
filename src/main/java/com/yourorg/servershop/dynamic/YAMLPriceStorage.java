package com.yourorg.servershop.dynamic;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * Price storage backed by a YAML file. Updates are queued in memory and
 * flushed to disk periodically by an async task. This avoids frequent disk IO
 * while still ensuring data is written on shutdown.
 */
public final class YAMLPriceStorage implements PriceStorage {
    private final JavaPlugin plugin;
    private final File file;
    private final java.util.Map<Material, PriceState> pending = new java.util.EnumMap<>(Material.class);
    private final int taskId;

    public YAMLPriceStorage(JavaPlugin plugin, long flushSeconds) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "prices.yml");
        long ticks = Math.max(1L, flushSeconds) * 20L;
        this.taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::flush, ticks, ticks).getTaskId();
    }

    @Override
    public synchronized java.util.Map<Material, PriceState> loadAll() {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        java.util.Map<Material, PriceState> map = new java.util.EnumMap<>(Material.class);
        ConfigurationSection sec = y.getConfigurationSection("prices");
        if (sec == null) return map;
        for (String key : sec.getKeys(false)) {
            Material m = Material.matchMaterial(key);
            if (m == null) continue;
            double mult = sec.getConfigurationSection(key).getDouble("multiplier", 1.0);
            long last = sec.getConfigurationSection(key).getLong("lastUpdate", System.currentTimeMillis());
            map.put(m, new PriceState(mult, last));
        }
        return map;
    }

    @Override
    public synchronized void save(Material mat, PriceState st) {
        pending.put(mat, st);
    }

    @Override
    public synchronized void saveAll(java.util.Map<Material, PriceState> map) {
        pending.putAll(map);
        flush();
    }

    @Override
    public synchronized void flush() {
        if (pending.isEmpty()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        for (var e : pending.entrySet()) {
            y.set("prices." + e.getKey().name() + ".multiplier", e.getValue().multiplier);
            y.set("prices." + e.getKey().name() + ".lastUpdate", e.getValue().lastUpdateMs);
        }
        pending.clear();
        try {
            y.save(file);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save prices: " + e.getMessage());
        }
    }

    @Override
    public synchronized void close() {
        Bukkit.getScheduler().cancelTask(taskId);
        flush();
    }
}

