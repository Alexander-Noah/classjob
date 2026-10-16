package org.example.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class MonitoringScrapeAccessTest {
    @Test
    void permitsScrapingOnlyOnSeparateManagementPort() throws Exception {
        assertTrue(scrape(8081, 8081));
        assertFalse(scrape(8080, 8081));
        assertFalse(scrape(8080, 8080));
        assertFalse(scrape(8080, -1));
    }

    private boolean scrape(int localPort, int managementPort) throws Exception {
        var filter = new JwtAuthenticationFilter(null, true);
        ReflectionTestUtils.setField(filter, "managementPort", managementPort);
        var request = new MockHttpServletRequest("GET", "/actuator/prometheus");
        request.setServletPath("/actuator/prometheus");
        request.setLocalPort(localPort);
        var response = new MockHttpServletResponse();
        var passed = new AtomicBoolean();
        filter.doFilter(request, response, (req, res) -> passed.set(true));
        if (!passed.get()) {
            assertEquals(401, response.getStatus());
        }
        return passed.get();
    }
}
