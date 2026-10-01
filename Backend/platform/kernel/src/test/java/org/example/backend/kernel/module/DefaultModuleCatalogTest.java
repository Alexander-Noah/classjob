package org.example.backend.kernel.module;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultModuleCatalogTest {

    @Test
    void listsDescriptorsInDeterministicModuleIdOrder() {
        ModuleDescriptor reservation = descriptor("reservation");
        ModuleDescriptor labResource = descriptor("lab-resource");

        ModuleCatalog catalog = new DefaultModuleCatalog(List.of(reservation, labResource));

        assertEquals(List.of(labResource, reservation), catalog.list());
    }

    @Test
    void rejectsDuplicateModuleIds() {
        ModuleDescriptor first = descriptor("reservation");
        ModuleDescriptor duplicate = new ModuleDescriptor(
                new ModuleId("reservation"),
                "Another reservation module",
                Set.of("reservation.read")
        );

        assertThrows(IllegalStateException.class, () -> new DefaultModuleCatalog(List.of(first, duplicate)));
    }

    @Test
    void returnsEmptyForUnknownModule() {
        ModuleCatalog catalog = new DefaultModuleCatalog(List.of(descriptor("reservation")));

        assertTrue(catalog.find(new ModuleId("teaching-schedule")).isEmpty());
    }

    private ModuleDescriptor descriptor(String id) {
        return new ModuleDescriptor(new ModuleId(id), id, Set.of(id + ".read"));
    }
}
