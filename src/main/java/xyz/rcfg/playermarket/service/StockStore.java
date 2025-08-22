package xyz.rcfg.playermarket.service;

import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class StockStore implements StockBackend {
    private final File file; private Map<String, Long> stock = new HashMap<>();
    public StockStore(File file){ this.file=file; if (!file.getParentFile().exists()) file.getParentFile().mkdirs(); load(); }
    @Override public synchronized long getStock(String key){ return stock.getOrDefault(key, 0L); }
    @Override public synchronized void setStock(String key, long value){ stock.put(key, Math.max(0L, value)); save(); }
    @Override public synchronized void addStock(String key, long delta){
        long nv = getStock(key) + delta;
        if (nv < 0) nv = 0;
        setStock(key, nv);
    }
    public synchronized void load(){ if (!file.exists()) return; var y = YamlConfiguration.loadConfiguration(file); for (String k : y.getKeys(false)) stock.put(k, y.getLong(k)); }
    public synchronized void save(){ var y = new YamlConfiguration(); for (var e:stock.entrySet()) y.set(e.getKey(), e.getValue()); try { y.save(file);} catch (IOException ignored){} }
}
