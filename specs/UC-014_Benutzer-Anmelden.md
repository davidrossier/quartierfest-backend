---
id: UC-014
type: Use Case
name: "Benutzer anmelden"
completeness: Minimum
traceability:
  impl_status: ausstehend
  endpoints:
    - "Backend: JWT-Validierung via JWKS bereits implementiert (AUTH-001)"
  test_ids: []
  it_classes: []
  last_traced: "2026-05-15"
---

# UC-014 – Benutzer anmelden

---

## Brief Description

> Der Benutzer (Organisator oder Partei) möchte sich im System authentifizieren, um auf die für seine Rolle bestimmten Funktionen zuzugreifen.

---

## Actors

| Actor | Type | Role |
|---|---|---|
| Organisator | `Human` | Authentifiziert sich für vollen Zugriff auf alle Domänen |
| Partei | `Human` | Authentifiziert sich für rollenbasierten Zugriff auf eigene Daten |
| Auth0 | `System` | Identity Provider; führt Authentifizierung durch und stellt JWT aus |

---

## Context & Background

> Das System verwendet OAuth 2.0 Authorization Code Flow with PKCE (Proof Key for Code Exchange) über Auth0 als Identity Provider. Das Angular-Frontend initiiert den Login-Flow; das Spring-Boot-Backend validiert das Access-Token via JWKS-Endpoint. Die Rollenzuordnung (ORGANISATOR / PARTEI) erfolgt in Auth0 und wird als Custom Claim im JWT-Access-Token mitgeführt.
>
> Die Backend-Absicherung via Spring Security und JWT-Validierung ist bereits implementiert (AUTH-001). Ausstehend sind der Frontend-Login-Flow sowie die rollenbasierte Navigation nach Anmeldung.

---

## Frontend-Kontext

> **Callback-Route:** `/callback` — Auth0 PKCE Redirect-Ziel
> **Login-Seite:** `/login` — Einstiegspunkt für unauthentifizierte Benutzer
> **Library:** `@auth0/auth0-angular`

- Route Guard (`AuthGuard`) schützt alle App-Routen und leitet unauthentifizierte Benutzer zu `/login` weiter.
- HTTP-Interceptor hängt `Authorization: Bearer <access_token>` automatisch an alle Requests gegen `/api/**`.
- Nach erfolgreichem Login wird rollenbasiert geroutet:
  - `ORGANISATOR` → `/personen` (bestehende App-Einstiegsseite)
  - `PARTEI` → `/meine-teilnahme` (UC-016)
- Token wird im Memory des Angular-Prozesses gehalten (kein LocalStorage); bei Page-Reload startet Silent Authentication.

---

## Preconditions

- Der Benutzer verfügt über einen aktiven Account im Auth0-Tenant des Systems.
- Der Account ist der Rolle `ORGANISATOR` oder `PARTEI` zugeordnet.
- Die Auth0-Applikation (SPA) und die API-Audience sind im Auth0-Tenant konfiguriert.
- Der Auth0-Custom-Action «Add Roles to Token» ist aktiv und fügt den Rollen-Claim in den Access Token ein.

---

## Trigger

> Der Benutzer öffnet die Applikation ohne aktive Sitzung, oder seine Sitzung ist abgelaufen.

---

## Description

1. Der Route Guard stellt fest, dass keine aktive Sitzung besteht.
2. Das System leitet den Benutzer auf die Login-Seite (`/login`) weiter.
3. Der Benutzer klickt auf «Anmelden». Das System initiiert den PKCE-Flow und leitet zu Auth0 weiter.
4. Der Benutzer authentifiziert sich bei Auth0 (E-Mail + Passwort).
5. Auth0 leitet mit einem Authorization Code zur Callback-Route (`/callback`) zurück.
6. Das Frontend tauscht den Authorization Code gegen Access Token und Refresh Token aus.
7. Der HTTP-Interceptor registriert das Access Token für alle künftigen API-Requests.
8. Das System liest den Rollen-Claim aus dem Access Token und leitet den Benutzer zur rollenbasierten Einstiegsseite weiter. *(→ A1 wenn Rolle unbekannt oder fehlend)*

