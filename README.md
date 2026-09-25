# quartierfest-backend

Spring Boot Backend für die Quartierfest-App — verwaltet Personen, Parteien, Events, Einladungen, Teilnahmen, Konsumationen, Abrechnungen, Zahlungen und Mahnungen.

Das zugehörige Frontend liegt unter [`../quartierfest-frontend`](../quartierfest-frontend) (Angular 21, läuft auf `http://localhost:4200`).

---

## Voraussetzungen

| Komponente | Version |
|---|---|
| Java | 21 |
| Maven Wrapper | enthalten (`./mvnw`) |
| PostgreSQL | 15+ (läuft lokal oder als Docker-Container) |
| Node.js / npm | nur für das Frontend benötigt |

**Datenbank starten (Docker):**
```bash
docker run --name quartierfest-db -e POSTGRES_DB=quartierfest \
  -e POSTGRES_USER=qfuser -e POSTGRES_PASSWORD=qfpass \
  -p 5432:5432 -d postgres:15
```

Verbindungsparameter:
- Host: `localhost:5432`
- Datenbank: `quartierfest`
- User/Password: `qfuser` / `qfpass`

Das Schema wird über **Flyway-Migrationen** (`src/main/resources/db/migration`) verwaltet; Hibernate läuft mit `ddl-auto=validate` (DB-001). Beim ersten Start gegen eine leere DB legt Flyway das Schema an (V1 + V2), eine bestehende, von Hibernate erzeugte DB wird automatisch auf V1 baselined und ab V2 migriert.

### Schema-Migrationen

| Skript | Inhalt |
|---|---|
| `V1__baseline.sql` | Ist-Stand des Hibernate-Schemas (Stand 2026-09-08) |
| `V2__db002_unique_constraints_geldpraezision.sql` | Unique-Constraints (Einladung je Event+Partei, Teilnahme je Einladung, Abrechnung je Teilnahme, Benutzer-E-Mail), Geldbeträge `numeric(10,2)` |

Regel: Jede Entity-Änderung bekommt ein neues `V<n>__<beschreibung>.sql` im selben PR — Hibernate `validate` bricht den Start ab, wenn Entities und Schema nicht zusammenpassen.

### Prod-Erstmigration (einmalig, beim ersten Deployment mit Flyway)

1. **Backup:** `pg_dump -Fc -U <user> <db> > quartierfest-$(date +%F).dump`
2. **Duplikat-Check:** `psql -U <user> -d <db> -f src/main/resources/db/check/duplikate-vor-v2.sql` — alle Abfragen müssen 0 Zeilen liefern. Treffer (mehrere Einladungen derselben Partei zum selben Event, mehrere Teilnahmen je Einladung, mehrere Abrechnungen je Teilnahme) vorher über die UI oder per SQL bereinigen, sonst schlägt V2 fehl und die App startet nicht (DDL wird zurückgerollt, die DB bleibt unverändert).
3. **Deploy + Start:** Flyway legt `flyway_schema_history` an, baselined auf V1 und führt V2 aus; Hibernate validiert das Schema.
4. **Kontrolle:** `SELECT version, description, success FROM flyway_schema_history;` → Zeilen `1` (Baseline) und `2` mit `success = t`.

---

## Backend starten

```bash
# Bauen (ohne Tests)
./mvnw clean install -DskipTests

# Anwendung starten → http://localhost:8080
./mvnw spring-boot:run
```

---

## Frontend starten

```bash
cd ../quartierfest-frontend
npm install
npm start        # http://localhost:4200
```

---

## Tests

```bash
# Unit-Tests (Controller-, Service- und Smoke-Tests; Smoke-Test braucht PostgreSQL)
./mvnw test

# Integrationstests (laufen gegen echte PostgreSQL)
./mvnw test -Dtest="*IT"

# Unit + Integration (bevorzugt für CI)
./mvnw verify

# Einzelne IT-Klasse
./mvnw verify -Dit.test=PersonVerwaltenIT
```

**Unit-Tests** (`./mvnw test`): 83 Testmethoden.
- 13 `*ControllerTest`-Klassen mit `@WebMvcTest` (58 Tests) — decken die HTTP-Schicht aller Domänen ab, ohne Datenbankabhängigkeit
- 4 Service-Tests mit Mockito (24 Tests): `ParteiServiceTest` (`personenIds`-Auflösung, unbekannte Person → 400, PUT → 404), `BenutzerServiceTest` (BCrypt, Duplikat-E-Mail, letzter ORGANISATOR, unbekannte Partei → 400), `AuthServiceTest` (Token-Claims, 401), `LoginDrosselungTest` (SEC-002)
- `BackendApplicationTests` — Spring-Kontext-Smoke-Test (braucht PostgreSQL)

