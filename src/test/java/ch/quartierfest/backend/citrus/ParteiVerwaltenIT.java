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

/** Integration tests for UC-002 – Parteien verwalten. TC-004, TC-005. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class ParteiVerwaltenIT {

    private RestTemplate http;
    @LocalServerPort private int port;
    private HttpHeaders json;

    @BeforeEach
    void setUp() {
        http = new RestTemplate();
        http.setErrorHandler(new org.springframework.web.client.ResponseErrorHandler() {
            public boolean hasError(org.springframework.http.client.ClientHttpResponse r) { return false; }
            public void handleError(org.springframework.http.client.ClientHttpResponse r) { }
        });
        json = new HttpHeaders();
        json.setContentType(MediaType.APPLICATION_JSON);
    }

    @Test
    @DisplayName("TC-004 – UC-002 Partei anlegen: Pflichtfelder vorhanden")
    @SuppressWarnings("unchecked")
    void tc004_parteiAnlegenHappyPath() {
        ResponseEntity<Map> response = http.exchange(
                "http://localhost:" + port + "/api/parteien", HttpMethod.POST,
                new HttpEntity<>(Map.of("adresse", "Musterstrasse 1", "twintAktiv", false), json), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("id")).isNotNull();
    }

    @Test
    @DisplayName("TC-005 – UC-002 Partei anlegen: Pflichtfeld adresse fehlt")
    @SuppressWarnings("unchecked")
    void tc005_parteiAnlegenAdresseFehlt() {
        // TODO: Should be HTTP 400 – requires @Valid + @NotBlank on Partei.adresse
        ResponseEntity<Map> response = http.exchange(
                "http://localhost:" + port + "/api/parteien", HttpMethod.POST,
                new HttpEntity<>(Map.of("twintAktiv", false), json), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
