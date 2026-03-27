# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build (skip tests)
./mvnw clean install -DskipTests

# Run application
./mvnw spring-boot:run

# Unit tests only  (BackendApplicationTests – context load smoke test)
./mvnw test

# Integration tests only
./mvnw test -Dtest="*IT"

# Unit tests + integration tests (preferred for CI)
./mvnw verify

# Single IT class
./mvnw verify -Dit.test=PersonVerwaltenIT

# Single test method
./mvnw test -Dtest=BackendApplicationTests#contextLoads
```

## Database

PostgreSQL is required. The app connects to:
- Host: `localhost:5432`
- Database: `quartierfest`
- Username: `qfuser` / Password: `qfpass`

Schema is auto-managed via `spring.jpa.hibernate.ddl-auto=update`.
SQL logging is enabled via `spring.jpa.show-sql=true`.

## Tech Stack

- **Spring Boot 4.0.3** / **Spring Framework 7.x** / **Java 21**
- **Spring Data JPA** + **PostgreSQL** (runtime)
- **Lombok** (`@Data`, `@RequiredArgsConstructor`) — never write boilerplate manually
- **Spring WebMVC** (synchronous) — not WebFlux
- **spring-boot-devtools** (runtime, optional)

Test scope:
- `spring-boot-starter-data-jpa-test` + `spring-boot-starter-webmvc-test`
- `citrus-bom 4.9.4` (on classpath but **not used** — incompatible with Spring Framework 7.x; `HttpHeaders` no longer implements `MultiValueMap`)
- **maven-failsafe-plugin** runs `*IT.java` classes during `verify`

## Architecture

### CORS

`WebConfig.java` erlaubt Cross-Origin-Requests von `http://localhost:4200` (Angular-Dev-Server) auf alle `/api/**`-Endpunkte für die Methoden `GET`, `POST`, `DELETE`.

### Domänen

Code is organized by domain under `ch.quartierfest.backend`. Each domain is a package with exactly 4 files:

- **Entity** (`@Entity`, `@Data`) — JPA-mapped table
- **Repository** (`JpaRepository<Entity, Long>`) — data access
- **Service** (`@Service`, `@RequiredArgsConstructor`) — business logic
- **Controller** (`@RestController`, `@RequestMapping("/api/...")`) — HTTP endpoints

All controllers expose the same three operations:
- `GET /api/{resource}` — list all
- `POST /api/{resource}` — create, returns `200 OK` + saved entity
- `DELETE /api/{resource}/{id}` — delete, returns `200 OK`

No PUT/PATCH endpoints exist.

| Domain | Endpoint | Beziehungen |
|---|---|---|
| `person` | `/api/persons` | — |
| `partei` | `/api/parteien` | `@OneToMany` → Person |
| `event` | `/api/events` | — |
| `einladung` | `/api/einladungen` | `@ManyToOne` → Event, Partei |
| `teilnahme` | `/api/teilnahmen` | `@OneToOne` → Einladung |
| `konsumationsangebot` | `/api/konsumationsangebote` | `@ManyToOne` → Event |
| `konsumation` | `/api/konsumationen` | `@ManyToOne` → Teilnahme, Konsumationsangebot |
| `allgemeinausgabe` | `/api/allgemeinausgaben` | `@ManyToOne` → Event |
| `abrechnung` | `/api/abrechnungen` | `@OneToOne` → Teilnahme |
| `zahlung` | `/api/zahlungen` | `@ManyToOne` → Abrechnung |
| `mahnung` | `/api/mahnungen` | `@ManyToOne` → Abrechnung |

Enums sind als innere Klassen in der jeweiligen Entity definiert:
`Einladung.EinladungStatus`, `Einladung.BuffetBeitrag`, `Abrechnung.Zustellungskanal`, `Zahlung.Zahlungskanal`

## Tests

### Unit tests
`src/test/java/ch/quartierfest/backend/BackendApplicationTests.java` — Spring context load smoke test.

