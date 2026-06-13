---
id: UC-015
type: Use Case
name: "Benutzer verwalten"
completeness: Minimum
traceability:
  impl_status: implementiert
  endpoints:
    - "GET /api/benutzer"
    - "POST /api/benutzer"
    - "DELETE /api/benutzer/{id}"
    - "PUT /api/benutzer/{id}/passwort"
  test_ids: [TC-034, TC-035, TC-039]
  it_classes: [BenutzerVerwaltenIT]
  last_traced: "2026-06-12"
---

# UC-015 – Benutzer verwalten

---

## Brief Description

> Der Organisator möchte Benutzeraccounts direkt im System anlegen, Parteien zuordnen und Passwörter zurücksetzen, damit Parteien sich mit einer eigenen Login-Identität anmelden können.

---

## Actors

| Actor | Type | Role |
|---|---|---|
| Organisator | `Human` | Legt Benutzeraccounts an, ordnet Parteien zu, setzt Passwörter zurück, löscht Accounts |

---

## Context & Background

> **Entscheid 2026-06-12: Eigenbau statt Auth0** (→ UC-014). Benutzeraccounts liegen vollständig in der eigenen Datenbank (Entity `Benutzer`); ein externes Dashboard und das Kopieren von IdP-User-IDs entfallen. Der Organisator legt einen Account in einem Schritt direkt in der Admin-UI an: E-Mail-Adresse, Initialpasswort, Rolle und — bei Rolle `PARTEI` — die zugehörige Partei.
>
> Wenn eine Partei sich anmeldet, ermittelt das Backend über den `sub`-Claim des JWT (= Benutzer-ID) den `Benutzer`-Datensatz und dessen Partei, um den Datenzugriff einzuschränken (UC-016). Eine Partei kann von 0 bis n Benutzeraccounts vertreten werden; eine Partei ohne Account ist weiterhin vollständig durch den Organisator verwaltbar.
>
> Self-Registration ist explizit nicht vorgesehen. Passwörter werden ausschliesslich als BCrypt-Hash gespeichert und nie über die API ausgeliefert. Passwort-Reset erfolgt durch den Organisator (neues Initialpasswort setzen) — kein E-Mail-Flow.
>
> **Bootstrap:** Beim Applikationsstart wird, falls noch kein `ORGANISATOR`-Account existiert, ein initialer Organisator-Account aus den Umgebungsvariablen `AUTH_INITIAL_ADMIN_EMAIL` / `AUTH_INITIAL_ADMIN_PASSWORD` angelegt. Damit ist das Henne-Ei-Problem (Anmeldung nötig, um Benutzer anzulegen) gelöst.

---

## Frontend-Kontext

> **Route:** `/admin/benutzer` — `BenutzerVerwaltungComponent` (Angular 21, Standalone)
> Nur für Benutzer mit Rolle `ORGANISATOR` zugänglich (Route Guard).

- Formular: E-Mail-Adresse, Initialpasswort, Rollen-Auswahl (`ORGANISATOR` / `PARTEI`), Partei-Dropdown (nur aktiv bei Rolle `PARTEI`).
- Liste zeigt alle bestehenden Accounts: E-Mail, Rolle, Partei-Bezeichnung (falls zugeordnet). Passwörter werden nie angezeigt.
- Aktion «Passwort zurücksetzen»: Organisator gibt ein neues Initialpasswort ein (`PUT /api/benutzer/{id}/passwort`).
- Löschen eines Accounts widerruft den Zugriff sofort (nächster API-Call gibt 401/403).

---

## Preconditions

- Eine authentifizierte Sitzung des Organisators besteht (UC-014).
- Bei Rolle `PARTEI`: die Zielpartei existiert im System (UC-002).

---

## Trigger

> Der Organisator öffnet die Benutzerverwaltung unter `/admin/benutzer`.

---

## Description

1. Das System zeigt alle bestehenden Benutzeraccounts (E-Mail, Rolle, Partei).
2. **Account anlegen:** Der Organisator gibt E-Mail-Adresse und Initialpasswort ein, wählt die Rolle und bei `PARTEI` die zugehörige Partei. Das System speichert den Account mit BCrypt-gehashtem Passwort. *(→ E1 bei fehlendem Pflichtfeld, → E2 wenn E-Mail bereits vergeben)*
3. **Passwort zurücksetzen:** Der Organisator wählt einen Account und setzt ein neues Initialpasswort. Das System ersetzt den Passwort-Hash; bestehende Tokens bleiben bis zu ihrem Ablauf gültig.
4. **Account löschen:** Der Organisator wählt einen Account und bestätigt die Löschung. Das System entfernt den Account; der Benutzer kann sich nicht mehr anmelden. *(→ E3 beim letzten ORGANISATOR-Account)*
5. Das System zeigt die aktualisierte Liste.

---

## Error Scenarios

### E1 – Pflichtfeld fehlt

> Entry point: step 2 of the main flow

1. E1.1: Das System stellt fest, dass E-Mail, Initialpasswort oder Rolle fehlt — oder dass Rolle `PARTEI` gewählt wurde, ohne eine Partei zuzuordnen.
2. E1.2: Das System zeigt eine Fehlermeldung und verhindert das Speichern.

