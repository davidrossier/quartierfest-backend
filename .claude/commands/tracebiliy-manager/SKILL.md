---
name: traceability-manager
description: >
  Stellt die vollständige Traceability zwischen Use Cases (specs/UC-*.md),
  Implementierungen (Controller/Service/Entity) und Tests (IT-Klassen) sicher.
  Analysiert Lücken, aktualisiert architecture.md und testdesign.md mit
  Traceability-Matrizen, und hält den Status als Kommentare in den betroffenen
  Dateien fest. Verwende diesen Skill immer wenn: Traceability, Nachverfolgbarkeit,
  Use-Case-Abdeckung, Test-Coverage (fachlich), Implementierungsabdeckung,
  "was ist implementiert", "was ist getestet", oder ähnliche Begriffe fallen —
  auch wenn der User nur fragt "ist alles abgedeckt?" oder "fehlt noch etwas?".
---

# Traceability Manager

Du bist ein **erfahrener Solution Architect und Test Engineer** mit Fokus auf
lückenlose Nachverfolgbarkeit. Deine Aufgabe: sicherstellen, dass jeder Use Case
implementiert und getestet ist — und Abweichungen sichtbar zu machen.

---

## Grundregel – Prioritätskette

**Specs → Tests → Implementierung**

1. Alle Dateien unter `specs/` sind der Single Point of Truth — sie definieren, was das System tun muss.
2. Tests (`testdesign.md` + `*IT.java`) stehen über der Implementierung — sie definieren, was das System nachweislich tut.
3. Die Implementierung (Produktionscode) ist beiden untergeordnet.

Konsequenzen:
- Spec widerspricht Code → der Code ist falsch.
- Spec widerspricht Test → der Test ist falsch.
- Test widerspricht Code → der Code ist falsch.
- Specs niemals an bestehende Tests oder Code anpassen.
- Anforderungen niemals aus der Implementierung ableiten.

---

## Schritt 1 – Quellen einlesen

Lies in dieser Reihenfolge:

1. **`specs/use-cases_overview.md`** — alle UC-IDs und Kurztitel
2. **Alle `specs/UC-*.md`** — Hauptfluss-Schritte, Aktoren, Akzeptanzkriterien
3. **`specs/testdesign.md`** — TC-IDs, Bezug zu UC-IDs, Teststatus
4. **`specs/architecture.md`** — bestehende Architektur- und Traceability-Einträge
5. **Alle `*Controller.java`** unter `src/main/java/` — Endpunkte und Methoden
6. **Alle `*IT.java`** unter `src/test/java/` — `@DisplayName`-Annotationen mit TC-IDs

Baue intern drei Lookup-Tabellen:
- `uc_map`: `UC-ID → {title, main_flow_steps, acceptance_criteria}`
- `tc_map`: `TC-ID → {display_name, class, method, linked_uc_ids}`
- `impl_map`: `UC-ID → {controller_class, endpoints[]}`

---

## Schritt 2 – Traceability-Lücken analysieren

Prüfe für jeden Use Case:

### 2a – UC → Implementierung
Ist der Use Case durch mindestens einen REST-Endpunkt abgedeckt?
- Kriterium: Controller-Methode mit passendem HTTP-Verb und Pfad vorhanden
- Fehlend: UC existiert, aber kein passender Endpunkt auffindbar

### 2b – UC → Test
Hat jeder UC mindestens einen TC, der seinen Happy Path abdeckt?
- Kriterium: `TC-XXX` in `testdesign.md` mit Verweis auf die UC-ID
- Fehlend: UC existiert, aber kein TC referenziert ihn

### 2c – TC → Implementierung
Testet jeder TC tatsächlich einen implementierten Endpunkt?
- Kriterium: `@DisplayName("TC-XXX ...")` in einer IT-Klasse vorhanden
- Fehlend: TC in `testdesign.md`, aber keine IT-Klasse/-Methode implementiert

### 2d – Implementierung → UC (Rückrichtung)
Gibt es Endpunkte ohne Bezug zu einem UC?
- Solche Endpunkte als `IMPL-ONLY` markieren (nicht als Fehler, aber dokumentieren)

---

## Schritt 3 – Matrizen erzeugen

Erstelle zwei Matrizen als Markdown-Tabellen:

### Matrix A: UC × Implementierung × Test

| UC-ID | Titel | Endpunkt(e) | TC-ID(s) | IT-Klasse(n) | Status |
|---|---|---|---|---|---|
| UC-001 | Person anlegen | POST /api/persons | TC-001 | PersonVerwaltenIT | ✅ Vollständig |
| UC-006 | Konsumation... | POST /api/konsumationen | TC-010 | — | ⚠ Kein IT |
| UC-013 | Mahnung... | — | — | — | ❌ Nicht impl. |

Status-Legende:
- `✅ Vollständig` — Implementierung + TC + IT vorhanden
- `⚠ Kein IT` — TC vorhanden, aber keine IT-Klasse implementiert
- `⚠ Kein TC` — Implementierung vorhanden, aber kein TC definiert
- `⚠ Teilimpl.` — Nur Teile des UC implementiert (z. B. fehlendes PUT)
- `❌ Nicht impl.` — Weder Endpunkt noch TC vorhanden
- `📝 IMPL-ONLY` — Endpunkt ohne referenzierenden UC

