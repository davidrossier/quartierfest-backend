# Technische Schulden

> Stand: 2026-07-09. Quellen: SonarQube-Analyse, Clean-Code-Review, Deployment-Analyse, AUTH-002-Spec-Session (revidiert 2026-06-12: Eigenbau statt Auth0), Repo-Review Frontend+Backend 2026-07-06, Multi-Perspektiven-Review (BA/Architektur/Dev/Test/Security/UX/Data/DevOps) 2026-07-09.
> UC-spezifische Punkte sind in den jeweiligen `UC-*.md`-Open-Items erfasst.
> Architektur-/Infrastruktur-Übersicht → `specs/architecture.md` (Abschnitt "Bekannte technische Schulden").

---

## Konsolidierte Priorisierung (Review 2026-07-09)

**Vor dem nächsten Prod-Deployment (blockierend):**
1. **DB-001 + DB-002** — Flyway-Baseline inkl. fehlender Unique-Constraints und Geld-Präzision
2. **SEC-002** — Brute-Force-Drosselung auf `/api/auth/login` (internet-exponiert)

**Hoher Nutzen, geringer Aufwand (nächster Sprint):**
3. **ERROR-001** — `@RestControllerAdvice` (danach TC-012/TC-023 auf 404 korrigieren)
4. **REST-001** — POST-Upsert unterbinden, Frontend auf `update()` umstellen
5. **CODE-001 + DEP-001** — Quick Wins (je < 1 h)
6. **API-001 Stufe 1** — springdoc + generierte Frontend-Typen mit Drift-Check

**Mittelfristig:**
7. **CI-001** — E2E-Workflow (Nightly), Actuator-Health als Readiness (→ OPS-001)
8. **BIZ-001** — UC-011-Berechnungslogik (fachlich wichtigste Lücke) inkl. `AbrechnungServiceTest`; UC-009-Endpunkt
9. **QUAL-001** — angular-eslint, Prettier-Check, Dependabot, Coverage
10. **REFACT-001/002 + TEST-001/002** — Basisklassen/`MeldungService`; Meldungs-UX/a11y (UX-001) gleich mitlösen
11. **OPS-001, DATA-001, SEC-003** — Deployment/Backups dokumentieren, Löschkonzept, Audit-Trail-Entscheid

---

## MAJOR

### DB-001 – `ddl-auto=update` auch in Production, keine Schema-Migrationen

`spring.jpa.hibernate.ddl-auto=update` steht in `application.properties` und wird von `application-prod.properties` **nicht** überschrieben — Hibernate migriert das Prod-Schema still bei jedem Start. Schemaänderungen sind dadurch weder reviewbar noch reproduzierbar; destruktive Änderungen (Spalte umbenennen/löschen) führen zu Schema-Drift, weil `update` nichts entfernt.

**Empfehlung:** Flyway (oder Liquibase) einführen: `flyway-core` + `flyway-database-postgresql` in die `pom.xml`, bestehendes Schema als `V1__baseline.sql` einfrieren (`spring.flyway.baseline-on-migrate=true` für bestehende DBs), danach `ddl-auto=validate` setzen. Jede künftige Entity-Änderung bekommt ein nummeriertes Migrationsskript im PR. Vor ernsthaftem Prod-Betrieb umsetzen. Fehlende Constraints (DB-002) im selben Zug einziehen.

---

### DB-002 – Fachliche Kardinalitäten nicht per DB-Constraint erzwungen *(Review 2026-07-09)*

Das Datenmodell definiert `Einladung 1—1 Teilnahme`, `Teilnahme 1—1 Abrechnung` und faktisch eine Einladung pro Partei und Event — auf DB-Ebene erzwingt das nichts: kein `unique` auf `teilnahme.einladung_id`, `abrechnung.teilnahme_id` oder `einladung(event_id, partei_id)`. Der POST-Upsert-Pfad (REST-001) und die Bulk-Einladungserstellung im Frontend machen Duplikate realistisch. Zudem haben die `BigDecimal`-Geldfelder keine explizite `precision`/`scale` — Hibernate wählt den Spaltentyp selbst.

