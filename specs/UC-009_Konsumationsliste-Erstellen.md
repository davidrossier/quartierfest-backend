---
id: UC-009
type: Use Case
name: "Konsumationsliste erstellen"
completeness: Minimum
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

- [ ] OPEN: Welches Format soll die ausgedruckte Liste haben (A3, A4 Querformat)? Die Anforderung "gross ausgedruckt" deutet auf A3 oder A4 Querformat hin.
- [ ] OPEN: Soll das System die Druckfunktion direkt anbieten (z.B. PDF-Export) oder nur eine Bildschirmansicht zur Verfügung stellen?
- [ ] REVIEW: Soll die Spalte "definitive Teilnehmerzahl" mit dem aktuellen `anzahlPersonenEffektiv`-Wert aus der Teilnahme vorausgefüllt werden, oder soll sie für handschriftliche Eintragung am Event leer bleiben?

---

## Dependencies & References

- **Depends on**: UC-005 (Teilnahmen verwalten), UC-008 (Konsumationsangebot verwalten)
