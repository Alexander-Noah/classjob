package org.example.backend.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = "coverage.expected", matches = "true")
class CoverageAgentTest {
    /** 防止覆盖率插件因缺少 agent 数据而跳过报告，却把测试结果标为通过。 */
    @Test
    void coverageProfileReallyInstrumentsTheTestJvm() throws Exception {
        assertTrue(ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
                .anyMatch(argument -> argument.contains("jacoco")), "coverage agent was not attached to the test JVM");
        Object agent = Class.forName("org.jacoco.agent.rt.RT").getMethod("getAgent").invoke(null);
        Class.forName("org.jacoco.agent.rt.IAgent").getMethod("dump", boolean.class).invoke(agent, false);
        assertTrue(Files.isRegularFile(Path.of("target/jacoco.exec")), "coverage agent did not write target/jacoco.exec");
        assertTrue(Files.size(Path.of("target/jacoco.exec")) > 0, "coverage data file is empty");
    }
}
