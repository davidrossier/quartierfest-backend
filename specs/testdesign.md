# Testdesign – Citrus Integration Tests

## Scope

| UC | Covered | Notes |
|---|---|---|
| UC-001 Person verwalten | Yes | TC-001..003 |
| UC-002 Parteien verwalten | Yes | TC-004..005 |
| UC-003 Event anlegen | Yes | TC-006..007 |
| UC-004 Einladung verwalten | Yes | TC-008..010 |
| UC-005 Teilnahme verwalten | Partial | TC-011..012 – kein Auto-Create wenn Einladung auf ANGEMELDET gesetzt wird; Teilnahme wird manuell via POST erstellt (siehe Open Items) |
| UC-006 Bestätigung verwalten | Partial | TC-013 – kein PATCH-Endpunkt; flag nur bei Creation setzbar |
| UC-007 Allgemeinausgabe verwalten | Yes | TC-014..015 |
| UC-008 Konsumationsangebot verwalten | Yes | TC-016..017 |
| UC-009 Konsumationsliste erstellen | Partial | TC-018..019 – kein dedizierter Listengenerierungs-Endpunkt; nur Datenbasis testbar |
| UC-010 Konsumation übernehmen | Yes | TC-020..021 |
| UC-011 Abrechnung erstellen | Partial | TC-022..023 – keine Berechnungslogik im API; manuelle Eingabe aller Felder |
| UC-012 Abrechnung zustellen | Partial | TC-024..025 – kein PATCH für zustellungsDatum/Kanal nach Creation |
| UC-013 Inkasso sicherstellen | Yes | TC-026..028 |

## Test environment assumptions

- Spring Boot App läuft mit `DEFINED_PORT` (Standard: 8080)
- PostgreSQL läuft auf `localhost:5432`, Datenbank `quartierfest`, User `qfuser/qfpass`
- Keine externen Mocks; alle Tests treffen die echte Datenbank
- Tests sind nicht isoliert gegenüber Fremddaten in der DB (keine automatische DB-Bereinigung)
- Testdaten werden in `@BeforeEach` via `RestTemplate` angelegt und in `@AfterEach` bereinigt (Best-Effort)

## Transport strategy

Alle 11 REST-Endpunkte sprechen HTTP/JSON. Kein Messaging-System (Kafka, JMS) vorhanden.

**Transport:** `http().client(baseUrl)` — Citrus HTTP REST Client
**Correlation:** Citrus korreliert Send/Receive über dieselbe `baseUrl`-Instanz pro Testmethode.

## API-Verhalten (beobachtet)

| Aspekt | Verhalten |
|---|---|
| POST – happy path | `200 OK` + Entity-Body |
| POST – DB-Constraint verletzt | `500 Internal Server Error` (kein `@Valid`, keine Controller-Validierung) |
| DELETE | `200 OK`, leerer Body |
| GET | `200 OK` + JSON-Array |
| Fehler-HTTP-Status | Pflichtfeld-Fehler resultieren in `500`, nicht `400` |
| Update-Endpunkte | **Nicht vorhanden** – kein PUT/PATCH für irgendeinen Endpunkt |
| Berechnungslogik | **Nicht vorhanden** – Abrechnung muss manuell mit berechneten Werten gespeichert werden |

## Test cases

### TC-001 – UC-001 Person anlegen: Pflichtfelder vorhanden
- **Source**: UC-001, Hauptfluss
- **Type**: Happy path
- **Given**: Keine Voraussetzung
- **When**: POST `/api/persons` mit `{vorname: "Hans", name: "Müller"}`
- **Then**: HTTP 200, Response-Body enthält `id`
- **Citrus actions**: `send POST /api/persons`, `receive 200`

### TC-002 – UC-001 Person anlegen: Pflichtfeld name fehlt
- **Source**: UC-001, implizites Fehler-Szenario
- **Type**: Error scenario
- **Given**: Keine Voraussetzung
- **When**: POST `/api/persons` ohne Feld `name`
- **Then**: HTTP 500 (DB-Constraint `nullable = false`)
- **Citrus actions**: `send POST /api/persons`, `receive 500`
- **TODO**: Sollte HTTP 400 sein; erfordert `@Valid` + `@NotNull` in Entity/Controller

### TC-003 – UC-001 Person löschen
- **Source**: UC-001, Hauptfluss
- **Type**: Happy path
- **Given**: Eine Person wurde via Setup-RestTemplate angelegt
- **When**: DELETE `/api/persons/{id}`
- **Then**: HTTP 200
- **Citrus actions**: `send DELETE /api/persons/{id}`, `receive 200`

