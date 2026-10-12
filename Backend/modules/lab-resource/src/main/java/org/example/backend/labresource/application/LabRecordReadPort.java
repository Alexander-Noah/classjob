package org.example.backend.labresource.application;

import java.util.List;

public interface LabRecordReadPort {
    List<LabRecord> findAll(Long managerUserId);

    LabRecord findById(long id, Long managerUserId);
}
