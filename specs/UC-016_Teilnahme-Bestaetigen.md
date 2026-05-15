---
id: UC-016
type: Use Case
name: "Teilnahme bestätigen"
completeness: Minimum
traceability:
  impl_status: ausstehend
  endpoints:
    - "GET /api/teilnahmen/meine"
    - "PUT /api/teilnahmen/{id}"
  test_ids: []
  it_classes: []
  last_traced: "2026-05-15"
---

# UC-016 – Teilnahme bestätigen

---

## Brief Description

> Die Partei möchte ihre eigene Teilnahme an einem Event einsehen und anpassen, um dem Organisator verlässliche Planungsangaben zu ihren Personen und Buffet-Beiträgen zu liefern.

---

## Actors

| Actor | Type | Role |
|---|---|---|
| Partei | `Human` | Bestätigt und aktualisiert die eigene Teilnahme |
| Organisator | `Human` | Kann Teilnahmeangaben der Partei jederzeit überschreiben |

---

## Context & Background

> Die Teilnahme (UC-005) wird durch den Organisator aus einer ANGEMELDET-Einladung erstellt. Im vorliegenden Use Case erhält die Partei die Möglichkeit, ihre eigenen Angaben direkt zu pflegen: Anzahl anwesender Personen, Hilfe beim Auf- und Abbauen sowie Buffet-Beiträge.
>
> Der Organisator kann alle Felder jederzeit überschreiben — seine Sicht (UC-005) hat immer Vorrang. Es besteht kein Konfliktmechanismus; der zuletzt gespeicherte Wert gilt. Dies gilt für beide Richtungen: sowohl Änderungen der Partei als auch des Organisators werden ohne Warnung gespeichert.
>
> Der Zugriff der Partei ist auf ihre eigene Teilnahme beschränkt. Das Backend ermittelt die zugehörige Partei anhand des `sub`-Claims im Access Token via der `ParteiBenutzer`-Tabelle (UC-015) und verweigert Zugriff auf fremde Teilnahmen mit HTTP 403.

---

## Frontend-Kontext

> **Route:** `/meine-teilnahme` — `MeineTeilnahmeComponent` (Angular 21, Standalone)
> Nur für Benutzer mit Rolle `PARTEI` zugänglich (Route Guard).

- Das System zeigt die Teilnahme der eigenen Partei für den nächsten Event (frühestes Event-Datum in der Zukunft).
- Das Formular zeigt: Anzahl Personen (`anzahlPersonenEffektiv`), Hilft Aufstellen, Hilft Aufräumen, Buffet-Beiträge (Art + Beschreibung, beliebig viele Einträge).
- Buffet-Beiträge können dynamisch hinzugefügt und entfernt werden (analog UC-005).
- Speichern sendet `PUT /api/teilnahmen/{id}`.
- Die Seite ist für den Organisator **nicht** zugänglich — er nutzt UC-005.

---

## Preconditions

- Eine authentifizierte Sitzung der Partei besteht (UC-014).
- Der Auth0-Account der Partei ist mit ihrer Partei verknüpft (UC-015).
- Eine Teilnahme für die Partei wurde vom Organisator aus einer ANGEMELDET-Einladung erstellt (UC-005).

---

## Trigger

> Die Partei öffnet die Seite «Meine Teilnahme» nach dem Anmelden.

---

## Description

1. Das System ermittelt die Partei der angemeldeten Benutzerin via `sub`-Claim → `ParteiBenutzer` → `Partei`.
2. Das System lädt die Teilnahme der Partei für den nächsten Event via `GET /api/teilnahmen/meine`. *(→ E1 wenn noch keine Teilnahme vorhanden)*
3. Das System zeigt die aktuellen Angaben: Anzahl Personen, Hilft Aufstellen, Hilft Aufräumen, Buffet-Beiträge.
4. Die Partei passt die Angaben an.
5. Das System speichert die Änderungen via `PUT /api/teilnahmen/{id}` und validiert den Zugriff. *(→ E2 bei Zugriff auf fremde Teilnahme)*
6. Das System zeigt eine Bestätigung und die aktualisierten Angaben.

---

## Alternative Flows

### A1 – Organisator überschreibt Angaben der Partei

> Entry point: nach Schritt 5 des Hauptflows, unabhängig ausgelöst via UC-005

