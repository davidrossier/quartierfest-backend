# Testdesign – Citrus Integration Tests

## Scope

| UC | Covered | Notes |
|---|---|---|
| UC-001 Person verwalten | Yes | TC-001..002, TC-029 |
| UC-002 Parteien verwalten | Yes | TC-004..005, TC-030 |
| UC-003 Event anlegen | Yes | TC-006..007, TC-031 — inkl. `PUT /api/events/{id}` bearbeiten |
| UC-004 Einladung verwalten | Yes | TC-008..010 |
| UC-005 Teilnahme verwalten | Yes | TC-011..012, TC-033 – explizite Erstellung via POST bestätigt; kein Auto-Create gewünscht; mehrere Buffet-Beiträge je Teilnahme |
| UC-006 Bestätigung verwalten | Yes | TC-013 – `bestaetigungVersendet` via POST/Upsert nachträglich setzbar; kein PATCH benötigt |
| UC-007 Allgemeinausgabe verwalten | Yes | TC-014..015 |
| UC-008 Konsumationsangebot verwalten | Yes | TC-016 |
| UC-009 Konsumationsliste erstellen | Partial | TC-018..019 – kein dedizierter Listengenerierungs-Endpunkt; nur Datenbasis testbar |
| UC-010 Konsumation übernehmen | Yes | TC-020..021 |
| UC-011 Abrechnung erstellen | Partial | TC-022..023 – keine Berechnungslogik im API; manuelle Eingabe aller Felder |
| UC-012 Abrechnung zustellen | Yes | TC-024..025, TC-032 – `zustellungsDatum` und Kanal via POST/Upsert nachträglich setzbar |
| UC-013 Inkasso sicherstellen | Yes | TC-026..028 |

**Hinweis:** TC-003 (Person löschen) ist in TC-001 integriert. TC-017 (Konsumationsangebot löschen) ist in TC-016 integriert. Die Löschung erfolgt als letzter Schritt des jeweiligen Happy-Path-Tests (Lösch-Test).

## Test environment assumptions

- Spring Boot App läuft mit `DEFINED_PORT` (Standard: 8080)
- PostgreSQL läuft auf `localhost:5432`, Datenbank `quartierfest`, User `qfuser/qfpass`
- Keine externen Mocks; alle Tests treffen die echte Datenbank
- Tests sind nicht isoliert gegenüber Fremddaten in der DB (keine automatische DB-Bereinigung zwischen Test-Runs)

## Cleanup-Strategie

Jeder Happy-Path-Test, der Testdaten anlegt, löscht diese **innerhalb des Tests** via expliziter DELETE-Assertion (Lösch-Test). Der Cleanup ist damit gleichzeitig ein Testfall für die DELETE-Funktionalität der jeweiligen Ressource.

```
POST /api/xxx            → assert 200 + id
DELETE /api/xxx/{id}     → assert 200          ← Lösch-Test + Cleanup
```

`@BeforeEach`-Fixtures (Voraussetzungen) werden im `@AfterEach` in umgekehrter FK-Reihenfolge bereinigt — erst nachdem alle vom Test angelegten Datensätze bereits im Test selbst gelöscht wurden:

```
zahlung / mahnung / konsumation  →  abrechnung  →  konsumationsangebot / allgemeinausgabe
→  teilnahme  →  einladung  →  person  →  partei  →  event
```

Für einen einmaligen DB-Reset nach Altdaten (z.B. aus früheren Test-Runs):
```bash
psql -U qfuser -d quartierfest -f src/test/resources/cleanup-testdata.sql
```

## Transport strategy

Alle 11 REST-Endpunkte sprechen HTTP/JSON. Kein Messaging-System (Kafka, JMS) vorhanden.

**Transport:** `RestTemplate` mit no-op `ResponseErrorHandler` (wirft nie bei 4xx/5xx)
**Correlation:** `http` (Testaufruf) vs. `setup` (Voraussetzungen) — zwei separate RestTemplate-Instanzen pro Test.

## API-Verhalten (beobachtet)