### TC-004 – UC-002 Partei anlegen: Pflichtfelder vorhanden
- **Source**: UC-002, Hauptfluss
- **Type**: Happy path
- **Given**: Keine Voraussetzung
- **When**: POST `/api/parteien` mit `{adresse: "...", twintAktiv: false}`
- **Then**: HTTP 200, Response-Body enthält `id`
- **Citrus actions**: `send POST /api/parteien`, `receive 200`

### TC-005 – UC-002 Partei anlegen: Pflichtfeld adresse fehlt
- **Source**: UC-002, implizites Fehler-Szenario
- **Type**: Error scenario
- **Given**: Keine Voraussetzung
- **When**: POST `/api/parteien` ohne Feld `adresse`
- **Then**: HTTP 500 (DB-Constraint)
- **Citrus actions**: `send POST /api/parteien`, `receive 500`

### TC-006 – UC-003 Event anlegen: Pflichtfelder vorhanden
- **Source**: UC-003, Hauptfluss
- **Type**: Happy path
- **Given**: Keine Voraussetzung
- **When**: POST `/api/events` mit `{datum, startzeit, standort}`
- **Then**: HTTP 200, Response-Body enthält `id`
- **Citrus actions**: `send POST /api/events`, `receive 200`

### TC-007 – UC-003 Event anlegen: Pflichtfeld datum fehlt
- **Source**: UC-003, implizites Fehler-Szenario
- **Type**: Error scenario
- **Given**: Keine Voraussetzung
- **When**: POST `/api/events` ohne Feld `datum`
- **Then**: HTTP 500 (DB-Constraint)
- **Citrus actions**: `send POST /api/events`, `receive 500`

### TC-008 – UC-004 Einladung erstellen (Status OFFEN)
- **Source**: UC-004, Gherkin "Einladungen für alle Parteien erstellen"
- **Type**: Happy path
- **Given**: Event und Partei existieren (Setup)
- **When**: POST `/api/einladungen` mit `{event: {id}, partei: {id}, status: "OFFEN", bestaetigungVersendet: false}`
- **Then**: HTTP 200, Einladung mit Status OFFEN angelegt
- **Citrus actions**: `send POST /api/einladungen`, `receive 200`

### TC-009 – UC-004 Rückmeldung ANGEMELDET erfassen
- **Source**: UC-004, Gherkin "Rückmeldung Anmeldung erfassen"
- **Type**: Happy path
- **Given**: Event und Partei existieren (Setup)
- **When**: POST `/api/einladungen` mit `status: "ANGEMELDET"`, `anzahlPersonen: 3`, `hilftAufstellen: true`, `buffetBeitrag: "SALAT"`
- **Then**: HTTP 200, Response-Body enthält `status: "ANGEMELDET"`
- **Citrus actions**: `send POST /api/einladungen`, `receive 200`

### TC-010 – UC-004 Rückmeldung ABGEMELDET erfassen (A1)
- **Source**: UC-004, Alternative Flow A1
- **Type**: Alternative flow
- **Given**: Event und Partei existieren (Setup)
- **When**: POST `/api/einladungen` mit `status: "ABGEMELDET"`
- **Then**: HTTP 200, Response-Body enthält `status: "ABGEMELDET"`
- **Citrus actions**: `send POST /api/einladungen`, `receive 200`
- **TODO**: UC-004 E1 (Duplikat-Prüfung) ist nicht implementiert; DB hat keinen Unique-Constraint auf `(event, partei)`

### TC-011 – UC-005 Teilnahme erstellen: happy path
- **Source**: UC-005, Hauptfluss
- **Type**: Happy path
- **Given**: Einladung mit Status ANGEMELDET existiert (Setup)
- **When**: POST `/api/teilnahmen` mit `{einladung: {id}, anzahlPersonenEffektiv: 2}`
- **Then**: HTTP 200, Response-Body enthält `id`
- **Citrus actions**: `send POST /api/teilnahmen`, `receive 200`

### TC-012 – UC-005 Teilnahme erstellen: Einladung fehlt
- **Source**: UC-005, implizites Fehler-Szenario
- **Type**: Error scenario
- **Given**: Keine Voraussetzung
- **When**: POST `/api/teilnahmen` mit ungültiger `einladung.id` (999999)
- **Then**: HTTP 500
- **Citrus actions**: `send POST /api/teilnahmen`, `receive 500`

### TC-013 – UC-006 Bestätigung versendet Flag setzen
- **Source**: UC-006, Gherkin "Bestätigung erfolgreich versenden"
- **Type**: Happy path
- **Given**: Event und Partei existieren (Setup)
- **When**: POST `/api/einladungen` mit `bestaetigungVersendet: true`
- **Then**: HTTP 200, Response-Body enthält `bestaetigungVersendet: true`
- **Citrus actions**: `send POST /api/einladungen`, `receive 200`
- **TODO**: UC-006 erfordert Aktualisierung des Flags nach Creation. Kein PATCH-Endpunkt vorhanden. Test prüft nur, dass das Flag bei Creation gespeichert wird.

