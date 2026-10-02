package org.example.backend.architecture;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

class ConstructorInjectionTest {
    @Test
    void modularCodeDoesNotUseFieldInjection() {
        noFields().that().areDeclaredInClassesThat().resideInAnyPackage(
                        "..kernel..",
                        "..platform..",
                        "..identityaccess..",
                        "..teachingschedule..",
                        "..labresource..",
                        "..reservation..",
                        "..environmentreadiness..",
                        "..teachingcoordination..",
                        "..bridge..",
                        "..integration..",
                        "..host.."
                )
                .should().beAnnotatedWith(Autowired.class)
                .check(ArchitectureClasses.ALL);
    }
}
