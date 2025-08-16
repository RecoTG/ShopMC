package com.yourorg.servershop.gui;

import com.yourorg.servershop.ServerShopPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;

public final class CategoryMenu implements MenuView {
    private static final int PAGE_SIZE = 12;
    private final ServerShopPlugin plugin; private final int page;
    public CategoryMenu(ServerShopPlugin plugin, int page) { this.plugin = plugin; this.page = Math.max(0, page); }

    @Override public Inventory build() {
        int rows = Math.max(1, plugin.getConfig().getInt("gui.rows.categories", 3));
        Inventory inv = Bukkit.createInventory(null, rows*9, title());
        List<String> cats = new ArrayList<>();
        for (String c : plugin.catalog().categories().keySet()) if (plugin.categorySettings().isEnabled(c)) cats.add(c);
        int start = page * PAGE_SIZE;
        int end = Math.min(cats.size(), start + PAGE_SIZE);
        int[] slots = {10,11,12,13,14,15,16,19,20,21,24,25};
        int ptr = 0;
        for (int idx = start; idx < end && ptr < slots.length; idx++) {
            String cat = cats.get(idx);
            List<Material> mats = plugin.catalog().categories().get(cat);
            Material icon = (mats != null && !mats.isEmpty() && mats.get(0).isItem()) ? mats.get(0) : Material.CHEST;
            inv.setItem(slots[ptr++], GuiUtil.item(icon, "&e"+cat, GuiUtil.lore("&7Click to view items")));
        }
        if (page > 0) inv.setItem(rows*9-9, GuiUtil.item(Material.ARROW, "&aPrevious Page", GuiUtil.lore("&7Page "+page)));
        if (end < cats.size()) inv.setItem(rows*9-1, GuiUtil.item(Material.ARROW, "&aNext Page", GuiUtil.lore("&7Page "+(page+2))));
        inv.setItem(4, GuiUtil.item(Material.BOOK, "&bPage "+(page+1)+"/"+Math.max(1, (int)Math.ceil(cats.size()/(double)PAGE_SIZE)), java.util.List.of()));
        inv.setItem(rows*9-5, GuiUtil.item(Material.CLOCK, "&bWeekly Picks", GuiUtil.lore("&7Weekly discounts")));
        inv.setItem(rows*9-4, GuiUtil.item(Material.GOLD_INGOT, "&aSell Items", GuiUtil.lore("&7Sell your loot")));
        return inv;
    }

    @Override public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        var it = e.getCurrentItem(); if (it == null || it.getItemMeta() == null) return;
        String name = org.bukkit.ChatColor.stripColor(it.getItemMeta().getDisplayName());
        if (name.equalsIgnoreCase("Previous Page")) { plugin.menus().openCategories(p, Math.max(0, page-1)); return; }
        if (name.equalsIgnoreCase("Next Page")) { plugin.menus().openCategories(p, page+1); return; }
        if (name.contains("Weekly")) { plugin.menus().openWeekly(p); return; }
        if (name.contains("Sell")) { plugin.menus().openSell(p); return; }
        if (!plugin.categorySettings().isEnabled(name)) { p.sendMessage(plugin.prefixed("Category disabled.")); return; }
        plugin.menus().openItems(p, name, 0);
    }

    @Override public String title() { return plugin.getConfig().getString("gui.titles.categories", "Server Shop"); }
}

