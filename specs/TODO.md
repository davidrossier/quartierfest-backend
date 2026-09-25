# Technische Schulden

> Stand: 2026-09-25. Quellen: SonarQube-Analyse, Clean-Code-Review, Deployment-Analyse, AUTH-002-Spec-Session (revidiert 2026-06-12: Eigenbau statt Auth0), Repo-Review Frontend+Backend 2026-07-06, Multi-Perspektiven-Review (BA/Architektur/Dev/Test/Security/UX/Data/DevOps) 2026-07-09, Lücken-Review TODO.md ↔ Code/Specs 2026-09-18.
> UC-spezifische Punkte sind in den jeweiligen `UC-*.md`-Open-Items erfasst.
> Architektur-/Infrastruktur-Übersicht → `specs/architecture.md` (Abschnitt "Bekannte technische Schulden").

---

## Konsolidierte Priorisierung (Review 2026-07-09)

**Vor dem nächsten Prod-Deployment (blockierend):**
1. **DB-001 + DB-002** — Flyway-Baseline inkl. fehlender Unique-Constraints und Geld-Präzision — ✅ behoben 2026-09-08 (Prod-Erstmigration: Runbook in `README.md`, Duplikat-Check `db/check/duplikate-vor-v2.sql` vorher ausführen)
2. **SEC-002** — Brute-Force-Drosselung auf `/api/auth/login` (internet-exponiert) — ✅ behoben 2026-09-17 (Caffeine-Zähler pro E-Mail/IP, 429; TC-045)

**Hoher Nutzen, geringer Aufwand (nächster Sprint):**
3. **ERROR-001** — `@RestControllerAdvice` (danach TC-012/TC-023 auf 404 korrigieren) — ✅ behoben 2026-07-09 (empirisch: 409 statt 404)
4. **REST-001** — POST-Upsert unterbinden, Frontend auf `update()` umstellen — ✅ behoben 2026-07-09 (Teilnahme-Pfad; die PUT-Endpunkte für UC-006/UC-012 folgten mit **REST-003**, ✅ 2026-09-25)
5. **CODE-001 + DEP-001** — Quick Wins (je < 1 h) — ✅ beide behoben 2026-07-09
6. **API-001** — Stufe 1 springdoc + generierte Frontend-Typen mit Drift-Check — ✅ behoben 2026-09-17; Stufe 2 DTO-Layer — ✅ behoben 2026-09-25 (Pläne: `specs/plans/API-001_Stufe-1_Plan.md`, `specs/plans/API-001_Stufe-2_Plan.md`)

**Mittelfristig:**
7. **CI-001** — E2E-Workflow (Nightly), Actuator-Health als Readiness (→ OPS-001) — ✅ behoben 2026-09-17 (Frontend `e2e.yml`, Backend `/actuator/health` + TC-047; OPS-001 Punkt 3 damit erledigt)
8. **BIZ-001** — UC-011-Berechnung ins Backend verlagern (fachlich wichtigste Lücke) inkl. `AbrechnungServiceTest` (TEST-003); UC-009-Endpunkt und Event-Filter (API-002)
9. **CI-002 + QUAL-001** — gesamte Qualitäts-Pipeline durchgehend prüfen (Linting → Unit → IT → Contract → E2E → Coverage → Traceability-Check in CI); QUAL-001 (angular-eslint, Prettier-Check, Dependabot, Coverage als CI-Artifact, `playwright-report/` aus Git nehmen) geht darin auf
10. **REFACT-001/002 + TEST-001/002** — Basisklassen/`MeldungService`; Meldungs-UX/a11y (UX-001) gleich mitlösen. REST-002, REST-003 und CODE-002 sind mit API-001 Stufe 2 erledigt (2026-09-25); REFACT-001 nach dem DTO-Layer neu beurteilen
11. **OPS-001, DATA-001, SEC-003** — Deployment (inkl. Nginx-`X-Forwarded-For` für SEC-002)/Backups dokumentieren, Löschkonzept, Audit-Trail-Entscheid
12. **TEST-004** — Frontend-Unit-Tests für `shared/sortierung.ts` und `computed`-Ableitungen; günstig, aber ohne Blocker-Charakter, deshalb zuletzt

---

## MAJOR

### BIZ-001 – UC-011: Abrechnungs-Berechnung nur im Frontend, Backend rechnet und validiert nicht (fachlich wichtigste Lücke) *(Review 2026-07-09, präzisiert 2026-09-18)*

Die Berechnung existiert, aber ausschliesslich clientseitig: `AbrechnungenVerwaltungComponent.abrechnungenErstellen()` rechnet `totalKonsumation = Σ(anzahl × preis)` und `anteilAllgemeinkosten = (Σ Allgemeinausgaben ÷ Σ anzahlPersonenEffektiv) × anzahlPersonenEffektiv` (Verteilschlüssel **pro effektive Person**, so in UC-011 Schritt 2–3 spezifiziert) und schickt die drei Beträge als fertige Werte an `POST /api/abrechnungen`. Das Backend (`AbrechnungService`) ist reine Repository-Delegation: Es prüft weder `totalBetrag == anteilAllgemeinkosten + totalKonsumation` noch die Herleitung der Einzelbeträge — inkonsistente oder manipulierte Beträge werden unverändert gespeichert. Die Abrechnung ist aber der Kern des Nutzenversprechens der Nachbearbeitung; die Rechenregel gehört als Fachlogik ins Backend, testbar und unabhängig vom UI. Bisher nur als Traceability-Lücke geführt (UC-011, UC-009); hier konsolidiert, weil die fachliche Priorität über mehreren technischen MAJORs liegt.

**Empfehlung:**
- `AbrechnungService.berechneFuerEvent(eventId)`: Rechenregel 1:1 aus dem Frontend übernehmen (Verteilschlüssel pro effektive Person ist entschieden, siehe UC-011 Schritt 2–3), `totalBetrag` als Summe; Frontend anschliessend auf den Endpunkt umstellen und die eigene Berechnung entfernen.
- Endpunkt z.B. `POST /api/events/{id}/abrechnungen/berechnen` (erstellt/aktualisiert alle Abrechnungen des Events); manuelle Übersteuerung via bestehendem POST erhalten, dort aber mindestens `totalBetrag == anteil + konsumation` validieren (400).
- Rundung: Frontend rundet heute kaufmännisch auf 0.01 (`rundeAufRappen()`, Name irreführend). Ob auf 0.05 (Schweizer Rappenrundung) gerundet werden soll, ist **offen** — mit dem Organisator klären und im UC-011 festhalten; im Backend `BigDecimal` mit `RoundingMode.HALF_UP`.
- Unit-Tests für den `AbrechnungService` gleichzeitig einführen (→ TEST-003).
- UC-009 analog: `GET /api/events/{id}/konsumationsliste` schliesst die zweite Teilimplementierung (Event-Filter generell → API-002).

