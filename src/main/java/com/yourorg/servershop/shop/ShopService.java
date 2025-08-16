package com.yourorg.servershop.shop;

import com.yourorg.servershop.ServerShopPlugin;
import com.yourorg.servershop.logging.Transaction;
import com.yourorg.servershop.util.CurrencyUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

public final class ShopService {
    private final ServerShopPlugin plugin;

    public ShopService(ServerShopPlugin plugin) { this.plugin = plugin; }

    public Optional<String> buy(Player p, Material mat, int qty) {
        var opt = plugin.catalog().get(mat);
        if (opt.isEmpty() || !opt.get().canBuy()) return Optional.of(msg("not-for-sale").replace("%material%", mat.name()));
        String cat = plugin.catalog().categoryOf(mat);
        if (!plugin.categorySettings().isEnabled(cat)) return Optional.of("Category disabled: "+cat);
        BigDecimal unit = CurrencyUtil.bd(plugin.dynamic().buyPrice(mat, opt.get().buyPrice()));
        BigDecimal total = CurrencyUtil.zeroIfNegative(CurrencyUtil.multiply(unit, qty));
        var econ = plugin.economy();
        BigDecimal balance = CurrencyUtil.bd(econ.getBalance(p));
        if (balance.compareTo(total) < 0) {
            BigDecimal need = CurrencyUtil.zeroIfNegative(total.subtract(balance));
            return Optional.of(msg("not-enough-money").replace("%amount%", CurrencyUtil.format(need)));
        }
        var r = econ.withdrawPlayer(p, total.doubleValue());
        if (!r.transactionSuccess()) return Optional.of("Payment failed: " + r.errorMessage);
        var stack = new ItemStack(mat, qty);
        var left = p.getInventory().addItem(stack);
        if (!left.isEmpty()) { econ.depositPlayer(p, total.doubleValue()); return Optional.of("Inventory full."); }
        plugin.dynamic().adjustOnBuy(mat, qty);
        plugin.logger().logAsync(new Transaction(Instant.now(), p.getName(), Transaction.Type.BUY, mat, qty, total));
        p.sendMessage(plugin.prefixed(msg("purchased").replace("%qty%", String.valueOf(qty)).replace("%material%", mat.name()).replace("%price%", CurrencyUtil.format(total))));
        return Optional.empty();
    }

    public Optional<String> sell(Player p, Material mat, int qty) {
        var opt = plugin.catalog().get(mat);
        if (opt.isEmpty() || !opt.get().canSell()) return Optional.of(msg("not-sellable").replace("%material%", mat.name()));
        String cat = plugin.catalog().categoryOf(mat);
        if (!plugin.categorySettings().isEnabled(cat)) return Optional.of("Category disabled: "+cat);
        int removed = removeFromInventory(p, mat, qty);
        if (removed <= 0) return Optional.of("You don't have that.");
        BigDecimal unit = CurrencyUtil.bd(plugin.dynamic().sellPrice(mat, opt.get().sellPrice()));
        BigDecimal total = CurrencyUtil.zeroIfNegative(CurrencyUtil.multiply(unit, removed));
        plugin.economy().depositPlayer(p, total.doubleValue());
        plugin.dynamic().adjustOnSell(mat, removed);
        plugin.logger().logAsync(new Transaction(Instant.now(), p.getName(), Transaction.Type.SELL, mat, removed, total));
        p.sendMessage(plugin.prefixed(msg("sold").replace("%qty%", String.valueOf(removed)).replace("%material%", mat.name()).replace("%price%", CurrencyUtil.format(total))));
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

    private String msg(String key) { return plugin.getConfig().getString("messages." + key, key); }
}
