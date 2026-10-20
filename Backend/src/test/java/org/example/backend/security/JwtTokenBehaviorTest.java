package org.example.backend.security;

import org.example.backend.entity.UserEntity;
import org.junit.jupiter.api.Test;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

class JwtTokenBehaviorTest {
    private static final String SECRET = "test-only-signing-secret-over-thirty-two-characters";
    private final JwtService service = new JwtService(SECRET, 7200, "smart-lab");

    @Test
    void roundTripPreservesIdentityAndEscapedCharacters() {
        UserEntity user = new UserEntity();
        user.setId(7);
        user.setUsername("管理员\"\\\b\f\n\r\t\u0001");
        user.setRoleCode("labAdmin");
        user.setRealName("测试用户");
        long now = Instant.now().getEpochSecond();
        var claims = service.validateToken(service.generateToken(user));
        assertEquals(user.getUsername(), claims.username());
        assertEquals("labAdmin", claims.roleCode());
        assertEquals("7", claims.userId());
        assertTrue(claims.expiresAt() >= now + 7200);
        assertTrue(claims.expiresAt() <= Instant.now().getEpochSecond() + 7200);
    }

    @Test
    void rejectsAbsentMalformedAndTamperedTokens() {
        for (String token : new String[]{null, "", " ", "abc", "a.b", "a.b.c.d", "a.b.c"}) {
            assertThrows(IllegalArgumentException.class, () -> service.validateToken(token));
        }
        UserEntity user = new UserEntity();
        user.setUsername("admin");
        String token = service.generateToken(user);
        String[] parts = token.split("\\.");
        assertThrows(IllegalArgumentException.class, () -> service.validateToken(parts[0] + "." + encode("{\"sub\":\"attacker\"}") + "." + parts[2]));
        assertThrows(IllegalArgumentException.class, () -> new JwtService(SECRET + "other", 7200, "smart-lab").validateToken(token));
    }

    @Test
    void rejectsWrongAlgorithmTypeIssuerExpiredAndMissingSubjects() throws Exception {
        String valid = payload("admin", Instant.now().getEpochSecond() + 1000);
        for (String header : new String[]{"{\"alg\":\"none\",\"typ\":\"JWT\"}", "{\"alg\":\"HS256\",\"typ\":\"other\"}"}) {
            assertThrows(IllegalArgumentException.class, () -> service.validateToken(signed(encode(header), encode(valid))));
        }
        assertThrows(IllegalArgumentException.class, () -> service.validateToken(token(valid.replace("smart-lab", "other"))));
        assertThrows(IllegalArgumentException.class, () -> service.validateToken(token(payload("admin", 1))));
        assertThrows(IllegalArgumentException.class, () -> service.validateToken(token(payload("", Instant.now().getEpochSecond() + 1000))));
        assertThrows(IllegalArgumentException.class, () -> service.validateToken(token(valid.replaceFirst("[0-9]+", "null"))));
        assertThrows(IllegalArgumentException.class, () -> service.validateToken(signed("%%%", encode(valid))));
    }

    @Test
    void validatesUnicodeEscapesAndOptionalClaims() throws Exception {
        var claims = service.validateToken(token(payload("\\u4e2d\\/\\b\\f\\n\\r\\t\\\\\\\"", Instant.now().getEpochSecond() + 1000)));
        assertEquals("中/\b\f\n\r\t\\\"", claims.username());
        assertEquals("", claims.roleCode());
        assertEquals("", claims.userId());
        for (String subject : new String[]{"\\uZZZZ", "\\u12"}) {
            assertThrows(IllegalArgumentException.class, () -> service.validateToken(token(payload(subject, Instant.now().getEpochSecond() + 1000))));
        }
    }

    @Test
    void enforcesSecretStrengthAndMinimumLifetime() {
        assertThrows(IllegalStateException.class, () -> new JwtService(null, 1, null));
        assertThrows(IllegalStateException.class, () -> new JwtService("short", 1, null));
        UserEntity user = new UserEntity();
        user.setUsername("admin");
        JwtService minimum = new JwtService("  " + SECRET + "  ", 1, null);
        long now = Instant.now().getEpochSecond();
        assertTrue(minimum.validateToken(minimum.generateToken(user)).expiresAt() >= now + 300);
    }

    @Test void signedTruncatedClaimsStillFailValidationAndWhitespaceDelimitedNumbersRemainValid() throws Exception {
        long future=Instant.now().getEpochSecond()+1000;
        var valid=service.validateToken(token("{\"iss\" : \"smart-lab\",\"exp\" : "+future+",\"sub\" : \"admin\"}"));
        assertEquals("admin",valid.username());
        for(String broken:new String[]{"{\"iss\"}","{\"iss\":   ",
                "{\"iss\":\"smart-lab\",\"exp\":true}",
                "{\"iss\":\"smart-lab\",\"exp\":"+future+",\"sub\":\"unterminated",
                "{\"iss\":\"smart-lab\",\"exp\":"+future+",\"sub\":\""+"\\u1"}) {
            assertThrows(IllegalArgumentException.class,()->service.validateToken(token(broken)),broken);
        }
    }
    private String payload(String subject, long expires) {
        return "{\"sub\":\"" + subject + "\",\"iss\":\"smart-lab\",\"exp\":" + expires + "}";
    }
    private String token(String payload) throws Exception {
        return signed(encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}"), encode(payload));
    }
    private String signed(String header, String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String input = header + "." + payload;
        return input + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));
    }
    private String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
