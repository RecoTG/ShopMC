package com.yourorg.servershop.commands;

import com.yourorg.servershop.ServerShopPlugin;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public final class SellCommand implements CommandExecutor {
    private final ServerShopPlugin plugin;
    public SellCommand(ServerShopPlugin plugin) { this.plugin = plugin; }

    @Override public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage(plugin.prefixed(plugin.getConfig().getString("messages.not-a-player"))); return true; }
        if (args.length < 2) { p.sendMessage(plugin.prefixed("/sell <material> <qty>")); return true; }
        Material mat = Util.parseMaterial(args[1]);
        int qty; try { qty = Math.max(1, Integer.parseInt(args.length > 2 ? args[2] : "1")); } catch (Exception e) { qty = 1; }
        if (mat == null) { p.sendMessage(plugin.prefixed(plugin.getConfig().getString("messages.unknown-material").replace("%material%", args[1]))); return true; }
        plugin.shop().sell(p, mat, qty).ifPresent(err -> p.sendMessage(plugin.prefixed(err)));
        return true;
    }
}
