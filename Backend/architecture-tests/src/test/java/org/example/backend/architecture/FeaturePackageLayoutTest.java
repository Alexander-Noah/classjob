package org.example.backend.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FeaturePackageLayoutTest {
    private static final String BASE = "org.example.backend.";
    private static final Set<String> FEATURES = Set.of(
            "identityaccess",
            "teachingschedule",
            "labresource",
            "reservation",
            "environmentreadiness",
            "teachingcoordination"
    );

    @Test
    void featureModulesUseOnlyTheApprovedIndustryRecognizableLayers() {
        ArchitectureClasses.ALL.stream()
                .filter(this::belongsToFeatureModule)
                .forEach(javaClass -> assertTrue(isApprovedPackage(javaClass.getPackageName()),
                        () -> javaClass.getName()
                                + " must reside in the module root, api, application, domain, infrastructure, or web package"));
    }

    private boolean belongsToFeatureModule(JavaClass javaClass) {
        return FEATURES.stream().anyMatch(feature -> javaClass.getPackageName().startsWith(BASE + feature));
    }

    private boolean isApprovedPackage(String packageName) {
        return FEATURES.stream().anyMatch(feature -> {
            String root = BASE + feature;
            return packageName.equals(root)
                    || packageName.equals(root + ".api")
                    || packageName.startsWith(root + ".api.")
                    || packageName.equals(root + ".application")
                    || packageName.startsWith(root + ".application.")
                    || packageName.equals(root + ".domain")
                    || packageName.startsWith(root + ".domain.")
                    || packageName.equals(root + ".infrastructure")
                    || packageName.startsWith(root + ".infrastructure.")
                    || packageName.equals(root + ".web")
                    || packageName.startsWith(root + ".web.");
        });
    }
}
