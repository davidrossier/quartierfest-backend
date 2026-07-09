package ch.quartierfest.backend.einladung;

/**
 * Traceability:
 *   UC: UC-006 (Bestätigung erstellen und versenden)
 *   TCs: TC-013
 *   Last traced: 2026-05-01
 */

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for UC-006 – Bestätigung erstellen und versenden.
 * Covers TC-013.
 *
 * UC-006 main flow: bestaetigungVersendet wird via POST/Upsert nachträglich auf true gesetzt.
 * POST /api/einladungen mit id im Body agiert als Upsert (JPA save() mit vorhandener ID).
 * Kein PATCH-Endpunkt notwendig.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("dev")
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
                Map.of("bezeichnung", "Bestaetigung-Partei", "adresse", "Bestaetigungsstrasse 1", "twintAktiv", false)));
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
    @DisplayName("TC-013 – UC-006 Bestätigung versendet via POST/Upsert nachträglich setzen")
    @SuppressWarnings("unchecked")
    void tc013_bestaetigungVersendetViаUpsert() {
        // Given: Einladung mit bestaetigungVersendet=false anlegen
        ResponseEntity<Map> created = http.exchange("http://localhost:" + port + "/api/einladungen", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "event", Map.of("id", eventId),
                        "partei", Map.of("id", parteiId),
                        "status", "ANGEMELDET",
                        "anzahlPersonen", 2,
                        "bestaetigungVersendet", false), json), Map.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(created.getBody().get("bestaetigungVersendet")).isEqualTo(false);
        long einladungId = ((Number) created.getBody().get("id")).longValue();

        // When: bestaetigungVersendet via POST/Upsert (id im Body) auf true setzen
        ResponseEntity<Map> updated = http.exchange("http://localhost:" + port + "/api/einladungen", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "id", einladungId,
                        "event", Map.of("id", eventId),
                        "partei", Map.of("id", parteiId),
                        "status", "ANGEMELDET",
                        "anzahlPersonen", 2,
                        "bestaetigungVersendet", true), json), Map.class);

        // Then: Flag ist auf true gesetzt
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody()).isNotNull();
        assertThat(updated.getBody().get("bestaetigungVersendet")).isEqualTo(true);
        assertThat(updated.getBody().get("id")).isEqualTo((int) einladungId);

        // Cleanup als Lösch-Test (vor @AfterEach-Partei/Event-Cleanup)
        String url = "http://localhost:" + port + "/api/einladungen/" + einladungId;
        ResponseEntity<Void> del = http.exchange(url, HttpMethod.DELETE, null, Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
