---
id: UC-006
type: Use Case
name: "Bestätigung erstellen und versenden"
completeness: Minimum
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
| Partei | `Human` | Empfängt die Bestätigung |

---

## Context & Background

> Die Bestätigung wird einige Tage vor dem Event-Datum an alle Parteien mit Status ANGEMELDET verschickt. Sie enthält den definitiven Standort (ggf. den alternativen Standort), die relevanten Zeiten, eine Zusammenstellung der zugesagten Buffetbeiträge sowie das Konsumationsangebot. Der Organisator markiert in der Einladung, ob die Bestätigung versendet wurde (`bestaetigungVersendet`). Der eigentliche Versand (E-Mail, Post) findet ausserhalb des Systems statt.

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

- [ ] OPEN: Gibt es einen definierten Zeitpunkt (z.B. X Tage vor Event-Datum), ab dem die Bestätigung versendet werden soll, oder liegt der Zeitpunkt im freien Ermessen des Organisators?
- [ ] OPEN: Wie wird die Bestätigung technisch erzeugt und versendet — generiert das System ein Dokument (PDF, E-Mail-Text) oder ist der Versand vollständig manuell?
- [ ] OPEN: Soll die Bestätigung pro Partei individuell (mit deren spezifischem Buffetbeitrag) oder als einheitliches Rundschreiben verschickt werden?
- [ ] REVIEW: Die Partei ist in der Akteurstabelle als `Human`-Aktor geführt, interagiert aber nie direkt mit dem System (analog UC-004 Open Item). Klären ob Partei als sekundärer Aktor oder als Stakeholder ausserhalb des UC-Scopes zu führen ist.
- [ ] REVIEW: Schritt 4 verlangt das Setzen von `bestaetigungVersendet = true` per Einladung, aber das System besitzt keinen PATCH-Endpunkt. Klären ob ein PATCH-Endpunkt eingeführt oder ob das Markieren über einen alternativen Mechanismus (z.B. DELETE + neu anlegen) gelöst werden soll.

---

## Dependencies & References

- **Depends on**: UC-004 (Einladung erstellen und verwalten), UC-008 (Konsumationsangebot verwalten)
