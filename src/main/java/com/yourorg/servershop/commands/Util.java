package com.yourorg.servershop.commands;

import com.yourorg.servershop.ServerShopPlugin;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;

public final class Util {
    public static Material parseMaterial(ServerShopPlugin plugin, CommandSender sender, String raw) {
        if (plugin == null) return null;
        return plugin.lang().parseMaterial(sender, raw);
    }
}
