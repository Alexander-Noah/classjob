/*
 * 实验室预约能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import java.util.List;

public interface ReservationCatalogQuery {
    /**
     * 列出当前操作。
     * @param scope scope参数。
     * @return 当前操作得到的记录集合。
     */
    List<ReservationView> list(ReservationAccessScope scope);

    /**
     * 查找By标识。
     * @param id 标识参数。
     * @param scope scope参数。
     * @return ReservationView 类型的处理结果。
     */
    ReservationView findById(long id, ReservationAccessScope scope);
}
