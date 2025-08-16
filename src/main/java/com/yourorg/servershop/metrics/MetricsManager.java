package com.yourorg.servershop.metrics;

import com.yourorg.servershop.ServerShopPlugin;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Material;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Handles bStats metrics for the plugin.
 */
public final class MetricsManager {
    private static final int BSTATS_PLUGIN_ID = 21312; // TODO: replace with your bStats plugin id

    private final Metrics metrics;
    private int buys;
    private int sells;
    private double totalValue;
    private int transactions;
    private final Map<Material, Integer> itemUsage = new ConcurrentHashMap<>();

    public MetricsManager(ServerShopPlugin plugin) {
        this.metrics = new Metrics(plugin, BSTATS_PLUGIN_ID);
        metrics.addCustomChart(new SingleLineChart("buys", () -> buys));
        metrics.addCustomChart(new SingleLineChart("sells", () -> sells));
        metrics.addCustomChart(new AdvancedPie("top_10_items", this::top10Items));
        metrics.addCustomChart(new SingleLineChart("avg_transaction_value", this::averageValue));
        metrics.addCustomChart(new SimplePie("economy_provider", () -> plugin.economy().getName()));
    }

    public void recordBuy(Material mat, double total) {
        buys++;
        record(mat, total);
    }

    public void recordSell(Material mat, double total) {
        sells++;
        record(mat, total);
    }

    private void record(Material mat, double total) {
        itemUsage.merge(mat, 1, Integer::sum);
        totalValue += total;
        transactions++;
    }

    private Map<String, Integer> top10Items() {
        return itemUsage.entrySet().stream()
                .sorted(Map.Entry.<Material, Integer>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(e -> e.getKey().name(), Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    private int averageValue() {
        if (transactions == 0) return 0;
        return (int) Math.round(totalValue / transactions);
    }
}

