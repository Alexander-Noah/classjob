package org.example.backend.architecture;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class BusinessModuleBoundaryTest {
    @Test
    void businessModulesDoNotDependOnOtherBusinessModules() {
        for (String sourcePackage : ArchitectureClasses.BUSINESS_PACKAGES) {
            String[] otherPackages = Arrays.stream(ArchitectureClasses.BUSINESS_PACKAGES)
                    .filter(candidate -> !candidate.equals(sourcePackage))
                    .toArray(String[]::new);

            noClasses().that().resideInAPackage(sourcePackage)
                    .should().dependOnClassesThat().resideInAnyPackage(otherPackages)
                    .because("business modules collaborate only through independently removable bridges")
                    .check(ArchitectureClasses.ALL);
        }
    }
}
