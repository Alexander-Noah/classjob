package org.example.backend.architecture;

import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class BridgeBoundaryTest {
    @Test
    void bridgesNeverDependOnPersistenceOrImplementationPackages() {
        noClasses().that().resideInAPackage("..bridge..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..entity..",
                        "..mapper..",
                        "..repository..",
                        "..service.impl..",
                        "..adapter.out..",
                        "..infrastructure.."
                )
                .check(ArchitectureClasses.ALL);
    }
}