| Aspekt | Verhalten |
|---|---|
| POST – happy path | `200 OK` + Entity-Body |
| POST – DB-Constraint verletzt | `500 Internal Server Error` (kein `@Valid`, keine Controller-Validierung) |
| DELETE | `200 OK`, leerer Body |
| GET | `200 OK` + JSON-Array |
| Fehler-HTTP-Status | Pflichtfeld-Fehler resultieren in `500`, nicht `400` |
| Update-Endpunkte | `PUT /api/persons/{id}`, `PUT /api/parteien/{id}` und `PUT /api/events/{id}` vorhanden; kein PUT auf anderen Ressourcen |
| Upsert via POST | `POST /api/einladungen` und `POST /api/abrechnungen` mit `id` im Body agieren als Upsert (JPA `save()`) |
| Berechnungslogik | **Nicht vorhanden** – Abrechnung muss manuell mit berechneten Werten gespeichert werden |

## Test cases

### TC-001 – UC-001 Person anlegen und löschen
- **Source**: UC-001, Hauptfluss (inkl. ehem. TC-003 Person löschen)
- **Type**: Happy path + Lösch-Test
- **Given**: Keine Voraussetzung
- **When**: POST `/api/persons` mit `{vorname: "Hans", name: "Müller"}`, dann DELETE `/api/persons/{id}`
- **Then**: POST → HTTP 200 + id; DELETE → HTTP 200
- **Citrus actions**: `send POST /api/persons`, `receive 200`, `send DELETE /api/persons/{id}`, `receive 200`

### TC-029 – UC-001 Person bearbeiten
- **Source**: UC-001, Hauptfluss Schritt 3 (Bearbeiten)
- **Type**: Happy path + Lösch-Test
- **Given**: Keine Voraussetzung
- **When**: POST `/api/persons` (Setup), dann PUT `/api/persons/{id}` mit `{vorname: "Anna", name: "Müller", mobilenummer: "+41791234567"}`, dann DELETE
- **Then**: PUT → HTTP 200, `mobilenummer: "+41791234567"` in Response; DELETE → HTTP 200
- **Citrus actions**: `send PUT /api/persons/{id}`, `receive 200`, `send DELETE /api/persons/{id}`, `receive 200`

### TC-002 – UC-001 Person anlegen: Pflichtfeld name fehlt
- **Source**: UC-001, implizites Fehler-Szenario
- **Type**: Error scenario
- **Given**: Keine Voraussetzung
- **When**: POST `/api/persons` ohne Feld `name`
- **Then**: HTTP 500 (DB-Constraint `nullable = false`)
- **Citrus actions**: `send POST /api/persons`, `receive 500`
- **TODO**: Sollte HTTP 400 sein; erfordert `@Valid` + `@NotNull` in Entity/Controller

### TC-004 – UC-002 Partei anlegen und löschen
- **Source**: UC-002, Hauptfluss
- **Type**: Happy path + Lösch-Test
- **Given**: Keine Voraussetzung
- **When**: POST `/api/parteien` mit `{bezeichnung: "Familie Müller", adresse: "...", twintAktiv: false}`, dann DELETE `/api/parteien/{id}`
- **Then**: POST → HTTP 200 + id + `bezeichnung: "Familie Müller"`; DELETE → HTTP 200
- **Citrus actions**: `send POST /api/parteien`, `receive 200`, `send DELETE /api/parteien/{id}`, `receive 200`

### TC-005 – UC-002 Partei anlegen: Pflichtfeld adresse fehlt
- **Source**: UC-002, Error Scenario E1
- **Type**: Error scenario
- **Given**: Keine Voraussetzung
- **When**: POST `/api/parteien` ohne Feld `adresse`
- **Then**: HTTP 500 (DB-Constraint)
- **Citrus actions**: `send POST /api/parteien`, `receive 500`