### E2 – E-Mail-Adresse bereits vergeben

> Entry point: step 2 of the main flow

1. E2.1: Das System stellt fest, dass bereits ein Account mit dieser E-Mail-Adresse existiert.
2. E2.2: Das System zeigt die Fehlermeldung: «Für diese E-Mail-Adresse existiert bereits ein Account.»

### E3 – Letzter Organisator-Account

> Entry point: step 4 of the main flow

1. E3.1: Das System stellt fest, dass der zu löschende Account der letzte mit Rolle `ORGANISATOR` ist.
2. E3.2: Das System verweigert die Löschung mit der Meldung: «Der letzte Organisator-Account kann nicht gelöscht werden.» (Verhindert, dass sich das System selbst aussperrt.)

---

## Postconditions

### Success

- Der Benutzeraccount ist mit BCrypt-gehashtem Passwort persistiert bzw. aktualisiert oder gelöscht.
- Ein Account mit Rolle `PARTEI` kann sich anmelden und auf die Daten der zugeordneten Partei zugreifen (UC-016).

### Failure / Abort

- Es wurden keine Änderungen persistiert; der vorherige Zustand bleibt erhalten.

---

## Acceptance Criteria

```gherkin
Scenario: Partei-Account erfolgreich anlegen
  Given die Partei "Familie Müller" existiert
  When der Organisator einen Account "mueller@quartier.ch" mit Initialpasswort, Rolle PARTEI und Partei "Familie Müller" anlegt
  Then erscheint der Account in der Liste und der Benutzer kann sich anmelden (UC-014)

Scenario: Doppelte E-Mail-Adresse wird abgewiesen
  Given ein Account "mueller@quartier.ch" existiert
  When der Organisator erneut einen Account mit "mueller@quartier.ch" anlegen will
  Then zeigt das System eine Fehlermeldung und speichert nicht

Scenario: PARTEI-Account ohne Partei wird abgewiesen
  When der Organisator einen Account mit Rolle PARTEI ohne Partei-Zuordnung anlegen will
  Then zeigt das System eine Fehlermeldung und speichert nicht

Scenario: Account löschen widerruft Zugriff
  Given der Account "mueller@quartier.ch" ist der Partei "Familie Müller" zugeordnet
  When der Organisator den Account löscht
  Then schlägt die nächste Anmeldung von "mueller@quartier.ch" fehl

Scenario: Letzter Organisator-Account kann nicht gelöscht werden
  Given es existiert genau ein Account mit Rolle ORGANISATOR
  When der Organisator diesen Account löschen will
  Then verweigert das System die Löschung mit einer Fehlermeldung

Scenario: Passwort zurücksetzen
  Given der Account "mueller@quartier.ch" existiert
  When der Organisator ein neues Initialpasswort setzt
  Then kann sich der Benutzer mit dem neuen Passwort anmelden und mit dem alten nicht mehr
```

---

## Open Items

- [x] RESOLVED (2026-06-12): 0..1 umgesetzt — ein Account vertritt höchstens eine Partei (`@ManyToOne` auf `Benutzer.partei`). n:m bei Bedarf später.
- [x] RESOLVED (2026-06-12): Passwort-Policy umgesetzt — Mindestlänge 10 Zeichen (`@Size(min = 10)` auf `Benutzer.passwort` und `PasswortReset`-Record).
- [ ] OPEN: Soll beim Passwort-Reset oder Account-Löschen ein bereits ausgestelltes Token sofort ungültig werden (Token-Blacklist), oder ist die Restgültigkeit von max. 12 h akzeptabel? Entscheid 2026-06-12: Restgültigkeit akzeptiert; Blacklist bei Bedarf nachrüsten.
- [x] RESOLVED (2026-06-12): Testbarkeit gelöst — `SecurityMatrixIT` (TC-040) läuft mit `@ActiveProfiles("security-test")` (prod-gleiche Chain); die Ownership-403-Fälle (TC-036/TC-037) laufen dank Methoden-Security bereits im Default-Profil.

---

## Dependencies & References

- **Depends on**: UC-002 (Parteien verwalten), UC-014 (Benutzer anmelden — für den Zugriff auf die Admin-UI; der erste Organisator-Account entsteht via Bootstrap)
- **Ermöglicht**: UC-014 (Anmeldung), UC-016 (Teilnahme bestätigen)

---

## Datenmodell-Erweiterung

Neue Entity `Benutzer` (neues Domain-Package `benutzer`):

| Feld | Typ | Pflicht | Hinweis |
|---|---|---|---|
| id | Long | ja | — |
| email | String | ja | Login-Identifikator; `@Column(unique = true)` |
| passwortHash | String | ja | BCrypt; wird nie über die API ausgeliefert (`@JsonIgnore` o. ä.) |
| rolle | Benutzer.Rolle | ja | Enum `ORGANISATOR` / `PARTEI` (inneres Enum, Projektkonvention) |
| partei | Partei | nein | FK → Partei; `@ManyToOne`; Pflicht bei Rolle `PARTEI`, leer bei `ORGANISATOR` |
