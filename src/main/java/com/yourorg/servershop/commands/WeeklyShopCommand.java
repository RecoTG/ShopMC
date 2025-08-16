package com.yourorg.servershop.commands;

import com.yourorg.servershop.ServerShopPlugin;
import org.bukkit.command.*;

public final class WeeklyShopCommand implements CommandExecutor {
    private final ServerShopPlugin plugin;
    public WeeklyShopCommand(ServerShopPlugin plugin) { this.plugin = plugin; }

    @Override public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!plugin.checkCommandCooldown(sender)) return true;
        plugin.menus().openWeekly(sender instanceof org.bukkit.entity.Player p ? p : null);
        return true;
    }
}
