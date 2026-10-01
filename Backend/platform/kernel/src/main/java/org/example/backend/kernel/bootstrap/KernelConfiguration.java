package org.example.backend.kernel.bootstrap;

import org.example.backend.kernel.module.DefaultModuleCatalog;
import org.example.backend.kernel.module.ModuleCatalog;
import org.example.backend.kernel.module.ModuleDescriptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration(proxyBeanMethods = false)
public class KernelConfiguration {

    @Bean
    public ModuleCatalog moduleCatalog(List<ModuleDescriptor> descriptors) {
        return new DefaultModuleCatalog(descriptors);
    }
}
