package com.yourorg.servershop.logging;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public final class YAMLLogStorage implements LogStorage {
    private final File file; private final int maxEntries;

    public YAMLLogStorage(File dataFolder, int maxEntries) {
        this.file = new File(dataFolder, "transactions.yml");
        this.maxEntries = Math.max(100, maxEntries);
    }

    @Override public synchronized void append(Transaction tx) throws Exception {
        YamlConfiguration y = load();
        java.util.List<java.util.Map<String, Object>> entries = (java.util.List<java.util.Map<String, Object>>) y.getList("entries", new java.util.ArrayList<>());
        java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
        row.put("time", tx.time.toEpochMilli());
        row.put("player", tx.player);
        row.put("type", tx.type.name());
        row.put("material", tx.material.name());
        row.put("quantity", tx.quantity);
        row.put("amount", tx.amount);
        row.put("category", tx.category);
        entries.add(row);
        while (entries.size() > maxEntries) entries.remove(0);
        y.set("entries", entries);
        y.save(file);
    }

    @Override public synchronized java.util.List<Transaction> last(int limit) throws Exception {
        return query(null, null, null, 0, limit);
    }

    @Override public synchronized java.util.List<Transaction> lastOf(String player, int limit) throws Exception {
        return query(player, null, null, 0, limit);
    }

    @Override public synchronized java.util.List<Transaction> query(String player, org.bukkit.Material material, String category, int offset, int limit) throws Exception {
        YamlConfiguration y = load();
        java.util.List<java.util.Map<String, Object>> entries = (java.util.List<java.util.Map<String, Object>>) y.getList("entries", java.util.Collections.emptyList());
        java.util.List<Transaction> list = new java.util.ArrayList<>();
        int skipped = 0;
        String playerLower = player == null ? null : player.toLowerCase(java.util.Locale.ROOT);
        String categoryLower = category == null ? null : category.toLowerCase(java.util.Locale.ROOT);
        for (int i = entries.size() - 1; i >= 0; i--) {
            java.util.Map<String, Object> e = entries.get(i);
            String p = String.valueOf(e.get("player"));
            if (playerLower != null && !p.toLowerCase(java.util.Locale.ROOT).equals(playerLower)) continue;
            org.bukkit.Material m = org.bukkit.Material.matchMaterial(String.valueOf(e.get("material")));
            if (material != null && m != material) continue;
            String cat = String.valueOf(e.getOrDefault("category", ""));
            if (categoryLower != null && !cat.toLowerCase(java.util.Locale.ROOT).equals(categoryLower)) continue;
            if (skipped < offset) { skipped++; continue; }
            Transaction t = new Transaction(
                    java.time.Instant.ofEpochMilli(((Number) e.get("time")).longValue()),
                    p,
                    Transaction.Type.valueOf(String.valueOf(e.get("type"))),
                    m,
                    ((Number) e.get("quantity")).intValue(),
                    ((Number) e.get("amount")).doubleValue(),
                    cat
            );
            list.add(t);
            if (list.size() >= limit) break;
        }
        return list;
    }

    @Override public synchronized java.util.List<Transaction> since(java.time.Instant from) throws Exception {
        YamlConfiguration y = load();
        java.util.List<java.util.Map<String, Object>> entries = (java.util.List<java.util.Map<String, Object>>) y.getList("entries", java.util.Collections.emptyList());
        java.util.List<Transaction> list = new java.util.ArrayList<>();
        long min = from.toEpochMilli();
        for (java.util.Map<String, Object> e : entries) {
            long tms = ((Number) e.get("time")).longValue();
            if (tms < min) continue;
            String p = String.valueOf(e.get("player"));
            org.bukkit.Material m = org.bukkit.Material.matchMaterial(String.valueOf(e.get("material")));
            String cat = String.valueOf(e.getOrDefault("category", ""));
            Transaction t = new Transaction(
                    java.time.Instant.ofEpochMilli(tms),
                    p,
                    Transaction.Type.valueOf(String.valueOf(e.get("type"))),
                    m,
                    ((Number) e.get("quantity")).intValue(),
                    ((Number) e.get("amount")).doubleValue(),
                    cat
            );
            list.add(t);
        }
        return list;
    }

    @Override public void close() { }

    private YamlConfiguration load() { return YamlConfiguration.loadConfiguration(file); }
}
