package com.yourorg.servershop.logging;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;

public final class SQLLogStorage implements LogStorage {
    private final HikariDataSource ds;

    public SQLLogStorage(String host, int port, String database, String user, String password, int maxPool, int minIdle, long connTimeout, long idleTimeout, long maxLifetime) throws Exception {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        cfg.setUsername(user); cfg.setPassword(password);
        cfg.setMaximumPoolSize(maxPool); cfg.setMinimumIdle(minIdle);
        cfg.setConnectionTimeout(connTimeout);
        cfg.setIdleTimeout(idleTimeout);
        cfg.setMaxLifetime(maxLifetime);
        cfg.setPoolName("ServerShopPool");
        this.ds = new HikariDataSource(cfg);
        init();
    }

    private void init() throws Exception {
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS servershop_transactions (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "time_ms BIGINT NOT NULL," +
                    "player VARCHAR(32) NOT NULL," +
                    "type VARCHAR(8) NOT NULL," +
                    "material VARCHAR(64) NOT NULL," +
                    "quantity INT NOT NULL," +
                    "amount DECIMAL(10,2) NOT NULL," +
                    "INDEX idx_player_time (player, time_ms)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        }
    }

    @Override public void append(Transaction tx) throws Exception {
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO servershop_transactions(time_ms, player, type, material, quantity, amount) VALUES (?,?,?,?,?,?)")) {
            ps.setLong(1, tx.time.toEpochMilli());
            ps.setString(2, tx.player);
            ps.setString(3, tx.type.name());
            ps.setString(4, tx.material.name());
            ps.setInt(5, tx.quantity);
            ps.setBigDecimal(6, tx.amount);
            ps.executeUpdate();
        }
    }

    @Override public java.util.List<Transaction> last(int limit) throws Exception { return query(null, limit); }
    @Override public java.util.List<Transaction> lastOf(String player, int limit) throws Exception { return query(player, limit); }
    @Override public void close() { if (ds != null) ds.close(); }

    private java.util.List<Transaction> query(String player, int limit) throws Exception {
        String sql = "SELECT time_ms, player, type, material, quantity, amount FROM servershop_transactions " +
                (player != null ? "WHERE player=? " : "") +
                "ORDER BY time_ms DESC LIMIT ?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            int idx = 1;
            if (player != null) ps.setString(idx++, player);
            ps.setInt(idx, limit);
            try (ResultSet rs = ps.executeQuery()) {
                java.util.List<Transaction> list = new java.util.ArrayList<>();
                while (rs.next()) {
                    list.add(new Transaction(
                            java.time.Instant.ofEpochMilli(rs.getLong(1)),
                            rs.getString(2),
                            Transaction.Type.valueOf(rs.getString(3)),
                            org.bukkit.Material.matchMaterial(rs.getString(4)),
                            rs.getInt(5),
                            rs.getBigDecimal(6)));
                }
                return list;
            }
        }
    }
}
