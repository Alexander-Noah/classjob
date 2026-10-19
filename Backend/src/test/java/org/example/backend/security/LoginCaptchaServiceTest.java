package org.example.backend.security;

import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.util.Map;
import java.util.Base64;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoginCaptchaServiceTest {
    @SuppressWarnings("unchecked")
    private String answer(LoginCaptchaService service, String id) throws Exception {
        var field = LoginCaptchaService.class.getDeclaredField("challenges");
        field.setAccessible(true);
        return ((Map<String, LoginCaptchaService.Challenge>) field.get(service)).get(id).answer();
    }
    @Test void rendersPngAndAcceptsCaseInsensitiveAnswerOnlyOnce() throws Exception {
        var service = new LoginCaptchaService();
        var challenge = service.create();
        var image = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(challenge.get("image").split(",")[1])));
        assertEquals(144, image.getWidth());
        String id = challenge.get("captchaId"), code = answer(service, id);
        assertTrue(service.verify(id, " " + code.toLowerCase() + " "));
        assertFalse(service.verify(id, code));
    }
    @Test void rejectsExpiredMissingAndIncorrectChallenges() throws Exception {
        Clock clock = mock(Clock.class);
        when(clock.millis()).thenReturn(1000L);
        var service = new LoginCaptchaService(clock);
        var challenge = service.create();
        String id = challenge.get("captchaId"), code = answer(service, id);
        when(clock.millis()).thenReturn(301000L);
        assertFalse(service.verify(id, code));
        assertFalse(service.verify(null, null));
        id = service.create().get("captchaId"); code = answer(service, id);
        assertFalse(service.verify(id, "wrong"));
        assertFalse(service.verify(id, code));
    }
}
