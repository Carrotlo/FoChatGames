package me.foesio.foChatGames.util;

import me.foesio.core.number.LargeNumberParser;
import me.foesio.core.number.NumberFormatters;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

public final class NumberUtil {
    private NumberUtil() {
    }

    public static String format(long value) {
        return NumberFormatters.whole(value);
    }

    public static String formatSeconds(double value) {
        if (Double.isInfinite(value) || value <= 0.0D) {
            return "-";
        }
        return NumberFormatters.decimal(BigDecimal.valueOf(value));
    }

    public static Long parseLarge(String input) {
        return parseRounded(input);
    }

    private static Long parseRounded(String input) {
        Optional<BigDecimal> parsed = LargeNumberParser.parse(input);
        if (parsed.isEmpty() || parsed.get().compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) > 0 || parsed.get().compareTo(BigDecimal.valueOf(Long.MIN_VALUE)) < 0) {
            return null;
        }
        try {
            return parsed.get().setScale(0, RoundingMode.HALF_UP).longValueExact();
        } catch (ArithmeticException exception) {
            return null;
        }
    }
}
