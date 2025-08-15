package com.yourorg.servershop.dynamic;

public final class PriceState {
    public double multiplier;
    public long lastUpdateMs;

    public PriceState(double multiplier, long lastUpdateMs) {
        this.multiplier = multiplier; this.lastUpdateMs = lastUpdateMs;
    }
}
