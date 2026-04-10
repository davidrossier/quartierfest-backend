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

Das Schema wird automatisch via `spring.jpa.hibernate.ddl-auto=update` verwaltet.

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
# Unit-Test (Context-Load-Smoke-Test)
./mvnw test

# Integrationstests (laufen gegen echte PostgreSQL)
./mvnw test -Dtest="*IT"

# Unit + Integration (bevorzugt für CI)
./mvnw verify

# Einzelne IT-Klasse
./mvnw verify -Dit.test=PersonVerwaltenIT
```

26 Testmethoden (TC-001..TC-028, ohne TC-003 und TC-017) in 13 `*IT`-Klassen unter `src/test/java/ch/quartierfest/backend/citrus/`.

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
| Citrus BOM (Test-Scope) | 4.9.4 |

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

### CORS
`WebConfig.java` erlaubt Cross-Origin-Requests von `http://localhost:4200` auf alle `/api/**`-Endpunkte für `GET`, `POST`, `PUT`, `DELETE`.

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

Alle Endpunkte erreichbar unter `http://localhost:8080`.

| Ressource | GET (Liste) | POST (Erstellen) | PUT (Aktualisieren) | DELETE |
|---|---|---|---|---|
| Personen | `GET /api/persons` | `POST /api/persons` | `PUT /api/persons/{id}` | `DELETE /api/persons/{id}` |
| Parteien | `GET /api/parteien` | `POST /api/parteien` | `PUT /api/parteien/{id}` | `DELETE /api/parteien/{id}` |
| Events | `GET /api/events` | `POST /api/events` | `PUT /api/events/{id}` | `DELETE /api/events/{id}` |
| Einladungen | `GET /api/einladungen` | `POST /api/einladungen` | — | `DELETE /api/einladungen/{id}` |
| Teilnahmen | `GET /api/teilnahmen` | `POST /api/teilnahmen` | — | `DELETE /api/teilnahmen/{id}` |
| Allgemeinausgaben | `GET /api/allgemeinausgaben` | `POST /api/allgemeinausgaben` | — | `DELETE /api/allgemeinausgaben/{id}` |
| Konsumationsangebote | `GET /api/konsumationsangebote` | `POST /api/konsumationsangebote` | — | `DELETE /api/konsumationsangebote/{id}` |
| Konsumationen | `GET /api/konsumationen` | `POST /api/konsumationen` | — | `DELETE /api/konsumationen/{id}` |
| Abrechnungen | `GET /api/abrechnungen` | `POST /api/abrechnungen` | — | `DELETE /api/abrechnungen/{id}` |
| Zahlungen | `GET /api/zahlungen` | `POST /api/zahlungen` | — | `DELETE /api/zahlungen/{id}` |
| Mahnungen | `GET /api/mahnungen` | `POST /api/mahnungen` | — | `DELETE /api/mahnungen/{id}` |

Alle Endpunkte geben `200 OK` zurück (auch POST und DELETE). Beziehungen:

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

---

## Spezifikationen

Alle Spezifikationen liegen unter `specs/`:

| Datei | Inhalt |
|---|---|
| `use-cases_overview.md` | Übersicht aller 13 Use Cases |
| `UC-001` .. `UC-013` | Einzelne Use Cases |
| `testdesign.md` | Testdesign TC-001..TC-028, Transportstrategie, Open Items |
| `datamodel.md` | Datenmodell |
| `architecture.md` | Architekturdiagramm |
