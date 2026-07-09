package ch.quartierfest.backend.abrechnung;

/**
 * Traceability:
 *   UC: UC-012 (Abrechnung zustellen)
 *   TCs: TC-024, TC-025, TC-032
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
 * Integration tests for UC-012 – Abrechnung zustellen.
 * Covers TC-024, TC-025, TC-032.
 *
 * UC-012: zustellungsDatum und Zustellungskanal werden via POST/Upsert gesetzt.
 * POST /api/abrechnungen mit id im Body agiert als Upsert (JPA save() mit vorhandener ID).
 * Kein PATCH-Endpunkt notwendig.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("dev")
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
                Map.of("bezeichnung", "Zustell-Partei-1", "adresse", "Zustellgasse 1", "twintAktiv", false)));
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
                Map.of("bezeichnung", "Zustell-Partei-2", "adresse", "Zustellgasse 2", "twintAktiv", true, "twintMobilenummer", "+41791234567")));
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
        // Abrechnungen werden im Test gelöscht; Teilnahmen können danach bereinigt werden
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

        // Cleanup als Lösch-Test (vor @AfterEach-Teilnahme-Cleanup)
        String url = "http://localhost:" + port + "/api/abrechnungen/" + response.getBody().get("id");
        ResponseEntity<Void> del = http.exchange(url, HttpMethod.DELETE, null, Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.OK);
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

        // Cleanup als Lösch-Test (vor @AfterEach-Teilnahme-Cleanup)
        String url = "http://localhost:" + port + "/api/abrechnungen/" + response.getBody().get("id");
        ResponseEntity<Void> del = http.exchange(url, HttpMethod.DELETE, null, Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("TC-032 – UC-012 Zustellungsdatum nachträglich via POST/Upsert setzen")
    @SuppressWarnings("unchecked")
    void tc032_zustellungsDatumViaUpsert() {
        // Given: Abrechnung ohne zustellungsDatum anlegen
        ResponseEntity<Map> created = http.exchange("http://localhost:" + port + "/api/abrechnungen", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "teilnahme", Map.of("id", teilnahmeId),
                        "anteilAllgemeinkosten", 40.00,
                        "totalKonsumation", 17.00,
                        "totalBetrag", 57.00,
                        "zustellungskanal", "EMAIL"), json), Map.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(created.getBody().get("zustellungsDatum")).isNull();
        long abrechnungId = ((Number) created.getBody().get("id")).longValue();

        // When: zustellungsDatum via POST/Upsert (id im Body) nachträglich setzen
        ResponseEntity<Map> updated = http.exchange("http://localhost:" + port + "/api/abrechnungen", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "id", abrechnungId,
                        "teilnahme", Map.of("id", teilnahmeId),
                        "anteilAllgemeinkosten", 40.00,
                        "totalKonsumation", 17.00,
                        "totalBetrag", 57.00,
                        "zustellungskanal", "EMAIL",
                        "zustellungsDatum", "2025-07-10"), json), Map.class);

        // Then: Zustellungsdatum ist gesetzt
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody()).isNotNull();
        assertThat(updated.getBody().get("zustellungsDatum")).isEqualTo("2025-07-10");
        assertThat(updated.getBody().get("id")).isEqualTo((int) abrechnungId);

        // Cleanup als Lösch-Test
        String url = "http://localhost:" + port + "/api/abrechnungen/" + abrechnungId;
        ResponseEntity<Void> del = http.exchange(url, HttpMethod.DELETE, null, Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
