# Architekturdiagramm Quartierfest-Backend

## Schichtenarchitektur

```mermaid
graph LR
    Angular([Angular\nlocalhost:4200])
    Other([Andere Clients])

    subgraph Spring Boot Backend
        direction TB
        CORS[WebConfig\nCORS-Filter]
        Controller[Controller\nREST API]
        Service[Service\nGeschäftslogik]
        Repository[Repository\nJPA / Spring Data]
        CORS --> Controller
        Controller --> Service
        Service --> Repository
    end

    DB[(PostgreSQL\nquartierfest)]

    Angular -->|HTTP + CORS| CORS
    Other -->|HTTP| CORS
    Repository -->|JDBC| DB
```

### CORS-Konfiguration

`WebConfig.java` konfiguriert CORS global für alle `/api/**`-Endpunkte:

| Einstellung | Wert |
|---|---|
| Erlaubter Origin | `http://localhost:4200` (Angular-Dev-Server) |
| Erlaubte Methoden | `GET`, `POST`, `PUT`, `DELETE` |
| Erlaubte Headers | `*` |

Andere Origins werden vom Browser blockiert. Server-seitige Clients (z.B. `RestTemplate` in Integrationstests) sind von CORS nicht betroffen.

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
        BuffetBeitrag buffetBeitrag
        String buffetBeitragBeschreibung
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

    PARTEI ||--o{ PERSON : "besteht aus"
    PARTEI ||--o{ EINLADUNG : "erhält"
    EVENT ||--o{ EINLADUNG : "hat"
    EINLADUNG ||--|| TEILNAHME : "wird zu"
    EVENT ||--o{ KONSUMATIONSANGEBOT : "bietet an"
    EVENT ||--o{ ALLGEMEINAUSGABE : "hat"
    TEILNAHME ||--o{ KONSUMATION : "konsumiert"
    KONSUMATIONSANGEBOT ||--o{ KONSUMATION : "wird konsumiert in"
    TEILNAHME ||--|| ABRECHNUNG : "erhält"
    ABRECHNUNG ||--o{ ZAHLUNG : "wird bezahlt durch"
    ABRECHNUNG ||--o{ MAHNUNG : "wird gemahnt durch"
```

---

## REST-Endpunkte

| Domain              | Endpunkt                      | GET | POST | PUT | DELETE |
|---------------------|-------------------------------|:---:|:----:|:---:|:------:|
| Person              | `/api/persons`                | ✓   | ✓    | ✓   | ✓      |
| Partei              | `/api/parteien`               | ✓   | ✓    | ✓   | ✓      |
| Event               | `/api/events`                 | ✓   | ✓    | —   | ✓      |
| Einladung           | `/api/einladungen`            | ✓   | ✓    | —   | ✓      |
| Teilnahme           | `/api/teilnahmen`             | ✓   | ✓    | —   | ✓      |
| Konsumationsangebot | `/api/konsumationsangebote`   | ✓   | ✓    | —   | ✓      |
| Konsumation         | `/api/konsumationen`          | ✓   | ✓    | —   | ✓      |
| Allgemeinausgabe    | `/api/allgemeinausgaben`      | ✓   | ✓    | —   | ✓      |
| Abrechnung          | `/api/abrechnungen`           | ✓   | ✓    | —   | ✓      |
| Zahlung             | `/api/zahlungen`              | ✓   | ✓    | —   | ✓      |
| Mahnung             | `/api/mahnungen`              | ✓   | ✓    | —   | ✓      |

---

## Traceability

> Automatisch generiert durch Traceability-Manager — Stand: 2026-04-03
> UC-Abdeckung: 9/13 vollständig | 4 mit Lücken | 0 nicht implementiert

### UC × Implementierung × Test

| UC-ID | Titel | Endpunkt(e) | TC-ID(s) | IT-Klasse | Status |
|-------|-------|-------------|----------|-----------|--------|
| UC-001 | Personendaten verwalten | GET/POST/PUT/DELETE `/api/persons` | TC-001, TC-002, TC-029 | PersonVerwaltenIT | ✅ Vollständig |
| UC-002 | Parteien verwalten | GET/POST/PUT/DELETE `/api/parteien` | TC-004, TC-005, TC-030 | ParteiVerwaltenIT | ✅ Vollständig |
| UC-003 | Event anlegen | GET/POST/DELETE `/api/events` | TC-006, TC-007 | EventAnlegenIT | ✅ Vollständig |
| UC-004 | Einladung verwalten | GET/POST/DELETE `/api/einladungen` | TC-008, TC-009, TC-010 | EinladungVerwaltenIT | ✅ Vollständig |
| UC-005 | Teilnahme erfassen | GET/POST/DELETE `/api/teilnahmen` | TC-011, TC-012 | TeilnahmeVerwaltenIT | ✅ Vollständig |
| UC-006 | Bestätigung versenden | POST `/api/einladungen` (Flag `bestaetigungVersendet`) | TC-013 | BestaetigungVerwaltenIT | ⚠ Teilimpl. |
| UC-007 | Allgemeinausgaben verwalten | GET/POST/DELETE `/api/allgemeinausgaben` | TC-014, TC-015 | AllgemeinausgabeVerwaltenIT | ✅ Vollständig |
| UC-008 | Konsumationsangebot verwalten | GET/POST/DELETE `/api/konsumationsangebote` | TC-016 | KonsumationsangebotVerwaltenIT | ✅ Vollständig |
| UC-009 | Konsumationsliste erstellen | GET `/api/konsumationsangebote`, GET `/api/teilnahmen` | TC-018, TC-019 | KonsumationslisteErstellenIT | ⚠ Teilimpl. |
| UC-010 | Konsumation übernehmen | GET/POST/DELETE `/api/konsumationen` | TC-020, TC-021 | KonsumationUebernehmenIT | ✅ Vollständig |
| UC-011 | Abrechnung erstellen | GET/POST/DELETE `/api/abrechnungen` | TC-022, TC-023 | AbrechnungErstellenIT | ⚠ Teilimpl. |
| UC-012 | Abrechnung zustellen | POST `/api/abrechnungen` (Feld `zustellungskanal`) | TC-024, TC-025 | AbrechnungZustellenIT | ⚠ Teilimpl. |
| UC-013 | Inkasso sicherstellen | GET/POST/DELETE `/api/zahlungen`, `/api/mahnungen` | TC-026, TC-027, TC-028 | InkassoSicherstellenIT | ✅ Vollständig |

### Offene Traceability-Lücken

- **UC-006** (Bestätigung versenden): Kein dedizierter PATCH-Endpunkt für `bestaetigungVersendet`; Flag wird über POST `/api/einladungen` gesetzt. → Empfehlung: `PATCH /api/einladungen/{id}/bestaetigung` ergänzen
- **UC-009** (Konsumationsliste erstellen): Kein dedizierter `GET /api/events/{id}/konsumationsliste`-Endpunkt. → Empfehlung: Endpunkt für Event-spezifische Konsumationsansicht implementieren
- **UC-011** (Abrechnung erstellen): Keine automatische Berechnung von `anteilAllgemeinkosten` / `totalKonsumation`. Werte werden manuell übergeben. → Empfehlung: `POST /api/abrechnungen/kalkulieren` oder Berechnung im Service
- **UC-012** (Abrechnung zustellen): Kein PATCH-Endpunkt für `zustellungsDatum`. Datum wird beim POST gesetzt. → Empfehlung: `PATCH /api/abrechnungen/{id}/zustellung` ergänzen
