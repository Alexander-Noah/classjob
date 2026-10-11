package org.example.backend.identityaccess.adapter.out.security;

import org.example.backend.identityaccess.application.port.out.PasswordPort;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class Pbkdf2PasswordAdapter implements PasswordPort {
    private static final String PREFIX = "pbkdf2";
    private final SecureRandom random = new SecureRandom();
    private final int iterations;

    public Pbkdf2PasswordAdapter(int iterations) {
        this.iterations = Math.max(iterations, 10_000);
    }

    @Override
    public String encode(String rawPassword) {
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return String.join("$", PREFIX, String.valueOf(iterations), Base64.getEncoder().encodeToString(salt),
                Base64.getEncoder().encodeToString(hash(rawPassword, salt, iterations)));
    }

    @Override
    public boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null || storedPassword.isBlank()) return false;
        if (!isEncoded(storedPassword)) {
            return MessageDigest.isEqual(rawPassword.getBytes(StandardCharsets.UTF_8), storedPassword.getBytes(StandardCharsets.UTF_8));
        }
        try {
            String[] parts = storedPassword.split("\\$");
            return MessageDigest.isEqual(Base64.getDecoder().decode(parts[3]),
                    hash(rawPassword, Base64.getDecoder().decode(parts[2]), Integer.parseInt(parts[1])));
        } catch (RuntimeException exception) {
            return false;
        }
    }

    @Override
    public boolean needsRehash(String storedPassword) {
        if (!isEncoded(storedPassword)) return true;
        try { return Integer.parseInt(storedPassword.split("\\$")[1]) < iterations; }
        catch (RuntimeException exception) { return true; }
    }

    private boolean isEncoded(String value) {
        return value != null && value.startsWith(PREFIX + "$") && value.split("\\$").length == 4;
    }

    private byte[] hash(String rawPassword, byte[] salt, int count) {
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(new PBEKeySpec(rawPassword.toCharArray(), salt, count, 256)).getEncoded();
        } catch (Exception exception) {
            throw new IllegalStateException("密码哈希转换失败", exception);
        }
    }
}
