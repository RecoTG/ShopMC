package com.yourorg.servershop.commands;

import org.bukkit.Material;

public final class Util {
    public static Material parseMaterial(String raw) {
        if (raw == null) return null;
        return Material.matchMaterial(raw.toUpperCase().replace('-', '_').replace(' ', '_'));
    }
}
