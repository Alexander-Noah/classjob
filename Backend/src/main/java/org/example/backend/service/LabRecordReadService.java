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

    /**
     * 构造 LabRecordReadService，初始化当前实例。
     * @param mapper 数据映射器参数。
     */
    public LabRecordReadService(LabRecordReadMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    /**
     * 查找All。
     * @param managerUserId manager用户标识参数。
     * @return 当前操作得到的记录集合。
     */
    public List<LabRecord> findAll(Long managerUserId) {
        List<LabRecord> records = mapper.findAll(managerUserId);
        return records == null ? List.of() : records;
    }

    /**
     * 查找By标识。
     * @param id 标识参数。
     * @param managerUserId manager用户标识参数。
     * @return LabRecord 类型的处理结果。
     */
    public LabRecord findById(long id, Long managerUserId) {
        return mapper.findById(id, managerUserId);
    }
}
