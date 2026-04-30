---
id: UC-002
type: Use Case
name: "Parteien verwalten"
completeness: Intermediate
traceability:
  impl_status: vollständig
  endpoints:
    - "GET /api/parteien"
    - "POST /api/parteien"
    - "PUT /api/parteien/{id}"
    - "DELETE /api/parteien/{id}"
  test_ids:
    - TC-004
    - TC-005
    - TC-030
  it_classes:
    - ParteiVerwaltenIT
  last_traced: "2026-05-01"
---

# UC-002 – Parteien verwalten

---

## Brief Description

> Der Organisator möchte Parteien (Haushalte) mit einem bezeichnenden Namen erfassen, aktualisieren, Personen zuordnen oder löschen, um die Empfängereinheiten der Einladungen zu pflegen.

---

## Actors

| Actor | Type | Role |
|---|---|---|
| Organisator | `Human` | Verwaltet Parteien und deren Personenzuordnung |

---

## Context & Background

> Eine Partei repräsentiert typischerweise einen Haushalt und ist die Einheit, die eine Einladung erhält und darauf antwortet. Jede Partei trägt einen bezeichnenden Namen (z. B. „Familie Müller" oder „Meier/Huber"), der sie in Listen eindeutig identifizierbar macht. Eine Partei besteht aus einer oder mehreren Personen (UC-001), die über eine Tabelle zugeordnet werden. Für die Abrechnung via Twint wird die Mobilenummer der Partei benötigt. Parteien sind eventübergreifend und können bei jedem neuen Event wieder eingeladen werden.

---

## Frontend-Kontext

> **Route:** `/parteien` — `ParteienVerwaltungComponent` (Angular 21, Standalone)

- Formularvalidierung clientseitig: `bezeichnung` und `adresse` sind `Validators.required`. `twintMobilenummer` wird nur übermittelt, wenn `twintAktiv = true`; das Feld wird im Payload weggelassen, sobald Twint inaktiv ist.
- Die Personenauswahl zeigt **alle im System erfassten Personen** als Toggle-Liste (`togglePerson()` via `Set<number>`). Beim Bearbeiten einer bestehenden Partei werden die aktuell zugeordneten Personen vorausgewählt. Die ausgewählten IDs werden als `personenIds`-Array übermittelt.
- Löschen öffnet einen Browser-`confirm()`-Dialog.
- Liste ist sortierbar nach bezeichnung, adresse, twintAktiv, twintMobilenummer.

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
2. **Erfassen:** Der Organisator gibt den bezeichnenden Namen (Pflicht) und die Adresse (Pflicht) ein, setzt das Flag `twintAktiv` und gibt bei aktivem Twint die Mobilenummer ein. Das System speichert die neue Partei. *(→ E1 bei fehlendem Pflichtfeld, → A1 wenn Twint nicht aktiv)*
3. **Personenzuordnung:** Das System zeigt eine Tabelle aller im System erfassten Personen (Spalten: Vorname, Name, Mobilenummer). Der Organisator wählt eine oder mehrere Personen aus und bestätigt die Zuordnung. Das System speichert die Zuordnung. *(→ E2 wenn keine Person ausgewählt)*
4. **Bearbeiten:** Der Organisator wählt eine bestehende Partei, ändert die gewünschten Felder (inkl. Bezeichnung, Adresse, Twint-Angabe, Personen) und speichert. Das System aktualisiert den Datensatz. *(→ E1 bei fehlendem Pflichtfeld)*
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

> Entry point: step 2 or 4 of the main flow

1. E1.1: Das System stellt fest, dass Bezeichnung oder Adresse leer ist.
2. E1.2: Das System zeigt eine Fehlermeldung und verhindert das Speichern.

### E2 – Keine Person zugeordnet

> Entry point: step 3 of the main flow

1. E2.1: Der Organisator versucht zu speichern ohne eine Person in der Tabelle auszuwählen.
2. E2.2: Das System zeigt einen Warnhinweis (Partei ohne Personen ist für Einladungen nicht nutzbar).
3. E2.3: Der Organisator kann trotzdem speichern oder die Zuordnung nachholen.

### E3 – Partei hat bestehende Einladungen

> Entry point: step 5 of the main flow

1. E3.1: Das System stellt fest, dass für die Partei bereits Einladungen existieren.
2. E3.2: Das System zeigt einen Hinweis und verhindert die Löschung.

---

## Postconditions

### Success

- Die Partei ist mit Bezeichnung, Adresse, Twint-Angabe und zugeordneten Personen im System gespeichert, aktualisiert oder gelöscht.
- Die Parteiliste spiegelt den aktuellen Stand wider.

### Failure / Abort

- Es wurden keine Änderungen persistiert; der vorherige Zustand bleibt erhalten.

---

## Acceptance Criteria

```gherkin
Scenario: Partei mit Bezeichnung und Twint erfolgreich erfassen
  Given der Organisator ist in der Parteiverwaltung
  When er Bezeichnung "Familie Müller", Adresse "Seestrasse 1, 3000 Bern", twintAktiv "true" und Mobilenummer "+41791234567" eingibt und speichert
  Then ist die Partei "Familie Müller" in der Liste vorhanden und als Twint-fähig markiert

Scenario: Personen einer Partei über Tabelle zuordnen
  Given die Partei "Familie Müller" ist im System vorhanden
  And die Personen "Anna Müller" und "Beat Müller" sind im System vorhanden
  When der Organisator in der Personentabelle "Anna Müller" und "Beat Müller" auswählt und speichert
  Then sind "Anna Müller" und "Beat Müller" der Partei "Familie Müller" zugeordnet

Scenario: Partei ohne Bezeichnung speichern schlägt fehl
  Given der Organisator ist in der Parteiverwaltung
  When er versucht eine Partei ohne Bezeichnung zu speichern
  Then zeigt das System eine Fehlermeldung und speichert nicht

Scenario: Partei ohne Adresse speichern schlägt fehl
  Given der Organisator ist in der Parteiverwaltung
  When er versucht eine Partei ohne Adresse zu speichern
  Then zeigt das System eine Fehlermeldung und speichert nicht

Scenario: Partei mit bestehender Einladung löschen schlägt fehl
  Given die Partei "Familie Müller" hat eine Einladung für Event 2025
  When der Organisator versucht die Partei zu löschen
  Then zeigt das System einen Hinweis auf bestehende Einladungen und bricht ab
```

---

## Dependencies & References

- **Depends on**: UC-001 (Personendaten verwalten) — Personen müssen vor der Zuordnung existieren

---

## Open Items

- [x] ~~OPEN: Ist das Feld `bezeichnung` in der Partei-Entity als neues Pflichtfeld zu ergänzen?~~ → Implementiert als `@Column(nullable = false)` in `Partei.java`.
- [x] ~~OPEN: Zeigt die Personentabelle in der UI alle Personen im System (mit Auswahl-Checkbox), oder nur die bereits zugeordneten Personen?~~ → **Beantwortet:** Die Tabelle zeigt alle Personen im System. Beim Bearbeiten einer Partei werden die zugeordneten Personen vorausgewählt (`selectedPersonenIds = new Set(partei.personen.map(p => p.id))`); beim Neuanlegen ist keine Person vorausgewählt.
