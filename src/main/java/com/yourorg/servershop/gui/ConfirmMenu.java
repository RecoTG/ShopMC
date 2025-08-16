package com.yourorg.servershop.gui;

import com.yourorg.servershop.ServerShopPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.function.Consumer;

public final class ConfirmMenu implements MenuView {
    private final ServerShopPlugin plugin;
    private final Material material;
    private final Consumer<Player> back;

    public ConfirmMenu(ServerShopPlugin plugin, Material material, Consumer<Player> back) {
        this.plugin = plugin;
        this.material = material;
        this.back = back;
    }

    @Override public Inventory build() {
        Inventory inv = Bukkit.createInventory(null, 3*9, title());
        double unit = plugin.shop().priceBuy(material);
        inv.setItem(13, GuiUtil.item(material.isItem()?material:Material.BOOK, "&e"+material.name(), GuiUtil.lore(
                "&7Price each: &a$"+String.format("%.2f", unit))));
        inv.setItem(11, GuiUtil.item(Material.EMERALD, "&aBuy 1", GuiUtil.lore("&7Cost: &a$"+String.format("%.2f", unit))));
        inv.setItem(15, GuiUtil.item(Material.EMERALD_BLOCK, "&aBuy 16", GuiUtil.lore("&7Cost: &a$"+String.format("%.2f", unit*16))));
        inv.setItem(18, GuiUtil.item(Material.ARROW, "&cBack", GuiUtil.lore("&7Return")));
        return inv;
    }

    @Override public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        var it = e.getCurrentItem(); if (it == null || it.getItemMeta() == null) return;
        String name = org.bukkit.ChatColor.stripColor(it.getItemMeta().getDisplayName());
        if (name.equalsIgnoreCase("Back")) { back.accept(p); return; }
        int qty = name.contains("16") ? 16 : 1;
        plugin.shop().buy(p, material, qty).ifPresent(err -> p.sendMessage(plugin.prefixed(err)));
        back.accept(p);
    }

    @Override public String title() {
        return plugin.getConfig().getString("gui.titles.confirm", "Confirm Purchase");
    }
}

