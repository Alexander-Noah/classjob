package org.example.backend.architecture;

import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class LabResourceLayerBoundaryTest {
    @Test
    void apiAndApplicationLayersDoNotDependOnAdaptersOrBootstrap() {
        noClasses().that().resideInAnyPackage(
                        "..labresource.api..",
                        "..labresource.application.."
                )
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..labresource.adapter..",
                        "..labresource.bootstrap.."
                )
                .because("module policy points inward and infrastructure implements application ports")
                .check(ArchitectureClasses.ALL);
    }

    @Test
    void publicApiDoesNotExposeApplicationOrInfrastructureTypes() {
        noClasses().that().resideInAPackage("..labresource.api..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..labresource.application..",
                        "..labresource.adapter..",
                        "..labresource.bootstrap.."
                )
                .because("the public API remains stable when internal layers are replaced")
                .check(ArchitectureClasses.ALL);
    }
}
