package org.example.backend.host;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication(scanBasePackages = "org.example.backend.host")
@Import(ModularHostConfiguration.class)
public class ModularBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(ModularBackendApplication.class, args);
    }
}
