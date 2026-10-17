/*
 * AI 课表查询的 MyBatis 持久化契约。
 * SQL 定义位于 resources/mapper/AiScheduleReadMapper.xml，修改参数或字段需同步接口与 XML。
 */
package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.example.backend.service.ScheduleEntryView;
import java.util.List;
import java.util.Map;

@Mapper
public interface AiScheduleReadMapper {
    /**
     * 统计当前操作。
     * @param filters 筛选条件参数。
     * @return long 类型的处理结果。
     */
    long count(Map<String, Object> filters);
    /**
     * 搜索当前操作。
     * @param filters 筛选条件参数。
     * @return 当前操作得到的记录集合。
     */
    List<ScheduleEntryView> search(Map<String, Object> filters);
    /**
     * 汇总当前操作。
     * @param filters 筛选条件参数。
     * @return 当前操作得到的记录集合。
     */
    List<Map<String, Object>> aggregate(Map<String, Object> filters);
}
