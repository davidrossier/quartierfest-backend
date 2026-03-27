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
| Erlaubte Methoden | `GET`, `POST`, `DELETE` |
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

| Domain              | Endpunkt                      | GET | POST | DELETE |
|---------------------|-------------------------------|:---:|:----:|:------:|
| Person              | `/api/persons`                | ✓   | ✓    | ✓      |
| Partei              | `/api/parteien`               | ✓   | ✓    | ✓      |
| Event               | `/api/events`                 | ✓   | ✓    | ✓      |
| Einladung           | `/api/einladungen`            | ✓   | ✓    | ✓      |
| Teilnahme           | `/api/teilnahmen`             | ✓   | ✓    | ✓      |
| Konsumationsangebot | `/api/konsumationsangebote`   | ✓   | ✓    | ✓      |
| Konsumation         | `/api/konsumationen`          | ✓   | ✓    | ✓      |
| Allgemeinausgabe    | `/api/allgemeinausgaben`      | ✓   | ✓    | ✓      |
| Abrechnung          | `/api/abrechnungen`           | ✓   | ✓    | ✓      |
| Zahlung             | `/api/zahlungen`              | ✓   | ✓    | ✓      |
| Mahnung             | `/api/mahnungen`              | ✓   | ✓    | ✓      |
