package org.example.backend.kernel.module;

import java.util.Set;

public record ModuleDescriptor(
        ModuleId id,
        String displayName,
        Set<String> capabilities
) {

    public ModuleDescriptor {
        capabilities = Set.copyOf(capabilities);
    }
}
