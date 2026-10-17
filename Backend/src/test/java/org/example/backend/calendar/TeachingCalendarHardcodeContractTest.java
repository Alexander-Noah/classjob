/*
 * 教学日历回归测试；测试夹具与生产配置分离。
 * 覆盖场景：backendServicesDoNotFallBackToAReleasedSemesterDate。
 */
package org.example.backend.calendar;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

class TeachingCalendarHardcodeContractTest {
    /**
     * backend服务DoNotFallBack目标AReleased学期日期的处理逻辑。
     * @throws IOException 当前调用无法继续完成时抛出。
     */
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
