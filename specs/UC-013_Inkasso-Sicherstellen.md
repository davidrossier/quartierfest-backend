---
id: UC-013
type: Use Case
name: "Inkasso sicherstellen"
completeness: Intermediate
traceability:
  impl_status: vollständig
  endpoints:
    - "GET /api/zahlungen"
    - "POST /api/zahlungen"
    - "DELETE /api/zahlungen/{id}"
    - "GET /api/mahnungen"
    - "POST /api/mahnungen"
    - "DELETE /api/mahnungen/{id}"
  test_ids:
    - TC-026
    - TC-027
    - TC-028
  it_classes:
    - InkassoSicherstellenIT
  last_traced: "2026-04-10"
---

# UC-013 – Inkasso sicherstellen

---

## Brief Description

> Der Organisator möchte eingegangene Zahlungen kontinuierlich erfassen und ausstehende Beträge durch Mahnungen nachverfolgen, um sicherzustellen, dass alle Parteien ihre Eventkosten begleichen.

---

## Actors

| Actor | Type | Role |
|---|---|---|
| Organisator | `Human` | Erfasst Zahlungen und versendet Mahnungen |
| Partei | `External` | Leistet die Zahlung ausserhalb des Systems |

---

## Context & Background

> Nach der Zustellung der Abrechnungen (UC-012) erwartet der Organisator Zahlungseingänge. Zahlungen können via Twint, Banküberweisung oder bar erfolgen. Der Organisator kontrolliert regelmässig, ob Zahlungen eingegangen sind, und erfasst diese im System. Parteien, die trotz Zustellung nicht bezahlt haben, erhalten eine Mahnung, die ebenfalls im System festgehalten wird. Eine Abrechnung kann mehrere Teilzahlungen haben.

---

## Frontend-Kontext

> **Route:** `/nachbearbeitung/inkasso` — `InkassoVerwaltungComponent` (Angular 21, Standalone)
> Event-kontextabhängig; zeigt alle Abrechnungen des gewählten Events.

- **Expand-Row-Pattern:** Jede Zeile kann per Klick expandiert werden (`expandierteId`). In der Detailansicht werden Zahlungen, Mahnungen und die dazugehörigen Erfassungsformulare angezeigt.
- **Zahlungsformular:** zahlungskanal (TWINT default), datum (heute), betrag (required, `min: 0.01`). Formular öffnet sich via `zahlungFormOeffnen()`.
- **Mahnungsformular:** datum (heute), bemerkung (optional). Formular öffnet sich via `mahnungFormOeffnen()`.
- **Offener Betrag:** wird clientseitig berechnet: `abrechnung.totalBetrag − Σ(zahlung.betrag)`.
- Zahlungen und Mahnungen können einzeln gelöscht werden (`confirm()`-Dialog).
- Liste sortierbar nach partei, totalBetrag.

---

## Preconditions

- Eine authentifizierte Sitzung des Organisators besteht.
- Mindestens eine Abrechnung wurde zugestellt (UC-012).

---

## Trigger

> Der Organisator kontrolliert eingegangene Zahlungen (z.B. Kontoauszug prüfen, Twint-Benachrichtigung erhalten) oder stellt fest, dass eine Zahlung aussteht.

---

## Description

1. Der Organisator öffnet die Zahlungsübersicht für den ausgewählten Event.
2. Das System zeigt alle Abrechnungen mit Totalbetrag, bereits bezahltem Betrag und offenen Beträgen.
3. **Zahlung erfassen:** Der Organisator wählt eine Abrechnung und erfasst eine Zahlung mit Zahlungskanal (TWINT, UEBERWEISUNG, BAR), Datum und Betrag. Das System speichert die Zahlung. *(→ E1 bei fehlendem Pflichtfeld)*
4. Das System aktualisiert den offenen Betrag der Abrechnung.
5. **Mahnung erfassen:** Ist nach einer angemessenen Frist keine Zahlung eingegangen, wählt der Organisator "Mahnung erfassen" für die betroffene Abrechnung und gibt das Datum sowie eine optionale Bemerkung ein. Das System speichert die Mahnung. *(→ A1)*
6. Der Organisator wiederholt Schritt 3 oder 5 bis alle Abrechnungen beglichen oder abgeschrieben sind.

