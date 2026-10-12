package org.example.backend.labresource.infrastructure;

import org.apache.ibatis.annotations.Param;
import org.example.backend.labresource.application.LabRecord;

import java.util.List;

public interface LabRecordReadMapper {
    List<LabRecord> findAll(@Param("managerUserId") Long managerUserId);

    LabRecord findById(@Param("id") long id, @Param("managerUserId") Long managerUserId);
}
