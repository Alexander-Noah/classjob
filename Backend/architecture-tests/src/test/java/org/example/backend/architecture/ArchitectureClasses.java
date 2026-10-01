package org.example.backend.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

final class ArchitectureClasses {
    static final JavaClasses ALL = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("org.example.backend");

    static final String[] BUSINESS_PACKAGES = {
            "..identityaccess..",
            "..teachingschedule..",
            "..labresource..",
            "..reservation..",
            "..environmentreadiness..",
            "..teachingcoordination.."
    };

    private ArchitectureClasses() {
    }
}
