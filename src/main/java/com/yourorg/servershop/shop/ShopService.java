package com.yourorg.servershop.shop;

import com.yourorg.servershop.ServerShopPlugin;
import com.yourorg.servershop.logging.Transaction;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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
        double unit = plugin.dynamic().buyPrice(mat, opt.get().buyPrice());
        double subtotal = unit * qty;
        double tax = subtotal * plugin.taxRate();
        double total = subtotal + tax;
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
        var priceComponent = plugin.mini().deserialize("<green>$" + fmt(total) + "</green>")
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(plugin.mini().deserialize(
                        "<gray>$" + fmt(unit) + " × " + qty + " → tax $" + fmt(tax) + " → total $" + fmt(total) + "</gray>")));
        var msg = plugin.mini().deserialize("Bought " + qty + "x " + mat.name() + " for ").append(priceComponent);
        plugin.adventure().player(p).sendMessage(plugin.prefixed(msg));
        return Optional.empty();
    }

    public Optional<String> sell(Player p, Material mat, int qty) {
        var opt = plugin.catalog().get(mat);
        if (opt.isEmpty() || !opt.get().canSell()) return Optional.of(msg("not-sellable").replace("%material%", mat.name()));
        String cat = plugin.catalog().categoryOf(mat);
        if (!plugin.categorySettings().isEnabled(cat)) return Optional.of("Category disabled: "+cat);
        int removed = removeFromInventory(p, mat, qty);
        if (removed <= 0) return Optional.of("You don't have that.");
        double unit = plugin.dynamic().sellPrice(mat, opt.get().sellPrice());
        double subtotal = unit * removed;
        double tax = subtotal * plugin.taxRate();
        double total = subtotal - tax;
        plugin.economy().depositPlayer(p, total);
        plugin.dynamic().adjustOnSell(mat, removed);
        plugin.logger().logAsync(new Transaction(Instant.now(), p.getName(), Transaction.Type.SELL, mat, removed, total));
        var priceComponent = plugin.mini().deserialize("<green>$" + fmt(total) + "</green>")
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(plugin.mini().deserialize(
                        "<gray>$" + fmt(unit) + " × " + removed + " → tax $" + fmt(tax) + " → total $" + fmt(total) + "</gray>")));
        var msg = plugin.mini().deserialize("Sold " + removed + "x " + mat.name() + " for ").append(priceComponent);
        plugin.adventure().player(p).sendMessage(plugin.prefixed(msg));
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
