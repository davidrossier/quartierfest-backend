package ch.quartierfest.backend.benutzer;

/**
 * Traceability:
 *   UC: UC-015 (Benutzer verwalten)
 *   TCs: TC-034, TC-035, TC-039
 *   Last traced: 2026-06-12
 */

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Integration tests for UC-015 – Benutzer verwalten (AUTH-002). TC-034, TC-035, TC-039. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("dev")
class BenutzerVerwaltenIT {

    private RestTemplate http;
    @LocalServerPort
    private int port;

    private RestTemplate setup;
    private HttpHeaders json;
    private Long parteiId;

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

        parteiId = id(setupPost("http://localhost:" + port + "/api/parteien",
                Map.of("bezeichnung", "Benutzer-Test-Partei", "adresse", "Benutzerweg 1", "twintAktiv", false)));
    }

    @AfterEach
    void tearDown() {
        if (parteiId != null) tryDelete("http://localhost:" + port + "/api/parteien/" + parteiId);
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
    @DisplayName("TC-034 – UC-015 Benutzer anlegen und löschen")
    @SuppressWarnings("unchecked")
    void tc034_benutzerAnlegen() {
        ResponseEntity<Map> response = http.exchange(
                "http://localhost:" + port + "/api/benutzer", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "email", "tc034.mueller@quartier.ch",
                        "passwort", "geheim-1234",
                        "rolle", "PARTEI",
                        "partei", Map.of("id", parteiId)), json),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("id")).isNotNull();
        assertThat(response.getBody().get("email")).isEqualTo("tc034.mueller@quartier.ch");
        // Passwort darf in keiner Form in der Antwort erscheinen
        assertThat(response.getBody()).doesNotContainKeys("passwort", "passwortHash");

        ResponseEntity<List> liste = http.exchange(
                "http://localhost:" + port + "/api/benutzer", HttpMethod.GET, null, List.class);
        assertThat(liste.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<Map<String, Object>>) liste.getBody())
                .anyMatch(b -> "tc034.mueller@quartier.ch".equals(b.get("email")));

        // Cleanup als Lösch-Test
        String url = "http://localhost:" + port + "/api/benutzer/" + response.getBody().get("id");
        ResponseEntity<Void> del = http.exchange(url, HttpMethod.DELETE, null, Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("TC-035 – UC-015 Benutzer mit doppelter E-Mail wird abgelehnt")
    @SuppressWarnings("unchecked")
    void tc035_benutzerDuplikatEmail() {
        Map<String, Object> erster = setupPost("http://localhost:" + port + "/api/benutzer",
                Map.of("email", "tc035.doppelt@quartier.ch",
                        "passwort", "geheim-1234",
                        "rolle", "PARTEI",
                        "partei", Map.of("id", parteiId)));
        try {
            ResponseEntity<Map> response = http.exchange(
                    "http://localhost:" + port + "/api/benutzer", HttpMethod.POST,
                    new HttpEntity<>(Map.of(
                            "email", "tc035.doppelt@quartier.ch",
                            "passwort", "anderes-passwort-99",
                            "rolle", "PARTEI",
                            "partei", Map.of("id", parteiId)), json),
                    Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        } finally {
            tryDelete("http://localhost:" + port + "/api/benutzer/" + id(erster));
        }
    }

    @Test
    @DisplayName("TC-039 – UC-015 Letzter ORGANISATOR nicht löschbar")
    @SuppressWarnings("unchecked")
    void tc039_letzterOrganisatorNichtLoeschbar() {
        ResponseEntity<List> liste = http.exchange(
                "http://localhost:" + port + "/api/benutzer", HttpMethod.GET, null, List.class);
        assertThat(liste.getStatusCode()).isEqualTo(HttpStatus.OK);

        List<Map<String, Object>> organisatoren = ((List<Map<String, Object>>) liste.getBody()).stream()
                .filter(b -> "ORGANISATOR".equals(b.get("rolle")))
                .toList();
        assertThat(organisatoren).as("Bootstrap-ORGANISATOR muss existieren").isNotEmpty();
        Assumptions.assumeTrue(organisatoren.size() == 1,
                "Test setzt genau einen ORGANISATOR voraus (Bootstrap-Admin)");

        String url = "http://localhost:" + port + "/api/benutzer/" + organisatoren.get(0).get("id");
        ResponseEntity<Void> del = http.exchange(url, HttpMethod.DELETE, null, Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        ResponseEntity<List> danach = http.exchange(
                "http://localhost:" + port + "/api/benutzer", HttpMethod.GET, null, List.class);
        assertThat((List<Map<String, Object>>) danach.getBody())
                .anyMatch(b -> "ORGANISATOR".equals(b.get("rolle")));
    }
}
