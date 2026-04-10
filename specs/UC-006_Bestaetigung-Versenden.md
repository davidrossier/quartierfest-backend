---
id: UC-006
type: Use Case
name: "Bestätigung erstellen und versenden"
completeness: Intermediate
traceability:
  impl_status: vollständig
  endpoints:
    - "POST /api/einladungen"
  test_ids:
    - TC-013
  it_classes:
    - BestaetigungVerwaltenIT
  last_traced: "2026-04-10"
---

# UC-006 – Bestätigung erstellen und versenden

---

## Brief Description

> Der Organisator möchte einige Tage vor dem Event eine Bestätigung an alle angemeldeten Parteien versenden, um die definitiven Event-Details und das Konsumationsangebot zu kommunizieren.

---

## Actors

| Actor | Type | Role |
|---|---|---|
| Organisator | `Human` | Erstellt und versendet die Bestätigung |
| Partei | `External` | Empfängt die Bestätigung ausserhalb des Systems |

---

## Context & Background

> Die Bestätigung wird einige Tage vor dem Event-Datum an alle Parteien mit Status ANGEMELDET verschickt. Sie enthält den definitiven Standort (ggf. den alternativen Standort), die relevanten Zeiten, eine Zusammenstellung der Buffetbeiträge sowie das Konsumationsangebot. Die Buffet-Zusammenstellung stammt aus den **Teilnahmen** (UC-005) — nicht aus den Einladungen — da nur dort mehrere Beiträge je Partei erfasst werden können. Der Organisator markiert in der Einladung, ob die Bestätigung versendet wurde (`bestaetigungVersendet`). Der eigentliche Versand (E-Mail, Post) findet ausserhalb des Systems statt.

---

## Frontend-Kontext

> **Route:** `/planung/bestaetigung` — `BestaetigungUebersichtComponent` (Angular 21, Standalone)
> Event-kontextabhängig; zeigt nur Einladungen mit Status ANGEMELDET für den gewählten Event.

- **Ansichtsinhalt:** Buffet-Zusammenstellung (gruppiert nach Beitragstyp, mit Partei-Namen und Beschreibung), Konsumationsangebot mit Preisen, Anzahl noch nicht versendeter Bestätigungen (`unversendeteAnzahl`). Die Buffet-Daten werden aus `GET /api/teilnahmen` gelesen (`buffetBeitraege`-Liste je Teilnahme), **nicht** aus den Einladungen.
- **Warnung:** Wenn kein Konsumationsangebot für den Event erfasst ist, zeigt `keinAngebot`-Signal einen Warnhinweis.
- **`bestaetigungVersendet` setzen:** Das Frontend setzt das Flag via erneuten `POST`-Aufruf (Upsert-Mechanismus) auf `true` — es wird kein PATCH-Endpunkt benötigt.
- **Einzeln markieren:** `markiereVersendet(einladung)` — setzt `bestaetigungVersendet = true` für eine Einladung.
- **Alle markieren:** `alleMarkieren()` — setzt via `forkJoin` alle noch nicht markierten Einladungen auf `bestaetigungVersendet = true`.
- Es gibt keine PDF-Generierung; der Versand ist vollständig manuell ausserhalb des Systems.

---

## Preconditions

- Eine authentifizierte Sitzung des Organisators besteht.
- Mindestens eine Einladung mit Status ANGEMELDET existiert (UC-004).
- Das Konsumationsangebot für den Event ist erfasst (UC-008).

---

## Trigger

> Der Organisator entscheidet, die Bestätigungen für einen Event zu versenden (typischerweise einige Tage vor dem Event).

---

## Description

1. Der Organisator öffnet die Einladungsübersicht für den Event und wählt "Bestätigungen erstellen".
2. Das System zeigt eine Zusammenfassung der Bestätigungsinhalte:
   - Definitiver Standort (oder alternativer Standort)
   - Startzeit, Aufstellzeit, Aufräumzeit
   - Zusammenstellung der Buffetbeiträge aller angemeldeten Parteien
   - Konsumationsangebot mit Preisen
3. Der Organisator prüft die Zusammenfassung und versendet die Bestätigung ausserhalb des Systems (E-Mail, Post).
4. Der Organisator markiert die betreffenden Einladungen als `bestaetigungVersendet = true`.
5. Das System speichert den Status und zeigt die aktualisierte Einladungsübersicht.

---

## Error Scenarios

### E1 – Konsumationsangebot fehlt

> Entry point: step 2 of the main flow

1. E1.1: Das System stellt fest, dass für den Event kein Konsumationsangebot erfasst wurde.
2. E1.2: Das System zeigt einen Warnhinweis, dass die Bestätigung kein Konsumationsangebot enthält.
3. E1.3: Der Organisator kann fortfahren oder zuerst das Konsumationsangebot erfassen (UC-008).

---

## Postconditions

### Success

- Die Einladungen der bestätigten Parteien haben `bestaetigungVersendet = true`.
- Die Parteien wurden ausserhalb des Systems informiert.

### Failure / Abort

- Der `bestaetigungVersendet`-Status bleibt unverändert.

---

## Acceptance Criteria

```gherkin
Scenario: Bestätigung erfolgreich als versendet markieren
  Given die Einladung der Partei "Müller" für Event 2025 hat Status ANGEMELDET und bestaetigungVersendet=false
  When der Organisator die Bestätigung versendet und als versendet markiert
  Then hat die Einladung der Partei "Müller" bestaetigungVersendet=true

Scenario: Bestätigung ohne Konsumationsangebot zeigt Warnung
  Given der Event 2025 hat kein Konsumationsangebot
  When der Organisator die Bestätigungsübersicht öffnet
  Then zeigt das System einen Warnhinweis zum fehlenden Konsumationsangebot
```

---

## Open Items

- [x] ~~OPEN: Gibt es einen definierten Zeitpunkt?~~ → **Beantwortet:** Kein fixer Zeitpunkt — liegt im Ermessen des Organisators.
- [x] ~~OPEN: Wie wird die Bestätigung technisch erzeugt und versendet?~~ → **Beantwortet:** Das System generiert kein Dokument. Der Versand (E-Mail, Post) ist vollständig manuell; das System dient nur zur Ansicht der Bestätigungsinhalte und zum Setzen des `bestaetigungVersendet`-Flags.
- [x] ~~OPEN: Individuell oder Rundschreiben?~~ → **Beantwortet:** Das Frontend markiert jede Einladung **individuell** (`markiereVersendet()`), bietet aber auch eine Bulk-Markierung aller unversendeten Bestätigungen (`alleMarkieren()`).
- [x] ~~REVIEW: Partei als `Human`-Aktor?~~ → **Beantwortet (analog UC-004):** Partei ist externer Stakeholder, kein primärer Systemakteur. Typ auf `External` korrigiert.
- [x] ~~REVIEW: `bestaetigungVersendet` ohne PATCH-Endpunkt?~~ → **Beantwortet:** Das Frontend nutzt den bestehenden `POST`-Endpunkt als Upsert (`einladungService.save({id: ..., bestaetigungVersendet: true, ...})`). Kein PATCH-Endpunkt benötigt.

---

## Dependencies & References

- **Depends on**: UC-004 (Einladung erstellen und verwalten), UC-005 (Teilnahmen verwalten), UC-008 (Konsumationsangebot verwalten)
