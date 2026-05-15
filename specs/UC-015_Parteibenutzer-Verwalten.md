---
id: UC-015
type: Use Case
name: "Parteibenutzer verwalten"
completeness: Minimum
traceability:
  impl_status: ausstehend
  endpoints:
    - "GET /api/parteibenutzer"
    - "POST /api/parteibenutzer"
    - "DELETE /api/parteibenutzer/{id}"
  test_ids: []
  it_classes: []
  last_traced: "2026-05-15"
---

# UC-015 – Parteibenutzer verwalten

---

## Brief Description

> Der Organisator möchte Auth0-Benutzeraccounts mit Parteien verknüpfen, damit Parteien sich mit einer eigenen Login-Identität im System anmelden können.

---

## Actors

| Actor | Type | Role |
|---|---|---|
| Organisator | `Human` | Erstellt und löscht Verknüpfungen zwischen Auth0-Accounts und Parteien |

---

## Context & Background

> Eine Partei kann von 0 bis n Auth0-Benutzeraccounts vertreten werden. Die Verknüpfung erfolgt über die neue Entity `ParteiBenutzer`, die den unveränderlichen `sub`-Claim des Auth0-JWT mit einer Partei verbindet. Wenn eine Partei sich anmeldet, schlägt das Backend den `sub`-Claim des Access Tokens in der `ParteiBenutzer`-Tabelle nach, um die zugehörige Partei zu ermitteln und den Datenzugriff entsprechend einzuschränken.
>
> Der Organisator legt Auth0-Accounts zuerst im Auth0-Dashboard an (E-Mail + Passwort, Rolle `PARTEI` zuweisen) und verknüpft den Account danach über die Admin-UI mit der entsprechenden Partei. Eine Partei ohne Verknüpfung erhält keinen PARTEI-seitigen Zugriff auf ihre Daten, ist aber weiterhin durch den Organisator verwaltbar.

---

## Frontend-Kontext

> **Route:** `/admin/benutzer` — `ParteibenutzerVerwaltungComponent` (Angular 21, Standalone)
> Nur für Benutzer mit Rolle `ORGANISATOR` zugänglich (Route Guard).

- Formular: Dropdown zur Partei-Auswahl (alle existierenden Parteien), Eingabefeld für Auth0-User-ID (`sub`, z. B. `auth0|abc123`), optionales Feld für E-Mail-Adresse (nur zur Anzeige, wird nicht für Auth genutzt).
- Liste zeigt alle bestehenden Verknüpfungen: Partei-Bezeichnung, Auth0-User-ID, E-Mail.
- Löschen eines Eintrags widerruft den PARTEI-Datenzugriff sofort (nächster API-Call gibt 403).

---

## Preconditions

- Eine authentifizierte Sitzung des Organisators besteht (UC-014).
- Die Zielpartei existiert im System (UC-002).
- Der Auth0-Account des Zielbenutzers wurde im Auth0-Dashboard angelegt und mit der Rolle `PARTEI` versehen.
- Die Auth0-User-ID (`sub`) des Zielbenutzers ist dem Organisator bekannt (abrufbar im Auth0-Dashboard).

---

## Trigger

> Der Organisator öffnet die Benutzerverwaltung unter `/admin/benutzer`.

---

## Description

1. Das System zeigt alle bestehenden Parteibenutzer-Verknüpfungen (Partei, Auth0-User-ID, E-Mail).
2. **Verknüpfung erstellen:** Der Organisator wählt eine Partei aus dem Dropdown, gibt die Auth0-User-ID (`sub`) ein und optional die E-Mail-Adresse. Das System speichert die Verknüpfung. *(→ E1 bei fehlendem Pflichtfeld, → E2 wenn Auth0-User-ID bereits vergeben)*
3. **Verknüpfung löschen:** Der Organisator wählt einen bestehenden Eintrag und bestätigt die Löschung. Das System entfernt die Verknüpfung; der Benutzer verliert damit den PARTEI-Zugriff auf die zugehörigen Daten.
4. Das System zeigt die aktualisierte Liste der Verknüpfungen.

---

## Error Scenarios

### E1 – Pflichtfeld fehlt

> Entry point: step 2 of the main flow

1. E1.1: Das System stellt fest, dass Partei oder Auth0-User-ID nicht angegeben wurde.
2. E1.2: Das System zeigt eine Fehlermeldung und verhindert das Speichern.

### E2 – Auth0-User-ID bereits einer Partei zugeordnet

> Entry point: step 2 of the main flow

1. E2.1: Das System stellt fest, dass die eingegebene Auth0-User-ID bereits mit einer Partei verknüpft ist.
2. E2.2: Das System zeigt die Fehlermeldung: «Diese Auth0-User-ID ist bereits der Partei [Bezeichnung] zugeordnet.»

---

## Postconditions

### Success

- Die Verknüpfung zwischen Auth0-Account und Partei ist persistiert.
- Der verknüpfte Benutzer kann sich anmelden und auf die Daten der zugeordneten Partei zugreifen (UC-016).

### Failure / Abort

- Es wurden keine Änderungen persistiert; der vorherige Zustand bleibt erhalten.

---

## Acceptance Criteria

```gherkin
Scenario: Parteibenutzer-Verknüpfung erfolgreich erstellen
  Given die Partei "Familie Müller" existiert
  And der Auth0-Account "auth0|abc123" hat die Rolle PARTEI
  When der Organisator die Auth0-User-ID "auth0|abc123" mit "Familie Müller" verknüpft
  Then ist die Verknüpfung in der Liste sichtbar

Scenario: Doppelte Auth0-User-ID wird abgewiesen
  Given "auth0|abc123" ist bereits der Partei "Familie Müller" zugeordnet
  When der Organisator "auth0|abc123" erneut einer anderen Partei zuordnen will
  Then zeigt das System eine Fehlermeldung und speichert nicht

Scenario: Verknüpfung löschen widerruft Datenzugriff
  Given "auth0|abc123" ist der Partei "Familie Müller" zugeordnet
  When der Organisator die Verknüpfung löscht
  Then erhält der nächste API-Call von "auth0|abc123" auf Teilnahmedaten HTTP 403

Scenario: Partei ohne Verknüpfung hat keinen PARTEI-Zugriff
  Given die Partei "Familie Meier" hat keine ParteiBenutzer-Verknüpfung
  Then kann kein Auth0-Account als PARTEI auf die Daten von "Familie Meier" zugreifen
```

---

## Open Items

- [ ] OPEN: Soll die Auth0-User-ID manuell eingegeben werden, oder soll die UI eine Live-Suche gegen die Auth0 Management API anbieten? (Management API benötigt separaten M2M-Grant im Auth0-Tenant)
- [ ] OPEN: Kann ein Auth0-Account mehrere Parteien vertreten (n:m), oder maximal eine (unique-Constraint auf `idpSub`)? Aktuell modelliert als unique — ein Account vertritt genau eine Partei.

---

## Dependencies & References

- **Depends on**: UC-002 (Parteien verwalten), UC-014 (Benutzer anmelden)
- **Ermöglicht**: UC-016 (Teilnahme bestätigen)

---

## Datenmodell-Erweiterung

Neue Entity `ParteiBenutzer` (neues Domain-Package `parteibenutzer`):

| Feld | Typ | Pflicht | Hinweis |
|---|---|---|---|
| id | Long | ja | — |
| partei | Partei | ja | FK → Partei; `@ManyToOne` |
| idpSub | String | ja | `sub`-Claim des Auth0-JWT; `@Column(unique = true)` |
| email | String | nein | Nur zur Anzeige; wird nicht für Authentifizierung verwendet |
