package org.example.backend.reservation.api;

import org.example.backend.reservation.domain.ReservationStatus;
import org.example.backend.reservation.application.CreateReservationCommand;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReservationContractsTest {
    @Test
    void keepsAuthenticatedActorIdentitySeparateFromCreateData() {
        CreateReservationCommand command = new CreateReservationCommand(
                "王老师", "计算机学院", "13800000000", 7L, LocalDate.of(2026, 10, 1),
                "09:00-11:00", "课程", 30, "软件工程实验"
        );

        assertEquals("王老师", command.applicantName());
        assertEquals(7L, command.labId());
        assertNotEquals(42L, command.labId());
        assertEquals(42L, ReservationActor.teacher(42L).userId());
    }

    @Test
    void rejectsMissingOrNonPositiveActorIdentifiers() {
        assertThrows(IllegalArgumentException.class, () -> new ReservationActor(null, ReservationActorRole.TEACHER));
        assertThrows(IllegalArgumentException.class, () -> new ReservationActor(0L, ReservationActorRole.TEACHER));
        assertThrows(IllegalArgumentException.class, () -> new ReservationActor(-1L, ReservationActorRole.TEACHER));
    }

    @Test
    void preservesDistinctTeacherLaboratoryAdminAndSystemAdminRoles() {
        assertEquals(ReservationActorRole.TEACHER, ReservationActor.teacher(1L).role());
        assertEquals(ReservationActorRole.LABORATORY_ADMINISTRATOR, ReservationActor.laboratoryAdministrator(2L).role());
        assertEquals(ReservationActorRole.SYSTEM_ADMINISTRATOR, ReservationActor.systemAdministrator(3L).role());
    }

    @Test
    void exposesCanonicalImmutableReservationView() {
        ReservationView view = new ReservationView(
                1L, 2L, "王老师", "计算机学院", "13800000000", 3L,
                LocalDate.of(2026, 10, 1), "09:00-11:00", "课程", 30, "软件工程实验",
                ReservationStatus.PENDING, false, null, null,
                LocalDateTime.of(2026, 9, 30, 10, 0), LocalDateTime.of(2026, 9, 30, 10, 0),
                "创新实验室", "LAB-101"
        );

        assertEquals(ReservationStatus.PENDING, view.status());
        assertEquals("LAB-101", view.labCode());
    }
}
