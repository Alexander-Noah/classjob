package org.example.backend.architecture;

import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class HostPurityTest {
    @Test
    void hostDoesNotDependOnBusinessApiApplicationDomainOrAdapters() {
        noClasses().that().resideInAPackage("..host..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..identityaccess.api..", "..identityaccess.application..", "..identityaccess.domain..", "..identityaccess.adapter..",
                        "..teachingschedule.api..", "..teachingschedule.application..", "..teachingschedule.domain..", "..teachingschedule.adapter..",
                        "..labresource.api..", "..labresource.application..", "..labresource.domain..", "..labresource.adapter..",
                        "..reservation.api..", "..reservation.application..", "..reservation.domain..", "..reservation.adapter..",
                        "..environmentreadiness.api..", "..environmentreadiness.application..", "..environmentreadiness.domain..", "..environmentreadiness.adapter..",
                        "..teachingcoordination.api..", "..teachingcoordination.application..", "..teachingcoordination.domain..", "..teachingcoordination.adapter.."
                )
                .because("the host wires bootstrap configurations and never orchestrates business behavior")
                .check(ArchitectureClasses.ALL);
    }
}
