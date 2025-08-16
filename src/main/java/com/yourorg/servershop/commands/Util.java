package com.yourorg.servershop.commands;

import com.yourorg.servershop.ServerShopPlugin;
import org.bukkit.Material;

public final class Util {
    public static Material parseMaterial(ServerShopPlugin plugin, String raw) {
        if (raw == null) return null;
        Material aliased = plugin.aliases().resolve(raw);
        if (aliased != null) return aliased;
        return Material.matchMaterial(raw.toUpperCase().replace('-', '_').replace(' ', '_'));
    }
}