**Empfehlung:** Im Zuge der Flyway-Baseline (DB-001), nach Prüfung/Bereinigung des Bestands:
- `ALTER TABLE teilnahme ADD CONSTRAINT uk_teilnahme_einladung UNIQUE (einladung_id);` analog `abrechnung.teilnahme_id` und `einladung (event_id, partei_id)`
- Entities nachführen: `@OneToOne` + `@JoinColumn(unique = true)` bzw. `@Table(uniqueConstraints = ...)`, damit `ddl-auto=validate` konsistent bleibt
- Geldbeträge explizit als `numeric(10,2)` (`@Column(precision = 10, scale = 2)`) auf `Konsumationsangebot.preis`, `Allgemeinausgabe.betrag`, `Abrechnung.*`, `Zahlung.betrag`
- Constraint-Verletzung liefert dann 409 via ERROR-001-Handler (`DataIntegrityViolationException`)

---

### API-001 – API-Contract nur implizit (kein OpenAPI, kein DTO-Layer)

JPA-Entities sind direkt der API-Contract (inkl. verschachtelter Beziehungen wie `Teilnahme → Einladung → Partei → Personen`); die TypeScript-Interfaces im Frontend werden von Hand synchron gehalten. Es gibt keine OpenAPI-Spec, keine generierten Typen und keine Contract-Tests — Drift zwischen Entity und Frontend-Model fällt erst im lokal laufenden Playwright-E2E auf (das nicht in CI läuft, → CI-001). Folgeproblem: Entity-Serialisierung erzwingt die in PERF-001 dokumentierten verschachtelten Payloads.

**Empfehlung:** In zwei Stufen:
1. `springdoc-openapi-starter-webmvc-ui` einbinden → `/v3/api-docs` dokumentiert den Ist-Contract ohne Codeänderung; Frontend-Typen daraus generieren (z.B. `openapi-typescript`) und die Generierung in der Frontend-CI gegen das eingecheckte Schema diffen (Drift-Erkennung).
2. Mittelfristig DTO-Layer (Java Records je Endpunkt) einführen, beginnend bei den Endpunkten mit verschachtelten Payloads (`/api/einladungen`, `/api/teilnahmen`, `/api/abrechnungen`) — entkoppelt Frontend vom DB-Schema und löst den PERF-001-Rest.

---

### CI-001 – Playwright-E2E läuft nicht in CI

Die E2E-Suite (UC-001..016, wertvollste Absicherung des Frontend↔Backend-Zusammenspiels) läuft nur lokal. Contract- oder Integrationsfehler zwischen den Repos werden von keiner Pipeline erkannt.

**Empfehlung:** Eigener Workflow im Frontend-Repo (Push/PR oder Nightly): PostgreSQL-16-Service-Container (wie Backend-CI) → Backend-Repo via `actions/checkout` (`repository: davidrossier/quartierfest-backend`) auschecken und mit `./mvnw spring-boot:run` im Hintergrund starten → `npm start` im Hintergrund → `npx playwright install chromium --with-deps` → `npm run e2e`. Auf Backend-Readiness warten — sauber via `/actuator/health` (→ OPS-001; `curl --retry` auf `/api/persons` funktioniert nur, weil `spring-boot:run` das `dev`-Profil setzt — fail-closed liefert dort 401). Playwright-Report als Artifact hochladen. Falls Laufzeit stört: als Nightly-`schedule` statt pro Push.

---

### SEC-002 – Kein Brute-Force-Schutz auf `POST /api/auth/login` *(Review 2026-07-09; hochgestuft aus UC-014-Open-Item)*

Der Login-Endpunkt ist `permitAll()` und unter `https://davidrossier.ch` internet-exponiert; Passwörter dürfen nur min. 10 Zeichen haben, es gibt keinerlei Drosselung. Credential-Stuffing/Brute-Force ist damit unbeschränkt möglich. In AUTH-002 als «bewusst offen» akzeptiert — für den Prod-Betrieb im Internet ist das nicht mehr vertretbar (MAJOR).

**Empfehlung:** Einfacher In-Memory-Ansatz genügt (Single-Instance-Deployment): Fehlversuchs-Zähler pro E-Mail **und** pro Client-IP (z.B. Caffeine-Cache mit TTL), nach 5 Fehlversuchen 15 Minuten sperren → 429 mit generischer Meldung; erfolgreicher Login setzt den Zähler zurück. Alternativ Bucket4j. Fehlversuche mit WARN loggen (ohne Passwort). Umsetzung in `AuthService.login()` bzw. als vorgeschalteter Check im `AuthController`; IT-Testfall ergänzen (TC-041, `testdesign.md` nachführen). UC-014-Open-Item danach schliessen.

