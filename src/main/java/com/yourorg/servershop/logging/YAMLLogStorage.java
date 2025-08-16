package com.yourorg.servershop.logging;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class YAMLLogStorage implements LogStorage {
    private final File file;
    private final int maxEntries;
    private final List<Map<String, Object>> entries;

    public YAMLLogStorage(File dataFolder, int maxEntries) {
        this.file = new File(dataFolder, "transactions.yml");
        this.maxEntries = Math.max(100, maxEntries);
        this.entries = (List<Map<String, Object>>) YamlConfiguration.loadConfiguration(file)
                .getList("entries", new ArrayList<>());
    }

    @Override public synchronized void append(Transaction tx) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("time", tx.time.toEpochMilli());
        row.put("player", tx.player);
        row.put("type", tx.type.name());
        row.put("material", tx.material.name());
        row.put("quantity", tx.quantity);
        row.put("amount", tx.amount);
        entries.add(row);
        while (entries.size() > maxEntries) entries.remove(0);
    }

    @Override public synchronized List<Transaction> last(int limit) { return filter(null, limit); }
    @Override public synchronized List<Transaction> lastOf(String player, int limit) { return filter(player.toLowerCase(Locale.ROOT), limit); }

    @Override public synchronized void flush() throws Exception {
        YamlConfiguration y = new YamlConfiguration();
        y.set("entries", entries);
        y.save(file);
    }

    @Override public void close() { try { flush(); } catch (Exception ignored) { } }

    private List<Transaction> filter(String playerLower, int limit) {
        List<Transaction> list = new ArrayList<>();
        for (int i = entries.size() - 1; i >= 0 && list.size() < limit; i--) {
            Map<String, Object> e = entries.get(i);
            String p = String.valueOf(e.get("player"));
            if (playerLower != null && !p.toLowerCase(Locale.ROOT).equals(playerLower)) continue;
            Transaction t = new Transaction(
                    java.time.Instant.ofEpochMilli(((Number) e.get("time")).longValue()),
                    p,
                    Transaction.Type.valueOf(String.valueOf(e.get("type"))),
                    org.bukkit.Material.matchMaterial(String.valueOf(e.get("material"))),
                    ((Number) e.get("quantity")).intValue(),
                    ((Number) e.get("amount")).doubleValue()
            );
            list.add(t);
        }
        return list;
    }
}
