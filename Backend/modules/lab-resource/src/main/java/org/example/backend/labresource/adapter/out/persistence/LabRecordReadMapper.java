package org.example.backend.labresource.adapter.out.persistence;

import org.apache.ibatis.annotations.Param;
import org.example.backend.labresource.application.port.out.LabRecord;

import java.util.List;

public interface LabRecordReadMapper {
    List<LabRecord> findAll(@Param("managerUserId") Long managerUserId);

    LabRecord findById(@Param("id") long id, @Param("managerUserId") Long managerUserId);
}
