---
id: UC-005
type: Use Case
name: "Teilnahmen verwalten"
completeness: Intermediate
traceability:
  impl_status: vollständig
  endpoints:
    - "GET /api/teilnahmen"
    - "POST /api/teilnahmen"
    - "DELETE /api/teilnahmen/{id}"
  test_ids:
    - TC-011
    - TC-012
    - TC-033
  it_classes:
    - TeilnahmeVerwaltenIT
  last_traced: "2026-05-01"
---

# UC-005 – Teilnahmen verwalten

---

## Brief Description

> Der Organisator möchte die Teilnahmedaten der angemeldeten Parteien konsolidiert einsehen und nachträglich anpassen, um eine verlässliche Planungsgrundlage für den Event zu haben.

---

## Actors

| Actor | Type | Role |
|---|---|---|
| Organisator | `Human` | Pflegt und korrigiert Teilnahmedaten |

---

## Context & Background

> Eine Teilnahme ist die konsolidierte, editierbare Sicht der tatsächlichen Anwesenheit einer Partei. Sie wird aus einer ANGEMELDET-Einladung abgeleitet und kann vom Organisator bis zum Event und danach (für die Abrechnung) angepasst werden. Das Feld `anzahlPersonenEffektiv` kann von den ursprünglichen Einladungsangaben abweichen und ist massgebend für die Abrechnung. Teilnahmen sind Grundlage für Konsumationen (UC-010) und Abrechnungen (UC-011).

---

## Frontend-Kontext

> **Route:** `/planung/teilnahmen` — `TeilnahmenVerwaltungComponent` (Angular 21, Standalone)
> Event-kontextabhängig: die Liste wird nach dem gewählten Event gefiltert.

- **"Teilnahmen aus Einladungen erstellen"** (`teilnahmenAusEinladungenErstellen()`): erstellt via `forkJoin` für alle ANGEMELDET-Einladungen des Events, die noch **keine Teilnahme** haben, einen Teilnahme-Datensatz. `anzahlPersonenEffektiv` wird dabei aus `einladung.anzahlPersonen` übernommen.
- Das Formular erlaubt ausschliesslich das **Bearbeiten** bestehender Teilnahmen — eine manuelle Neuerstellung ohne Einladungsbasis ist über die UI nicht möglich.
- Buffet-Beiträge (`buffetBeitraege`) sind als Liste gespeichert: eine Partei kann **mehrere Beiträge** anbieten, jeder mit einer Art (SALAT, BROT_ZOPF, DESSERT, WEITERE) und einer optionalen Beschreibung. Im Formular können Beiträge dynamisch hinzugefügt und entfernt werden.
- Beim Erstellen aus Einladungen wird ein vorhandener Einladungs-Buffetbeitrag (sofern nicht KEINER) als einzelner Listeneintrag übernommen.
- Löschen öffnet `confirm()`-Dialog.
- Liste sortierbar nach partei, anzahlPersonenEffektiv.

---

## Preconditions

- Eine authentifizierte Sitzung des Organisators besteht.
- Mindestens eine Einladung mit Status ANGEMELDET existiert (UC-004).

---

## Trigger

> Der Organisator öffnet die Teilnahmeübersicht für einen Event.

---

## Description

1. Das System zeigt alle Teilnahmen des ausgewählten Events mit den aus den Einladungen übernommenen Angaben.
2. Der Organisator prüft die Angaben und korrigiert bei Bedarf:
   - Effektive Anzahl Personen (`anzahlPersonenEffektiv`)
   - Hilft beim Aufstellen
   - Hilft beim Aufräumen
   - Buffet-Beiträge: beliebig viele Einträge, jeder mit Art (SALAT, BROT_ZOPF, DESSERT, WEITERE) und optionaler Beschreibung
3. Das System speichert die Änderungen.
4. Der Organisator erhält eine konsolidierte Übersicht aller Teilnehmenden (Anzahl Personen, Buffetbeiträge, Helfende).

---

## Alternative Flows

### A1 – Effektive Personenzahl weicht ab

> Entry point: step 2 of the main flow

1. A1.1: Am Event-Tag stellt der Organisator fest, dass eine Partei mit mehr oder weniger Personen erschienen ist.
2. A1.2: Der Organisator aktualisiert `anzahlPersonenEffektiv` entsprechend.
3. A1.3: Das System speichert den aktualisierten Wert — dieser Wert wird für die Abrechnung verwendet.

---

## Error Scenarios

### E1 – Keine Teilnahme vorhanden

> Entry point: step 1 of the main flow

1. E1.1: Das System stellt fest, dass keine Einladungen mit Status ANGEMELDET existieren.
2. E1.2: Das System zeigt eine leere Liste mit einem entsprechenden Hinweis.

---

## Postconditions

### Success

- Alle Teilnahmedaten sind aktuell und korrekt im System gespeichert.
- Die effektive Personenzahl je Teilnahme ist für die Abrechnung massgebend.

### Failure / Abort

- Nicht gespeicherte Änderungen werden verworfen; der vorherige Zustand bleibt erhalten.

---

## Acceptance Criteria

```gherkin
Scenario: Konsolidierte Teilnahmeübersicht anzeigen
  Given 5 Einladungen für Event 2025 haben Status ANGEMELDET
  When der Organisator die Teilnahmeübersicht für Event 2025 öffnet
  Then werden 5 Teilnahmen mit ihren Angaben angezeigt

Scenario: Effektive Personenzahl nachträglich anpassen
  Given die Teilnahme der Partei "Müller" hat anzahlPersonenEffektiv=3
  When der Organisator den Wert auf 4 ändert und speichert
  Then hat die Teilnahme der Partei "Müller" anzahlPersonenEffektiv=4

Scenario: Teilnahmeübersicht ohne Anmeldungen ist leer
  Given kein Event hat Einladungen mit Status ANGEMELDET
  When der Organisator die Teilnahmeübersicht öffnet
  Then zeigt das System eine leere Liste mit Hinweis

Scenario: Mehrere Buffet-Beiträge erfassen
  Given die Teilnahme der Partei "Müller" hat keine Buffet-Beiträge
  When der Organisator zwei Beiträge hinzufügt (Salat mit "Rüebli-Salat" und Dessert mit "Tiramisu") und speichert
  Then hat die Teilnahme der Partei "Müller" zwei Buffet-Beiträge mit den entsprechenden Beschreibungen
```

---

## Open Items

- [x] ~~REVIEW: Wann und durch wen werden Teilnahmen erstellt?~~ → **Beantwortet:** Teilnahmen werden **explizit** durch den Organisator über den Button "Teilnahmen aus Einladungen erstellen" erzeugt (`TeilnahmenVerwaltungComponent.teilnahmenAusEinladungenErstellen()`). Es gibt **keine automatische Erstellung** beim Setzen einer Einladung auf ANGEMELDET. `POST /api/teilnahmen` wird clientseitig via `forkJoin` für alle fehlenden Teilnahmen aufgerufen.

---

## Dependencies & References

- **Depends on**: UC-004 (Einladung erstellen und verwalten)
