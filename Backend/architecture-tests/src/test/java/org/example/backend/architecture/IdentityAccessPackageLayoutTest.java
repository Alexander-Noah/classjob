package org.example.backend.architecture;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class IdentityAccessPackageLayoutTest {
    @Test
    void identityModuleUsesRecognizableLayersAndKeepsItsApiIndependent() {
        ArchitectureClasses.ALL.stream()
                .filter(javaClass -> javaClass.getPackageName().startsWith("org.example.backend.identityaccess"))
                .forEach(javaClass -> assertTrue(isAllowedIdentityPackage(javaClass.getPackageName()),
                        () -> javaClass.getName()
                                + " must reside in the module root, api, application, infrastructure, or web package"));

        noClasses().that().resideInAPackage("org.example.backend.identityaccess.api..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.example.backend.identityaccess.application..",
                        "org.example.backend.identityaccess.infrastructure..",
                        "org.example.backend.identityaccess.web..")
                .because("the public API must not expose implementation details")
                .check(ArchitectureClasses.ALL);
    }

    private boolean isAllowedIdentityPackage(String packageName) {
        String root = "org.example.backend.identityaccess";
        return packageName.equals(root)
                || packageName.equals(root + ".api")
                || packageName.startsWith(root + ".api.")
                || packageName.equals(root + ".application")
                || packageName.startsWith(root + ".application.")
                || packageName.equals(root + ".infrastructure")
                || packageName.startsWith(root + ".infrastructure.")
                || packageName.equals(root + ".web")
                || packageName.startsWith(root + ".web.");
    }
}
