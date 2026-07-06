# Architekturdiagramm Quartierfest-Backend

## Schichtenarchitektur

```mermaid
graph LR
    Angular([Angular\nlocalhost:4200])
    Other([Andere Clients])

    subgraph Spring Boot Backend
        direction TB
        SEC[SecurityConfig\nCORS + JWT-Filter]
        Controller[Controller\nREST API]
        Service[Service\nGeschäftslogik]
        Repository[Repository\nJPA / Spring Data]
        SEC --> Controller
        Controller --> Service
        Service --> Repository
    end

    DB[(PostgreSQL\nquartierfest)]

    Angular -->|HTTP + CORS + JWT| SEC
    Other -->|HTTP| SEC
    Repository -->|JDBC| DB
```

### CORS-Konfiguration

`SecurityConfig.java` konfiguriert CORS global für alle `/api/**`-Endpunkte via `CorsConfigurationSource`-Bean:

| Einstellung | Wert |
|---|---|
| Erlaubter Origin | Property `cors.allowed-origins` (Default: `http://localhost:4200`) |
| Erlaubte Methoden | `GET`, `POST`, `PUT`, `DELETE` |
| Erlaubte Headers | `*` |

Andere Origins werden vom Browser blockiert. Server-seitige Clients (z.B. `RestTemplate` in Integrationstests) sind von CORS nicht betroffen.

### Sicherheit (JWT / Spring Security)

Alle `/api/**`-Endpunkte sind über Spring Security 7.x abgesichert. Die Absicherung ist profil-abhängig:

| Profil | SecurityFilterChain | Beschreibung |
|--------|---------------------|--------------|
| `prod` | JWT-Pflicht | Alle Requests benötigen ein gültiges Bearer-Token mit Rolle `ORGANISATOR` |
| kein / `test` / `dev` | `permitAll()` | Kein Token erforderlich — für lokale Entwicklung und Tests |

**Eigenbau-Login (AUTH-002, implementiert 2026-06-12 — UC-014/UC-015/UC-016):**
- Das Backend stellt das JWT selbst aus: `POST /api/auth/login` prüft E-Mail/Passwort (BCrypt) und signiert mit HS256 (`JwtEncoder`; Secret via `auth.jwt.secret`, prod `AUTH_JWT_SECRET`; Gültigkeit 12 h; Claims `sub` = Benutzer-ID, `email`, `rolle`)
- Validierung als OAuth2 Resource Server (AUTH-001), `JwtDecoder` mit symmetrischem Schlüssel statt JWKS/issuer-uri
- `JwtAuthenticationConverter` mappt den Claim `rolle` auf `ROLE_ORGANISATOR`/`ROLE_PARTEI` (der Spring-Default liest nur `scope`-Claims)
- Autorisierungsmatrix (Profile `prod` und `security-test`): Login offen; `/api/benutzer/**` nur ORGANISATOR; PARTEI ausschliesslich `GET /api/teilnahmen/meine` + `PUT /api/teilnahmen/{id}`; alle übrigen `/api/**` nur ORGANISATOR
- Ownership: `@PreAuthorize` + `TeilnahmeZugriff`-Bean (JWT `sub` → `Benutzer` → `Partei` → Teilnahme-Lookup) — Methoden-Security, wirkt in allen Profilen
- Die offene Dev/Test-Chain verarbeitet Bearer-Tokens trotzdem (`oauth2ResourceServer`), bleibt aber `permitAll()` — lokale Entwicklung und bestehende ITs unverändert
- `Benutzer`-Domain (UC-015): `email` unique, `passwortHash` (BCrypt, nie in API-Antworten), `rolle`, optionaler Partei-FK; letzter ORGANISATOR nicht löschbar (409); Bootstrap des ersten ORGANISATOR-Accounts beim Start (`auth.bootstrap.*`, Dev-Default `admin@quartierfest.local`)
- Frontend-Login (UC-014): eigenes Formular unter `/login`, Token in `sessionStorage`, HTTP-Interceptor (Bearer + 401-Redirect), Route Guards, rollenbasiertes Routing — keine externe Library
- Self-Registration ist explizit **nicht vorgesehen** — Accounts werden durch den Organisator in der Admin-UI (`/admin/benutzer`) angelegt; Passwort-Reset nur durch den Organisator

