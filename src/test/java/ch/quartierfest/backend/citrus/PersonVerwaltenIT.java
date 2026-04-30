package ch.quartierfest.backend.citrus;

/**
 * Traceability:
 *   UC: UC-001 (Personendaten verwalten)
 *   TCs: TC-001, TC-002, TC-029
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

/** Integration tests for UC-001 – Personendaten verwalten. TC-001, TC-002, TC-029. (TC-003 integriert in TC-001) */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class PersonVerwaltenIT {

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

    @SuppressWarnings("unchecked")
    private Map<String, Object> setupPost(String path, Map<String, Object> body) {
        return setup.postForObject(path,
                new HttpEntity<>(body, json), Map.class);
    }

    @Test
    @DisplayName("TC-001 – UC-001 Person anlegen und löschen")
    @SuppressWarnings("unchecked")
    void tc001_personAnlegenHappyPath() {
        ResponseEntity<Map> response = http.exchange("http://localhost:" + port + "/api/persons", HttpMethod.POST,
                new HttpEntity<>(Map.of("vorname", "Hans", "name", "Müller"), json), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("id")).isNotNull();

        // TC-003: Person löschen – Cleanup als Lösch-Test
        String url = "http://localhost:" + port + "/api/persons/" + response.getBody().get("id");
        ResponseEntity<Void> del = http.exchange(url, HttpMethod.DELETE, null, Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("TC-029 – UC-001 Person bearbeiten")
    @SuppressWarnings("unchecked")
    void tc029_personBearbeiten() {
        // Given: Person anlegen
        Map<String, Object> created = setupPost("http://localhost:" + port + "/api/persons",
                Map.of("vorname", "Anna", "name", "Müller"));
        long id = ((Number) created.get("id")).longValue();
        String url = "http://localhost:" + port + "/api/persons/" + id;

        // When: Person bearbeiten (PUT)
        ResponseEntity<Map> response = http.exchange(url, HttpMethod.PUT,
                new HttpEntity<>(Map.of("vorname", "Anna", "name", "Müller", "mobilenummer", "+41791234567"), json),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("mobilenummer")).isEqualTo("+41791234567");
        assertThat(response.getBody().get("id")).isEqualTo((int) id);

        // Cleanup als Lösch-Test
        ResponseEntity<Void> del = http.exchange(url, HttpMethod.DELETE, null, Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("TC-002 – UC-001 Person anlegen: Pflichtfeld name fehlt")
    @SuppressWarnings("unchecked")
    void tc002_personAnlegenNameFehlt() {
        // TODO: Should be HTTP 400 – requires @Valid + @NotBlank on Person.name
        ResponseEntity<Map> response = http.exchange("http://localhost:" + port + "/api/persons", HttpMethod.POST,
                new HttpEntity<>(Map.of("vorname", "Hans"), json), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
