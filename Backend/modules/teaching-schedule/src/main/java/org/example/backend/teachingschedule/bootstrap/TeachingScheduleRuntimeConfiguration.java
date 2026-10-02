package org.example.backend.teachingschedule.bootstrap;

import org.example.backend.teachingschedule.adapter.in.web.TeachingScheduleQueryController;
import org.example.backend.teachingschedule.adapter.out.persistence.TeachingScheduleReadMapper;
import org.example.backend.teachingschedule.api.TeachingScheduleQuery;
import org.example.backend.teachingschedule.application.DefaultTeachingScheduleQuery;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@MapperScan(basePackageClasses = TeachingScheduleReadMapper.class)
public class TeachingScheduleRuntimeConfiguration {
    @Bean
    public TeachingScheduleQuery teachingScheduleQuery(TeachingScheduleReadMapper mapper) {
        return new DefaultTeachingScheduleQuery(mapper);
    }

    @Bean
    public TeachingScheduleQueryController teachingScheduleQueryController(TeachingScheduleQuery query) {
        return new TeachingScheduleQueryController(query);
    }
}
