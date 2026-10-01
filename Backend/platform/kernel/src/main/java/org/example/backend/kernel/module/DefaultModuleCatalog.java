package org.example.backend.kernel.module;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class DefaultModuleCatalog implements ModuleCatalog {

    private final List<ModuleDescriptor> descriptors;
    private final Map<ModuleId, ModuleDescriptor> descriptorsById;

    public DefaultModuleCatalog(Collection<ModuleDescriptor> descriptors) {
        List<ModuleDescriptor> ordered = descriptors.stream()
                .sorted(Comparator.comparing(descriptor -> descriptor.id().value()))
                .toList();
        Map<ModuleId, ModuleDescriptor> indexed = new LinkedHashMap<>();
        for (ModuleDescriptor descriptor : ordered) {
            if (indexed.putIfAbsent(descriptor.id(), descriptor) != null) {
                throw new IllegalStateException("duplicate module id: " + descriptor.id().value());
            }
        }
        this.descriptors = List.copyOf(ordered);
        this.descriptorsById = Map.copyOf(indexed);
    }

    @Override
    public Collection<ModuleDescriptor> list() {
        return descriptors;
    }

    @Override
    public Optional<ModuleDescriptor> find(ModuleId moduleId) {
        return Optional.ofNullable(descriptorsById.get(moduleId));
    }
}
