package com.yourorg.servershop.placeholder;

import com.yourorg.servershop.ServerShopPlugin;
import java.util.Locale;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Simple PlaceholderAPI expansion for shop data. */
public class ShopExpansion extends PlaceholderExpansion {
  private final ServerShopPlugin plugin;

  public ShopExpansion(ServerShopPlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public String getIdentifier() {
    return "shop";
  }

  @Override
  public String getAuthor() {
    return String.join(", ", plugin.getDescription().getAuthors());
  }

  @Override
  public String getVersion() {
    return plugin.getDescription().getVersion();
  }

  @Override
  public boolean canRegister() {
    return true;
  }

  @Override
  public String onPlaceholderRequest(Player player, String identifier) {
    if (identifier == null) {
      return null;
    }
    String id = identifier.toLowerCase(Locale.ROOT);
    if (id.startsWith("price_")) {
      String matName = id.substring("price_".length()).toUpperCase(Locale.ROOT);
      Material mat = Material.matchMaterial(matName);
      if (mat == null) {
        return "";
      }
      double price = plugin.shop().priceSell(mat);
      if (price < 0) {
        return "";
      }
      return String.format(Locale.US, "%.2f", price);
    }
    if (id.startsWith("multiplier_")) {
      String cat = identifier.substring("multiplier_".length());
      double mult = plugin.categorySettings().multiplier(cat);
      return String.format(Locale.US, "%.2f", mult);
    }
    return null;
  }
}
