---
id: UC-011
type: Use Case
name: "Abrechnung erstellen"
completeness: Minimum
---

# UC-011 – Abrechnung erstellen

---

## Brief Description

> Der Organisator möchte für jede teilnehmende Partei eine individuelle Abrechnung erstellen, die den Anteil an Allgemeinkosten und die persönliche Konsumation zusammenfasst.

---

## Actors

| Actor | Type | Role |
|---|---|---|
| Organisator | `Human` | Löst die Abrechnungserstellung aus und prüft die Resultate |
| System | `System` | Berechnet die Abrechnungsbeträge automatisch |

---

## Context & Background

> Die Abrechnung je Partei setzt sich aus zwei Komponenten zusammen: (1) dem Anteil an den Allgemeinausgaben (UC-007), aufgeteilt nach effektiver Personenzahl aller Teilnehmenden, und (2) dem Total der persönlichen Konsumation (UC-010). Das System berechnet diese Werte automatisch und speichert sie als Abrechnung, die dann an die Parteien zugestellt wird (UC-012).

---

## Preconditions

- Eine authentifizierte Sitzung des Organisators besteht.
- Alle Teilnahmen für den Event sind bereinigt und die effektiven Personenzahlen sind korrekt erfasst (UC-005).
- Alle Konsumationen sind digital erfasst (UC-010).
- Alle Allgemeinausgaben sind erfasst (UC-007).

---

## Trigger

> Der Organisator hat alle Event-Daten erfasst und startet die Abrechnungserstellung.

---

## Description

1. Der Organisator öffnet die Abrechnungsfunktion für den ausgewählten Event und wählt "Abrechnungen erstellen".
2. Das System berechnet den Anteil Allgemeinkosten je Person: Summe aller Allgemeinausgaben ÷ Gesamtanzahl Personen (Summe aller `anzahlPersonenEffektiv`).
3. Das System berechnet je Partei den Anteil Allgemeinkosten: Anteil je Person × `anzahlPersonenEffektiv` der Partei.
4. Das System summiert je Partei das Total der Konsumation aus allen Konsumations-Datensätzen.
5. Das System erstellt je Partei eine Abrechnung mit: `anteilAllgemeinkosten`, `totalKonsumation`, `totalBetrag` (Summe beider) und setzt den Zustellungskanal basierend auf der Twint-Angabe der Partei (TWINT wenn `twintAktiv = true`, sonst EMAIL).
6. Das System zeigt alle erstellten Abrechnungen zur Überprüfung an.
7. Der Organisator prüft die Abrechnungen und korrigiert bei Bedarf den Zustellungskanal manuell.

---

## Alternative Flows

### A1 – Abrechnung neu berechnen

> Entry point: step 1 of the main flow

1. A1.1: Eine Abrechnung wurde bereits erstellt, aber Daten haben sich geändert (z.B. Konsumation korrigiert).
2. A1.2: Der Organisator löscht die bestehende Abrechnung und startet die Erstellung erneut.
3. A1.3: Das System berechnet alle Werte neu.

---

## Error Scenarios

### E1 – Keine Teilnahmen vorhanden

> Entry point: step 1 of the main flow

1. E1.1: Das System stellt fest, dass keine Teilnahmen mit `anzahlPersonenEffektiv > 0` existieren.
2. E1.2: Das System zeigt einen Hinweis und erstellt keine Abrechnungen.

### E2 – Keine Allgemeinausgaben vorhanden

> Entry point: step 2 of the main flow

1. E2.1: Das System stellt fest, dass keine Allgemeinausgaben erfasst sind.
2. E2.2: Das System setzt `anteilAllgemeinkosten = 0` und erstellt die Abrechnungen mit nur dem Konsumationsanteil. Das System zeigt einen Warnhinweis.

---

## Postconditions

### Success

- Für jede Teilnahme existiert eine Abrechnung mit korrekt berechneten Beträgen und gesetztem Zustellungskanal.
- Die Abrechnungen stehen für die Zustellung (UC-012) bereit.

### Failure / Abort

- Keine Abrechnungen wurden erstellt oder verändert.

---

## Acceptance Criteria

```gherkin
Scenario: Anteil Allgemeinkosten korrekt berechnen
  Given Event 2025 hat Allgemeinausgaben von CHF 120.00 und 3 Teilnahmen mit je 2 Personen (total 6)
  When der Organisator die Abrechnungen erstellt
  Then hat jede Partei einen anteilAllgemeinkosten von CHF 40.00 (2 Personen × CHF 20.00)

Scenario: Abrechnung mit Konsumation korrekt berechnen
  Given Partei "Müller" hat anteilAllgemeinkosten CHF 40.00 und totalKonsumation CHF 17.00
  When das System die Abrechnung erstellt
  Then hat die Abrechnung "Müller" einen totalBetrag von CHF 57.00

Scenario: Zustellungskanal automatisch setzen
  Given Partei "Müller" hat twintAktiv=true
  When das System die Abrechnung für "Müller" erstellt
  Then hat die Abrechnung den Zustellungskanal TWINT
```

---

## Open Items

- [ ] REVIEW: Das Datenmodell definiert eine 1:1-Beziehung zwischen Teilnahme und Abrechnung. Verhindert das System die Erstellung einer zweiten Abrechnung für dieselbe Teilnahme (Unique-Constraint), oder liegt es am Organisator, zuerst zu löschen (A1)? Fehlverhalten bei Duplikat muss spezifiziert werden.

---

## Dependencies & References

- **Depends on**: UC-005 (Teilnahmen verwalten), UC-007 (Allgemeinausgaben verwalten), UC-010 (Konsumation übernehmen)
