package ch.quartierfest.backend.auth;

/**
 * Traceability:
 *   UC: UC-014 (Benutzer anmelden)
 *   TCs: TC-038, TC-045
 *   Last traced: 2026-09-17
 */

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

/** Integration tests for UC-014 – Benutzer anmelden (AUTH-002, SEC-002). TC-038, TC-045. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("dev")
class BenutzerAnmeldenIT {

    private RestTemplate http;
    @LocalServerPort
    private int port;
    @Autowired
    private LoginDrosselung loginDrosselung;

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
                        "parteiId", parteiId)));
    }

    @AfterEach
    void tearDown() {
        // SEC-002: Zähler leeren, damit andere ITs im geteilten Context (alle von 127.0.0.1) nicht gesperrt werden
        loginDrosselung.zuruecksetzen();
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

    @Test
    @DisplayName("TC-045 – SEC-002 Login nach 5 Fehlversuchen für 15 Minuten gesperrt (429)")
    @SuppressWarnings("unchecked")
    void tc045_loginNachFuenfFehlversuchenGesperrt() {
        String url = "http://localhost:" + port + "/api/auth/login";
        HttpEntity<Map<String, String>> falsch = new HttpEntity<>(
                Map.of("email", "tc038.login@quartier.ch", "passwort", "falsches-passwort"), json);
        HttpEntity<Map<String, String>> korrekt = new HttpEntity<>(
                Map.of("email", "tc038.login@quartier.ch", "passwort", "login-geheim-12"), json);

        for (int i = 1; i <= 5; i++) {
            ResponseEntity<Map> r = http.exchange(url, HttpMethod.POST, falsch, Map.class);
            assertThat(r.getStatusCode()).as("Fehlversuch %d", i).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        ResponseEntity<Map> gesperrt = http.exchange(url, HttpMethod.POST, falsch, Map.class);
        assertThat(gesperrt.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(gesperrt.getBody().get("message").toString()).contains("Zu viele Fehlversuche");

        // Auch das korrekte Passwort wird während der Sperre abgewiesen
        ResponseEntity<Map> korrektGesperrt = http.exchange(url, HttpMethod.POST, korrekt, Map.class);
        assertThat(korrektGesperrt.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(korrektGesperrt.getBody()).doesNotContainKey("token");

        // Andere Benutzer von derselben IP bleiben anmeldbar (IP-Limit 20 > E-Mail-Limit 5)
        ResponseEntity<Map> admin = http.exchange(url, HttpMethod.POST, new HttpEntity<>(
                Map.of("email", "admin@quartierfest.local", "passwort", "quartierfest-admin"), json), Map.class);
        assertThat(admin.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
