package org.example.backend.mapper;

import org.apache.ibatis.annotations.Param;
import org.example.backend.service.LabRecord;

import java.util.List;

public interface LabRecordReadMapper {
    List<LabRecord> findAll(@Param("managerUserId") Long managerUserId);

    LabRecord findById(@Param("id") long id, @Param("managerUserId") Long managerUserId);
}