---

### BIZ-001 – UC-011: Abrechnungs-Berechnungslogik fehlt (fachlich wichtigste Lücke) *(Review 2026-07-09)*

`anteilAllgemeinkosten`, `totalKonsumation` und `totalBetrag` werden manuell erfasst — die Abrechnung ist aber der Kern des Nutzenversprechens der Nachbearbeitung, und die manuelle Rechnung ist genau der fehleranfällige Schritt, den die Software abnehmen soll. Bisher nur als Traceability-Lücke geführt (UC-011, UC-009); hier konsolidiert, weil die fachliche Priorität über mehreren technischen MAJORs liegt.

**Empfehlung:**
- `AbrechnungService.berechneFuerEvent(eventId)`: pro Teilnahme `totalKonsumation = Σ(konsumation.anzahl × angebot.preis)`, `anteilAllgemeinkosten = Σ(allgemeinausgaben des Events) ÷ Verteilschlüssel`, `totalBetrag` als Summe. **Verteilschlüssel vorab mit dem Organisator klären** (pro Teilnahme vs. pro effektive Person) und im UC-011 festhalten.
- Endpunkt z.B. `POST /api/events/{id}/abrechnungen/berechnen` (erstellt/aktualisiert alle Abrechnungen des Events); manuelle Übersteuerung via bestehendem POST erhalten.
- Rundung: `BigDecimal` mit `RoundingMode.HALF_UP` auf 0.05 (Schweizer Rappenrundung) — mit Organisator klären.
- Unit-Tests für den `AbrechnungService` gleichzeitig einführen (→ TEST-003).
- UC-009 analog: `GET /api/events/{id}/konsumationsliste` schliesst die zweite Teilimplementierung.

---

### OPS-001 – Deployment-Prozess undokumentiert, keine Backup-Strategie, kein Health-Endpoint *(Review 2026-07-09)*

Es gibt kein Dockerfile, kein Deploy-Skript und keine Beschreibung, wie Jar + Angular-Build hinter Nginx auf `davidrossier.ch` landen — das Wissen existiert nur im Kopf des Betreibers. Für die Prod-DB (Personen- und Zahlungsdaten des Vereins) ist keine Backup-Strategie dokumentiert. `spring-boot-starter-actuator` fehlt, daher kein `/actuator/health` für Readiness-Checks (betrifft auch CI-001).

**Empfehlung:**
1. `DEPLOYMENT.md` im Backend-Repo: Build-Schritte, benötigte Umgebungsvariablen (`AUTH_JWT_SECRET`, `AUTH_INITIAL_ADMIN_*`, `DB_*`), Nginx-Routing (`/api` → 8080, Rest → Angular-`dist/`), Startkommando mit `--spring.profiles.active=prod`
2. Backups: täglicher `pg_dump` per Cron + gelegentlicher Restore-Test; Aufbewahrung dokumentieren (→ DATA-001)
3. `spring-boot-starter-actuator` ergänzen; in `SecurityConfig` nur `/actuator/health` freigeben (`requestMatchers("/actuator/health").permitAll()`), Rest der Actuator-Endpunkte gesperrt lassen
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

---

### TEST-001 – IT-Test-Boilerplate ohne Basisklasse

Das `setUp()`-Muster (zwei `RestTemplate`, no-op `ResponseErrorHandler`, JSON-Header) ist in allen 17 IT-Klassen identisch kopiert; `tryDelete()`/Fixture-Cleanup zusätzlich in 14 davon (~400+ Zeilen Duplikat-Code).

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

### ERROR-001 – Kein globaler Exception-Handler (500 statt 404, kein einheitliches Fehler-JSON)

Referenzen auf nicht-existierende FK-IDs liefern HTTP 500 (`DataIntegrityViolation`/`EntityNotFound` ungefangen — TC-012, TC-023). Es gibt kein einheitliches Fehler-JSON; das Frontend zeigt `err.error?.message` an und bekommt bei 500ern nichts Brauchbares.

**Empfehlung:** Einen `@RestControllerAdvice` einführen: `EntityNotFoundException`/`ResponseStatusException(404)` → 404, `DataIntegrityViolationException` → 409, Fallback → 500 mit generischer Meldung; einheitliches Format `{status, message}`. TC-012/TC-023 danach auf 404 anpassen (Erwartung in `testdesign.md` nachführen).

---