---

### CI-002 – Gesamte Qualitäts-Pipeline überprüfen: Linting → Teststufen → Traceability *(2026-09-18)*

Die Pipeline ist über mehrere Reviews hinweg stückweise gewachsen (DEPLOY-003, CI-001, API-001, QUAL-001) und wurde nie als Ganzes geprüft. Es ist nicht belegt, dass jede Qualitätsstufe (a) existiert, (b) in der CI tatsächlich läuft, (c) bei Verstoss rot wird und (d) mit den Specs verknüpft ist. Ist-Stand der Stufen:

| Stufe | Backend | Frontend | Läuft in CI? | Bricht bei Verstoss? |
|---|---|---|---|---|
| Formatierung | — (kein Spotless/Checkstyle) | Prettier konfiguriert (`.prettierrc`) | nein | nein |
| Linting / statische Analyse | — (SonarQube einmalig 2026-05-01, `sonar-maven-plugin` ungenutzt) | kein ESLint | nein | nein |
| Unit-Tests | `./mvnw verify` (Surefire) | Vitest (`npm test`) | ja (`ci.yml`) | ja |
| Integrationstests | `./mvnw verify` (Failsafe, `*IT`) gegen PostgreSQL-Service | — | ja (`ci.yml`) | ja |
| Contract-Test | `OpenApiContractIT` (TC-046) | `npm run api:check` gegen Backend-`main` | ja | ja |
| Security-Matrix | `SecurityMatrixIT` (TC-040/047) | — | ja | ja |
| E2E | — | Playwright (`e2e.yml`, nächtlich) | nur nightly/manuell | ja, aber nicht PR-blockierend |
| Coverage | JaCoCo-Reports lokal | — | nein (nicht publiziert) | nein (keine Schwelle) |
| Traceability UC ↔ Impl ↔ Test | `/traceability-manager` (manuell), Matrizen in `architecture.md`/`testdesign.md` | `e2e/TRACEABILITY.md` («automatisch generiert») | nein | nein |

**Konkrete Befunde beim Aufnehmen:** Die Traceability-Matrix in `architecture.md` stand bis 2026-09-18 auf dem Stand 2026-06-12 (TC-041..044 fehlten) — niemand merkt, wenn sie veraltet. Das Skill-Verzeichnis heisst `.claude/commands/tracebiliy-manager/` (Tippfehler), während CLAUDE.md `/traceability-manager` dokumentiert; ob der Skill unter dem dokumentierten Namen auflösbar ist, ist zu prüfen. `e2e/TRACEABILITY.md` sagt «automatisch generiert», ein Generator ist im Repo nicht auffindbar.

**Empfehlung (Reihenfolge = Pipeline-Reihenfolge):**
1. **Linting/Format als erste Stufe:** Frontend `ng add angular-eslint` + `prettier --check`; Backend Spotless (google-java-format oder Eclipse-Formatter) mit `spotless:check` im `verify`; beides als CI-Step vor den Tests (→ QUAL-001).
2. **Teststufen explizit machen:** Backend-CI in `test` (Unit) und `integration-test` (Failsafe) trennen, damit ein Fehlschlag der Stufe zuordenbar ist; JaCoCo-Report als Artifact und Mindest-Coverage (`jacoco:check`) für die Service-Schicht, sobald BIZ-001 Logik dorthin bringt; Vitest `--coverage` analog.
3. **E2E-Stufe absichern:** Nightly-Fehlschlag muss sichtbar werden (GitHub-Notification oder Badge im README); prüfen, ob ein reduzierter Smoke-Lauf (Login + ein UC) pro PR tragbar ist.
4. **Traceability als prüfbare Stufe:** Skript (z.B. `specs/check-traceability.sh` oder Maven-`exec`) das (a) jede `TC-xxx` aus `testdesign.md` in genau einer IT-Methode findet, (b) jede `@DisplayName("TC-…")` in `testdesign.md` vorkommt, (c) jede `UC-xxx` in `architecture.md`-Matrix, `use-cases_overview.md` und `e2e/TRACEABILITY.md` konsistent gelistet ist; in beiden CIs laufen lassen und bei Abweichung rot. Skill-Verzeichnis umbenennen und `/traceability-manager` einmal vollständig laufen lassen, um den Ausgangszustand herzustellen.
5. **Dependabot** in beiden Repos (→ QUAL-001), damit die Pipeline auch Abhängigkeits-Drift meldet.
6. Ergebnis als Abschnitt «Qualitäts-Pipeline» in `architecture.md` dokumentieren: welche Stufe wo läuft, was sie blockiert, wie man sie lokal reproduziert.

---

### OPS-001 – Deployment-Prozess undokumentiert, keine Backup-Strategie, kein Health-Endpoint *(Review 2026-07-09)*

Es gibt kein Dockerfile, kein Deploy-Skript und keine Beschreibung, wie Jar + Angular-Build hinter Nginx auf `davidrossier.ch` landen — das Wissen existiert nur im Kopf des Betreibers. Für die Prod-DB (Personen- und Zahlungsdaten des Vereins) ist keine Backup-Strategie dokumentiert. ~~`spring-boot-starter-actuator` fehlt, daher kein `/actuator/health` für Readiness-Checks (betrifft auch CI-001).~~ → Punkt 3 erledigt 2026-09-17.

**Offene Frage aus SEC-002 (2026-09-17):** Ob und welcher Reverse-Proxy vor dem Backend läuft, ist nicht dokumentiert. Die IP-Drosselung (`LoginDrosselung`) zählt pro `request.getRemoteAddr()`; hinter Nginx ist das ohne weiteres `127.0.0.1` für **alle** Clients — das IP-Limit (20 Fehlversuche) würde dann global wirken und alle Haushalte gemeinsam sperren. `server.forward-headers-strategy=native` ist in `application-prod.properties` gesetzt und akzeptiert `X-Forwarded-For` von Loopback/privaten Netzen, Nginx muss den Header aber setzen.

