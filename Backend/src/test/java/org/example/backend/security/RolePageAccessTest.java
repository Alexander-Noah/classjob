package org.example.backend.security;

import org.example.backend.controller.RolePageAccessController;
import org.example.backend.service.ModuleRecordService;
import org.example.backend.service.RolePageAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RolePageAccessTest {
    ModuleRecordService store;
    RolePageAccessService service;
    Map<String, List<Map<String,Object>>> saved;
    @BeforeEach void setup() throws Exception {
        store = mock(ModuleRecordService.class);
        saved = new HashMap<>();
        when(store.listModuleRecords(anyString())).thenAnswer(call -> saved.getOrDefault(call.getArgument(0), List.of()));
        when(store.replaceModuleRecords(anyString(), anyList())).thenAnswer(call -> {
            saved.put(call.getArgument(0), call.getArgument(1)); return call.getArgument(1);
        });
        service = new RolePageAccessService(store, new ObjectMapper());
    }

    @Test void defaultsPreserveExistingRoleAndHiddenSubpage() {
        var policy = service.current("labAdmin");
        assertTrue(policy.accessiblePaths().contains("/lab-admin/devices"));
        assertTrue(policy.accessiblePaths().contains("/lab-admin/maintenance-records/new"));
        assertFalse(policy.visiblePaths().contains("/lab-admin/maintenance-records/new"));
    }
    @Test void denyAllKeepsOnlyLockedEntrancesAndSurvivesServiceRecreation() throws Exception {
        service.save("labAdmin", List.of(), List.of());
        var reloaded = new RolePageAccessService(store, new ObjectMapper()).current("labAdmin");
        assertEquals(List.of("/lab-admin"), reloaded.accessiblePaths());
        assertFalse(service.isApiAllowed("GET", "/devices", "labAdmin"));
        assertFalse(service.isApiAllowed("POST", "/devices", "labAdmin"));
    }
    @Test void hiddenMenuDoesNotDenyAuthorizedPageOrApi() {
        service.save("labAdmin", List.of("/lab-admin/devices"), List.of());
        assertFalse(service.current("labAdmin").visiblePaths().contains("/lab-admin/devices"));
        assertTrue(service.isApiAllowed("GET", "/devices/18", "labAdmin"));
        assertFalse(service.isApiAllowed("GET", "/consumables", "labAdmin"));
    }
    @Test void dependentPagesCanReadRequiredCollectionsWithoutOpeningSourcePagesOrWrites() {
        Map<String, List<String>> dependencies = Map.ofEntries(
                Map.entry("/lab-admin/devices", List.of("/labs")),
                Map.entry("/lab-admin/environments", List.of("/labs", "/class-timetables")),
                Map.entry("/lab-admin/reservations", List.of("/labs", "/class-timetables")),
                Map.entry("/lab-admin/maintenance-records", List.of("/labs", "/devices")),
                Map.entry("/lab-admin/maintenance-records/new", List.of("/labs", "/devices")),
                Map.entry("/lab-admin/records", List.of("/labs")),
                Map.entry("/lab-admin/room-control", List.of("/labs")),
                Map.entry("/lab-admin/device-status", List.of("/labs")),
                Map.entry("/lab-admin/statistics", List.of("/labs", "/devices", "/reservations",
                        "/usage-records", "/class-timetables", "/class-timetables/crawler-config",
                        "/repairs", "/course-environment-requests", "/environment-templates")));
        dependencies.forEach((page, endpoints) -> {
            service.save("labAdmin", List.of(), List.of());
            Map<String, Boolean> originalWriteAccess = new HashMap<>();
            for (String endpoint : endpoints) {
                for (String method : List.of("POST", "PUT", "PATCH", "DELETE")) {
                    originalWriteAccess.put(method + " " + endpoint, service.isApiAllowed(method, endpoint, "labAdmin"));
                }
            }
            service.save("labAdmin", List.of(page), List.of());
            assertFalse(service.current("labAdmin").accessiblePaths().contains("/lab-admin/labs"));
            for (String endpoint : endpoints) {
                assertTrue(service.isApiAllowed("GET", endpoint, "labAdmin"), page + " requires " + endpoint);
                for (String method : List.of("POST", "PUT", "PATCH", "DELETE")) {
                    assertEquals(originalWriteAccess.get(method + " " + endpoint),
                            service.isApiAllowed(method, endpoint, "labAdmin"), page + " must not grant additional " + method + " " + endpoint);
                }
            }
            assertFalse(service.isApiAllowed("GET", "/labs/12", "labAdmin"));
            assertFalse(service.isApiAllowed("GET", "/class-timetables/credential", "labAdmin"));
            assertFalse(service.isApiAllowed("POST", "/class-timetables/crawl", "labAdmin"));
        });
        service.save("labAdmin", List.of(), List.of());
        assertFalse(service.isApiAllowed("GET", "/labs", "labAdmin"));
        assertFalse(service.isApiAllowed("GET", "/devices", "labAdmin"));
    }
    @Test void platformStatisticsReadsOnlyItsRequiredCollections() {
        service.save("systemAdmin", List.of("/system-admin/statistics"), List.of());
        for (String endpoint : List.of("/admin/users", "/notices")) {
            assertTrue(service.isApiAllowed("GET", endpoint, "systemAdmin"));
            assertFalse(service.isApiAllowed("POST", endpoint, "systemAdmin"));
            assertFalse(service.isApiAllowed("DELETE", endpoint + "/12", "systemAdmin"));
        }
        assertFalse(service.isApiAllowed("GET", "/admin/users/12", "systemAdmin"));
        service.save("systemAdmin", List.of(), List.of());
        assertFalse(service.isApiAllowed("GET", "/admin/users", "systemAdmin"));
    }
    @Test void filterAllowsDeviceStatusLabLookupButRejectsLabMutation() throws Exception {
        ObjectProvider<RolePageAccessService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(service);
        var filter = new RoleAuthorizationFilter(provider);
        service.save("labAdmin", List.of("/lab-admin/device-status"), List.of());
        for (String method : List.of("GET", "POST")) {
            var request = new MockHttpServletRequest(method, "/labs");
            request.setAttribute(JwtAuthenticationFilter.AUTH_ROLE_ATTRIBUTE, "labAdmin");
            var response = new MockHttpServletResponse();
            var reached = new AtomicBoolean();
            filter.doFilter(request, response, (req,res) -> reached.set(true));
            assertEquals(method.equals("GET"), reached.get());
            assertEquals(method.equals("GET") ? 200 : 403, response.getStatus());
        }
    }
    @Test void rejectsCrossRolePathsInvalidMenusAndMissingListsBeforePersisting() {
        assertThrows(IllegalArgumentException.class, () -> service.save("labAdmin", List.of("/system-admin/users"), List.of()));
        assertThrows(IllegalArgumentException.class, () -> service.save("labAdmin", List.of(), List.of("/lab-admin/devices")));
        assertThrows(IllegalArgumentException.class, () -> service.save("labAdmin", null, List.of()));
        verify(store, never()).replaceModuleRecords(anyString(), anyList());
    }
    @Test void timetableReadPageCannotAuthorizeCrawlerCredentialsOrTrigger() {
        service.save("labAdmin", List.of("/lab-admin/class-timetable-data"), List.of());
        assertTrue(service.isApiAllowed("GET", "/class-timetables", "labAdmin"));
        assertFalse(service.isApiAllowed("GET", "/class-timetables/credential", "labAdmin"));
        assertFalse(service.isApiAllowed("POST", "/class-timetables/crawl", "labAdmin"));
    }
    @Test void protectedAdminEntryRemainsAvailableAndRetiredRolesStayRetired() {
        service.save("systemAdmin", List.of(), List.of());
        assertTrue(service.current("systemAdmin").visiblePaths().contains("/system-admin/role-access"));
        assertFalse(service.isApiAllowed("GET", "/admin/users", "systemAdmin"));
        assertFalse(service.isApiAllowed("POST", "/admin/users/1/reset-password", "systemAdmin"));
        assertFalse(service.isApiAllowed("POST", "/admin/activation-codes/generate", "systemAdmin"));
        assertThrows(IllegalArgumentException.class, () -> service.save("teacher", List.of(), List.of()));
    }
    @Test void controllerRequiresSuperAdminAndUsesAuthenticatedRoleForCurrent() {
        var controller = new RolePageAccessController(service);
        var request = new MockHttpServletRequest();
        request.setAttribute(JwtAuthenticationFilter.AUTH_ROLE_ATTRIBUTE, "labAdmin");
        assertThrows(ResponseStatusException.class, () -> controller.roles(request));
        assertThrows(ResponseStatusException.class, () -> controller.save("labAdmin", new RolePageAccessController.Update(List.of(), List.of()), request));
        assertNotNull(controller.current(request));
        request.setAttribute(JwtAuthenticationFilter.AUTH_ROLE_ATTRIBUTE, "systemAdmin");
        assertNotNull(controller.detail("labAdmin", request));
    }
    @Test void filterBlocksDirectApiAfterRevocationWithoutNewToken() throws Exception {
        ObjectProvider<RolePageAccessService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(service);
        var filter = new RoleAuthorizationFilter(provider);
        service.save("labAdmin", List.of(), List.of());
        var request = new MockHttpServletRequest("GET", "/devices");
        request.setAttribute(JwtAuthenticationFilter.AUTH_ROLE_ATTRIBUTE, "labAdmin");
        var response = new MockHttpServletResponse();
        var reached = new AtomicBoolean();
        filter.doFilter(request, response, (req,res) -> reached.set(true));
        assertEquals(403, response.getStatus());
        assertFalse(reached.get());
    }
    @Test void corruptSavedPolicyFailsClosedInsteadOfRestoringAllPages() {
        saved.put("role-page-access-labAdmin", List.of(Map.of("accessiblePaths", "invalid", "visiblePaths", List.of())));
        assertThrows(IllegalArgumentException.class, () -> service.isApiAllowed("GET", "/devices", "labAdmin"));
    }
    @Test void revokedAiPageCannotKeepUsingAnExistingSocket() throws Exception {
        var ai = mock(org.example.backend.service.AiAssistantService.class);
        var speech = mock(org.example.backend.service.SpeechRecognitionService.class);
        var handler = new org.example.backend.websocket.AiAssistantWebSocketHandler(ai, speech, new ObjectMapper());
        handler.configureRolePageAccess(service);
        service.save("labAdmin", List.of(), List.of());
        var session = mock(org.springframework.web.socket.WebSocketSession.class);
        when(session.getAttributes()).thenReturn(new HashMap<>(Map.of("authRole", "labAdmin")));
        handler.handleMessage(session, new org.springframework.web.socket.TextMessage("{\"type\":\"hello\"}"));
        verify(session).close(org.springframework.web.socket.CloseStatus.POLICY_VIOLATION);
        verifyNoInteractions(ai);
    }
}
