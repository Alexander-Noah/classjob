package org.example.backend.architecture;

import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class KernelPurityTest {
    @Test
    void kernelDoesNotDependOnBusinessBridgesIntegrationsOrHost() {
        noClasses().that().resideInAPackage("..kernel..")
                .should().dependOnClassesThat().resideInAnyPackage(
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
                .check(ArchitectureClasses.ALL);
    }
}
