package xyz.rcfg.playermarket.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import xyz.rcfg.playermarket.service.ShopService;

import java.util.ArrayList;
import java.util.List;

public class RootMenu implements InventoryHolder {
    private final ShopService svc;
    private final Player p;
    private Inventory inv;
    private static final int[] CENTERED = { 10,12,14,16, 19,21,23,25, 28,30,32,34, 37,39,41,43 };
    public RootMenu(ShopService svc, Player p){ this.svc=svc; this.p=p; }
    public void open(){
        inv = Bukkit.createInventory(this, 54, ChatColor.GOLD + "Player Market");
        xyz.rcfg.playermarket.gui.GuiUtil.fill(inv);
        List<ShopService.Category> cats = new ArrayList<>(svc.getCategories().values());
        int si=0;
        for (ShopService.Category c : cats) {
            ItemStack it = new ItemStack(c.icon());
            ItemMeta im = it.getItemMeta();
            im.setDisplayName("§e§l"+c.name());
            try { im.setEnchantmentGlintOverride(true); } catch (Throwable ignored) {}
            it.setItemMeta(im);
            int slot = (c.id().equalsIgnoreCase("weekly")) ? 40 : (si< CENTERED.length ? CENTERED[si++] : -1);
            if (slot>=0) inv.setItem(slot, it);
        }
        p.openInventory(inv);
    }
    @Override public Inventory getInventory(){ return inv; }
}
