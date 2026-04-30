# Technische Schulden — Offene Punkte

> Stand: 2026-05-01. Quellen: SonarQube-Analyse, Clean-Code-Review.
> UC-spezifische Punkte sind in den jeweiligen `UC-*.md`-Open-Items erfasst.
> Architektur-/Infrastruktur-Übersicht → `specs/architecture.md` (Abschnitt "Bekannte technische Schulden").

---

## CRITICAL

### AUTH-001 – Keine Authentifizierung / Autorisierung

Alle `/api/**`-Endpunkte sind ohne Authentifizierung zugänglich. Spring Security ist nicht konfiguriert.
Jeder mit Netzwerkzugang kann Personen, Parteien, Einladungen, Abrechnungen und Zahlungsdaten lesen, schreiben und löschen.

**Betroffen:** Alle 11 Controller.
**Empfehlung:** Spring Security mit HTTP Basic Auth (für lokalen Betrieb) oder JWT-Token (für Netzwerkbetrieb) konfigurieren.

---

## MAJOR

### CORS-001 – CORS `allowedOrigins` hardcoded

`WebConfig.java:13`: `allowedOrigins("http://localhost:4200")` ist hardcoded.
Kein Profil-Support — für Production-Deployment nicht verwendbar.

**Empfehlung:** `@Value("${cors.allowed-origins:http://localhost:4200}")` mit Profil-spezifischen Properties.

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
