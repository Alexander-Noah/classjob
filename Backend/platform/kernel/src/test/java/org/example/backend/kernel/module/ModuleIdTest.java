package org.example.backend.kernel.module;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModuleIdTest {

    @Test
    void rejectsNullAndBlankValues() {
        assertThrows(IllegalArgumentException.class, () -> new ModuleId(null));
        assertThrows(IllegalArgumentException.class, () -> new ModuleId("   "));
    }

    @Test
    void trimsTheCanonicalValue() {
        assertEquals("reservation", new ModuleId(" reservation ").value());
    }
}
