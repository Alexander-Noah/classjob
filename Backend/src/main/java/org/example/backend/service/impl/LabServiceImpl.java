/*
 * 实验室实现模块。
 * 主要协作组件：LabMapper、BusinessLoopService。
 */
package org.example.backend.service.impl;

import org.example.backend.entity.LabEntity;
import org.example.backend.mapper.LabMapper;
import org.example.backend.service.BusinessLoopService;
import org.example.backend.service.LabService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class LabServiceImpl implements LabService {
    private final LabMapper labMapper;
    private final BusinessLoopService businessLoopService;

    /**
     * 构造 LabServiceImpl，初始化当前实例。
     * @param labMapper 实验室数据映射器参数。
     * @param businessLoopService businessLoop服务参数。
     */
    public LabServiceImpl(LabMapper labMapper, BusinessLoopService businessLoopService) {
        this.labMapper = labMapper;
        this.businessLoopService = businessLoopService;
    }

    /**
     * 获取实验室。
     * @return 当前操作得到的记录集合。
     */
    @Override
    public List<LabEntity> getLabs() {
        List<LabEntity> labs = labMapper.getLabs();
        return labs == null ? List.of() : labs;
    }

    /**
     * 获取实验室。
     * @param managerUserId manager用户标识参数。
     * @return 当前操作得到的记录集合。
     */
    @Override
    public List<LabEntity> getLabs(Integer managerUserId) {
        if (managerUserId == null) {
            return getLabs();
        }
        List<LabEntity> labs = labMapper.getLabsByManagerUserId(managerUserId);
        return labs == null ? List.of() : labs;
    }

    /**
     * 添加实验室。
     * @param lab 实验室参数。
     * @return LabEntity 类型的处理结果。
     */
    @Override
    public LabEntity addLab(LabEntity lab) {
        if (lab.getOpenStatus() == null || lab.getOpenStatus().isBlank()) {
            lab.setOpenStatus("开放");
        }
        labMapper.addLab(lab);
        LabEntity savedLab = labMapper.getLabById(lab.getId());
        LabEntity result = savedLab == null ? lab : savedLab;
        businessLoopService.recordEvent("lab", "create", result.getLabName(), result.getOpenStatus(), Map.of(
                "labId", result.getId(),
                "capacity", result.getCapacity() == null ? 0 : result.getCapacity()
        ));
        return result;
    }

    /**
     * 获取实验室By标识。
     * @param id 标识参数。
     * @return Object 类型的处理结果。
     */
    @Override
    public Object getLabById(Integer id) {
        LabEntity lab = labMapper.getLabById(id);
        if (lab != null) {
            return lab;
        }
        return null;
    }

    /**
     * 更新实验室。
     * @param id 标识参数。
     * @param lab 实验室参数。
     */
    @Override
    public void updateLab(Integer id, LabEntity lab) {
        labMapper.updateLab(id, lab);
        LabEntity updatedLab = labMapper.getLabById(id);
        businessLoopService.recordEvent("lab", "update", updatedLab == null ? String.valueOf(id) : updatedLab.getLabName(), updatedLab == null ? "已更新" : updatedLab.getOpenStatus(), Map.of(
                "labId", id
        ));
    }

    /**
     * 更新实验室。
     * @param labs 实验室参数。
     * @return 当前操作得到的记录集合。
     */
    @Override
    public List<LabEntity> updateLabs(List<LabEntity> labs) {
        if (labs == null || labs.isEmpty()) {
            return getLabs();
        }

        for (LabEntity lab : labs) {
            if (lab.getId() != null) {
                updateLab(lab.getId(), lab);
            }
        }

        return getLabs();
    }

    /**
     * 删除实验室。
     * 涉及：通过 Spring 代理入口应用事务配置。
     * @param id 标识参数。
     */
    @Override
    @Transactional
    public void deleteLab(Integer id) {
        LabEntity lab = labMapper.getLabById(id);
        if (lab == null) {
            throw new IllegalArgumentException("实验室不存在或已删除");
        }

        labMapper.deleteRepairsByLabId(id);
        labMapper.deleteReservationsByLabId(id);
        labMapper.deleteCourseEnvironmentsByLabId(id);
        labMapper.deleteLabSoftwareByLabId(id);
        labMapper.deleteDevicesByLabId(id);
        int deletedRows = labMapper.deleteLab(id);

        if (deletedRows == 0) {
            throw new IllegalArgumentException("实验室不存在或已删除");
        }

        businessLoopService.recordEvent("lab", "delete", lab.getLabName(), "已删除", Map.of(
                "labId", id
        ));
    }
}
