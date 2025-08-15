package com.yourorg.servershop.shop;

import org.bukkit.Material;

public final class ItemEntry {
    private final Material material;
    private final double buyPrice;
    private final double sellPrice;

    public ItemEntry(Material material, double buyPrice, double sellPrice) {
        this.material = material; this.buyPrice = buyPrice; this.sellPrice = sellPrice;
    }
    public Material material() { return material; }
    public double buyPrice() { return buyPrice; }
    public double sellPrice() { return sellPrice; }
    public boolean canBuy() { return buyPrice > 0; }
    public boolean canSell() { return sellPrice > 0; }
}
