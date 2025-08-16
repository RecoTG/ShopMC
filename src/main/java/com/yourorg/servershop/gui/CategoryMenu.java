package com.yourorg.servershop.gui;

import com.yourorg.servershop.ServerShopPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

public final class CategoryMenu implements MenuView {
    private final ServerShopPlugin plugin;
    private final Player viewer;
    public CategoryMenu(ServerShopPlugin plugin, Player viewer) { this.plugin = plugin; this.viewer = viewer; }

    @Override public Inventory build() {
        int rows = Math.max(1, plugin.getConfig().getInt("gui.rows.categories", 3));
        Inventory inv = Bukkit.createInventory(null, rows*9, title());
        int slot = 10;
        for (String cat : plugin.catalog().categories().keySet()) {
            if (!plugin.categorySettings().isEnabled(cat)) continue;
            String perm = plugin.categorySettings().permission(cat);
            if (!perm.isEmpty() && !viewer.hasPermission(perm)) continue;
            List<Material> mats = plugin.catalog().categories().get(cat);
            Material icon = (mats != null && !mats.isEmpty() && mats.get(0).isItem()) ? mats.get(0) : Material.CHEST;
            inv.setItem(slot, GuiUtil.item(icon, "&e"+cat, GuiUtil.lore("&7Click to view items")));
            slot += (slot % 9 == 7) ? 3 : 1;
        }
        inv.setItem(rows*9-5, GuiUtil.item(Material.CLOCK, "&bWeekly Picks", GuiUtil.lore("&7Weekly discounts")));
        inv.setItem(rows*9-4, GuiUtil.item(Material.GOLD_INGOT, "&aSell Items", GuiUtil.lore("&7Sell your loot")));
        return inv;
    }

    @Override public void onClick(org.bukkit.event.inventory.InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        var it = e.getCurrentItem(); if (it == null) return;
        var name = it.getItemMeta() != null ? it.getItemMeta().getDisplayName() : "";
        if (name.contains("Weekly")) { plugin.menus().openWeekly(p); return; }
        if (name.contains("Sell")) { plugin.menus().openSell(p); return; }
        String clean = org.bukkit.ChatColor.stripColor(name);
        if (!plugin.categorySettings().isEnabled(clean)) { p.sendMessage(plugin.prefixed("Category disabled.")); return; }
        String perm = plugin.categorySettings().permission(clean);
        if (!perm.isEmpty() && !p.hasPermission(perm)) {
            String msg = plugin.getConfig().getString("messages.no-category-permission", "No permission.");
            p.sendMessage(plugin.prefixed(msg));
            return;
        }
        plugin.menus().openItems(p, clean);
    }

    @Override public String title() { return plugin.getConfig().getString("gui.titles.categories", "Server Shop"); }
}
