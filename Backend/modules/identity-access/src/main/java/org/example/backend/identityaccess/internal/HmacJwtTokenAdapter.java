package org.example.backend.identityaccess.internal;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.backend.identityaccess.api.UserView;
import org.example.backend.identityaccess.internal.TokenPort;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HmacJwtTokenAdapter implements TokenPort {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final byte[] secret;
    private final long expirationSeconds;
    private final String issuer;
    private final Clock clock;

    public HmacJwtTokenAdapter(String secret, long expirationSeconds, String issuer) {
        this(secret, expirationSeconds, issuer, Clock.systemUTC());
    }

    HmacJwtTokenAdapter(String secret, long expirationSeconds, String issuer, Clock clock) {
        String value = secret == null ? "" : secret.trim();
        if (value.length() < 32 || value.toLowerCase().contains("change-in-production")) {
            throw new IllegalStateException("SMART_LAB_JWT_SECRET must be configured with at least 32 characters");
        }
        this.secret = value.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = Math.max(expirationSeconds, 300);
        this.issuer = issuer == null ? "" : issuer.trim();
        this.clock = clock;
    }

    @Override
    public String issue(UserView user) {
        long issuedAt = clock.instant().getEpochSecond();
        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", user.username());
        payload.put("uid", user.id());
        payload.put("role", user.roleCode());
        payload.put("name", user.realName());
        payload.put("iss", issuer);
        payload.put("iat", issuedAt);
        payload.put("exp", issuedAt + expirationSeconds);
        String unsigned = encode(header) + "." + encode(payload);
        return unsigned + "." + sign(unsigned);
    }

    @Override
    public UserView validate(String token) {
        if (token == null || token.isBlank()) throw new IllegalArgumentException("缺少令牌");
        String[] parts = token.split("\\.");
        if (parts.length != 3) throw new IllegalArgumentException("无效的令牌格式");
        String expected = sign(parts[0] + "." + parts[1]);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8)))
            throw new IllegalArgumentException("无效的令牌签名");
        Map<String, Object> header = decode(parts[0]);
        Map<String, Object> payload = decode(parts[1]);
        if (!"HS256".equals(header.get("alg")) || !"JWT".equals(header.get("typ")))
            throw new IllegalArgumentException("无效的令牌头");
        if (!issuer.isBlank() && !issuer.equals(String.valueOf(payload.get("iss"))))
            throw new IllegalArgumentException("无效的令牌发布者");
        if (number(payload.get("exp")) <= clock.instant().getEpochSecond())
            throw new IllegalArgumentException("令牌已过期");
        String username = String.valueOf(payload.getOrDefault("sub", ""));
        if (username.isBlank()) throw new IllegalArgumentException("缺少令牌主体");
        return new UserView(number(payload.get("uid")), username, text(payload.get("name")), text(payload.get("role")),
                null, null, null, null, null, null);
    }

    private String encode(Map<String, Object> value) {
        try { return Base64.getUrlEncoder().withoutPadding().encodeToString(JSON.writeValueAsBytes(value)); }
        catch (Exception exception) { throw new IllegalStateException("JWT 序列化失败", exception); }
    }

    private Map<String, Object> decode(String value) {
        try { return JSON.readValue(Base64.getUrlDecoder().decode(value), new TypeReference<>() {}); }
        catch (Exception exception) { throw new IllegalArgumentException("令牌内容解析失败", exception); }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) { throw new IllegalStateException("JWT 签名失败", exception); }
    }

    private long number(Object value) {
        if (value instanceof Number number) return number.longValue();
        try { return Long.parseLong(String.valueOf(value)); } catch (RuntimeException exception) { return 0; }
    }

    private String text(Object value) { return value == null ? "" : String.valueOf(value); }
}