### TC-030 – UC-002 Partei anlegen: Pflichtfeld bezeichnung fehlt
- **Source**: UC-002, Error Scenario E1
- **Type**: Error scenario
- **Given**: Keine Voraussetzung
- **When**: POST `/api/parteien` ohne Feld `bezeichnung`
- **Then**: HTTP 500 (DB-Constraint)
- **Citrus actions**: `send POST /api/parteien`, `receive 500`
- **TODO**: Sollte HTTP 400 sein; erfordert `@Valid` + `@NotBlank` auf `Partei.bezeichnung`

### TC-006 – UC-003 Event anlegen und löschen
- **Source**: UC-003, Hauptfluss
- **Type**: Happy path + Lösch-Test
- **Given**: Keine Voraussetzung
- **When**: POST `/api/events` mit `{datum, startzeit, standort}`, dann DELETE `/api/events/{id}`
- **Then**: POST → HTTP 200 + id; DELETE → HTTP 200
- **Citrus actions**: `send POST /api/events`, `receive 200`, `send DELETE /api/events/{id}`, `receive 200`

### TC-007 – UC-003 Event anlegen: Pflichtfeld datum fehlt
- **Source**: UC-003, implizites Fehler-Szenario
- **Type**: Error scenario
- **Given**: Keine Voraussetzung
- **When**: POST `/api/events` ohne Feld `datum`
- **Then**: HTTP 500 (DB-Constraint)
- **Citrus actions**: `send POST /api/events`, `receive 500`

### TC-008 – UC-004 Einladung erstellen (Status OFFEN) und löschen
- **Source**: UC-004, Gherkin "Einladungen für alle Parteien erstellen"
- **Type**: Happy path + Lösch-Test
- **Given**: Event und Partei existieren (Setup)
- **When**: POST `/api/einladungen` mit `{event: {id}, partei: {id}, status: "OFFEN", bestaetigungVersendet: false}`, dann DELETE
- **Then**: POST → HTTP 200 + id; DELETE → HTTP 200
- **Citrus actions**: `send POST /api/einladungen`, `receive 200`, `send DELETE /api/einladungen/{id}`, `receive 200`

### TC-009 – UC-004 Rückmeldung ANGEMELDET erfassen und löschen
- **Source**: UC-004, Gherkin "Rückmeldung Anmeldung erfassen"
- **Type**: Happy path + Lösch-Test
- **Given**: Event und Partei existieren (Setup)
- **When**: POST `/api/einladungen` mit `status: "ANGEMELDET"`, `anzahlPersonen: 3`, `hilftAufstellen: true`, `buffetBeitrag: "SALAT"`, dann DELETE
- **Then**: POST → HTTP 200, `status: "ANGEMELDET"` in Response; DELETE → HTTP 200
- **Citrus actions**: `send POST /api/einladungen`, `receive 200`, `send DELETE /api/einladungen/{id}`, `receive 200`

### TC-010 – UC-004 Rückmeldung ABGEMELDET erfassen und löschen (A1)
- **Source**: UC-004, Alternative Flow A1
- **Type**: Alternative flow + Lösch-Test
- **Given**: Event und Partei existieren (Setup)
- **When**: POST `/api/einladungen` mit `status: "ABGEMELDET"`, dann DELETE
- **Then**: POST → HTTP 200, `status: "ABGEMELDET"`; DELETE → HTTP 200
- **Citrus actions**: `send POST /api/einladungen`, `receive 200`, `send DELETE /api/einladungen/{id}`, `receive 200`
- **TODO**: UC-004 E1 (Duplikat-Prüfung) ist nicht implementiert; DB hat keinen Unique-Constraint auf `(event, partei)`

### TC-011 – UC-005 Teilnahme erstellen und löschen: happy path
- **Source**: UC-005, Hauptfluss
- **Type**: Happy path + Lösch-Test
- **Given**: Einladung mit Status ANGEMELDET existiert (Setup)
- **When**: POST `/api/teilnahmen` mit `{einladung: {id}, anzahlPersonenEffektiv: 2}`, dann DELETE
- **Then**: POST → HTTP 200 + id; DELETE → HTTP 200
- **Citrus actions**: `send POST /api/teilnahmen`, `receive 200`, `send DELETE /api/teilnahmen/{id}`, `receive 200`

