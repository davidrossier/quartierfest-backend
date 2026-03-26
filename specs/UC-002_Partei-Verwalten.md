---
id: UC-002
type: Use Case
name: "Parteien verwalten"
completeness: Intermediate
---

# UC-002 – Parteien verwalten

---

## Brief Description

> Der Organisator möchte Parteien (Haushalte) erfassen, aktualisieren oder löschen, um die Empfängereinheiten der Einladungen zu pflegen.

---

## Actors

| Actor | Type | Role |
|---|---|---|
| Organisator | `Human` | Verwaltet Parteien und deren Personenzuordnung |

---

## Context & Background

> Eine Partei repräsentiert typischerweise einen Haushalt und ist die Einheit, die eine Einladung erhält und darauf antwortet. Eine Partei besteht aus einer oder mehreren Personen (UC-001). Für die Abrechnung via Twint wird die Mobilenummer der Partei benötigt. Parteien sind eventübergreifend und können bei jedem neuen Event wieder eingeladen werden.

---

## Preconditions

- Eine authentifizierte Sitzung des Organisators besteht.
- Für die Personenzuordnung existiert mindestens eine Person im System (UC-001).

---

## Trigger

> Der Organisator öffnet die Parteiverwaltung im System.

---

## Description

1. Der Organisator wählt die Aktion: Partei erfassen, bearbeiten oder löschen.
2. **Erfassen:** Der Organisator gibt die Adresse ein (Pflicht), setzt das Flag `twintAktiv` und gibt bei aktivem Twint die Mobilenummer ein. Das System speichert die neue Partei. *(→ E1 bei fehlendem Pflichtfeld)*
3. **Personenzuordnung:** Der Organisator weist der Partei eine oder mehrere bestehende Personen zu. Das System speichert die Zuordnung. *(→ E2 wenn keine Person ausgewählt)*
4. **Bearbeiten:** Der Organisator wählt eine bestehende Partei, ändert die gewünschten Felder und speichert. Das System aktualisiert den Datensatz.
5. **Löschen:** Der Organisator wählt eine bestehende Partei und bestätigt die Löschung. Das System entfernt die Partei. *(→ E3 wenn Partei bereits Einladungen hat)*
6. Das System zeigt die aktualisierte Parteiliste an.

---

## Alternative Flows

### A1 – Twint nicht aktiv

> Entry point: step 2 of the main flow

1. A1.1: Der Organisator setzt `twintAktiv` auf `false`.
2. A1.2: Das System blendet das Feld für die Twint-Mobilenummer aus.
3. A1.3: Die Partei wird ohne Twint-Angabe gespeichert.

---

## Error Scenarios

### E1 – Pflichtfeld fehlt

> Entry point: step 2 of the main flow

1. E1.1: Das System stellt fest, dass die Adresse leer ist.
2. E1.2: Das System zeigt eine Fehlermeldung und verhindert das Speichern.

### E2 – Keine Person zugeordnet

> Entry point: step 3 of the main flow

1. E2.1: Der Organisator versucht zu speichern ohne eine Person zuzuweisen.
2. E2.2: Das System zeigt einen Warnhinweis (Partei ohne Personen ist für Einladungen nicht nutzbar).
3. E2.3: Der Organisator kann trotzdem speichern oder die Zuordnung nachholen.

### E3 – Partei hat bestehende Einladungen

> Entry point: step 5 of the main flow

1. E3.1: Das System stellt fest, dass für die Partei bereits Einladungen existieren.
2. E3.2: Das System zeigt einen Hinweis und verhindert die Löschung.

---

## Postconditions

### Success

- Die Partei ist mit Adresse, Twint-Angabe und zugeordneten Personen im System gespeichert, aktualisiert oder gelöscht.

### Failure / Abort

- Es wurden keine Änderungen persistiert; der vorherige Zustand bleibt erhalten.

---

## Acceptance Criteria

```gherkin
Scenario: Partei mit Twint erfolgreich erfassen
  Given der Organisator ist in der Parteiverwaltung
  When er Adresse "Seestrasse 1, 3000 Bern", twintAktiv "true" und Mobilenummer "079 123 45 67" eingibt und speichert
  Then ist die Partei in der Liste vorhanden und als Twint-fähig markiert

Scenario: Partei ohne Adresse speichern schlägt fehl
  Given der Organisator ist in der Parteiverwaltung
  When er versucht eine Partei ohne Adresse zu speichern
  Then zeigt das System eine Fehlermeldung und speichert nicht

Scenario: Partei mit bestehender Einladung löschen schlägt fehl
  Given die Partei "Müller, Seestrasse 1" hat eine Einladung für Event 2025
  When der Organisator versucht die Partei zu löschen
  Then zeigt das System einen Hinweis auf bestehende Einladungen und bricht ab
```

---

## Dependencies & References

- **Depends on**: UC-001 (Personendaten verwalten) — Personen müssen vor der Zuordnung existieren
