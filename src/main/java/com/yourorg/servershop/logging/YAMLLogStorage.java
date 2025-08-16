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
        row.put("uuid", tx.uuid.toString());
        row.put("type", tx.type.name());
        row.put("material", tx.material.name());
        row.put("quantity", tx.quantity);
        row.put("amount", tx.amount.doubleValue());
        entries.add(row);
        while (entries.size() > maxEntries) entries.remove(0);
        y.set("entries", entries);
        y.save(file);
    }

    @Override public synchronized java.util.List<Transaction> last(int limit) throws Exception { return filter(null, limit); }
    @Override public synchronized java.util.List<Transaction> lastOf(String playerUuid, int limit) throws Exception { return filter(playerUuid, limit); }
    @Override public void close() { }

    private java.util.List<Transaction> filter(String playerUuid, int limit) throws Exception {
        YamlConfiguration y = load();
        java.util.List<java.util.Map<String, Object>> entries = (java.util.List<java.util.Map<String, Object>>) y.getList("entries", java.util.Collections.emptyList());
        java.util.List<Transaction> list = new java.util.ArrayList<>();
        for (int i = entries.size() - 1; i >= 0 && list.size() < limit; i--) {
            java.util.Map<String, Object> e = entries.get(i);
            String p = String.valueOf(e.get("player"));
            Object uuidObj = e.get("uuid");
            if (uuidObj == null) continue;
            String u = uuidObj.toString();
            if (playerUuid != null && !u.equals(playerUuid)) continue;
            Transaction t = new Transaction(
                    java.time.Instant.ofEpochMilli(((Number) e.get("time")).longValue()),
                    p,
                    java.util.UUID.fromString(u),
                    Transaction.Type.valueOf(String.valueOf(e.get("type"))),
                    org.bukkit.Material.matchMaterial(String.valueOf(e.get("material"))),
                    ((Number) e.get("quantity")).intValue(),
                    java.math.BigDecimal.valueOf(((Number) e.get("amount")).doubleValue()).setScale(4, java.math.RoundingMode.HALF_UP)
            );
            list.add(t);
        }
        return list;
    }

    private YamlConfiguration load() { return YamlConfiguration.loadConfiguration(file); }

    @Override public synchronized void purgeOlderThan(long cutoffMs) throws Exception {
        YamlConfiguration y = load();
        java.util.List<java.util.Map<String, Object>> entries = (java.util.List<java.util.Map<String, Object>>) y.getList("entries", new java.util.ArrayList<>());
        entries.removeIf(e -> ((Number) e.get("time")).longValue() < cutoffMs);
        while (entries.size() > maxEntries) entries.remove(0);
        y.set("entries", entries);
        y.save(file);
    }

    @Override public synchronized void compact() throws Exception {
        purgeOlderThan(Long.MIN_VALUE);
    }
}
