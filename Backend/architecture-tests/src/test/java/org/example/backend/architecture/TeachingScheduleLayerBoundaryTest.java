package org.example.backend.architecture;

import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class TeachingScheduleLayerBoundaryTest {
    @Test
    void apiAndApplicationLayersDoNotDependOnWebOrInfrastructure() {
        noClasses().that().resideInAnyPackage(
                        "..teachingschedule.api..",
                        "..teachingschedule.application.."
                )
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..teachingschedule.web..",
                        "..teachingschedule.infrastructure.."
                )
                .because("module policy points inward and infrastructure implements application ports")
                .check(ArchitectureClasses.ALL);
    }

    @Test
    void publicApiDoesNotExposeApplicationOrInfrastructureTypes() {
        noClasses().that().resideInAPackage("..teachingschedule.api..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..teachingschedule.application..",
                        "..teachingschedule.web..",
                        "..teachingschedule.infrastructure.."
                )
                .because("the public API remains stable when internal layers are replaced")
                .check(ArchitectureClasses.ALL);
    }
}
