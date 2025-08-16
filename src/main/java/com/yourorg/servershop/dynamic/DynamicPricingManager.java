package com.yourorg.servershop.dynamic;

import com.yourorg.servershop.ServerShopPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;

public final class DynamicPricingManager {
    private final ServerShopPlugin plugin;
    private final PriceStorage storage;
    private final java.util.Map<Material, PriceState> map = new java.util.EnumMap<>(Material.class);
    private final boolean enabled;
    private final double initMult, minMult, maxMult, buyStep, sellStep, perHourTowards1;
    private final boolean decayEnabled;

    public DynamicPricingManager(ServerShopPlugin plugin) {
        this.plugin = plugin;
        var c = plugin.getConfig();
        var dp = c.getConfigurationSection("dynamicPricing");
        this.enabled = dp.getBoolean("enabled", true);
        this.initMult = dp.getDouble("initialMultiplier", 1.0);
        this.minMult = dp.getDouble("minMultiplier", 0.5);
        this.maxMult = dp.getDouble("maxMultiplier", 2.0);
        this.buyStep = dp.getDouble("buyStep", 0.005);
        this.sellStep = dp.getDouble("sellStep", 0.005);
        var dec = dp.getConfigurationSection("decay");
        this.decayEnabled = dec.getBoolean("enabled", true);
        this.perHourTowards1 = dec.getDouble("perHourTowards1", 0.02);

        String mode = dp.getString("storage", "YAML").toUpperCase();
        PriceStorage ps;
        if ("MYSQL".equals(mode)) {
            try {
                ps = new SQLPriceStorage(
                        c.getString("mysql.host"),
                        c.getInt("mysql.port"),
                        c.getString("mysql.database"),
                        c.getString("mysql.user"),
                        c.getString("mysql.password"),
                        c.getInt("mysql.pool.maxPoolSize"),
                        c.getInt("mysql.pool.minimumIdle"),
                        c.getLong("mysql.pool.connectionTimeoutMs"),
                        c.getLong("mysql.pool.idleTimeoutMs"),
                        c.getLong("mysql.pool.maxLifetimeMs")
                );
                plugin.getLogger().info("Dynamic pricing storage: MySQL");
            } catch (Exception e) {
                plugin.getLogger().warning("MySQL price storage failed, falling back to YAML: " + e.getMessage());
                ps = new YAMLPriceStorage(plugin.getDataFolder());
            }
        } else {
            ps = new YAMLPriceStorage(plugin.getDataFolder());
            plugin.getLogger().info("Dynamic pricing storage: YAML");
        }
        this.storage = ps;

        try { map.putAll(storage.loadAll()); } catch (Exception e) { plugin.getLogger().warning("Failed to load prices: "+e.getMessage()); }
        for (Material m : plugin.catalog().allMaterials()) map.computeIfAbsent(m, k -> new PriceState(initMult, System.currentTimeMillis()));
    }

    public synchronized double buyPrice(Material m, double base) {
        double mult = currentMultiplier(m);
        double weeklyDiscount = plugin.weekly().isWeekly(m) ? plugin.getConfig().getDouble("weekly.discount", 0.80) : 1.0;
        String cat = plugin.catalog().categoryOf(m);
        double catMult = plugin.categorySettings().multiplier(cat);
        double price = base * mult * weeklyDiscount * catMult;
        return clampToBounds(price, base);
    }

    public synchronized double sellPrice(Material m, double base) {
        double mult = currentMultiplier(m);
        String cat = plugin.catalog().categoryOf(m);
        double catMult = plugin.categorySettings().multiplier(cat);
        double price = base * mult * catMult;
        return clampToBounds(price, base);
    }

    public synchronized void adjustOnBuy(Material m, int qty) {
        if (!enabled) return;
        PriceState st = map.computeIfAbsent(m, k -> new PriceState(initMult, System.currentTimeMillis()));
        st.multiplier = clampMult(st.multiplier + buyStep * qty);
        st.lastUpdateMs = System.currentTimeMillis();
        saveLater(m, st);
    }

    public synchronized void adjustOnSell(Material m, int qty) {
        if (!enabled) return;
        PriceState st = map.computeIfAbsent(m, k -> new PriceState(initMult, System.currentTimeMillis()));
        st.multiplier = clampMult(st.multiplier - sellStep * qty);
        st.lastUpdateMs = System.currentTimeMillis();
        saveLater(m, st);
    }

    private void saveLater(Material m, PriceState st) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try { storage.save(m, st); } catch (Exception e) { plugin.getLogger().warning("Failed to save price: "+e.getMessage()); }
        });
    }

    public synchronized void tickSaveAll() {
        if (!enabled) return;
        long now = System.currentTimeMillis();
        for (var e : map.entrySet()) applyDecay(e.getValue(), now);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try { storage.saveAll(new java.util.EnumMap<>(map)); } catch (Exception e) { plugin.getLogger().warning("Failed to save prices: "+e.getMessage()); }
        });
    }

    public synchronized void decayTowardsOne(double factor) {
        if (!enabled) return;
        for (var st : map.values()) {
            double m = st.multiplier;
            if (m < minMult || m > maxMult) continue;
            st.multiplier = clampMult(m + (1.0 - m) * factor);
            st.lastUpdateMs = System.currentTimeMillis();
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try { storage.saveAll(new java.util.EnumMap<>(map)); } catch (Exception e) { plugin.getLogger().warning("Failed to save prices: "+e.getMessage()); }
        });
    }

    public synchronized void close() {
        try { storage.saveAll(map); storage.close(); } catch (Exception ignored) { }
    }

    private double clampToBounds(double value, double base) { return plugin.catalog().priceModel().clampToBounds(value, base); }
    private double clampMult(double mult) { return Math.max(minMult, Math.min(maxMult, mult)); }

    private double currentMultiplier(Material m) {
        PriceState st = map.computeIfAbsent(m, k -> new PriceState(initMult, System.currentTimeMillis()));
        applyDecay(st, System.currentTimeMillis());
        return st.multiplier;
    }

    private void applyDecay(PriceState st, long nowMs) {
        if (!decayEnabled) { st.lastUpdateMs = nowMs; return; }
        long dt = Math.max(0, nowMs - st.lastUpdateMs);
        if (dt < 60_000L) { st.lastUpdateMs = nowMs; return; }
        double hours = dt / 3600000.0;
        double factor = Math.pow(1.0 - Math.max(0.0, Math.min(1.0, perHourTowards1)), hours);
        double newMult = 1.0 + (st.multiplier - 1.0) * factor;
        st.multiplier = clampMult(newMult);
        st.lastUpdateMs = nowMs;
    }
}
