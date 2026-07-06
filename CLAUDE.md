# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build (skip tests)
./mvnw clean install -DskipTests

# Run application
./mvnw spring-boot:run

# Unit tests (Controller-, Service- und Smoke-Tests)
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

**CI:** GitHub Actions (`.github/workflows/ci.yml`) läuft bei Push/PR auf `main`: `./mvnw --batch-mode verify` gegen einen PostgreSQL-16-Service-Container (gleiche Credentials wie lokal — keine Teständerungen nötig).

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
- **spring-boot-starter-validation** — Bean Validation (`@NotBlank`, `@NotNull` auf Entities; `@Valid` auf `@RequestBody`)
- **spring-boot-starter-oauth2-resource-server** — JWT-Validierung; Eigenbau-Login (AUTH-002): Backend stellt HS256-JWTs selbst aus (`JwtEncoder`/`NimbusJwtDecoder.withSecretKey`), kein externer IdP
- **spring-boot-devtools** (runtime, optional)

Test scope:
- `spring-boot-starter-data-jpa-test` + `spring-boot-starter-webmvc-test`
- `spring-security-test` — Mock-JWT-Support für Slice-Tests (aktuell v. a. als Reserve; die Auth-ITs nutzen echte Tokens via `POST /api/auth/login`)
- `citrus-bom 4.9.4` (on classpath but **not used** — incompatible with Spring Framework 7.x; `HttpHeaders` no longer implements `MultiValueMap`)
- **maven-failsafe-plugin** runs `*IT.java` classes during `verify`

## Architecture

### Security & Auth (AUTH-001/AUTH-002, Eigenbau-Login)

`SecurityConfig.java` definiert zwei profil-abhängige Filter-Chains plus CORS (Property `cors.allowed-origins`, Dev-Default `http://localhost:4200`):

| Profil | Verhalten |
|---|---|
| `prod`, `security-test` | Autorisierungsmatrix: `POST /api/auth/login` offen; `/api/benutzer/**` nur `ORGANISATOR`; `GET /api/teilnahmen/meine` + `PUT /api/teilnahmen/{id}` für `ORGANISATOR`/`PARTEI`; alle übrigen `/api/**` nur `ORGANISATOR` |
| Default (Dev/Tests) | `permitAll()`, aber Bearer-Tokens werden trotzdem verarbeitet — `/meine` und Ownership-Checks funktionieren auch lokal |

- **Eigenbau-JWT:** `POST /api/auth/login` (Package `auth`) prüft BCrypt-Hash und stellt ein HS256-JWT aus (Claims `sub` = Benutzer-ID, `email`, `rolle`; 12 h). Secret: `auth.jwt.secret` (prod: `AUTH_JWT_SECRET`, min. 32 Zeichen).
- **Rollen-Mapping:** `JwtAuthenticationConverter` mappt den Claim `rolle` → `ROLE_*` (Spring-Default liest nur `scope`).
- **Ownership (UC-016):** `@PreAuthorize("@teilnahmeZugriff.darfBearbeiten(...)")` auf `TeilnahmeService.update()` — Methoden-Security (`@EnableMethodSecurity`), wirkt in allen Profilen.
- **Bootstrap:** Beim Start wird ohne vorhandenen `ORGANISATOR` ein Admin aus `auth.bootstrap.email/password` angelegt (Dev: `admin@quartierfest.local` / `quartierfest-admin`; prod: `AUTH_INITIAL_ADMIN_*`).
- **Passwörter:** nie in API-Antworten (`@JsonIgnore` auf `passwortHash`, `passwort` ist `@Transient` WRITE_ONLY, min. 10 Zeichen). Achtung: Hibernate validiert `@Transient`-Constraints beim Persistieren — `passwort` muss beim `save()` gesetzt sein und wird erst danach geleert.

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

