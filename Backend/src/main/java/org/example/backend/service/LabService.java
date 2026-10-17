/*
 * 实验室能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import org.example.backend.entity.LabEntity;

import java.util.List;

public interface LabService {
    List<LabEntity> getLabs();

    List<LabEntity> getLabs(Integer managerUserId);

    LabEntity addLab(LabEntity lab);

    Object getLabById(Integer id);

    void updateLab(Integer id, LabEntity lab);

    List<LabEntity> updateLabs(List<LabEntity> labs);

    void deleteLab(Integer id);
}
