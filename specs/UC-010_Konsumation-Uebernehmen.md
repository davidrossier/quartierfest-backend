---
id: UC-010
type: Use Case
name: "Konsumation übernehmen"
completeness: Intermediate
traceability:
  impl_status: vollständig
  endpoints:
    - "GET /api/konsumationen"
    - "POST /api/konsumationen"
    - "DELETE /api/konsumationen/{id}"
  test_ids:
    - TC-020
    - TC-021
  it_classes:
    - KonsumationUebernehmenIT
  last_traced: "2026-04-10"
---

# UC-010 – Konsumation übernehmen

---

## Brief Description

> Der Organisator möchte die handschriftlich ausgefüllte Konsumationsliste nach dem Event digital erfassen, um die Konsumationsdaten für die Abrechnung im System verfügbar zu machen.

---

## Actors

| Actor | Type | Role |
|---|---|---|
| Organisator | `Human` | Überträgt die handschriftlichen Konsumationsdaten ins System |

---

## Context & Background

> Nach dem Event liegt die ausgefüllte Konsumationsliste (UC-009) vor. Der Organisator überträgt die Strichlisten-Einträge je Partei und Getränk ins System. Je Kombination aus Teilnahme und Konsumationsangebot wird ein Konsumations-Datensatz mit der Anzahl gespeichert. Diese Daten sind Grundlage für die individuelle Kostenberechnung in der Abrechnung (UC-011).

---

## Frontend-Kontext

> **Route:** `/durchfuehrung/konsumationen` — `KonsumationenVerwaltungComponent` (Angular 21, Standalone)
> Event-kontextabhängig; Matrix aus Teilnahmen × Konsumationsangebote des gewählten Events.

- **Matrixeingabe:** Jede Zelle ist ein editierbares Zahlenfeld (`matrixWerte` Signal mit Key `teilnahmeId-angebotId`). Alle Werte werden in einem einzigen "Speichern"-Button-Klick per `forkJoin` persistiert.
- **Upsert-Logik:** Einträge mit Anzahl > 0 werden erstellt oder aktualisiert (anhand der `konsumationIds`-Map). Einträge mit Anzahl = 0 werden gelöscht, wenn ein bestehender Datensatz vorhanden ist.
- **Clientseitige Validierung:** `hatUngueltigeWerte()` prüft auf negative Zahlen; das Speichern wird blockiert.
- **Totals:** `totalFuerTeilnahme()` berechnet den Konsumationstotal pro Partei clientseitig (Anzahl × Preis).

---

## Preconditions

- Eine authentifizierte Sitzung des Organisators besteht.
- Teilnahmen für den Event existieren (UC-005).
- Das Konsumationsangebot für den Event ist vollständig erfasst (UC-008).
- Die handschriftliche Konsumationsliste liegt vor.

---

## Trigger

> Der Organisator hat die physische Konsumationsliste vom Event und öffnet die Konsumationserfassung im System.

---

## Description

1. Der Organisator öffnet die Konsumationserfassung für den ausgewählten Event.
2. Das System zeigt eine Eingabemaske mit der gleichen Matrixstruktur wie die gedruckte Liste (Parteien × Angebote).
3. Der Organisator überträgt je Partei und Getränk die Anzahl aus der handschriftlichen Liste.
4. Das System speichert jeden Eintrag als Konsumations-Datensatz (Teilnahme + Konsumationsangebot + Anzahl). *(→ E1 bei negativer Anzahl)*
5. Der Organisator prüft die übertragenen Daten auf Vollständigkeit.
6. Das System berechnet pro Partei den Konsumationstotal und zeigt ihn zur Kontrolle an.

---

## Alternative Flows

### A1 – Partei hat keine Konsumation

> Entry point: step 3 of the main flow

1. A1.1: Der Organisator lässt alle Felder einer Partei leer oder trägt 0 ein.
2. A1.2: Das System speichert keine Konsumations-Datensätze für diese Partei (oder Datensätze mit Anzahl 0).

---

## Error Scenarios

### E1 – Ungültige Anzahl

> Entry point: step 3 of the main flow

1. E1.1: Der Organisator gibt eine negative Zahl oder einen nicht-numerischen Wert ein.
2. E1.2: Das System zeigt eine Fehlermeldung und verhindert das Speichern des betroffenen Eintrags.

---

## Postconditions

### Success

- Alle Konsumationen sind als Datensätze im System gespeichert und mit Teilnahme und Konsumationsangebot verknüpft.
- Die Konsumationsdaten stehen für die Abrechnung (UC-011) zur Verfügung.

### Failure / Abort

- Bereits gespeicherte Einträge bleiben erhalten; nicht gespeicherte Änderungen werden verworfen.

---

## Acceptance Criteria

```gherkin
Scenario: Konsumation einer Partei erfolgreich erfassen
  Given Partei "Müller" hat eine Teilnahme am Event 2025 und "Bier (0.5l)" ist im Angebot
  When der Organisator für "Müller" und "Bier (0.5l)" die Anzahl 4 eingibt und speichert
  Then existiert ein Konsumations-Datensatz mit Teilnahme "Müller", Angebot "Bier (0.5l)" und Anzahl 4

Scenario: Konsumationstotal wird korrekt berechnet
  Given Partei "Müller" hat 4 × "Bier" à CHF 3.50 und 2 × "Wasser" à CHF 1.50 konsumiert
  When der Organisator die Konsumationsübersicht für "Müller" betrachtet
  Then zeigt das System einen Total von CHF 17.00

Scenario: Negative Anzahl wird abgelehnt
  Given der Organisator ist in der Konsumationserfassung
  When er für eine Partei eine Anzahl von -1 eingibt
  Then zeigt das System eine Fehlermeldung und speichert nicht
```

---

## Dependencies & References

- **Depends on**: UC-005 (Teilnahmen verwalten), UC-008 (Konsumationsangebot verwalten), UC-009 (Konsumationsliste erstellen)
