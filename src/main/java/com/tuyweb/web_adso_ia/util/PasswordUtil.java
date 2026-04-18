package com.tuyweb.web_adso_ia.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public final class PasswordUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException("La clave es obligatoria.");
        }

        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);

        byte[] hash = sha256(salt, plainPassword.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);
    }

    public static boolean verifyPassword(String plainPassword, String storedHash) {
        if (plainPassword == null || plainPassword.isBlank() || storedHash == null || storedHash.isBlank()) {
            return false;
        }

        String[] parts = storedHash.split(":");
        if (parts.length != 2) {
            return false;
        }

        byte[] salt = Base64.getDecoder().decode(parts[0]);
        byte[] expectedHash = Base64.getDecoder().decode(parts[1]);
        byte[] actualHash = sha256(salt, plainPassword.getBytes(StandardCharsets.UTF_8));

        return MessageDigest.isEqual(expectedHash, actualHash);
    }

    private static byte[] sha256(byte[] salt, byte[] plainPassword) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(salt);
            return messageDigest.digest(plainPassword);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("No se pudo calcular el hash de la clave.", ex);
        }
    }
}
