---
id: UC-004
type: Use Case
name: "Einladung erstellen und verwalten"
completeness: Minimum
traceability:
  impl_status: vollständig
  endpoints:
    - "GET /api/einladungen"
    - "POST /api/einladungen"
    - "DELETE /api/einladungen/{id}"
  test_ids:
    - TC-008
    - TC-009
    - TC-010
  it_classes:
    - EinladungVerwaltenIT
  last_traced: "2026-04-10"
---

# UC-004 – Einladung erstellen und verwalten

---

## Brief Description

> Der Organisator möchte für einen Event Einladungen an alle relevanten Parteien erstellen und deren Rückmeldungen erfassen, um die Teilnahmeplanung zu ermöglichen.

---

## Actors

| Actor | Type | Role |
|---|---|---|
| Organisator | `Human` | Erstellt Einladungen und erfasst Rückmeldungen der Parteien |
| Partei | `External` | Empfängt die Einladung ausserhalb des Systems und meldet sich ab oder an |

---

## Context & Background

> Eine Einladung verbindet eine Partei mit einem Event und enthält alle relevanten Event-Angaben sowie die Rückmeldung der Partei. Der initiale Status ist OFFEN. Meldet sich eine Partei an, wechselt der Status zu ANGEMELDET und die Partei gibt die Anzahl Personen, Hilfsbereitschaft und Buffetbeitrag an. Der Organisator erfasst diese Rückmeldungen im System — die Einladung selbst wird ausserhalb des Systems (z.B. per Post, E-Mail) versendet. Aus ANGEMELDET-Einladungen werden Teilnahmen (UC-005) abgeleitet.

---

## Frontend-Kontext

> **Route:** `/planung/einladungen` — `EinladungenVerwaltungComponent` (Angular 21, Standalone)
> Event-kontextabhängig: die Liste wird nach dem im `EventKontextService` gewählten Event gefiltert.

- **"Einladungen für alle erstellen"** (`einladungenFuerAlleErstellen()`): erstellt via `forkJoin` für alle Parteien, die **noch keine Einladung** für den gewählten Event haben, eine Einladung mit Status OFFEN. Parteien mit bestehender Einladung werden übersprungen — E1 ist damit sicher implizit verhindert.
- **Rückmeldung erfassen:** das Formular enthält status, anzahlPersonen, hilftAufstellen, hilftAufraumen, buffetBeitrag, buffetBeitragBeschreibung. Felder für Personenzahl und Hilfsbereitschaft werden nur übermittelt, wenn status = ANGEMELDET.
- **Einzelne Einladung neu:** Dropdown zur Parteiauswahl zeigt nur Parteien ohne bestehende Einladung für den Event (`nichtEingeladeneParteien`).
- **`buffetBeitragBeschreibung`** wird nur übermittelt, wenn buffetBeitrag = WEITERE.
- Löschen öffnet `confirm()`-Dialog.

---

## Preconditions

- Eine authentifizierte Sitzung des Organisators besteht.
- Ein Event existiert im System (UC-003).
- Mindestens eine Partei existiert im System (UC-002).

---

## Trigger

> Der Organisator startet die Einladungserstellung für einen ausgewählten Event.

---

## Description

1. Der Organisator wählt einen Event und startet "Einladungen erstellen".
2. Das System erstellt für alle im System vorhandenen Parteien je eine Einladung mit Status OFFEN.
3. Der Organisator versendet die physischen oder digitalen Einladungen ausserhalb des Systems.
4. Eine Partei meldet sich an oder ab.
5. Der Organisator öffnet die Einladung der Partei und erfasst die Rückmeldung:
   - Status: ANGEMELDET oder ABGEMELDET *(→ A1 bei ABGEMELDET)*
   - Anzahl Personen
   - Hilft beim Aufstellen (Boolean)
   - Hilft beim Aufräumen (Boolean)
   - Buffetbeitrag (KEINER, SALAT, BROT_ZOPF, DESSERT, WEITERE) und optionale Beschreibung
6. Das System speichert die Rückmeldung zur Einladung.
7. Der Organisator wiederholt Schritt 5–6 für jede eintreffende Rückmeldung.

---

## Alternative Flows

### A1 – Partei meldet sich ab

> Entry point: step 5 of the main flow

1. A1.1: Der Organisator setzt den Status auf ABGEMELDET.
2. A1.2: Die Felder für Anzahl Personen, Hilfsbereitschaft und Buffetbeitrag bleiben leer.
3. A1.3: Das System speichert die Einladung mit Status ABGEMELDET.

### A2 – Einzelne Einladung nachträglich hinzufügen

> Entry point: step 2 of the main flow

1. A2.1: Der Organisator erstellt manuell eine einzelne Einladung für eine bestimmte Partei.
2. A2.2: Das System speichert die neue Einladung mit Status OFFEN.

---

## Error Scenarios

### E1 – Einladung bereits vorhanden

> Entry point: step 2 of the main flow

1. E1.1: Das System stellt fest, dass für eine Partei bereits eine Einladung für diesen Event existiert.
2. E1.2: Das System überspringt diese Partei und zeigt einen Hinweis.

---

## Postconditions

### Success

- Für alle ausgewählten Parteien existiert je eine Einladung zum Event.
- Eintreffende Rückmeldungen sind im Status und den Detailfeldern der Einladung abgebildet.

### Failure / Abort

- Bereits gespeicherte Einladungen bleiben erhalten; nicht gespeicherte Änderungen werden verworfen.

---

## Acceptance Criteria

```gherkin
Scenario: Einladungen für alle Parteien erstellen
  Given der Event "Buchlenwiese, 05.07.2025" existiert und 10 Parteien sind im System
  When der Organisator "Einladungen erstellen" für diesen Event auslöst
  Then existieren 10 Einladungen mit Status OFFEN für diesen Event

Scenario: Rückmeldung Anmeldung erfassen
  Given eine Einladung der Partei "Müller" für Event 2025 hat Status OFFEN
  When der Organisator Status ANGEMELDET, 3 Personen, hilftAufstellen=true und BuffetBeitrag SALAT erfasst
  Then hat die Einladung Status ANGEMELDET mit den entsprechenden Angaben

Scenario: Rückmeldung Abmeldung erfassen
  Given eine Einladung der Partei "Weber" für Event 2025 hat Status OFFEN
  When der Organisator Status ABGEMELDET erfasst
  Then hat die Einladung Status ABGEMELDET

Scenario: Einladungen erstellen überspringt bereits vorhandene Einladung
  Given die Partei "Müller" hat bereits eine Einladung für Event 2025
  When der Organisator "Einladungen erstellen" für Event 2025 erneut auslöst
  Then wird die Einladung der Partei "Müller" nicht dupliziert und das System zeigt einen Hinweis
```

---

## Open Items

- [x] ~~REVIEW: Die Partei ist in der Akteurstabelle als `Human`-Aktor geführt, interagiert aber nie direkt mit dem System.~~ → **Beantwortet:** Die Partei interagiert in keinem UC direkt mit dem System. Sie ist als **externer Stakeholder / sekundärer Aktor** zu verstehen — der Organisator ist der einzige primäre Aktor. Partei bleibt in der Akteurstabelle als Kontextreferenz, erhält aber den Typ `External`.

---

## Dependencies & References

- **Depends on**: UC-002 (Parteien verwalten), UC-003 (Event anlegen)
