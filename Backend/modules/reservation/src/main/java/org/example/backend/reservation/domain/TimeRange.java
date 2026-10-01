package org.example.backend.reservation.domain;

import java.time.Instant;

public record TimeRange(Instant startsAt, Instant endsAt) {
}
