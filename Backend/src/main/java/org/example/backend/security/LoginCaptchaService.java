package org.example.backend.security;

import org.springframework.stereotype.Component;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.*;
import javax.imageio.ImageIO;

/** Short-lived, single-use challenges. Single JVM storage, like LoginAttemptLimiter. */
@Component
public class LoginCaptchaService {
    private static final String ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Challenge> challenges = new HashMap<>();
    private final Clock clock;
    public LoginCaptchaService() { this(Clock.systemUTC()); }
    LoginCaptchaService(Clock clock) { this.clock = clock; }
    record Challenge(String answer, long expiresAt) {}

    public synchronized Map<String, String> create() {
        long now = clock.millis();
        challenges.values().removeIf(c -> c.expiresAt() <= now);
        if (challenges.size() >= 10000) throw new IllegalArgumentException("验证码请求过多，请稍后重试");
        StringBuilder answer = new StringBuilder();
        for (int i = 0; i < 4; i++) answer.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        BufferedImage image = new BufferedImage(144, 56, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(new Color(242, 244, 255)); g.fillRect(0, 0, 144, 56);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (int i = 0; i < 8; i++) {
                g.setColor(new Color(160 + random.nextInt(70), 170 + random.nextInt(60), 220));
                g.drawLine(random.nextInt(144), random.nextInt(56), random.nextInt(144), random.nextInt(56));
            }
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
            for (int i = 0; i < 4; i++) {
                g.setColor(new Color(35 + random.nextInt(65), 45 + random.nextInt(65), 125 + random.nextInt(65)));
                double angle = (random.nextDouble() - .5) * .4;
                int x = 15 + i * 31, y = 36 + random.nextInt(5);
                g.rotate(angle, x, y); g.drawString(answer.substring(i, i + 1), x, y); g.rotate(-angle, x, y);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            String id = UUID.randomUUID().toString();
            challenges.put(id, new Challenge(answer.toString(), now + 300000));
            return Map.of("captchaId", id, "image", "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray()));
        } catch (java.io.IOException e) { throw new IllegalStateException("验证码生成失败", e); }
        finally { g.dispose(); }
    }

    public synchronized boolean verify(String id, String answer) {
        Challenge challenge = id == null ? null : challenges.remove(id);
        return challenge != null && challenge.expiresAt() > clock.millis()
                && answer != null && challenge.answer().equalsIgnoreCase(answer.trim());
    }
}
