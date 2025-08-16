package com.yourorg.servershop.config;

import com.yourorg.servershop.ServerShopPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class Aliases {
    private final ServerShopPlugin plugin;
    private final File file;
    private final Map<String, Material> map = new HashMap<>();

    public Aliases(ServerShopPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "aliases.yml");
        load();
    }

    public synchronized void load() {
        map.clear();
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        for (String key : y.getKeys(false)) {
            String val = y.getString(key);
            if (val == null) continue;
            Material m = Material.matchMaterial(val.toUpperCase(Locale.ROOT));
            if (m != null) map.put(key.toLowerCase(Locale.ROOT), m);
        }
    }

    public synchronized Material resolve(String alias) {
        if (alias == null) return null;
        return map.get(alias.toLowerCase(Locale.ROOT));
    }
}
