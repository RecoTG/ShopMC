package com.yourorg.servershop.gui;

import com.yourorg.servershop.ServerShopPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public final class SellMenu implements MenuView {
    private final ServerShopPlugin plugin;
    public SellMenu(ServerShopPlugin plugin) { this.plugin = plugin; }

    @Override public Inventory build() {
        return Bukkit.createInventory(null, 6*9, title());
    }

    @Override public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        var it = e.getCurrentItem(); if (it == null) return;
        var m = it.getType(); if (m == Material.AIR) return;
        if (it.getItemMeta() != null && org.bukkit.ChatColor.stripColor(it.getItemMeta().getDisplayName()).equalsIgnoreCase("Sell All")) {
            sellAll(p); return;
        }
        int qty = e.isShiftClick() ? countOf(p, m) : Math.min(64, it.getAmount());
        plugin.shop().sell(p, m, qty).ifPresent(err -> p.sendMessage(plugin.prefixed(err)));
        refresh(p);
    }

    @Override public String title() { return plugin.getConfig().getString("gui.titles.sell", "Sell Items"); }

    public void refresh(Player p) {
        Inventory inv = p.getOpenInventory().getTopInventory();
        inv.clear();
        java.util.Map<Material, Integer> map = new java.util.TreeMap<>(java.util.Comparator.comparing(Enum::name));
        for (ItemStack s : p.getInventory().getContents()) {
            if (s == null) continue; var m = s.getType();
            var e = plugin.catalog().get(m).orElse(null); if (e == null || !e.canSell()) continue;
            map.put(m, map.getOrDefault(m, 0) + s.getAmount());
        }
        int i = 10;
        for (var ent : map.entrySet()) {
            double unit = plugin.shop().priceSell(ent.getKey());
            inv.setItem(i, GuiUtil.item(ent.getKey().isItem()?ent.getKey():Material.PAPER, "&a"+ent.getKey().name(), GuiUtil.lore(
                    "&7Unit: &6$"+String.format("%.2f", unit), "&7You have: &e"+ent.getValue(), "&8Click: sell stack  |  Shift: sell all of this")));
            i += (i % 9 == 7) ? 3 : 1;
        }
        inv.setItem(6*9-5, GuiUtil.item(Material.BARRIER, "&cSell All", GuiUtil.lore("&7Sells every sellable item")));
    }

    private void sellAll(Player p) {
        double total = 0.0; int stacks = 0;
        for (int i = 0; i < p.getInventory().getSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s == null) continue; var m = s.getType();
            var e = plugin.catalog().get(m).orElse(null); if (e == null || !e.canSell()) continue;
            int qty = s.getAmount();
            double unit = plugin.shop().priceSell(m);
            double amount = unit * qty;
            total += amount; stacks++;
            p.getInventory().setItem(i, null);
            plugin.logger().logAsync(new com.yourorg.servershop.logging.Transaction(java.time.Instant.now(), p.getName(), com.yourorg.servershop.logging.Transaction.Type.SELL, m, qty, amount));
            plugin.dynamic().adjustOnSell(m, qty);
        }
        if (total > 0) plugin.economy().depositPlayer(p, total);
        p.sendMessage(plugin.prefixed(plugin.getConfig().getString("messages.soldall").replace("%count%", String.valueOf(stacks)).replace("%total%", String.format("%.2f", total))));
        refresh(p);
    }

    private static int countOf(Player p, Material m){ int c=0; for (ItemStack s: p.getInventory().getContents()) if (s!=null && s.getType()==m) c+=s.getAmount(); return c; }
}
