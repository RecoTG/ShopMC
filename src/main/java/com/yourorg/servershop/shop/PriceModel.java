package com.yourorg.servershop.shop;

import org.bukkit.configuration.file.FileConfiguration;

public final class PriceModel {
    private final double minFactor, maxFactor, sellStep, sellMultiplier;

    public PriceModel(FileConfiguration cfg) {
        var sec = cfg.getConfigurationSection("priceModel");
        this.minFactor = sec.getDouble("minFactor", 0.5);
        this.maxFactor = sec.getDouble("maxFactor", 1.5);
        this.sellStep = sec.getDouble("sellStep", 0.01);
        this.sellMultiplier = sec.getDouble("sellMultiplier", 0.8);
    }

    public double clampToBounds(double value, double base) {
        double min = Math.max(0.01, base * minFactor);
        double max = Math.max(min, base * maxFactor);
        return Math.max(min, Math.min(max, value));
    }

    public double afterSold(double current) { return current * (1.0 - sellStep); }
    public double sellMultiplier() { return sellMultiplier; }
}
