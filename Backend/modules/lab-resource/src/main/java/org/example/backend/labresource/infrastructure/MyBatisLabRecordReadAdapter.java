package org.example.backend.labresource.infrastructure;

import org.example.backend.labresource.application.LabRecord;
import org.example.backend.labresource.application.LabRecordReadPort;

import java.util.List;
import java.util.Objects;

public final class MyBatisLabRecordReadAdapter implements LabRecordReadPort {
    private final LabRecordReadMapper mapper;

    public MyBatisLabRecordReadAdapter(LabRecordReadMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    @Override
    public List<LabRecord> findAll(Long managerUserId) {
        List<LabRecord> records = mapper.findAll(managerUserId);
        return records == null ? List.of() : records;
    }

    @Override
    public LabRecord findById(long id, Long managerUserId) {
        return mapper.findById(id, managerUserId);
    }
}
