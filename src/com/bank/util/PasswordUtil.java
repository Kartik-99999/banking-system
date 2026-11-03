package com.bank.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** SHA-256 hashing for PINs. The database never stores a plain-text PIN. */
public final class PasswordUtil {
    private PasswordUtil() {
    }

    public static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    public static boolean verify(String pin, String hash) {
        if (pin == null || hash == null) {
            return false;
        }
        return sha256(pin).equals(hash);
    }
}
