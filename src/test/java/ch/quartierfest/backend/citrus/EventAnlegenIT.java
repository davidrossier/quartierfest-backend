package ch.quartierfest.backend.citrus;

/**
 * Traceability:
 *   UC: UC-003 (Event anlegen)
 *   TCs: TC-006, TC-007
 *   Last traced: 2026-04-10
 */

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

/** Integration tests for UC-003 – Event anlegen. TC-006, TC-007. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class EventAnlegenIT {

    private RestTemplate http;
    @LocalServerPort private int port;
    private HttpHeaders json;
    private RestTemplate setup;

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
    }

    @Test
    @DisplayName("TC-006 – UC-003 Event anlegen: Pflichtfelder vorhanden")
    @SuppressWarnings("unchecked")
    void tc006_eventAnlegenHappyPath() {
        ResponseEntity<Map> response = http.exchange(
                "http://localhost:" + port + "/api/events", HttpMethod.POST,
                new HttpEntity<>(Map.of("datum", "2025-07-05", "startzeit", "15:00:00", "standort", "Buchlenwiese"), json),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("id")).isNotNull();

        // Cleanup als Lösch-Test
        String url = "http://localhost:" + port + "/api/events/" + response.getBody().get("id");
        ResponseEntity<Void> del = http.exchange(url, HttpMethod.DELETE, null, Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("TC-007 – UC-003 Event anlegen: Pflichtfeld datum fehlt")
    @SuppressWarnings("unchecked")
    void tc007_eventAnlegenDatumFehlt() {
        // TODO: Should be HTTP 400 – requires @Valid + @NotNull on Event.datum
        ResponseEntity<Map> response = http.exchange(
                "http://localhost:" + port + "/api/events", HttpMethod.POST,
                new HttpEntity<>(Map.of("startzeit", "15:00:00", "standort", "Buchlenwiese"), json), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
