package org.example.backend.service;

import java.util.List;

public interface ReservationCatalogQuery {
    List<ReservationView> list(ReservationAccessScope scope);

    ReservationView findById(long id, ReservationAccessScope scope);
}
