package org.example.backend.teachingschedule.bootstrap;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeachingScheduleRuntimeConfigurationTest {
    @Test
    void composesModuleMetadataWithRuntimeAdapters() {
        Import imported = TeachingScheduleRuntimeConfiguration.class.getAnnotation(Import.class);

        assertNotNull(imported);
        assertTrue(Arrays.asList(imported.value()).contains(TeachingScheduleModuleConfiguration.class));
    }
}
