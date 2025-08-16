package com.yourorg.servershop.metrics;

import com.yourorg.servershop.ServerShopPlugin;
import com.yourorg.servershop.logging.Transaction;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.MultiLineChart;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class MetricsManager {
    private static final int BSTATS_ID = 21357; // bStats plugin ID
    private final boolean enabled;
    private final Stats stats = new Stats();

    public MetricsManager(ServerShopPlugin plugin) {
        this.enabled = plugin.getConfig().getBoolean("metrics.enabled", false);
        if (!enabled) return;
        Metrics metrics = new Metrics(plugin, BSTATS_ID);
        metrics.addCustomChart(new MultiLineChart("transactions", () -> stats.buySellMap()));
        metrics.addCustomChart(new AdvancedPie("top_items", () -> stats.topItems()));
        metrics.addCustomChart(new SingleLineChart("average_transaction", () -> stats.averageAmount()));
    }

    public void track(Transaction tx) {
        if (!enabled) return;
        stats.record(tx);
    }

    private static class Stats {
        private int buys = 0;
        private int sells = 0;
        private double totalAmount = 0;
        private final Map<Material, Integer> itemCounts = new HashMap<>();

        synchronized void record(Transaction tx) {
            if (tx.type == Transaction.Type.BUY) buys++; else sells++;
            totalAmount += tx.amount;
            itemCounts.merge(tx.material, tx.quantity, Integer::sum);
        }

        synchronized Map<String, Integer> buySellMap() {
            Map<String, Integer> map = new HashMap<>();
            map.put("buys", buys);
            map.put("sells", sells);
            return map;
        }

        synchronized Map<String, Integer> topItems() {
            return itemCounts.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .limit(5)
                    .collect(Collectors.toMap(e -> e.getKey().name(), Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
        }

        synchronized int averageAmount() {
            int total = buys + sells;
            if (total == 0) return 0;
            return (int) Math.round(totalAmount / total);
        }
    }
}