---

## Alternative Flows

### A1 – Unbekannte oder fehlende Rolle

> Entry point: step 8 of the main flow

1. A1.1: Das System stellt fest, dass der Access Token keinen gültigen Rollen-Claim enthält.
2. A1.2: Das System zeigt die Fehlermeldung: «Ihr Account hat keine gültige Rollenzuordnung. Bitte kontaktieren Sie den Organisator.»
3. A1.3: Der Benutzer wird nicht weitergeroutet; die Login-Seite bleibt aktiv.

### A2 – Silent Authentication (Seitenreload)

> Entry point: step 1 of the main flow

1. A2.1: Das System erkennt, dass ein Refresh-Token vorliegt.
2. A2.2: Das Frontend führt Silent Authentication durch (kein sichtbarer Redirect).
3. A2.3: Bei Erfolg wird die ursprünglich angeforderte Route geladen.
4. A2.4: Bei Ablauf des Refresh-Tokens → zurück zu Schritt 2 des Hauptflows.

---

## Error Scenarios

### E1 – Authentifizierung bei Auth0 fehlgeschlagen

> Entry point: step 4 of the main flow

1. E1.1: Auth0 meldet fehlgeschlagene Authentifizierung (falsches Passwort, gesperrter Account).
2. E1.2: Auth0 zeigt die entsprechende Fehlermeldung auf der Auth0-Login-Seite.
3. E1.3: Der Benutzer kann den Anmeldevorgang erneut starten.

---

## Postconditions

### Success

- Eine aktive Sitzung mit gültigem Access Token besteht.
- Alle API-Requests enthalten den `Authorization: Bearer`-Header.
- Der Benutzer befindet sich auf der für seine Rolle bestimmten Einstiegsseite.

### Failure / Abort

- Keine Sitzung wurde erstellt; der Benutzer verbleibt auf der Login-Seite.

---

## Acceptance Criteria

```gherkin
Scenario: Organisator meldet sich erfolgreich an
  Given ein Auth0-Account mit Rolle ORGANISATOR existiert
  When der Organisator die App öffnet und sich über Auth0 anmeldet
  Then ist er auf der Seite "/personen" und alle API-Requests enthalten einen Bearer-Token

Scenario: Partei meldet sich erfolgreich an
  Given ein Auth0-Account mit Rolle PARTEI existiert
  When die Partei die App öffnet und sich über Auth0 anmeldet
  Then ist sie auf der Seite "/meine-teilnahme" und alle API-Requests enthalten einen Bearer-Token

Scenario: Unauthentifizierter Zugriff auf geschützte Route wird umgeleitet
  Given der Benutzer hat keine aktive Sitzung
  When er direkt "/personen" aufruft
  Then leitet das System ihn auf "/login" weiter

Scenario: Anmeldung mit ungültigem Passwort schlägt fehl
  Given ein Auth0-Account existiert
  When der Benutzer ein falsches Passwort eingibt
  Then zeigt Auth0 eine Fehlermeldung und die Sitzung wird nicht erstellt

Scenario: Account ohne Rollenzuordnung wird abgewiesen
  Given ein Auth0-Account existiert ohne Rollenzuordnung
  When der Benutzer sich anmeldet
  Then zeigt das System die Meldung "Ihr Account hat keine gültige Rollenzuordnung"
```

---

## Open Items

- [ ] OPEN: Welcher Custom-Claim-Namespace wird für Rollen verwendet (z. B. `https://quartierfest.ch/roles`)? Nach Auth0-Tenant-Konfiguration festlegen.
- [ ] OPEN: Silent Authentication via Refresh-Token oder via Auth0-Session-Cookie (`prompt=none`)?

---

## Dependencies & References

- **Ermöglicht**: UC-015 (Parteibenutzer verwalten), UC-016 (Teilnahme bestätigen)
- **Backend vorausgesetzt**: AUTH-001 (Spring Security + JWT-Validierung bereits implementiert)
