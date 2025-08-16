package com.yourorg.servershop.commands;

import com.yourorg.servershop.ServerShopPlugin;
import com.yourorg.servershop.importer.ShopImporter;
import org.bukkit.ChatColor;
import org.bukkit.command.*;

import java.io.File;

public final class AdminCommand {
    private final ServerShopPlugin plugin;
    public AdminCommand(ServerShopPlugin plugin) { this.plugin = plugin; }

    public boolean handle(CommandSender sender, String[] args) {
        if (!sender.hasPermission("servershop.admin")) { sender.sendMessage(plugin.prefixed("No permission.")); return true; }
        if (args.length < 2) { help(sender); return true; }
        switch (args[1].toLowerCase()) {
            case "category": return handleCategory(sender, slice(args, 2));
            case "import": return handleImport(sender);
            case "reload":
                plugin.reloadConfig();
                plugin.categorySettings().load();
                plugin.catalog().reload();
                sender.sendMessage(plugin.prefixed("Reloaded."));
                return true;
            default: help(sender); return true;
        }
    }

    private boolean handleImport(CommandSender sender) {
        try {
            File imports = new File(plugin.getDataFolder(), "imports");
            int before = plugin.catalog().allMaterials().size();
            int n = new ShopImporter(plugin).importFrom(imports);
            plugin.catalog().reload();
            int after = plugin.catalog().allMaterials().size();
            sender.sendMessage(plugin.prefixed("Imported "+n+" entries. Items now: "+after+" (was "+before+")"));
        } catch (Exception e) {
            sender.sendMessage(plugin.prefixed("Import failed: "+e.getMessage()));
        }
        return true;
    }

    private boolean handleCategory(CommandSender sender, String[] args) {
        if (args.length == 0) { helpCategory(sender); return true; }
        String sub = args[0].toLowerCase();
        if (sub.equals("list")) {
            StringBuilder sb = new StringBuilder("Categories: ");
            for (String c : plugin.categorySettings().categories()) {
                boolean en = plugin.categorySettings().isEnabled(c);
                double m = plugin.categorySettings().multiplier(c);
                sb.append(ChatColor.YELLOW).append(c).append(ChatColor.GRAY).append("[")
                        .append(en?"on":"off").append(", x").append(m).append("] ");
            }
            sender.sendMessage(sb.toString());
            return true;
        }
        if (sub.equals("setmult") && args.length >= 3) {
            String cat = args[1];
            double m;
            try { m = Double.parseDouble(args[2]); } catch (Exception e) { sender.sendMessage(plugin.prefixed("Bad number.")); return true; }
            plugin.categorySettings().setMultiplier(cat, m);
            sender.sendMessage(plugin.prefixed("Set multiplier for "+cat+" to x"+m));
            return true;
        }
        if (sub.equals("toggle") && args.length >= 3) {
            String cat = args[1];
            boolean on = args[2].equalsIgnoreCase("on") || args[2].equalsIgnoreCase("true");
            plugin.categorySettings().setEnabled(cat, on);
            sender.sendMessage(plugin.prefixed("Category "+cat+" is now "+(on?"enabled":"disabled")));
            return true;
        }
        if (sub.equals("reload")) {
            plugin.categorySettings().load();
            sender.sendMessage(plugin.prefixed("Categories reloaded."));
            return true;
        }
        helpCategory(sender); return true;
    }

    private static String[] slice(String[] a, int from) { String[] b = new String[Math.max(0, a.length-from)]; System.arraycopy(a, from, b, 0, b.length); return b; }
    private void help(CommandSender s) { s.sendMessage(plugin.prefixed("/shop admin category <list|setmult|toggle|reload> ... | import | reload")); }
    private void helpCategory(CommandSender s) { s.sendMessage(plugin.prefixed("/shop admin category list | setmult <cat> <x> | toggle <cat> <on|off> | reload")); }
}
