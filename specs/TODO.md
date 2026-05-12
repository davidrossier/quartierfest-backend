# Technische Schulden

> Stand: 2026-05-09. Quellen: SonarQube-Analyse, Clean-Code-Review, Deployment-Analyse.
> UC-spezifische Punkte sind in den jeweiligen `UC-*.md`-Open-Items erfasst.
> Architektur-/Infrastruktur-Übersicht → `specs/architecture.md` (Abschnitt "Bekannte technische Schulden").

---

## MAJOR


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

**Umsetzung (zwei Repos):**

- **Backend** (`.github/workflows/ci.yml`): PostgreSQL-Service-Container (postgres:16, db=quartierfest, user=qfuser, pass=qfpass) + `./mvnw verify` — Unit-Tests und alle `*IT.java` laufen gegen die echte DB, keine Teständerungen nötig.
- **Frontend** (`.github/workflows/ci.yml`): `npm ci` + `npm run build -- --configuration production` — prüft, ob der Production-Build fehlerfrei kompiliert.

---

### PERF-001 – FetchType.EAGER — N+1-Query-Risiko

- `Partei.personen`: `@OneToMany(fetch = FetchType.EAGER)` — bei `GET /api/parteien` werden für jede Partei alle Personen geladen (N+1-Queries)
- `Teilnahme.buffetBeitraege`: `@ElementCollection(fetch = FetchType.EAGER)` — bei `GET /api/teilnahmen` werden alle Beiträge für jede Teilnahme geladen

**Betroffen:** `Partei.java`, `Teilnahme.java`
**Empfehlung:** `FetchType.LAZY` verwenden (JPA-Default); bei Bedarf explizite `JOIN FETCH`-Queries im Repository.

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

---

## Behoben

### VALID-001 – Input-Validierung (@Valid) implementiert ✅ `2026-05-12`

`spring-boot-starter-validation` ergänzt. `@Valid` auf allen `@RequestBody`-Parametern.
Bean Validation Constraints auf allen Entities:
- `@NotBlank` auf String-Pflichtfeldern (vorname, name, bezeichnung, adresse, standort, beschreibung, bezeichnung)
- `@NotNull` auf Object/Enum/Datums-Pflichtfeldern und required Beziehungen (@ManyToOne, @OneToOne)

Pflichtfeld-Verletzungen liefern neu HTTP 400 (statt 500).
TCs TC-002, TC-005, TC-007, TC-015, TC-021, TC-027, TC-030 angepasst.
TC-012 und TC-023 (FK nicht gefunden) bleiben bei HTTP 500 — kein Validierungsfehler.

---

### DEPLOY-002 – Frontend: `localhost:8080` in allen Services ersetzt ✅ `2026-05-12`

Angular-Environments eingeführt (`src/environments/environment.ts` / `environment.prod.ts`).
- Dev: `apiUrl: 'http://localhost:8080'` — lokales Verhalten unverändert
- Prod: `apiUrl: ''` — relative URLs (`/api/...`), Nginx routet zum Backend
- Alle 11 Services auf `` `${environment.apiUrl}/api/...` `` umgestellt
- `angular.json`: `fileReplacements` für Production-Build ergänzt

---

### AUTH-001 – Keine Authentifizierung / Autorisierung ✅ `2026-05-09`

Spring Security 7.x + OAuth2 Resource Server implementiert (Branch `feature/auth-001`).
- `SecurityConfig.java` absichert alle `/api/**`-Endpunkte mit `hasRole('ORGANISATOR')` im `prod`-Profil
- JWT-Validierung via JWKS-Endpoint des IdP (`spring.security.oauth2.resourceserver.jwt.issuer-uri`)
- Dev/Test-Betrieb (kein `prod`-Profil): `permitAll()` — keine Teständerungen nötig
- `WebConfig.java` entfernt; CORS via `CorsConfigurationSource`-Bean in `SecurityConfig`

**Noch ausstehend (künftiges Feature):** Rolle `PARTEI` + datensatz-seitige Autorisierung via `@PreAuthorize`

---

### CORS-001 – CORS `allowedOrigins` hardcoded ✅ `2026-05-09`

Origin via Property `cors.allowed-origins` konfigurierbar (Branch `feature/auth-001`).
- Dev-Default: `http://localhost:4200` in `application.properties`
- Prod: `https://davidrossier.ch` in `application-prod.properties`

---

### DEPLOY-001 – Kein Spring-Profil für Production ✅ `2026-05-09`

`application-prod.properties` angelegt (Branch `feature/auth-001`).
- DB-Credentials via `${DB_URL}`, `${DB_USER}`, `${DB_PASSWORD}`
- JWT-Issuer-URI Platzhalter vorhanden (IdP-spezifisch eintragen)
- Start: `java -jar backend.jar --spring.profiles.active=prod`
