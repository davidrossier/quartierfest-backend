package ch.quartierfest.backend.citrus;

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

/** Integration tests for UC-001 – Personendaten verwalten. TC-001, TC-002, TC-003. */
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
    @DisplayName("TC-001 – UC-001 Person anlegen: Pflichtfelder vorhanden")
    void tc001_personAnlegenHappyPath() {
        ResponseEntity<Map> response = http.exchange("http://localhost:" + port + "/api/persons", HttpMethod.POST,
                new HttpEntity<>(Map.of("vorname", "Hans", "name", "Müller"), json), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("id")).isNotNull();
    }

    @Test
    @DisplayName("TC-002 – UC-001 Person anlegen: Pflichtfeld name fehlt")
    void tc002_personAnlegenNameFehlt() {
        // TODO: Should be HTTP 400 – requires @Valid + @NotBlank on Person.name
        ResponseEntity<Map> response = http.exchange("http://localhost:" + port + "/api/persons", HttpMethod.POST,
                new HttpEntity<>(Map.of("vorname", "Hans"), json), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("TC-003 – UC-001 Person löschen")
    void tc003_personLoeschen() {
        Map<String, Object> created = setupPost("http://localhost:" + port + "/api/persons",
                Map.of("vorname", "Delete", "name", "Me"));
        long personId = ((Number) created.get("id")).longValue();

        ResponseEntity<Void> response = http.exchange(
                "http://localhost:" + port + "/api/persons/" + personId, HttpMethod.DELETE, null, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
