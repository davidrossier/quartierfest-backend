---
id: UC-014
type: Use Case
name: "Benutzer anmelden"
completeness: Minimum
traceability:
  impl_status: implementiert
  endpoints:
    - "POST /api/auth/login"
  test_ids: [TC-038, TC-040]
  it_classes: [BenutzerAnmeldenIT, SecurityMatrixIT]
  last_traced: "2026-06-12"
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

---

## Context & Background

> **Entscheid 2026-06-12: Eigenbau statt externer IdP** (ersetzt den Auth0-Entscheid vom 2026-05-15). Begründung: geschlossener Benutzerkreis, Accounts werden ausschliesslich durch den Organisator angelegt (UC-015), keine Self-Registration, kein E-Mail-basierter Passwort-Reset nötig — damit entfällt der Hauptnutzen eines externen IdP, und die externe Abhängigkeit sowie der Konfigurationsaufwand (Tenant, Custom Action, Claim-Namespace) fallen weg.
>
> Die Authentifizierung verwendet ausschliesslich etablierte Spring-Security-Bausteine: Passwörter werden mit BCrypt gehasht (`PasswordEncoder`), das Backend stellt nach erfolgreicher Prüfung ein selbst signiertes JWT aus (HS256, symmetrischer Schlüssel aus der Umgebungsvariable `AUTH_JWT_SECRET`, Gültigkeit 12 Stunden). Die bestehende Resource-Server-Validierung aus AUTH-001 bleibt erhalten; der `JwtDecoder` wird von JWKS/issuer-uri auf den symmetrischen Schlüssel umgestellt. Die Rolle steht als Claim `rolle` im Token (kein Namespace nötig) und wird im Backend via `JwtAuthenticationConverter` auf `ROLE_ORGANISATOR`/`ROLE_PARTEI` gemappt.
>
> Passwort-Reset ist kein Self-Service: Der Benutzer meldet sich beim Organisator, der über die Benutzerverwaltung (UC-015) ein neues Passwort setzt.

---

## Frontend-Kontext

> **Login-Seite:** `/login` — eigenes Formular (E-Mail + Passwort), `LoginComponent` (Angular 21, Standalone)
> Keine externe Library, kein Redirect-/Callback-Flow.

- Route Guard (`AuthGuard`) schützt alle App-Routen und leitet unauthentifizierte Benutzer zu `/login` weiter.
- HTTP-Interceptor hängt `Authorization: Bearer <token>` automatisch an alle Requests gegen `/api/**`; bei einer 401-Antwort (Token abgelaufen/ungültig) wird das Token verworfen und zu `/login` umgeleitet.
- Nach erfolgreichem Login wird rollenbasiert geroutet:
  - `ORGANISATOR` → `/personen` (bestehende App-Einstiegsseite)
  - `PARTEI` → `/meine-teilnahme` (UC-016)
- Token wird in `sessionStorage` gehalten: übersteht einen Seitenreload, wird beim Schliessen des Tabs gelöscht. Kein Refresh-Token — nach Ablauf der 12 Stunden ist eine erneute Anmeldung nötig.

---

## Preconditions

- Für den Benutzer existiert ein Account in der `Benutzer`-Tabelle (durch den Organisator angelegt, UC-015; der erste Organisator-Account wird beim Applikationsstart aus Umgebungsvariablen erzeugt).
- Der Account hat die Rolle `ORGANISATOR` oder `PARTEI`; bei `PARTEI` ist eine Partei zugeordnet.
- Dem Benutzer sind E-Mail-Adresse und (Initial-)Passwort bekannt.

---

## Trigger

> Der Benutzer öffnet die Applikation ohne aktive Sitzung, oder seine Sitzung ist abgelaufen.

---

## Description

1. Der Route Guard stellt fest, dass kein gültiges Token vorliegt.
2. Das System leitet den Benutzer auf die Login-Seite (`/login`) weiter.
3. Der Benutzer gibt E-Mail-Adresse und Passwort ein und klickt auf «Anmelden». Das Frontend sendet `POST /api/auth/login`.
4. Das Backend prüft die E-Mail-Adresse und vergleicht das Passwort gegen den BCrypt-Hash. *(→ E1 bei ungültigen Anmeldedaten)*
5. Das Backend stellt ein signiertes JWT aus (Claims: `sub` = Benutzer-ID, `email`, `rolle`; Gültigkeit 12 h) und liefert es zurück.
6. Das Frontend legt das Token in `sessionStorage` ab; der HTTP-Interceptor verwendet es für alle künftigen API-Requests.
7. Das System liest den Rollen-Claim aus dem Token und leitet den Benutzer zur rollenbasierten Einstiegsseite weiter.

---

## Alternative Flows

### A1 – Seitenreload mit gültigem Token

> Entry point: step 1 of the main flow

1. A1.1: Das System findet ein noch gültiges Token in `sessionStorage`.
2. A1.2: Die ursprünglich angeforderte Route wird direkt geladen, ohne Login-Umweg.

