package com.yourorg.servershop.weekly;

import com.yourorg.servershop.ServerShopPlugin;
import org.bukkit.Material;

import java.time.*;
import java.util.*;

public final class WeeklyShopManager {
    private final ServerShopPlugin plugin;

    public WeeklyShopManager(ServerShopPlugin plugin) { this.plugin = plugin; }

    public boolean isWeekly(Material m) { return currentPicks().contains(m); }

    public java.util.Set<Material> currentPicks() {
        int count = Math.max(1, plugin.getConfig().getInt("weekly.count", 6));
        LocalDate now = LocalDate.now();
        DayOfWeek dow = DayOfWeek.valueOf(plugin.getConfig().getString("weekly.firstDayOfWeek", "MONDAY"));
        LocalDate weekStart = now.with(java.time.temporal.TemporalAdjusters.previousOrSame(dow));
        long seed = weekStart.toEpochDay();
        java.util.List<Material> pool = new java.util.ArrayList<>(plugin.catalog().allMaterials());
        pool.sort(java.util.Comparator.comparing(Enum::name));
        java.util.Random rnd = new java.util.Random(seed);
        java.util.Set<Material> pick = new java.util.LinkedHashSet<>();
        while (pick.size() < Math.min(count, pool.size())) pick.add(pool.get(rnd.nextInt(pool.size())));
        return pick;
    }
}
