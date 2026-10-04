package org.example.backend.reservation.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ReservationReviewRequest(@JsonAlias("note") String reviewRemark) {
}
