/*
 * 软件环境能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import org.example.backend.result.Result;
import org.example.backend.entity.softwareEntity;

public interface softwareService {
    Result getLabSoftware();

    Result InserterLabSoftware(softwareEntity software);

    Result updateLabSoftware(Long id, softwareEntity software);

    Result deleteLabSoftware(Long id);
}
