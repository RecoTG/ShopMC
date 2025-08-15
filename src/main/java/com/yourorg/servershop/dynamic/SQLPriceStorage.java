package com.yourorg.servershop.dynamic;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Material;

import java.sql.*;

public final class SQLPriceStorage implements PriceStorage {
    private final HikariDataSource ds;

    public SQLPriceStorage(String host, int port, String database, String user, String password, int maxPool, int minIdle, long connTimeout, long idleTimeout, long maxLifetime) throws Exception {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        cfg.setUsername(user); cfg.setPassword(password);
        cfg.setMaximumPoolSize(maxPool); cfg.setMinimumIdle(minIdle);
        cfg.setConnectionTimeout(connTimeout);
        cfg.setIdleTimeout(idleTimeout);
        cfg.setMaxLifetime(maxLifetime);
        cfg.setPoolName("ServerShopPrices");
        this.ds = new HikariDataSource(cfg);
        init();
    }

    private void init() throws Exception {
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS servershop_prices (" +
                    "material VARCHAR(64) PRIMARY KEY," +
                    "multiplier DOUBLE NOT NULL," +
                    "last_update_ms BIGINT NOT NULL" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        }
    }

    @Override public java.util.Map<Material, PriceState> loadAll() throws Exception {
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT material, multiplier, last_update_ms FROM servershop_prices")) {
            java.util.Map<Material, PriceState> map = new java.util.EnumMap<>(Material.class);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Material m = Material.matchMaterial(rs.getString(1));
                    if (m == null) continue;
                    map.put(m, new PriceState(rs.getDouble(2), rs.getLong(3)));
                }
            }
            return map;
        }
    }

    @Override public void save(Material mat, PriceState st) throws Exception {
        String sql = "INSERT INTO servershop_prices(material, multiplier, last_update_ms) VALUES (?,?,?) ON DUPLICATE KEY UPDATE multiplier=VALUES(multiplier), last_update_ms=VALUES(last_update_ms)";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, mat.name());
            ps.setDouble(2, st.multiplier);
            ps.setLong(3, st.lastUpdateMs);
            ps.executeUpdate();
        }
    }

    @Override public void saveAll(java.util.Map<Material, PriceState> map) throws Exception {
        String sql = "INSERT INTO servershop_prices(material, multiplier, last_update_ms) VALUES (?,?,?) ON DUPLICATE KEY UPDATE multiplier=VALUES(multiplier), last_update_ms=VALUES(last_update_ms)";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (var e : map.entrySet()) {
                ps.setString(1, e.getKey().name());
                ps.setDouble(2, e.getValue().multiplier);
                ps.setLong(3, e.getValue().lastUpdateMs);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    @Override public void close() { if (ds != null) ds.close(); }
}
