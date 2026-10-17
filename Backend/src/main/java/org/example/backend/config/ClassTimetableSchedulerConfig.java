package org.example.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/** 为课表采集提供独立调度线程，避免长时间浏览器任务占用其他调度器。 */
@Configuration
public class ClassTimetableSchedulerConfig {

    /**
     * class课表TaskScheduler的处理逻辑。
     * @return ThreadPoolTaskScheduler 类型的处理结果。
     */
    @Bean
    public ThreadPoolTaskScheduler classTimetableTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("class-timetable-crawler-");
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.initialize();
        return scheduler;
    }
}