---

## Domänenmodell (Entity-Beziehungen)

```mermaid
erDiagram
    PERSON {
        Long id
        String vorname
        String name
        String telefonnummer
        String mobilenummer
        String email
    }

    PARTEI {
        Long id
        String bezeichnung
        String adresse
        boolean twintAktiv
        String twintMobilenummer
    }

    EVENT {
        Long id
        LocalDate datum
        LocalTime startzeit
        String standort
        String alternativerStandort
        LocalTime zeitAufstellen
        LocalTime zeitAufraumen
    }

    EINLADUNG {
        Long id
        EinladungStatus status
        Integer anzahlPersonen
        Boolean hilftAufstellen
        Boolean hilftAufraumen
        BuffetBeitrag buffetBeitrag
        String buffetBeitragBeschreibung
        boolean bestaetigungVersendet
    }

    TEILNAHME {
        Long id
        Integer anzahlPersonenEffektiv
        Boolean hilftAufstellen
        Boolean hilftAufraumen
    }

    TEILNAHME_BUFFET_BEITRAG {
        BuffetBeitrag art
        String beschreibung
    }

    KONSUMATIONSANGEBOT {
        Long id
        String bezeichnung
        BigDecimal preis
    }

    KONSUMATION {
        Long id
        Integer anzahl
    }

    ALLGEMEINAUSGABE {
        Long id
        String beschreibung
        String herkunft
        BigDecimal betrag
    }

    ABRECHNUNG {
        Long id
        BigDecimal anteilAllgemeinkosten
        BigDecimal totalKonsumation
        BigDecimal totalBetrag
        Zustellungskanal zustellungskanal
        LocalDate zustellungsDatum
    }

    ZAHLUNG {
        Long id
        Zahlungskanal zahlungskanal
        LocalDate datum
        BigDecimal betrag
    }

    MAHNUNG {
        Long id
        LocalDate datum
        String bemerkung
    }

    BENUTZER {
        Long id
        String email
        String passwortHash
        Rolle rolle
    }

    PARTEI ||--o{ PERSON : "besteht aus"
    PARTEI |o--o{ BENUTZER : "meldet sich an als"
    PARTEI ||--o{ EINLADUNG : "erhält"
    EVENT ||--o{ EINLADUNG : "hat"
    EINLADUNG ||--|| TEILNAHME : "wird zu"
    EVENT ||--o{ KONSUMATIONSANGEBOT : "bietet an"
    EVENT ||--o{ ALLGEMEINAUSGABE : "hat"
    TEILNAHME ||--o{ KONSUMATION : "konsumiert"
    KONSUMATIONSANGEBOT ||--o{ KONSUMATION : "wird konsumiert in"
    TEILNAHME ||--o{ TEILNAHME_BUFFET_BEITRAG : "hat"
    TEILNAHME ||--|| ABRECHNUNG : "erhält"
    ABRECHNUNG ||--o{ ZAHLUNG : "wird bezahlt durch"
    ABRECHNUNG ||--o{ MAHNUNG : "wird gemahnt durch"
```

---

## REST-Endpunkte