**Empfehlung:**
1. `DEPLOYMENT.md` im Backend-Repo: Build-Schritte, benötigte Umgebungsvariablen (`AUTH_JWT_SECRET`, `AUTH_INITIAL_ADMIN_*`, `DB_*`), Nginx-Routing (`/api` → 8080, Rest → Angular-`dist/`), Startkommando mit `--spring.profiles.active=prod`. **Pflicht im Nginx-Block:** `proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;` (und `X-Forwarded-Proto`), sonst greift die SEC-002-IP-Drosselung falsch. Nach dem Deployment verifizieren: zwei Fehl-Logins von verschiedenen Clients dürfen sich nicht gegenseitig hochzählen (WARN-Log zeigt die IP).
2. Backups: täglicher `pg_dump` per Cron + gelegentlicher Restore-Test; Aufbewahrung dokumentieren (→ DATA-001)
3. ✅ (2026-09-17) `spring-boot-starter-actuator`; `management.endpoints.web.exposure.include=health`, `show-details=never`; `SecurityConfig` gibt nur `GET /actuator/health` frei, alle übrigen Actuator-Pfade bleiben `authenticated()` (401) und sind zudem nicht exponiert (404 mit Token) — TC-047 in `SecurityMatrixIT`
4. Optional: Dockerfile + Compose (App + PostgreSQL + Nginx) für reproduzierbares Deployment

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

**Nachtrag 2026-09-25:** Seit API-001 Stufe 2 unterscheiden sich die Controller und Services durch Records, Referenz-Auflösung und Whitelist-PUTs; der identische Boilerplate ist deutlich kleiner. Vor der Umsetzung neu beurteilen, ob sich eine Basisklasse noch lohnt.

---

### TEST-001 – IT-Test-Boilerplate ohne Basisklasse

Das `setUp()`-Muster (zwei `RestTemplate`, no-op `ResponseErrorHandler`, JSON-Header) ist in 17 der 18 IT-Klassen identisch kopiert (alle Domain-ITs; `OpenApiContractIT` nutzt das Muster nicht); `tryDelete()`/Fixture-Cleanup zusätzlich in 14 davon (~400+ Zeilen Duplikat-Code).

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
`@SuppressWarnings("unchecked")` ist in den 17 IT-Klassen auf fast jeder Testmethode notwendig.

**Empfehlung:** Typisierte Response-Records oder eigene Assertion-Helpers einführen (z.B. `assertField(response, "id")`). Alternativ akzeptieren (bei simpler CRUD-Struktur vertretbar).

---

### TEST-003 – Keine Unit-Tests für 10 Services

Drei Services haben Mockito-Unit-Tests (`ParteiServiceTest`, `BenutzerServiceTest`, `AuthServiceTest`).
Die übrigen 10 Services haben 0% Unit-Test-Abdeckung und werden nur durch IT-Tests abgedeckt.

**Einschätzung:** Da die Services fast ausschliesslich 1:1 an das Repository delegieren, ist der Mehrwert von Unit-Tests gering. Sinnvoll wäre ein Unit-Test für `AbrechnungService`, sobald dort Berechnungslogik (UC-011) implementiert wird.

---

### TEST-004 – Frontend: kaum Unit-Tests ausserhalb Auth

Nur 7 Spec-Dateien (~31 Tests), fast ausschliesslich `auth/` + `app`. Die 20+ Feature-Komponenten und 11 HTTP-Services haben keine Unit-Tests — insbesondere die `computed`-Logik (Event-Filterung, `einladungenOhneTeilnahme`, Sortierung via `shared/sortierung.ts`) wäre günstig testbar.

**Empfehlung:** Vitest-Specs priorisiert für (1) `shared/sortierung.ts`, (2) `computed`-Ableitungen der Verwaltungs-Komponenten (Signal setzen → Ableitung prüfen, ohne DOM), (3) Services via `provideHttpClientTesting`. Ziel: Kernlogik abgedeckt, nicht Template-Details.

---

### QUAL-001 – Keine statische Analyse / kein Lint in der CI

SonarQube lief einmalig manuell (2026-05-01). Frontend hat kein ESLint; Prettier wird in CI nicht geprüft. Backend: JaCoCo ist in der `pom.xml` konfiguriert (Unit- und IT-Report unter `target/site/jacoco*`), die Reports werden aber in der CI weder hochgeladen noch ausgewertet; Frontend hat keine Coverage. Kein Dependency-Update-Bot in beiden Repos.

**Beifang (2026-09-18):** Im Frontend-Repo sind `playwright-report/index.html` und `test-results/.last-run.json` versioniert und nicht in der `.gitignore` — jeder lokale E2E-Lauf macht den Working Tree schmutzig und lädt zum versehentlichen Commit ein (die CI lädt den Report bereits als Artifact hoch, im Repo hat er nichts verloren).

**Beifang (2026-09-25, API-001 Stufe 2):** Die Playwright-Dateien (`e2e/**/*.ts`) werden von keiner Stufe typgeprüft — Playwright transpiliert nur. Die E2E-Hilfen nutzen seit Stufe 2 die generierten Contract-Typen (`satisfies XxxRequest`); das schützt aber erst, wenn ein `tsc --noEmit` über `e2e/` in der CI läuft. Ein lokaler Lauf zeigt einen bestehenden Typfehler in `e2e/specs/UC-005_Teilnahme-Verwalten.spec.ts:108` und fehlende Node-Typen in `playwright.config.ts`.

**Empfehlung:** Frontend: `ng add angular-eslint` + `npx prettier --check .` als CI-Steps; `playwright-report/` und `test-results/` in die `.gitignore` aufnehmen und per `git rm --cached` aus dem Index entfernen. Beide Repos: Dependabot aktivieren (`.github/dependabot.yml` für npm bzw. maven + github-actions). Optional: SonarCloud (gratis für öffentliche Repos) oder `-Dspotbugs` in die Backend-CI; JaCoCo-Reports (`actions/upload-artifact`) und Vitest-Coverage (`--coverage`) als CI-Artifact.

---

### REFACT-002 – Frontend: dupliziertes CRUD-/Meldungs-Muster in allen Verwaltungs-Komponenten

Das Muster `ladevorgang/fehler/erfolg`-Signals + `setTimeout(3–4s)` zum Ausblenden + `laden()`-Reload ist in 10 Verwaltungs-Komponenten kopiert (Stand 2026-09-18: 10 Komponenten-Dateien mit `setTimeout`; Pendant zu REFACT-001 im Backend). Querschnittsänderungen am Meldungsverhalten erfordern 10 gleichlautende Edits.

