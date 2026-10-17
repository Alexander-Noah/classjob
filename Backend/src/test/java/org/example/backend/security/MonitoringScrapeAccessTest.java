/*
 * MonitoringScrapeAccessTest回归测试；测试夹具与生产配置分离。
 * 覆盖场景：permitsScrapingOnlyOnSeparateManagementPort。
 */
package org.example.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class MonitoringScrapeAccessTest {
    /**
     * permitsScrapingOnlyOnSeparateManagement端口的处理逻辑。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    @Test
    void permitsScrapingOnlyOnSeparateManagementPort() throws Exception {
        assertTrue(scrape(8081, 8081));
        assertFalse(scrape(8080, 8081));
        assertFalse(scrape(8080, 8080));
        assertFalse(scrape(8080, -1));
    }

    /**
     * scrape的处理逻辑。
     * @param localPort 本地端口参数。
     * @param managementPort management端口参数。
     * @return 当前判断的布尔结果。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    private boolean scrape(int localPort, int managementPort) throws Exception {
        var filter = new JwtAuthenticationFilter(null, true);
        ReflectionTestUtils.setField(filter, "managementPort", managementPort);
        var request = new MockHttpServletRequest("GET", "/actuator/prometheus");
        request.setServletPath("/actuator/prometheus");
        request.setLocalPort(localPort);
        var response = new MockHttpServletResponse();
        var passed = new AtomicBoolean();
        filter.doFilter(request, response, /** 处理 doFilter 调用中的回调逻辑。 入参：req、res。 */ (req, res) -> passed.set(true));
        if (!passed.get()) {
            assertEquals(401, response.getStatus());
        }
        return passed.get();
    }
}
