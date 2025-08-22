package xyz.rcfg.playermarket.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import xyz.rcfg.playermarket.service.PricingService;
import xyz.rcfg.playermarket.service.ShopService;

import java.util.ArrayList;
import java.util.List;

public class CategoryMenu implements InventoryHolder {
    private final ShopService svc;
    private final Player p;
    private final ShopService.Category cat;
    private final int page;
    private Inventory inv;

    private static final int[] CONTENT_SLOTS = { 10,11,12,13,14,15,16, 19,20,21,22,23,24,25, 28,29,30,31,32,33,34, 37,38,39,40,41,42,43 };

    public CategoryMenu(ShopService svc, Player p, ShopService.Category cat, int page){ this.svc=svc; this.p=p; this.cat=cat; this.page=page; }
    public ShopService.Category getCategory(){ return cat; }
    public int getPage(){ return page; }

    public void open(){
        inv = Bukkit.createInventory(this, 54, ChatColor.BLUE + cat.name());
        xyz.rcfg.playermarket.gui.GuiUtil.fill(inv);

        int pageSize = CONTENT_SLOTS.length;
        int maxPage = Math.max(0, (cat.items().size() - 1) / pageSize);
        inv.setItem(49, xyz.rcfg.playermarket.gui.GuiUtil.button(Material.BARRIER,"§cBack"));
        inv.setItem(45, (page > 0) ? xyz.rcfg.playermarket.gui.GuiUtil.button(Material.ARROW,  "§ePrevious")
                                   : xyz.rcfg.playermarket.gui.GuiUtil.button(Material.GRAY_STAINED_GLASS_PANE, "§7No previous"));
        inv.setItem(53, (page < maxPage) ? xyz.rcfg.playermarket.gui.GuiUtil.button(Material.ARROW,  "§eNext")
                                         : xyz.rcfg.playermarket.gui.GuiUtil.button(Material.GRAY_STAINED_GLASS_PANE, "§7No next"));

        int start = page * pageSize;
        for (int i=0;i< CONTENT_SLOTS.length;i++){
            int idx = start+i; if (idx>=cat.items().size()) break;
            Material m = cat.items().get(idx);
            ItemStack it = new ItemStack(m);
            long stock = svc.getStock().getStock(m.name());
            PricingService.Prices prices = svc.getPricing().computePrices(svc.getItemInfo(m), stock, false);
            int max = it.getMaxStackSize(); int s16 = Math.min(16, max); int s64 = Math.min(64, max);
            List<String> lore = new ArrayList<>();
            lore.add("§7Buy 1: §a$"+String.format("%.2f", prices.buyUnit()));
            lore.add("§7Buy "+s16+": §a$"+String.format("%.2f", prices.buyUnit()*s16));
            lore.add("§7Buy "+s64+": §a$"+String.format("%.2f", prices.buyUnit()*s64));
            lore.add("");
            lore.add("§7Sell 1: §a$"+String.format("%.2f", prices.sellUnit()));
            lore.add("§7Sell "+s16+": §a$"+String.format("%.2f", prices.sellUnit()*s16));
            lore.add("§7Sell "+s64+": §a$"+String.format("%.2f", prices.sellUnit()*s64));
            lore.add("");
            lore.add("§7Stock: §e"+stock);
            ItemMeta im = it.getItemMeta(); im.setLore(lore); it.setItemMeta(im);
            inv.setItem(CONTENT_SLOTS[i], it);
        }
        p.openInventory(inv);
    }
    @Override public Inventory getInventory(){ return inv; }
}