**Integrationstests** (`./mvnw verify`): 52 Testmethoden, 74 Ausführungen inkl. Parametrisierung (TC-001..TC-054, ohne TC-003 und TC-017) in 20 `*IT`-Klassen — 17 je im Domain-Package unter `src/test/java/ch/quartierfest/backend/<domäne>/` plus `OpenApiContractIT` (TC-046, Abgleich `/v3/api-docs` ↔ `specs/openapi.json`), `RestKonventionenIT` (TC-052/053) und `AbfrageAnzahlIT` (TC-054) —, laufen gegen echte PostgreSQL.

**API-Contract (API-001):** Controller nehmen `*Request`-Records entgegen und liefern `*Response`-Records, nie JPA-Entities (Stufe 2, `specs/plans/API-001_Stufe-2_Plan.md`). Referenzen im Request sind flache IDs (`eventId`, `parteiId`, …). Nach einer Änderung an Records/Controllern die versionierte OpenAPI-Spec neu erzeugen und mitcommitten, sonst schlägt TC-046 fehl:

```bash
OPENAPI_UPDATE=true ./mvnw verify -Dit.test=OpenApiContractIT
```

**CI:** GitHub Actions (`.github/workflows/ci.yml`) führt `./mvnw verify` bei Push/PR auf `main` gegen einen PostgreSQL-16-Service-Container aus. Die Playwright-E2E-Suite läuft nächtlich im Frontend-Repo (`e2e.yml`) gegen `main` dieses Repos.

---

## Tech Stack

### Backend
| Technologie | Version |
|---|---|
| Spring Boot | 4.0.3 |
| Spring Framework | 7.x |
| Java | 21 |
| Spring Data JPA + PostgreSQL | — |
| Lombok | `@Data`, `@RequiredArgsConstructor` |
| Spring WebMVC (synchronous) | — |
| Flyway (Schema-Migrationen) | 11.x (Spring-Boot-managed) |
| springdoc-openapi (OpenAPI-Spec + Swagger-UI) | 3.1.1 |
| Spring Boot Actuator (nur `/actuator/health`) | Spring-Boot-managed |

### Frontend
| Technologie | Version |
|---|---|
| Angular (Standalone-API) | 21.2.0 |
| TypeScript | 5.9.x |
| RxJS | 7.8.x |
| Vitest | 4.x |
| Prettier | 3.x |

---

## Architektur

### Sicherheit & CORS
`SecurityConfig.java` kapselt CORS und Authentifizierung:
- **CORS:** erlaubter Origin via Property `cors.allowed-origins` (Dev-Default `http://localhost:4200`) für alle `/api/**`-Endpunkte (`GET`, `POST`, `PUT`, `DELETE`)
- **Auth (AUTH-002, Eigenbau):** `POST /api/auth/login` stellt ein HS256-JWT aus (12 h); die Autorisierungsmatrix (Rollen `ORGANISATOR`/`PARTEI`) ist der Default (fail-closed, SEC-001) — `permitAll()` mit Token-Verarbeitung nur bei explizitem `dev`-Profil, das `./mvnw spring-boot:run` automatisch setzt. Details → `specs/architecture.md`
- **Dev-Login:** `admin@quartierfest.local` / `quartierfest-admin` (Bootstrap-Admin, wird beim Start angelegt, falls kein ORGANISATOR existiert)

### Backend-Domänen
Jede Domäne besteht aus genau 4 Dateien unter `ch.quartierfest.backend.<domäne>`:
`Entity` · `Repository` · `Service` · `Controller`

### Frontend-Struktur
Das Frontend ist in vier Workflow-Phasen unterteilt:

| Phase | Route-Präfix | Domänen |
|---|---|---|
| Stammdaten | `/personen`, `/parteien`, `/events` | Personen, Parteien, Events |
| Planung | `/planung/...` | Einladungen, Teilnahmen, Allgemeinausgaben, Konsumationsangebote, Bestätigung |
| Durchführung | `/durchfuehrung/...` | Konsumationsliste, Konsumationen |
| Nachbearbeitung | `/nachbearbeitung/...` | Abrechnungen, Zahlungen, Mahnungen |

Event-abhängige Routen nutzen einen gemeinsamen `EventKontextService` (Angular Signals), der den aktuell gewählten Event über alle Routing-Gruppen hinweg hält.

---

## API-Endpunkte

