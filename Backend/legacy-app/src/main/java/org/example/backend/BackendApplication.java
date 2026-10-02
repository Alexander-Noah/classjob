package org.example.backend;

import org.example.backend.config.LocalDevelopmentSecretsInitializer;
import org.example.backend.kernel.bootstrap.KernelConfiguration;
import org.example.backend.teachingschedule.bootstrap.TeachingScheduleRuntimeConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.nio.file.Path;

@EnableScheduling
@SpringBootApplication(scanBasePackages = {
        "org.example.backend.config",
        "org.example.backend.controller",
        "org.example.backend.security",
        "org.example.backend.service",
        "org.example.backend.websocket"
})
@MapperScan(basePackages = "org.example.backend.mapper")
@Import({KernelConfiguration.class, TeachingScheduleRuntimeConfiguration.class})
public class BackendApplication {

    public static void main(String[] args) {
        LocalDevelopmentSecretsInitializer.ensure(Path.of(""));
        SpringApplication.run(BackendApplication.class, args);
    }

}