---

## Alternative Flows

### A1 – Mahnung versenden

> Entry point: step 5 of the main flow

1. A1.1: Der Organisator informiert die Partei über den ausstehenden Betrag (ausserhalb des Systems).
2. A1.2: Der Organisator erfasst die Mahnung im System mit aktuellem Datum.
3. A1.3: Das System speichert die Mahnung und zeigt die Mahnungshistorie der Abrechnung.

---

## Error Scenarios

### E1 – Pflichtfeld Zahlung fehlt

> Entry point: step 3 of the main flow

1. E1.1: Das System stellt fest, dass Zahlungskanal, Datum oder Betrag fehlt.
2. E1.2: Das System zeigt eine Fehlermeldung und verhindert das Speichern.

### E2 – Bezahlter Betrag übersteigt Abrechnungsbetrag

> Entry point: step 3 of the main flow

1. E2.1: Die Summe aller Zahlungen übersteigt den Totalbetrag der Abrechnung.
2. E2.2: Das System zeigt einen Warnhinweis (Überzahlung möglich, z.B. bei Rundungsfehlern).
3. E2.3: Der Organisator kann trotzdem speichern oder den Betrag korrigieren.

---

## Postconditions

### Success

- Alle Zahlungseingänge sind mit Kanal, Datum und Betrag gespeichert.
- Mahnungen sind mit Datum und Bemerkung gespeichert.
- Die offenen Beträge pro Abrechnung sind aktuell und korrekt.

### Failure / Abort

- Keine Änderungen wurden persistiert; der vorherige Zustand bleibt erhalten.

---

## Acceptance Criteria

```gherkin
Scenario: Twint-Zahlung erfolgreich erfassen
  Given die Abrechnung der Partei "Müller" hat einen Totalbetrag von CHF 57.00 und keine Zahlung
  When der Organisator eine Zahlung via TWINT, Datum heute, Betrag CHF 57.00 erfasst
  Then hat die Abrechnung eine Zahlung von CHF 57.00 und der offene Betrag beträgt CHF 0.00

Scenario: Mahnung erfassen
  Given die Abrechnung der Partei "Weber" ist seit 14 Tagen zugestellt und unbezahlt
  When der Organisator eine Mahnung mit aktuellem Datum erfasst
  Then ist die Mahnung bei der Abrechnung "Weber" gespeichert

Scenario: Zahlung mit fehlendem Pflichtfeld wird abgelehnt
  Given der Organisator ist in der Zahlungserfassung
  When er eine Zahlung ohne Datum speichern möchte
  Then zeigt das System eine Fehlermeldung und speichert nicht

Scenario: Teilzahlung reduziert offenen Betrag korrekt
  Given die Abrechnung der Partei "Fischer" hat Totalbetrag CHF 80.00 und keine Zahlung
  When der Organisator eine Zahlung von CHF 50.00 erfasst
  Then beträgt der offene Betrag der Abrechnung CHF 30.00
```

---

## Open Items

- [x] ~~REVIEW: "Abgeschrieben" als Abschlusszustand?~~ → **Beantwortet:** Das Frontend und Datenmodell kennen keinen Abschreibungs-Status. Der Abschluss einer Abrechnung erfolgt implizit, wenn der offene Betrag CHF 0.00 erreicht. Forderungsabschreibungen sind nicht implementiert.
- [x] ~~REVIEW: Partei als `Human`-Aktor?~~ → **Beantwortet (analog UC-004):** Partei ist externer Stakeholder. Typ auf `External` korrigiert.

---

## Dependencies & References

- **Depends on**: UC-012 (Abrechnung zustellen)
