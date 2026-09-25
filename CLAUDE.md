# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build (skip tests)
./mvnw clean install -DskipTests

# Run application (aktiviert automatisch das dev-Profil → offene Security-Chain, SEC-001)
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

**CI:** GitHub Actions (`.github/workflows/ci.yml`) läuft bei Push/PR auf `main`: `./mvnw --batch-mode verify` gegen einen PostgreSQL-16-Service-Container (gleiche Credentials wie lokal — keine Teständerungen nötig). Die Playwright-E2E-Suite läuft nächtlich im Frontend-Repo (`e2e.yml`, CI-001) gegen dieses Repo auf `main` (manuell auch gegen einen Feature-Branch via `backend_ref`); Readiness über `GET /actuator/health`.

## Database

PostgreSQL is required. The app connects to:
- Host: `localhost:5432`
- Database: `quartierfest`
- Username: `qfuser` / Password: `qfpass`

Schema-Verwaltung (DB-001): **Flyway-Migrationen** in `src/main/resources/db/migration` (`V1__baseline.sql` = eingefrorenes Hibernate-Schema, `V2__…` = DB-002-Constraints). Hibernate läuft mit `spring.jpa.hibernate.ddl-auto=validate` — jede Entity-Änderung braucht ein neues `V<n>__<beschreibung>.sql` im selben PR, sonst schlägt der Start mit `SchemaManagementException` fehl. Bestehende DBs werden via `spring.flyway.baseline-on-migrate=true` beim ersten Start auf V1 baselined; leere DBs (CI) bauen V1+V2 auf.
SQL logging is enabled via `spring.jpa.show-sql=true`.

## Tech Stack

- **Spring Boot 4.0.3** / **Spring Framework 7.x** / **Java 21**
- **Spring Data JPA** + **PostgreSQL** (runtime)
- **Lombok** (`@Data`, `@RequiredArgsConstructor`) — never write boilerplate manually
- **Spring WebMVC** (synchronous) — not WebFlux
- **spring-boot-starter-validation** — Bean Validation (`@NotBlank`, `@NotNull` auf Entities; `@Valid` auf `@RequestBody`)
- **caffeine** — In-Memory-Cache mit TTL für die Login-Drosselung (SEC-002)
- **spring-boot-starter-flyway** + `flyway-database-postgresql` — Schema-Migrationen (DB-001); Spring Boot 4 aktiviert Flyway nur über den Starter, `flyway-core` allein reicht nicht
- **spring-boot-starter-oauth2-resource-server** — JWT-Validierung; Eigenbau-Login (AUTH-002): Backend stellt HS256-JWTs selbst aus (`JwtEncoder`/`NimbusJwtDecoder.withSecretKey`), kein externer IdP
- **spring-boot-starter-actuator** — nur `GET /actuator/health` exponiert (ohne Details, ohne Token; OPS-001/CI-001) als Readiness-Signal für die E2E-CI und den Betrieb
- **springdoc-openapi-starter-webmvc-ui 3.1.1** — OpenAPI-Spec `/v3/api-docs` + Swagger-UI `/swagger-ui.html` (API-001 Stufe 1); 3.x ist die Linie für Spring Boot 4 / Jackson 3
- **spring-boot-devtools** (runtime, optional)

Test scope:
- `spring-boot-starter-data-jpa-test` + `spring-boot-starter-webmvc-test`
- `spring-security-test` — Mock-JWT-Support für Slice-Tests (aktuell v. a. als Reserve; die Auth-ITs nutzen echte Tokens via `POST /api/auth/login`)
- **maven-failsafe-plugin** runs `*IT.java` classes during `verify`

## Architecture

### Security & Auth (AUTH-001/AUTH-002, Eigenbau-Login)

`SecurityConfig.java` definiert zwei profil-abhängige Filter-Chains plus CORS (Property `cors.allowed-origins`, Dev-Default `http://localhost:4200`). Der Default ist fail-closed (SEC-001):

| Profil | Verhalten |
|---|---|
| Default (kein Profil), `prod`, `security-test` | Autorisierungsmatrix: `POST /api/auth/login` und `GET /actuator/health` offen; `/api/benutzer/**` nur `ORGANISATOR`; `GET /api/teilnahmen/meine` + `PUT /api/teilnahmen/{id}` für `ORGANISATOR`/`PARTEI`; alle übrigen `/api/**` nur `ORGANISATOR` |
| `dev` (explizit) | `permitAll()` mit WARN-Log, aber Bearer-Tokens werden trotzdem verarbeitet — `/meine` und Ownership-Checks funktionieren auch lokal |

