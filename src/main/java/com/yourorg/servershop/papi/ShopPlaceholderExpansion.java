package com.yourorg.servershop.papi;

import com.yourorg.servershop.ServerShopPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * PlaceholderAPI expansion for the shop plugin.
 */
public final class ShopPlaceholderExpansion extends PlaceholderExpansion {
    private final ServerShopPlugin plugin;

    public ShopPlaceholderExpansion(ServerShopPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getIdentifier() { return "shop"; }
    @Override public String getAuthor() { return String.join(", ", plugin.getDescription().getAuthors()); }
    @Override public String getVersion() { return plugin.getDescription().getVersion(); }
    @Override public boolean persist() { return true; }
    @Override public boolean canRegister() { return true; }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (params == null) return "";
        params = params.toLowerCase();

        if (params.startsWith("price_")) {
            String matName = params.substring(6).toUpperCase();
            Material mat = Material.matchMaterial(matName);
            if (mat == null) return "";
            double price = plugin.shop().priceBuy(mat);
            return price < 0 ? "" : String.format("%.2f", price);
        }

        if (params.startsWith("sellprice_")) {
            String matName = params.substring(10).toUpperCase();
            Material mat = Material.matchMaterial(matName);
            if (mat == null) return "";
            double price = plugin.shop().priceSell(mat);
            return price < 0 ? "" : String.format("%.2f", price);
        }

        if (params.startsWith("category_multiplier_")) {
            String cat = params.substring("category_multiplier_".length());
            double mult = plugin.categorySettings().multiplier(cat);
            return String.format("%.2f", mult);
        }

        if (params.startsWith("category_enabled_")) {
            String cat = params.substring("category_enabled_".length());
            return String.valueOf(plugin.categorySettings().isEnabled(cat));
        }

        return null;
    }
}

