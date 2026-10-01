package org.example.backend.platform.websupport;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class WebSupportConfigurationTest {
    @Test
    void suppliesUniqueUuidCorrelationIds() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(WebSupportConfiguration.class)) {
            CorrelationIdProvider provider = context.getBean(CorrelationIdProvider.class);
            String first = provider.next();
            String second = provider.next();

            assertDoesNotThrow(() -> UUID.fromString(first));
            assertNotEquals(first, second);
        }
    }
}
