package xyz.rcfg.playermarket.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;
import java.sql.Connection;
import java.sql.SQLException;

public class Database implements AutoCloseable {
    private final HikariDataSource ds; private final String tablePrefix;
    public Database(ConfigurationSection mysqlConfig) {
        String host = mysqlConfig.getString("host","localhost");
        int port = mysqlConfig.getInt("port",3306);
        String db = mysqlConfig.getString("database","playermarket");
        String user = mysqlConfig.getString("username","root");
        String pass = mysqlConfig.getString("password","");
        boolean useSsl = mysqlConfig.getBoolean("useSsl", false);
        this.tablePrefix = mysqlConfig.getString("tablePrefix","pm_");
        String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=" + useSsl + "&allowPublicKeyRetrieval=true&characterEncoding=utf8&useUnicode=true&serverTimezone=UTC";
        HikariConfig cfg = new HikariConfig(); cfg.setJdbcUrl(jdbcUrl); cfg.setUsername(user); cfg.setPassword(pass);
        cfg.setMaximumPoolSize(5); cfg.setMinimumIdle(1); cfg.setPoolName("PlayerMarketPool");
        cfg.addDataSourceProperty("cachePrepStmts", "true"); cfg.addDataSourceProperty("prepStmtCacheSize","250");
        cfg.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        this.ds = new HikariDataSource(cfg);
    }
    public String tablePrefix(){ return tablePrefix; }
    public Connection getConnection() throws SQLException { return ds.getConnection(); }
    public Connection get() throws SQLException { return getConnection(); }
    public void migrate() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "stock (k VARCHAR(128) PRIMARY KEY, qty BIGINT NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
        try (var c = getConnection(); var st = c.createStatement()) { st.execute(sql); }
    }
    @Override public void close() { if (ds!=null) ds.close(); }
}
