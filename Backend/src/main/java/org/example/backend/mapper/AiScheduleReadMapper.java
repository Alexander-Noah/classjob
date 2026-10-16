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
