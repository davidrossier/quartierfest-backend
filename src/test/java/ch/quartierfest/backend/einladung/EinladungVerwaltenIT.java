package ch.quartierfest.backend.einladung;

/**
 * Traceability:
 *   UC: UC-004 (Einladung erstellen und verwalten)
 *   TCs: TC-008, TC-009, TC-010, TC-042, TC-048
 *   Last traced: 2026-09-25
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

/** Integration tests for UC-004 – Einladung erstellen und verwalten. TC-008, TC-009, TC-010, TC-042, TC-048. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("dev")
class EinladungVerwaltenIT {

    private RestTemplate http;
    @LocalServerPort private int port;

    private HttpHeaders json;
    private RestTemplate setup;
    private Long eventId, parteiId;

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

        eventId = id(setupPost("http://localhost:" + port + "/api/events",
                Map.of("datum", "2025-07-05", "startzeit", "15:00:00", "standort", "Einladung-Test")));
        parteiId = id(setupPost("http://localhost:" + port + "/api/parteien",
                Map.of("bezeichnung", "Einladung-Partei", "adresse", "Testgasse 1", "twintAktiv", false)));
    }

    @AfterEach
    void tearDown() {
        if (parteiId != null) tryDelete("http://localhost:" + port + "/api/parteien/" + parteiId);
        if (eventId != null) tryDelete("http://localhost:" + port + "/api/events/" + eventId);
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
    @DisplayName("TC-008 – UC-004 Einladung erstellen (Status OFFEN)")
    @SuppressWarnings("unchecked")
    void tc008_einladungErstellenStatusOffen() {
        ResponseEntity<Map> response = http.exchange("http://localhost:" + port + "/api/einladungen", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "eventId", eventId,
                        "parteiId", parteiId,
                        "status", "OFFEN",
                        "bestaetigungVersendet", false), json), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("id")).isNotNull();

        // Cleanup als Lösch-Test
        String url = "http://localhost:" + port + "/api/einladungen/" + response.getBody().get("id");
        ResponseEntity<Void> del = http.exchange(url, HttpMethod.DELETE, null, Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("TC-009 – UC-004 Rückmeldung ANGEMELDET erfassen")
    @SuppressWarnings("unchecked")
    void tc009_rueckmeldungAngemeldetErfassen() {
        ResponseEntity<Map> response = http.exchange("http://localhost:" + port + "/api/einladungen", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "eventId", eventId,
                        "parteiId", parteiId,
                        "status", "ANGEMELDET",
                        "anzahlPersonen", 3,
                        "hilftAufstellen", true,
                        "buffetBeitrag", "SALAT",
                        "bestaetigungVersendet", false), json), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("ANGEMELDET");

        // Cleanup als Lösch-Test
        String url = "http://localhost:" + port + "/api/einladungen/" + response.getBody().get("id");
        ResponseEntity<Void> del = http.exchange(url, HttpMethod.DELETE, null, Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("TC-010 – UC-004 Rückmeldung ABGEMELDET erfassen (A1)")
    @SuppressWarnings("unchecked")
    void tc010_rueckmeldungAbgemeldetErfassen() {
        ResponseEntity<Map> response = http.exchange("http://localhost:" + port + "/api/einladungen", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "eventId", eventId,
                        "parteiId", parteiId,
                        "status", "ABGEMELDET",
                        "bestaetigungVersendet", false), json), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("ABGEMELDET");

        // Cleanup als Lösch-Test
        String url = "http://localhost:" + port + "/api/einladungen/" + response.getBody().get("id");
        ResponseEntity<Void> del = http.exchange(url, HttpMethod.DELETE, null, Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("TC-042 – UC-004 E1: zweite Einladung für dieselbe Partei zum selben Event wird abgelehnt (DB-002)")
    @SuppressWarnings("unchecked")
    void tc042_einladungDuplikatEventParteiAbgelehnt() {
        Map<String, Object> body = Map.of(
                "eventId", eventId,
                "parteiId", parteiId,
                "status", "OFFEN",
                "bestaetigungVersendet", false);
        ResponseEntity<Map> erste = http.exchange("http://localhost:" + port + "/api/einladungen", HttpMethod.POST,
                new HttpEntity<>(body, json), Map.class);
        assertThat(erste.getStatusCode()).isEqualTo(HttpStatus.OK);
        Number einladungId = (Number) erste.getBody().get("id");

        // Unique-Constraint uk_einladung_event_partei → 409 mit einheitlichem Fehler-JSON (ERROR-001)
        ResponseEntity<Map> duplikat = http.exchange("http://localhost:" + port + "/api/einladungen", HttpMethod.POST,
                new HttpEntity<>(body, json), Map.class);
        assertThat(duplikat.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplikat.getBody().get("status")).isEqualTo(409);
        assertThat((String) duplikat.getBody().get("message")).contains("existiert bereits");

        // Cleanup als Lösch-Test
        ResponseEntity<Void> del = http.exchange("http://localhost:" + port + "/api/einladungen/" + einladungId,
                HttpMethod.DELETE, null, Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("TC-048 – UC-004 Rückmeldung per PUT erfassen; Event und Partei bleiben unveränderlich (REST-003)")
    @SuppressWarnings("unchecked")
    void tc048_rueckmeldungPerPutErfassen() {
        ResponseEntity<Map> erstellt = http.exchange("http://localhost:" + port + "/api/einladungen", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "eventId", eventId,
                        "parteiId", parteiId,
                        "status", "OFFEN",
                        "bestaetigungVersendet", false), json), Map.class);
        assertThat(erstellt.getStatusCode()).isEqualTo(HttpStatus.OK);
        Number einladungId = (Number) erstellt.getBody().get("id");
        String url = "http://localhost:" + port + "/api/einladungen/" + einladungId;

        ResponseEntity<Map> rueckmeldung = http.exchange(url, HttpMethod.PUT,
                new HttpEntity<>(Map.of(
                        "status", "ANGEMELDET",
                        "anzahlPersonen", 3,
                        "hilftAufstellen", true,
                        "buffetBeitrag", "SALAT",
                        "bestaetigungVersendet", false), json), Map.class);
        assertThat(rueckmeldung.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rueckmeldung.getBody().get("status")).isEqualTo("ANGEMELDET");
        assertThat(rueckmeldung.getBody().get("anzahlPersonen")).isEqualTo(3);
        assertThat(rueckmeldung.getBody().get("buffetBeitrag")).isEqualTo("SALAT");
        assertThat(((Map<String, Object>) rueckmeldung.getBody().get("partei")).get("id"))
                .isEqualTo(((Number) parteiId).intValue());

        // Event/Partei sind nicht Teil der Whitelist → unbekanntes Feld → 400
        ResponseEntity<Map> mitEvent = http.exchange(url, HttpMethod.PUT,
                new HttpEntity<>(Map.of(
                        "eventId", eventId,
                        "status", "ANGEMELDET",
                        "bestaetigungVersendet", false), json), Map.class);
        assertThat(mitEvent.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((String) mitEvent.getBody().get("message")).isEqualTo("Unbekanntes Feld: eventId");

        ResponseEntity<Void> del = http.exchange(url, HttpMethod.DELETE, null, Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
