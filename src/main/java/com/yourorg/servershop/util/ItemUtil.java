package com.yourorg.servershop.util;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class ItemUtil {
    private ItemUtil() {}

    public static boolean hasCustomMeta(ItemStack stack) {
        if (stack == null) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        var factory = Bukkit.getItemFactory();
        ItemMeta def = factory.getItemMeta(stack.getType());
        return !factory.equals(meta, def);
    }
}
