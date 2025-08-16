package com.yourorg.servershop.commands;

import com.yourorg.servershop.ServerShopPlugin;
import com.yourorg.servershop.logging.Transaction;
import org.bukkit.Material;
import org.bukkit.command.*;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class ShopLogCommand implements CommandExecutor {
    private final ServerShopPlugin plugin;
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    public ShopLogCommand(ServerShopPlugin plugin) { this.plugin = plugin; }

    @Override public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("export")) {
            return handleExport(sender, args);
        }

        int page = 0; String player = null; Material item = null; String category = null;
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            switch (a.toLowerCase()) {
                case "player": if (i + 1 < args.length) player = args[++i]; break;
                case "item": if (i + 1 < args.length) item = Material.matchMaterial(args[++i]); break;
                case "category": if (i + 1 < args.length) category = args[++i]; break;
                case "page": if (i + 1 < args.length) try { page = Math.max(0, Integer.parseInt(args[++i]) - 1); } catch (Exception ignored) {} break;
                default:
                    if (page == 0) try { page = Math.max(0, Integer.parseInt(a) - 1); } catch (Exception ignored) {}
            }
        }
        final int pageSize = 10;
        final int offset = page * pageSize;
        final String fPlayer = player; final Material fItem = item; final String fCategory = category; final int fPage = page;
        plugin.logger().queryAsync(fPlayer, fItem, fCategory, offset, pageSize, list -> {
            sender.sendMessage(plugin.prefixed(plugin.getConfig().getString("messages.log-header").replace("%n%", String.valueOf(pageSize)) + " (page " + (fPage + 1) + ")"));
            if (list.isEmpty()) { sender.sendMessage(plugin.prefixed("No entries.")); return; }
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

    private boolean handleExport(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(plugin.prefixed("Usage: /shoplog export <time> [player <p>] [item <m>] [category <c>]")); return true; }
        Duration dur = parseDuration(args[1]);
        if (dur == null) { sender.sendMessage(plugin.prefixed("Invalid duration.")); return true; }
        String player = null; Material item = null; String category = null;
        for (int i = 2; i < args.length; i++) {
            String a = args[i];
            switch (a.toLowerCase()) {
                case "player": if (i + 1 < args.length) player = args[++i]; break;
                case "item": if (i + 1 < args.length) item = Material.matchMaterial(args[++i]); break;
                case "category": if (i + 1 < args.length) category = args[++i]; break;
            }
        }
        Instant from = Instant.now().minus(dur);
        final String fPlayer = player; final Material fItem = item; final String fCategory = category;
        plugin.logger().sinceAsync(from, list -> {
            List<Transaction> filtered = new ArrayList<>();
            for (Transaction t : list) {
                if (fPlayer != null && !t.player.equalsIgnoreCase(fPlayer)) continue;
                if (fItem != null && t.material != fItem) continue;
                if (fCategory != null && !t.category.equalsIgnoreCase(fCategory)) continue;
                filtered.add(t);
            }
            File out = new File(plugin.getDataFolder(), "shoplog-" + System.currentTimeMillis() + ".csv");
            try (PrintWriter pw = new PrintWriter(out, StandardCharsets.UTF_8)) {
                pw.println("time,player,type,material,quantity,amount,category");
                for (Transaction t : filtered) {
                    pw.println(fmt.format(t.time) + "," + t.player + "," + t.type.name().toLowerCase() + "," + t.material.name() + "," + t.quantity + "," + String.format("%.2f", t.amount) + "," + t.category);
                }
            } catch (Exception e) {
                sender.sendMessage(plugin.prefixed("Export failed: " + e.getMessage()));
                return;
            }
            sender.sendMessage(plugin.prefixed("Exported " + filtered.size() + " entries to " + out.getName()));
        });
        return true;
    }

    private Duration parseDuration(String s) {
        try {
            if (s.length() < 2) return null;
            char unit = Character.toLowerCase(s.charAt(s.length() - 1));
            long val = Long.parseLong(s.substring(0, s.length() - 1));
            switch (unit) {
                case 'd': return Duration.ofDays(val);
                case 'h': return Duration.ofHours(val);
                case 'm': return Duration.ofMinutes(val);
                default: return null;
            }
        } catch (Exception e) {
            return null;
        }
    }
}
