package com.yourorg.servershop.config;

import com.yourorg.servershop.ServerShopPlugin;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles per-rank buy/sell multipliers via LuckPerms groups.
 * If LuckPerms is not present, all multipliers default to 1.0.
 */
public final class RankMultipliers {
    private final ServerShopPlugin plugin;
    private LuckPerms luckPerms;
    private final Map<String, Multiplier> multipliers = new HashMap<>();

    public RankMultipliers(ServerShopPlugin plugin) {
        this.plugin = plugin;
        // Attempt to hook into LuckPerms if present
        try {
            if (plugin.getServer().getPluginManager().getPlugin("LuckPerms") != null) {
                this.luckPerms = LuckPermsProvider.get();
            }
        } catch (Exception ignored) {}
        reload();
    }

    /** Reload multipliers from config. */
    public void reload() {
        multipliers.clear();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("rankMultipliers");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                double buy = sec.getDouble(key + ".buy", 1.0);
                double sell = sec.getDouble(key + ".sell", 1.0);
                multipliers.put(key.toLowerCase(), new Multiplier(buy, sell));
            }
        }
    }

    public double buyMultiplier(Player p) {
        Multiplier m = multipliers.get(groupOf(p));
        return m != null ? m.buy : 1.0;
    }

    public double sellMultiplier(Player p) {
        Multiplier m = multipliers.get(groupOf(p));
        return m != null ? m.sell : 1.0;
    }

    private String groupOf(Player p) {
        if (luckPerms != null) {
            var user = luckPerms.getPlayerAdapter(Player.class).getUser(p);
            if (user != null) return user.getPrimaryGroup().toLowerCase();
        }
        return "default";
    }

    private record Multiplier(double buy, double sell) {}
}

