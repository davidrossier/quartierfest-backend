package ch.quartierfest.backend.auth;

/**
 * Traceability:
 *   UC: UC-014 (Benutzer anmelden) / AUTH-002 Autorisierungsmatrix
 *   TCs: TC-040
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the AUTH-002 authorization matrix (TC-040).
 * Einziger IT mit aktiver Security: Profil security-test läuft auf der gesicherten
 * Default-Chain (SEC-001: fail-closed; Konfiguration in
 * src/test/resources/application-security-test.properties).
 * Alle Fixtures laufen über die API mit dem Token des Bootstrap-ORGANISATORs.
 * RANDOM_PORT statt DEFINED_PORT: der security-test-Context koexistiert mit dem
 * gecachten dev-Context der übrigen ITs, die Port 8080 belegen.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("security-test")
class SecurityMatrixIT {

    // Dev-Bootstrap-Admin aus application.properties — die ITs teilen sich die DB,
    // der ORGANISATOR existiert daher unabhängig davon, welcher Context zuerst startet
    private static final String ADMIN_EMAIL = "admin@quartierfest.local";
    private static final String ADMIN_PASSWORT = "quartierfest-admin";

    private RestTemplate http;
    @LocalServerPort
    private int port;

    private HttpHeaders json;
    private String base;
    private String orgaToken;
    private Long parteiId;
    private Long benutzerId;

    @BeforeEach
    void setUp() {
        http = new RestTemplate();
        http.setErrorHandler(new org.springframework.web.client.ResponseErrorHandler() {
            public boolean hasError(org.springframework.http.client.ClientHttpResponse r) { return false; }
            public void handleError(org.springframework.http.client.ClientHttpResponse r) { }
        });
        json = new HttpHeaders();
        json.setContentType(MediaType.APPLICATION_JSON);
        base = "http://localhost:" + port;
    }

    @AfterEach
    void tearDown() {
        if (orgaToken != null) {
            if (benutzerId != null) tryDeleteMitToken(base + "/api/benutzer/" + benutzerId, orgaToken);
            if (parteiId != null) tryDeleteMitToken(base + "/api/parteien/" + parteiId, orgaToken);
        }
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }

    private void tryDeleteMitToken(String path, String token) {
        try {
            http.exchange(path, HttpMethod.DELETE, new HttpEntity<>(bearer(token)), Void.class);
        } catch (Exception ignored) {}
    }

    @SuppressWarnings("unchecked")
    private long id(Map<String, Object> m) { return ((Number) m.get("id")).longValue(); }

    @Test
    @DisplayName("TC-040 – AUTH-002 Autorisierungsmatrix (security-test)")
    @SuppressWarnings("unchecked")
    void tc040_autorisierungsmatrix() {
        // 1. Ohne Token → 401
        ResponseEntity<String> ohneToken = http.exchange(
                base + "/api/persons", HttpMethod.GET, null, String.class);
        assertThat(ohneToken.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // 2. Login ist ohne Token erreichbar (Bootstrap-ORGANISATOR)
        ResponseEntity<Map> login = http.exchange(
                base + "/api/auth/login", HttpMethod.POST,
                new HttpEntity<>(Map.of("email", ADMIN_EMAIL, "passwort", ADMIN_PASSWORT), json),
                Map.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        orgaToken = (String) login.getBody().get("token");
        assertThat(orgaToken).isNotNull();

        // 5. (vorgezogen) ORGANISATOR hat vollen Zugriff
        ResponseEntity<String> orgaPersons = http.exchange(
                base + "/api/persons", HttpMethod.GET, new HttpEntity<>(bearer(orgaToken)), String.class);
        assertThat(orgaPersons.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Fixtures via ORGANISATOR-Token: Partei + PARTEI-Benutzer
        ResponseEntity<Map> partei = http.exchange(
                base + "/api/parteien", HttpMethod.POST,
                new HttpEntity<>(Map.of("bezeichnung", "Matrix-Partei", "adresse", "Matrixweg 1",
                        "twintAktiv", false), bearer(orgaToken)),
                Map.class);
        assertThat(partei.getStatusCode()).isEqualTo(HttpStatus.OK);
        parteiId = id(partei.getBody());

        ResponseEntity<Map> benutzer = http.exchange(
                base + "/api/benutzer", HttpMethod.POST,
                new HttpEntity<>(Map.of("email", "tc040.partei@quartier.ch",
                        "passwort", "matrix-geheim-1",
                        "rolle", "PARTEI",
                        "partei", Map.of("id", parteiId)), bearer(orgaToken)),
                Map.class);
        assertThat(benutzer.getStatusCode()).isEqualTo(HttpStatus.OK);
        benutzerId = id(benutzer.getBody());

        ResponseEntity<Map> parteiLogin = http.exchange(
                base + "/api/auth/login", HttpMethod.POST,
                new HttpEntity<>(Map.of("email", "tc040.partei@quartier.ch",
                        "passwort", "matrix-geheim-1"), json),
                Map.class);
        assertThat(parteiLogin.getStatusCode()).isEqualTo(HttpStatus.OK);
        String parteiToken = (String) parteiLogin.getBody().get("token");

        // 3. PARTEI auf fremde Ressource → 403
        ResponseEntity<String> parteiPersons = http.exchange(
                base + "/api/persons", HttpMethod.GET, new HttpEntity<>(bearer(parteiToken)), String.class);
        assertThat(parteiPersons.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // 4. PARTEI auf /api/teilnahmen/meine → erlaubt (404, da keine Teilnahme existiert)
        ResponseEntity<String> meine = http.exchange(
                base + "/api/teilnahmen/meine", HttpMethod.GET, new HttpEntity<>(bearer(parteiToken)), String.class);
        assertThat(meine.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.NOT_FOUND);
        assertThat(meine.getStatusCode()).isNotIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }
}
