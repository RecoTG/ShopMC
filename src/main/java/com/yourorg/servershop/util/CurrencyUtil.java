package com.yourorg.servershop.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class CurrencyUtil {
    private static final int SCALE = 2;
    private CurrencyUtil() {}

    public static BigDecimal bd(double value) {
        return BigDecimal.valueOf(value).setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal multiply(BigDecimal value, int qty) {
        return value.multiply(BigDecimal.valueOf(qty)).setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal zeroIfNegative(BigDecimal value) {
        return value.max(BigDecimal.ZERO).setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static String format(BigDecimal value) {
        return value.setScale(SCALE, RoundingMode.HALF_UP).toPlainString();
    }

    public static String format(double value) {
        return format(bd(value));
    }
}
