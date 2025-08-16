package com.yourorg.servershop.gui;

import com.yourorg.servershop.ServerShopPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

public final class MenuManager implements Listener {
    private final ServerShopPlugin plugin;
    private final java.util.Map<java.util.UUID, MenuView> open = new java.util.HashMap<>();

    public MenuManager(ServerShopPlugin plugin) { this.plugin = plugin; }

    public void openCategories(Player p) { openCategories(p, 0); }
    public void openCategories(Player p, int page) { open(p, new CategoryMenu(plugin, page)); }
    public void openItems(Player p, String category) { openItems(p, category, 0); }
    public void openItems(Player p, String category, int page) { open(p, new ItemsMenu(plugin, category, page)); }
    public void openWeekly(Player p) { if (p!=null) open(p, new WeeklyMenu(plugin)); }
    public void openSell(Player p) { open(p, new SellMenu(plugin)); }
    public void openSearch(Player p, String query, java.util.List<org.bukkit.Material> results) { open(p, new SearchMenu(plugin, query, results, 0)); }
    public void openSearch(Player p, String query, java.util.List<org.bukkit.Material> results, int page) { open(p, new SearchMenu(plugin, query, results, page)); }
    public void openConfirm(Player p, org.bukkit.Material mat, int qty, java.util.function.Consumer<Player> back) { open(p, new ConfirmMenu(plugin, mat, qty, back)); }

    private void open(Player p, MenuView view) {
        Inventory inv = view.build();
        open.put(p.getUniqueId(), view);
        p.openInventory(inv);
    }

    @EventHandler public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        MenuView view = open.get(p.getUniqueId());
        if (view == null) return;
        if (!e.getView().getTitle().equals(view.title())) return;
        e.setCancelled(true);
        view.onClick(e);
    }

    @EventHandler public void onClose(InventoryCloseEvent e) { open.remove(e.getPlayer().getUniqueId()); }
}
