package com.yourorg.servershop.commands;

import com.yourorg.servershop.ServerShopPlugin;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public final class SellAllCommand implements CommandExecutor {
    private final ServerShopPlugin plugin;
    public SellAllCommand(ServerShopPlugin plugin) { this.plugin = plugin; }

    @Override public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage(plugin.prefixed(plugin.getConfig().getString("messages.not-a-player"))); return true; }
        plugin.menus().openSell(p);
        return true;
    }
}
