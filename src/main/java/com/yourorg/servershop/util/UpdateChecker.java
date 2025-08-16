package com.yourorg.servershop.util;

import com.yourorg.servershop.ServerShopPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class UpdateChecker {

    private static final String API_URL = "https://api.github.com/repos/yourorg/ShopMC/releases/latest";
    private static final String RELEASES_URL = "https://github.com/yourorg/ShopMC/releases";

    private UpdateChecker() {}

    public static void check(ServerShopPlugin plugin) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(API_URL).openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
                try (InputStream in = conn.getInputStream()) {
                    String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    String latest = parseTag(body);
                    String current = plugin.getDescription().getVersion();
                    if (latest != null && !latest.equalsIgnoreCase(current)) {
                        String msg = plugin.prefixed(ChatColor.YELLOW + "A new version (" + latest + ") is available: " + RELEASES_URL);
                        plugin.getServer().getConsoleSender().sendMessage(msg);
                        for (Player p : plugin.getServer().getOnlinePlayers()) {
                            if (p.hasPermission("servershop.admin")) p.sendMessage(msg);
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().fine("Update check failed: " + e.getMessage());
            }
        });
    }

    private static String parseTag(String json) {
        int idx = json.indexOf("\"tag_name\"");
        if (idx == -1) return null;
        int start = json.indexOf('"', idx + 10);
        if (start == -1) return null;
        int end = json.indexOf('"', start + 1);
        if (end == -1) return null;
        return json.substring(start + 1, end);
    }
}

