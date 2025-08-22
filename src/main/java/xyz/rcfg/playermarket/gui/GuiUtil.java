package xyz.rcfg.playermarket.gui;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class GuiUtil {
    private GuiUtil(){}
    private static ItemStack makeFiller() {
        ItemStack it = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta im = it.getItemMeta();
        im.setDisplayName(" ");
        it.setItemMeta(im);
        return it;
    }
    private static final ItemStack FILLER = makeFiller();
    public static void fill(Inventory inv) { for (int i=0;i<inv.getSize();i++) inv.setItem(i, FILLER.clone()); }
    public static ItemStack button(Material mat, String name) {
        ItemStack it = new ItemStack(mat);
        ItemMeta im = it.getItemMeta();
        im.setDisplayName(name);
        try { im.setEnchantmentGlintOverride(true); } catch (Throwable ignored) {}
        it.setItemMeta(im);
        return it;
    }
}
