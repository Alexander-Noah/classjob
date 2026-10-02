package org.example.backend.teachingschedule.bootstrap;

import org.example.backend.teachingschedule.adapter.in.web.TeachingScheduleQueryController;
import org.example.backend.teachingschedule.adapter.out.persistence.MyBatisTeachingScheduleReadAdapter;
import org.example.backend.teachingschedule.adapter.out.persistence.TeachingScheduleReadMapper;
import org.example.backend.teachingschedule.api.TeachingScheduleQuery;
import org.example.backend.teachingschedule.application.DefaultTeachingScheduleQuery;
import org.example.backend.teachingschedule.application.port.out.TeachingScheduleReadPort;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@MapperScan(basePackageClasses = TeachingScheduleReadMapper.class)
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
    public TeachingScheduleQueryController teachingScheduleQueryController(TeachingScheduleQuery query) {
        return new TeachingScheduleQueryController(query);
    }
}
