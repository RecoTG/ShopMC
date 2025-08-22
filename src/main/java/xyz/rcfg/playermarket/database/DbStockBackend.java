package xyz.rcfg.playermarket.database;

import xyz.rcfg.playermarket.service.StockBackend;
import java.sql.*;

public class DbStockBackend implements StockBackend {
    private final Database db;
    public DbStockBackend(Database db){ this.db=db; }
    @Override public long getStock(String key){
        String sql = "SELECT qty FROM " + db.tablePrefix() + "stock WHERE k=?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, key); ResultSet rs = ps.executeQuery(); if (rs.next()) return rs.getLong(1);
        } catch (SQLException ignored) {}
        return 0L;
    }
    @Override public void setStock(String key, long value){
        if (value < 0) value = 0;
        String sql = "INSERT INTO " + db.tablePrefix() + "stock (k, qty) VALUES (?,?) ON DUPLICATE KEY UPDATE qty=?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, key); ps.setLong(2, value); ps.setLong(3, value); ps.executeUpdate();
        } catch (SQLException ignored) {}
    }
    @Override public void addStock(String key, long delta){
        String sql = "INSERT INTO " + db.tablePrefix() + "stock (k, qty) VALUES (?,?) ON DUPLICATE KEY UPDATE qty=GREATEST(qty+VALUES(qty),0)";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, key); ps.setLong(2, delta); ps.executeUpdate();
        } catch (SQLException ignored) {}
    }
}
