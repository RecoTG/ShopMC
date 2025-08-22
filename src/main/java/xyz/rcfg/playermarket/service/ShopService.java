package xyz.rcfg.playermarket.service;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import xyz.rcfg.playermarket.PlayerMarketPlugin;
import xyz.rcfg.playermarket.database.Database;
import xyz.rcfg.playermarket.database.DbStockBackend;
import xyz.rcfg.playermarket.gui.CategoryMenu;
import xyz.rcfg.playermarket.gui.RootMenu;
import xyz.rcfg.playermarket.gui.SellAllMenu;

import java.io.File;
import java.util.*;

public class ShopService {
    public record Category(String id, String name, Material icon, java.util.List<Material> items) {}
    public record ItemInfo(Material material, double basePrice, int anchor) {}

    private final PlayerMarketPlugin plugin;
    private final Map<Material, ItemInfo> items = new HashMap<>();
    private final StockBackend stock;
    private final PricingService pricing;
    private final LinkedHashMap<String, Category> categories = new LinkedHashMap<>();
    private Database database;

    public ShopService(PlayerMarketPlugin plugin){
        this.plugin=plugin;
        int anchor = plugin.getConfig().getInt("pricing.default_anchor", 256);
        double alpha = plugin.getConfig().getDouble("pricing.alpha", 0.35);
        double spread = plugin.getConfig().getDouble("pricing.sell_spread", 0.80);
        this.pricing = new PricingService(anchor, alpha, spread);

        StockBackend chosen;
        String mode = plugin.getConfig().getString("storage.mode","FLATFILE").toUpperCase(java.util.Locale.ROOT);
        if ("MYSQL".equals(mode)) {
            try { this.database = new Database(plugin.getConfig().getConfigurationSection("storage.mysql")); this.database.migrate(); chosen = new DbStockBackend(database); }
            catch (Exception ex){ plugin.getLogger().severe("MySQL init failed, using flatfile: "+ex.getMessage()); chosen = new StockStore(new File(plugin.getDataFolder(),"data/stock.yml")); }
        } else chosen = new StockStore(new File(plugin.getDataFolder(),"data/stock.yml"));
        this.stock = chosen;

        loadPrices();
        rebuildCategories();
    }

    public PlayerMarketPlugin getPlugin(){ return plugin; }
    public PricingService getPricing(){ return pricing; }
    public StockBackend getStock(){ return stock; }
    public Map<Material, ItemInfo> getItemStore(){ return items; }
    public ItemInfo getItemInfo(Material m){ return items.get(m); }
    public Map<String, Category> getCategories(){ return categories; }

    public void ensureItem(Material m){ items.putIfAbsent(m, new ItemInfo(m, 1.0, plugin.getConfig().getInt("pricing.default_anchor",256))); }

    public int loadPrices(){
        try {
            File f = new File(plugin.getDataFolder(), plugin.getConfig().getString("importer.file","data/base-prices.yml"));
            if (!f.exists()) plugin.saveResource("data/base-prices.yml", false);
            YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
            Map<String,Object> map = y.isConfigurationSection("prices") ? y.getConfigurationSection("prices").getValues(false) : y.getValues(false);
            int anchor = plugin.getConfig().getInt("pricing.default_anchor", 256);
            int c=0;
            for (var e : map.entrySet()) {
                try {
                    Material m = Material.valueOf(e.getKey().toUpperCase(java.util.Locale.ROOT));
                    double v = Double.parseDouble(String.valueOf(e.getValue()));
                    items.put(m, new ItemInfo(m, v, anchor)); c++;
                } catch (Exception ignored){}
            } return c;
        } catch (Exception ex){ plugin.getLogger().severe("Price load failed: "+ex.getMessage()); return 0; }
    }

    public int exportPrices(){
        try {
            File f = new File(plugin.getDataFolder(), "data/exported-prices.yml"); f.getParentFile().mkdirs();
            YamlConfiguration y = new YamlConfiguration(); for (var e: items.entrySet()) y.set("prices."+e.getKey().name(), e.getValue().basePrice()); y.save(f); return items.size();
        } catch (Exception ex){ plugin.getLogger().severe("Price export failed: "+ex.getMessage()); return 0; }
    }

