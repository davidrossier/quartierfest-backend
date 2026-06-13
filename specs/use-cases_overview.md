# Quartierfest – Use-Case-Übersicht

Software zur Unterstützung bei Planung, Durchführung und Nachbearbeitung von Quartierfesten (Buchlenfest).

---

## Akteure

| Actor | Typ | Beschreibung |
|---|---|---|
| Organisator | `Human` | Primärer Systemakteur; plant, führt durch und rechnet ab |
| Partei | `Human` / `External` | Haushalt als Einladungseinheit; in UC-001–013 externer Stakeholder; ab UC-014 direkter Systemakteur |

---

## Übersicht aller Use Cases

| UC-ID | Datei | Name | Phase | Actors | Completeness | Status |
|---|---|---|---|---|---|---|
| [UC-001](UC-001_Person-Verwalten.md) | `UC-001_Person-Verwalten.md` | Personendaten verwalten | Stammdaten | Organisator | Intermediate | ✅ Impl. vollständig |
| [UC-002](UC-002_Partei-Verwalten.md) | `UC-002_Partei-Verwalten.md` | Parteien verwalten | Stammdaten | Organisator | Intermediate | ✅ Impl. vollständig |
| [UC-003](UC-003_Event-Anlegen.md) | `UC-003_Event-Anlegen.md` | Event anlegen | Stammdaten | Organisator | Intermediate | ✅ Impl. vollständig |
| [UC-004](UC-004_Einladung-Verwalten.md) | `UC-004_Einladung-Verwalten.md` | Einladung erstellen und verwalten | Planung | Organisator, Partei (External) | Minimum | ✅ Impl. vollständig |
| [UC-005](UC-005_Teilnahme-Verwalten.md) | `UC-005_Teilnahme-Verwalten.md` | Teilnahmen verwalten | Planung | Organisator | Intermediate | ✅ Impl. vollständig |
| [UC-006](UC-006_Bestaetigung-Versenden.md) | `UC-006_Bestaetigung-Versenden.md` | Bestätigung erstellen und versenden | Planung | Organisator, Partei (External) | Intermediate | ✅ Impl. vollständig |
| [UC-007](UC-007_Allgemeinausgabe-Verwalten.md) | `UC-007_Allgemeinausgabe-Verwalten.md` | Allgemeinausgaben verwalten | Planung | Organisator | Intermediate | ✅ Impl. vollständig |
| [UC-008](UC-008_Konsumationsangebot-Verwalten.md) | `UC-008_Konsumationsangebot-Verwalten.md` | Konsumationsangebot verwalten | Planung | Organisator | Intermediate | ✅ Impl. vollständig |
| [UC-009](UC-009_Konsumationsliste-Erstellen.md) | `UC-009_Konsumationsliste-Erstellen.md` | Konsumationsliste erstellen | Durchführung | Organisator | Minimum | ⚠ Impl. teilweise |
| [UC-010](UC-010_Konsumation-Uebernehmen.md) | `UC-010_Konsumation-Uebernehmen.md` | Konsumation übernehmen | Durchführung | Organisator | Intermediate | ✅ Impl. vollständig |
| [UC-011](UC-011_Abrechnung-Erstellen.md) | `UC-011_Abrechnung-Erstellen.md` | Abrechnung erstellen | Nachbearbeitung | Organisator | Minimum | ⚠ Impl. teilweise |
| [UC-012](UC-012_Abrechnung-Zustellen.md) | `UC-012_Abrechnung-Zustellen.md` | Abrechnung zustellen | Nachbearbeitung | Organisator, Partei (External) | Intermediate | ✅ Impl. vollständig |
| [UC-013](UC-013_Inkasso-Sicherstellen.md) | `UC-013_Inkasso-Sicherstellen.md` | Inkasso sicherstellen | Nachbearbeitung | Organisator, Partei (External) | Intermediate | ✅ Impl. vollständig |
| [UC-014](UC-014_Benutzer-Anmelden.md) | `UC-014_Benutzer-Anmelden.md` | Benutzer anmelden | Auth / Querschnitt | Organisator, Partei | Minimum | ✅ Impl. vollständig |
| [UC-015](UC-015_Benutzer-Verwalten.md) | `UC-015_Benutzer-Verwalten.md` | Benutzer verwalten | Auth / Querschnitt | Organisator | Minimum | ✅ Impl. vollständig |
| [UC-016](UC-016_Teilnahme-Bestaetigen.md) | `UC-016_Teilnahme-Bestaetigen.md` | Teilnahme bestätigen | Planung (Partei-Sicht) | Partei, Organisator | Minimum | ✅ Impl. vollständig |

---

## Abhängigkeitskette

```
UC-001 ──┐
UC-002 ──┼──→ UC-004 ──→ UC-005 ──→ UC-006
UC-003 ──┘              │           │
                        │           └──→ UC-008 ──→ UC-009 ──→ UC-010
                        │                                      │
                        └──→ UC-007 ──────────────────────────┤
                                                               ↓
                                                           UC-011 ──→ UC-012 ──→ UC-013

UC-015 ──→ UC-014 ──→ UC-016 (erweitert UC-005; UC-015-Admin-UI setzt Anmeldung via UC-014 voraus, Bootstrap-Account löst das Henne-Ei-Problem)
```

---

## Phasen-Zuordnung (Frontend-Routen)

| Phase | Route-Präfix | Use Cases |
|---|---|---|
| Stammdaten | `/personen`, `/parteien`, `/events` | UC-001, UC-002, UC-003 |
| Planung | `/planung/...` | UC-004, UC-005, UC-006, UC-007, UC-008 |
| Durchführung | `/durchfuehrung/...` | UC-009, UC-010 |
| Nachbearbeitung | `/nachbearbeitung/...` | UC-011, UC-012, UC-013 |
| Auth / Admin | `/login`, `/admin/benutzer` | UC-014, UC-015 |
| Partei-Ansicht | `/meine-teilnahme` | UC-016 |
