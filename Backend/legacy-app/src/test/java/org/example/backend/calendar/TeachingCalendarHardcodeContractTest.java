package org.example.backend.calendar;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

class TeachingCalendarHardcodeContractTest {
    @Test
    void backendServicesDoNotFallBackToAReleasedSemesterDate() throws IOException {
        for (String sourceFile : List.of(
                "ClassTimetableServiceImpl.java",
                "ReservationsServiceImpl.java"
        )) {
            Path source = Path.of("src/main/java/org/example/backend/service/impl", sourceFile);
            assertFalse(Files.readString(source).contains("2026-03-02"), sourceFile);
        }
    }
}
