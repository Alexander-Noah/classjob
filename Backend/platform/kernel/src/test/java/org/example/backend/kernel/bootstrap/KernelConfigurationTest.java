package org.example.backend.kernel.bootstrap;

import org.example.backend.kernel.module.ModuleCatalog;
import org.example.backend.kernel.module.ModuleDescriptor;
import org.example.backend.kernel.module.ModuleId;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KernelConfigurationTest {

    @Test
    void exposesCatalogForRegisteredModuleDescriptors() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(KernelConfiguration.class, DescriptorFixture.class);
            context.refresh();

            ModuleCatalog catalog = context.getBean(ModuleCatalog.class);

            assertEquals("test-module", catalog.list().iterator().next().id().value());
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class DescriptorFixture {
        @Bean
        ModuleDescriptor testModuleDescriptor() {
            return new ModuleDescriptor(new ModuleId("test-module"), "Test module", Set.of("test.read"));
        }
    }
}
