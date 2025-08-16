package com.yourorg.servershop.lang;

import com.yourorg.servershop.ServerShopPlugin;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class LanguageManager {
    private final ServerShopPlugin plugin;
    private final Map<String, Map<String, Material>> aliases = new HashMap<>();
    private final Map<UUID, String> playerLang = new HashMap<>();
    private final java.util.List<String> languages = new ArrayList<>();

    public LanguageManager(ServerShopPlugin plugin) {
        this.plugin = plugin;
        // ensure default resources exist
        plugin.saveResource("aliases-en.yml", false);
        plugin.saveResource("aliases-de.yml", false);
        loadAliases();
        loadPlayers();
    }

    private void loadAliases() {
        aliases.clear();
        languages.clear();
        File folder = plugin.getDataFolder();
        File[] files = folder.listFiles(f -> f.getName().startsWith("aliases-") && f.getName().endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            String name = f.getName();
            String lang = name.substring("aliases-".length(), name.length() - 4).toLowerCase(Locale.ROOT);
            YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
            Map<String, Material> map = new HashMap<>();
            for (String key : y.getKeys(false)) {
                String matName = y.getString(key);
                if (matName == null) continue;
                Material m = Material.matchMaterial(matName.toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_'));
                if (m != null) map.put(key.toLowerCase(Locale.ROOT).replace('-', ' ').replace('_', ' '), m);
            }
            aliases.put(lang, map);
            languages.add(lang);
        }
    }

    private void loadPlayers() {
        playerLang.clear();
        File f = new File(plugin.getDataFolder(), "lang.yml");
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        for (String key : y.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                String lang = y.getString(key, "en");
                playerLang.put(id, lang);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private void savePlayers() {
        File f = new File(plugin.getDataFolder(), "lang.yml");
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<UUID, String> e : playerLang.entrySet()) {
            y.set(e.getKey().toString(), e.getValue());
        }
        try { y.save(f); } catch (IOException ignored) {}
    }

    public Material parseMaterial(CommandSender sender, String raw) {
        if (raw == null) return null;
        String norm = raw.toLowerCase(Locale.ROOT).replace('-', ' ').replace('_', ' ');
        if (sender instanceof Player p) {
            String lang = playerLang.get(p.getUniqueId());
            if (lang != null) {
                Material m = match(lang, norm);
                if (m != null) return m;
            }
        }
        for (String lang : languages) {
            Material m = match(lang, norm);
            if (m != null) return m;
        }
        return Material.matchMaterial(raw.toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_'));
    }

    private Material match(String lang, String norm) {
        Map<String, Material> map = aliases.get(lang);
        if (map == null) return null;
        return map.get(norm);
    }

    public String toggle(Player p) {
        if (languages.isEmpty()) return "en";
        String cur = playerLang.getOrDefault(p.getUniqueId(), languages.get(0));
        int idx = languages.indexOf(cur);
        String next = languages.get((idx + 1) % languages.size());
        playerLang.put(p.getUniqueId(), next);
        savePlayers();
        return next;
    }
}
