package org.example.backend.controller;
import org.example.backend.entity.ReservationsEntity;
import org.example.backend.service.*;
import org.example.backend.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LegacyReservationEndpointBehaviorTest {
    @Test void legacyReservationRoutesPreserveParametersAndBatchDefaults() throws Exception{ReservationsService service=mock(ReservationsService.class);var mvc=MockMvcBuilders.standaloneSetup(new ReservationsController(service)).build();mvc.perform(get("/public/labs/7/reservation-profile").param("code","signed")).andExpect(status().isOk());mvc.perform(post("/public/reservations/scan").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());mvc.perform(get("/public/reservations/9/status").param("contact","123")).andExpect(status().isOk());mvc.perform(put("/reservations/9").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());mvc.perform(put("/reservations/batch").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());mvc.perform(put("/reservations/batch").contentType(MediaType.APPLICATION_JSON).content("{\"resource\":\"reservations\",\"records\":[{\"id\":9}]}")).andExpect(status().isOk());mvc.perform(delete("/reservations/9")).andExpect(status().isOk());mvc.perform(post("/reservations/reset")).andExpect(status().isOk());verify(service).getScanReservationProfile(7,"signed");verify(service).submitScanReservation(any());verify(service).getScanReservationStatus(9,"123");verify(service).updateReservation(eq(9),any());verify(service).getReservation(9);verify(service).replaceReservations(List.of());verify(service).replaceReservations(argThat(rows->rows.size()==1&&rows.get(0).getId()==9));verify(service).deleteReservation(9);verify(service).getReservations();}
    @Test void dashboardUsesManagerIdentityUnlessCallerIsSystemAdministrator() throws Exception{DashboardService service=mock(DashboardService.class);var mvc=MockMvcBuilders.standaloneSetup(new DashboardController(service)).build();mvc.perform(get("/dashboard/overview").requestAttr(JwtAuthenticationFilter.AUTH_ROLE_ATTRIBUTE,"systemAdmin").requestAttr(JwtAuthenticationFilter.AUTH_USER_ID_ATTRIBUTE,7)).andExpect(status().isOk());mvc.perform(get("/dashboard/overview").requestAttr(JwtAuthenticationFilter.AUTH_USER_ID_ATTRIBUTE,"7")).andExpect(status().isOk());mvc.perform(get("/dashboard/overview").requestAttr(JwtAuthenticationFilter.AUTH_USER_ID_ATTRIBUTE,"invalid")).andExpect(status().isOk());mvc.perform(get("/dashboard/overview")).andExpect(status().isOk());verify(service).getOverview(7);verify(service,times(3)).getOverview(null);}
}
