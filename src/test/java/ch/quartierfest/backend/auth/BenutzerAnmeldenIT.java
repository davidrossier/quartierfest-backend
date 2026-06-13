package ch.quartierfest.backend.auth;

/**
 * Traceability:
 *   UC: UC-014 (Benutzer anmelden)
 *   TCs: TC-038
 *   Last traced: 2026-06-12
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
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Integration tests for UC-014 – Benutzer anmelden (AUTH-002). TC-038. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class BenutzerAnmeldenIT {

    private RestTemplate http;
    @LocalServerPort
    private int port;

    private RestTemplate setup;
    private HttpHeaders json;
    private Long parteiId;
    private Long benutzerId;

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
                Map.of("bezeichnung", "Login-Test-Partei", "adresse", "Loginweg 1", "twintAktiv", false)));
        benutzerId = id(setupPost("http://localhost:" + port + "/api/benutzer",
                Map.of("email", "tc038.login@quartier.ch",
                        "passwort", "login-geheim-12",
                        "rolle", "PARTEI",
                        "partei", Map.of("id", parteiId))));
    }

    @AfterEach
    void tearDown() {
        if (benutzerId != null) tryDelete("http://localhost:" + port + "/api/benutzer/" + benutzerId);
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
    @DisplayName("TC-038 – UC-014 Login Happy Path und falsches Passwort")
    @SuppressWarnings("unchecked")
    void tc038_loginHappyPathUndFalschesPasswort() {
        ResponseEntity<Map> ok = http.exchange(
                "http://localhost:" + port + "/api/auth/login", HttpMethod.POST,
                new HttpEntity<>(Map.of("email", "tc038.login@quartier.ch",
                        "passwort", "login-geheim-12"), json),
                Map.class);

        assertThat(ok.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ok.getBody().get("token")).isNotNull();
        assertThat(ok.getBody().get("token").toString().split("\\.")).hasSize(3);

        ResponseEntity<Map> falsch = http.exchange(
                "http://localhost:" + port + "/api/auth/login", HttpMethod.POST,
                new HttpEntity<>(Map.of("email", "tc038.login@quartier.ch",
                        "passwort", "falsches-passwort"), json),
                Map.class);

        assertThat(falsch.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
