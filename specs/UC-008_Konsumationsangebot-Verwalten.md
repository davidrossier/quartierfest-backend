---
id: UC-008
type: Use Case
name: "Konsumationsangebot verwalten"
completeness: Intermediate
traceability:
  impl_status: vollständig
  endpoints:
    - "GET /api/konsumationsangebote"
    - "POST /api/konsumationsangebote"
    - "DELETE /api/konsumationsangebote/{id}"
  test_ids:
    - TC-016
  it_classes:
    - KonsumationsangebotVerwaltenIT
  last_traced: "2026-04-10"
---

# UC-008 – Konsumationsangebot verwalten

---

## Brief Description

> Der Organisator möchte das Getränke- und Konsumationsangebot eines Events erfassen und pflegen, damit es in der Bestätigung kommuniziert und als Grundlage für die Konsumationsliste verwendet werden kann.

---

## Actors

| Actor | Type | Role |
|---|---|---|
| Organisator | `Human` | Erfasst und pflegt das Konsumationsangebot |

---

## Context & Background

> Das Konsumationsangebot besteht aus einer Liste von Getränken oder Produkten mit Einzelpreisen, die an einem Event individuell konsumiert und später verrechnet werden. Es ist einem Event zugeordnet und bildet die X-Achse der Konsumationsliste (UC-009). Das Angebot wird in der Bestätigung an die Parteien kommuniziert (UC-006). Konsumationsangebote können von Event zu Event variieren.

---

## Frontend-Kontext

> **Route:** `/planung/konsumationsangebote` — `KonsumationsangeboteVerwaltungComponent` (Angular 21, Standalone)
> Event-kontextabhängig; die Liste wird nach dem gewählten Event gefiltert.

- Formularvalidierung clientseitig: `bezeichnung` und `preis` sind Pflichtfelder.
- Löschen öffnet `confirm()`-Dialog; FK-Constraint-Fehler bei bestehenden Konsumationen wird als Fehlermeldung angezeigt.

---

## Preconditions

- Eine authentifizierte Sitzung des Organisators besteht.
- Ein Event existiert im System (UC-003).

---

## Trigger

> Der Organisator plant das Getränkeangebot für einen Event und öffnet die Konsumationsangebot-Verwaltung.

---

## Description

1. Der Organisator öffnet das Konsumationsangebot für den ausgewählten Event.
2. Der Organisator wählt "Neues Angebot erfassen".
3. Der Organisator gibt Bezeichnung (Pflicht) und Preis (Pflicht) ein. *(→ E1 bei fehlendem Pflichtfeld)*
4. Das System speichert den Angebotseintrag.
5. Der Organisator wiederholt Schritt 2–4 für alle Einträge des Angebots.
6. Der Organisator kann bestehende Einträge bearbeiten oder löschen. *(→ E2 beim Löschen wenn Konsumationen vorhanden)*
7. Das System zeigt das vollständige Angebot mit allen Positionen und Preisen.

---

## Error Scenarios

### E1 – Pflichtfeld fehlt

> Entry point: step 3 of the main flow

1. E1.1: Das System stellt fest, dass Bezeichnung oder Preis fehlt.
2. E1.2: Das System zeigt eine Fehlermeldung und verhindert das Speichern.

### E2 – Löschen bei bestehenden Konsumationen

> Entry point: step 6 of the main flow

1. E2.1: Das System stellt fest, dass für den Angebotseintrag bereits Konsumationen erfasst sind.
2. E2.2: Das System zeigt einen Hinweis und verhindert die Löschung.

---

## Postconditions

### Success

- Das Konsumationsangebot des Events ist vollständig mit Bezeichnungen und Preisen im System erfasst.
- Das Angebot steht für die Bestätigung (UC-006) und die Konsumationsliste (UC-009) zur Verfügung.

### Failure / Abort

- Keine Änderungen wurden persistiert; der vorherige Zustand bleibt erhalten.

---

## Acceptance Criteria

```gherkin
Scenario: Angebotseintrag erfolgreich erfassen
  Given der Organisator ist in der Konsumationsangebot-Verwaltung für Event 2025
  When er Bezeichnung "Bier (0.5l)" und Preis 3.50 eingibt und speichert
  Then ist "Bier (0.5l) – CHF 3.50" im Angebot des Events vorhanden

Scenario: Angebotseintrag ohne Preis speichern schlägt fehl
  Given der Organisator ist in der Konsumationsangebot-Verwaltung
  When er nur eine Bezeichnung eingibt und versucht zu speichern
  Then zeigt das System eine Fehlermeldung und speichert nicht

Scenario: Angebotseintrag mit bestehenden Konsumationen löschen schlägt fehl
  Given "Bier (0.5l)" hat bereits 3 erfasste Konsumationen
  When der Organisator versucht "Bier (0.5l)" zu löschen
  Then zeigt das System einen Hinweis auf bestehende Konsumationen und bricht ab
```

---

## Dependencies & References

- **Depends on**: UC-003 (Event anlegen)
