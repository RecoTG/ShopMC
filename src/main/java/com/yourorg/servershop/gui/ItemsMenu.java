package com.yourorg.servershop.gui;

import com.yourorg.servershop.ServerShopPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.List;

public final class ItemsMenu implements MenuView {
    private static final int PAGE_SIZE = 42;
    private final ServerShopPlugin plugin; private final String category; private final int page;
    public ItemsMenu(ServerShopPlugin plugin, String category, int page) { this.plugin = plugin; this.category = category; this.page = Math.max(0, page); }

    @Override public Inventory build() {
        int rows = Math.max(1, plugin.getConfig().getInt("gui.rows.items", 6));
        Inventory inv = Bukkit.createInventory(null, rows*9, title());
        var mats = plugin.catalog().categories().getOrDefault(category, List.of());
        int start = page * PAGE_SIZE;
        int end = Math.min(mats.size(), start + PAGE_SIZE);
        int i = 10;
        for (int idx = start; idx < end; idx++) {
            var m = mats.get(idx);
            double buy = plugin.shop().priceBuy(m);
            double sell = plugin.shop().priceSell(m);
            inv.setItem(i, GuiUtil.item(m.isItem() ? m : Material.BOOK, "&e"+m.name(), GuiUtil.lore(
                    "&7Buy: &a$"+String.format("%.2f", buy),
                    "&7Sell: &6$"+(sell>0?String.format("%.2f", sell):"-"),
                    "&8Left-click: buy 1  |  Shift-left: buy 16",
                    "&8Right-click: show price"
            )));
            i += (i % 9 == 7) ? 3 : 1;
        }
        if (page > 0) inv.setItem(rows*9-9, GuiUtil.item(Material.ARROW, "&aPrevious Page", GuiUtil.lore("&7Page "+page)));
        if (end < mats.size()) inv.setItem(rows*9-1, GuiUtil.item(Material.ARROW, "&aNext Page", GuiUtil.lore("&7Page "+(page+2))));
        inv.setItem(rows*9-5, GuiUtil.item(Material.BOOK, "&bPage "+(page+1)+"/"+Math.max(1, (int)Math.ceil(mats.size()/(double)PAGE_SIZE)), java.util.List.of()));
        return inv;
    }

    @Override public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        var it = e.getCurrentItem(); if (it == null || it.getItemMeta() == null) return;
        String name = org.bukkit.ChatColor.stripColor(it.getItemMeta().getDisplayName());
        if (name.equalsIgnoreCase("Previous Page")) { plugin.menus().openItems(p, category, Math.max(0, page-1)); return; }
        if (name.equalsIgnoreCase("Next Page")) { plugin.menus().openItems(p, category, page+1); return; }
        var m = org.bukkit.Material.matchMaterial(name);
        if (m == null) return;
        if (e.isRightClick()) {
            double buy = plugin.shop().priceBuy(m);
            p.sendMessage(plugin.prefixed(m.name()+": $"+String.format("%.2f", buy)));
            return;
        }
        int qty = e.isShiftClick() ? 16 : 1;
        double total = plugin.shop().priceBuy(m) * qty;
        double threshold = plugin.getConfig().getDouble("gui.confirmThreshold", 1000.0);
        if (total >= threshold) {
            plugin.menus().openConfirm(p, m, qty, pl -> plugin.menus().openItems(pl, category, page));
        } else {
            plugin.shop().buy(p, m, qty).ifPresent(err -> p.sendMessage(plugin.prefixed(err)));
        }
    }

    @Override public String title() { return plugin.getConfig().getString("gui.titles.items", "%category%").replace("%category%", category); }
}

