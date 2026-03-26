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
 * Integration tests for UC-012 – Abrechnung zustellen.
 * Covers TC-024, TC-025.
 *
 * NOTE: UC-012 requires updating zustellungsDatum and zustellungskanal after an
 * Abrechnung has been created. The API has no PATCH endpoint; zustellungsDatum
 * can only be set at creation time.
 * TODO: Implement PATCH /api/abrechnungen/{id} to set zustellungsDatum separately.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class AbrechnungZustellenIT {

    private RestTemplate http;
    @LocalServerPort private int port;

    private HttpHeaders json;
    private RestTemplate setup;
    private Long eventId;
    private Long parteiId;
    private Long parteiId2;
    private Long einladungId;
    private Long einladungId2;
    private Long teilnahmeId;
    private Long teilnahmeId2;

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
                Map.of("datum", "2025-07-05", "startzeit", "15:00:00", "standort", "Zustell-Test")));

        // Partei 1 – EMAIL delivery
        parteiId = id(setupPost("http://localhost:" + port + "/api/parteien",
                Map.of("adresse", "Zustellgasse 1", "twintAktiv", false)));
        einladungId = id(setupPost("http://localhost:" + port + "/api/einladungen", Map.of(
                "event", Map.of("id", eventId),
                "partei", Map.of("id", parteiId),
                "status", "ANGEMELDET",
                "anzahlPersonen", 2,
                "bestaetigungVersendet", false)));
        teilnahmeId = id(setupPost("http://localhost:" + port + "/api/teilnahmen",
                Map.of("einladung", Map.of("id", einladungId), "anzahlPersonenEffektiv", 2)));

        // Partei 2 – TWINT delivery
        parteiId2 = id(setupPost("http://localhost:" + port + "/api/parteien",
                Map.of("adresse", "Zustellgasse 2", "twintAktiv", true, "twintMobilenummer", "+41791234567")));
        einladungId2 = id(setupPost("http://localhost:" + port + "/api/einladungen", Map.of(
                "event", Map.of("id", eventId),
                "partei", Map.of("id", parteiId2),
                "status", "ANGEMELDET",
                "anzahlPersonen", 3,
                "bestaetigungVersendet", false)));
        teilnahmeId2 = id(setupPost("http://localhost:" + port + "/api/teilnahmen",
                Map.of("einladung", Map.of("id", einladungId2), "anzahlPersonenEffektiv", 3)));
    }

    @AfterEach
    void tearDown() {
        for (Long id : new Long[]{teilnahmeId, teilnahmeId2}) {
            if (id != null) tryDelete("http://localhost:" + port + "/api/teilnahmen/" + id);
        }
        for (Long id : new Long[]{einladungId, einladungId2}) {
            if (id != null) tryDelete("http://localhost:" + port + "/api/einladungen/" + id);
        }
        for (Long id : new Long[]{parteiId, parteiId2}) {
            if (id != null) tryDelete("http://localhost:" + port + "/api/parteien/" + id);
        }
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
    @DisplayName("TC-024 – UC-012 Abrechnung zustellen: Kanal EMAIL mit Zustellungsdatum")
    @SuppressWarnings("unchecked")
    void tc024_abrechnungZustellenKanalEmail() {
        // TODO: UC-012 requires setting zustellungsDatum after creation via PATCH.
        //       No PATCH endpoint exists; zustellungsDatum is set at creation time here.
        ResponseEntity<Map> response = http.exchange("http://localhost:" + port + "/api/abrechnungen", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "teilnahme", Map.of("id", teilnahmeId),
                        "anteilAllgemeinkosten", 40.00,
                        "totalKonsumation", 17.00,
                        "totalBetrag", 57.00,
                        "zustellungskanal", "EMAIL",
                        "zustellungsDatum", "2025-07-10"), json), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("zustellungskanal")).isEqualTo("EMAIL");
    }

    @Test
    @DisplayName("TC-025 – UC-012 Abrechnung zustellen: Kanal TWINT")
    @SuppressWarnings("unchecked")
    void tc025_abrechnungZustellenKanalTwint() {
        ResponseEntity<Map> response = http.exchange("http://localhost:" + port + "/api/abrechnungen", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "teilnahme", Map.of("id", teilnahmeId2),
                        "anteilAllgemeinkosten", 60.00,
                        "totalKonsumation", 12.00,
                        "totalBetrag", 72.00,
                        "zustellungskanal", "TWINT"), json), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("zustellungskanal")).isEqualTo("TWINT");
    }
}