**Empfehlung:** (1) `MeldungService` (oder Composable `createMeldungen()`) für Erfolg/Fehler inkl. Auto-Ausblenden extrahieren — dabei die Meldungs-UX/a11y-Punkte aus UX-001 gleich mitlösen (ein Refactoring, ein Verhalten); (2) generischen `CrudService<T, P>` als Basis der 11 HTTP-Services einführen. Komponenten-Templates bewusst individuell lassen.

---

### UX-001 – Meldungs-UX, a11y und Mobile-Tauglichkeit *(Review 2026-07-09)*

Vier zusammenhängende Befunde:
1. Erfolgs-/Fehlermeldungen verschwinden nach 3–4 s (`setTimeout`) — für die Zielgruppe (Quartierverein, breite Altersspanne) zu schnell, und ohne `aria-live` für Screenreader unsichtbar.
2. Native `confirm()`/`prompt()`-Dialoge; insbesondere zeigt `window.prompt` beim Passwort-Reset (`BenutzerVerwaltungComponent`) die Eingabe **unmaskiert** — UX- und Security-Problem zugleich.
3. ~~Fehlermeldungen bei 500ern generisch («konnte nicht gespeichert werden»)~~ — durch ERROR-001 behoben (2026-07-09): das Backend liefert jetzt einheitlich `{status, message}`, `err.error?.message` greift bei allen Fehlerstatus.
4. Die Konsumationserfassung (UC-010, Matrix Teilnahmen × Angebote) passiert real am Fest auf dem Smartphone — responsive Verhalten ist ungeprüft.

**Empfehlung:** (1)+(2) im Zuge von REFACT-002 lösen: `MeldungService` rendert in eine zentrale Region mit `role="status"`/`aria-live="polite"`, Erfolgsmeldungen ≥ 8 s oder abweisbar, Fehler bleiben stehen; Passwort-Reset als Inline-Formular mit `<input type="password">` statt `window.prompt`. (4) Mobile-Durchstich von UC-010 vor dem nächsten Fest (Playwright mit `devices['iPhone 15']`-Projekt wäre der billigste dauerhafte Check).

---

### SEC-003 – Kein Audit-Trail für finanzrelevante Änderungen *(Review 2026-07-09)*

Abrechnungen, Zahlungen und Mahnungen sind ohne Nachvollziehbarkeit änder- und löschbar (wer, wann). Für die Vereinsrevision potenziell relevant.

**Empfehlung:** Entweder bewusst als Nicht-Anforderung hier dokumentieren (bei einem Quartierverein vertretbar) — oder günstig nachrüsten: Spring Data Auditing (`@EnableJpaAuditing`, `@CreatedDate`/`@LastModifiedDate`/`@CreatedBy`/`@LastModifiedBy` als `@Embeddable`/`@MappedSuperclass` auf `Abrechnung`, `Zahlung`, `Mahnung`; `AuditorAware<String>` liest die E-Mail aus dem JWT). Neue Spalten via Flyway-Migration (setzt DB-001 voraus).

---

### DATA-001 – Kein Löschkonzept für Personendaten (revDSG) *(Review 2026-07-09)*

Das System speichert Namen, Adressen, Telefonnummern und Zahlungsdaten von Quartierbewohnern unbefristet; eine Aufbewahrungs-/Löschregel ist nirgends spezifiziert.

**Befund zu den Kaskaden (2026-09-18):** Keine Entity trägt `cascade`/`orphanRemoval`. Das Löschen einer Partei mit Einladungen (oder eines Events mit Einladungen/Angeboten/Ausgaben) scheitert mit 409 «wird noch verwendet» (ERROR-001). Manuelles Löschen heisst deshalb heute: Zahlungen/Mahnungen → Abrechnungen → Konsumationen → Teilnahmen → Einladungen → Partei/Personen, jeweils über die entsprechende Maske. Für ein einzelnes Wegzugs-Szenario zumutbar, für die Bereinigung ganzer Event-Jahrgänge nicht.

**Empfehlung:** Mit dem Organisator eine einfache Betriebsregel festlegen und dokumentieren (z.B. «Event-Daten inkl. Konsumationen/Abrechnungen x Jahre nach dem Event löschen; Stammdaten von Personen/Parteien beim Wegzug entfernen»). Technisch reicht vorerst manuelles Löschen über die bestehende UI in FK-Reihenfolge (siehe oben); sobald die Regel steht, entweder gezielte Kaskaden (z.B. `Event` → Einladungen/Angebote/Ausgaben) oder ein `DELETE /api/events/{id}?mitDaten=true` einführen. Als Abschnitt in `DEPLOYMENT.md` (→ OPS-001) oder eigenem Betriebs-Dokument festhalten.

---

### API-002 – Listen-Endpunkte ohne Event-Filter *(testdesign.md-Open-Item, als eigener Eintrag 2026-09-18)*

Alle Listen-Endpunkte (`GET /api/einladungen`, `/api/teilnahmen`, `/api/konsumationsangebote`, `/api/konsumationen`, `/api/allgemeinausgaben`, `/api/abrechnungen`, `/api/zahlungen`, `/api/mahnungen`) liefern sämtliche Datensätze über alle Events; jede Verwaltungs-Komponente filtert clientseitig über den `EventKontextService`. Das ist funktional korrekt, skaliert aber mit jedem weiteren Festjahr linear (Payload-Grösse, PERF-001-Rest) und ist der Grund, warum UC-009 als «teilweise implementiert» gilt. `KonsumationslisteErstellenIT` (TC-018/TC-019) hält die Lücke als TODO-Kommentar fest.

**Empfehlung:** Optionaler Query-Parameter `?eventId={id}` auf den event-gebundenen Listen (Repository-Methoden `findByEventId` bzw. Fetch-Join über `teilnahme.einladung.event`), Frontend-Services reichen die aktuelle Event-Auswahl durch. Die Response-Typen sind seit API-001 Stufe 2 (2026-09-25) vorbereitet: alle event-gebundenen Antworten tragen die Event-Referenz, der Filter ist rein additiv. `GET /api/events/{id}/konsumationsliste` aus BIZ-001 wird damit zum Spezialfall.

---

## Behoben

### API-001 – API-Contract nur implizit (kein OpenAPI, kein DTO-Layer) ✅ Stufe 1 `2026-09-17`, Stufe 2 `2026-09-25`

