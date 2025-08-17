package com.yourorg.servershop.weekly;

import java.time.*;
import java.util.*;

/**
 * Minimal cron expression parser supporting five fields: minute, hour, day of
 * month, month and day of week. Only simple numeric values, ranges and
 * comma-separated lists are supported. This is sufficient for weekly schedules
 * such as "0 0 * * MON".
 */
public final class CronSchedule {
    private final Set<Integer> minutes;
    private final Set<Integer> hours;
    private final Set<Integer> doms;
    private final Set<Integer> months;
    private final Set<Integer> dows; // 1=Mon .. 7=Sun

    public CronSchedule(String expr) {
        String[] parts = expr.trim().split("\\s+");
        if (parts.length != 5) throw new IllegalArgumentException("Cron must have 5 parts");
        minutes = parsePart(parts[0], 0, 59);
        hours = parsePart(parts[1], 0, 23);
        doms = parsePart(parts[2], 1, 31);
        months = parsePart(parts[3], 1, 12);
        dows = parseDow(parts[4]);
    }

    private Set<Integer> parsePart(String part, int min, int max) {
        Set<Integer> set = new LinkedHashSet<>();
        if ("*".equals(part)) {
            for (int i = min; i <= max; i++) set.add(i);
            return set;
        }
        for (String t : part.split(",")) {
            if (t.contains("-")) {
                String[] lr = t.split("-");
                int l = Integer.parseInt(lr[0]);
                int r = Integer.parseInt(lr[1]);
                for (int i = l; i <= r; i++) set.add(i);
            } else {
                set.add(Integer.parseInt(t));
            }
        }
        return set;
    }

    private Set<Integer> parseDow(String part) {
        Set<Integer> set = new LinkedHashSet<>();
        if ("*".equals(part)) {
            for (int i = 1; i <= 7; i++) set.add(i);
            return set;
        }
        for (String t : part.split(",")) {
            if (t.contains("-")) {
                String[] lr = t.split("-");
                int l = toDow(lr[0]);
                int r = toDow(lr[1]);
                for (int i = l; i <= r; i++) set.add(i);
            } else {
                set.add(toDow(t));
            }
        }
        return set;
    }

    private int toDow(String s) {
        try {
            int v = Integer.parseInt(s);
            return v == 0 ? 7 : v; // cron 0 or 7 = Sunday
        } catch (NumberFormatException ignored) {
            return DayOfWeek.valueOf(s.toUpperCase()).getValue();
        }
    }

    private boolean matches(ZonedDateTime z) {
        return minutes.contains(z.getMinute()) &&
               hours.contains(z.getHour()) &&
               doms.contains(z.getDayOfMonth()) &&
               months.contains(z.getMonthValue()) &&
               dows.contains(z.getDayOfWeek().getValue());
    }

    /**
     * Returns the next instant strictly after {@code from} that matches the
     * cron expression.
     */
    public Instant next(Instant from, ZoneId zone) {
        ZonedDateTime z = ZonedDateTime.ofInstant(from, zone).withSecond(0).withNano(0).plusMinutes(1);
        for (int i = 0; i < 500000; i++) { // safety bound ~1 year
            if (matches(z)) return z.toInstant();
            z = z.plusMinutes(1);
        }
        return z.toInstant();
    }
}

