package com.yourorg.servershop.shop;

import com.yourorg.servershop.ServerShopPlugin;
import com.yourorg.servershop.logging.Transaction;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

public final class ShopService {
    private final ServerShopPlugin plugin;
    private final int capPerTxn;
    private final int capPerDay;
    private final java.util.Map<java.util.UUID, Integer> dailySold = new java.util.HashMap<>();
    private long currentDay = today();

    public ShopService(ServerShopPlugin plugin) {
        this.plugin = plugin;
        var caps = plugin.getConfig().getConfigurationSection("sellCaps");
        this.capPerTxn = caps == null ? Integer.MAX_VALUE : caps.getInt("perTransaction", Integer.MAX_VALUE);
        this.capPerDay = caps == null ? Integer.MAX_VALUE : caps.getInt("perDay", Integer.MAX_VALUE);
    }

    public Optional<String> buy(Player p, Material mat, int qty) {
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
        resetIfNewDay();
        var opt = plugin.catalog().get(mat);
        if (opt.isEmpty() || !opt.get().canSell()) return Optional.of(msg("not-sellable").replace("%material%", mat.name()));
        String cat = plugin.catalog().categoryOf(mat);
        if (!plugin.categorySettings().isEnabled(cat)) return Optional.of("Category disabled: "+cat);

        int allowed = qty;
        if (capPerTxn > 0) allowed = Math.min(allowed, capPerTxn);
        int sold = dailySold.getOrDefault(p.getUniqueId(), 0);
        if (capPerDay > 0) {
            int remaining = capPerDay - sold;
            if (remaining <= 0) return Optional.of("Daily sell limit reached.");
            allowed = Math.min(allowed, remaining);
        }

        int removed = removeFromInventory(p, mat, allowed);
        if (removed <= 0) return Optional.of("You don't have that.");
        double unit = plugin.dynamic().sellPrice(mat, opt.get().sellPrice());
        double total = unit * removed;
        plugin.economy().depositPlayer(p, total);
        plugin.dynamic().adjustOnSell(mat, removed);
        plugin.logger().logAsync(new Transaction(Instant.now(), p.getName(), Transaction.Type.SELL, mat, removed, total));
        dailySold.put(p.getUniqueId(), sold + removed);
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

    private static long today() { return LocalDate.now().toEpochDay(); }

    private void resetIfNewDay() {
        long t = today();
        if (t != currentDay) {
            currentDay = t;
            dailySold.clear();
        }
    }
}