Alle Endpunkte erreichbar unter `http://localhost:8080`. OpenAPI-Spec: `GET /v3/api-docs`, Swagger-UI: `http://localhost:8080/swagger-ui.html` (beides im `dev`-Profil offen, sonst 401; im `prod`-Profil deaktiviert). Der versionierte Contract liegt in `specs/openapi.json`. Readiness: `GET /actuator/health` → `{"status":"UP"}` (ohne Token, ohne Details; übrige Actuator-Pfade gesperrt).

| Ressource | GET (Liste) | POST (Erstellen) | PUT (Aktualisieren) | DELETE |
|---|---|---|---|---|
| Personen | `GET /api/persons` | `POST /api/persons` | `PUT /api/persons/{id}` | `DELETE /api/persons/{id}` |
| Parteien | `GET /api/parteien` | `POST /api/parteien` | `PUT /api/parteien/{id}` | `DELETE /api/parteien/{id}` |
| Events | `GET /api/events` | `POST /api/events` | `PUT /api/events/{id}` | `DELETE /api/events/{id}` |
| Einladungen | `GET /api/einladungen` | `POST /api/einladungen` | `PUT /api/einladungen/{id}` (Rückmeldung, Bestätigung; REST-003) | `DELETE /api/einladungen/{id}` |
| Teilnahmen | `GET /api/teilnahmen`, `GET /api/teilnahmen/meine` (PARTEI, UC-016) | `POST /api/teilnahmen` | `PUT /api/teilnahmen/{id}` (Whitelist-DTO, UC-016) | `DELETE /api/teilnahmen/{id}` |
| Allgemeinausgaben | `GET /api/allgemeinausgaben` | `POST /api/allgemeinausgaben` | `PUT /api/allgemeinausgaben/{id}` | `DELETE /api/allgemeinausgaben/{id}` |
| Konsumationsangebote | `GET /api/konsumationsangebote` | `POST /api/konsumationsangebote` | `PUT /api/konsumationsangebote/{id}` | `DELETE /api/konsumationsangebote/{id}` |
| Konsumationen | `GET /api/konsumationen` | `POST /api/konsumationen` | `PUT /api/konsumationen/{id}` (nur Anzahl) | `DELETE /api/konsumationen/{id}` |
| Abrechnungen | `GET /api/abrechnungen` | `POST /api/abrechnungen` | `PUT /api/abrechnungen/{id}` (Kanal, Zustellung, Beträge; REST-003) | `DELETE /api/abrechnungen/{id}` |
| Zahlungen | `GET /api/zahlungen` | `POST /api/zahlungen` | — | `DELETE /api/zahlungen/{id}` |
| Mahnungen | `GET /api/mahnungen` | `POST /api/mahnungen` | — | `DELETE /api/mahnungen/{id}` |
| Benutzer | `GET /api/benutzer` | `POST /api/benutzer` | `PUT /api/benutzer/{id}/passwort` (Reset) | `DELETE /api/benutzer/{id}` |
| Auth | — | `POST /api/auth/login` → `{token}` | — | — |

Alle Endpunkte geben `200 OK` zurück (auch POST und DELETE). Ein POST mit `id` im Body wird mit `400` abgelehnt (unbekanntes Feld), ein PUT auf eine unbekannte id mit `404`. Beziehungen (alle lazy geladen, OSIV aus):

| Entität | Beziehungen |
|---|---|
| `Partei` | `@OneToMany` → Person |
| `Einladung` | `@ManyToOne` → Event, Partei |
| `Teilnahme` | `@OneToOne` → Einladung |
| `Konsumationsangebot` | `@ManyToOne` → Event |
| `Konsumation` | `@ManyToOne` → Teilnahme, Konsumationsangebot |
| `Allgemeinausgabe` | `@ManyToOne` → Event |
| `Abrechnung` | `@OneToOne` → Teilnahme |
| `Zahlung` | `@ManyToOne` → Abrechnung |
| `Mahnung` | `@ManyToOne` → Abrechnung |
| `Benutzer` | `@ManyToOne` → Partei (optional; Pflicht bei Rolle PARTEI) |

---

## Spezifikationen

Alle Spezifikationen liegen unter `specs/`:

| Datei | Inhalt |
|---|---|
| `use-cases_overview.md` | Übersicht aller 16 Use Cases |
| `UC-001` .. `UC-016` | Einzelne Use Cases |
| `testdesign.md` | Testdesign TC-001..TC-054, Transportstrategie, Open Items |
| `datamodel.md` | Datenmodell |
| `architecture.md` | Architekturdiagramm, Traceability-Matrix, technische Schulden |
| `TODO.md` | Technische Schulden und Refactoring-Backlog |
| `plans/` | Umsetzungspläne grösserer TODO-Punkte (z.B. `API-001_Stufe-2_Plan.md`) |
