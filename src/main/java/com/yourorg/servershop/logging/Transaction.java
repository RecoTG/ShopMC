package com.yourorg.servershop.logging;

import org.bukkit.Material;
import java.time.Instant;

public final class Transaction {
    public enum Type { BUY, SELL }
    public final Instant time;
    public final String player;
    public final Type type;
    public final Material material;
    public final int quantity;
    public final double amount;

    public Transaction(Instant time, String player, Type type, Material material, int quantity, double amount) {
        this.time = time; this.player = player; this.type = type; this.material = material; this.quantity = quantity; this.amount = amount;
    }
}
