package com.yourorg.servershop.commands;

import com.yourorg.servershop.ServerShopPlugin;
import com.yourorg.servershop.shop.ItemEntry;
import com.yourorg.servershop.util.Fuzzy;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public final class ShopCommand implements TabExecutor {
    private final ServerShopPlugin plugin;
    private final AdminCommand admin;

    public ShopCommand(ServerShopPlugin plugin) { this.plugin = plugin; this.admin = new AdminCommand(plugin); }

    @Override public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player p) { plugin.menus().openCategories(p); } else sender.sendMessage(plugin.prefixed(plugin.getConfig().getString("messages.not-a-player")));
            return true;
        }
        if (args[0].equalsIgnoreCase("admin")) return admin.handle(sender, args);
        if (args[0].equalsIgnoreCase("search")) return search(sender, args);
        if (args[0].equalsIgnoreCase("price")) return price(sender, args);
        if (args[0].equalsIgnoreCase("buy")) return buy(sender, args);
        sender.sendMessage(plugin.prefixed("Unknown subcommand."));
        return true;
    }

    private boolean search(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(plugin.prefixed("/shop search <name> [page]")); return true; }
        int pageArg = 1;
        java.util.List<String> parts = new java.util.ArrayList<>(java.util.Arrays.asList(args).subList(1, args.length));
        try { int maybe = Integer.parseInt(parts.get(parts.size()-1)); if (maybe > 0) { pageArg = maybe; parts.remove(parts.size()-1); } } catch (Exception ignored) {}
        String q = String.join(" ", parts).trim();
        if (q.isEmpty()) { sender.sendMessage(plugin.prefixed("Please provide a search term.")); return true; }

        java.util.Set<Material> allowed = new java.util.TreeSet<>(plugin.catalog().allMaterials());
        java.util.List<Material> mats = Fuzzy.rankMaterials(allowed, q, 500, 0.45, plugin.aliases(), plugin.locale(sender));
        java.util.List<Material> enabled = mats.stream().filter(m -> plugin.categorySettings().isEnabled(plugin.catalog().categoryOf(m))).collect(java.util.stream.Collectors.toList());
        if (enabled.isEmpty()) { sender.sendMessage(plugin.prefixed("No matches.")); return true; }

        if (sender instanceof Player p) {
            plugin.menus().openSearch(p, q, enabled, Math.max(0, pageArg-1));
        } else {
            int perPage = 20; int page0 = Math.max(0, pageArg-1);
            int start = page0*perPage; int end = Math.min(enabled.size(), start+perPage);
            sender.sendMessage(plugin.prefixed("Results "+(start+1)+"-"+end+" of "+enabled.size()+" (page "+(page0+1)+")."));
            for (int i=start;i<end;i++) {
                Material m = enabled.get(i);
                var e = plugin.catalog().get(m).orElse(null); if (e == null) continue;
                double price = plugin.shop().priceBuy(m);
                sender.sendMessage(" - "+m.name()+": $"+String.format("%.2f", price));
            }
        }
        return true;
    }

    private boolean price(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(plugin.prefixed("/shop price <material>")); return true; }
        Material mat = Util.parseMaterial(plugin, sender, args[1]);
        if (mat == null) { sender.sendMessage(plugin.prefixed(msg("unknown-material").replace("%material%", args[1]))); return true; }
        Optional<ItemEntry> opt = plugin.catalog().get(mat);
        if (opt.isEmpty() || !opt.get().canBuy()) { sender.sendMessage(plugin.prefixed(msg("not-for-sale").replace("%material%", mat.name()))); return true; }
        double price = plugin.shop().priceBuy(mat);
        sender.sendMessage(plugin.prefixed(mat.name() + ": $" + String.format("%.2f", price)));
        return true;
    }

    private boolean buy(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage(plugin.prefixed(msg("not-a-player"))); return true; }
        if (plugin.economy() == null) { sender.sendMessage(plugin.prefixed(msg("no-economy"))); return true; }
        if (args.length < 3) { p.sendMessage(plugin.prefixed("/shop buy <material> <qty>")); return true; }
        Material mat = Util.parseMaterial(plugin, sender, args[1]);
        int qty; try { qty = Math.max(1, Integer.parseInt(args[2])); } catch (Exception ex) { qty = 1; }
        if (mat == null) { p.sendMessage(plugin.prefixed(msg("unknown-material").replace("%material%", args[1]))); return true; }
        plugin.shop().buy(p, mat, qty).ifPresent(err -> p.sendMessage(plugin.prefixed(err)));
        return true;
    }

    private String msg(String key) { return plugin.getConfig().getString("messages." + key, key); }

    @Override public java.util.List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        java.util.List<String> out = new java.util.ArrayList<>();
        if (args.length == 1) { suggest(out, args[0], "price","buy","search","admin"); return out; }
        if (args[0].equalsIgnoreCase("admin")) {
            if (args.length == 2) { suggest(out, args[1], "category","import","reload"); return out; }
            if (args[1].equalsIgnoreCase("category")) {
                if (args.length == 3) { suggest(out, args[2], "list","setmult","toggle"); return out; }
                if (args[2].equalsIgnoreCase("setmult")) {
                    if (args.length == 4) { suggestCats(out, args[3]); return out; }
                    if (args.length == 5) { suggest(out, args[4], "0.5","0.75","1","1.25","1.5"); return out; }
                }
                if (args[2].equalsIgnoreCase("toggle")) {
                    if (args.length == 4) { suggestCats(out, args[3]); return out; }
                    if (args.length == 5) { suggest(out, args[4], "on","off"); return out; }
                }
            }
            return out;
        }
        if (args[0].equalsIgnoreCase("price") || args[0].equalsIgnoreCase("buy")) {
              if (args.length == 2) { suggestMats(sender, out, args[1]); return out; }
            if (args[0].equalsIgnoreCase("buy") && args.length == 3) { suggest(out, args[2], "1","16","64"); return out; }
        }
        if (args[0].equalsIgnoreCase("search")) { return out; }
        return out;
    }

    private void suggest(java.util.List<String> out, String token, String... opts) {
        String t = token == null ? "" : token.toLowerCase(java.util.Locale.ROOT);
        for (String o : opts) if (o.toLowerCase(java.util.Locale.ROOT).startsWith(t)) out.add(o);
    }
    private void suggestCats(java.util.List<String> out, String token) {
        String t = token == null ? "" : token.toLowerCase(java.util.Locale.ROOT);
        for (String c : plugin.categorySettings().categories()) if (c.toLowerCase(java.util.Locale.ROOT).startsWith(t)) out.add(c);
    }
      private void suggestMats(CommandSender sender, java.util.List<String> out, String token) {
          String t = token == null ? "" : token.toUpperCase(java.util.Locale.ROOT).replace(' ', '_');
          String lang = plugin.locale(sender);
          for (Material m : new java.util.TreeSet<>(plugin.catalog().allMaterials())) {
              var e = plugin.catalog().get(m).orElse(null);
              if (e == null || !e.canBuy()) continue;
              String name = m.name();
              if (name.startsWith(t)) out.add(name);
              for (String a : plugin.aliases().aliases(m, lang)) {
                  String A = a.toUpperCase(java.util.Locale.ROOT);
                  if (A.startsWith(t)) out.add(a);
              }
              if (out.size() >= 50) break;
          }
      }
}
