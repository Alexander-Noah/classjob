/*
 * ManagerNamePort能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import java.util.Map;
import java.util.Set;

public interface ManagerNamePort {
    /**
     * 查找名称集合。
     * @param managerUserIds manager用户标识集合参数。
     * @return 当前操作返回的键值结果。
     */
    Map<Long, String> findNames(Set<Long> managerUserIds);
}
