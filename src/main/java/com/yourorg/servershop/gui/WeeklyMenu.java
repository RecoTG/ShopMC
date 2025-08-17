package com.yourorg.servershop.gui;

import com.yourorg.servershop.ServerShopPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public final class WeeklyMenu implements MenuView {
    private final ServerShopPlugin plugin;
    public WeeklyMenu(ServerShopPlugin plugin) { this.plugin = plugin; }

    @Override public Inventory build() {
        Inventory inv = Bukkit.createInventory(null, 6*9, title());
        int i = 10;
        for (var m : plugin.weekly().currentPicks()) {
            double regular = plugin.dynamic().buyPrice(m, plugin.catalog().get(m).map(e -> e.buyPrice()).orElse(0.0));
            double discount = plugin.getConfig().getDouble("weekly.discount", 1.0);
            double price = regular * discount;
            inv.setItem(i, GuiUtil.item(m.isItem() ? m : Material.BOOK, "&b" + m.name(), GuiUtil.lore(
                    "&7Weekly price: &a$" + String.format("%.2f", price),
                    "&7Original: &c$" + String.format("%.2f", regular),
                    "&8Left-click: buy 1  |  Shift-left: buy 16")));
            i += (i % 9 == 7) ? 3 : 1;
        }
        return inv;
    }

    @Override public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        var it = e.getCurrentItem(); if (it == null) return;
        var m = org.bukkit.Material.matchMaterial(org.bukkit.ChatColor.stripColor(it.getItemMeta().getDisplayName()));
        if (m == null) return;
        int qty = e.isShiftClick() ? 16 : 1;
        plugin.shop().buy(p, m, qty).ifPresent(err -> p.sendMessage(plugin.prefixed(err)));
    }

    @Override public String title() { return plugin.getConfig().getString("gui.titles.weekly", "Weekly Picks"); }
}
