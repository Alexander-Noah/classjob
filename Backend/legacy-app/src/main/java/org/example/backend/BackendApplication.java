package org.example.backend;

import org.example.backend.config.LocalDevelopmentSecretsInitializer;
import org.example.backend.config.LabResourceHostConfiguration;
import org.example.backend.bridge.reservationlab.ReservationLabResourceBridgeConfiguration;
import org.example.backend.bridge.reservationschedule.ReservationTeachingScheduleBridgeConfiguration;
import org.example.backend.kernel.bootstrap.KernelConfiguration;
import org.example.backend.labresource.bootstrap.LabResourceRuntimeConfiguration;
import org.example.backend.reservation.bootstrap.ReservationPersistenceRuntimeConfiguration;
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
        "org.example.backend.service"
})
@MapperScan(basePackages = "org.example.backend.mapper")
@Import({
        KernelConfiguration.class,
        TeachingScheduleRuntimeConfiguration.class,
        LabResourceHostConfiguration.class,
        LabResourceRuntimeConfiguration.class,
        ReservationLabResourceBridgeConfiguration.class,
        ReservationTeachingScheduleBridgeConfiguration.class,
        ReservationPersistenceRuntimeConfiguration.class
})
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(BackendApplication.class);
        if (LocalDevelopmentSecretsInitializer.ensure(Path.of(""))) {
            application.setAdditionalProfiles("local");
        }
        application.run(args);
    }

}
