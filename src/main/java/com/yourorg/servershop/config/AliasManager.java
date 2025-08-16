package com.yourorg.servershop.config;

import com.yourorg.servershop.ServerShopPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

/**
 * Loads per-locale alias mappings from aliases-<lang>.yml files.
 */
public final class AliasManager {
    private final ServerShopPlugin plugin;
    private final String defaultLang;
    private final Map<String, Map<String, Material>> byLang = new HashMap<>();
    private final Map<String, Map<Material, Set<String>>> reverse = new HashMap<>();

    public AliasManager(ServerShopPlugin plugin, String defaultLang) {
        this.plugin = plugin;
        this.defaultLang = defaultLang.toLowerCase(Locale.ROOT);
        reload();
    }

    public void reload() {
        byLang.clear();
        reverse.clear();
        File dir = plugin.getDataFolder();
        File[] files = dir.listFiles((d, n) -> n.startsWith("aliases-") && n.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            String name = f.getName();
            String lang = name.substring("aliases-".length(), name.length() - 4).toLowerCase(Locale.ROOT);
            YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
            Map<String, Material> map = byLang.computeIfAbsent(lang, k -> new HashMap<>());
            Map<Material, Set<String>> rev = reverse.computeIfAbsent(lang, k -> new HashMap<>());
            for (String key : y.getKeys(false)) {
                String matName = y.getString(key);
                Material m = matName == null ? null : Material.matchMaterial(matName.toUpperCase(Locale.ROOT));
                if (m != null) {
                    String alias = key.toLowerCase(Locale.ROOT);
                    map.put(alias, m);
                    rev.computeIfAbsent(m, k -> new LinkedHashSet<>()).add(alias);
                }
            }
        }
    }

    public Material match(String token, String lang) {
        if (token == null) return null;
        String t = token.toLowerCase(Locale.ROOT);
        if (lang != null) {
            Map<String, Material> map = byLang.get(lang.toLowerCase(Locale.ROOT));
            if (map != null) {
                Material m = map.get(t);
                if (m != null) return m;
            }
        }
        Map<String, Material> def = byLang.get(defaultLang);
        if (def != null) {
            Material m = def.get(t);
            if (m != null) return m;
        }
        return null;
    }

    public Collection<String> aliases(Material m, String lang) {
        if (m == null || lang == null) return Collections.emptySet();
        Map<Material, Set<String>> map = reverse.get(lang.toLowerCase(Locale.ROOT));
        if (map == null) return Collections.emptySet();
        Set<String> set = map.get(m);
        return set == null ? Collections.emptySet() : set;
    }
}
