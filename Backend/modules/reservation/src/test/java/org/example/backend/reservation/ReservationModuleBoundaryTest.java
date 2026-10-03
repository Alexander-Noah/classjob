package org.example.backend.reservation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ReservationModuleBoundaryTest {
    @Test
    void publicModuleCodeDoesNotImportLegacyApplicationTypes() throws IOException {
        Path sourceRoot = Path.of("src/main/java/org/example/backend/reservation");
        try (Stream<Path> sources = Files.walk(sourceRoot)) {
            assertFalse(sources
                    .filter(path -> path.toString().endsWith(".java"))
                    .map(this::read)
                    .anyMatch(source -> source.contains("org.example.backend.entity")
                            || source.contains("org.example.backend.mapper")
                            || source.contains("org.example.backend.service")
                            || source.contains("org.example.backend.controller")
                            || source.contains("org.example.backend.security")
                            || source.contains("org.example.backend.result")));
        }
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
