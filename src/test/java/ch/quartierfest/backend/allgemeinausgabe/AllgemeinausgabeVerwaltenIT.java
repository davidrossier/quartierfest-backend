package ch.quartierfest.backend.allgemeinausgabe;

/**
 * Traceability:
 *   UC: UC-007 (Allgemeinausgaben verwalten)
 *   TCs: TC-014, TC-015
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

/** Integration tests for UC-007 – Allgemeinausgaben verwalten. TC-014, TC-015. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("dev")
class AllgemeinausgabeVerwaltenIT {

    private RestTemplate http;
    @LocalServerPort private int port;

    private HttpHeaders json;
    private RestTemplate setup;
    private Long eventId;

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
                Map.of("datum", "2025-07-05", "startzeit", "15:00:00", "standort", "Ausgaben-Test")));
    }

    @AfterEach
    void tearDown() {
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
    @DisplayName("TC-014 – UC-007 Allgemeinausgabe anlegen: happy path")
    @SuppressWarnings("unchecked")
    void tc014_allgemeinausgabeAnlegenHappyPath() {
        ResponseEntity<Map> response = http.exchange("http://localhost:" + port + "/api/allgemeinausgaben", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "event", Map.of("id", eventId),
                        "beschreibung", "Getränkeeinkauf",
                        "herkunft", "Coop",
                        "betrag", "120.00"), json), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("id")).isNotNull();

        // Cleanup als Lösch-Test (vor @AfterEach-Event-Cleanup)
        String url = "http://localhost:" + port + "/api/allgemeinausgaben/" + response.getBody().get("id");
        ResponseEntity<Void> del = http.exchange(url, HttpMethod.DELETE, null, Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("TC-015 – UC-007 Allgemeinausgabe anlegen: Pflichtfeld betrag fehlt")
    @SuppressWarnings("unchecked")
    void tc015_allgemeinausgabeAnlegenBetragFehlt() {
        ResponseEntity<Map> response = http.exchange("http://localhost:" + port + "/api/allgemeinausgaben", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "event", Map.of("id", eventId),
                        "beschreibung", "Ohne Betrag"), json), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
