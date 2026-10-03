package org.example.backend.labresource.application.port.out;

import java.util.List;

public interface LabRecordReadPort {
    List<LabRecord> findAll(Long managerUserId);

    LabRecord findById(long id, Long managerUserId);
}