### REST-001 – Frontend aktualisiert Teilnahmen via POST-Upsert statt PUT

`TeilnahmenVerwaltungComponent.speichern()` sendet beim Bearbeiten `POST /api/teilnahmen` mit gesetzter `id` (JPA-`save()` wirkt als Upsert) statt den vorhandenen `PUT /api/teilnahmen/{id}` zu nutzen. Gleiches Upsert-Muster bei UC-006/UC-012 (`bestaetigungVersendet`, `zustellungsDatum`). Der POST-Endpunkt umgeht damit faktisch die UC-016-Whitelist-Semantik des PUT.

**Empfehlung:** Frontend auf `teilnahmeService.update(id, dto)` umstellen; POST-Upsert unterbinden (im Controller `id != null` → 400 oder `id` vor `save()` nullen). Für UC-006/UC-012 dedizierte PATCH/PUT-Endpunkte erwägen (bestehendes TODO in den IT-Klassen). Security-Einordnung (Review 2026-07-09): In der gesicherten Chain ist POST ORGANISATOR-only, der Whitelist-Bypass ist also nicht durch PARTEI ausnutzbar — trotzdem beheben, zwei Update-Pfade mit unterschiedlichen Regeln bleiben ein Fehlerherd.

---

### TEST-004 – Frontend: kaum Unit-Tests ausserhalb Auth

Nur 7 Spec-Dateien (~31 Tests), fast ausschliesslich `auth/` + `app`. Die 20+ Feature-Komponenten und 11 HTTP-Services haben keine Unit-Tests — insbesondere die `computed`-Logik (Event-Filterung, `einladungenOhneTeilnahme`, Sortierung via `shared/sortierung.ts`) wäre günstig testbar.

**Empfehlung:** Vitest-Specs priorisiert für (1) `shared/sortierung.ts`, (2) `computed`-Ableitungen der Verwaltungs-Komponenten (Signal setzen → Ableitung prüfen, ohne DOM), (3) Services via `provideHttpClientTesting`. Ziel: Kernlogik abgedeckt, nicht Template-Details.

---

### QUAL-001 – Keine statische Analyse / kein Lint in der CI

SonarQube lief einmalig manuell (2026-05-01). Frontend hat kein ESLint; Prettier wird in CI nicht geprüft. Keine Coverage-Reports, kein Dependency-Update-Bot.

**Empfehlung:** Frontend: `ng add angular-eslint` + `npx prettier --check .` als CI-Steps. Beide Repos: Dependabot aktivieren (`.github/dependabot.yml` für npm bzw. maven + github-actions). Optional: SonarCloud (gratis für öffentliche Repos) oder `-Dspotbugs` in die Backend-CI; Vitest/JaCoCo-Coverage als CI-Artifact.

---

### REFACT-002 – Frontend: dupliziertes CRUD-/Meldungs-Muster in allen Verwaltungs-Komponenten

Das Muster `ladevorgang/fehler/erfolg`-Signals + `setTimeout(3–4s)` zum Ausblenden + `laden()`-Reload ist in allen ~12 Verwaltungs-Komponenten kopiert (Pendant zu REFACT-001 im Backend). Querschnittsänderungen am Meldungsverhalten erfordern ~12 gleichlautende Edits.

**Empfehlung:** (1) `MeldungService` (oder Composable `createMeldungen()`) für Erfolg/Fehler inkl. Auto-Ausblenden extrahieren — dabei die Meldungs-UX/a11y-Punkte aus UX-001 gleich mitlösen (ein Refactoring, ein Verhalten); (2) generischen `CrudService<T, P>` als Basis der 11 HTTP-Services einführen. Komponenten-Templates bewusst individuell lassen.

---

### DEP-001 – Ungenutzte `citrus-bom` in der `pom.xml`

Citrus 4.9.4 liegt auf dem Test-Classpath, ist aber dokumentiert inkompatibel mit Spring Framework 7.x und ungenutzt. Zieht Jackson 2.x in den Test-Scope und erzeugt so die in CLAUDE.md dokumentierte `ObjectMapper`-Verwechslungsfalle (Jackson 2 vs. 3).

**Empfehlung:** `citrus-bom` und zugehörige Dependencies aus der `pom.xml` entfernen; Hinweise dazu in CLAUDE.md (Tech-Stack, Jackson-3.x-Warnung) zurückbauen.

---

### CODE-001 – Kyrillische Homoglyphen in zwei IT-Methodennamen