Ausnahmen:
- `PersonController`, `ParteiController` und `EventController` haben zusätzlich `PUT /api/{resource}/{id}` — update, returns `200 OK` + updated entity.
- `TeilnahmeController` (UC-016): `GET /api/teilnahmen/meine` (eigene Teilnahme via JWT `sub`, nächster Event) und `PUT /api/teilnahmen/{id}` mit Whitelist-DTO `TeilnahmeUpdateRequest` (`einladung` nie änderbar; PARTEI nur eigene → sonst 403).
- `BenutzerController` (UC-015): zusätzlich `PUT /api/benutzer/{id}/passwort` (Reset); Duplikat-E-Mail und letzter-ORGANISATOR-Löschung → `409`.
- `AuthController` (UC-014): nur `POST /api/auth/login` → `{token}`; falsche Credentials → `401`.

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
| `benutzer` | `/api/benutzer` | `@ManyToOne` → Partei (optional; Pflicht bei Rolle PARTEI) |
| `auth` | `/api/auth/login` | kein Entity — Login-Endpunkt, JwtConfig-Beans in `SecurityConfig`, Bootstrap in `auth/BootstrapConfig` |

Enums sind als innere Klassen in der jeweiligen Entity definiert:
`Einladung.EinladungStatus`, `Einladung.BuffetBeitrag`, `Abrechnung.Zustellungskanal`, `Zahlung.Zahlungskanal`, `Benutzer.Rolle`

## Tests

### Unit tests

**Controller-Tests** (`*ControllerTest.java`, 13 Klassen unter `src/test/java/ch/quartierfest/backend/<domäne>/`):
- Verwende `@WebMvcTest(<Controller>.class)` — lädt nur die Web-Schicht, kein PostgreSQL nötig
- `@MockitoBean` für den Service; `@Autowired MockMvc` für Requests
- **Jackson 3.x:** Spring Boot 4.x konfiguriert `tools.jackson.databind.ObjectMapper` als Bean — `@Autowired ObjectMapper` muss diesen Typ importieren, **nicht** `com.fasterxml.jackson.databind.ObjectMapper` (Jackson 2.x, nur im Test-Scope via Citrus vorhanden und nicht als Spring Bean registriert)
- Traceability via `@DisplayName("UC-XXX: ...")`
- 49 Testmethoden
- **Einschränkung:** `@AuthenticationPrincipal`-Parameter sind im MVC-Slice nicht auflösbar (Resolver fehlt) — `GET /api/teilnahmen/meine` wird deshalb nur via IT getestet (TC-036)

```java
@WebMvcTest(PersonController.class)
class PersonControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private PersonService personService;
    @Autowired private tools.jackson.databind.ObjectMapper objectMapper;  // Jackson 3.x!

    @Test
    @DisplayName("UC-001: POST /api/persons legt eine Person an")
    void create_returnsSavedPerson() throws Exception {
        when(personService.save(any(Person.class))).thenReturn(buildPerson());
        mockMvc.perform(post("/api/persons")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildPerson())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.vorname").value("Hans"));
    }
}
```

**Service-Tests** (`@ExtendWith(MockitoExtension.class)` — reine Mockito-Tests, kein Spring-Kontext):
- `ParteiServiceTest` — `save()` löst `personenIds` via `PersonRepository` auf (4 Methoden)
- `BenutzerServiceTest` — BCrypt-Hashing, Duplikat-E-Mail/letzter-ORGANISATOR → 409, Passwort-Reset (6 Methoden)
- `AuthServiceTest` — Token-Claims, 401 bei falschen Credentials/unbekannter E-Mail (3 Methoden)

**Smoke-Test**: `BackendApplicationTests.java` — Spring-Kontext-Ladetest (braucht PostgreSQL).

### Integration tests
17 `*IT.java` Klassen je im Domain-Package unter `src/test/java/ch/quartierfest/backend/<domäne>/` (z.B. `person/PersonVerwaltenIT.java`, `benutzer/BenutzerVerwaltenIT.java`).
Laufen gegen eine echte PostgreSQL-Datenbank (kein Mocking).
**38 Testmethoden (TC-001..TC-040, ohne TC-003 und TC-017 die in TC-001 bzw. TC-016 integriert sind).**

