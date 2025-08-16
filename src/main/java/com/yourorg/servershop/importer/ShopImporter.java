package com.yourorg.servershop.importer;

import com.yourorg.servershop.ServerShopPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class ShopImporter {
    private final ServerShopPlugin plugin;

    private static final List<String> DISALLOWED = List.of(
            "SWORD","AXE","PICKAXE","SHOVEL","HOE","SHEARS","FISHING_ROD","FLINT_AND_STEEL",
            "HELMET","CHESTPLATE","LEGGINGS","BOOTS","SHIELD","ELYTRA","HORSE_ARMOR",
            "BOW","CROSSBOW","TRIDENT",
            "POTION","SPLASH_POTION","LINGERING_POTION","TIPPED_ARROW",
            "ARROW","SPECTRAL_ARROW",
            "SPAWN_EGG","SPAWNER",
            "FIREWORK","MUSIC_DISC",
            "BUCKET",
            "_PANE","CARPET","_BED"
    );

    private static final List<String> FOOD = List.of(
            "APPLE","BEEF","PORK","MUTTON","CHICKEN","RABBIT",
            "COD","SALMON","TROPICAL_FISH","PUFFERFISH",
            "COOKED","COOKIE","MELON_SLICE","POTATO","CARROT","BEETROOT",
            "SWEET_BERRIES","GLOW_BERRIES",
            "BREAD","PUMPKIN_PIE","HONEY_BOTTLE","CHORUS_FRUIT","ROTTEN_FLESH","SPIDER_EYE"
    );

    public ShopImporter(ServerShopPlugin plugin) { this.plugin = plugin; }

    private static boolean disallowed(Material m) {
        String u = m.name();
        if (u.contains("BOOK") && !u.equals("BOOKSHELF")) return true;
        for (String s : DISALLOWED) if (u.contains(s)) return true;
        for (String s : FOOD) if (u.equals(s) || u.endsWith("_"+s) || u.contains(s)) return true;
        return false;
    }

    private static String familyKey(Material m) {
        String u = m.name();
        for (String suf : new String[]{"_LOG","_STEM","_HYPHAE","_PLANKS","_WOOL","_CONCRETE","_TERRACOTTA","_GLASS","_GLAZED_TERRACOTTA"})
            if (u.endsWith(suf)) return "ANY"+suf;
        return u;
    }

    public int importFrom(File importsDir) throws IOException {
        File shops = new File(importsDir, "shops");
        if (!shops.isDirectory()) return 0;

        Map<String,double[]> familyPrice = new LinkedHashMap<>();
        Map<String,List<Material>> byCategory = new LinkedHashMap<>();

        File[] files = shops.listFiles((d,f) -> f.toLowerCase().endsWith(".yml"));
        if (files == null) return 0;
        Arrays.sort(files, Comparator.comparing(File::getName));

        for (File f : files) {
            String cat = f.getName().replaceFirst("\\.yml$", "");
            YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
            ConfigurationSection items = y.getConfigurationSection("items");
            if (items == null) continue;
            for (String key : items.getKeys(false)) {
                ConfigurationSection node = items.getConfigurationSection(key);
                if (node == null) continue;
                ConfigurationSection prods = node.getConfigurationSection("products");
                if (prods == null) continue;
                double buy = firstAmount(node.getConfigurationSection("buy-prices"));
                double sell = firstAmount(node.getConfigurationSection("sell-prices"));
                for (String idx : prods.getKeys(false)) {
                    String matName = prods.getConfigurationSection(idx).getString("material");
                    Material m = Material.matchMaterial(matName == null ? "" : matName.toUpperCase());
                    if (m == null) continue;
                    if (disallowed(m)) continue;
                    String fam = familyKey(m);
                    familyPrice.computeIfAbsent(fam, k -> new double[]{Double.NaN, Double.NaN});
                    double[] arr = familyPrice.get(fam);
                    if (Double.isNaN(arr[0]) && !Double.isNaN(buy)) arr[0] = buy;
                    if (Double.isNaN(arr[1]) && !Double.isNaN(sell)) arr[1] = sell;
                    byCategory.computeIfAbsent(cat, k -> new ArrayList<>()).add(m);
                }
            }
        }

        YamlConfiguration out = new YamlConfiguration();
        for (String cat : new TreeSet<>(byCategory.keySet())) {
            String outCatKey = cat.toLowerCase().replace(' ', '_');
            for (Material m : new TreeSet<>(byCategory.get(cat))) {
                String fam = familyKey(m);
                double[] pr = familyPrice.getOrDefault(fam, new double[]{Double.NaN, Double.NaN});
                if (Double.isNaN(pr[0]) && Double.isNaN(pr[1])) continue;
                String path = "categories."+outCatKey+"."+m.name();
                if (!Double.isNaN(pr[0])) out.set(path+".buy", round2(pr[0]));
                if (!Double.isNaN(pr[1])) out.set(path+".sell", round2(pr[1]));
            }
        }
        try { out.save(new File(plugin.getDataFolder(), "shop.yml")); }
        catch (IOException ioe) { throw ioe; }
        return out.getKeys(true).size();
    }

    private static double round2(double v) { return Math.round(v*100.0)/100.0; }
    private static double firstAmount(ConfigurationSection prices) {
        if (prices == null) return Double.NaN;
        for (String k : new TreeSet<>(prices.getKeys(false))) {
            try { return Double.parseDouble(String.valueOf(prices.getConfigurationSection(k).get("amount")).replace("$","")); }
            catch (Exception ignored) { }
        }
        return Double.NaN;
    }
}
