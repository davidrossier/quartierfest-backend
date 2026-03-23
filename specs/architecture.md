# Architekturdiagramm Quartierfest-Backend

## Schichtenarchitektur

```mermaid
graph LR
    Client([Client])

    subgraph Spring Boot Backend
        direction TB
        Controller[Controller\nREST API]
        Service[Service\nGeschäftslogik]
        Repository[Repository\nJPA / Spring Data]
    end

    DB[(PostgreSQL\nquartierfest)]

    Client -->|HTTP| Controller
    Controller --> Service
    Service --> Repository
    Repository -->|JDBC| DB
```

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
