package com.yourorg.servershop.config;

import com.yourorg.servershop.ServerShopPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public final class ConfigMigrator {
    private static final int CURRENT_VERSION = 2;

    private ConfigMigrator() {}

    public static void migrate(ServerShopPlugin plugin) {
        FileConfiguration cfg = plugin.getConfig();
        int ver = cfg.getInt("configVersion", 0);
        boolean changed = false;

        if (ver < 2) {
            if (!cfg.contains("logging.retentionDays")) {
                cfg.set("logging.retentionDays", 30);
            }
            ver = 2;
            cfg.set("configVersion", ver);
            changed = true;
        }

        if (changed) {
            plugin.saveConfig();
            plugin.reloadConfig();
        }
    }
}
