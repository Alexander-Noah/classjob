package org.example.backend.config;

import org.example.backend.service.CrawlerCredentialException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

class FriendlyErrorResponseTest {
    @Test
    void genericErrorsKeepInternalDetailsAndRequestIdsOutOfTheUserMessage() {
        MDC.put("requestId", "test-request-identifier");
        try {
            var response = new GlobalExceptionHandler().handleException(
                    new IllegalStateException("internal-db-detail"), new MockHttpServletRequest("GET", "/example"));
            assertEquals(500, response.getStatusCode().value());
            assertEquals("服务暂时不可用，请稍后重试", response.getBody().getMessage());
        } finally { MDC.clear(); }
    }

    @Test
    void unreadableCredentialsReturnAnActionableConflict() {
        var response = new GlobalExceptionHandler().handleCrawlerCredentialException(
                new CrawlerCredentialException(new IllegalStateException("internal-key-detail")),
                new MockHttpServletRequest("POST", "/class-timetables/crawl"));
        assertEquals(409, response.getStatusCode().value());
        assertEquals(CrawlerCredentialException.USER_MESSAGE, response.getBody().getMessage());
        assertFalse(response.getBody().getMessage().contains("internal-key-detail"));
    }
}
