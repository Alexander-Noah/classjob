package org.example.backend.architecture;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class PersistenceOwnershipTest {
    private static final String[] BUSINESS_ROOTS = {
            "org.example.backend.identityaccess",
            "org.example.backend.teachingschedule",
            "org.example.backend.labresource",
            "org.example.backend.reservation",
            "org.example.backend.environmentreadiness",
            "org.example.backend.teachingcoordination"
    };

    private static final String[] LEGACY_PERSISTENCE_PACKAGES = {
            "org.example.backend.entity..",
            "org.example.backend.mapper..",
            "org.example.backend.repository..",
            "org.example.backend.service.impl.."
    };

    @Test
    void businessModulesNeverImportAnotherModulesPersistenceTypes() {
        for (String sourceRoot : BUSINESS_ROOTS) {
            List<String> forbiddenPackages = new ArrayList<>(List.of(LEGACY_PERSISTENCE_PACKAGES));

            for (String targetRoot : BUSINESS_ROOTS) {
                if (!targetRoot.equals(sourceRoot)) {
                    addPersistencePackages(forbiddenPackages, targetRoot);
                }
            }

            noClasses().that().resideInAPackage(sourceRoot + "..")
                    .should().dependOnClassesThat().resideInAnyPackage(forbiddenPackages.toArray(String[]::new))
                    .because("persistence types are private to the module that owns them")
                    .check(ArchitectureClasses.ALL);
        }
    }

    @Test
    void infrastructureModulesNeverImportBusinessPersistenceTypes() {
        noClasses().that().resideInAnyPackage(
                        "..kernel..",
                        "..platform..",
                        "..bridge..",
                        "..integration..",
                        "..host.."
                )
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..entity..",
                        "..mapper..",
                        "..repository..",
                        "..service.impl.."
                )
                .because("platform, bridge, integration, and host code must use public module contracts")
                .check(ArchitectureClasses.ALL);
    }

    private static void addPersistencePackages(List<String> packages, String moduleRoot) {
        packages.add(moduleRoot + "..entity..");
        packages.add(moduleRoot + "..mapper..");
        packages.add(moduleRoot + "..repository..");
        packages.add(moduleRoot + "..service.impl..");
    }
}
