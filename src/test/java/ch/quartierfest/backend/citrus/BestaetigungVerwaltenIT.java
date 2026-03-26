package ch.quartierfest.backend.citrus;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for UC-006 – Bestätigung erstellen und versenden.
 * Covers TC-013.
 *
 * NOTE: UC-006 requires updating bestaetigungVersendet on an existing ANGEMELDET Einladung.
 * The API has no PATCH endpoint. TC-013 tests only that the flag is persisted at creation time.
 * TODO: Implement PATCH /api/einladungen/{id} to fully cover UC-006.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class BestaetigungVerwaltenIT {

    private RestTemplate http;
    @LocalServerPort private int port;

    private HttpHeaders json;
    private RestTemplate setup;
    private Long eventId;
    private Long parteiId;

    @BeforeEach
    void setUp() {
        setup = new RestTemplate();
        http = new RestTemplate();
        http.setErrorHandler(new org.springframework.web.client.ResponseErrorHandler() {
            public boolean hasError(org.springframework.http.client.ClientHttpResponse r) { return false; }
            public void handleError(org.springframework.http.client.ClientHttpResponse r) { }
        });
        json = new HttpHeaders();
        json.setContentType(MediaType.APPLICATION_JSON);

        eventId = id(setupPost("http://localhost:" + port + "/api/events",
                Map.of("datum", "2025-07-05", "startzeit", "15:00:00", "standort", "Bestaetigung-Test")));
        parteiId = id(setupPost("http://localhost:" + port + "/api/parteien",
                Map.of("adresse", "Bestaetigungsstrasse 1", "twintAktiv", false)));
    }

    @AfterEach
    void tearDown() {
        if (parteiId != null) tryDelete("http://localhost:" + port + "/api/parteien/" + parteiId);
        if (eventId != null) tryDelete("http://localhost:" + port + "/api/events/" + eventId);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> setupPost(String path, Map<String, Object> body) {
        return setup.postForObject(path,
                new HttpEntity<>(body, json), Map.class);
    }
    private long id(Map<String, Object> m) { return ((Number) m.get("id")).longValue(); }
    private void tryDelete(String path) {
        try { setup.delete(path); } catch (Exception ignored) {}
    }

    @Test
    @DisplayName("TC-013 – UC-006 Bestätigung versendet Flag setzen")
    @SuppressWarnings("unchecked")
    void tc013_bestaetigungVersendetFlagSetzen() {
        // TODO: UC-006 main flow requires PATCH to update bestaetigungVersendet=true on an
        //       existing ANGEMELDET Einladung. No PATCH endpoint exists. This test verifies
        //       only that the flag is correctly stored when set at creation time.

        ResponseEntity<Map> response = http.exchange("http://localhost:" + port + "/api/einladungen", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "event", Map.of("id", eventId),
                        "partei", Map.of("id", parteiId),
                        "status", "ANGEMELDET",
                        "anzahlPersonen", 2,
                        "bestaetigungVersendet", true), json), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("bestaetigungVersendet")).isEqualTo(true);
    }
}