    public boolean isExcludedByRules(Material m){
        if (!m.isItem()) return true;
        String n = m.name();
        if (n.endsWith("_GLASS_PANE")) return true;
        if (n.equals("SPYGLASS") || n.equals("GLASS_BOTTLE")) return true;
        if (n.endsWith("_WALL") || n.endsWith("_RAIL") || n.endsWith("_CARPET") || n.equals("CHAIN")) return true;
        if (n.endsWith("_BUTTON") || n.endsWith("_PRESSURE_PLATE")) return true;
        if (n.contains("SCULK")) return true;
        if (n.contains("_ORE")) return true;
        if (n.contains("CORAL")) return true;
        if (n.contains("FROGSPAWN")) return true;
        if (n.contains("GLOW_LICHEN")) return true;
        if (n.equals("HANGING_ROOTS")) return true;
        if (n.contains("AMETHYST_BUD") || n.contains("LEAF_LITTER")) return true;
        if (n.equals("HEAVY_CORE") || n.equals("RESPAWN_ANCHOR")) return true;
        if (n.equals("BELL")) return true;
        if (n.endsWith("SHULKER_BOX")) return true;
        if (n.contains("CHISELED") || n.contains("_CUT") || n.contains("CUT_") || n.contains("GRATE")) return true;
        if (n.equals("SEAGRASS") || n.equals("TALL_SEAGRASS") || n.equals("SEA_PICKLE")) return true;
        if (n.equals("GRASS_BLOCK") || n.equals("SHORT_GRASS") || n.equals("TALL_GRASS") || n.equals("FERN") || n.equals("LARGE_FERN")) return true;
        if (n.endsWith("_SPAWN_EGG") || n.equals("SNIFFER_EGG") || n.equals("EGG")) return true;
        if (n.equals("VAULT") || n.equals("TRIAL_SPAWNER")) return true;
        if (n.equals("CRAFTING_TABLE") || n.contains("CHEST") || n.contains("FURNACE") || n.equals("BARREL")) return true;
        if (n.equals("CARTOGRAPHY_TABLE")) return true;
        if (n.equals("BEACON")) return true;
        if (n.equals("CAULDRON") || n.equals("WATER_CAULDRON") || n.equals("LAVA_CAULDRON") || n.equals("POWDER_SNOW_CAULDRON")) return true;
        if (n.contains("STRUCTURE_") || n.equals("JIGSAW") || n.endsWith("COMMAND_BLOCK") || n.equals("BARRIER") || n.equals("BEDROCK") || n.equals("LIGHT") || n.equals("DEBUG_STICK") || n.equals("REINFORCED_DEEPSLATE")) return true;
        if (n.endsWith("_POTTERY_SHERD")) return true;
        if (n.equals("END_CRYSTAL") || n.equals("END_PORTAL_FRAME")) return true;
        if (n.endsWith("_SIGN") || n.endsWith("_HANGING_SIGN")) return true;
        if (n.endsWith("_FENCE") || n.endsWith("_FENCE_GATE")) return true;
        if (n.endsWith("_STAIRS") || n.endsWith("_SLAB")) return true;
        if (n.endsWith("_BOAT") || n.endsWith("_CHEST_BOAT") || n.contains("BOAT")) return true;
        if (n.equals("NETHER_SPROUTS")) return true;
        if (n.endsWith("_STEW") || n.endsWith("_SOUP")) return true;
        if (n.equals("ROOTED_DIRT")) return true;
        if (n.endsWith("_HEAD") || n.endsWith("_SKULL")) return true;
        if (n.contains("CANDLE")) return true;
        if (n.contains("BANNER") || n.contains("BANNER_PATTERN")) return true;
        if (n.contains("PITCHER")) return true;
        if (n.contains("DRIPSTONE")) return true;
        return false;
    }

