---
id: UC-001
type: Use Case
name: "Personendaten verwalten"
completeness: Intermediate
---

# UC-001 – Personendaten verwalten

---

## Brief Description

> Der Organisator möchte Personendaten erfassen, aktualisieren oder löschen, um einen wiederverwendbaren Stamm an Einladungsempfängern über mehrere Events hinweg zu pflegen.

---

## Actors

| Actor | Type | Role |
|---|---|---|
| Organisator | `Human` | Verwaltet die Stammdaten der Personen im System |

---

## Context & Background

> Personen sind die physischen Empfänger von Einladungen. Da dieselben Personen an mehreren Quartierfest-Ausgaben teilnehmen können, werden sie eventübergreifend im System geführt. Personen sind immer einer Partei zugeordnet (UC-002); die Partei erhält die eigentliche Einladung. Eine Person kann ohne Partei-Zuordnung existieren, ist jedoch für die Einladungslogik erst relevant, wenn sie einer Partei angehört.

---

## Preconditions

- Eine authentifizierte Sitzung des Organisators besteht.

---

## Trigger

> Der Organisator öffnet die Personenverwaltung im System.

---

## Description

1. Der Organisator wählt die Aktion: Person erfassen, bearbeiten oder löschen.
2. **Erfassen:** Der Organisator gibt Vorname und Name ein (Pflicht) sowie optional Telefonnummer, Mobilenummer und E-Mail-Adresse. Das System speichert die neue Person. *(→ E1 bei fehlendem Pflichtfeld)*
3. **Bearbeiten:** Der Organisator wählt eine bestehende Person, ändert die gewünschten Felder und speichert. Das System aktualisiert den Datensatz. *(→ E1 bei fehlendem Pflichtfeld)*
4. **Löschen:** Der Organisator wählt eine bestehende Person und bestätigt die Löschung. Das System entfernt die Person. *(→ E2 wenn Person einer Partei zugeordnet ist)*
5. Das System zeigt die aktualisierte Personenliste an.

---

## Error Scenarios

### E1 – Pflichtfeld fehlt

> Entry point: step 2 or 3 of the main flow

1. E1.1: Das System stellt fest, dass Vorname oder Name leer ist.
2. E1.2: Das System zeigt eine Fehlermeldung und verhindert das Speichern.
3. E1.3: Der Organisator korrigiert die Eingabe und wiederholt den Speichervorgang.

### E2 – Person ist einer Partei zugeordnet

> Entry point: step 4 of the main flow

1. E2.1: Das System stellt fest, dass die Person noch einer oder mehreren Parteien zugeordnet ist.
2. E2.2: Das System zeigt einen Hinweis und verhindert die Löschung.
3. E2.3: Der Organisator entfernt zuerst die Partei-Zuordnung und wiederholt den Löschvorgang.

---

## Postconditions

### Success

- Der Personendatensatz ist im System gespeichert, aktualisiert oder gelöscht.
- Die Personenliste spiegelt den aktuellen Stand wider.

### Failure / Abort

- Es wurden keine Änderungen persistiert; der vorherige Zustand bleibt erhalten.

---

## Acceptance Criteria

```gherkin
Scenario: Person erfolgreich erfassen
  Given der Organisator ist in der Personenverwaltung
  When er Vorname "Anna" und Name "Müller" eingibt und speichert
  Then ist die Person "Anna Müller" in der Personenliste vorhanden

Scenario: Person erfolgreich bearbeiten
  Given die Person "Anna Müller" ist im System vorhanden
  When der Organisator die Mobilenummer auf "+41791234567" setzt und speichert
  Then enthält der Datensatz "Anna Müller" die Mobilenummer "+41791234567"

Scenario: Person ohne Pflichtfeld speichern schlägt fehl
  Given der Organisator ist in der Personenverwaltung
  When er nur einen Vornamen eingibt und versucht zu speichern
  Then zeigt das System eine Fehlermeldung und speichert nicht

Scenario: Person löschen die einer Partei zugeordnet ist schlägt fehl
  Given die Person "Anna Müller" ist der Partei "Müller, Seestrasse 1" zugeordnet
  When der Organisator versucht "Anna Müller" zu löschen
  Then zeigt das System einen Hinweis auf die bestehende Partei-Zuordnung und bricht ab
```

---