### TC-012 – UC-005 Teilnahme erstellen: Einladung fehlt
- **Source**: UC-005, implizites Fehler-Szenario
- **Type**: Error scenario
- **Given**: Keine Voraussetzung
- **When**: POST `/api/teilnahmen` mit ungültiger `einladung.id` (999999)
- **Then**: HTTP 500
- **Citrus actions**: `send POST /api/teilnahmen`, `receive 500`

### TC-033 – UC-005 Teilnahme mit mehreren Buffet-Beiträgen erstellen
- **Source**: UC-005, neues Acceptance Criterion "Mehrere Buffet-Beiträge erfassen"
- **Type**: Happy path + Lösch-Test
- **Given**: Einladung mit Status ANGEMELDET existiert (Setup)
- **When**: POST `/api/teilnahmen` mit `buffetBeitraege: [{art: "SALAT", beschreibung: "Grüner Salat"}, {art: "DESSERT", beschreibung: "Mousse au chocolat"}, {art: "WEITERE", beschreibung: "Baguette"}]`, dann DELETE
- **Then**: POST → HTTP 200 + id + `buffetBeitraege` hat 3 Einträge; DELETE → HTTP 200
- **Citrus actions**: `send POST /api/teilnahmen`, `receive 200`, verify `buffetBeitraege.size == 3`, `send DELETE /api/teilnahmen/{id}`, `receive 200`

### TC-013 – UC-006 Bestätigung versendet via POST/Upsert nachträglich setzen
- **Source**: UC-006, Gherkin "Bestätigung erfolgreich versenden"
- **Type**: Happy path (vollständiger UC-Fluss) + Lösch-Test
- **Given**: Event und Partei existieren (Setup)
- **When**: POST `/api/einladungen` mit `bestaetigungVersendet: false` (Initial), dann POST mit gleicher `id` und `bestaetigungVersendet: true` (Upsert), dann DELETE
- **Then**: Initial → HTTP 200, `bestaetigungVersendet: false`; Upsert → HTTP 200, `bestaetigungVersendet: true`; DELETE → HTTP 200
- **Citrus actions**: `send POST`, `receive 200`, `send POST (Upsert)`, `receive 200`, `send DELETE`, `receive 200`

### TC-014 – UC-007 Allgemeinausgabe anlegen und löschen: happy path
- **Source**: UC-007, Hauptfluss
- **Type**: Happy path + Lösch-Test
- **Given**: Event existiert (Setup)
- **When**: POST `/api/allgemeinausgaben` mit `{event: {id}, beschreibung: "...", betrag: "50.00"}`, dann DELETE
- **Then**: POST → HTTP 200 + id; DELETE → HTTP 200
- **Citrus actions**: `send POST /api/allgemeinausgaben`, `receive 200`, `send DELETE /api/allgemeinausgaben/{id}`, `receive 200`

### TC-015 – UC-007 Allgemeinausgabe anlegen: Pflichtfeld betrag fehlt
- **Source**: UC-007, Error Scenario E2
- **Type**: Error scenario
- **Given**: Event existiert (Setup)
- **When**: POST `/api/allgemeinausgaben` ohne Feld `betrag`
- **Then**: HTTP 500
- **Citrus actions**: `send POST /api/allgemeinausgaben`, `receive 500`

### TC-016 – UC-008 Konsumationsangebot anlegen und löschen
- **Source**: UC-008, Hauptfluss (inkl. ehem. TC-017 Konsumationsangebot löschen)
- **Type**: Happy path + Lösch-Test
- **Given**: Event existiert (Setup)
- **When**: POST `/api/konsumationsangebote` mit `{event: {id}, bezeichnung: "Bier 5dl", preis: "3.00"}`, dann DELETE
- **Then**: POST → HTTP 200 + id; DELETE → HTTP 200
- **Citrus actions**: `send POST /api/konsumationsangebote`, `receive 200`, `send DELETE /api/konsumationsangebote/{id}`, `receive 200`

