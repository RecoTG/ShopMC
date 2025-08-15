package com.yourorg.servershop.commands;

import com.yourorg.servershop.ServerShopPlugin;
import com.yourorg.servershop.logging.Transaction;
import org.bukkit.command.*;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class ShopLogCommand implements CommandExecutor {
    private final ServerShopPlugin plugin;
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    public ShopLogCommand(ServerShopPlugin plugin) { this.plugin = plugin; }

    @Override public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        int limit = 10; String player = null;
        if (args.length >= 1) player = args[0];
        if (args.length >= 2) try { limit = Integer.parseInt(args[1]); } catch (Exception ignored) {}
        final int flimit = limit; final String fplayer = player;
        plugin.logger().lastAsync(fplayer, flimit, list -> {
            sender.sendMessage(plugin.prefixed(plugin.getConfig().getString("messages.log-header").replace("%n%", String.valueOf(flimit))));
            for (Transaction t : list) {
                String line = plugin.getConfig().getString("messages.log-line");
                line = line.replace("%time%", fmt.format(t.time))
                        .replace("%player%", t.player)
                        .replace("%type%", t.type.name().toLowerCase())
                        .replace("%qty%", String.valueOf(t.quantity))
                        .replace("%material%", t.material.name())
                        .replace("%amount%", String.format("%.2f", t.amount));
                sender.sendMessage(plugin.prefixed(line));
            }
        });
        return true;
    }
}