1. A1.1: Der Organisator öffnet die Teilnahmeübersicht in UC-005 und ändert Angaben der Partei.
2. A1.2: Das System speichert die Änderungen des Organisators ohne Rückfrage.
3. A1.3: Beim nächsten Laden der Seite durch die Partei werden die überschriebenen Werte angezeigt.
4. A1.4: Es wird kein Konflikt signalisiert — der zuletzt gespeicherte Wert gilt.

---

## Error Scenarios

### E1 – Noch keine Teilnahme vorhanden

> Entry point: step 2 of the main flow

1. E1.1: Das System stellt fest, dass für die Partei keine Teilnahme existiert (Einladung noch nicht auf ANGEMELDET gesetzt oder Teilnahme noch nicht erstellt).
2. E1.2: Das System zeigt den Hinweis: «Für Ihren Haushalt wurde noch keine Teilnahme erstellt. Bitte wenden Sie sich an den Organisator.»

### E2 – Zugriff auf fremde Teilnahme

> Entry point: step 5 of the main flow

1. E2.1: Das Backend stellt fest, dass die angefragte Teilnahme nicht der authentifizierten Partei gehört.
2. E2.2: Das Backend antwortet mit HTTP 403.
3. E2.3: Das Frontend zeigt eine generische Fehlermeldung.

---

## Postconditions

### Success

- Die Teilnahmeangaben der Partei sind aktualisiert und im System gespeichert.
- Der Organisator sieht die aktualisierten Angaben in der Teilnahmeübersicht (UC-005).

### Failure / Abort

- Es wurden keine Änderungen persistiert; der vorherige Zustand bleibt erhalten.

---

## Acceptance Criteria

```gherkin
Scenario: Partei sieht eigene Teilnahme nach Anmeldung
  Given die Partei "Familie Müller" hat eine Teilnahme für den nächsten Event
  And der Auth0-Account "auth0|abc123" ist mit "Familie Müller" verknüpft
  When der Benutzer "auth0|abc123" sich anmeldet und "/meine-teilnahme" aufruft
  Then sieht er die Teilnahmedaten der Partei "Familie Müller"

Scenario: Partei aktualisiert Anzahl Personen und Buffet-Beitrag
  Given die Partei "Familie Müller" hat eine Teilnahme mit anzahlPersonenEffektiv=3
  When sie den Wert auf 4 setzt, einen Buffet-Beitrag (SALAT, "Rüebli-Salat") hinzufügt und speichert
  Then hat die Teilnahme anzahlPersonenEffektiv=4 und einen Buffet-Beitrag

Scenario: Organisator überschreibt Angaben der Partei ohne Konfliktmeldung
  Given die Partei hat anzahlPersonenEffektiv=4 gesetzt
  When der Organisator den Wert in UC-005 auf 2 korrigiert
  Then sieht die Partei beim nächsten Laden anzahlPersonenEffektiv=2 ohne Fehlermeldung

Scenario: Partei kann nicht auf fremde Teilnahme zugreifen
  Given der Auth0-Account "auth0|abc123" ist mit Partei "Familie Müller" verknüpft
  When er versucht die Teilnahme der Partei "Familie Meier" zu bearbeiten
  Then antwortet das Backend mit HTTP 403

Scenario: Noch keine Teilnahme vorhanden
  Given die Partei "Familie Müller" hat zwar eine Einladung, aber noch keine Teilnahme
  When der Benutzer "/meine-teilnahme" aufruft
  Then zeigt das System den Hinweis "Für Ihren Haushalt wurde noch keine Teilnahme erstellt"
```

---

## Open Items

- [ ] OPEN: Welcher Event gilt als «nächster Event»? Frühestes Event-Datum in der Zukunft? Oder kann die Partei zwischen mehreren Events wählen?
- [ ] OPEN: Soll die Partei zusätzlich zur Teilnahme auch ihre Einladung (UC-004) und Abrechnung (UC-011) einsehen können?

---

## Dependencies & References

- **Depends on**: UC-005 (Teilnahmen verwalten), UC-014 (Benutzer anmelden), UC-015 (Parteibenutzer verwalten)
- **Erweitert**: UC-005 — fügt Schreibzugriff für Rolle PARTEI via `PUT /api/teilnahmen/{id}` hinzu; dieser Endpunkt existiert noch nicht und muss neu implementiert werden