### TC-018 – UC-009 Konsumationsangebote für Event abrufbar (X-Achse)
- **Source**: UC-009, Hauptfluss Step 2 (X-Achse der Liste)
- **Type**: Happy path
- **Given**: Event mit Konsumationsangeboten existiert (Setup)
- **When**: GET `/api/konsumationsangebote`
- **Then**: HTTP 200, Response-Array nicht leer
- **Citrus actions**: `send GET /api/konsumationsangebote`, `receive 200`
- **TODO**: Kein Endpunkt `/api/konsumationsangebote?eventId={id}` – GET gibt alle zurück, kein Event-Filter

### TC-019 – UC-009 Teilnahmen für Event abrufbar und löschen (Y-Achse)
- **Source**: UC-009, Hauptfluss Step 2 (Y-Achse der Liste)
- **Type**: Happy path + Lösch-Test
- **Given**: Event mit Einladung (ANGEMELDET) existiert (Setup)
- **When**: POST `/api/teilnahmen`, dann GET `/api/teilnahmen`, dann DELETE `/api/teilnahmen/{id}`
- **Then**: GET → HTTP 200, nicht leer; DELETE → HTTP 200
- **Citrus actions**: `send GET /api/teilnahmen`, `receive 200`, `send DELETE /api/teilnahmen/{id}`, `receive 200`
- **TODO**: Kein dedizierter Konsumationslisten-Endpunkt (z.B. GET `/api/events/{id}/konsumationsliste`)

### TC-020 – UC-010 Konsumation erfassen und löschen: happy path
- **Source**: UC-010, Gherkin "Konsumation korrekt erfassen"
- **Type**: Happy path + Lösch-Test
- **Given**: Teilnahme und Konsumationsangebot existieren (Setup)
- **When**: POST `/api/konsumationen` mit `{teilnahme: {id}, konsumationsangebot: {id}, anzahl: 3}`, dann DELETE
- **Then**: POST → HTTP 200 + id; DELETE → HTTP 200
- **Citrus actions**: `send POST /api/konsumationen`, `receive 200`, `send DELETE /api/konsumationen/{id}`, `receive 200`

### TC-021 – UC-010 Konsumation erfassen: anzahl fehlt
- **Source**: UC-010, Error Scenario E1
- **Type**: Error scenario
- **Given**: Teilnahme und Konsumationsangebot existieren (Setup)
- **When**: POST `/api/konsumationen` ohne Feld `anzahl`
- **Then**: HTTP 500
- **Citrus actions**: `send POST /api/konsumationen`, `receive 500`

### TC-022 – UC-011 Abrechnung erstellen und löschen: happy path
- **Source**: UC-011, Hauptfluss
- **Type**: Happy path + Lösch-Test
- **Given**: Teilnahme existiert (Setup)
- **When**: POST `/api/abrechnungen` mit `{teilnahme: {id}, anteilAllgemeinkosten: "40.00", totalKonsumation: "17.00", totalBetrag: "57.00", zustellungskanal: "EMAIL"}`, dann DELETE
- **Then**: POST → HTTP 200, `totalBetrag: "57.00"` in Response; DELETE → HTTP 200
- **Citrus actions**: `send POST /api/abrechnungen`, `receive 200`, `send DELETE /api/abrechnungen/{id}`, `receive 200`
- **TODO**: Keine Berechnungslogik im API; Beträge werden manuell übergeben

### TC-023 – UC-011 Abrechnung erstellen: Teilnahme fehlt
- **Source**: UC-011, Error Scenario E1
- **Type**: Error scenario
- **Given**: Keine Voraussetzung
- **When**: POST `/api/abrechnungen` mit ungültiger `teilnahme.id` (999999)
- **Then**: HTTP 500
- **Citrus actions**: `send POST /api/abrechnungen`, `receive 500`

