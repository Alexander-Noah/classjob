package org.example.backend.teachingschedule;

import org.example.backend.teachingschedule.web.TeachingScheduleQueryController;
import org.example.backend.teachingschedule.infrastructure.MyBatisTeachingScheduleReadAdapter;
import org.example.backend.teachingschedule.infrastructure.TeachingScheduleReadMapper;
import org.example.backend.teachingschedule.api.TeachingScheduleQuery;
import org.example.backend.teachingschedule.api.ScheduleOccupancyQuery;
import org.example.backend.teachingschedule.application.DefaultScheduleOccupancyQuery;
import org.example.backend.teachingschedule.application.DefaultTeachingScheduleQuery;
import org.example.backend.teachingschedule.application.TeachingScheduleReadPort;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@MapperScan(basePackageClasses = TeachingScheduleReadMapper.class)
@Import(TeachingScheduleModuleConfiguration.class)
public class TeachingScheduleRuntimeConfiguration {
    @Bean
    public TeachingScheduleReadPort teachingScheduleReadPort(TeachingScheduleReadMapper mapper) {
        return new MyBatisTeachingScheduleReadAdapter(mapper);
    }

    @Bean
    public TeachingScheduleQuery teachingScheduleQuery(TeachingScheduleReadPort readPort) {
        return new DefaultTeachingScheduleQuery(readPort);
    }

    @Bean
    public ScheduleOccupancyQuery scheduleOccupancyQuery(TeachingScheduleReadPort readPort) {
        return new DefaultScheduleOccupancyQuery(readPort);
    }

    @Bean
    public TeachingScheduleQueryController teachingScheduleQueryController(TeachingScheduleQuery query) {
        return new TeachingScheduleQueryController(query);
    }
}
