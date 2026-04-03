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

/** Integration tests for UC-005 – Teilnahmen verwalten. TC-011, TC-012. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class TeilnahmeVerwaltenIT {

    private RestTemplate http;
    @LocalServerPort private int port;

    private HttpHeaders json;
    private RestTemplate setup;
    private Long eventId, parteiId, einladungId;

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
                Map.of("datum", "2025-07-05", "startzeit", "15:00:00", "standort", "Teilnahme-Test")));
        parteiId = id(setupPost("http://localhost:" + port + "/api/parteien",
                Map.of("bezeichnung", "Teilnahme-Partei", "adresse", "Teilnahmeweg 1", "twintAktiv", false)));
        einladungId = id(setupPost("http://localhost:" + port + "/api/einladungen", Map.of(
                "event", Map.of("id", eventId),
                "partei", Map.of("id", parteiId),
                "status", "ANGEMELDET",
                "anzahlPersonen", 2,
                "bestaetigungVersendet", false)));
    }

    @AfterEach
    void tearDown() {
        if (einladungId != null) tryDelete("http://localhost:" + port + "/api/einladungen/" + einladungId);
        if (parteiId != null) tryDelete("http://localhost:" + port + "/api/parteien/" + parteiId);
        if (eventId != null) tryDelete("http://localhost:" + port + "/api/events/" + eventId);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> setupPost(String path, Map<String, Object> body) {
        return setup.postForObject(path, new HttpEntity<>(body, json), Map.class);
    }
    private long id(Map<String, Object> m) { return ((Number) m.get("id")).longValue(); }
    private void tryDelete(String path) {
        try { setup.delete(path); } catch (Exception ignored) {}
    }

    @Test
    @DisplayName("TC-011 – UC-005 Teilnahme erstellen: happy path")
    @SuppressWarnings("unchecked")
    void tc011_teilnahmeErstellenHappyPath() {
        ResponseEntity<Map> response = http.exchange("http://localhost:" + port + "/api/teilnahmen", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "einladung", Map.of("id", einladungId),
                        "anzahlPersonenEffektiv", 2,
                        "hilftAufstellen", true), json), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("id")).isNotNull();

        // Cleanup als Lösch-Test (vor @AfterEach-Einladungs-Cleanup)
        String url = "http://localhost:" + port + "/api/teilnahmen/" + response.getBody().get("id");
        ResponseEntity<Void> del = http.exchange(url, HttpMethod.DELETE, null, Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("TC-012 – UC-005 Teilnahme erstellen: Einladung existiert nicht")
    void tc012_teilnahmeErstellenEinladungFehlt() {
        ResponseEntity<Map> response = http.exchange("http://localhost:" + port + "/api/teilnahmen", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "einladung", Map.of("id", 999999),
                        "anzahlPersonenEffektiv", 2), json), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
