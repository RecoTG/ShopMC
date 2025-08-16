package com.yourorg.servershop.gui;

import com.yourorg.servershop.ServerShopPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.List;

public final class ItemsMenu implements MenuView {
    private final ServerShopPlugin plugin; private final String category;
    public ItemsMenu(ServerShopPlugin plugin, String category) { this.plugin = plugin; this.category = category; }

    @Override public Inventory build(Player viewer) {
        int rows = Math.max(1, plugin.getConfig().getInt("gui.rows.items", 6));
        Inventory inv = Bukkit.createInventory(null, rows*9, title());
        var mats = plugin.catalog().categories().getOrDefault(category, List.of());
        int i = 10;
        for (var m : mats) {
            double buy = plugin.shop().priceBuy(viewer, m);
            double sell = plugin.shop().priceSell(viewer, m);
            inv.setItem(i, GuiUtil.item(m.isItem() ? m : Material.BOOK, "&e"+m.name(), GuiUtil.lore(
                    "&7Buy: &a$"+String.format("%.2f", buy),
                    "&7Sell: &6$"+(sell>0?String.format("%.2f", sell):"-"),
                    "&8Left-click: buy 1  |  Shift-left: buy 16",
                    "&8Right-click: show price"
            )));
            i += (i % 9 == 7) ? 3 : 1;
        }
        return inv;
    }

    @Override public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        var it = e.getCurrentItem(); if (it == null) return;
        var mat = it.getType();
        if (mat == null || mat == Material.AIR) return;
        var m = org.bukkit.Material.matchMaterial(org.bukkit.ChatColor.stripColor(it.getItemMeta().getDisplayName()));
        if (m == null) return;
        if (e.isRightClick()) {
            double buy = plugin.shop().priceBuy(p, m);
            p.sendMessage(plugin.prefixed(m.name()+": $"+String.format("%.2f", buy)));
            return;
        }
        int qty = e.isShiftClick() ? 16 : 1;
        plugin.shop().buy(p, m, qty).ifPresent(err -> p.sendMessage(plugin.prefixed(err)));
    }

    @Override public String title() { return plugin.getConfig().getString("gui.titles.items", "%category%").replace("%category%", category); }
}
