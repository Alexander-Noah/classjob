package org.example.backend.config;

import org.example.backend.service.CapabilityResult;
import org.example.backend.service.CapabilityStatus;
import org.example.backend.service.EnvironmentSnapshot;
import org.example.backend.service.EnvironmentSnapshotPort;
import org.example.backend.service.EnvironmentSnapshotQuery;
import org.example.backend.service.EnvironmentSnapshotView;
import org.example.backend.service.ReservationSnapshot;
import org.example.backend.service.ReservationSnapshotPort;
import org.example.backend.service.ReservationSnapshotQuery;
import org.example.backend.service.ReservationSnapshotView;
import org.example.backend.service.ScheduleSnapshot;
import org.example.backend.service.ScheduleSnapshotPort;
import org.example.backend.service.ScheduleSnapshotQuery;
import org.example.backend.service.ScheduleSnapshotView;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides fallback snapshot queries and converts their views into the
 * capability types consumed by teaching coordination.
 */
@Configuration(proxyBeanMethods = false)
public class FeatureConfiguration {
    /**
     * unavailable环境快照查询的处理逻辑。
     * @return EnvironmentSnapshotQuery 类型的处理结果。
     */
    @Bean
    @ConditionalOnMissingBean(EnvironmentSnapshotQuery.class)
    public EnvironmentSnapshotQuery unavailableEnvironmentSnapshotQuery() {
        return /** 处理当前调用传入的回调逻辑。 入参：teachingTaskId。 */ teachingTaskId -> CapabilityResult.unavailable("environment data is unavailable");
    }

    /**
     * unavailable预约快照查询的处理逻辑。
     * @return ReservationSnapshotQuery 类型的处理结果。
     */
    @Bean
    @ConditionalOnMissingBean(ReservationSnapshotQuery.class)
    public ReservationSnapshotQuery unavailableReservationSnapshotQuery() {
        return /** 处理当前调用传入的回调逻辑。 入参：teachingTaskId。 */ teachingTaskId -> CapabilityResult.unavailable("reservation data is unavailable");
    }

    /**
     * unavailable课表快照查询的处理逻辑。
     * @return ScheduleSnapshotQuery 类型的处理结果。
     */
    @Bean
    @ConditionalOnMissingBean(ScheduleSnapshotQuery.class)
    public ScheduleSnapshotQuery unavailableScheduleSnapshotQuery() {
        return /** 处理当前调用传入的回调逻辑。 入参：teachingTaskId。 */ teachingTaskId -> CapabilityResult.unavailable("teaching schedule data is unavailable");
    }

    /**
     * 环境快照端口的处理逻辑。
     * @param query 查询参数。
     * @return EnvironmentSnapshotPort 类型的处理结果。
     */
    @Bean
    public EnvironmentSnapshotPort environmentSnapshotPort(EnvironmentSnapshotQuery query) {
        return /** 处理当前调用传入的回调逻辑。 入参：teachingTaskId。 */ teachingTaskId -> {
            CapabilityResult<EnvironmentSnapshotView> result = query.findByTeachingTaskId(teachingTaskId);
            if (result.status() == CapabilityStatus.UNAVAILABLE) {
                return CapabilityResult.unavailable(result.message());
            }
            EnvironmentSnapshotView view = result.value();
            return CapabilityResult.available(new EnvironmentSnapshot(view.teachingTaskId(), view.requestId(), view.status()));
        };
    }

    /**
     * 预约快照端口的处理逻辑。
     * @param query 查询参数。
     * @return ReservationSnapshotPort 类型的处理结果。
     */
    @Bean
    public ReservationSnapshotPort reservationSnapshotPort(ReservationSnapshotQuery query) {
        return /** 处理当前调用传入的回调逻辑。 入参：teachingTaskId。 */ teachingTaskId -> {
            CapabilityResult<ReservationSnapshotView> result = query.findByTeachingTaskId(teachingTaskId);
            if (result.status() == CapabilityStatus.UNAVAILABLE) {
                return CapabilityResult.unavailable(result.message());
            }
            ReservationSnapshotView view = result.value();
            return CapabilityResult.available(new ReservationSnapshot(view.teachingTaskId(), view.reservationId(), view.status()));
        };
    }

    /**
     * 安排快照端口。
     * @param query 查询参数。
     * @return ScheduleSnapshotPort 类型的处理结果。
     */
    @Bean
    public ScheduleSnapshotPort scheduleSnapshotPort(ScheduleSnapshotQuery query) {
        return /** 处理当前调用传入的回调逻辑。 入参：teachingTaskId。 */ teachingTaskId -> {
            CapabilityResult<ScheduleSnapshotView> result = query.findByTeachingTaskId(teachingTaskId);
            if (result.status() == CapabilityStatus.UNAVAILABLE) {
                return CapabilityResult.unavailable(result.message());
            }
            ScheduleSnapshotView view = result.value();
            return CapabilityResult.available(new ScheduleSnapshot(
                    view.teachingTaskId(), view.courseName(), view.teacherName(), view.startsAt(), view.endsAt()
            ));
        };
    }

}