### A2 – Sitzung abgelaufen

> Entry point: beliebiger API-Request während der Nutzung

1. A2.1: Das Backend beantwortet einen Request mit 401 (Token abgelaufen oder ungültig).
2. A2.2: Der Interceptor verwirft das Token und leitet zu `/login` weiter; das System zeigt den Hinweis «Sitzung abgelaufen, bitte erneut anmelden.»

---

## Error Scenarios

### E1 – Ungültige Anmeldedaten

> Entry point: step 4 of the main flow

1. E1.1: Das Backend stellt fest, dass die E-Mail-Adresse unbekannt ist oder das Passwort nicht stimmt.
2. E1.2: Das Backend antwortet mit 401 — bewusst ohne Unterscheidung, welches der beiden Felder falsch war (verhindert das Ausspähen gültiger E-Mail-Adressen).
3. E1.3: Das Frontend zeigt: «E-Mail-Adresse oder Passwort falsch.» Der Benutzer kann es erneut versuchen.

### E2 – Passwort vergessen

> Entry point: step 3 of the main flow

1. E2.1: Der Benutzer kennt sein Passwort nicht mehr. Es gibt keinen Self-Service-Reset.
2. E2.2: Die Login-Seite zeigt den Hinweis: «Passwort vergessen? Bitte wenden Sie sich an den Organisator.»
3. E2.3: Der Organisator setzt über die Benutzerverwaltung (UC-015) ein neues Passwort und teilt es dem Benutzer mit.

---

## Postconditions

### Success

- Eine aktive Sitzung mit gültigem, selbst ausgestelltem JWT besteht.
- Alle API-Requests enthalten den `Authorization: Bearer`-Header.
- Der Benutzer befindet sich auf der für seine Rolle bestimmten Einstiegsseite.

### Failure / Abort

- Keine Sitzung wurde erstellt; der Benutzer verbleibt auf der Login-Seite.

---

## Acceptance Criteria

```gherkin
Scenario: Organisator meldet sich erfolgreich an
  Given ein Benutzer "orga@quartier.ch" mit Rolle ORGANISATOR existiert
  When er sich mit korrektem Passwort anmeldet
  Then ist er auf der Seite "/personen" und alle API-Requests enthalten einen Bearer-Token

Scenario: Partei meldet sich erfolgreich an
  Given ein Benutzer "mueller@quartier.ch" mit Rolle PARTEI und Partei-Zuordnung existiert
  When er sich mit korrektem Passwort anmeldet
  Then ist er auf der Seite "/meine-teilnahme" und alle API-Requests enthalten einen Bearer-Token

Scenario: Unauthentifizierter Zugriff auf geschützte Route wird umgeleitet
  Given der Benutzer hat keine aktive Sitzung
  When er direkt "/personen" aufruft
  Then leitet das System ihn auf "/login" weiter

Scenario: Anmeldung mit falschem Passwort schlägt fehl
  Given ein Benutzer "orga@quartier.ch" existiert
  When er ein falsches Passwort eingibt
  Then antwortet das Backend mit 401 und das Frontend zeigt "E-Mail-Adresse oder Passwort falsch"

Scenario: Seitenreload erhält die Sitzung
  Given ein angemeldeter Benutzer mit gültigem Token
  When er die Seite neu lädt
  Then bleibt er angemeldet und die angeforderte Route wird direkt geladen

Scenario: Abgelaufenes Token führt zurück zum Login
  Given ein angemeldeter Benutzer, dessen Token abgelaufen ist
  When der nächste API-Request mit 401 beantwortet wird
  Then leitet das System zu "/login" und zeigt "Sitzung abgelaufen"
```

---

## Open Items

- [ ] OPEN: Brute-Force-Schutz für `POST /api/auth/login` — Umfang festlegen (z. B. zunehmende Verzögerung oder temporäre Sperre nach n Fehlversuchen pro E-Mail/IP). Für den geschlossenen Benutzerkreis reicht vermutlich eine einfache Drosselung. (Entscheid 2026-06-12: nicht Teil der ersten Iteration.)
- [ ] OPEN: Soll der Benutzer sein eigenes Passwort ändern können (z. B. `POST /api/auth/passwort`)? Reduziert, dass der Organisator Passwörter dauerhaft kennt. (Entscheid 2026-06-12: nicht Teil der ersten Iteration.)
- [x] RESOLVED (2026-06-12): Token-Gültigkeit 12 h umgesetzt (`auth.jwt.ttl-stunden`, konfigurierbar) — deckt einen Festtag ab.

---

## Dependencies & References

- **Depends on**: UC-015 (Benutzer verwalten — `Benutzer`-Entity, Account-Anlage, Bootstrap des ersten Organisator-Accounts)
- **Ermöglicht**: UC-016 (Teilnahme bestätigen), Nutzung der Admin-UI aus UC-015
- **Backend vorausgesetzt**: AUTH-001 (Spring Security + JWT-Validierung; `JwtDecoder` wird auf symmetrischen Schlüssel umgestellt)
