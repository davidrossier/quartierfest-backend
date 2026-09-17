# API-001 Stufe 1 – OpenAPI-Contract, generierte Frontend-Typen, Drift-Check

> Stand: 2026-09-17. Umsetzungsplan zu `TODO.md` → API-001 (Stufe 1). Stufe 2 (DTO-Layer) bleibt offen.
> Branch (beide Repos): `feature/api-001-openapi-stufe-1`

## Ziel

Der API-Contract zwischen Backend und Frontend wird explizit, versioniert und in beiden CIs geprüft:

1. Backend liefert unter `/v3/api-docs` eine OpenAPI-3-Spec des Ist-Contracts (springdoc, keine Controller-Änderung).
2. Die Spec liegt eingecheckt als `specs/openapi.json` im Backend-Repo. Ein Integrationstest schlägt fehl, wenn Entities/Controller sich ändern, ohne dass die Spec nachgeführt wurde.
3. Das Frontend generiert daraus `src/app/api/schema.d.ts` (openapi-typescript). Die handgepflegten Response-Interfaces in den `*.model.ts` werden aus dem Schema abgeleitet.
4. Die Frontend-CI generiert die Typen gegen den Backend-`main`-Contract neu und schlägt bei Abweichung fehl (Drift-Check).

**Nicht Teil von Stufe 1:** DTOs/Records, Entkopplung vom DB-Schema, Auflösung der verschachtelten Payloads (PERF-001-Rest), `@Data` → `@Getter/@Setter` (CODE-002). Das ist Stufe 2.

## Entscheide

| # | Entscheid | Begründung |
|---|---|---|
| E1 | **springdoc-openapi 3.1.1** (`springdoc-openapi-starter-webmvc-ui`) | 3.x ist die Linie für Spring Boot 4 / Spring Framework 7 / Jackson 3. 2.8.x läuft nicht auf Boot 4. |
| E2 | **Spec als Datei eingecheckt, Abgleich per IT** statt `springdoc-openapi-maven-plugin` | Passt zum bestehenden IT-Muster (`@ActiveProfiles("dev")`, gecachter Context, DB in CI vorhanden). Das Maven-Plugin bräuchte zusätzlich `spring-boot:start/stop` im Build-Lifecycle. |
| E3 | **Keine Änderung an `SecurityConfig`** | Default-/prod-/security-test-Chain: `/v3/api-docs` und `/swagger-ui/**` fallen unter `anyRequest().authenticated()` → 401 ohne JWT (fail-closed, SEC-001). Dev-Chain: offen. In `prod` werden api-docs und Swagger-UI zusätzlich per Property ganz abgeschaltet. |
| E4 | **Drift-Check im Frontend gegen Backend-`main`** (Sparse-Checkout des öffentlichen Backend-Repos) | Beide Repos sind public → kein Token nötig. Lokal wird das Schwester-Repo (`../quartierfest-backend/specs/openapi.json`) verwendet, in CI der Checkout-Pfad via `OPENAPI_SPEC`. |
| E5 | **Response-Typen aus dem Schema, `*Payload`-Typen bleiben handgeschrieben** | Entity = Request- und Response-Schema, deshalb ist `id` im Schema optional und verschachtelte Referenzen (`event: {id}`) sind nicht als eigener Typ ausgedrückt. Helper `Persisted<T> = T & { id: number }` für Antworten; Payloads erst mit DTOs (Stufe 2) generierbar. |
| E6 | **Deterministische Spec** (`springdoc.writer-with-order-by-keys=true`, pretty-print über Jackson beim Schreiben) | Sonst ist `git diff` auf `openapi.json` unbrauchbar. |

## Reihenfolge

Backend-PR zuerst mergen. Der Frontend-Drift-Check zieht die Spec von Backend-`main`; ein Frontend-PR gegen eine noch nicht gemergte Backend-Änderung schlägt in der CI bewusst fehl. Lokal ist die Frontend-Arbeit parallel möglich (Schwester-Repo auf dem Feature-Branch).

---

