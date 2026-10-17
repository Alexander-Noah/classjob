/*
 * ModuleRecordService能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import java.util.List;
import java.util.Map;

public interface ModuleRecordService {
    /**
     * 列出Module记录集合。
     * @param moduleName module名称参数。
     * @return 当前操作得到的记录集合。
     */
    List<Map<String, Object>> listModuleRecords(String moduleName);

    /**
     * replaceModule记录集合的处理逻辑。
     * @param moduleName module名称参数。
     * @param records 记录集合参数。
     * @return 当前操作得到的记录集合。
     */
    List<Map<String, Object>> replaceModuleRecords(String moduleName, List<Map<String, Object>> records);

    /**
     * 重置Module记录集合。
     * @param moduleName module名称参数。
     * @return 当前操作得到的记录集合。
     */
    List<Map<String, Object>> resetModuleRecords(String moduleName);
}