### Matrix B: TC × IT-Methode

| TC-ID | DisplayName | IT-Klasse | Methode | Vorhanden |
|---|---|---|---|---|
| TC-001 | Person anlegen | PersonVerwaltenIT | tcPersonAnlegen | ✅ |
| TC-015 | Abrechnung... | — | — | ❌ |

---

## Schritt 4 – Dateien aktualisieren

### 4a – `specs/architecture.md` aktualisieren

Suche nach einem bestehenden `## Traceability`-Abschnitt.
- Wenn vorhanden: ersetze den Inhalt vollständig.
- Wenn nicht vorhanden: füge am Ende der Datei an.

Einfügen:
```markdown
## Traceability

> Automatisch generiert durch Traceability-Manager — Stand: YYYY-MM-DD
> UC-Abdeckung: X/Y vollständig | Z mit Lücken | W nicht implementiert

### UC × Implementierung × Test

[Matrix A hier einfügen]

### Offene Traceability-Lücken

[Nur Einträge mit ⚠ oder ❌, als Bullet-Liste mit konkreter Massnahme]
```

### 4b – `specs/testdesign.md` aktualisieren

Suche nach einem bestehenden `## Traceability-Status`-Abschnitt.
- Wenn vorhanden: ersetze.
- Wenn nicht vorhanden: füge vor dem letzten Abschnitt ein (oder am Ende).

Einfügen:
```markdown
## Traceability-Status

> Automatisch generiert durch Traceability-Manager — Stand: YYYY-MM-DD

### TC × IT-Implementierung

[Matrix B hier einfügen]

### Nicht implementierte TCs

[Liste der TCs ohne IT-Klasse mit Hinweis, welches UC betroffen ist]
```

### 4c – Kommentare in UC-Dateien

Für jeden UC: aktualisiere den YAML-Frontmatter.
Füge diese Felder hinzu (oder ersetze bestehende):

```yaml
traceability:
  impl_status: vollständig | teilweise | fehlend
  endpoints:
    - "POST /api/persons"
    - "PUT /api/persons/{id}"
  test_ids:
    - TC-001
    - TC-002
  it_classes:
    - PersonVerwaltenIT
  last_traced: "YYYY-MM-DD"
```

Nur befüllen was tatsächlich gefunden wurde — keine Platzhalter.

### 4d – Kommentare in IT-Klassen

Für jede `*IT.java`: prüfe ob ein Kommentarblock am Dateikopf vorhanden ist.

```java
/**
 * Traceability:
 *   UC: UC-001 (Person anlegen), UC-002 (Person aktualisieren)
 *   TCs: TC-001, TC-002, TC-004
 *   Last traced: YYYY-MM-DD
 */
```

- Wenn vorhanden: aktualisiere die Werte.
- Wenn nicht vorhanden: füge direkt nach dem Package-Statement ein.

---

## Schritt 5 – Abschlussbericht

Gib einen strukturierten Bericht aus:

```
## Traceability-Bericht — YYYY-MM-DD

### Zusammenfassung
- Use Cases gesamt: X
- Vollständig abgedeckt (impl + TC + IT): X (XX%)
- Mit Lücken: X
- Nicht implementiert: X

### Änderungen vorgenommen
- architecture.md: Traceability-Abschnitt [erstellt|aktualisiert]
- testdesign.md: Traceability-Status [erstellt|aktualisiert]
- X UC-Dateien: traceability-Frontmatter aktualisiert
- X IT-Klassen: Traceability-Kommentar [ergänzt|aktualisiert]

### Kritische Lücken (Handlungsbedarf)
- UC-013 (Mahnung versenden): Weder Endpunkt noch TC vorhanden
  → Empfehlung: Implementierung priorisieren oder UC explizit als "out of scope" markieren
- TC-015: In testdesign.md definiert, aber keine IT-Klasse vorhanden
  → Empfehlung: IT-Klasse MahnungVerwaltenIT erstellen

### Nicht-kritische Hinweise
- POST /api/abrechnungen/kalkulieren: Kein referenzierender UC gefunden (IMPL-ONLY)
```

Halte den Bericht knapp — eine Zeile pro Befund, keine Prosa-Absätze.

---

## Hinweise zur Implementierung

- **Datums-Stempel**: Verwende das aktuelle Datum im Format `YYYY-MM-DD`
- **Frontmatter-Parsing**: YAML-Frontmatter in UC-Dateien ist zwischen `---`-Trennern — beim Schreiben darauf achten, die restliche Datei unangetastet zu lassen
- **IT-Klassen-Scan**: `@DisplayName("TC-XXX")` ist das primäre Mapping — falls kein `@DisplayName`, den Methodennamen als Fallback nutzen
- **Partial Match**: Ein Endpunkt deckt einen UC ab, wenn Verb + Ressource semantisch übereinstimmen — kein exakter String-Match nötig
- **Vorsicht bei Mehrfach-TCs pro UC**: Ein UC kann mehrere TCs haben (Happy Path + Fehlerfälle) — alle auflisten