## Teil A – Backend (`quartierfest-backend`)

### A1 Dependency und Konfiguration

- `pom.xml`: `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1` (compile scope).
- `application.properties`:
  ```properties
  # API-001: OpenAPI-Spec (/v3/api-docs) und Swagger-UI (/swagger-ui.html); Ausgabe deterministisch für den Contract-Abgleich
  springdoc.writer-with-order-by-keys=true
  springdoc.swagger-ui.operations-sorter=alpha
  springdoc.swagger-ui.tags-sorter=alpha
  ```
- `application-prod.properties`:
  ```properties
  # API-001: Contract wird im Repo versioniert (specs/openapi.json) – in prod nicht exponieren
  springdoc.api-docs.enabled=false
  springdoc.swagger-ui.enabled=false
  ```
- Prüfen, ob `spring-boot-devtools` oder der OAuth2-Resource-Server springdoc zusätzliche Pfade unterjubelt (z.B. Actuator-Gruppen) – nicht erwartet, aber im ersten `/v3/api-docs`-Dump kontrollieren.

### A2 `OpenApiConfig` (klein, optional aber empfohlen)

`ch.quartierfest.backend.OpenApiConfig` mit einem `OpenAPI`-Bean: Titel «Quartierfest API», Version aus `BuildProperties` oder fix `0.0.1`, plus `SecurityScheme` `bearerAuth` (HTTP bearer, JWT) als globales `SecurityRequirement`. Damit funktioniert in der Swagger-UI der «Authorize»-Button mit dem Token aus `POST /api/auth/login` – nützlich, sobald man lokal ohne dev-Profil testet.

### A3 Contract-Test `OpenApiContractIT` (TC-046)

- Package `ch.quartierfest.backend`, `@SpringBootTest(DEFINED_PORT)` + `@ActiveProfiles("dev")` → teilt den gecachten Context der übrigen ITs. Traceability-Header wie in den anderen ITs (UC: übergreifend / API-001, TC-046).
- Ablauf:
  1. `GET http://localhost:{port}/v3/api-docs` → JSON-String.
  2. Mit `tools.jackson.databind.ObjectMapper` (Jackson 3!) als `JsonNode` einlesen und **pretty-printed, Keys sortiert** serialisieren (`SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS` + `INDENT_OUTPUT`).
  3. Wenn Umgebungsvariable `OPENAPI_UPDATE=true`: `specs/openapi.json` schreiben, Test ist grün.
  4. Sonst: mit dem eingecheckten `specs/openapi.json` vergleichen; bei Abweichung `assertThat(...).isEqualTo(...)` mit Hinweis «Contract geändert – `OPENAPI_UPDATE=true ./mvnw verify -Dit.test=OpenApiContractIT` ausführen und `specs/openapi.json` mitcommitten».
- Pfad relativ zum Modul-Root (`Path.of("specs", "openapi.json")`) – Maven setzt das Arbeitsverzeichnis auf das Modul.
- Der Test erzeugt keine Daten → kein Cleanup.
- Entwickler-Workflow (in CLAUDE.md festhalten): Entity-/Controller-Änderung → Flyway-Migration (DB-001) **und** Spec-Update im selben PR, sonst rot in CI.

### A4 Erst-Dump und Sichtkontrolle

`OPENAPI_UPDATE=true ./mvnw verify -Dit.test=OpenApiContractIT` → `specs/openapi.json` entsteht. Prüfen:
- Alle 13 Ressourcen + `/api/auth/login`, `/api/teilnahmen/meine`, `/api/benutzer/{id}/passwort` vorhanden.
- `Benutzer`: `passwortHash` fehlt (`@JsonIgnore`), `passwort` als `writeOnly`.
- Enums als `enum`-Arrays (`EinladungStatus`, `BuffetBeitrag`, `Zustellungskanal`, `Zahlungskanal`, `Rolle`).
- `@NotNull`/`@NotBlank`-Felder unter `required`.
- `TeilnahmeUpdateRequest` und `{token}`-Antwort von `/api/auth/login` korrekt.
- Fehler-JSON `{status, message}` (ERROR-001) taucht nicht automatisch auf – akzeptiert für Stufe 1, optional per `@ApiResponse` am `GlobalExceptionHandler` nachrüsten (nicht blockierend).

