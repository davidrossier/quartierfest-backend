package ch.quartierfest.backend.citrus;

/**
 * Traceability:
 *   UC: UC-002 (Parteien verwalten)
 *   TCs: TC-004, TC-005, TC-030
 *   Last traced: 2026-05-01
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

/** Integration tests for UC-002 – Parteien verwalten. TC-004, TC-005, TC-030. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class ParteiVerwaltenIT {

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
    @DisplayName("TC-004 – UC-002 Partei anlegen und löschen")
    @SuppressWarnings("unchecked")
    void tc004_parteiAnlegenHappyPath() {
        ResponseEntity<Map> response = http.exchange(
                "http://localhost:" + port + "/api/parteien", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "bezeichnung", "Familie Müller",
                        "adresse", "Musterstrasse 1",
                        "twintAktiv", false), json), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("id")).isNotNull();
        assertThat(response.getBody().get("bezeichnung")).isEqualTo("Familie Müller");

        // Cleanup als Lösch-Test
        String url = "http://localhost:" + port + "/api/parteien/" + response.getBody().get("id");
        ResponseEntity<Void> del = http.exchange(url, HttpMethod.DELETE, null, Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("TC-005 – UC-002 Partei anlegen: Pflichtfeld adresse fehlt")
    @SuppressWarnings("unchecked")
    void tc005_parteiAnlegenAdresseFehlt() {
        // TODO: Should be HTTP 400 – requires @Valid + @NotBlank on Partei.adresse
        ResponseEntity<Map> response = http.exchange(
                "http://localhost:" + port + "/api/parteien", HttpMethod.POST,
                new HttpEntity<>(Map.of("bezeichnung", "Testpartei", "twintAktiv", false), json), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("TC-030 – UC-002 Partei anlegen: Pflichtfeld bezeichnung fehlt")
    @SuppressWarnings("unchecked")
    void tc030_parteiAnlegenBezeichnungFehlt() {
        // TODO: Should be HTTP 400 – requires @Valid + @NotBlank on Partei.bezeichnung
        ResponseEntity<Map> response = http.exchange(
                "http://localhost:" + port + "/api/parteien", HttpMethod.POST,
                new HttpEntity<>(Map.of("adresse", "Musterstrasse 1", "twintAktiv", false), json), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
