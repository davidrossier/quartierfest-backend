---
id: UC-012
type: Use Case
name: "Abrechnung zustellen"
completeness: Intermediate
traceability:
  impl_status: vollständig
  endpoints:
    - "POST /api/abrechnungen"
  test_ids:
    - TC-024
    - TC-025
    - TC-032
  it_classes:
    - AbrechnungZustellenIT
  last_traced: "2026-05-01"
---

# UC-012 – Abrechnung zustellen

---

## Brief Description

> Der Organisator möchte die erstellten Abrechnungen an die Parteien zustellen, um die Bezahlung der Eventkosten einzuleiten.

---

## Actors

| Actor | Type | Role |
|---|---|---|
| Organisator | `Human` | Stellt die Abrechnungen zu und markiert die Zustellung im System |
| Partei | `External` | Empfängt die Abrechnung ausserhalb des Systems und bezahlt den ausstehenden Betrag |

---

## Context & Background

> Nach der Abrechnungserstellung (UC-011) werden die Abrechnungen den Parteien über den festgelegten Kanal zugestellt. Der eigentliche Versand findet je nach Kanal ausserhalb des Systems statt: Twint-Zahlungsanforderungen werden direkt via Twint-App gesendet, E-Mail und Papier werden manuell versendet. Im System wird lediglich das Zustellungsdatum festgehalten. Zahlungseingänge werden in UC-013 erfasst.

---

## Frontend-Kontext

> **Route:** `/nachbearbeitung/abrechnungen` — `AbrechnungenVerwaltungComponent` (Angular 21, Standalone)
> UC-012 ist in derselben Komponente wie UC-011 implementiert.

- **Zustellungskanal ändern:** Dropdown-Selektion pro Zeile (`kanalAendern()`), wird mit "Speichern"-Button via erneuten POST-Upsert persistiert (`kanalSpeichern()`).
- **Als zugestellt markieren:** `alsZugestelltMarkieren()` setzt `zustellungsDatum` auf `new Date().toISOString().substring(0, 10)` (heutiges Datum) via POST-Upsert.
- Kein PATCH-Endpunkt benötigt; POST agiert als Upsert.
- Es gibt keine automatische Vorlage oder Dokumentgenerierung; der Versand erfolgt manuell ausserhalb des Systems.

---

## Preconditions

- Eine authentifizierte Sitzung des Organisators besteht.
- Abrechnungen für den Event sind erstellt (UC-011).

---

## Trigger

> Der Organisator ist bereit, die Abrechnungen an die Parteien zu versenden.

---

## Description

1. Der Organisator öffnet die Abrechnungsübersicht für den ausgewählten Event.
2. Das System zeigt alle Abrechnungen mit Betrag, Partei, Zustellungskanal und Zustellungsdatum.
3. Der Organisator wählt eine oder mehrere Abrechnungen und stellt sie zu:
   - **TWINT:** Organisator sendet Zahlungsanforderung via Twint-App an die hinterlegte Mobilenummer. *(→ A1)*
   - **EMAIL:** Organisator versendet ein Abrechnungsschreiben per E-Mail. *(→ A2)*
   - **PAPIER:** Organisator übergibt oder versendet ein gedrucktes Abrechnungsschreiben. *(→ A2)*
4. Der Organisator markiert die zugestellten Abrechnungen im System und setzt das Zustellungsdatum.
5. Das System speichert das Zustellungsdatum.

---

## Alternative Flows

### A1 – Zustellung via Twint

> Entry point: step 3 of the main flow

1. A1.1: Der Organisator öffnet die Twint-App und sendet eine Zahlungsanforderung an die Mobilenummer der Partei.
2. A1.2: Der Organisator markiert die Abrechnung im System als zugestellt.

### A2 – Zustellung via E-Mail oder Papier

> Entry point: step 3 of the main flow

1. A2.1: Das System zeigt die Abrechnungsdetails (Betrag, Partei, Adresse) an.
2. A2.2: Der Organisator versendet das Schreiben ausserhalb des Systems.
3. A2.3: Der Organisator markiert die Abrechnung im System als zugestellt.

### A3 – Zustellungskanal ändern

> Entry point: step 3 of the main flow

1. A3.1: Der Organisator stellt fest, dass der vorgeschlagene Kanal nicht passt.
2. A3.2: Der Organisator ändert den Zustellungskanal der Abrechnung und wiederholt die Zustellung.

---

## Postconditions

### Success

- Das Zustellungsdatum ist bei den zugestellten Abrechnungen gespeichert.
- Die Parteien haben ihre Abrechnungen erhalten und können bezahlen.

### Failure / Abort

- Das Zustellungsdatum bleibt leer; die Abrechnung gilt als nicht zugestellt.

---

## Acceptance Criteria

```gherkin
Scenario: Abrechnung als zugestellt markieren
  Given die Abrechnung der Partei "Müller" hat kein Zustellungsdatum
  When der Organisator die Abrechnung als zugestellt markiert
  Then hat die Abrechnung das heutige Datum als Zustellungsdatum

Scenario: Zustellungskanal vor Versand ändern
  Given die Abrechnung der Partei "Weber" hat Zustellungskanal TWINT
  When der Organisator den Kanal auf EMAIL ändert und speichert
  Then hat die Abrechnung Zustellungskanal EMAIL
```

---

## Open Items

- [x] ~~OPEN: PDF-Vorlage oder manuell?~~ → **Beantwortet:** Kein Dokument generiert; der Organisator erstellt das Schreiben manuell ausserhalb des Systems.
- [x] ~~OPEN: Bulk-Markierung?~~ → **Beantwortet:** Keine Bulk-Markierung für Zustellung implementiert (Einzelaktion via `alsZugestelltMarkieren()`). Bulk existiert nur für `bestaetigungVersendet` in UC-006.
- [x] ~~REVIEW: Partei als `Human`-Aktor?~~ → **Beantwortet (analog UC-004):** Partei ist externer Stakeholder. Typ auf `External` korrigiert.
- [x] ~~REVIEW: `zustellungsDatum` und Kanalanpassung ohne PATCH-Endpunkt?~~ → **Beantwortet:** Frontend nutzt POST als Upsert für beide Felder. Kein PATCH-Endpunkt benötigt.

---

## Dependencies & References

- **Depends on**: UC-011 (Abrechnung erstellen)
- **Included Use Cases**: UC-013 (Inkasso sicherstellen)