JPA-Entities waren direkt der API-Contract (inkl. verschachtelter Beziehungen wie `Teilnahme → Einladung → Partei → Personen`); die TypeScript-Interfaces im Frontend wurden von Hand synchron gehalten, Drift fiel erst im E2E auf. Folgeproblem: verschachtelte Payloads mit Lazy-Loading bei der Serialisierung (PERF-001-Rest).

1. **Stufe 1 (2026-09-17):** springdoc 3.1.1, Contract versioniert in `specs/openapi.json` (TC-046), Frontend generiert `schema.d.ts` und prüft in der CI auf Drift. Plan: `specs/plans/API-001_Stufe-1_Plan.md`.
2. **Stufe 2 (2026-09-25):** DTO-Layer. Plan und Umsetzungsnotizen: `specs/plans/API-001_Stufe-2_Plan.md`.
   - Controller nehmen `*Request`-Records und liefern `*Response`-Records; Referenzen als `*Kurz`-Records ohne Collections, Pfade fürs Frontend unverändert. Requests referenzieren über flache IDs.
   - Unbekannte Referenz → 400 (vorher 409 via FK), unbekannte JSON-Felder → 400 mit Feldname, `null`-Felder werden weggelassen; `OpenApiCustomizer` markiert Response-Pflichtfelder, `ApiError` steht als `4XX` in der Spec.
   - Alle Services transaktional, alle To-one-Beziehungen lazy, `open-in-view=false`, Fetch-Joins in den Listen-Abfragen — TC-054 misst höchstens zwei Statements pro Listen-Endpunkt (vorher bis 35).
   - Frontend: alle `*.model.ts` sind Schema-Aliase; `Persisted<>` und die handgeschriebenen Payload-Typen sind entfernt.
   - Beifang: CODE-002, REST-002, REST-003 (siehe unten).

### REST-003 – Kein PUT für Einladung und Abrechnung: UC-006/UC-012 hingen am POST-Upsert ✅ `2026-09-25`

REST-001 hatte den POST-Upsert nur auf dem Teilnahme-Pfad unterbunden. **Bei der Planung von API-001 Stufe 2 erweitert:** Auch Konsumation (UC-010, Matrix), Konsumationsangebot (UC-008) und Allgemeinausgabe (UC-007) wurden im Frontend per POST mit `id` aktualisiert.

**Umsetzung (API-001 Stufe 2):** `PUT` auf Einladung (Whitelist inkl. der Rückmeldungsfelder aus UC-004, nicht nur `status`/`bestaetigungVersendet` wie ursprünglich empfohlen; Event/Partei fix), Abrechnung (Kanal, Zustelldatum, Beträge; Teilnahme fix), Konsumation (nur `anzahl`), Konsumationsangebot und Allgemeinausgabe (voller Request). POST mit `id` → 400 auf allen Ressourcen (TC-053), PUT auf unbekannte id → 404 (TC-052). TC-013 und TC-032 auf PUT umgeschrieben, neu TC-048..TC-051; Frontend-Services mit `update()`, E2E-Tests für das Bearbeiten in UC-007/008/010.

### REST-002 – PUT wirkte als Upsert: stilles Anlegen bei nicht-existenter id ✅ `2026-09-25`

`PersonController`, `ParteiController` und `EventController` implementierten `update()` als `setId(id)` + `save()`.

**Umsetzung (API-001 Stufe 2):** Services laden die Entity über `Referenzen.laden()` → 404 «… nicht gefunden.» und übertragen die Felder aus dem Request-Record. Gilt für alle PUT-Endpunkte (TC-052).

### CODE-002 – Lombok `@Data` auf den JPA-Entities ✅ `2026-09-25`

`@Data` generierte `equals`/`hashCode`/`toString` über alle Felder inkl. Kollektionen (Lazy-Loading-Falle in `toString`, instabiler `hashCode`).

**Umsetzung (API-001 Stufe 2):** `@Getter`/`@Setter` auf den zwölf Entities, `equals`/`hashCode` weggelassen (Objektidentität); das Embeddable `TeilnahmeBuffetBeitrag` bleibt Wertobjekt mit `@Data`. Kein Test stützte sich auf Entity-Gleichheit.

### CI-001 – Playwright-E2E läuft nicht in CI ✅ `2026-09-17`

Die E2E-Suite (UC-001..016, wertvollste Absicherung des Frontend↔Backend-Zusammenspiels) lief nur lokal. Contract- oder Integrationsfehler zwischen den Repos wurden von keiner Pipeline erkannt.

**Umsetzung:** `quartierfest-frontend/.github/workflows/e2e.yml` — Trigger `schedule` (täglich 03:00 UTC) und `workflow_dispatch` mit Input `backend_ref` (Default `main`, erlaubt den Lauf gegen einen Backend-Feature-Branch vor dessen Merge). Ablauf: PostgreSQL-16-Service → Backend-Checkout nach `backend/` → `./mvnw spring-boot:run` (dev-Profil) und `npm start` im Hintergrund → Warten auf `GET /actuator/health` = `UP` (OPS-001 Punkt 3) bzw. Port 4200 → `npx playwright install chromium --with-deps` → `npm run e2e` (Playwright-`retries: 2` in CI) → `playwright-report/` immer, `backend.log`/`frontend.log` bei Fehler als Artifact. Bewusst nicht pro Push/PR: Laufzeit (Maven-Build + Browser) und Kaltstart-Flakiness (UC-004/UC-015).

### SEC-002 – Brute-Force-Drosselung auf `POST /api/auth/login` ✅ `2026-09-17`

Der Login-Endpunkt war `permitAll()` und internet-exponiert ohne jede Drosselung (Credential-Stuffing/Brute-Force unbeschränkt möglich).

