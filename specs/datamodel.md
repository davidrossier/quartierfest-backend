# Datenmodell Quartierfest

## Entities und Beziehungen

---

### Person
Empfänger einer Einladung. Wiederverwendbar über mehrere Events.

| Feld           | Typ     | Pflicht |
|----------------|---------|---------|
| id             | Long    | ja      |
| vorname        | String  | ja      |
| name           | String  | ja      |
| telefonnummer  | String  | nein    |
| mobilenummer   | String  | nein    |
| email          | String  | nein    |

---

### Partei
Gruppierung von Personen (typischerweise ein Haushalt). Erhält die Einladung.

| Feld             | Typ     | Pflicht |
|------------------|---------|---------|
| id               | Long    | ja      |
| adresse          | String  | ja      |
| twintAktiv       | Boolean | ja      |
| twintMobilenummer| String  | nein    |

**Beziehungen:**
- `Partei` → `Person`: 1:n (eine Partei hat eine oder mehrere Personen)

---

### Event
Ein Quartierfest-Anlass (Buchlenfest).

| Feld                    | Typ          | Pflicht |
|-------------------------|--------------|---------|
| id                      | Long         | ja      |
| datum                   | LocalDate    | ja      |
| startzeit               | LocalTime    | ja      |
| standort                | String       | ja      |
| alternativerStandort    | String       | nein    |
| zeitAufstellen          | LocalTime    | nein    |
| zeitAufraumen           | LocalTime    | nein    |

---

### Einladung
Verbindet eine Partei mit einem Event. Enthält die Rückmeldung der Partei.

| Feld                    | Typ             | Pflicht |
|-------------------------|-----------------|---------|
| id                      | Long            | ja      |
| event                   | Event           | ja      |
| partei                  | Partei          | ja      |
| status                  | Enum (OFFEN, ANGEMELDET, ABGEMELDET) | ja |
| anzahlPersonen          | Integer         | nein    |
| hilftAufstellen         | Boolean         | nein    |
| hilftAufraumen          | Boolean         | nein    |
| buffetBeitrag           | Enum (KEINER, SALAT, BROT_ZOPF, DESSERT, WEITERE) | nein |
| buffetBeitragBeschreibung | String        | nein    |
| bestaetigungVersendet   | Boolean         | ja      |

**Beziehungen:**
- `Einladung` → `Event`: n:1
- `Einladung` → `Partei`: n:1

---

### Teilnahme
Konsolidierte, editierbare Sicht der tatsächlichen Teilnahme einer Partei an einem Event (nach Rückmeldung).

| Feld                    | Typ      | Pflicht |
|-------------------------|----------|---------|
| id                      | Long     | ja      |
| einladung               | Einladung| ja      |
| anzahlPersonenEffektiv  | Integer  | nein    |
| hilftAufstellen         | Boolean  | nein    |
| hilftAufraumen          | Boolean  | nein    |
| buffetBeitrag           | Enum (KEINER, SALAT, BROT_ZOPF, DESSERT, WEITERE) | nein |
| buffetBeitragBeschreibung | String  | nein    |

**Beziehungen:**
- `Teilnahme` → `Einladung`: 1:1

---

### Konsumationsangebot
Ein Getränk/Produkt, das an einem Event individuell konsumiert und verrechnet wird.

| Feld       | Typ           | Pflicht |
|------------|---------------|---------|
| id         | Long          | ja      |
| event      | Event         | ja      |
| bezeichnung| String        | ja      |
| preis      | BigDecimal    | ja      |

**Beziehungen:**
- `Konsumationsangebot` → `Event`: n:1

---

### Konsumation
Erfasste Konsumation einer Partei für ein bestimmtes Angebot an einem Event.

| Feld                  | Typ                   | Pflicht |
|-----------------------|-----------------------|---------|
| id                    | Long                  | ja      |
| teilnahme             | Teilnahme             | ja      |
| konsumationsangebot   | Konsumationsangebot   | ja      |
| anzahl                | Integer               | ja      |

**Beziehungen:**
- `Konsumation` → `Teilnahme`: n:1
- `Konsumation` → `Konsumationsangebot`: n:1

---

### Allgemeinausgabe
Gemeinschaftliche Ausgabe für einen Event, die auf alle Teilnehmenden aufgeteilt wird.

| Feld        | Typ        | Pflicht |
|-------------|------------|---------|
| id          | Long       | ja      |
| event       | Event      | ja      |
| beschreibung| String     | ja      |
| herkunft    | String     | nein    |
| betrag      | BigDecimal | ja      |

**Beziehungen:**
- `Allgemeinausgabe` → `Event`: n:1

---

### Abrechnung
Individuelle Abrechnung je Partei für einen Event.

| Feld                    | Typ        | Pflicht |
|-------------------------|------------|---------|
| id                      | Long       | ja      |
| teilnahme               | Teilnahme  | ja      |
| anteilAllgemeinkosten   | BigDecimal | ja      |
| totalKonsumation        | BigDecimal | ja      |
| totalBetrag             | BigDecimal | ja      |
| zustellungskanal        | Enum (TWINT, EMAIL, PAPIER) | ja |
| zustellungsDatum        | LocalDate  | nein    |

**Beziehungen:**
- `Abrechnung` → `Teilnahme`: 1:1

---

### Zahlung
Erfassung einer eingegangenen Zahlung zu einer Abrechnung.

| Feld          | Typ        | Pflicht |
|---------------|------------|---------|
| id            | Long       | ja      |
| abrechnung    | Abrechnung | ja      |
| zahlungskanal | Enum (TWINT, UEBERWEISUNG, BAR) | ja |
| datum         | LocalDate  | ja      |
| betrag        | BigDecimal | ja      |

**Beziehungen:**
- `Zahlung` → `Abrechnung`: n:1

---

### Mahnung
Festgehaltene Mahnung zu einer Abrechnung.

| Feld       | Typ        | Pflicht |
|------------|------------|---------|
| id         | Long       | ja      |
| abrechnung | Abrechnung | ja      |
| datum      | LocalDate  | ja      |
| bemerkung  | String     | nein    |

**Beziehungen:**
- `Mahnung` → `Abrechnung`: n:1

---

## Beziehungsübersicht

```
Person          ←── n:1 ──── Partei
Partei          ←── n:1 ──── Einladung ──── n:1 ───→ Event
Einladung       ──── 1:1 ──→ Teilnahme
Teilnahme       ←── n:1 ──── Konsumation ── n:1 ───→ Konsumationsangebot
Event           ←── n:1 ──── Konsumationsangebot
Event           ←── n:1 ──── Allgemeinausgabe
Teilnahme       ──── 1:1 ──→ Abrechnung
Abrechnung      ←── n:1 ──── Zahlung
Abrechnung      ←── n:1 ──── Mahnung
```
