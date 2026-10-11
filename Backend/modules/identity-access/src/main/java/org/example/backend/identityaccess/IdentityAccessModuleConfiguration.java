package org.example.backend.identityaccess;

import org.example.backend.kernel.module.ModuleDescriptor;
import org.example.backend.kernel.module.ModuleId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration(proxyBeanMethods = false)
public class IdentityAccessModuleConfiguration {
    @Bean
    public ModuleDescriptor identityAccessModuleDescriptor() {
        return new ModuleDescriptor(new ModuleId("identity-access"), "Identity access", Set.of("identity.authenticate", "identity.authorize"));
    }
}