- `auth/LoginDrosselung` (`@Component`): In-Memory-Zähler (Caffeine, `expireAfterWrite`) pro E-Mail (normalisiert) **und** pro Client-IP; Limits und Sperrdauer via `auth.drosselung.max-fehlversuche-email=5`, `max-fehlversuche-ip=20`, `sperre-minuten=15` (0 = Prüfung aus). Jeder Fehlversuch verlängert die Sperre, erfolgreicher Login löscht beide Zähler. Single-Instance-Annahme wie in der Empfehlung.
- `AuthService.login(email, passwort, clientIp)`: gesperrt → 429 «Zu viele Fehlversuche. Bitte später erneut versuchen.» **vor** dem BCrypt-Vergleich; Fehlversuche mit WARN geloggt (E-Mail + IP, nie Passwort). `AuthController` liefert `request.getRemoteAddr()`.
- IP-Limit bewusst höher als E-Mail-Limit: mehrere Haushalte hinter einer NAT-IP und die ITs (alle von 127.0.0.1) sperren sich nicht gegenseitig.
- Prod: `server.forward-headers-strategy=native` — Tomcat übernimmt `X-Forwarded-For` nur von Proxys aus privaten Netzen; ob ein Reverse-Proxy vor dem Backend läuft, ist noch unbekannt (→ OPS-001), die Einstellung ist in beiden Fällen korrekt (kein Spoofing von öffentlichen Clients).
- Tests: `LoginDrosselungTest` (gestellte Uhr), `AuthServiceTest` (+2), `AuthControllerTest` (+1), TC-045 in `BenutzerAnmeldenIT` (5× 401 → 429, auch mit korrektem Passwort; Admin von derselben IP weiterhin anmeldbar; `@AfterEach` setzt die Zähler zurück). Frontend: `LoginComponent` mappt 429 auf «Zu viele Fehlversuche. Bitte versuchen Sie es in 15 Minuten erneut.»
- Nicht gelöst: Sperre überlebt keinen Neustart (In-Memory) — für den Prod-Betrieb bewusst akzeptiert.

### DB-001 – Flyway-Migrationen statt `ddl-auto=update` ✅ `2026-09-08`

`spring.jpa.hibernate.ddl-auto=update` galt in allen Profilen inkl. `prod` — Hibernate migrierte das Prod-Schema still bei jedem Start.

- `spring-boot-starter-flyway` + `flyway-database-postgresql` (Spring Boot 4 liefert die Flyway-Autokonfiguration nur noch über den Starter — `flyway-core` allein wird nicht aktiviert).
- `V1__baseline.sql`: Ist-Stand des Hibernate-Schemas (aus `pg_dump` der Dev-DB, mit sprechenden Constraint-Namen). `spring.flyway.baseline-on-migrate=true` + `baseline-version=1`: bestehende DBs (Prod, lokale Dev-DB) werden beim ersten Start auf V1 baselined und laufen ab V2 normal; leere DBs (CI) bauen V1+V2 von null auf.
- `ddl-auto=validate` in allen Profilen — jede Entity-Änderung braucht ein neues `V<n>__*.sql` im selben PR.
- Verifiziert: `./mvnw verify` gegen die bestehende Dev-DB (Baseline + V2) und gegen eine leere DB (V1 + V2) — beide grün (64 Unit-, 42 IT-Methoden).

### DB-002 – Unique-Constraints und Geld-Präzision ✅ `2026-09-08`

Befund beim Umsetzen: Hibernate hatte für die `@OneToOne`-Beziehungen (`teilnahme.einladung_id`, `abrechnung.teilnahme_id`) bereits Unique-Constraints mit Hash-Namen angelegt — tatsächlich fehlte nur `einladung(event_id, partei_id)`; die Geldfelder waren `numeric(38,2)`.

- `V2__db002_unique_constraints_geldpraezision.sql`: `uk_einladung_event_partei` (neu), `uk_teilnahme_einladung`, `uk_abrechnung_teilnahme`, `uk_benutzer_email` (bestehende Hibernate-Constraints werden umbenannt, fehlende angelegt — idempotent gegenüber Prod- und V1-Schema); alle Geldbeträge auf `numeric(10,2)`.
- Entities: `@Table(uniqueConstraints = …)` und `@Column(precision = 10, scale = 2)` nachgeführt; Entity-TODO-Kommentare entfernt.
- `GlobalExceptionHandler`: Unique-Verletzung (`ConstraintViolationException.ConstraintKind.UNIQUE`) → 409 «Datensatz existiert bereits.», FK-Verletzung → 409 wie bisher.
- Neue ITs TC-042 (Duplikat-Einladung), TC-043 (Duplikat-Teilnahme), TC-044 (Duplikat-Abrechnung); TC-022 prüft die Betragspräzision. UC-004 E1 und das UC-011-Open-Item sind damit geschlossen.
- Frontend-Beifang: `AbrechnungenVerwaltungComponent` rundet `anteil`/`konsumation` vor dem POST auf 2 Nachkommastellen, damit `totalBetrag = anteil + konsumation` auch nach der DB-Rundung stimmt (Rappenrundung auf 0.05 bleibt BIZ-001).
- **Prod-Erstmigration:** V2 schlägt bei Duplikaten im Bestand fehl (App startet nicht) — vorher `src/main/resources/db/check/duplikate-vor-v2.sql` ausführen; Runbook im `README.md`.

### REST-001 – Frontend aktualisiert Teilnahmen via POST-Upsert statt PUT ✅ `2026-07-09`

`TeilnahmenVerwaltungComponent.speichern()` sendete beim Bearbeiten `POST /api/teilnahmen` mit gesetzter `id` (JPA-`save()` wirkt als Upsert) statt den vorhandenen `PUT /api/teilnahmen/{id}` — der POST umging damit die UC-016-Whitelist-Semantik des PUT.

Behoben auf dem Teilnahme-Pfad: Frontend nutzt `teilnahmeService.update(id, dto)` mit `TeilnahmeUpdatePayload` (Whitelist ohne `einladung`; `id` aus `TeilnahmePayload` entfernt); Backend lehnt `POST /api/teilnahmen` mit gesetzter `id` mit 400 ab (Fehlerformat aus ERROR-001, Verweis auf den PUT). Neu TC-041 in `TeilnahmeVerwaltenIT` + Slice-Test in `TeilnahmeControllerTest`; Playwright UC-005/UC-016 lokal grün (6/6).

**Folgearbeit:** UC-006/UC-012 nutzen dasselbe Upsert-Muster (`bestaetigungVersendet`, `zustellungsDatum`), hatten aber keinen PUT-Endpunkt — ab 2026-09-18 als eigener Eintrag **REST-003** geführt, ✅ behoben 2026-09-25 mit API-001 Stufe 2.

---

### ERROR-001 – Kein globaler Exception-Handler (500 statt 404, kein einheitliches Fehler-JSON) ✅ `2026-07-09`

Referenzen auf nicht-existierende FK-IDs lieferten HTTP 500 (TC-012, TC-023); es gab kein einheitliches Fehler-JSON, das Frontend bekam bei 500ern nichts Brauchbares in `err.error?.message`.

