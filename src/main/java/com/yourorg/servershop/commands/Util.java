package com.yourorg.servershop.commands;

import com.yourorg.servershop.ServerShopPlugin;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;

public final class Util {
    public static Material parseMaterial(ServerShopPlugin plugin, CommandSender sender, String raw) {
        if (raw == null) return null;
        String lang = plugin.locale(sender);
        Material viaAlias = plugin.aliases().match(raw, lang);
        if (viaAlias != null) return viaAlias;
        return Material.matchMaterial(raw.toUpperCase().replace('-', '_').replace(' ', '_'));
    }
}
