package com.yourorg.servershop.weekly;

import com.yourorg.servershop.ServerShopPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

/**
 * Manages the list of weekly picks. Picks are rotated automatically according to
 * a cron expression and chosen from a configurable pool (optionally weighted).
 */
public final class WeeklyShopManager {
    private final ServerShopPlugin plugin;
    private final int count;
    private final Map<Material, Double> pool = new EnumMap<>(Material.class);
    private final Set<Material> current = new LinkedHashSet<>();
    private final Random random = new Random();
    private final File file;
    private final CronSchedule schedule;
    private final ZoneId zone = ZoneId.of("UTC");
    private long lastRotation = 0L;

    public WeeklyShopManager(ServerShopPlugin plugin) {
        this.plugin = plugin;
        var cfg = plugin.getConfig().getConfigurationSection("weekly");
        this.count = Math.max(1, cfg.getInt("count", 6));
        this.schedule = new CronSchedule(cfg.getString("cron", "0 0 * * MON"));
        loadPool(cfg);
        this.file = new File(plugin.getDataFolder(), "weekly.yml");
        load();
        Instant now = Instant.now();
        if (current.isEmpty() || lastRotation == 0L ||
                schedule.next(Instant.ofEpochMilli(lastRotation), zone).isBefore(now)) {
            rotate();
        }
        scheduleNext();
    }

    /**
     * Returns true if the given material is in the current weekly picks.
     */
    public synchronized boolean isWeekly(Material m) { return current.contains(m); }

    /**
     * Returns the set of current weekly picks.
     */
    public synchronized Set<Material> currentPicks() { return new LinkedHashSet<>(current); }

    private void loadPool(ConfigurationSection cfg) {
        if (cfg.isConfigurationSection("pool")) {
            ConfigurationSection sec = cfg.getConfigurationSection("pool");
            for (String k : sec.getKeys(false)) {
                Material m = Material.matchMaterial(k);
                if (m != null) pool.put(m, Math.max(0.0, sec.getDouble(k, 1.0)));
            }
        } else if (cfg.isList("pool")) {
            for (String s : cfg.getStringList("pool")) {
                Material m = Material.matchMaterial(s);
                if (m != null) pool.put(m, 1.0);
            }
        }
        if (pool.isEmpty()) {
            for (Material m : plugin.catalog().allMaterials()) pool.put(m, 1.0);
        }
    }

    private synchronized void rotate() {
        current.clear();
        List<Map.Entry<Material, Double>> entries = new ArrayList<>(pool.entrySet());
        for (int i = 0; i < Math.min(count, entries.size()); i++) {
            double total = 0.0;
            for (var e : entries) total += Math.max(0.0, e.getValue());
            double r = random.nextDouble() * total;
            double acc = 0.0;
            Iterator<Map.Entry<Material, Double>> it = entries.iterator();
            while (it.hasNext()) {
                var e = it.next();
                acc += Math.max(0.0, e.getValue());
                if (r <= acc) { current.add(e.getKey()); it.remove(); break; }
            }
        }
        lastRotation = System.currentTimeMillis();
        save();
    }

    private synchronized void load() {
        current.clear();
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        lastRotation = y.getLong("lastRotation", 0L);
        for (String s : y.getStringList("picks")) {
            Material m = Material.matchMaterial(s);
            if (m != null) current.add(m);
        }
    }

    private synchronized void save() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("lastRotation", lastRotation);
        List<String> list = new ArrayList<>();
        for (Material m : current) list.add(m.name());
        y.set("picks", list);
        try { y.save(file); } catch (IOException e) {
            plugin.getLogger().warning("Failed to save weekly picks: " + e.getMessage());
        }
    }

    private void scheduleNext() {
        Instant next = schedule.next(Instant.ofEpochMilli(lastRotation), zone);
        long delayTicks = Math.max(1L, (next.toEpochMilli() - System.currentTimeMillis()) / 50L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            rotate();
            scheduleNext();
        }, delayTicks);
    }
}

