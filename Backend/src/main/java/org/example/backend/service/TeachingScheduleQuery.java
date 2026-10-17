/*
 * 教学课表查询能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import java.util.List;

public interface TeachingScheduleQuery {
    /**
     * 搜索当前操作。
     * @param criteria criteria参数。
     * @return 当前操作得到的记录集合。
     */
    List<ScheduleEntryView> search(ScheduleSearchCriteria criteria);

    /**
     * 摘要的处理逻辑。
     * @return ScheduleSummaryView 类型的处理结果。
     */
    ScheduleSummaryView summary();

    /**
     * 列出学期。
     * @return 当前操作得到的记录集合。
     */
    List<String> listSemesters();
}