`./mvnw spring-boot:run` aktiviert das `dev`-Profil automatisch (pom.xml, `spring-boot-maven-plugin`). Das gepackte Jar und der IDE-Start der Main-Klasse sind fail-closed — dort das Profil bei Bedarf manuell setzen (`--spring.profiles.active=dev`).

- **Brute-Force-Drosselung (SEC-002):** `auth/LoginDrosselung` zählt Fehlversuche in-memory (Caffeine) pro E-Mail und pro Client-IP; `AuthService.login()` wirft bei Sperre `429` vor dem Credential-Check. Properties `auth.drosselung.max-fehlversuche-email=5`, `max-fehlversuche-ip=20`, `sperre-minuten=15` (0 = aus). Prod: `server.forward-headers-strategy=native` für die Client-IP hinter einem Reverse-Proxy. ITs, die Fehl-Logins provozieren, müssen `loginDrosselung.zuruecksetzen()` im `@AfterEach` aufrufen (geteilter Context).
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
- `AuthController` (UC-014): nur `POST /api/auth/login` → `{token}`; falsche Credentials → `401`; gesperrt (SEC-002) → `429`.

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

### API-Contract (API-001 Stufe 1)

Der Ist-Contract (Entities als Request-/Response-Schema) ist als OpenAPI-3-Spec in `specs/openapi.json` **versioniert**. `OpenApiContractIT` (TC-046, `dev`-Profil) vergleicht `GET /v3/api-docs` byte-genau mit der Datei (Keys sortiert, pretty-printed).

- **Workflow bei Contract-Änderung** (Entity-Feld, Controller, DTO): im selben PR wie die Flyway-Migration die Spec neu erzeugen — sonst rot in CI:
  ```bash
  OPENAPI_UPDATE=true ./mvnw verify -Dit.test=OpenApiContractIT
  ```
- Das Frontend generiert seine TypeScript-Typen aus dieser Datei (`npm run api:generate`) und prüft in seiner CI gegen Backend-`main` auf Drift → Backend-PR **vor** dem Frontend-PR mergen.
- `OpenApiConfig` liefert nur Metadaten (Titel, `bearerAuth`-Schema für den Authorize-Button der Swagger-UI). Swagger-UI lokal: `http://localhost:8080/swagger-ui.html` (`dev`-Profil). Ohne Profil: 401 (fail-closed); `prod`: `springdoc.api-docs.enabled=false` → 404.
- Stufe 2 (DTO-Layer, getrennte Request-/Response-Schemas) ist offen → `specs/TODO.md`.

### Fehlerbehandlung (ERROR-001)

`GlobalExceptionHandler` (`@RestControllerAdvice` im Root-Package, erbt von `ResponseEntityExceptionHandler`) liefert für alle Fehlerpfade einheitliches JSON `{status, message}` — kein Fehler-Handling in einzelnen Controllern nötig:
- `ResponseStatusException` behält Status + Reason (Services werfen weiterhin 401/403/404/409 damit)
- Constraint-Verletzung (`DataIntegrityViolationException`) → `409`: Unique-Verletzung (DB-002, `ConstraintKind.UNIQUE`) → «Datensatz existiert bereits.», FK-Verletzung → «Referenzierter Datensatz existiert nicht oder wird noch verwendet.»
- `EntityNotFoundException`/`JpaObjectRetrievalFailureException` → `404`
- Bean-Validation (`@Valid`) → `400` mit alphabetischer Feldliste in `message`
- Fallback → `500` generisch + ERROR-Log; `AccessDeniedException` aus `@PreAuthorize` wird an die Security-Kette durchgereicht (403, kein 500)

## Tests

### Unit tests

**Controller-Tests** (`*ControllerTest.java`, 13 Klassen unter `src/test/java/ch/quartierfest/backend/<domäne>/`):
- Verwende `@WebMvcTest(<Controller>.class)` — lädt nur die Web-Schicht, kein PostgreSQL nötig
- `@MockitoBean` für den Service; `@Autowired MockMvc` für Requests
- **Jackson 3.x:** Spring Boot 4.x konfiguriert `tools.jackson.databind.ObjectMapper` als Bean — `@Autowired ObjectMapper` muss diesen Typ importieren, nicht `com.fasterxml.jackson.databind.ObjectMapper` (Jackson 2.x, nicht auf dem Classpath). Die Annotations `com.fasterxml.jackson.annotation.*` (`@JsonIgnore`, `@JsonProperty`) bleiben dagegen legitim im Einsatz — Jackson 3 liest sie weiterhin
- Traceability via `@DisplayName("UC-XXX: ...")`
- 51 Testmethoden
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
- `AuthServiceTest` — Token-Claims, 401 bei falschen Credentials/unbekannter E-Mail, 429 bei Sperre, Zähler-Aufrufe (5 Methoden)
- `LoginDrosselungTest` — SEC-002-Zähler mit gestellter Caffeine-`Ticker`-Uhr: Limits, Normalisierung, Ablauf, Reset (6 Methoden)

