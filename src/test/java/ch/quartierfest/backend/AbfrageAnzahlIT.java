package ch.quartierfest.backend;

/**
 * Traceability:
 *   UC: übergreifend (API-001 Stufe 2, PERF-001)
 *   TCs: TC-054
 *   Last traced: 2026-09-25
 */

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TC-054 (PERF-001-Rest, API-001 Stufe 2): Die Listen-Endpunkte mit verschachtelten Antworten laden mit
 * einer konstanten Anzahl SQL-Statements, unabhängig von der Zeilenzahl — keine N+1-Nachlade-Queries.
 *
 * <p>Die Hibernate-Statistik wird im Test programmatisch eingeschaltet. Ein eigenes Testprofil würde einen
 * zweiten Spring-Kontext starten, der mit dem festen Port der übrigen dev-ITs kollidiert.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("dev")
class AbfrageAnzahlIT {

    private static final int ZEILEN = 3;
    private static final long MAX_STATEMENTS = 2;

    @Autowired private EntityManagerFactory entityManagerFactory;
    @LocalServerPort private int port;

    private RestTemplate setup;
    private HttpHeaders json;
    private Statistics statistik;
    /** In Anlage-Reihenfolge; das Aufräumen läuft rückwärts (Abhängige zuerst). */
    private final List<String> angelegt = new ArrayList<>();

    @BeforeEach
    void setUp() {
        setup = new RestTemplate();
        json = new HttpHeaders();
        json.setContentType(MediaType.APPLICATION_JSON);

        long eventId = anlegen("events", Map.of("datum", "2025-07-05", "startzeit", "15:00:00",
                "standort", "Abfrage-Test"));
        for (int i = 0; i < ZEILEN; i++) {
            long personId = anlegen("persons", Map.of("vorname", "Abfrage", "name", "Person " + i));
            long parteiId = anlegen("parteien", Map.of("bezeichnung", "Abfrage-Partei " + i,
                    "adresse", "Abfragestrasse " + i, "twintAktiv", false, "personenIds", List.of(personId)));
            long einladungId = anlegen("einladungen", Map.of("eventId", eventId, "parteiId", parteiId,
                    "status", "ANGEMELDET", "anzahlPersonen", 2, "bestaetigungVersendet", false));
            long teilnahmeId = anlegen("teilnahmen", Map.of("einladungId", einladungId,
                    "anzahlPersonenEffektiv", 2, "buffetBeitraege", List.of(Map.of("art", "SALAT"))));
            long abrechnungId = anlegen("abrechnungen", Map.of("teilnahmeId", teilnahmeId,
                    "anteilAllgemeinkosten", "10.00", "totalKonsumation", "5.00", "totalBetrag", "15.00",
                    "zustellungskanal", "EMAIL"));
            anlegen("zahlungen", Map.of("abrechnungId", abrechnungId, "zahlungskanal", "BAR",
                    "datum", "2025-07-15", "betrag", "15.00"));
        }

        statistik = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistik.setStatisticsEnabled(true);
    }

    @AfterEach
    void tearDown() {
        statistik.setStatisticsEnabled(false);
        List<String> rueckwaerts = new ArrayList<>(angelegt);
        Collections.reverse(rueckwaerts);
        for (String pfad : rueckwaerts) {
            try { setup.delete(pfad); } catch (Exception ignored) { }
        }
    }

    @ParameterizedTest(name = "GET /api/{0}")
    @ValueSource(strings = {"einladungen", "teilnahmen", "abrechnungen", "zahlungen", "parteien"})
    @DisplayName("TC-054 – Listen-Endpunkte laden ohne N+1-Queries (PERF-001)")
    void tc054_listenEndpunkteOhneNPlusEins(String ressource) {
        statistik.clear();

        List<?> antwort = setup.getForObject("http://localhost:" + port + "/api/" + ressource, List.class);

        assertThat(antwort).hasSizeGreaterThanOrEqualTo(ZEILEN);
        assertThat(statistik.getPrepareStatementCount())
                .as("SQL-Statements für GET /api/%s bei %d+ Zeilen", ressource, antwort.size())
                .isLessThanOrEqualTo(MAX_STATEMENTS);
    }

    @SuppressWarnings("unchecked")
    private long anlegen(String ressource, Map<String, Object> body) {
        Map<String, Object> antwort = setup.postForObject("http://localhost:" + port + "/api/" + ressource,
                new HttpEntity<>(body, json), Map.class);
        long id = ((Number) antwort.get("id")).longValue();
        angelegt.add("http://localhost:" + port + "/api/" + ressource + "/" + id);
        return id;
    }
}
