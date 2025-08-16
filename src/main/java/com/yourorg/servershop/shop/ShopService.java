package com.yourorg.servershop.shop;

import com.yourorg.servershop.ServerShopPlugin;
import com.yourorg.servershop.logging.Transaction;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public final class ShopService {
    private final ServerShopPlugin plugin;

    public ShopService(ServerShopPlugin plugin) { this.plugin = plugin; }

    public Optional<String> buy(Player p, Material mat, int qty) {
        if (!Bukkit.isPrimaryThread()) {
            try {
                return Bukkit.getScheduler().callSyncMethod(plugin, () -> buySync(p, mat, qty)).get();
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
                return Optional.of("Internal error");
            }
        }
        return buySync(p, mat, qty);
    }

    private Optional<String> buySync(Player p, Material mat, int qty) {
        var opt = plugin.catalog().get(mat);
        if (opt.isEmpty() || !opt.get().canBuy()) return Optional.of(msg("not-for-sale").replace("%material%", mat.name()));
        String cat = plugin.catalog().categoryOf(mat);
        if (!plugin.categorySettings().isEnabled(cat)) return Optional.of("Category disabled: "+cat);
        double unit = plugin.dynamic().buyPrice(mat, opt.get().buyPrice());
        double total = unit * qty;
        var econ = plugin.economy();
        if (econ.getBalance(p) + 1e-9 < total) {
            double need = Math.max(0, total - econ.getBalance(p));
            return Optional.of(msg("not-enough-money").replace("%amount%", fmt(need)));
        }
        var r = econ.withdrawPlayer(p, total);
        if (!r.transactionSuccess()) return Optional.of("Payment failed: " + r.errorMessage);
        var stack = new ItemStack(mat, qty);
        var left = p.getInventory().addItem(stack);
        if (!left.isEmpty()) { econ.depositPlayer(p, total); return Optional.of("Inventory full."); }
        plugin.dynamic().adjustOnBuy(mat, qty);
        plugin.logger().logAsync(new Transaction(Instant.now(), p.getName(), Transaction.Type.BUY, mat, qty, total));
        p.sendMessage(plugin.prefixed(msg("purchased").replace("%qty%", String.valueOf(qty)).replace("%material%", mat.name()).replace("%price%", fmt(total))));
        return Optional.empty();
    }

    public Optional<String> sell(Player p, Material mat, int qty) {
        if (!Bukkit.isPrimaryThread()) {
            try {
                return Bukkit.getScheduler().callSyncMethod(plugin, () -> sellSync(p, mat, qty)).get();
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
                return Optional.of("Internal error");
            }
        }
        return sellSync(p, mat, qty);
    }

    private Optional<String> sellSync(Player p, Material mat, int qty) {
        var opt = plugin.catalog().get(mat);
        if (opt.isEmpty() || !opt.get().canSell()) return Optional.of(msg("not-sellable").replace("%material%", mat.name()));
        String cat = plugin.catalog().categoryOf(mat);
        if (!plugin.categorySettings().isEnabled(cat)) return Optional.of("Category disabled: "+cat);
        int removed = removeFromInventory(p, mat, qty);
        if (removed <= 0) return Optional.of("You don't have that.");
        double unit = plugin.dynamic().sellPrice(mat, opt.get().sellPrice());
        double total = unit * removed;
        plugin.economy().depositPlayer(p, total);
        plugin.dynamic().adjustOnSell(mat, removed);
        plugin.logger().logAsync(new Transaction(Instant.now(), p.getName(), Transaction.Type.SELL, mat, removed, total));
        p.sendMessage(plugin.prefixed(msg("sold").replace("%qty%", String.valueOf(removed)).replace("%material%", mat.name()).replace("%price%", fmt(total))));
        return Optional.empty();
    }

    public double priceBuy(Material mat) {
        var e = plugin.catalog().get(mat).orElse(null); if (e == null || !e.canBuy()) return -1;
        return plugin.dynamic().buyPrice(mat, e.buyPrice());
    }

    public double priceSell(Material mat) {
        var e = plugin.catalog().get(mat).orElse(null); if (e == null || !e.canSell()) return -1;
        return plugin.dynamic().sellPrice(mat, e.sellPrice());
    }

    private int removeFromInventory(Player p, Material mat, int qty) {
        int remaining = qty;
        for (int i = 0; i < p.getInventory().getSize(); i++) {
            var stack = p.getInventory().getItem(i);
            if (stack == null || stack.getType() != mat) continue;
            int take = Math.min(stack.getAmount(), remaining);
            stack.setAmount(stack.getAmount() - take);
            if (stack.getAmount() <= 0) p.getInventory().setItem(i, null);
            remaining -= take;
            if (remaining <= 0) break;
        }
        return qty - remaining;
    }

    private static String fmt(double v) { return String.format("%.2f", v); }
    private String msg(String key) { return plugin.getConfig().getString("messages." + key, key); }
}
