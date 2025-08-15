package com.yourorg.servershop.gui;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;

public final class GuiUtil {
    public static ItemStack item(Material mat, String name, java.util.List<String> lore) {
        ItemStack i = new ItemStack(mat);
        ItemMeta m = i.getItemMeta();
        m.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        if (lore != null) {
            java.util.List<String> lr = new ArrayList<>();
            for (String s : lore) lr.add(ChatColor.translateAlternateColorCodes('&', s));
            m.setLore(lr);
        }
        i.setItemMeta(m);
        return i;
    }
    public static java.util.List<String> lore(String... lines) { return java.util.Arrays.asList(lines); }
}