    private boolean isWool(Material m){ return m.name().endsWith("_WOOL"); }
    private boolean isGlass(Material m){ return m == Material.GLASS || m.name().endsWith("_GLASS"); }
    private boolean isConcrete(Material m){ return m.name().endsWith("_CONCRETE"); }
    private boolean isTerracotta(Material m){ return m.name().endsWith("_TERRACOTTA") || m==Material.TERRACOTTA; }
    private boolean isBrickish(Material m){ return m.name().endsWith("_BRICKS") || m.name().contains("BRICK_"); }
    private boolean isLog(Material m){
        String n = m.name();
        return n.endsWith("_LOG") || n.endsWith("_WOOD") || n.endsWith("_STEM") || n.endsWith("_HYPHAE") ||
               n.equals("BAMBOO_BLOCK") || n.equals("STRIPPED_BAMBOO_BLOCK");
    }
    private boolean isNature2(Material m){
        String n = m.name();
        return n.endsWith("_SAPLING") || n.endsWith("_LEAVES") || n.endsWith("_FLOWER") ||
               n.endsWith("_GRASS") || n.endsWith("_TULIP") || n.contains("AZALEA") ||
               n.contains("LILY") || n.contains("ROOT") || n.contains("MOSS_BLOCK");
    }
    private boolean isFarming(Material m){
        String n = m.name();
        return n.contains("WHEAT") || n.contains("CARROT") || n.contains("POTATO") || n.contains("BEETROOT")
                || n.contains("PUMPKIN") || n.contains("MELON") || n.contains("SUGAR_CANE")
                || n.contains("BAMBOO") || n.contains("COCOA") || n.contains("CACTUS") || n.contains("NETHER_WART");
    }
    private boolean isMineral(Material m){
        String n = m.name();
        return n.endsWith("_INGOT") || n.endsWith("_NUGGET") || n.endsWith("_GEM")
                || n.equals("NETHERITE_INGOT") || n.equals("LAPIS_LAZULI") || n.equals("REDSTONE") || n.equals("QUARTZ")
                || n.endsWith("_BLOCK");
    }
    private boolean isRedstone(Material m){
        String n = m.name();
        return n.equals("REDSTONE") || n.contains("REPEATER") || n.contains("COMPARATOR")
                || n.contains("OBSERVER") || n.contains("PISTON") || n.contains("DROPPER") || n.contains("DISPENSER")
                || n.contains("HOPPER") || n.contains("NOTE_BLOCK") || n.contains("DAYLIGHT_DETECTOR") || n.equals("TARGET")
                || n.contains("LEVER") || n.contains("TRIPWIRE");
    }
    private boolean isNether(Material m){ String s = m.name(); return s.contains("NETHERRACK") || s.contains("NETHER_") || s.contains("QUARTZ") || s.contains("NYLIUM") || s.contains("BLACKSTONE") || s.contains("BASALT") || s.contains("WART_BLOCK") || s.contains("MAGMA") || s.contains("GLOWSTONE") || s.contains("SHROOMLIGHT") || s.contains("CRYING_OBSIDIAN"); }
    private boolean isEnd(Material m){ String s = m.name(); return s.contains("END_") || s.contains("PURPUR") || s.equals("CHORUS_FLOWER") || s.equals("CHORUS_FRUIT"); }

    private int compareByBaseThenName(Material a, Material b){
        double pa = items.getOrDefault(a, new ItemInfo(a, 1.0, 256)).basePrice();
        double pb = items.getOrDefault(b, new ItemInfo(b, 1.0, 256)).basePrice();
        int c = Double.compare(pa, pb); if (c!=0) return c; return a.name().compareTo(b.name());
    }