### TC-014 – UC-007 Allgemeinausgabe anlegen: happy path
- **Source**: UC-007, Hauptfluss
- **Type**: Happy path
- **Given**: Event existiert (Setup)
- **When**: POST `/api/allgemeinausgaben` mit `{event: {id}, beschreibung: "...", betrag: "50.00"}`
- **Then**: HTTP 200
- **Citrus actions**: `send POST /api/allgemeinausgaben`, `receive 200`

### TC-015 – UC-007 Allgemeinausgabe anlegen: Pflichtfeld betrag fehlt
- **Source**: UC-007, Error Scenario E2
- **Type**: Error scenario
- **Given**: Event existiert (Setup)
- **When**: POST `/api/allgemeinausgaben` ohne Feld `betrag`
- **Then**: HTTP 500
- **Citrus actions**: `send POST /api/allgemeinausgaben`, `receive 500`

### TC-016 – UC-008 Konsumationsangebot anlegen: happy path
- **Source**: UC-008, Hauptfluss
- **Type**: Happy path
- **Given**: Event existiert (Setup)
- **When**: POST `/api/konsumationsangebote` mit `{event: {id}, bezeichnung: "Bier 5dl", preis: "3.00"}`
- **Then**: HTTP 200
- **Citrus actions**: `send POST /api/konsumationsangebote`, `receive 200`

### TC-017 – UC-008 Konsumationsangebot löschen
- **Source**: UC-008, Hauptfluss
- **Type**: Happy path
- **Given**: Konsumationsangebot existiert (Setup)
- **When**: DELETE `/api/konsumationsangebote/{id}`
- **Then**: HTTP 200
- **Citrus actions**: `send DELETE /api/konsumationsangebote/{id}`, `receive 200`

### TC-018 – UC-009 Konsumationsangebote für Event abrufbar
- **Source**: UC-009, Hauptfluss Step 2 (X-Achse der Liste)
- **Type**: Happy path
- **Given**: Event mit Konsumationsangeboten existiert (Setup)
- **When**: GET `/api/konsumationsangebote`
- **Then**: HTTP 200, Response-Array nicht leer
- **Citrus actions**: `send GET /api/konsumationsangebote`, `receive 200`
- **TODO**: Kein Endpunkt `/api/konsumationsangebote?eventId={id}` – GET gibt alle zurück, kein Event-Filter

### TC-019 – UC-009 Teilnahmen für Event abrufbar
- **Source**: UC-009, Hauptfluss Step 2 (Y-Achse der Liste)
- **Type**: Happy path
- **Given**: Event mit Teilnahmen existiert (Setup)
- **When**: GET `/api/teilnahmen`
- **Then**: HTTP 200, Response-Array nicht leer
- **Citrus actions**: `send GET /api/teilnahmen`, `receive 200`
- **TODO**: Kein dedizierter Konsumationslisten-Endpunkt (z.B. GET `/api/events/{id}/konsumationsliste`)

### TC-020 – UC-010 Konsumation erfassen: happy path
- **Source**: UC-010, Gherkin "Konsumation korrekt erfassen"
- **Type**: Happy path
- **Given**: Teilnahme und Konsumationsangebot existieren (Setup)
- **When**: POST `/api/konsumationen` mit `{teilnahme: {id}, konsumationsangebot: {id}, anzahl: 3}`
- **Then**: HTTP 200
- **Citrus actions**: `send POST /api/konsumationen`, `receive 200`

### TC-021 – UC-010 Konsumation erfassen: anzahl fehlt
- **Source**: UC-010, Error Scenario E1
- **Type**: Error scenario
- **Given**: Teilnahme und Konsumationsangebot existieren (Setup)
- **When**: POST `/api/konsumationen` ohne Feld `anzahl`
- **Then**: HTTP 500
- **Citrus actions**: `send POST /api/konsumationen`, `receive 500`

### TC-022 – UC-011 Abrechnung erstellen: happy path
- **Source**: UC-011, Hauptfluss
- **Type**: Happy path
- **Given**: Teilnahme existiert (Setup)
- **When**: POST `/api/abrechnungen` mit `{teilnahme: {id}, anteilAllgemeinkosten: "40.00", totalKonsumation: "17.00", totalBetrag: "57.00", zustellungskanal: "EMAIL"}`
- **Then**: HTTP 200, `totalBetrag: "57.00"` in Response
- **Citrus actions**: `send POST /api/abrechnungen`, `receive 200`
- **TODO**: Keine Berechnungslogik im API; Beträge werden manuell übergeben

### TC-023 – UC-011 Abrechnung erstellen: Teilnahme fehlt
- **Source**: UC-011, Error Scenario E1
- **Type**: Error scenario
- **Given**: Keine Voraussetzung
- **When**: POST `/api/abrechnungen` mit ungültiger `teilnahme.id` (999999)
- **Then**: HTTP 500
- **Citrus actions**: `send POST /api/abrechnungen`, `receive 500`

