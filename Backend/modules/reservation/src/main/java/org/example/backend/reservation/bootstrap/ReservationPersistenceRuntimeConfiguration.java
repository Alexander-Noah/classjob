package org.example.backend.reservation.bootstrap;

import org.example.backend.reservation.adapter.out.persistence.MyBatisReservationPersistenceAdapter;
import org.example.backend.reservation.adapter.out.persistence.ReservationPersistenceMapper;
import org.example.backend.reservation.application.port.out.ReservationPersistencePort;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@MapperScan(basePackageClasses = ReservationPersistenceMapper.class)
@Import(ReservationModuleConfiguration.class)
public class ReservationPersistenceRuntimeConfiguration {
    @Bean
    public ReservationPersistencePort reservationPersistencePort(ReservationPersistenceMapper mapper) {
        return new MyBatisReservationPersistenceAdapter(mapper);
    }
}