### Integration tests
13 `*IT.java` Klassen unter `src/test/java/ch/quartierfest/backend/citrus/`.
Laufen gegen eine echte PostgreSQL-Datenbank (kein Mocking).
**28 Testfälle (TC-001..TC-028), alle grün.**

Verwendetes Muster:
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class XxxIT {
    private RestTemplate http;          // no-op error handler → wirft nie bei 4xx/5xx
    @LocalServerPort private int port;
    private RestTemplate setup;         // für @BeforeEach-Voraussetzungen
    private HttpHeaders json;
    private final List<String> toDelete = new ArrayList<>();  // im Test erstellte Datensätze

    @BeforeEach void setUp() {
        setup = new RestTemplate();
        http = new RestTemplate();
        http.setErrorHandler(new ResponseErrorHandler() {
            public boolean hasError(ClientHttpResponse r) { return false; }
            public void handleError(ClientHttpResponse r) { }
        });
        json = new HttpHeaders();
        json.setContentType(MediaType.APPLICATION_JSON);
        // Voraussetzungen via setup.postForObject("http://localhost:" + port + "/api/...", ...)
    }

    @AfterEach void tearDown() {
        toDelete.forEach(this::tryDelete);   // Kind-Datensätze zuerst
        // dann @BeforeEach-Datensätze in umgekehrter FK-Reihenfolge
        if (xyzId != null) tryDelete("http://localhost:" + port + "/api/xyz/" + xyzId);
    }

    private void tryDelete(String path) {
        try { setup.delete(path); } catch (Exception ignored) {}
    }

    @Test @DisplayName("TC-XXX – ...")
    void tcXxx_...() {
        ResponseEntity<Map> r = http.exchange(
            "http://localhost:" + port + "/api/...", HttpMethod.POST,
            new HttpEntity<>(Map.of(...), json), Map.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        toDelete.add("http://localhost:" + port + "/api/.../" + r.getBody().get("id"));
    }
}
```

**Teardown-Strategie:** JUnit 5 erzeugt pro Testmethode eine neue Instanz, daher ist `toDelete` per Test leer. Im Test erstellte Datensätze werden via `toDelete.add(...)` registriert und in `@AfterEach` als erstes gelöscht (vor den `@BeforeEach`-Datensätzen), damit FK-Constraints nicht verletzt werden.

Bekannte Einschränkungen (als TODO in den IT-Klassen markiert):
- Kein `@Valid` auf Controllern → Pflichtfeldverletzungen liefern `500` statt `400`
- Kein PATCH-Endpunkt (z.B. für `bestaetigungVersendet`, `zustellungsDatum`)
- Kein `GET /api/events/{id}/konsumationsliste` (UC-009 nur teilweise abgedeckt)
- Kein Auto-Kalkulationsendpunkt für Abrechnungen (UC-011 manuell)

## Specifications

Alle Spezifikationen liegen unter `specs/`:

| Datei | Inhalt |
|---|---|
| `use-cases_overview.md` | Übersicht aller 13 Use Cases |
| `UC-001` .. `UC-013` | Einzelne Use Cases (UC-004 = Einladung, UC-005 = Teilnahme) |
| `testdesign.md` | Testdesign mit TC-001..TC-028, Transportstrategie, Open Items |
| `datamodel.md` | Datenmodell |
| `architecture.md` | Architekturdiagramm |

## Claude Code Skills

Drei projektspezifische Skills in `.claude/commands/`:

| Slash-Befehl | Zweck |
|---|---|
| `/refine-use-case` | Rohe Use Cases in das Standard-Format (`_template_use-case.md`) umschreiben |
| `/review-use-case` | Alle `UC-*.md` in `specs/` prüfen, kleine Fehler beheben, Open Items anlegen |
| `/design-citrus-tests` | Testdesign erstellen und IT-Klassen generieren |
