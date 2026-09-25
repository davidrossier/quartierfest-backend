package ch.quartierfest.backend;

/**
 * Traceability:
 *   UC: übergreifend (API-001 Stufe 2, REST-002, REST-003)
 *   TCs: TC-052, TC-053
 *   Last traced: 2026-09-25
 */

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
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
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Übergreifende REST-Konventionen des DTO-Layers (API-001 Stufe 2):
 * <ul>
 *   <li>TC-052: PUT auf eine nicht-existente id liefert 404 und legt nichts an (REST-002, REST-003).</li>
 *   <li>TC-053: POST mit id im Body wird als unbekanntes Feld mit 400 abgelehnt — kein POST-Upsert
 *       mehr auf irgendeiner Ressource (REST-001, E6).</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("dev")
class RestKonventionenIT {

    private static final long UNBEKANNT = 999_999_999L;

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

    /** Pro PUT-Endpunkt ein gültiger Body — sonst würde die Validierung (400) vor der Existenzprüfung greifen. */
    static Stream<Arguments> putEndpunkte() {
        return Stream.of(
                Arguments.of("persons", Map.of("vorname", "Hans", "name", "Müller")),
                Arguments.of("events", Map.of("datum", "2025-07-05", "startzeit", "15:00:00", "standort", "X")),
                Arguments.of("parteien", Map.of("bezeichnung", "X", "adresse", "Y", "twintAktiv", false)),
                Arguments.of("einladungen", Map.of("status", "OFFEN", "bestaetigungVersendet", false)),
                Arguments.of("konsumationsangebote", Map.of("eventId", 1, "bezeichnung", "Bier", "preis", "3.00")),
                Arguments.of("allgemeinausgaben", Map.of("eventId", 1, "beschreibung", "Miete", "betrag", "10.00")),
                Arguments.of("konsumationen", Map.of("anzahl", 2)),
                Arguments.of("abrechnungen", Map.of("anteilAllgemeinkosten", "1.00", "totalKonsumation", "2.00",
                        "totalBetrag", "3.00", "zustellungskanal", "EMAIL")));
    }

    @ParameterizedTest(name = "PUT /api/{0}/<unbekannte id>")
    @MethodSource("putEndpunkte")
    @DisplayName("TC-052 – PUT auf nicht-existente id liefert 404 (REST-002, REST-003)")
    @SuppressWarnings("unchecked")
    void tc052_putAufUnbekannteIdLiefert404(String ressource, Map<String, Object> body) {
        ResponseEntity<Map> response = http.exchange(
                "http://localhost:" + port + "/api/" + ressource + "/" + UNBEKANNT, HttpMethod.PUT,
                new HttpEntity<>(body, json), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("status")).isEqualTo(404);
        assertThat((String) response.getBody().get("message")).endsWith("nicht gefunden.");
    }

    @ParameterizedTest(name = "POST /api/{0} mit id")
    @ValueSource(strings = {"persons", "events", "parteien", "benutzer", "einladungen", "teilnahmen",
            "konsumationsangebote", "allgemeinausgaben", "konsumationen", "abrechnungen", "zahlungen", "mahnungen"})
    @DisplayName("TC-053 – POST mit id im Body wird mit 400 abgelehnt (kein POST-Upsert, E6)")
    @SuppressWarnings("unchecked")
    void tc053_postMitIdWirdAbgelehnt(String ressource) {
        ResponseEntity<Map> response = http.exchange("http://localhost:" + port + "/api/" + ressource,
                HttpMethod.POST, new HttpEntity<>(Map.of("id", 1), json), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("message")).isEqualTo("Unbekanntes Feld: id");
    }
}
