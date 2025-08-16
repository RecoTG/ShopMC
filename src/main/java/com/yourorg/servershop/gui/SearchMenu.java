package com.yourorg.servershop.gui;

import com.yourorg.servershop.ServerShopPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.List;

public final class SearchMenu implements MenuView {
    private static final int PAGE_SIZE = 42;
    private final ServerShopPlugin plugin;
    private final String query;
    private final List<Material> results;
    private final int page;

    public SearchMenu(ServerShopPlugin plugin, String query, List<Material> results, int page) {
        this.plugin = plugin; this.query = query; this.results = results; this.page = Math.max(0, page);
    }

    @Override public Inventory build() {
        Inventory inv = Bukkit.createInventory(null, 6*9, title());
        int start = page * PAGE_SIZE;
        int end = Math.min(results.size(), start + PAGE_SIZE);
        int i = 10;
        for (int idx = start; idx < end; idx++) {
            Material m = results.get(idx);
            if (!plugin.categorySettings().isEnabled(plugin.catalog().categoryOf(m))) continue;
            double buy = plugin.shop().priceBuy(m);
            double sell = plugin.shop().priceSell(m);
            inv.setItem(i, GuiUtil.item(m.isItem()?m:Material.PAPER, "&e"+m.name(), GuiUtil.lore(
                    "&7Buy: &a$"+String.format("%.2f", buy),
                    "&7Sell: &6$"+(sell>0?String.format("%.2f", sell):"-"),
                    "&8Left-click: buy",
                    "&8Right-click: show price")));
            i += (i % 9 == 7) ? 3 : 1;
        }
        if (page > 0) inv.setItem(6*9-9, GuiUtil.item(Material.ARROW, "&aPrevious Page", GuiUtil.lore("&7Go to page "+page)));
        if (end < results.size()) inv.setItem(6*9-1, GuiUtil.item(Material.ARROW, "&aNext Page", GuiUtil.lore("&7Go to page "+(page+2))));
        inv.setItem(6*9-5, GuiUtil.item(Material.BOOK, "&bPage "+(page+1)+"/"+Math.max(1, (int)Math.ceil(results.size()/(double)PAGE_SIZE)), java.util.List.of()));
        return inv;
    }

    @Override public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        var it = e.getCurrentItem(); if (it == null || it.getItemMeta() == null) return;
        String name = org.bukkit.ChatColor.stripColor(it.getItemMeta().getDisplayName());
        if (name.equalsIgnoreCase("Previous Page")) { plugin.menus().openSearch(p, query, results, Math.max(0, page-1)); return; }
        if (name.equalsIgnoreCase("Next Page")) { plugin.menus().openSearch(p, query, results, page+1); return; }
        var m = org.bukkit.Material.matchMaterial(org.bukkit.ChatColor.stripColor(it.getItemMeta().getDisplayName()));
        if (m == null) return;
        if (e.isRightClick()) { double buy = plugin.shop().priceBuy(m); p.sendMessage(plugin.prefixed(m.name()+": $"+String.format("%.2f", buy))); return; }
        plugin.menus().openConfirm(p, m, pl -> plugin.menus().openSearch(pl, query, results, page));
    }

    @Override public String title() { return plugin.getConfig().getString("gui.titles.items", "%category%").replace("%category%", "Search: "+query+" ("+(page+1)+")"); }
}
