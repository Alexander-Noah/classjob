package org.example.backend.architecture;

import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class HostPurityTest {
    @Test
    void hostDoesNotDependOnBusinessImplementationLayers() {
        noClasses().that().resideInAPackage("..host..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..identityaccess.api..", "..identityaccess.application..", "..identityaccess.domain..", "..identityaccess.infrastructure..", "..identityaccess.web..",
                        "..teachingschedule.api..", "..teachingschedule.application..", "..teachingschedule.domain..", "..teachingschedule.infrastructure..", "..teachingschedule.web..",
                        "..labresource.api..", "..labresource.application..", "..labresource.domain..", "..labresource.infrastructure..", "..labresource.web..",
                        "..reservation.api..", "..reservation.application..", "..reservation.domain..", "..reservation.infrastructure..", "..reservation.web..",
                        "..environmentreadiness.api..", "..environmentreadiness.application..", "..environmentreadiness.domain..", "..environmentreadiness.infrastructure..", "..environmentreadiness.web..",
                        "..teachingcoordination.api..", "..teachingcoordination.application..", "..teachingcoordination.domain..", "..teachingcoordination.infrastructure..", "..teachingcoordination.web.."
                )
                .because("the host wires module configurations and never orchestrates business behavior")
                .check(ArchitectureClasses.ALL);
    }
}
