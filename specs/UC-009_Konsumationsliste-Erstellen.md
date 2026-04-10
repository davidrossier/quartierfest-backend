---
id: UC-009
type: Use Case
name: "Konsumationsliste erstellen"
completeness: Minimum
traceability:
  impl_status: teilweise
  endpoints:
    - "GET /api/konsumationsangebote"
    - "GET /api/teilnahmen"
  test_ids:
    - TC-018
    - TC-019
  it_classes:
    - KonsumationslisteErstellenIT
  last_traced: "2026-04-10"
---

# UC-009 – Konsumationsliste erstellen

---

## Brief Description

> Der Organisator möchte eine druckfertige Konsumationsliste für den Event erstellen, damit die Getränkekonsumation der Parteien am Event handschriftlich festgehalten werden kann.

---

## Actors

| Actor | Type | Role |
|---|---|---|
| Organisator | `Human` | Erstellt und druckt die Konsumationsliste |

---

## Context & Background

> Die Konsumationsliste ist ein physisches Dokument, das am Event am Kühlschrank montiert wird. Sie hat eine Matrixstruktur: auf der X-Achse das Konsumationsangebot (Getränke mit Preisen), auf der Y-Achse die teilnehmenden Parteien. Parteien tragen ihre Konsumationen handschriftlich mit Strichen ein. Zusätzlich enthält die Liste je Partei eine Zeile zur Erfassung der definitiven Teilnehmerzahl. Nach dem Event wird diese Liste für die digitale Erfassung (UC-010) verwendet.

---

## Frontend-Kontext

> **Route:** `/durchfuehrung/konsumationsliste` — `KonsumationslisteComponent` (Angular 21, Standalone)
> Event-kontextabhängig; Matrix aus Teilnahmen × Konsumationsangebote des gewählten Events.

- **Drucken:** `drucken()` ruft `window.print()` auf — der Browser-Druckdialog öffnet sich. Es gibt keinen PDF-Export; Layout und Papierformat werden via CSS (@media print) gesteuert.
- Bei fehlendem Konsumationsangebot zeigt `keinAngebot`-Signal einen Warnhinweis. Bei fehlenden Teilnahmen zeigt `keineTeilnahmen`-Signal einen Hinweis.

---

## Preconditions

- Eine authentifizierte Sitzung des Organisators besteht.
- Das Konsumationsangebot für den Event ist vollständig erfasst (UC-008).
- Mindestens eine Teilnahme für den Event existiert (UC-005).

---

## Trigger

> Der Organisator bereitet den Event vor und möchte die Konsumationsliste ausdrucken.

---

## Description

1. Der Organisator öffnet die Konsumationslisten-Funktion für den ausgewählten Event.
2. Das System generiert eine Listenansicht mit:
   - Y-Achse: alle angemeldeten Parteien
   - X-Achse: alle Konsumationsangebote mit Bezeichnung und Preis
   - Je Partei: eine zusätzliche Zeile/Spalte für die definitive Teilnehmerzahl
3. Der Organisator prüft die Liste und druckt sie aus.
4. Die ausgedruckte Liste wird am Event am Kühlschrank montiert.

---

## Error Scenarios

### E1 – Kein Konsumationsangebot vorhanden

> Entry point: step 2 of the main flow

1. E1.1: Das System stellt fest, dass kein Konsumationsangebot für den Event existiert.
2. E1.2: Das System zeigt einen Hinweis und kann keine Liste generieren.
3. E1.3: Der Organisator erfasst zuerst das Konsumationsangebot (UC-008).

### E2 – Keine Teilnahmen vorhanden

> Entry point: step 2 of the main flow

1. E2.1: Das System stellt fest, dass keine Teilnahmen für den Event existieren.
2. E2.2: Das System zeigt einen Hinweis; die Liste hat keine Zeilen.

---

## Postconditions

### Success

- Eine druckfertige Konsumationsliste wurde generiert und steht zum Ausdrucken bereit.

### Failure / Abort

- Keine Liste wurde generiert; der Organisator behebt die fehlenden Voraussetzungen.

---

## Acceptance Criteria

```gherkin
Scenario: Konsumationsliste erfolgreich generieren
  Given Event 2025 hat 5 Teilnahmen und 4 Konsumationsangebote
  When der Organisator die Konsumationsliste für Event 2025 generiert
  Then enthält die Liste 5 Zeilen (Parteien) und 4 Spalten (Angebote) plus eine Spalte für effektive Personenzahl

Scenario: Konsumationsliste ohne Angebot zeigt Fehler
  Given Event 2025 hat kein Konsumationsangebot
  When der Organisator die Konsumationsliste generieren möchte
  Then zeigt das System einen Hinweis auf das fehlende Konsumationsangebot
```

---

## Open Items

- [x] ~~OPEN: Format A3/A4 Querformat?~~ → **Beantwortet:** Kein fixes Format vorgegeben. Das Layout wird via CSS (@media print) gesteuert; der Organisator wählt Papierformat im Browser-Druckdialog.
- [x] ~~OPEN: PDF-Export oder Bildschirmansicht?~~ → **Beantwortet:** Nur Bildschirmansicht mit "Drucken"-Button (`window.print()`). Kein PDF-Export.
- [ ] OPEN: Wird die Spalte "definitive Teilnehmerzahl" in der Druckansicht mit dem `anzahlPersonenEffektiv`-Wert vorausgefüllt (zur Kontrolle) oder leer gelassen (für handschriftliche Eintragung)? → Entscheid abhängig vom HTML-Template der Komponente.

---

## Dependencies & References

- **Depends on**: UC-005 (Teilnahmen verwalten), UC-008 (Konsumationsangebot verwalten)
