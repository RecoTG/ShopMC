package com.yourorg.servershop.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public interface MenuView {
    Inventory build();
    void onClick(InventoryClickEvent e);
    String title();
}
