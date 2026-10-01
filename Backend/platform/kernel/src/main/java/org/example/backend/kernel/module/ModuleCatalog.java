package org.example.backend.kernel.module;

import java.util.Collection;
import java.util.Optional;

public interface ModuleCatalog {

    Collection<ModuleDescriptor> list();

    Optional<ModuleDescriptor> find(ModuleId moduleId);
}
