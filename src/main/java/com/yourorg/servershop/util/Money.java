package com.yourorg.servershop.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {
    private Money() {}

    public static BigDecimal money(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }

    public static String fmt(double v) {
        return money(v).toPlainString();
    }

    public static BigDecimal money(Object o) {
        if (o instanceof Number n) {
            return money(n.doubleValue());
        }
        try {
            return money(new BigDecimal(String.valueOf(o)).doubleValue());
        } catch (Exception e) {
            return money(0.0);
        }
    }
}
