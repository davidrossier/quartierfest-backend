# Technische Schulden — Offene Punkte

> Stand: 2026-05-08. Quellen: SonarQube-Analyse, Clean-Code-Review, Deployment-Analyse.
> UC-spezifische Punkte sind in den jeweiligen `UC-*.md`-Open-Items erfasst.
> Architektur-/Infrastruktur-Übersicht → `specs/architecture.md` (Abschnitt "Bekannte technische Schulden").

---

## CRITICAL

### AUTH-001 – Keine Authentifizierung / Autorisierung

Alle `/api/**`-Endpunkte sind ohne Authentifizierung zugänglich. Spring Security ist nicht konfiguriert.
Jeder mit Netzwerkzugang kann Personen, Parteien, Einladungen, Abrechnungen und Zahlungsdaten lesen, schreiben und löschen.

**Betroffen:** Alle 11 Controller.

**Kontext:** Die App soll künftig öffentlich im Web verfügbar sein. Geplantes Feature: Parteien sollen sich direkt über die Web-App für einen Event anmelden können. Damit entstehen zwei Rollen:
- **Organisator** — voller Zugriff auf alle Domänen
- **Partei** — sieht nur ihre eigene Einladung, Teilnahme und Abrechnung (Datensatz-Ebene)

**Empfehlung: Externer Identity Provider (z.B. Auth0, Supabase Auth)**
- Login, Registrierung und Passwort-Reset werden an den IdP delegiert
- Spring Security validiert nur den JWT via JWKS-Endpoint (~30 Zeilen Config)
- Rollen (`ORGANISATOR`, `PARTEI`) werden im IdP verwaltet und im Token transportiert
- Datensatz-Autorisierung (Partei sieht nur eigene Daten) via `@PreAuthorize` + Custom Security Expression im Service
- Kostenloser Tier bei Auth0/Supabase ausreichend für diesen Use Case

**Nicht empfohlen:** HTTP Basic Auth (kein Multi-User, kein Self-Registration), JWT selbst implementiert (Session-Management, Passwort-Reset, Token-Refresh = hoher Eigenaufwand).

---

## MAJOR

### CORS-001 – CORS `allowedOrigins` hardcoded

`WebConfig.java:13`: `allowedOrigins("http://localhost:4200")` ist hardcoded.
Kein Profil-Support — für Production-Deployment nicht verwendbar.

**Empfehlung:** `@Value("${cors.allowed-origins:http://localhost:4200}")` mit Profil-spezifischen Properties.

```properties
# application-prod.properties
cors.allowed-origins=https://davidrossier.ch
```

---

### DEPLOY-001 – Kein Spring-Profil für Production

Es existiert nur `application.properties` (dev-Defaults: `localhost`, `qfuser/qfpass`).
Für ein Deployment auf `davidrossier.ch` fehlt `application-prod.properties` mit produktiven DB-Credentials, CORS-Origin und ggf. anderem Log-Level.

**Empfehlung:** `application-prod.properties` anlegen; Start mit `--spring.profiles.active=prod`. Sensitive Werte (DB-Passwort) via Umgebungsvariablen injizieren:

```properties
# application-prod.properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
cors.allowed-origins=https://davidrossier.ch
spring.jpa.show-sql=false
```

---

### DEPLOY-002 – Frontend: `localhost:8080` hardcoded in allen Services

Alle 11 Angular-Services (z.B. `person.service.ts`) setzen `http://localhost:8080/api/...` direkt als URL.
Ein Production-Build liefert dadurch eine App, die gegen `localhost` statt den echten Server zeigt.

**Betroffen:** `person.service.ts`, `partei.service.ts`, `event.service.ts`, `einladung.service.ts`, `teilnahme.service.ts`, `konsumationsangebot.service.ts`, `konsumation.service.ts`, `allgemeinausgabe.service.ts`, `abrechnung.service.ts`, `mahnung.service.ts`, `zahlung.service.ts` (im Frontend-Repo).

**Empfehlung:** Angular-Environments einführen (`src/environments/environment.ts` / `environment.prod.ts`) und alle Services auf `environment.apiUrl` umstellen. Für Production: leere `apiUrl` verwenden (relative URLs `/api/...`) — Nginx übernimmt das Routing zum Backend.

---

### DEPLOY-003 – Kein CI/CD-Pipeline-Setup

Es existiert keine `.github/workflows/`-Konfiguration (oder äquivalent).
Tests laufen nur lokal; die Integration Tests (`*IT.java`) benötigen eine echte PostgreSQL-Instanz.

**Empfehlung:** GitHub Actions Workflow mit PostgreSQL-Service-Container:

```yaml
services:
  postgres:
    image: postgres:16
    env:
      POSTGRES_DB: quartierfest
      POSTGRES_USER: qfuser
      POSTGRES_PASSWORD: qfpass
    ports:
      - 5432:5432
```

Pipeline-Schritte: `mvn verify` (Backend-Unit + IT-Tests) → `ng build --configuration production` (Frontend-Build-Check).

---

### PERF-001 – FetchType.EAGER — N+1-Query-Risiko

- `Partei.personen`: `@OneToMany(fetch = FetchType.EAGER)` — bei `GET /api/parteien` werden für jede Partei alle Personen geladen (N+1-Queries)
- `Teilnahme.buffetBeitraege`: `@ElementCollection(fetch = FetchType.EAGER)` — bei `GET /api/teilnahmen` werden alle Beiträge für jede Teilnahme geladen

**Betroffen:** `Partei.java`, `Teilnahme.java`
**Empfehlung:** `FetchType.LAZY` verwenden (JPA-Default); bei Bedarf explizite `JOIN FETCH`-Queries im Repository.

---

### VALID-001 – Fehlende Input-Validierung (@Valid)

Pflichtfeld-Verletzungen liefern HTTP 500 (DB-Constraint-Exception) statt HTTP 400 (Bad Request).
`@Valid` fehlt auf allen `@RequestBody`-Parametern; `@NotNull`/`@NotBlank` fehlen auf Entity-Pflichtfeldern.

**Betroffen:** Alle Controller und Entities.
**Betroffene TCs (als TODO markiert):** TC-002, TC-005, TC-007, TC-012, TC-015, TC-021, TC-023, TC-027, TC-030.
**Empfehlung:** `@Valid` auf `@RequestBody` + Bean-Validation-Annotationen auf Entities ergänzen. Erfordert Spring Boot Validation Starter.

---

## MINOR

### REFACT-001 – Controller/Service CRUD-Boilerplate

8 Controller (`Einladung`, `Teilnahme`, `Konsumationsangebot`, `Konsumation`, `Allgemeinausgabe`, `Abrechnung`, `Zahlung`, `Mahnung`) und 10 Services haben identischen GET/POST/DELETE-Code.

```java
// Identisch in allen 8 Controllern:
@GetMapping public List<T> findAll() { return service.findAll(); }
@PostMapping public T create(@RequestBody T t) { return service.save(t); }
@DeleteMapping("/{id}") public void delete(@PathVariable Long id) { service.delete(id); }
```

**Empfehlung:** Abstrakte `BaseCrudController<T, ID>` und `BaseCrudService<T, ID, R extends JpaRepository<T, ID>>` Basisklassen einführen. `Person-`, `Partei-` und `EventController` erben zusätzlich `PUT`.

---

### TEST-001 – IT-Test-Boilerplate ohne Basisklasse

`setUp()`, `setupPost()`, `tryDelete()`, `id()` sind in allen 13 IT-Klassen identisch kopiert (~400 Zeilen Duplikat-Code).

**Empfehlung:**

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
abstract class AbstractQuartierfestIT {
    protected RestTemplate http;
    protected RestTemplate setup;
    protected HttpHeaders json;
    @LocalServerPort protected int port;

    @BeforeEach void initHttp() { /* gemeinsamer Setup */ }
    protected Map<String, Object> setupPost(String path, Map<String, Object> body) { ... }
    protected long id(Map<String, Object> m) { ... }
    protected void tryDelete(String path) { ... }
}
```

---

### TEST-002 – Raw `Map` / `@SuppressWarnings("unchecked")` in IT-Tests

`RestTemplate.exchange(..., Map.class)` liefert unkontrolliertes `Map<String, Object>`.
`@SuppressWarnings("unchecked")` ist in allen 13 IT-Klassen auf fast jeder Testmethode notwendig.

**Empfehlung:** Typisierte Response-Records oder eigene Assertion-Helpers einführen (z.B. `assertField(response, "id")`). Alternativ akzeptieren (bei simpler CRUD-Struktur vertretbar).

---

### TEST-003 – Keine Unit-Tests für 10 Services

Nur `ParteiService` hat einen Mockito-Unit-Test (`ParteiServiceTest`).
Alle anderen 10 Services haben 0% Unit-Test-Abdeckung und werden nur durch IT-Tests abgedeckt.

**Einschätzung:** Da die Services fast ausschliesslich 1:1 an das Repository delegieren, ist der Mehrwert von Unit-Tests gering. Sinnvoll wäre ein Unit-Test für `AbrechnungService`, sobald dort Berechnungslogik (UC-011) implementiert wird.
