/*
 * ModuleRecordService能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import java.util.List;
import java.util.Map;

public interface ModuleRecordService {
    List<Map<String, Object>> listModuleRecords(String moduleName);

    List<Map<String, Object>> replaceModuleRecords(String moduleName, List<Map<String, Object>> records);

    List<Map<String, Object>> resetModuleRecords(String moduleName);
}
