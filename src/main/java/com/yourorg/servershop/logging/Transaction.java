package com.yourorg.servershop.logging;

import org.bukkit.Material;
import java.time.Instant;
import java.util.UUID;
import java.math.BigDecimal;

public final class Transaction {
    public enum Type { BUY, SELL }
    public final Instant time;
    public final String player;
    public final UUID uuid;
    public final Type type;
    public final Material material;
    public final int quantity;
    public final BigDecimal amount;

    public Transaction(Instant time, String player, UUID uuid, Type type, Material material, int quantity, BigDecimal amount) {
        this.time = time;
        this.player = player;
        this.uuid = uuid;
        this.type = type;
        this.material = material;
        this.quantity = quantity;
        this.amount = amount;
    }
}
