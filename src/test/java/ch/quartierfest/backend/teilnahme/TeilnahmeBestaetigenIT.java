package ch.quartierfest.backend.teilnahme;

/**
 * Traceability:
 *   UC: UC-016 (Teilnahme bestätigen)
 *   TCs: TC-036, TC-037
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for UC-016 – Teilnahme bestätigen (AUTH-002). TC-036, TC-037.
 * Nutzt echte JWTs via POST /api/auth/login; die Ownership-Prüfung
 * (Methoden-Security) wirkt auch im Default-Profil.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("dev")
class TeilnahmeBestaetigenIT {

    private RestTemplate http;
    @LocalServerPort
    private int port;

    private RestTemplate setup;
    private HttpHeaders json;

    private Long eventId;
    private Long parteiAId, einladungAId, teilnahmeAId, benutzerAId;
    private Long parteiBId, einladungBId, teilnahmeBId;
    private String tokenA;

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

        String base = "http://localhost:" + port;
        // Zukünftiger Event — GET /api/teilnahmen/meine wählt den frühesten Event mit Datum >= heute
        eventId = id(setupPost(base + "/api/events",
                Map.of("datum", LocalDate.now().plusDays(30).toString(),
                        "startzeit", "15:00:00", "standort", "Bestaetigen-Test")));

        parteiAId = id(setupPost(base + "/api/parteien",
                Map.of("bezeichnung", "Bestaetigen-Partei-A", "adresse", "Weg A", "twintAktiv", false)));
        einladungAId = id(setupPost(base + "/api/einladungen",
                Map.of("event", Map.of("id", eventId), "partei", Map.of("id", parteiAId),
                        "status", "ANGEMELDET", "anzahlPersonen", 3, "bestaetigungVersendet", false)));
        teilnahmeAId = id(setupPost(base + "/api/teilnahmen",
                Map.of("einladung", Map.of("id", einladungAId), "anzahlPersonenEffektiv", 3)));

        parteiBId = id(setupPost(base + "/api/parteien",
                Map.of("bezeichnung", "Bestaetigen-Partei-B", "adresse", "Weg B", "twintAktiv", false)));
        einladungBId = id(setupPost(base + "/api/einladungen",
                Map.of("event", Map.of("id", eventId), "partei", Map.of("id", parteiBId),
                        "status", "ANGEMELDET", "anzahlPersonen", 2, "bestaetigungVersendet", false)));
        teilnahmeBId = id(setupPost(base + "/api/teilnahmen",
                Map.of("einladung", Map.of("id", einladungBId), "anzahlPersonenEffektiv", 2)));

        benutzerAId = id(setupPost(base + "/api/benutzer",
                Map.of("email", "tc036.partei-a@quartier.ch",
                        "passwort", "partei-a-geheim",
                        "rolle", "PARTEI",
                        "partei", Map.of("id", parteiAId))));

        Map<String, Object> login = setupPost(base + "/api/auth/login",
                Map.of("email", "tc036.partei-a@quartier.ch", "passwort", "partei-a-geheim"));
        tokenA = (String) login.get("token");
    }

    @AfterEach
    void tearDown() {
        String base = "http://localhost:" + port;
        if (benutzerAId != null) tryDelete(base + "/api/benutzer/" + benutzerAId);
        if (teilnahmeBId != null) tryDelete(base + "/api/teilnahmen/" + teilnahmeBId);
        if (einladungBId != null) tryDelete(base + "/api/einladungen/" + einladungBId);
        if (parteiBId != null) tryDelete(base + "/api/parteien/" + parteiBId);
        if (teilnahmeAId != null) tryDelete(base + "/api/teilnahmen/" + teilnahmeAId);
        if (einladungAId != null) tryDelete(base + "/api/einladungen/" + einladungAId);
        if (parteiAId != null) tryDelete(base + "/api/parteien/" + parteiAId);
        if (eventId != null) tryDelete(base + "/api/events/" + eventId);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> setupPost(String path, Map<String, Object> body) {
        return setup.postForObject(path, new HttpEntity<>(body, json), Map.class);
    }

    private long id(Map<String, Object> m) { return ((Number) m.get("id")).longValue(); }

    private void tryDelete(String path) {
        try { setup.delete(path); } catch (Exception ignored) {}
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }

    @Test
    @DisplayName("TC-036 – UC-016 PARTEI bestätigt eigene Teilnahme")
    @SuppressWarnings("unchecked")
    void tc036_parteiBestaetigtEigeneTeilnahme() {
        String base = "http://localhost:" + port;

        // Ohne Token ist /meine nicht nutzbar (auch im Default-Profil)
        ResponseEntity<Map> ohneToken = http.exchange(
                base + "/api/teilnahmen/meine", HttpMethod.GET, null, Map.class);
        assertThat(ohneToken.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<Map> meine = http.exchange(
                base + "/api/teilnahmen/meine", HttpMethod.GET,
                new HttpEntity<>(bearer(tokenA)), Map.class);
        assertThat(meine.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) meine.getBody().get("id")).longValue()).isEqualTo(teilnahmeAId);

        ResponseEntity<Map> aktualisiert = http.exchange(
                base + "/api/teilnahmen/" + teilnahmeAId, HttpMethod.PUT,
                new HttpEntity<>(Map.of(
                        "anzahlPersonenEffektiv", 4,
                        "hilftAufstellen", true,
                        "hilftAufraumen", false,
                        "buffetBeitraege", List.of(
                                Map.of("art", "SALAT", "beschreibung", "Rüebli-Salat"))),
                        bearer(tokenA)),
                Map.class);

        assertThat(aktualisiert.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(aktualisiert.getBody().get("anzahlPersonenEffektiv")).isEqualTo(4);
        assertThat(aktualisiert.getBody().get("hilftAufstellen")).isEqualTo(true);
        assertThat((List<Map<String, Object>>) aktualisiert.getBody().get("buffetBeitraege")).hasSize(1);
        // Whitelist-DTO: einladung bleibt unangetastet
        assertThat(((Map<String, Object>) aktualisiert.getBody().get("einladung")).get("id"))
                .isEqualTo(einladungAId.intValue());
    }

    @Test
    @DisplayName("TC-037 – UC-016 PARTEI auf fremde Teilnahme verweigert")
    @SuppressWarnings("unchecked")
    void tc037_parteiFremdeTeilnahmeVerweigert() {
        String base = "http://localhost:" + port;

        ResponseEntity<Map> response = http.exchange(
                base + "/api/teilnahmen/" + teilnahmeBId, HttpMethod.PUT,
                new HttpEntity<>(Map.of("anzahlPersonenEffektiv", 99), bearer(tokenA)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // Fremde Teilnahme unverändert
        ResponseEntity<List> alle = http.exchange(
                base + "/api/teilnahmen", HttpMethod.GET, null, List.class);
        Map<String, Object> teilnahmeB = ((List<Map<String, Object>>) alle.getBody()).stream()
                .filter(t -> teilnahmeBId.equals(((Number) t.get("id")).longValue()))
                .findFirst().orElseThrow();
        assertThat(teilnahmeB.get("anzahlPersonenEffektiv")).isEqualTo(2);
    }
}
