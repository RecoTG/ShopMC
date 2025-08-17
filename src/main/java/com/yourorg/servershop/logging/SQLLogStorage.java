package com.yourorg.servershop.logging;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.UUID;

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
                    "ts BIGINT NOT NULL," +
                    "uuid CHAR(36) NOT NULL," +
                    "player VARCHAR(32) NOT NULL," +
                    "type VARCHAR(8) NOT NULL," +
                    "item VARCHAR(64) NOT NULL," +
                    "quantity INT NOT NULL," +
                    "amount DECIMAL(19,4) NOT NULL," +
                    "INDEX idx_ts (ts)," +
                    "INDEX idx_uuid (uuid)," +
                    "INDEX idx_item (item)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            // Ensure efficient global queries by indexing by time as well
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_time_ms ON servershop_transactions(time_ms)");
        }
    }

    @Override public void append(Transaction tx) throws Exception {
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO servershop_transactions(ts, uuid, player, type, item, quantity, amount) VALUES (?,?,?,?,?,?,?)")) {
            ps.setLong(1, tx.time.toEpochMilli());
            ps.setString(2, tx.uuid.toString());
            ps.setString(3, tx.player);
            ps.setString(4, tx.type.name());
            ps.setString(5, tx.material.name());
            ps.setInt(6, tx.quantity);
            ps.setBigDecimal(7, tx.amount);
            ps.executeUpdate();
        }
    }

    @Override public java.util.List<Transaction> last(int limit) throws Exception { return query(null, limit); }
    @Override public java.util.List<Transaction> lastOf(String uuid, int limit) throws Exception { return query(uuid, limit); }
    @Override public void close() { if (ds != null) ds.close(); }

    private java.util.List<Transaction> query(String uuid, int limit) throws Exception {
        String sql = "SELECT ts, uuid, player, type, item, quantity, amount FROM servershop_transactions " +
                (uuid != null ? "WHERE uuid=? " : "") +
                "ORDER BY ts DESC LIMIT ?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            int idx = 1;
            if (uuid != null) ps.setString(idx++, uuid);
            ps.setInt(idx, limit);
            try (ResultSet rs = ps.executeQuery()) {
                java.util.List<Transaction> list = new java.util.ArrayList<>();
                while (rs.next()) {
                    list.add(new Transaction(
                            java.time.Instant.ofEpochMilli(rs.getLong(1)),
                            rs.getString(3),
                            UUID.fromString(rs.getString(2)),
                            Transaction.Type.valueOf(rs.getString(4)),
                            org.bukkit.Material.matchMaterial(rs.getString(5)),
                            rs.getInt(6),
                            rs.getBigDecimal(7)));
                }
                return list;
            }
        }
    }

    @Override public void purgeOlderThan(long cutoffMs) throws Exception {
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(
                "DELETE FROM servershop_transactions WHERE ts < ?")) {
            ps.setLong(1, cutoffMs);
            ps.executeUpdate();
        }
    }

    @Override public void compact() { }
}