### TC-024 – UC-012 Abrechnung zustellen: Kanal EMAIL
- **Source**: UC-012, Gherkin "Abrechnung via E-Mail zustellen"
- **Type**: Happy path
- **Given**: Teilnahme existiert (Setup)
- **When**: POST `/api/abrechnungen` mit `zustellungskanal: "EMAIL"` und `zustellungsDatum: "2025-07-10"`
- **Then**: HTTP 200, `zustellungskanal: "EMAIL"` in Response
- **Citrus actions**: `send POST /api/abrechnungen`, `receive 200`
- **TODO**: UC-012 erfordert nachträgliches Setzen von `zustellungsDatum`. Kein PATCH vorhanden.

### TC-025 – UC-012 Abrechnung zustellen: Kanal TWINT
- **Source**: UC-012, Gherkin "Abrechnung via Twint-Link zustellen"
- **Type**: Happy path
- **Given**: Teilnahme existiert (Setup)
- **When**: POST `/api/abrechnungen` mit `zustellungskanal: "TWINT"`
- **Then**: HTTP 200, `zustellungskanal: "TWINT"` in Response
- **Citrus actions**: `send POST /api/abrechnungen`, `receive 200`

### TC-026 – UC-013 TWINT-Zahlung erfassen
- **Source**: UC-013, Gherkin "Twint-Zahlung erfolgreich erfassen"
- **Type**: Happy path
- **Given**: Abrechnung existiert (Setup)
- **When**: POST `/api/zahlungen` mit `{abrechnung: {id}, zahlungskanal: "TWINT", datum: "2025-07-15", betrag: "57.00"}`
- **Then**: HTTP 200
- **Citrus actions**: `send POST /api/zahlungen`, `receive 200`

### TC-027 – UC-013 Zahlung ohne Datum
- **Source**: UC-013, Error Scenario E1 (Pflichtfeld fehlt)
- **Type**: Error scenario
- **Given**: Abrechnung existiert (Setup)
- **When**: POST `/api/zahlungen` ohne Feld `datum`
- **Then**: HTTP 500
- **Citrus actions**: `send POST /api/zahlungen`, `receive 500`

### TC-028 – UC-013 Mahnung erfassen
- **Source**: UC-013, Gherkin "Mahnung erfassen"
- **Type**: Happy path
- **Given**: Abrechnung existiert (Setup)
- **When**: POST `/api/mahnungen` mit `{abrechnung: {id}, datum: "2025-07-20"}`
- **Then**: HTTP 200
- **Citrus actions**: `send POST /api/mahnungen`, `receive 200`

---

## Open items

- [ ] **Keine Controller-Validierung**: Alle POST-Fehlerszenarien liefern HTTP 500 statt 400. Für korrekte Fehlerbehandlung müssen `@Valid` + Bean-Validation-Annotationen ergänzt werden (TC-002, TC-005, TC-007, TC-012, TC-015, TC-021, TC-023, TC-027).
- [ ] **Kein PATCH**: UC-006 (bestaetigungVersendet), UC-012 (zustellungsDatum) und UC-005 (anzahlPersonenEffektiv nachträglich setzen) erfordern PATCH-Endpunkte (TC-013, TC-024, TC-025).
- [ ] **Keine Berechnungslogik-API**: UC-011 Abrechnungsberechnung ist nicht im API implementiert (TC-022, TC-023).
- [ ] **Kein Konsumationslisten-Endpunkt**: UC-009 hat keinen dedizierten Endpunkt für die Listenansicht (TC-018, TC-019).
- [ ] **Kein Event-Filter auf Collections**: Kein `?eventId=` Query-Parameter auf `/api/konsumationsangebote`, `/api/teilnahmen` etc.
- [ ] **Citrus 4.9.4 + Spring Boot 4.x Kompatibilität**: Prüfen ob Citrus 4.9.4 mit Spring Boot 4.0.x / Spring Framework 7.x kompatibel ist.
- [ ] **pom.xml**: Citrus BOM fehlt (siehe Snippet unten).
- [ ] **UC-005 Teilnahme-Erzeugungsmodell (neu, aus UC-Review 2026-03-27)**: TC-011 testet explizites `POST /api/teilnahmen`. Falls das Design auf Auto-Create beim Status-Wechsel ANGEMELDET umgestellt wird (UC-005 Open Item), wird TC-011 hinfällig und muss durch einen Test ersetzt werden, der prüft, dass nach `POST /api/einladungen` mit `status=ANGEMELDET` automatisch eine Teilnahme in `GET /api/teilnahmen` erscheint. Entscheid abwarten bevor TC-011 geändert wird.
