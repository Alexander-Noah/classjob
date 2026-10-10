package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.backend.entity.AuditEventEntity;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AuditEventMapper {
    int insertEvent(AuditEventEntity event);

    List<AuditEventEntity> listRecent(@Param("limit") int limit);

    long countByTypeAndRange(
            @Param("aggregateType") String aggregateType,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
