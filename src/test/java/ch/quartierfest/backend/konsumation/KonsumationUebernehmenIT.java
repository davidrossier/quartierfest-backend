package ch.quartierfest.backend.konsumation;

/**
 * Traceability:
 *   UC: UC-010 (Konsumation übernehmen)
 *   TCs: TC-020, TC-021, TC-051
 *   Last traced: 2026-09-25
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
 * Integration tests for UC-010 – Konsumation übernehmen.
 * Covers TC-020, TC-021, TC-051.
 *
 * Preconditions: Event, Partei, Einladung, Teilnahme and Konsumationsangebot
 * are created in @BeforeEach.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("dev")
class KonsumationUebernehmenIT {

    private RestTemplate http;
    @LocalServerPort private int port;

    private HttpHeaders json;
    private RestTemplate setup;
    private Long eventId;
    private Long parteiId;
    private Long einladungId;
    private Long teilnahmeId;
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
                Map.of("datum", "2025-07-05", "startzeit", "15:00:00", "standort", "Konsumation-Test")));
        parteiId = id(setupPost("http://localhost:" + port + "/api/parteien",
                Map.of("bezeichnung", "Konsumation-Partei", "adresse", "Konsumationsweg 1", "twintAktiv", false)));
        einladungId = id(setupPost("http://localhost:" + port + "/api/einladungen", Map.of(
                "eventId", eventId,
                "parteiId", parteiId,
                "status", "ANGEMELDET",
                "anzahlPersonen", 2,
                "bestaetigungVersendet", false)));
        teilnahmeId = id(setupPost("http://localhost:" + port + "/api/teilnahmen",
                Map.of("einladungId", einladungId, "anzahlPersonenEffektiv", 2)));
        angebotId = id(setupPost("http://localhost:" + port + "/api/konsumationsangebote",
                Map.of("eventId", eventId, "bezeichnung", "Bier 5dl", "preis", "3.00")));
    }

    @AfterEach
    void tearDown() {
        if (angebotId != null) tryDelete("http://localhost:" + port + "/api/konsumationsangebote/" + angebotId);
        if (teilnahmeId != null) tryDelete("http://localhost:" + port + "/api/teilnahmen/" + teilnahmeId);
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
    @DisplayName("TC-020 – UC-010 Konsumation erfassen: happy path")
    @SuppressWarnings("unchecked")
    void tc020_konsumationErfassenHappyPath() {
        ResponseEntity<Map> response = http.exchange("http://localhost:" + port + "/api/konsumationen", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "teilnahmeId", teilnahmeId,
                        "konsumationsangebotId", angebotId,
                        "anzahl", 3), json), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("id")).isNotNull();

        // Cleanup als Lösch-Test (vor @AfterEach-Teardown)
        String url = "http://localhost:" + port + "/api/konsumationen/" + response.getBody().get("id");
        ResponseEntity<Void> del = http.exchange(url, HttpMethod.DELETE, null, Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("TC-021 – UC-010 Konsumation erfassen: Pflichtfeld anzahl fehlt")
    @SuppressWarnings("unchecked")
    void tc021_konsumationErfassenAnzahlFehlt() {
        ResponseEntity<Map> response = http.exchange("http://localhost:" + port + "/api/konsumationen", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "teilnahmeId", teilnahmeId,
                        "konsumationsangebotId", angebotId), json), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("TC-051 – UC-010 Anzahl einer Konsumation per PUT ändern (REST-003, erweitert)")
    @SuppressWarnings("unchecked")
    void tc051_konsumationAnzahlAendern() {
        ResponseEntity<Map> angelegt = http.exchange("http://localhost:" + port + "/api/konsumationen",
                HttpMethod.POST, new HttpEntity<>(Map.of("teilnahmeId", teilnahmeId,
                        "konsumationsangebotId", angebotId, "anzahl", 3), json), Map.class);
        assertThat(angelegt.getStatusCode()).isEqualTo(HttpStatus.OK);
        String url = "http://localhost:" + port + "/api/konsumationen/" + angelegt.getBody().get("id");

        ResponseEntity<Map> response = http.exchange(url, HttpMethod.PUT,
                new HttpEntity<>(Map.of("anzahl", 5), json), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("anzahl")).isEqualTo(5);
        assertThat(((Map<String, Object>) response.getBody().get("teilnahme")).get("id"))
                .isEqualTo(teilnahmeId.intValue());

        // Teilnahme und Angebot einer Matrix-Zelle sind fix → unbekanntes Feld → 400
        ResponseEntity<Map> mitAngebot = http.exchange(url, HttpMethod.PUT,
                new HttpEntity<>(Map.of("anzahl", 1, "konsumationsangebotId", angebotId), json), Map.class);
        assertThat(mitAngebot.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ResponseEntity<Void> del = http.exchange(url, HttpMethod.DELETE, null, Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