### A5 Dokumentation nachführen

- `CLAUDE.md`: Tech-Stack-Zeile springdoc; Abschnitt «Integration tests» 17 → 18 Klassen, TC-045 → TC-046; neuer Absatz «API-Contract (API-001)» mit Workflow und Swagger-UI-URL.
- `README.md`: Endpunkt-Tabelle um `/v3/api-docs` und `/swagger-ui.html` (nur dev/ohne prod-Profil); Testzahlen.
- `specs/testdesign.md`: TC-046 eintragen; Open Item «kein Contract-Test» schliessen.
- `specs/architecture.md`: Zeile API-001 → «Stufe 1 erledigt 2026-xx-xx, Stufe 2 offen».
- `specs/TODO.md`: Priorisierung Punkt 6 mit ✅ + Datum; im MAJOR-Block Stufe 1 als erledigt markieren, Stufe 2 stehen lassen.

### A6 Verifikation Backend

```bash
./mvnw verify                                   # alle Unit-Tests + 18 ITs grün, inkl. TC-046
./mvnw spring-boot:run                          # dann:
curl -s localhost:8080/v3/api-docs | head       # JSON
open http://localhost:8080/swagger-ui.html      # UI lädt, Endpunkte sichtbar
```
Zusätzlich fail-closed prüfen: gepacktes Jar ohne Profil starten → `curl -i localhost:8080/v3/api-docs` liefert 401.

---

## Teil B – Frontend (`quartierfest-frontend`)

### B1 Generator einrichten

- `npm i -D openapi-typescript@^7.13.0`.
- `package.json`-Scripts:
  ```json
  "api:generate": "openapi-typescript \"${OPENAPI_SPEC:-../quartierfest-backend/specs/openapi.json}\" -o src/app/api/schema.d.ts --root-types --root-types-no-schema-prefix --alphabetize",
  "api:check": "npm run api:generate && git diff --exit-code -- src/app/api/schema.d.ts"
  ```
  `--root-types` exportiert `Einladung`, `Person`, … direkt auf Root-Ebene (statt nur `components['schemas']['Einladung']`). `--alphabetize` hält die Datei diff-stabil.
- `src/app/api/schema.d.ts` wird **eingecheckt** (Generat, Header «do not edit»). In `.prettierignore` (neu anlegen) eintragen, damit ein späterer Prettier-Check (QUAL-001) das Generat nicht anfasst.
- `src/app/api/types.ts`:
  ```ts
  /** Persistierte Entität: id ist in Antworten immer gesetzt (im Schema optional, weil Entity = Request- und Response-Typ). */
  export type Persisted<T> = T & { id: number };
  ```

### B2 Modelle auf das Schema umstellen

Reihenfolge nach Verschachtelungstiefe, jedes Modell einzeln mit `npm run build` und `npm test` absichern:

| Schritt | Datei | Änderung |
|---|---|---|
| 1 | `person.model.ts`, `event.model.ts`, `konsumationsangebot.model.ts`, `allgemeinausgabe.model.ts` | `export type Person = Persisted<Schema.Person>`; `PersonPayload` bleibt. |
| 2 | `partei.model.ts`, `benutzer.model.ts` | dito; `Benutzer.partei` wird `Partei \| undefined`, `passwort` bleibt im Payload. |
| 3 | `einladung.model.ts` | `EinladungStatus`/`BuffetBeitrag` aus dem Schema re-exportieren (`Schema.Einladung['status']`), Interface ersetzen. |
| 4 | `teilnahme.model.ts`, `konsumation.model.ts` | `BuffetBeitragEintrag` aus Schema; `TeilnahmeUpdatePayload` ← `Schema.TeilnahmeUpdateRequest`. |
| 5 | `abrechnung.model.ts`, `zahlung.model.ts`, `mahnung.model.ts` | dito; `ZustellungsKanal`/`ZahlungsKanal`-Aliase auf Schema-Enums. |

