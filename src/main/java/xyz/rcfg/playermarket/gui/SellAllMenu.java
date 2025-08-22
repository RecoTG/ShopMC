package xyz.rcfg.playermarket.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class SellAllMenu implements InventoryHolder {
    private final Player p;
    private Inventory inv;
    public SellAllMenu(Player p){ this.p=p; }
    public void open(){
        inv = Bukkit.createInventory(this, 36, ChatColor.GREEN + "Sell All");
        ItemStack summary = new ItemStack(Material.PAPER);
        ItemMeta im = summary.getItemMeta();
        im.setDisplayName("§eSummary");
        List<String> lore = new ArrayList<>(); lore.add("§7Place items to sell, then close."); im.setLore(lore);
        summary.setItemMeta(im); inv.setItem(35, summary);
        p.openInventory(inv);
    }
    @Override public Inventory getInventory(){ return inv; }
}