    public void rebuildCategories(){
        categories.clear();
        File catsDir = new File(plugin.getDataFolder(), "cats");
        if (!catsDir.exists()) {
            String[] defaults = new String[]{
                    "cats/weekly.yml","cats/glass.yml","cats/wool.yml","cats/concrete.yml","cats/terracotta.yml","cats/bricks.yml",
                    "cats/logs.yml","cats/nature.yml","cats/farming.yml","cats/minerals.yml","cats/redstone.yml","cats/nether.yml","cats/end.yml","cats/blocks.yml"
            };
            for (String res : defaults) { try { plugin.saveResource(res, false); } catch (IllegalArgumentException ignored) {} }
        }
        java.util.List<CategoryFileLoader.CatDef> defs = CategoryFileLoader.load(catsDir, this::isExcludedByRules);
        if (!defs.isEmpty()){
            CategoryFileLoader.CatDef weekly=null;
            for (var d: defs) if ("weekly".equalsIgnoreCase(d.id)) { weekly = d; break; }
            if (weekly!=null) { for (var m: weekly.items) ensureItem(m); addCategory(weekly.id, weekly.name, weekly.icon, weekly.items, true); }
            for (var d: defs) {
                if (weekly!=null && d==weekly) continue;
                java.util.List<Material> mats = new java.util.ArrayList<>(d.items);
                if (mats.isEmpty()){
                    java.util.function.Predicate<Material> pred = switch (d.id) {
                        case "glass" -> this::isGlass;
                        case "wool" -> this::isWool;
                        case "concrete" -> this::isConcrete;
                        case "terracotta" -> this::isTerracotta;
                        case "bricks" -> this::isBrickish;
                        case "logs" -> this::isLog;
                        case "nature" -> this::isNature2;
                        case "farming" -> this::isFarming;
                        case "minerals" -> this::isMineral;
                        case "redstone" -> this::isRedstone;
                        case "nether" -> this::isNether;
                        case "end" -> this::isEnd;
                        case "blocks" -> (m -> m.isBlock() && m.isItem() && !isLog(m) && !isGlass(m) && !isWool(m) && !isConcrete(m) && !isTerracotta(m) && !isBrickish(m));
                        default -> (m -> false);
                    };
                    for (Material m : Material.values()) {
                        if (isExcludedByRules(m)) continue;
                        if (pred.test(m)) mats.add(m);
                    }
                    mats.sort(this::compareByBaseThenName);
                }
                for (var m : mats) ensureItem(m);
                addCategory(d.id, d.name, d.icon, mats);
            }
            return;
        }

        // Fallback generation if no files
        java.util.List<Material> weekly = new java.util.ArrayList<>();
        for (String s : plugin.getConfig().getStringList("weekly.items")) {
            try { Material m = Material.valueOf(s.toUpperCase(java.util.Locale.ROOT)); if (!isExcludedByRules(m)) { weekly.add(m); ensureItem(m); } } catch (Exception ignored) {}
        }
        addCategory("weekly","Weekly Items", Material.CLOCK, weekly, true);
        addCategory("glass","Glass", Material.GLASS, filter(this::isGlass));
        addCategory("wool","Wool", Material.WHITE_WOOL, filter(this::isWool));
        addCategory("concrete","Concrete Blocks", Material.WHITE_CONCRETE, filter(this::isConcrete));
        addCategory("terracotta","Terracotta Blocks", Material.TERRACOTTA, filter(this::isTerracotta));
        addCategory("bricks","Bricks", Material.BRICKS, filter(this::isBrickish));
        addCategory("logs","Logs", Material.OAK_LOG, filter(this::isLog));
        addCategory("nature","Nature", Material.OAK_SAPLING, filter(this::isNature2));
        addCategory("farming","Farming", Material.WHEAT, filter(this::isFarming));
        addCategory("minerals","Minerals", Material.IRON_INGOT, filter(this::isMineral));
        addCategory("redstone","Redstone", Material.REDSTONE, filter(this::isRedstone));
        addCategory("nether","Nether", Material.NETHERRACK, filter(this::isNether));
        addCategory("end","The End", Material.END_STONE, filter(this::isEnd));
        addCategory("blocks","Blocks", Material.STONE, filter(m -> m.isBlock() && m.isItem() && !isLog(m) && !isGlass(m) && !isWool(m) && !isConcrete(m) && !isTerracotta(m) && !isBrickish(m)));
    }

    private java.util.List<Material> filter(java.util.function.Predicate<Material> pred){
        java.util.List<Material> list = new java.util.ArrayList<>();
        for (Material m : Material.values()) {
            if (isExcludedByRules(m)) continue;
            if (pred.test(m)) { list.add(m); ensureItem(m); }
        }
        list.sort(this::compareByBaseThenName);
        return list;
    }

    private void addCategory(String id, String name, Material icon, java.util.List<Material> items){ categories.put(id, new Category(id, name, icon, items)); }
    private void addCategory(String id, String name, Material icon, java.util.List<Material> items, boolean weekly){ categories.put(id, new Category(id, name, icon, items)); }

    public void reloadAll(){ items.clear(); loadPrices(); rebuildCategories(); }
    public void openRoot(Player p){ new RootMenu(this, p).open(); }
    public void openCategory(Player p, Category c, int page){ new CategoryMenu(this, p, c, page).open(); }
    public void openSellAll(Player p){ new SellAllMenu(p).open(); }
    public PricingService.Prices computePrices(Material m, long stock, boolean weekly){ return pricing.computePrices(getItemInfo(m), stock, weekly); }
}
