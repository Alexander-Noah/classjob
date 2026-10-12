package org.example.backend.service;

import java.time.Instant;

public record TimeRange(Instant startsAt, Instant endsAt) {
}
