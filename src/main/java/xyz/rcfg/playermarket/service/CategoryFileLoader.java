package xyz.rcfg.playermarket.service;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.util.*;

public class CategoryFileLoader {
    public static class CatDef {
        public final String id; public final String name; public final Material icon; public final java.util.List<Material> items; public final boolean weekly; public final int order;
        public CatDef(String id, String name, Material icon, java.util.List<Material> items, boolean weekly, int order){ this.id=id; this.name=name; this.icon=icon; this.items=items; this.weekly=weekly; this.order=order; }
    }
    public static java.util.List<CatDef> load(File folder, java.util.function.Predicate<Material> exclude) {
        java.util.List<CatDef> out = new java.util.ArrayList<>(); if (!folder.exists()||!folder.isDirectory()) return out;
        File[] files = folder.listFiles((d,n)->n.toLowerCase(java.util.Locale.ROOT).endsWith(".yml")); if (files==null) return out;
        Arrays.sort(files, Comparator.comparing(File::getName));
        for (File f : files) {
            try {
                String id = f.getName().substring(0, f.getName().length()-4).toLowerCase(java.util.Locale.ROOT);
                var y = YamlConfiguration.loadConfiguration(f);
                String name = y.getString("name", id);
                String iconStr = y.getString("icon", "STONE");
                int order = y.getInt("order", 100);
                boolean weekly = id.equals("weekly") || y.getBoolean("weekly", false);
                Material icon = Material.matchMaterial(iconStr.toUpperCase(java.util.Locale.ROOT)); if (icon==null) icon = Material.STONE;
                java.util.List<Material> mats = new java.util.ArrayList<>();
                for (String s : y.getStringList("items")) {
                    Material m = Material.matchMaterial(s.toUpperCase(java.util.Locale.ROOT));
                    if (m==null) continue; if (exclude.test(m)) continue; mats.add(m);
                }
                out.add(new CatDef(id, name, icon, mats, weekly, order));
            } catch (Exception ignored){}
        }
        out.sort(Comparator.comparingInt(c->c.order));
        return out;
    }
}
