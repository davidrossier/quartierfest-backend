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
 * Integration tests for UC-011 – Abrechnung erstellen.
 * Covers TC-022, TC-023.
 *
 * NOTE: UC-011 requires automatic calculation of anteilAllgemeinkosten and totalKonsumation.
 * The API has no calculation endpoint; all amounts must be passed manually.
 * TODO: Implement POST /api/events/{id}/abrechnungen/erstellen to trigger auto-calculation.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
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
                Map.of("adresse", "Abrechnungsstrasse 1", "twintAktiv", false)));
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

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
