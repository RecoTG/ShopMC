package com.yourorg.servershop.gui;

import com.yourorg.servershop.ServerShopPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.function.Consumer;

public final class ConfirmMenu implements MenuView {
    private final ServerShopPlugin plugin; private final Material mat; private final int qty; private final Consumer<Player> back;
    public ConfirmMenu(ServerShopPlugin plugin, Material mat, int qty, Consumer<Player> back) { this.plugin = plugin; this.mat = mat; this.qty = qty; this.back = back; }

    @Override public Inventory build() {
        Inventory inv = Bukkit.createInventory(null, 3*9, title());
        double unit = plugin.shop().priceBuy(mat);
        double total = unit * qty;
        inv.setItem(13, GuiUtil.item(mat.isItem()?mat:Material.BOOK, "&e"+mat.name(), GuiUtil.lore(
                "&7Qty: &f"+qty,
                "&7Price: &a$"+String.format("%.2f", total))));
        inv.setItem(11, GuiUtil.item(Material.LIME_WOOL, "&aConfirm", GuiUtil.lore("&7Complete purchase")));
        inv.setItem(15, GuiUtil.item(Material.RED_WOOL, "&cCancel", GuiUtil.lore("&7Go back")));
        return inv;
    }

    @Override public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        var it = e.getCurrentItem(); if (it == null || it.getItemMeta() == null) return;
        String name = org.bukkit.ChatColor.stripColor(it.getItemMeta().getDisplayName());
        if (name.equalsIgnoreCase("Confirm")) {
            plugin.shop().buy(p, mat, qty).ifPresent(err -> p.sendMessage(plugin.prefixed(err)));
            back.accept(p);
        } else if (name.equalsIgnoreCase("Cancel")) {
            back.accept(p);
        }
    }

    @Override public String title() { return plugin.getConfig().getString("gui.titles.confirm", "Confirm Purchase"); }
}

