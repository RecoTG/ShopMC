package com.yourorg.servershop.logging;

import com.yourorg.servershop.ServerShopPlugin;
import org.bukkit.Bukkit;

import java.util.List;

public final class LoggerManager {
    private final ServerShopPlugin plugin;
    private final LogStorage storage;

    public LoggerManager(ServerShopPlugin plugin) {
        this.plugin = plugin;
        var c = plugin.getConfig();
        String mode = c.getString("logging.storage", "YAML").toUpperCase();
        long flushSec = c.getLong("yaml.flushEverySeconds", 5L);
        LogStorage s;
        if ("MYSQL".equals(mode)) {
            try {
                s = new SQLLogStorage(
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
                plugin.getLogger().info("Logging storage: MySQL");
            } catch (Exception e) {
                plugin.getLogger().warning("MySQL setup failed, falling back to YAML: " + e.getMessage());
                int max = c.getInt("logging.maxEntries", 1000);
                s = new YAMLLogStorage(plugin, max, flushSec);
            }
        } else {
            int max = c.getInt("logging.maxEntries", 1000);
            s = new YAMLLogStorage(plugin, max, flushSec);
            plugin.getLogger().info("Logging storage: YAML");
        }
        this.storage = s;
    }

    public void logAsync(Transaction tx) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try { storage.append(tx); } catch (Exception e) { plugin.getLogger().warning("Failed to log: " + e.getMessage()); }
        });
    }

    public void flush() { try { storage.flush(); } catch (Exception ignored) { } }

    public void close() { try { storage.close(); } catch (Exception ignored) { } }

    public void lastAsync(String playerOrNull, int limit, java.util.function.Consumer<List<Transaction>> cb) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                List<Transaction> list = (playerOrNull == null) ? storage.last(limit) : storage.lastOf(playerOrNull, limit);
                Bukkit.getScheduler().runTask(plugin, () -> cb.accept(list));
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to read log: " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> cb.accept(java.util.Collections.emptyList()));
            }
        });
    }
}