### TC-024 – UC-012 Abrechnung zustellen und löschen: Kanal EMAIL
- **Source**: UC-012, Gherkin "Abrechnung via E-Mail zustellen"
- **Type**: Happy path + Lösch-Test
- **Given**: Teilnahme existiert (Setup)
- **When**: POST `/api/abrechnungen` mit `zustellungskanal: "EMAIL"` und `zustellungsDatum: "2025-07-10"`, dann DELETE
- **Then**: POST → HTTP 200, `zustellungskanal: "EMAIL"` in Response; DELETE → HTTP 200
- **Citrus actions**: `send POST /api/abrechnungen`, `receive 200`, `send DELETE /api/abrechnungen/{id}`, `receive 200`
- **TODO**: UC-012 erfordert nachträgliches Setzen von `zustellungsDatum`. Kein PATCH vorhanden.

### TC-025 – UC-012 Abrechnung zustellen und löschen: Kanal TWINT
- **Source**: UC-012, Gherkin "Abrechnung via Twint-Link zustellen"
- **Type**: Happy path + Lösch-Test
- **Given**: Teilnahme existiert (Setup)
- **When**: POST `/api/abrechnungen` mit `zustellungskanal: "TWINT"`, dann DELETE
- **Then**: POST → HTTP 200, `zustellungskanal: "TWINT"` in Response; DELETE → HTTP 200
- **Citrus actions**: `send POST /api/abrechnungen`, `receive 200`, `send DELETE /api/abrechnungen/{id}`, `receive 200`

### TC-032 – UC-012 Zustellungsdatum nachträglich via POST/Upsert setzen
- **Source**: UC-012, Hauptfluss Schritt 4–5 (Zustellungsdatum setzen)
- **Type**: Happy path (vollständiger UC-Fluss) + Lösch-Test
- **Given**: Teilnahme existiert (Setup)
- **When**: POST `/api/abrechnungen` ohne `zustellungsDatum` (Initial), dann POST mit gleicher `id` und `zustellungsDatum: "2025-07-10"` (Upsert), dann DELETE
- **Then**: Initial → HTTP 200, `zustellungsDatum: null`; Upsert → HTTP 200, `zustellungsDatum: "2025-07-10"`; DELETE → HTTP 200
- **Citrus actions**: `send POST`, `receive 200`, `send POST (Upsert)`, `receive 200`, `send DELETE`, `receive 200`

### TC-026 – UC-013 TWINT-Zahlung erfassen und löschen
- **Source**: UC-013, Gherkin "Twint-Zahlung erfolgreich erfassen"
- **Type**: Happy path + Lösch-Test
- **Given**: Abrechnung existiert (Setup)
- **When**: POST `/api/zahlungen` mit `{abrechnung: {id}, zahlungskanal: "TWINT", datum: "2025-07-15", betrag: "57.00"}`, dann DELETE
- **Then**: POST → HTTP 200 + id; DELETE → HTTP 200
- **Citrus actions**: `send POST /api/zahlungen`, `receive 200`, `send DELETE /api/zahlungen/{id}`, `receive 200`

### TC-027 – UC-013 Zahlung ohne Datum
- **Source**: UC-013, Error Scenario E1 (Pflichtfeld fehlt)
- **Type**: Error scenario
- **Given**: Abrechnung existiert (Setup)
- **When**: POST `/api/zahlungen` ohne Feld `datum`
- **Then**: HTTP 500
- **Citrus actions**: `send POST /api/zahlungen`, `receive 500`

### TC-028 – UC-013 Mahnung erfassen und löschen
- **Source**: UC-013, Gherkin "Mahnung erfassen"
- **Type**: Happy path + Lösch-Test
- **Given**: Abrechnung existiert (Setup)
- **When**: POST `/api/mahnungen` mit `{abrechnung: {id}, datum: "2025-07-20"}`, dann DELETE
- **Then**: POST → HTTP 200 + id; DELETE → HTTP 200
- **Citrus actions**: `send POST /api/mahnungen`, `receive 200`, `send DELETE /api/mahnungen/{id}`, `receive 200`

---

## Traceability-Status

> Automatisch generiert durch Traceability-Manager — Stand: 2026-05-01

### TC × IT-Implementierung

