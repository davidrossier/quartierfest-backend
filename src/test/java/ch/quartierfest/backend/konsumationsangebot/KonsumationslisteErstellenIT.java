package ch.quartierfest.backend.konsumationsangebot;

/**
 * Traceability:
 *   UC: UC-009 (Konsumationsliste erstellen)
 *   TCs: TC-018, TC-019
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
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for UC-009 – Konsumationsliste erstellen.
 * Covers TC-018, TC-019.
 *
 * NOTE: No dedicated "Konsumationsliste" endpoint exists.
 * These tests verify that the underlying data (Konsumationsangebote + Teilnahmen)
 * can be retrieved successfully, which is the data basis for list generation.
 * TODO: Implement GET /api/events/{id}/konsumationsliste for full UC-009 coverage.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class KonsumationslisteErstellenIT {

    private RestTemplate http;
    @LocalServerPort private int port;

    private HttpHeaders json;
    private RestTemplate setup;
    private Long eventId;
    private Long parteiId;
    private Long einladungId;
    private Long angebotId;

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
                Map.of("datum", "2025-07-05", "startzeit", "15:00:00", "standort", "Liste-Test")));
        parteiId = id(setupPost("http://localhost:" + port + "/api/parteien",
                Map.of("bezeichnung", "Liste-Partei", "adresse", "Listenweg 1", "twintAktiv", false)));
        einladungId = id(setupPost("http://localhost:" + port + "/api/einladungen", Map.of(
                "event", Map.of("id", eventId),
                "partei", Map.of("id", parteiId),
                "status", "ANGEMELDET",
                "anzahlPersonen", 2,
                "bestaetigungVersendet", false)));
        angebotId = id(setupPost("http://localhost:" + port + "/api/konsumationsangebote",
                Map.of("event", Map.of("id", eventId), "bezeichnung", "Bier 5dl", "preis", "3.00")));
    }

    @AfterEach
    void tearDown() {
        if (angebotId != null) tryDelete("http://localhost:" + port + "/api/konsumationsangebote/" + angebotId);
        if (einladungId != null) tryDelete("http://localhost:" + port + "/api/einladungen/" + einladungId);
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
    @DisplayName("TC-018 – UC-009 Konsumationsangebote für Event abrufbar (X-Achse)")
    void tc018_konsumationsangeboteAbrufbar() {
        // TODO: No event-scoped filter on GET /api/konsumationsangebote.
        //       Returns all offers across all events.
        ResponseEntity<String> response = http.exchange(
                "http://localhost:" + port + "/api/konsumationsangebote", HttpMethod.GET, null, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotEmpty();
        // Kein Cleanup notwendig – kein Datensatz im Test angelegt
    }

    @Test
    @DisplayName("TC-019 – UC-009 Teilnahmen für Event abrufbar (Y-Achse)")
    @SuppressWarnings("unchecked")
    void tc019_teilnahmenAbrufbar() {
        // TODO: No event-scoped filter on GET /api/teilnahmen.
        //       Returns all participations across all events.

        // Given: create a Teilnahme for the ANGEMELDET Einladung from setup
        Map<String, Object> teilnahme = setupPost("http://localhost:" + port + "/api/teilnahmen",
                Map.of("einladung", Map.of("id", einladungId), "anzahlPersonenEffektiv", 2));

        ResponseEntity<String> response = http.exchange(
                "http://localhost:" + port + "/api/teilnahmen", HttpMethod.GET, null, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotEmpty();

        // Cleanup als Lösch-Test (vor @AfterEach-Einladungs-Cleanup)
        String url = "http://localhost:" + port + "/api/teilnahmen/" + teilnahme.get("id");
        ResponseEntity<Void> del = http.exchange(url, HttpMethod.DELETE, null, Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