Auth-Besonderheiten:
- `TeilnahmeBestaetigenIT` (TC-036/037) holt sich echte JWTs via `POST /api/auth/login` — die Ownership-403-Fälle laufen im Default-Profil (Methoden-Security)
- `SecurityMatrixIT` (TC-040) läuft als einziger IT mit `@ActiveProfiles("security-test")` (prod-gleiche URL-Matrix, `src/test/resources/application-security-test.properties`) und `RANDOM_PORT` (der Default-Context belegt 8080)
- Der Bootstrap-ORGANISATOR (`admin@quartierfest.local`) existiert in allen IT-Läufen; `tc039` setzt via JUnit-Assumption genau einen ORGANISATOR voraus

Verwendetes Muster:
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class XxxIT {
    private RestTemplate http;          // no-op error handler → wirft nie bei 4xx/5xx
    @LocalServerPort private int port;
    private RestTemplate setup;         // für @BeforeEach-Voraussetzungen
    private HttpHeaders json;

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
        // @BeforeEach-Fixtures in umgekehrter FK-Reihenfolge bereinigen
        if (xyzId != null) tryDelete("http://localhost:" + port + "/api/xyz/" + xyzId);
    }

    private void tryDelete(String path) {
        try { setup.delete(path); } catch (Exception ignored) {}
    }

    @Test @DisplayName("TC-XXX – ...")
    @SuppressWarnings("unchecked")
    void tcXxx_...() {
        ResponseEntity<Map> r = http.exchange(
            "http://localhost:" + port + "/api/...", HttpMethod.POST,
            new HttpEntity<>(Map.of(...), json), Map.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Cleanup als Lösch-Test
        String url = "http://localhost:" + port + "/api/.../" + r.getBody().get("id");
        ResponseEntity<Void> del = http.exchange(url, HttpMethod.DELETE, null, Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
```

**Cleanup-Strategie:** Im Test erstellte Datensätze werden **innerhalb des Tests** via expliziter `DELETE`-Assertion gelöscht — der Lösch-Test dient gleichzeitig als Cleanup. `@BeforeEach`-Fixtures werden im `@AfterEach` in umgekehrter FK-Reihenfolge bereinigt (abhängige Tabellen zuerst). Es gibt keine `toDelete`-Liste mehr.

Bekannte Einschränkungen (als TODO in den IT-Klassen markiert):
- Kein PATCH-Endpunkt (z.B. für `bestaetigungVersendet`, `zustellungsDatum`)
- Kein `GET /api/events/{id}/konsumationsliste` (UC-009 nur teilweise abgedeckt)
- Kein Auto-Kalkulationsendpunkt für Abrechnungen (UC-011 manuell)
- TC-012 / TC-023: Referenz auf nicht-existierende FK-ID liefert weiterhin `500` (kein EntityNotFound-Handler)

Vollständige technische Schulden → `specs/TODO.md`

## Specifications

Alle Spezifikationen liegen unter `specs/`:

| Datei | Inhalt |
|---|---|
| `use-cases_overview.md` | Übersicht aller 16 Use Cases |
| `UC-001` .. `UC-016` | Einzelne Use Cases (UC-004 = Einladung, UC-005 = Teilnahme, UC-014..016 = Auth/Eigenbau-Login) |
| `testdesign.md` | Testdesign mit TC-001..TC-040, Transportstrategie, Open Items |
| `datamodel.md` | Datenmodell |
| `architecture.md` | Architekturdiagramm, REST-Endpunkte, Traceability-Matrix, technische Schulden |
| `TODO.md` | Technische Schulden (SonarQube-Befunde, Refactoring-Backlog) |

## Claude Code Skills

Fünf projektspezifische Skills in `.claude/commands/`:

| Slash-Befehl | Zweck |
|---|---|
| `/refine-use-case` | Rohe Use Cases in das Standard-Format (`_template_use-case.md`) umschreiben |
| `/review-use-case` | Alle `UC-*.md` in `specs/` prüfen, kleine Fehler beheben, Open Items anlegen |
| `/design-citrus-tests` | Testdesign erstellen und IT-Klassen generieren |
| `/clean-code-review` | Code + Tests nach Clean-Code-Prinzipien prüfen, Spec-Konformität sicherstellen, UC-Referenzen als Kommentare setzen |
| `/traceability-manager` | UC → Implementierung → Test Traceability analysieren, Matrizen in `architecture.md` und `testdesign.md` aktualisieren, Lücken melden |
