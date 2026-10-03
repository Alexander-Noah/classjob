package org.example.backend.reservation.api;

import java.util.List;

public interface ReservationCatalogQuery {
    List<ReservationView> list(ReservationAccessScope scope);

    ReservationView findById(long id, ReservationAccessScope scope);
}
