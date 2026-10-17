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
    long count(Map<String, Object> filters);
    List<ScheduleEntryView> search(Map<String, Object> filters);
    List<Map<String, Object>> aggregate(Map<String, Object> filters);
}
