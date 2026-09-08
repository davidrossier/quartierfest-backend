package ch.quartierfest.backend.abrechnung;

/**
 * Traceability:
 *   UC: UC-011 (Abrechnung erstellen)
 *   TCs: TC-022, TC-023, TC-044
 *   Last traced: 2026-09-08
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
 * Integration tests for UC-011 – Abrechnung erstellen.
 * Covers TC-022, TC-023.
 *
 * NOTE: UC-011 requires automatic calculation of anteilAllgemeinkosten and totalKonsumation.
 * The API has no calculation endpoint; all amounts must be passed manually.
 * TODO: Implement POST /api/events/{id}/abrechnungen/erstellen to trigger auto-calculation.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("dev")
class AbrechnungErstellenIT {

    private RestTemplate http;
    @LocalServerPort private int port;

    private HttpHeaders json;
    private RestTemplate setup;
    private Long eventId;
    private Long parteiId;
    private Long einladungId;
    private Long teilnahmeId;

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
                Map.of("datum", "2025-07-05", "startzeit", "15:00:00", "standort", "Abrechnung-Test")));
        parteiId = id(setupPost("http://localhost:" + port + "/api/parteien",
                Map.of("bezeichnung", "Abrechnung-Partei", "adresse", "Abrechnungsstrasse 1", "twintAktiv", false)));
        einladungId = id(setupPost("http://localhost:" + port + "/api/einladungen", Map.of(
                "event", Map.of("id", eventId),
                "partei", Map.of("id", parteiId),
                "status", "ANGEMELDET",
                "anzahlPersonen", 2,
                "bestaetigungVersendet", false)));
        teilnahmeId = id(setupPost("http://localhost:" + port + "/api/teilnahmen",
                Map.of("einladung", Map.of("id", einladungId), "anzahlPersonenEffektiv", 2)));
    }

    @AfterEach
    void tearDown() {
        // Abrechnung wird im Test gelöscht; Teilnahme kann nur ohne referenzierte Abrechnung gelöscht werden
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
    @DisplayName("TC-022 – UC-011 Abrechnung erstellen: happy path (manuelle Beträge)")
    @SuppressWarnings("unchecked")
    void tc022_abrechnungErstellenHappyPath() {
        // TODO: anteilAllgemeinkosten (40.00) and totalKonsumation (17.00) are calculated
        //       manually here. UC-011 requires automatic calculation. No such endpoint exists.
        ResponseEntity<Map> response = http.exchange("http://localhost:" + port + "/api/abrechnungen", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "teilnahme", Map.of("id", teilnahmeId),
                        "anteilAllgemeinkosten", 40.00,
                        "totalKonsumation", 17.00,
                        "totalBetrag", 57.00,
                        "zustellungskanal", "EMAIL"), json), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("id")).isNotNull();
        // DB-002: Beträge kommen als numeric(10,2) unverändert zurück
        assertThat(((Number) response.getBody().get("totalBetrag")).doubleValue()).isEqualTo(57.00);

        // Cleanup als Lösch-Test (vor @AfterEach-Teilnahme-Cleanup: Abrechnung referenziert Teilnahme)
        String url = "http://localhost:" + port + "/api/abrechnungen/" + response.getBody().get("id");
        ResponseEntity<Void> del = http.exchange(url, HttpMethod.DELETE, null, Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("TC-023 – UC-011 Abrechnung erstellen: Teilnahme existiert nicht")
    @SuppressWarnings("unchecked")
    void tc023_abrechnungErstellenTeilnahmeFehlt() {
        ResponseEntity<Map> response = http.exchange("http://localhost:" + port + "/api/abrechnungen", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "teilnahme", Map.of("id", 999999),
                        "anteilAllgemeinkosten", 40.00,
                        "totalKonsumation", 17.00,
                        "totalBetrag", 57.00,
                        "zustellungskanal", "EMAIL"), json), Map.class);

        // ERROR-001: FK-Verletzung → 409 mit einheitlichem Fehler-JSON {status, message}
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("status")).isEqualTo(409);
        assertThat((String) response.getBody().get("message")).isNotBlank();
    }

    @Test
    @DisplayName("TC-044 – UC-011 Abrechnung erstellen: zweite Abrechnung zur selben Teilnahme wird abgelehnt (DB-002)")
    @SuppressWarnings("unchecked")
    void tc044_abrechnungDuplikatTeilnahmeAbgelehnt() {
        Map<String, Object> body = Map.of(
                "teilnahme", Map.of("id", teilnahmeId),
                "anteilAllgemeinkosten", 40.00,
                "totalKonsumation", 17.00,
                "totalBetrag", 57.00,
                "zustellungskanal", "EMAIL");
        ResponseEntity<Map> erste = http.exchange("http://localhost:" + port + "/api/abrechnungen", HttpMethod.POST,
                new HttpEntity<>(body, json), Map.class);
        assertThat(erste.getStatusCode()).isEqualTo(HttpStatus.OK);
        Number abrechnungId = (Number) erste.getBody().get("id");

        // Unique-Constraint uk_abrechnung_teilnahme → 409 mit einheitlichem Fehler-JSON (ERROR-001)
        ResponseEntity<Map> duplikat = http.exchange("http://localhost:" + port + "/api/abrechnungen", HttpMethod.POST,
                new HttpEntity<>(body, json), Map.class);
        assertThat(duplikat.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplikat.getBody().get("status")).isEqualTo(409);
        assertThat((String) duplikat.getBody().get("message")).contains("existiert bereits");

        // Cleanup als Lösch-Test (vor @AfterEach-Teilnahme-Cleanup)
        ResponseEntity<Void> del = http.exchange("http://localhost:" + port + "/api/abrechnungen/" + abrechnungId,
                HttpMethod.DELETE, null, Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
