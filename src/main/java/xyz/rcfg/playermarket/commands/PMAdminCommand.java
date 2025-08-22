package xyz.rcfg.playermarket.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.rcfg.playermarket.PlayerMarketPlugin;

public class PMAdminCommand implements CommandExecutor {
    private final PlayerMarketPlugin plugin;
    public PMAdminCommand(PlayerMarketPlugin plugin){ this.plugin = plugin; }
    @Override public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length==0) { sender.sendMessage("/pm <reloadcats|rebuildcats|exportprices|loadandfill|sellall>"); return true; }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "rebuildcats" -> { plugin.shop().rebuildCategories(); sender.sendMessage("§aCategories rebuilt."); }
            case "reloadcats" -> { plugin.shop().rebuildCategories(); sender.sendMessage("§aCategories reloaded."); }
            case "exportprices" -> { int c = plugin.shop().exportPrices(); sender.sendMessage("§aExported "+c+" prices."); }
            case "loadandfill" -> { plugin.shop().reloadAll(); sender.sendMessage("§aReloaded prices and categories."); }
            case "sellall" -> { if (sender instanceof Player p) plugin.shop().openSellAll(p); else sender.sendMessage("Players only."); }
            default -> sender.sendMessage("/pm <reloadcats|rebuildcats|exportprices|loadandfill|sellall>");
        } return true;
    }
}