| Domain              | Endpunkt                      | GET | POST | PUT | DELETE | Hinweis |
|---------------------|-------------------------------|:---:|:----:|:---:|:------:|---------|
| Person              | `/api/persons`                | ✓   | ✓    | ✓   | ✓      | — |
| Partei              | `/api/parteien`               | ✓   | ✓    | ✓   | ✓      | — |
| Event               | `/api/events`                 | ✓   | ✓    | ✓   | ✓      | — |
| Einladung           | `/api/einladungen`            | ✓   | ✓    | —   | ✓      | — |
| Teilnahme           | `/api/teilnahmen`             | ✓   | ✓    | —   | ✓      | — |
| Teilnahme (PARTEI)  | `/api/teilnahmen/meine`       | ✓   | —    | —   | —      | UC-016: eigene Teilnahme zum nächsten Event (JWT `sub`) |
| Teilnahme (update)  | `/api/teilnahmen/{id}`        | —   | —    | ✓   | —      | UC-016: Whitelist-DTO; PARTEI nur eigene (403 sonst), ORGANISATOR alle |
| Konsumationsangebot | `/api/konsumationsangebote`   | ✓   | ✓    | —   | ✓      | — |
| Konsumation         | `/api/konsumationen`          | ✓   | ✓    | —   | ✓      | — |
| Allgemeinausgabe    | `/api/allgemeinausgaben`      | ✓   | ✓    | —   | ✓      | — |
| Abrechnung          | `/api/abrechnungen`           | ✓   | ✓    | —   | ✓      | — |
| Zahlung             | `/api/zahlungen`              | ✓   | ✓    | —   | ✓      | — |
| Mahnung             | `/api/mahnungen`              | ✓   | ✓    | —   | ✓      | — |
| Benutzer            | `/api/benutzer`               | ✓   | ✓    | ✓   | ✓      | UC-015: PUT nur `/api/benutzer/{id}/passwort` (Reset); Duplikat-E-Mail/letzter ORGANISATOR → 409 |
| Auth                | `/api/auth/login`             | —   | ✓    | —   | —      | UC-014: stellt HS256-JWT aus; `permitAll()`; falsche Credentials → 401 |

---

## Bekannte technische Schulden

> Identifiziert durch SonarQube-Analyse 2026-05-01. Details und Massnahmen → `specs/TODO.md`.

| # | Bereich | Befund | Schweregrad | Stand |
|---|---------|--------|-------------|-------|
| AUTH-001 | Sicherheit | Keine Authentifizierung/Autorisierung — alle `/api/**`-Endpunkte offen | CRITICAL | ✅ Behoben 2026-05-09 |
| AUTH-002 | Sicherheit | Login (Eigenbau: `/api/auth/login` + Formular), Rolle PARTEI, Benutzer-Domain, PUT Teilnahme | MAJOR | ✅ Behoben 2026-06-12 (Eigenbau statt Auth0) |
| CORS-001 | Infrastruktur | `allowedOrigins("localhost:4200")` hardcoded, kein Profil-Support | MAJOR | ✅ Behoben 2026-05-09 |
| DEPLOY-001 | Deployment | Kein Spring-Profil für Production | MAJOR | ✅ Behoben 2026-05-09 |
| DEPLOY-002 | Deployment | `localhost:8080` hardcoded in allen 11 Angular-Services — Production-Build zeigt gegen localhost | MAJOR | ✅ Behoben 2026-05-12 |
| PERF-001 | Performance | `FetchType.EAGER` auf `Partei.personen` + `Teilnahme.buffetBeitraege` — N+1-Risiko | MAJOR | ✅ Behoben 2026-07-06 |
| VALID-001 | Validierung | Kein `@Valid` auf Controllern — Pflichtfeldverletzungen liefern HTTP 500 statt 400 | MAJOR | ✅ Behoben 2026-05-12 |
| REFACT-001 | Code-Qualität | 8 Controller + 10 Services mit identischem CRUD-Boilerplate, kein `BaseCrud*` | MINOR | Offen |
| DEPLOY-003 | CI/CD | Kein GitHub Actions Workflow — Tests laufen nur lokal | MAJOR | ✅ Behoben 2026-07-06 |
| TEST-001 | Tests | 13 IT-Klassen duplizieren `setUp()`/`tearDown()`/`setupPost()`/`tryDelete()` | MINOR | Offen |

---

## Traceability

> Automatisch generiert durch Traceability-Manager — Stand: 2026-06-12
> UC-Abdeckung: 14/16 vollständig | 2 mit Lücken (UC-009, UC-011)

### UC × Implementierung × Test

