package org.example.backend.kernel.capability;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CapabilityResultTest {

    @Test
    void availableCarriesTheValueWithoutAnError() {
        CapabilityResult<String> result = CapabilityResult.available("ready");

        assertEquals(CapabilityStatus.AVAILABLE, result.status());
        assertEquals("ready", result.value());
        assertNull(result.errorCode());
        assertNull(result.message());
    }

    @Test
    void unavailableUsesTheStableCapabilityErrorCode() {
        CapabilityResult<String> result = CapabilityResult.unavailable("module removed");

        assertEquals(CapabilityStatus.UNAVAILABLE, result.status());
        assertNull(result.value());
        assertEquals("CAPABILITY_UNAVAILABLE", result.errorCode());
        assertEquals("module removed", result.message());
    }
}
