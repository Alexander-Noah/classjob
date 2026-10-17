/*
 * 实验室预约能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import java.util.List;

public interface ReservationCatalogQuery {
    List<ReservationView> list(ReservationAccessScope scope);

    ReservationView findById(long id, ReservationAccessScope scope);
}
