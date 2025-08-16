package com.yourorg.servershop.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public interface MenuView {
    Inventory build(Player viewer);
    void onClick(InventoryClickEvent e);
    String title();
}