`BestaetigungVerwaltenIT.tc013_bestaetigungVersendetViаUpsert()` und `AbrechnungZustellenIT.tc032_zustellungsDatumViаUpsert()` enthalten ein kyrillisches «а» (U+0430) statt eines lateinischen «a» in «Viа» (kompiliert, aber bricht Textsuche, `-Dtest`-Filter und Copy-Paste). Die Traceability-Tabelle in `testdesign.md` hat das Zeichen mitkopiert.

**Empfehlung:** Beide Methoden auf lateinisches «a» umbenennen (`...ViaUpsert`), `testdesign.md`-Tabelle nachführen. Optional: Checkstyle/Editor-Regel gegen Nicht-ASCII-Identifier.

---

### UX-001 – Meldungs-UX, a11y und Mobile-Tauglichkeit *(Review 2026-07-09)*

Vier zusammenhängende Befunde:
1. Erfolgs-/Fehlermeldungen verschwinden nach 3–4 s (`setTimeout`) — für die Zielgruppe (Quartierverein, breite Altersspanne) zu schnell, und ohne `aria-live` für Screenreader unsichtbar.
2. Native `confirm()`/`prompt()`-Dialoge; insbesondere zeigt `window.prompt` beim Passwort-Reset (`BenutzerVerwaltungComponent`) die Eingabe **unmaskiert** — UX- und Security-Problem zugleich.
3. Fehlermeldungen bei 500ern generisch («konnte nicht gespeichert werden») — Folge von ERROR-001, löst sich mit dessen einheitlichem `{status, message}`-Format.
4. Die Konsumationserfassung (UC-010, Matrix Teilnahmen × Angebote) passiert real am Fest auf dem Smartphone — responsive Verhalten ist ungeprüft.

**Empfehlung:** (1)+(2) im Zuge von REFACT-002 lösen: `MeldungService` rendert in eine zentrale Region mit `role="status"`/`aria-live="polite"`, Erfolgsmeldungen ≥ 8 s oder abweisbar, Fehler bleiben stehen; Passwort-Reset als Inline-Formular mit `<input type="password">` statt `window.prompt`. (4) Mobile-Durchstich von UC-010 vor dem nächsten Fest (Playwright mit `devices['iPhone 15']`-Projekt wäre der billigste dauerhafte Check).

---

### SEC-003 – Kein Audit-Trail für finanzrelevante Änderungen *(Review 2026-07-09)*

Abrechnungen, Zahlungen und Mahnungen sind ohne Nachvollziehbarkeit änder- und löschbar (wer, wann). Für die Vereinsrevision potenziell relevant.

**Empfehlung:** Entweder bewusst als Nicht-Anforderung hier dokumentieren (bei einem Quartierverein vertretbar) — oder günstig nachrüsten: Spring Data Auditing (`@EnableJpaAuditing`, `@CreatedDate`/`@LastModifiedDate`/`@CreatedBy`/`@LastModifiedBy` als `@Embeddable`/`@MappedSuperclass` auf `Abrechnung`, `Zahlung`, `Mahnung`; `AuditorAware<String>` liest die E-Mail aus dem JWT). Neue Spalten via Flyway-Migration (setzt DB-001 voraus).

---

### DATA-001 – Kein Löschkonzept für Personendaten (revDSG) *(Review 2026-07-09)*

Das System speichert Namen, Adressen, Telefonnummern und Zahlungsdaten von Quartierbewohnern unbefristet; eine Aufbewahrungs-/Löschregel ist nirgends spezifiziert.

**Empfehlung:** Mit dem Organisator eine einfache Betriebsregel festlegen und dokumentieren (z.B. «Event-Daten inkl. Konsumationen/Abrechnungen x Jahre nach dem Event löschen; Stammdaten von Personen/Parteien beim Wegzug entfernen»). Technisch reicht vorerst manuelles Löschen über die bestehende UI (Kaskaden prüfen!); ein automatisierter Job ist erst nötig, wenn die Regel steht. Als Abschnitt in `DEPLOYMENT.md` (→ OPS-001) oder eigenem Betriebs-Dokument festhalten.

---

## Behoben

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

**Bewusst offen geblieben** (UC-014/015 Open Items): Brute-Force-Drosselung, Passwort-Selbstwechsel, Token-Blacklist bei Account-Löschung (Restgültigkeit max. 12 h akzeptiert).

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
