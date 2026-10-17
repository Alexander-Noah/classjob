/*
 * BackendApplication实现模块。
 */
package org.example.backend;

import org.example.backend.config.LocalDevelopmentSecretsInitializer;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.nio.file.Path;

@EnableScheduling
@SpringBootApplication
@MapperScan(basePackages = "org.example.backend.mapper")
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(BackendApplication.class);
        if (LocalDevelopmentSecretsInitializer.ensure(Path.of(""))) {
            application.setAdditionalProfiles("local");
        }
        application.run(args);
    }

}