| UC-ID | Titel | Endpunkt(e) | TC-ID(s) | IT-Klasse | Status |
|-------|-------|-------------|----------|-----------|--------|
| UC-001 | Personendaten verwalten | GET/POST/PUT/DELETE `/api/persons` | TC-001, TC-002, TC-029 | PersonVerwaltenIT | ✅ Vollständig |
| UC-002 | Parteien verwalten | GET/POST/PUT/DELETE `/api/parteien` | TC-004, TC-005, TC-030 | ParteiVerwaltenIT | ✅ Vollständig |
| UC-003 | Event anlegen | GET/POST/PUT/DELETE `/api/events` | TC-006, TC-007, TC-031 | EventAnlegenIT | ✅ Vollständig |
| UC-004 | Einladung verwalten | GET/POST/DELETE `/api/einladungen` | TC-008, TC-009, TC-010 | EinladungVerwaltenIT | ✅ Vollständig |
| UC-005 | Teilnahme erfassen | GET/POST/DELETE `/api/teilnahmen` | TC-011, TC-012, TC-033 | TeilnahmeVerwaltenIT | ✅ Vollständig |
| UC-006 | Bestätigung versenden | POST `/api/einladungen` (Upsert, Flag `bestaetigungVersendet`) | TC-013 | BestaetigungVerwaltenIT | ✅ Vollständig |
| UC-007 | Allgemeinausgaben verwalten | GET/POST/DELETE `/api/allgemeinausgaben` | TC-014, TC-015 | AllgemeinausgabeVerwaltenIT | ✅ Vollständig |
| UC-008 | Konsumationsangebot verwalten | GET/POST/DELETE `/api/konsumationsangebote` | TC-016 | KonsumationsangebotVerwaltenIT | ✅ Vollständig |
| UC-009 | Konsumationsliste erstellen | GET `/api/konsumationsangebote`, GET `/api/teilnahmen` | TC-018, TC-019 | KonsumationslisteErstellenIT | ⚠ Teilimpl. |
| UC-010 | Konsumation übernehmen | GET/POST/DELETE `/api/konsumationen` | TC-020, TC-021 | KonsumationUebernehmenIT | ✅ Vollständig |
| UC-011 | Abrechnung erstellen | GET/POST/DELETE `/api/abrechnungen` | TC-022, TC-023 | AbrechnungErstellenIT | ⚠ Teilimpl. |
| UC-012 | Abrechnung zustellen | POST `/api/abrechnungen` (Upsert, Felder `zustellungskanal`, `zustellungsDatum`) | TC-024, TC-025, TC-032 | AbrechnungZustellenIT | ✅ Vollständig |
| UC-013 | Inkasso sicherstellen | GET/POST/DELETE `/api/zahlungen`, `/api/mahnungen` | TC-026, TC-027, TC-028 | InkassoSicherstellenIT | ✅ Vollständig |
| UC-014 | Benutzer anmelden | POST `/api/auth/login` | TC-038, TC-040 | BenutzerAnmeldenIT, SecurityMatrixIT | ✅ Vollständig |
| UC-015 | Benutzer verwalten | GET/POST/DELETE `/api/benutzer`, PUT `/api/benutzer/{id}/passwort` | TC-034, TC-035, TC-039 | BenutzerVerwaltenIT | ✅ Vollständig |
| UC-016 | Teilnahme bestätigen | GET `/api/teilnahmen/meine`, PUT `/api/teilnahmen/{id}` | TC-036, TC-037 | TeilnahmeBestaetigenIT | ✅ Vollständig |

> **Unit-Test-Abdeckung (zusätzlich):** 11 `*ControllerTest.java`-Klassen (`@WebMvcTest`) decken die HTTP-Schicht aller UC-Domänen ab. `ParteiServiceTest` testet die Geschäftslogik von UC-002 (Personenauflösung via `personenIds`). Diese Tests sind nicht TC-gebunden, referenzieren UCs aber via `@DisplayName("UC-XXX: ...")`.

### Offene Traceability-Lücken

- **UC-009** (Konsumationsliste erstellen): Kein dedizierter `GET /api/events/{id}/konsumationsliste`-Endpunkt; Frontend kombiniert Daten clientseitig. → Empfehlung: Endpunkt für Event-spezifische Konsumationsansicht implementieren
- **UC-011** (Abrechnung erstellen): Keine automatische Berechnung von `anteilAllgemeinkosten` / `totalKonsumation`; Werte werden manuell übergeben. → Empfehlung: Berechnungslogik im Service kapseln
- **AUTH-002** (UC-014/015/016): ✅ Behoben 2026-06-12 — Eigenbau-Login, Benutzer-Domain und Teilnahme-Bestätigung vollständig implementiert und getestet (TC-034..TC-040). DEPLOY-003 (CI/CD) ist seit 2026-07-06 ebenfalls behoben
