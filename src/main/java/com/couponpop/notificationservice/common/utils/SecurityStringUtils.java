package com.couponpop.notificationservice.common.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SecurityStringUtils {

    public static String maskToken(String token) {
        if (token == null || token.isBlank()) {
            return "N/A";
        }

        if (token.length() <= 8) {
            char first = token.charAt(0);
            char last = token.charAt(token.length() - 1);
            return "%s****%s".formatted(first, last);
        }

        String prefix = token.substring(0, 4);
        String suffix = token.substring(token.length() - 4);
        return "%s****%s".formatted(prefix, suffix);
    }

    public static String safeString(Object value) {
        return value == null ? "N/A" : String.valueOf(value);
    }
}
