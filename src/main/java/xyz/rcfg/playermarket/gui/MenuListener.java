package xyz.rcfg.playermarket.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import xyz.rcfg.playermarket.PlayerMarketPlugin;
import xyz.rcfg.playermarket.service.PricingService;
import xyz.rcfg.playermarket.service.ShopService;

public class MenuListener implements Listener {
    private final ShopService svc;
    public MenuListener(ShopService svc){ this.svc = svc; }

    private int roomFor(Player p, Material m, int maxStack){
        int space = 0;
        var inv = p.getInventory();
        for (int i=0;i<inv.getSize();i++){
            ItemStack s = inv.getItem(i);
            if (s == null || s.getType() == Material.AIR) space += maxStack;
            else if (s.getType() == m && s.getAmount() < maxStack) space += (maxStack - s.getAmount());
        }
        return space;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e){
        Inventory top = e.getView().getTopInventory(); if (top==null) return;
        InventoryHolder holder = top.getHolder();
        if (!(holder instanceof RootMenu) && !(holder instanceof CategoryMenu) && !(holder instanceof SellAllMenu)) return;

        e.setCancelled(true);

        if (holder instanceof RootMenu) {
            ItemStack it = e.getCurrentItem(); if (it==null || it.getType()==Material.AIR) return;
            if (!it.hasItemMeta() || !it.getItemMeta().hasDisplayName()) return;
            String name = it.getItemMeta().getDisplayName();
            for (xyz.rcfg.playermarket.service.ShopService.Category c : svc.getCategories().values()) {
                String dn = "§e§l"+c.name();
                if (dn.equals(name)) { svc.openCategory((Player)e.getWhoClicked(), c, 0); return; }
            }
            return;
        }

        if (holder instanceof CategoryMenu cm) {
            ItemStack it = e.getCurrentItem(); if (it==null || it.getType()==Material.AIR) return;
            int raw = e.getRawSlot();

            final int pageSize = 28;
            int maxPage = Math.max(0, (cm.getCategory().items().size() - 1) / pageSize);
            if (raw == 49) { svc.openRoot((Player)e.getWhoClicked()); return; }
            if (raw == 45) { if (cm.getPage() > 0) svc.openCategory((Player)e.getWhoClicked(), cm.getCategory(), cm.getPage()-1); return; }
            if (raw == 53) { if (cm.getPage() < maxPage) svc.openCategory((Player)e.getWhoClicked(), cm.getCategory(), cm.getPage()+1); return; }

            if (raw >= top.getSize()) return;
            if (it.getType().name().endsWith("GLASS_PANE")) return;

            Material m = it.getType();
            var info = svc.getItemInfo(m);
            if (info == null) { ((Player)e.getWhoClicked()).sendMessage("§cThis item is not tradable here."); return; }

            long stock = svc.getStock().getStock(m.name());
            PricingService.Prices prices = svc.getPricing().computePrices(info, stock, false);
            Player p = (Player)e.getWhoClicked();
            int max = it.getMaxStackSize();
            int qty = 1;
            if (e.getClick() == ClickType.SHIFT_LEFT || e.getClick()==ClickType.SHIFT_RIGHT) qty = Math.min(max, 64);
            else if (e.getClick() == ClickType.MIDDLE) qty = Math.min(16, max);

            if (e.isLeftClick()) {
                if (stock <= 0) { p.sendMessage("§cOut of stock."); return; }
                int cap = roomFor(p, m, max);
                qty = Math.min(qty, cap);
                qty = (int)Math.min(qty, stock);
                if (qty <= 0) { p.sendMessage("§cNot enough stock or inventory space."); return; }

                double cost = prices.buyUnit() * qty;
                var eco = svc.getPlugin().economy();
                if (eco == null || eco.getBalance(p) < cost) { p.sendMessage("§cYou don’t have enough money."); return; }

                var r = eco.withdrawPlayer(p, cost);
                if (!r.transactionSuccess()) { p.sendMessage("§cTransaction failed."); return; }

                p.getInventory().addItem(new ItemStack(m, qty));
                svc.getStock().addStock(m.name(), -qty);
                p.sendMessage("§aBought "+qty+"x "+m.name()+" for §6$"+String.format("%.2f", cost));
                return;
            }

            if (e.isRightClick()) {
                int have = p.getInventory().all(m).values().stream().mapToInt(ItemStack::getAmount).sum();
                qty = Math.min(qty, have);
                if (qty <= 0) { p.sendMessage("§cYou have no "+m.name()+" to sell."); return; }

                double gain = prices.sellUnit() * qty;

                int toRemove = qty;
                for (int i=0;i<p.getInventory().getSize() && toRemove>0;i++){
                    ItemStack slot = p.getInventory().getItem(i);
                    if (slot!=null && slot.getType()==m){
                        int take = Math.min(toRemove, slot.getAmount());
                        slot.setAmount(slot.getAmount()-take);
                        if (slot.getAmount()==0) p.getInventory().setItem(i, null);
                        toRemove -= take;
                    }
                }
                var eco = svc.getPlugin().economy();
                if (eco != null) eco.depositPlayer(p, gain);
                svc.getStock().addStock(m.name(), qty);
                p.sendMessage("§aSold "+qty+"x "+m.name()+" for §6$"+String.format("%.2f", gain));
                return;
            }
            return;
        }

        if (holder instanceof SellAllMenu) {
            int raw = e.getRawSlot(); int size = top.getSize();
            if (raw == size - 1) { e.setCancelled(true); return; } // summary
            e.setCancelled(false);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e){
        if (!(e.getInventory().getHolder() instanceof SellAllMenu)) return;
        var top = e.getInventory(); var p = (org.bukkit.entity.Player)e.getPlayer();
        long total=0; double money=0.0; int unique=0;
        for (int i=0;i<top.getSize();i++){
            if (i==top.getSize()-1) continue;
            ItemStack it = top.getItem(i);
            if (it==null || it.getType()==Material.AIR) continue;
            Material m = it.getType();
            var info = svc.getItemInfo(m);
            long amt = it.getAmount();
            long stock = svc.getStock().getStock(m.name());
            var price = svc.getPricing().computePrices(info, stock, false);
            if (info == null) { p.getInventory().addItem(it); continue; }

            money += price.sellUnit() * amt; total += amt; unique++;
            svc.getStock().addStock(m.name(), amt);
        }
        if (money>0){ var eco = svc.getPlugin().economy(); if (eco != null) eco.depositPlayer(p, money);
            p.sendMessage("§aSold "+total+" items ("+unique+" types) for §6$"+String.format("%.2f", money)); }
    }
}