`GlobalExceptionHandler` (`@RestControllerAdvice`, erbt von `ResponseEntityExceptionHandler`) eingeführt — einheitliches Format `{status, message}` für alle Fehlerpfade: `ResponseStatusException` behält Status + Reason (401/404/409 aus Auth-/Benutzer-/TeilnahmeService), `EntityNotFoundException`/`JpaObjectRetrievalFailureException` → 404, `DataIntegrityViolationException` → 409 mit generischer Meldung (keine DB-Details), `MethodArgumentNotValidException` → 400 mit Feldliste, Fallback → 500 generisch + ERROR-Log. `AccessDeniedException` aus `@PreAuthorize` wird durchgereicht (403 via Security-Kette, kein 500). Die von Spring MVC vorbehandelten Fälle (kaputtes JSON → 400, unbekannter Pfad → 404, 405/415) behalten via `handleExceptionInternal`-Override ihren Status im selben Format.

Empirischer Befund: Die FK-Verletzung wirft `DataIntegrityViolationException` → TC-012/TC-023 liefern **409** (nicht die ursprünglich vermuteten 404); Tests + `testdesign.md` entsprechend nachgeführt. Verifiziert per curl gegen die laufende App: 401-Login, 409-FK, 400-Validierung mit Feldliste, 400 bei kaputtem JSON, 404 — alle im Format `{status, message}`.

---

### DEP-001 – Ungenutzte `citrus-bom` in der `pom.xml` ✅ `2026-07-09`

Citrus 4.9.4 lag auf dem Test-Classpath, war aber dokumentiert inkompatibel mit Spring Framework 7.x und ungenutzt; zog Jackson 2.x in den Test-Scope und erzeugte so die `ObjectMapper`-Verwechslungsfalle (Jackson 2 vs. 3).

`citrus-bom` (dependencyManagement) sowie `citrus-spring`, `citrus-http` und `citrus-junit5` aus der `pom.xml` entfernt. CLAUDE.md zurückgebaut: Citrus-Zeile im Tech-Stack gestrichen, Jackson-Warnung abgeschwächt — `com.fasterxml.jackson.annotation.*` (`@JsonIgnore`, `@JsonProperty`) bleibt legitim im Einsatz, nur der Jackson-2-`ObjectMapper` ist als Falle vom Classpath verschwunden. Verifiziert: `./mvnw verify` grün; kein Test importiert `com.fasterxml.jackson.databind`.

---

### CODE-001 – Kyrillische Homoglyphen in zwei IT-Methodennamen ✅ `2026-07-09`

`BestaetigungVerwaltenIT.tc013_bestaetigungVersendetViаUpsert()` und `AbrechnungZustellenIT.tc032_zustellungsDatumViаUpsert()` enthielten ein kyrillisches «а» (U+0430) statt eines lateinischen «a» in «Viа» (kompilierte, brach aber Textsuche, `-Dtest`/`-Dit.test`-Filter und Copy-Paste).

Beide Methoden auf lateinisches «a» umbenannt (`...ViaUpsert`), die mitkopierten Einträge in der `testdesign.md`-Traceability-Tabelle (TC-013, TC-032) nachgeführt. Verifiziert: `grep -P '\x{0430}'` über `src/` leer; Methoden-Filter `-Dit.test='BestaetigungVerwaltenIT#tc013_bestaetigungVersendetViaUpsert'` greift wieder. Auf eine Checkstyle-Regel gegen Nicht-ASCII-Identifier wurde verzichtet (Einzelfall).

---

### SEC-001 – Security-Default fail-open → fail-closed invertiert ✅ `2026-07-09`

Die `@Profile`-Bedingungen in `SecurityConfig` wurden umgekehrt: gesicherte Chain (Autorisierungsmatrix) ist jetzt der Default (`@Profile("!dev")`), die offene `permitAll()`-Chain greift nur noch bei explizitem `dev`-Profil (`@Profile("dev")`) — `prod`, `security-test` und jeder Start ohne Profil sind fail-closed.

- **Lokale Entwicklung:** `spring-boot-maven-plugin` in der `pom.xml` setzt für `spring-boot:run` automatisch das `dev`-Profil — lokaler Workflow und Playwright-E2E unverändert. Das gepackte Jar bleibt fail-closed (bewusste Abweichung von der ursprünglichen Empfehlung «Eintrag in `application.properties`», die fail-open fürs Jar wieder eingeführt hätte). IDE-Start der Main-Klasse braucht das Profil in der Run-Config.
- **ITs:** Die 16 Nicht-Security-ITs tragen `@ActiveProfiles("dev")` (identisch → weiterhin ein gemeinsamer gecachter Context); `SecurityMatrixIT` bleibt auf `security-test`, `BackendApplicationTests` bleibt profilfrei und smoke-testet damit den fail-closed-Boot. CI (`./mvnw verify` ohne Profil) unverändert.
- **WARN-Log** beim Aktivieren der offenen Chain («Offene Security-Chain aktiv…»).

---

### DOCS-001 – Doku-Drift zwischen CLAUDE.md, README und architecture.md ✅ `2026-07-06`

Vollständiger Konsistenz-Check aller Docs/Specs gegen den Code; behobene Drifts:

- **CLAUDE.md (Backend):** Spec-Tabelle «TC-001..TC-033» → TC-040; «UC-014..016 ausstehend» → implementiert; Kommentar «Controller-Tests + ParteiServiceTest» → drei Service-Tests
- **architecture.md:** obere Security-Tabelle auf AUTH-002-Stand (PARTEI-Rolle, `security-test`-Profil); Fussnote «11 ControllerTest-Klassen» → 13, Service-Tests ergänzt; TEST-001-Zeile 13 → 17 IT-Klassen
- **README (Backend):** Testzahlen (43 → 62 Unit-Testmethoden, 31 → 38 IT-Methoden, TC-033 → TC-040, 13 → 17 IT-Klassen), «kein PostgreSQL nötig» bei `./mvnw test` korrigiert (Smoke-Test braucht DB), CORS-Abschnitt referenzierte entferntes `WebConfig.java` → `SecurityConfig`, Endpunkt-Tabelle um Auth/Benutzer/Teilnahme-PUT ergänzt, Spec-Tabelle «13 Use Cases» → 16
- **README (Frontend):** UC-Nummern der Feature-Liste korrigiert (Einladungen = UC-004, Teilnahmen = UC-005, Bestätigung = UC-006, Allgemeinausgaben = UC-007, Konsumationsangebote = UC-008 — vorher verschoben), Auth-Features (UC-014/015/016) und `npm run e2e` ergänzt, Endpunkt-Tabelle vervollständigt
- **e2e/TRACEABILITY.md:** Zeilen UC-014/015/016 ergänzt, UC-002-Szenarienzahl korrigiert (3 Happy, 3 Error)
- **testdesign.md:** «Alle 11 REST-Endpunkte» → 13 Ressourcen; Open Items (13 → 17 IT-Klassen, drei Service-Tests) nachgeführt