Erwartete Reibung (bewusst in Kauf genommen, wird beim Umstellen behoben):
- Verschachtelte Referenzen (`einladung.event.id`) werden `number | undefined`, weil die verschachtelte Entity kein `Persisted<>` ist. Wo in Payloads `{ id: number }` gebraucht wird: an der Stelle `Persisted<>` auf das verschachtelte Objekt anwenden oder im Model einen präzisierten Typ bilden, z.B. `Einladung = Persisted<Omit<Schema.Einladung, 'event' | 'partei'> & { event: Persisted<Schema.Event>; partei: Persisted<Schema.Partei> }>`. Bevorzugt: einen generischen Helper `Deep`-Persisted vermeiden, lieber explizit pro Modell – bleibt lesbar und ist mit Stufe 2 obsolet.
- Felder, die das Frontend bisher als Pflicht deklariert hat, das Backend aber nicht mit `@NotNull` markiert (z.B. `Einladung.bestaetigungVersendet` als primitiver `boolean`): entweder im Frontend `?` akzeptieren (Template-Anpassungen mit `?? false`) oder – falls semantisch Pflicht – im Backend `@NotNull` ergänzen. Zweites verändert die Validierung und gehört dann in den Backend-PR mit Spec-Update. Entscheid beim ersten Auftreten treffen und in diesem Plan nachtragen.
- `noPropertyAccessFromIndexSignature` ist aktiv: Zugriffe auf `components['schemas'][...]` sind Typ-, nicht Wertzugriffe → kein Problem.

### B3 CI-Drift-Check (`.github/workflows/ci.yml`)

Neuer Schritt nach `npm ci`, vor den Tests:

```yaml
      - name: Backend-API-Contract auschecken (API-001)
        uses: actions/checkout@v5
        with:
          repository: davidrossier/quartierfest-backend
          ref: main
          path: backend-contract
          sparse-checkout: specs/openapi.json
          sparse-checkout-cone-mode: false

      - name: API-Typen generieren und Drift prüfen
        run: npm run api:check
        env:
          OPENAPI_SPEC: backend-contract/specs/openapi.json
```

Fehlerbild bei Drift: `git diff --exit-code` zeigt den Unterschied im Log → Entwickler führt lokal `npm run api:generate` aus, behebt Typfehler, committet `schema.d.ts`.

### B4 Dokumentation nachführen

- `CLAUDE.md`: Commands (`api:generate`, `api:check`), CI-Beschreibung, neuer Abschnitt «API-Contract (API-001)»: Quelle `../quartierfest-backend/specs/openapi.json`, Generat `src/app/api/schema.d.ts`, Regel «Modelle leiten Response-Typen aus dem Schema ab, Payloads bleiben handgeschrieben».
- Feature-Liste: die `*.model.ts`-Beschreibungen kürzen («abgeleitet aus Schema»).
- `README.md`: Abschnitt «API-Typen» mit den zwei Scripts.

### B5 Verifikation Frontend

```bash
npm run api:generate && git status --short src/app/api   # Generat aktuell, keine Änderung
npm test -- --watch=false
npm run build -- --configuration production
# Backend (dev-Profil) + npm start laufen:
npm run e2e                                              # UC-001..016 grün
```
Negativtest des Drift-Checks: in `specs/openapi.json` lokal ein Feld umbenennen → `npm run api:check` schlägt fehl; danach zurücksetzen.

---

## Abnahme (Definition of Done)

