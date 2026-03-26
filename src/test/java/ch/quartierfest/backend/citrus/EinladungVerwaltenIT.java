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

/** Integration tests for UC-004 – Einladung erstellen und verwalten. TC-008, TC-009, TC-010. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class EinladungVerwaltenIT {

    private RestTemplate http;
    @LocalServerPort private int port;

    private HttpHeaders json;
    private RestTemplate setup;
    private Long eventId, parteiId;

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
                Map.of("datum", "2025-07-05", "startzeit", "15:00:00", "standort", "Einladung-Test")));
        parteiId = id(setupPost("http://localhost:" + port + "/api/parteien",
                Map.of("adresse", "Testgasse 1", "twintAktiv", false)));
    }

    @AfterEach
    void tearDown() {
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
    @DisplayName("TC-008 – UC-004 Einladung erstellen (Status OFFEN)")
    void tc008_einladungErstellenStatusOffen() {
        ResponseEntity<Map> response = http.exchange("http://localhost:" + port + "/api/einladungen", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "event", Map.of("id", eventId),
                        "partei", Map.of("id", parteiId),
                        "status", "OFFEN",
                        "bestaetigungVersendet", false), json), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("id")).isNotNull();
    }

    @Test
    @DisplayName("TC-009 – UC-004 Rückmeldung ANGEMELDET erfassen")
    void tc009_rueckmeldungAngemeldetErfassen() {
        ResponseEntity<Map> response = http.exchange("http://localhost:" + port + "/api/einladungen", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "event", Map.of("id", eventId),
                        "partei", Map.of("id", parteiId),
                        "status", "ANGEMELDET",
                        "anzahlPersonen", 3,
                        "hilftAufstellen", true,
                        "buffetBeitrag", "SALAT",
                        "bestaetigungVersendet", false), json), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("ANGEMELDET");
    }

    @Test
    @DisplayName("TC-010 – UC-004 Rückmeldung ABGEMELDET erfassen (A1)")
    void tc010_rueckmeldungAbgemeldetErfassen() {
        // TODO: UC-004 E1 (Duplikat-Prüfung) fehlt – kein Unique-Constraint auf (event, partei)
        ResponseEntity<Map> response = http.exchange("http://localhost:" + port + "/api/einladungen", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "event", Map.of("id", eventId),
                        "partei", Map.of("id", parteiId),
                        "status", "ABGEMELDET",
                        "bestaetigungVersendet", false), json), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("ABGEMELDET");
    }
}
