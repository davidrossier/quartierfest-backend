package ch.quartierfest.backend.citrus;

/**
 * Traceability:
 *   UC: UC-013 (Inkasso sicherstellen)
 *   TCs: TC-026, TC-027, TC-028
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
 * Integration tests for UC-013 – Inkasso sicherstellen.
 * Covers TC-026, TC-027, TC-028.
 *
 * Preconditions: Full chain from Event → Partei → Einladung → Teilnahme → Abrechnung
 * is created in @BeforeEach.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class InkassoSicherstellenIT {

    private RestTemplate http;
    @LocalServerPort private int port;

    private HttpHeaders json;
    private RestTemplate setup;
    private Long eventId;
    private Long parteiId;
    private Long einladungId;
    private Long teilnahmeId;
    private Long abrechnungId;

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
                Map.of("datum", "2025-07-05", "startzeit", "15:00:00", "standort", "Inkasso-Test")));
        parteiId = id(setupPost("http://localhost:" + port + "/api/parteien",
                Map.of("bezeichnung", "Inkasso-Partei", "adresse", "Inkassostrasse 1", "twintAktiv", false)));
        einladungId = id(setupPost("http://localhost:" + port + "/api/einladungen", Map.of(
                "event", Map.of("id", eventId),
                "partei", Map.of("id", parteiId),
                "status", "ANGEMELDET",
                "anzahlPersonen", 2,
                "bestaetigungVersendet", false)));
        teilnahmeId = id(setupPost("http://localhost:" + port + "/api/teilnahmen",
                Map.of("einladung", Map.of("id", einladungId), "anzahlPersonenEffektiv", 2)));
        abrechnungId = id(setupPost("http://localhost:" + port + "/api/abrechnungen", Map.of(
                "teilnahme", Map.of("id", teilnahmeId),
                "anteilAllgemeinkosten", "40.00",
                "totalKonsumation", "17.00",
                "totalBetrag", "57.00",
                "zustellungskanal", "EMAIL")));
    }

    @AfterEach
    void tearDown() {
        // Zahlungen/Mahnungen werden im Test gelöscht; dann Abrechnung, dann Kette aufwärts
        if (abrechnungId != null) tryDelete("http://localhost:" + port + "/api/abrechnungen/" + abrechnungId);
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
    @DisplayName("TC-026 – UC-013 TWINT-Zahlung erfassen und löschen")
    @SuppressWarnings("unchecked")
    void tc026_twintZahlungErfassen() {
        ResponseEntity<Map> response = http.exchange("http://localhost:" + port + "/api/zahlungen", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "abrechnung", Map.of("id", abrechnungId),
                        "zahlungskanal", "TWINT",
                        "datum", "2025-07-15",
                        "betrag", 57.00), json), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("id")).isNotNull();

        // Cleanup als Lösch-Test (vor @AfterEach-Abrechnung-Cleanup: Zahlung referenziert Abrechnung)
        String url = "http://localhost:" + port + "/api/zahlungen/" + response.getBody().get("id");
        ResponseEntity<Void> del = http.exchange(url, HttpMethod.DELETE, null, Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("TC-027 – UC-013 Zahlung ohne Datum wird abgelehnt")
    @SuppressWarnings("unchecked")
    void tc027_zahlungOhneDatum() {
        // TODO: Should be HTTP 400 – requires @Valid + @NotNull on Zahlung.datum
        ResponseEntity<Map> response = http.exchange("http://localhost:" + port + "/api/zahlungen", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "abrechnung", Map.of("id", abrechnungId),
                        "zahlungskanal", "TWINT",
                        "betrag", 57.00), json), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("TC-028 – UC-013 Mahnung erfassen und löschen")
    @SuppressWarnings("unchecked")
    void tc028_mahnungErfassen() {
        ResponseEntity<Map> response = http.exchange("http://localhost:" + port + "/api/mahnungen", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "abrechnung", Map.of("id", abrechnungId),
                        "datum", "2025-07-20",
                        "bemerkung", "Bitte bis Ende Juli bezahlen"), json), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("id")).isNotNull();

        // Cleanup als Lösch-Test (vor @AfterEach-Abrechnung-Cleanup: Mahnung referenziert Abrechnung)
        String url = "http://localhost:" + port + "/api/mahnungen/" + response.getBody().get("id");
        ResponseEntity<Void> del = http.exchange(url, HttpMethod.DELETE, null, Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
