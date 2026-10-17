/*
 * 软件环境能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import org.example.backend.result.Result;
import org.example.backend.entity.softwareEntity;

public interface softwareService {
    /**
     * 获取实验室Software。
     * @return Result 类型的处理结果。
     */
    Result getLabSoftware();

    /**
     * Inserter实验室Software的处理逻辑。
     * @param software 软件环境参数。
     * @return Result 类型的处理结果。
     */
    Result InserterLabSoftware(softwareEntity software);

    /**
     * 更新实验室Software。
     * @param id 标识参数。
     * @param software 软件环境参数。
     * @return Result 类型的处理结果。
     */
    Result updateLabSoftware(Long id, softwareEntity software);

    /**
     * 删除实验室Software。
     * @param id 标识参数。
     * @return Result 类型的处理结果。
     */
    Result deleteLabSoftware(Long id);
}