| TC-ID | DisplayName | IT-Klasse | Methode | Vorhanden |
|-------|-------------|-----------|---------|-----------|
| TC-001 | UC-001 Person anlegen und löschen | PersonVerwaltenIT | tc001_personAnlegen | ✅ |
| TC-002 | UC-001 Person ohne Name wird abgelehnt | PersonVerwaltenIT | tc002_personOhneName | ✅ |
| TC-004 | UC-002 Partei anlegen und löschen | ParteiVerwaltenIT | tc004_parteiAnlegen | ✅ |
| TC-005 | UC-002 Partei ohne Adresse wird abgelehnt | ParteiVerwaltenIT | tc005_parteiOhneAdresse | ✅ |
| TC-006 | UC-003 Event anlegen und löschen | EventAnlegenIT | tc006_eventAnlegen | ✅ |
| TC-007 | UC-003 Event ohne Standort wird abgelehnt | EventAnlegenIT | tc007_eventOhneStandort | ✅ |
| TC-008 | UC-004 Einladung anlegen und löschen | EinladungVerwaltenIT | tc008_einladungAnlegen | ✅ |
| TC-009 | UC-004 Einladung ohne Event wird abgelehnt | EinladungVerwaltenIT | tc009_einladungOhneEvent | ✅ |
| TC-010 | UC-004 Einladungsstatus ändern | EinladungVerwaltenIT | tc010_einladungsstatusAendern | ✅ |
| TC-011 | UC-005 Teilnahme erfassen und löschen | TeilnahmeVerwaltenIT | tc011_teilnahmeErstellenHappyPath | ✅ |
| TC-012 | UC-005 Teilnahme ohne Einladung wird abgelehnt | TeilnahmeVerwaltenIT | tc012_teilnahmeErstellenEinladungFehlt | ✅ |
| TC-033 | UC-005 Teilnahme mit mehreren Buffet-Beiträgen | TeilnahmeVerwaltenIT | tc033_teilnahmeErstellenMehrereBeitraege | ✅ |
| TC-013 | UC-006 Bestätigung versendet via POST/Upsert | BestaetigungVerwaltenIT | tc013_bestaetigungVersendetViаUpsert | ✅ |
| TC-014 | UC-007 Allgemeinausgabe anlegen und löschen | AllgemeinausgabeVerwaltenIT | tc014_allgemeinausgabeAnlegen | ✅ |
| TC-015 | UC-007 Allgemeinausgabe ohne Beschreibung wird abgelehnt | AllgemeinausgabeVerwaltenIT | tc015_allgemeinausgabeOhneBeschreibung | ✅ |
| TC-016 | UC-008 Konsumationsangebot anlegen und löschen | KonsumationsangebotVerwaltenIT | tc016_konsumationsangebotAnlegen | ✅ |
| TC-018 | UC-009 Konsumationsliste lesen | KonsumationslisteErstellenIT | tc018_konsumationslisteLesenAngebote | ✅ |
| TC-019 | UC-009 Teilnahmen für Konsumation lesen | KonsumationslisteErstellenIT | tc019_konsumationslisteTeilnahmen | ✅ |
| TC-020 | UC-010 Konsumation erfassen und löschen | KonsumationUebernehmenIT | tc020_konsumationErfassen | ✅ |
| TC-021 | UC-010 Konsumation ohne Anzahl wird abgelehnt | KonsumationUebernehmenIT | tc021_konsumationOhneAnzahl | ✅ |
| TC-022 | UC-011 Abrechnung erstellen und löschen | AbrechnungErstellenIT | tc022_abrechnungErstellen | ✅ |
| TC-023 | UC-011 Abrechnung ohne Betrag wird abgelehnt | AbrechnungErstellenIT | tc023_abrechnungOhneBetrag | ✅ |
| TC-024 | UC-012 Abrechnung zustellen: Kanal EMAIL | AbrechnungZustellenIT | tc024_abrechnungZustellenEmail | ✅ |
| TC-025 | UC-012 Abrechnung zustellen: Kanal TWINT | AbrechnungZustellenIT | tc025_abrechnungZustellenTwint | ✅ |
| TC-026 | UC-013 TWINT-Zahlung erfassen und löschen | InkassoSicherstellenIT | tc026_twintZahlungErfassen | ✅ |
| TC-027 | UC-013 Zahlung ohne Datum wird abgelehnt | InkassoSicherstellenIT | tc027_zahlungOhneDatum | ✅ |
| TC-028 | UC-013 Mahnung erfassen und löschen | InkassoSicherstellenIT | tc028_mahnungErfassen | ✅ |
| TC-029 | UC-001 Person bearbeiten | PersonVerwaltenIT | tc029_personBearbeiten | ✅ |
| TC-030 | UC-002 Partei ohne Bezeichnung wird abgelehnt | ParteiVerwaltenIT | tc030_parteiOhneBezeichnung | ✅ |
| TC-031 | UC-003 Event bearbeiten | EventAnlegenIT | tc031_eventBearbeiten | ✅ |
| TC-032 | UC-012 Zustellungsdatum via POST/Upsert | AbrechnungZustellenIT | tc032_zustellungsDatumViаUpsert | ✅ |

