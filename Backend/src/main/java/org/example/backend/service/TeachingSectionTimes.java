package org.example.backend.service;

import java.time.LocalTime;

/** Same paired teaching periods used by reservation conflict checks. */
public final class TeachingSectionTimes {
    /**
     * 构造 TeachingSectionTimes，初始化当前实例。
     */
    private TeachingSectionTimes() {}
    private static final LocalTime[] STARTS = {
        LocalTime.of(8,30), LocalTime.of(8,30), LocalTime.of(10,20), LocalTime.of(10,20),
        LocalTime.of(14,0), LocalTime.of(14,0), LocalTime.of(15,50), LocalTime.of(15,50),
        LocalTime.of(18,40), LocalTime.of(18,40), LocalTime.of(20,20), LocalTime.of(20,20)
    };
    private static final LocalTime[] ENDS = {
        LocalTime.of(10,0), LocalTime.of(10,0), LocalTime.of(11,50), LocalTime.of(11,50),
        LocalTime.of(15,30), LocalTime.of(15,30), LocalTime.of(17,20), LocalTime.of(17,20),
        LocalTime.of(20,10), LocalTime.of(20,10), LocalTime.of(21,50), LocalTime.of(21,50)
    };
    /**
     * 启动当前操作。
     * @param section 节次参数。
     * @return LocalTime 类型的处理结果。
     */
    public static LocalTime start(int section) { return STARTS[section - 1]; }
    /**
     * 结束的处理逻辑。
     * @param section 节次参数。
     * @return LocalTime 类型的处理结果。
     */
    public static LocalTime end(int section) { return ENDS[section - 1]; }
    /**
     * 当前节次的处理逻辑。
     * @param time 时间参数。
     * @return Integer 类型的处理结果。
     */
    public static Integer currentSection(LocalTime time) {
        for (int i = 0; i < 12; i += 2) {
            if (!time.isBefore(STARTS[i]) && time.isBefore(ENDS[i])) return i + 1;
        }
        return null;
    }
}
