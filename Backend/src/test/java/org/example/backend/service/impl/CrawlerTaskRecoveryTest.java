package org.example.backend.service.impl;

import org.example.backend.mapper.ClassTimetableMapper;
import org.example.backend.service.TeachingScheduleQuery;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.Map;

import static org.mockito.Mockito.*;

class CrawlerTaskRecoveryTest {
    @Test
    void reconcilesInterruptedTasksBeforeSchedulingNewWork() {
        var mapper = mock(ClassTimetableMapper.class);
        when(mapper.getCrawlerConfig(anyString())).thenReturn(Map.of("scheduleEnabled", false));
        var scheduler = mock(ThreadPoolTaskScheduler.class);
        var service = new ClassTimetableServiceImpl(mapper, mock(TeachingScheduleQuery.class),
                new MockEnvironment(), scheduler, "test-only-credential-key-aaaaaaaaaaaaaaaaaaaa");

        service.initializeCrawlerScheduler();

        var order = inOrder(mapper);
        order.verify(mapper).failInterruptedCrawlerTasks();
        order.verify(mapper).getCrawlerConfig(anyString());
        verifyNoInteractions(scheduler);
    }
}
