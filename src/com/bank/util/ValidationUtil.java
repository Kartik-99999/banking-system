package com.bank.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Central input validation so the UI and service layers share the same rules. */
public final class ValidationUtil {
    private ValidationUtil() {
    }

    public static String requireNonBlank(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return value.trim();
    }

    public static void validateEmail(String email) {
        requireNonBlank(email, "Email");
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new IllegalArgumentException("Invalid email address.");
        }
    }

    public static void validatePhone(String phone) {
        requireNonBlank(phone, "Phone");
        if (!phone.matches("\\d{10}")) {
            throw new IllegalArgumentException("Phone must be exactly 10 digits.");
        }
    }

    public static void validatePin(String pin) {
        if (pin == null || !pin.matches("\\d{4,6}")) {
            throw new IllegalArgumentException("PIN must be 4 to 6 digits.");
        }
    }

    /** Parses user-typed money like "500" or "500.50" into paise-exact BigDecimal. */
    public static BigDecimal parseAmount(String text) {
        requireNonBlank(text, "Amount");
        try {
            BigDecimal amount = new BigDecimal(text.trim()).setScale(2, RoundingMode.HALF_UP);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Amount must be greater than zero.");
            }
            return amount;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid amount. Enter a number like 500 or 500.00.");
        }
    }

    public static long parseAccountNo(String text) {
        requireNonBlank(text, "Account number");
        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Account number must be numeric.");
        }
    }
}
