---
id: UC-003
type: Use Case
name: "Event anlegen"
completeness: Minimum
traceability:
  impl_status: vollständig
  endpoints:
    - "GET /api/events"
    - "POST /api/events"
    - "PUT /api/events/{id}"
    - "DELETE /api/events/{id}"
  test_ids:
    - TC-006
    - TC-007
  it_classes:
    - EventAnlegenIT
  last_traced: "2026-05-01"
---

# UC-003 – Event anlegen

---

## Brief Description

> Der Organisator möchte einen neuen Event erfassen und dessen Rahmendaten pflegen, um die Planung eines Quartierfests im System abzubilden.

---

## Actors

| Actor | Type | Role |
|---|---|---|
| Organisator | `Human` | Plant und verwaltet Events |

---

## Context & Background

> Ein Event repräsentiert eine konkrete Durchführung des Quartierfests (z.B. Buchlenfest). Alle weiteren Objekte — Einladungen, Konsumationsangebot, Allgemeinausgaben und Abrechnungen — sind einem Event zugeordnet. Das Datum und der Standort sind zentrale Angaben, die in den Einladungen und Bestätigungen kommuniziert werden. Bei schlechtem Wetter kann auf den alternativen Standort ausgewichen werden.

---

## Frontend-Kontext

> **Route:** `/events` — `EventsVerwaltungComponent` (Angular 21, Standalone)

- Formularvalidierung clientseitig: `datum`, `startzeit` und `standort` sind `Validators.required`. `alternativerStandort`, `zeitAufstellen` und `zeitAufraumen` sind optional und werden als `undefined` übermittelt, wenn leer.
- Bearbeiten öffnet das Formular inline (kein separater Dialog); alle Felder sind editierbar.
- Löschen öffnet `confirm('Event „{standort}, {datum}" wirklich löschen?')`.
- Liste ist sortierbar nach datum, startzeit, standort.
- Events werden in allen event-kontextabhängigen Routen (Planung, Durchführung, Nachbearbeitung) als Auswahlobjekt im `EventKontextService` verwendet.

---

## Preconditions

- Eine authentifizierte Sitzung des Organisators besteht.

---

## Trigger

> Der Organisator entscheidet, ein neues Quartierfest zu planen, und öffnet die Event-Verwaltung.

---

## Description

1. Der Organisator wählt "Neuen Event erfassen".
2. Der Organisator gibt Datum und Startzeit ein (Pflicht) sowie Standort (Pflicht). *(→ E1 bei fehlendem Pflichtfeld)*
3. Optional gibt der Organisator den alternativen Standort, die Aufstellzeit und die Aufräumzeit ein.
4. Das System speichert den Event.
5. Der Organisator kann den Event nachträglich bearbeiten und alle Felder anpassen.
6. Das System zeigt die aktualisierte Eventliste an.

---

## Error Scenarios

### E1 – Pflichtfeld fehlt

> Entry point: step 2 of the main flow

1. E1.1: Das System stellt fest, dass Datum, Startzeit oder Standort fehlt.
2. E1.2: Das System zeigt eine Fehlermeldung und verhindert das Speichern.
3. E1.3: Der Organisator ergänzt die fehlenden Angaben und wiederholt den Speichervorgang.

---

## Postconditions

### Success

- Der Event ist mit Datum, Startzeit und Standort im System gespeichert.
- Der Event ist als Basis für Einladungen (UC-004), Konsumationsangebot (UC-008) und Allgemeinausgaben (UC-007) nutzbar.

### Failure / Abort

- Kein neuer Event wurde gespeichert; das System bleibt im vorherigen Zustand.

---

## Acceptance Criteria

```gherkin
Scenario: Event erfolgreich anlegen
  Given der Organisator ist in der Event-Verwaltung
  When er Datum "2025-07-05", Startzeit "17:00" und Standort "Buchlenwiese" eingibt und speichert
  Then ist der Event "Buchlenwiese, 05.07.2025" in der Eventliste vorhanden

Scenario: Event ohne Pflichtfeld speichern schlägt fehl
  Given der Organisator ist in der Event-Verwaltung
  When er nur ein Datum eingibt und versucht zu speichern
  Then zeigt das System eine Fehlermeldung und speichert nicht

Scenario: Event nachträglich bearbeiten
  Given der Event "Buchlenwiese, 05.07.2025" existiert
  When der Organisator den alternativen Standort "Turnhalle Buchlenmatt" ergänzt und speichert
  Then ist der alternative Standort beim Event gespeichert
```

---

## Open Items

- [x] RESOLVED: `PUT /api/events/{id}` wurde in `EventController` ergänzt (analog `ParteiController`).