**31 TCs implementiert (TC-001..TC-033, ohne TC-003 und TC-017 die in TC-001 bzw. TC-016 integriert sind). Keine fehlenden IT-Methoden.**

---

## Open items

- [ ] **Keine Controller-Validierung**: Alle POST-Fehlerszenarien liefern HTTP 500 statt 400. Für korrekte Fehlerbehandlung müssen `@Valid` + Bean-Validation-Annotationen ergänzt werden (TC-002, TC-005, TC-007, TC-012, TC-015, TC-021, TC-023, TC-027).
- [x] **TC-031 Event bearbeiten ergänzt**: `PUT /api/events/{id}` implementiert + TC-031 in `EventAnlegenIT` (analog TC-029 für Person).
- [ ] **Keine Berechnungslogik-API**: UC-011 Abrechnungsberechnung ist nicht im API implementiert (TC-022, TC-023).
- [ ] **Kein Konsumationslisten-Endpunkt**: UC-009 hat keinen dedizierten Endpunkt für die Listenansicht (TC-018, TC-019).
- [ ] **Kein Event-Filter auf Collections**: Kein `?eventId=` Query-Parameter auf `/api/konsumationsangebote`, `/api/teilnahmen` etc.
- [x] **Citrus 4.9.4 + Spring Boot 4.x Kompatibilität**: Citrus ist inkompatibel (HttpHeaders implementiert MultiValueMap nicht mehr). Tests verwenden stattdessen `RestTemplate` direkt — kein Citrus-API im Einsatz.
- [ ] **AbstractQuartierfestIT fehlt**: `setUp()`, `setupPost()`, `tryDelete()`, `id()` sind in allen 13 IT-Klassen identisch kopiert (~400 Zeilen Boilerplate). Empfehlung: gemeinsame abstrakte Basisklasse einführen. Details → `specs/TODO.md` (TEST-001).
- [ ] **Keine Unit-Tests für 10 Services**: Nur `ParteiService` hat einen Mockito-Unit-Test. Alle anderen Services (Delegation an Repository) haben 0% Unit-Test-Abdeckung. Details → `specs/TODO.md` (TEST-003).
- [x] **UC-005 Teilnahme-Erzeugungsmodell**: Bestätigt: Explizite Erstellung via Button (kein Auto-Create). TC-011 bleibt gültig.
- [x] **UC-005 Mehrere Buffet-Beiträge**: `buffetBeitrag`/`buffetBeitragBeschreibung` durch `buffetBeitraege: List<TeilnahmeBuffetBeitrag>` ersetzt (`@ElementCollection`, Tabelle `teilnahme_buffet_beitrag`). TC-033 neu ergänzt.
- [x] **UC-006 PATCH für `bestaetigungVersendet`**: Kein PATCH benötigt — POST/Upsert mit `id` im Body funktioniert. TC-013 aktualisiert.
- [x] **UC-012 PATCH für `zustellungsDatum`**: Kein PATCH benötigt — POST/Upsert mit `id` im Body funktioniert. TC-032 ergänzt.
