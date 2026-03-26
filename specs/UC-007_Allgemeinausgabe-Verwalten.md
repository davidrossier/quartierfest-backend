---
id: UC-007
type: Use Case
name: "Allgemeinausgaben verwalten"
completeness: Intermediate
---

# UC-007 – Allgemeinausgaben verwalten

---

## Brief Description

> Der Organisator möchte die gemeinsamen Ausgaben eines Events erfassen und verwalten, damit diese bei der Schlussabrechnung gleichmässig auf alle Teilnehmenden aufgeteilt werden können.

---

## Actors

| Actor | Type | Role |
|---|---|---|
| Organisator | `Human` | Erfasst und pflegt die Allgemeinausgaben |

---

## Context & Background

> Allgemeinausgaben sind Kosten, die für die Gesamtheit der Teilnehmenden anfallen und nicht einer einzelnen Partei zugeordnet werden können (z.B. Kühlschrankmiete, Festbankgarnituren, Servietten, Tischtücher). Sie sind einem Event zugeordnet und werden bei der Abrechnung (UC-011) proportional nach Personenanzahl aufgeteilt.

---

## Preconditions

- Eine authentifizierte Sitzung des Organisators besteht.
- Ein Event existiert im System (UC-003).

---

## Trigger

> Der Organisator erfasst Ausgaben, die im Rahmen der Event-Vorbereitung oder -Durchführung entstanden sind.

---

## Description

1. Der Organisator öffnet die Allgemeinausgaben für den ausgewählten Event.
2. Der Organisator wählt "Neue Ausgabe erfassen".
3. Der Organisator gibt Beschreibung (Pflicht) und Betrag (Pflicht) ein sowie optional die Herkunft (z.B. Lieferant, Laden). *(→ E1 bei fehlendem Pflichtfeld, → E2 bei ungültigem Betrag)*
4. Das System speichert die Ausgabe und zeigt die aktualisierte Ausgabenliste mit Gesamtbetrag.
5. Der Organisator kann bestehende Ausgaben bearbeiten oder löschen.

---

## Error Scenarios

### E1 – Pflichtfeld fehlt

> Entry point: step 3 of the main flow

1. E1.1: Das System stellt fest, dass Beschreibung oder Betrag fehlt.
2. E1.2: Das System zeigt eine Fehlermeldung und verhindert das Speichern.

### E2 – Ungültiger Betrag

> Entry point: step 3 of the main flow

1. E2.1: Der Organisator gibt einen negativen oder nicht-numerischen Betrag ein.
2. E2.2: Das System zeigt eine Fehlermeldung und verhindert das Speichern.

---

## Postconditions

### Success

- Die Ausgabe ist dem Event zugeordnet und im System gespeichert.
- Der Gesamtbetrag aller Allgemeinausgaben des Events ist aktuell.
- Die Ausgabe wird bei der Abrechnung (UC-011) berücksichtigt.

### Failure / Abort

- Keine Ausgabe wurde gespeichert; der vorherige Zustand bleibt erhalten.

---

## Acceptance Criteria

```gherkin
Scenario: Allgemeinausgabe erfolgreich erfassen
  Given der Organisator ist in der Allgemeinausgaben-Verwaltung für Event 2025
  When er Beschreibung "Kühlschrankmiete", Herkunft "Metzgerei Meier" und Betrag 80.00 eingibt und speichert
  Then ist die Ausgabe "Kühlschrankmiete CHF 80.00" in der Ausgabenliste des Events vorhanden

Scenario: Ausgabe ohne Betrag speichern schlägt fehl
  Given der Organisator ist in der Allgemeinausgaben-Verwaltung
  When er nur eine Beschreibung eingibt und versucht zu speichern
  Then zeigt das System eine Fehlermeldung und speichert nicht

Scenario: Gesamtbetrag wird korrekt summiert
  Given Event 2025 hat Allgemeinausgaben von CHF 80.00 und CHF 45.00
  When der Organisator die Ausgabenliste öffnet
  Then zeigt das System einen Gesamtbetrag von CHF 125.00
```

---

## Dependencies & References

- **Depends on**: UC-003 (Event anlegen)