**Smoke-Test**: `BackendApplicationTests.java` — Spring-Kontext-Ladetest (braucht PostgreSQL).

### Integration tests
18 `*IT.java` Klassen: 17 je im Domain-Package unter `src/test/java/ch/quartierfest/backend/<domäne>/` (z.B. `person/PersonVerwaltenIT.java`, `benutzer/BenutzerVerwaltenIT.java`) plus `OpenApiContractIT` im Root-Package (TC-046, API-001).
Laufen gegen eine echte PostgreSQL-Datenbank (kein Mocking).
Alle ITs ausser `SecurityMatrixIT` tragen `@ActiveProfiles("dev")` (offene Security-Chain, SEC-001) — byte-identisch, damit alle denselben gecachten Spring-Context teilen.
**45 Testmethoden (TC-001..TC-047, ohne TC-003 und TC-017 die in TC-001 bzw. TC-016 integriert sind).**

Auth-Besonderheiten:
- `TeilnahmeBestaetigenIT` (TC-036/037) holt sich echte JWTs via `POST /api/auth/login` — die Ownership-403-Fälle laufen im dev-Profil (Methoden-Security)
- `SecurityMatrixIT` (TC-040; TC-047 Actuator-Health offen, Rest gesperrt) läuft als einziger IT mit `@ActiveProfiles("security-test")` (prod-gleiche URL-Matrix, `src/test/resources/application-security-test.properties`) und `RANDOM_PORT` (der Default-Context belegt 8080)
- Der Bootstrap-ORGANISATOR (`admin@quartierfest.local`) existiert in allen IT-Läufen; `tc039` setzt via JUnit-Assumption genau einen ORGANISATOR voraus

Verwendetes Muster:
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("dev")
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

Vollständige technische Schulden → `specs/TODO.md`

## Specifications

Alle Spezifikationen liegen unter `specs/`:

| Datei | Inhalt |
|---|---|
| `use-cases_overview.md` | Übersicht aller 16 Use Cases |
| `UC-001` .. `UC-016` | Einzelne Use Cases (UC-004 = Einladung, UC-005 = Teilnahme, UC-014..016 = Auth/Eigenbau-Login) |
| `testdesign.md` | Testdesign mit TC-001..TC-047, Transportstrategie, Open Items |
| `datamodel.md` | Datenmodell |
| `architecture.md` | Architekturdiagramm, REST-Endpunkte, Traceability-Matrix, technische Schulden |
| `TODO.md` | Technische Schulden (SonarQube-Befunde, Refactoring-Backlog) |
| `openapi.json` | Versionierter API-Contract (springdoc-Dump, via `OpenApiContractIT` abgeglichen; Quelle für die Frontend-Typen) |
| `plans/` | Umsetzungspläne für grössere TODO-Punkte (siehe unten) |

**Umsetzungspläne** liegen unter `specs/plans/`, benannt nach dem TODO-Punkt (`<ID>_<Stufe/Thema>_Plan.md`). Ein Plan wird vor der Umsetzung angelegt, im TODO-Eintrag verlinkt und während der Umsetzung im Abschnitt «Umsetzungsnotizen» nachgeführt; nach Abschluss bleibt er als Entscheidungsdokumentation liegen.

| Datei | Inhalt |
|---|---|
| `plans/API-001_Stufe-1_Plan.md` | API-001 Stufe 1 (OpenAPI, generierte Typen, Drift-Check) — umgesetzt 2026-09-17 |
| `plans/API-001_Stufe-2_Plan.md` | API-001 Stufe 2 (DTO-Layer, inkl. CODE-002, REST-002, REST-003) |

## Claude Code Skills

Fünf projektspezifische Skills in `.claude/commands/`:

| Slash-Befehl | Zweck |
|---|---|
| `/refine-use-case` | Rohe Use Cases in das Standard-Format (`_template_use-case.md`) umschreiben |
| `/review-use-case` | Alle `UC-*.md` in `specs/` prüfen, kleine Fehler beheben, Open Items anlegen |
| `/design-citrus-tests` | Testdesign erstellen und IT-Klassen generieren |
| `/clean-code-review` | Code + Tests nach Clean-Code-Prinzipien prüfen, Spec-Konformität sicherstellen, UC-Referenzen als Kommentare setzen |
| `/traceability-manager` | UC → Implementierung → Test Traceability analysieren, Matrizen in `architecture.md` und `testdesign.md` aktualisieren, Lücken melden |
