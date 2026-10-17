/*
 * FriendlyErrorResponseTest回归测试；测试夹具与生产配置分离。
 * 覆盖场景：oversizedAvatarHasAnActionable413Response、oversizedOtherUploadsDoNotUseTheAvatarMessage、genericErrorsKeepInternalDetailsAndRequestIdsOutOfTheUserMessage、unreadableCredentialsReturnAnActionableConflict。
 */
package org.example.backend.config;

import org.example.backend.service.CrawlerCredentialException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

class FriendlyErrorResponseTest {
    @Test
    void oversizedAvatarHasAnActionable413Response() {
        var response = new GlobalExceptionHandler().handleUploadTooLarge(
                new org.springframework.web.multipart.MaxUploadSizeExceededException(2 * 1024 * 1024),
                new MockHttpServletRequest("POST", "/files/avatars"));
        assertEquals(413, response.getStatusCode().value());
        assertEquals(413, response.getBody().getCode());
        assertTrue(response.getBody().getMessage().contains("2MB"));
        assertFalse(response.getBody().getMessage().contains("Exception"));
    }

    @Test
    void oversizedOtherUploadsDoNotUseTheAvatarMessage() {
        var response = new GlobalExceptionHandler().handleUploadTooLarge(
                new org.springframework.web.multipart.MaxUploadSizeExceededException(2 * 1024 * 1024),
                new MockHttpServletRequest("POST", "/devices/import"));
        assertEquals(413, response.getStatusCode().value());
        assertFalse(response.getBody().getMessage().contains("头像"));
    }

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