**Grundsatz (neu):** `specs/` ist die Quelle der Wahrheit; CLAUDE.md/README fassen zusammen und verlinken. Bei Test-/Endpunkt-Änderungen `/traceability-manager` laufen lassen — er schliesst die Tabellen in `architecture.md`, `testdesign.md` und `e2e/TRACEABILITY.md` ein.

---

### PERF-001 – FetchType.EAGER entfernt (N+1-Queries) ✅ `2026-07-06`

- `Partei.personen` und `Teilnahme.buffetBeitraege` auf `FetchType.LAZY` (JPA-Default) umgestellt
- `ParteiRepository.findAll()` und `TeilnahmeRepository.findAll()` mit `left join fetch` überschrieben — `GET /api/parteien` und `GET /api/teilnahmen` laden jetzt mit einer Query statt 1+N; `findEigeneAbStichtag` (UC-016) ebenfalls mit Fetch-Join
- API-Contract unverändert (OSIV bleibt aktiv, Collections werden weiterhin serialisiert)

**Bewusst offen geblieben:** Verschachtelte Payloads (z.B. `GET /api/einladungen` → `partei.personen`, `GET /api/abrechnungen` → `teilnahme.buffetBeitraege`) laden die Collections weiterhin pro Zeile — neu lazy bei der Serialisierung statt eager beim Query, gleiche Query-Anzahl wie vorher. Echte Behebung bräuchte DTOs oder `@EntityGraph` pro Endpunkt.

**Nachtrag 2026-09-25:** Mit API-001 Stufe 2 behoben — Referenzen ohne Collections, alle Beziehungen lazy, OSIV aus, Fetch-Joins; TC-054 sichert höchstens zwei Statements pro Listen-Endpunkt ab.

---

### DEPLOY-003 – CI/CD-Pipeline (GitHub Actions) eingerichtet ✅ `2026-07-06`

Beide Repos haben ein `.github/workflows/ci.yml` (Trigger: `push` auf `main` + `pull_request` gegen `main`):

- **Backend:** PostgreSQL-16-Service-Container (db=quartierfest, user=qfuser, pass=qfpass, mit `pg_isready`-Health-Check) + `./mvnw --batch-mode verify` — Unit-Tests und alle `*IT.java` laufen gegen die echte DB, keine Teständerungen nötig (Java 21 / Temurin, Maven-Cache).
- **Frontend:** `npm ci` → `npm test -- --watch=false` (Vitest-Unit-Tests) → `npm run build -- --configuration production` (Node 24, npm-Cache). Die Vitest-Tests wurden gegenüber der ursprünglichen Empfehlung (nur Build-Check) ergänzt; Playwright-E2E bleibt lokal (bräuchte Backend + DB im Frontend-Workflow).

---

### AUTH-002 – Login und Rollenverwaltung (Eigenbau) implementiert ✅ `2026-06-12`

UC-014/UC-015/UC-016 vollständig umgesetzt (Eigenbau-Entscheid vom 2026-06-12 statt Auth0).

**Backend:**
- Neue Domain `benutzer`: Entity (`email` unique, `passwortHash` BCrypt, `rolle`, optionaler Partei-FK), `GET/POST/DELETE /api/benutzer`, `PUT /api/benutzer/{id}/passwort`; Duplikat-E-Mail und letzter ORGANISATOR → 409; Bootstrap-ORGANISATOR via `auth.bootstrap.*` (ApplicationRunner)
- Neues Package `auth`: `POST /api/auth/login` stellt HS256-JWT aus (`JwtEncoder`, Claims `sub`/`email`/`rolle`, 12 h); falsche Credentials → 401 ohne Felddifferenzierung
- `SecurityConfig`: gesicherte Chain für `prod`/`security-test` (Login offen, `/api/benutzer/**` nur ORGANISATOR, PARTEI nur `GET /api/teilnahmen/meine` + `PUT /api/teilnahmen/{id}`, Rest ORGANISATOR); `JwtAuthenticationConverter` (`rolle` → `ROLE_*`); offene Chain verarbeitet Tokens trotzdem (Dev/ITs); `@EnableMethodSecurity`
- UC-016: `PUT /api/teilnahmen/{id}` mit Whitelist-DTO (`einladung` nie änderbar), `GET /api/teilnahmen/meine` (frühester zukünftiger Event), Ownership via `@PreAuthorize` + `TeilnahmeZugriff`-Bean
- `application-prod.properties`: `issuer-uri` ersetzt durch `AUTH_JWT_SECRET` / `AUTH_INITIAL_ADMIN_*`

**Frontend:**
- `auth/`: `AuthService` (Token in `sessionStorage`), funktionaler Interceptor (Bearer + 401→`/login`), `authGuard`/`roleGuard`, `LoginComponent`; rollenbasiertes Routing (ORGANISATOR → `/personen`, PARTEI → `/meine-teilnahme`); Nav rollenabhängig mit Logout
- `BenutzerVerwaltungComponent` (`/admin/benutzer`), `MeineTeilnahmeComponent` (`/meine-teilnahme`)

**Tests:** TC-034..TC-040 (BenutzerVerwaltenIT, BenutzerAnmeldenIT, TeilnahmeBestaetigenIT, SecurityMatrixIT mit `@ActiveProfiles("security-test")`); 19 neue Backend-Unit-Tests; 12 neue Vitest-Specs; Playwright-E2E UC-014/015/016 + Auto-Login-Fixture (`e2e/fixtures.ts`) für bestehende Specs.

**Bewusst offen geblieben** (UC-014/015 Open Items): ~~Brute-Force-Drosselung~~ (→ SEC-002, behoben 2026-09-17), Passwort-Selbstwechsel, Token-Blacklist bei Account-Löschung (Restgültigkeit max. 12 h akzeptiert).

---

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

~~**Noch ausstehend (künftiges Feature):** Rolle `PARTEI` + datensatz-seitige Autorisierung via `@PreAuthorize`~~ → umgesetzt mit AUTH-002 (2026-06-12)

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
