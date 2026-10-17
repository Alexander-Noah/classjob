/*
 * 实验室实现模块。
 * 主要协作组件：LabRecordReadMapper。
 */
package org.example.backend.service;

import org.example.backend.mapper.LabRecordReadMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public final class LabRecordReadService {
    private final LabRecordReadMapper mapper;

    public LabRecordReadService(LabRecordReadMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    public List<LabRecord> findAll(Long managerUserId) {
        List<LabRecord> records = mapper.findAll(managerUserId);
        return records == null ? List.of() : records;
    }

    public LabRecord findById(long id, Long managerUserId) {
        return mapper.findById(id, managerUserId);
    }
}