- [x] Backend `./mvnw verify` grün (44 IT-Methoden inkl. TC-046); `specs/openapi.json` eingecheckt. CI-Lauf folgt mit dem PR.
- [x] Jar ohne Profil: `/v3/api-docs` und `/swagger-ui.html` → 401; prod-Profil (mit Token): 404; dev: 200 — lokal geprüft 2026-09-17.
- [x] Frontend: `npm run api:check` Exit 0 ohne, Exit 1 mit provoziertem Drift (Feld umbenannt); CI-Schritt eingebaut, Lauf folgt mit dem PR.
- [x] Alle zehn `*.model.ts` leiten ihre Response-Typen aus `schema.d.ts` ab; zusätzlich `Rolle`/`LoginResponse` in `auth.service.ts`.
- [x] Vitest 27/27, Production-Build ok; Playwright-E2E lokal: erster Vollauf 56 passed / 1 skipped / 2 failed (UC-004 Rückmeldung, UC-015 PARTEI-Account, beide isoliert sofort grün), zweiter Vollauf 58 passed / 1 skipped / 0 failed → Flakiness beim Kaltstart, nicht typbedingt (Typen haben keine Laufzeitwirkung).
- [x] Doku in beiden Repos (CLAUDE.md, README, testdesign.md, architecture.md, TODO.md) nachgeführt.

## Umsetzungsnotizen (2026-09-17)

- **springdoc 3.1.1** lief auf Spring Boot 4.0.3 ohne Anpassung; Jackson-3-`ObjectMapper` (`tools.jackson`) in der IT via `rebuild()` mit `INDENT_OUTPUT` + `ORDER_MAP_ENTRIES_BY_KEYS`. Achtung: `JsonNode`-Serialisierung ignoriert die Key-Sortierung — deshalb Umweg über `convertValue(tree, Map.class)`.
- **Spec-Befund:** `Benutzer.passwort` ist `writeOnly` **und** `required` (Bean-Validation auf dem `@Transient`-Feld) → im Frontend per `Omit<ApiBenutzer, 'passwort'>` aus dem Antwort-Typ entfernt. `Partei.personenIds` ebenfalls `writeOnly`. Response-Content-Type in der Spec ist `*/*` (kein `produces` an den Controllern) — für openapi-typescript unerheblich.
- **`Persisted<T, K>`** statt nur `Persisted<T>`: zweiter Typparameter macht Felder Pflicht, die das Backend immer liefert, im Entity-Schema aber optional sind (`bestaetigungVersendet`, `twintAktiv`, `personen`, `buffetBeitraege`). Verschachtelte Referenzen pro Modell explizit auf den Modell-Typ umgebogen (E5 wie geplant, kein generischer Deep-Helper).
- **Einzige Reibung in B2:** die Test-Fixture in `meine-teilnahme.component.spec.ts` war gegenüber dem echten Backend-Payload unvollständig (fehlende `startzeit`, `adresse`, `twintAktiv`, `personen`, `bestaetigungVersendet`) und wurde vervollständigt. Keine Komponenten- oder Template-Änderung nötig; ein bestehendes NG8107-Warning (`buffetBeitraege?.length`) ist unverändert.
- **`Rolle`/`LoginResponse`** (`auth.service.ts`) ebenfalls aus dem Schema abgeleitet — vorher handgepflegt ausserhalb der Modelle.
- **CI-Drift-Check** nutzt `git diff --exit-code` auf `schema.d.ts`; das funktioniert nur, wenn die Datei getrackt ist (erster Commit dieses Branches). Beim manuellen Test mit `git add -N` liefert der Diff immer «alles neu» — stattdessen regulär stagen.

## Aufwand (Schätzung)

| Teil | Aufwand |
|---|---|
| A1–A4 Backend | 2 h |
| A5/A6 Doku + Verifikation | 1 h |
| B1, B3 Generator + CI | 1 h |
| B2 Modell-Umstellung inkl. Typfehler | 3–4 h (grösste Unsicherheit: verschachtelte Referenzen) |
| B4/B5 Doku + Verifikation | 1 h |

## Offene Punkte für Stufe 2 (nur notieren)

- DTO-Records je Endpunkt → `Persisted<>` und die handgeschriebenen `*Payload`-Typen entfallen, Request-/Response-Schemas werden getrennt.
- Fehler-Schema `{status, message}` in die Spec aufnehmen.
- CODE-002 (`@Data` → `@Getter/@Setter`) zusammen mit den DTOs.
