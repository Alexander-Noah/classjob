/*
 * 教学课表查询能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import java.util.List;

public interface TeachingScheduleQuery {
    List<ScheduleEntryView> search(ScheduleSearchCriteria criteria);

    ScheduleSummaryView summary();

    List<String> listSemesters();
}
