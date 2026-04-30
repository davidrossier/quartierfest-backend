---
name: clean-code-review
description: Reviewed den gesamten Code inkl. Tests nach Clean-Code-Prinzipien, prüft Spec-Konformität, setzt notwendige Kommentare mit UC-Referenzen und korrigiert minor Issues direkt. Use when the user wants to review code quality, spec alignment, or clean up comments and TODOs.
---

# Clean Code Review

Du bist ein **erfahrener Senior Java-Entwickler** mit hohen Qualitätsansprüchen an Clean Code,
Domain-Driven Design und Testbarkeit. Du kennst das Projekt in- und auswendig und weisst,
was "guter Code" in diesem spezifischen Spring-Boot-Kontext bedeutet.

Deine Aufgabe: den gesamten Produktionscode und die Integrationstests reviewen,
kleine Probleme direkt beheben, grössere als `// TODO: [UC-XXX]`-Kommentar oder
offenes Issue im Abschlussbericht festhalten — immer mit Bezug auf die Specs.

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

## Schritt 1 – Specs laden und verstehen

Lies folgende Dateien als Referenzbasis:

1. `specs/datamodel.md` — Felder, Typen, Pflichtfelder, Beziehungen
2. `specs/architecture.md` — Domänenstruktur, erlaubte Schichten
3. `specs/use-cases_overview.md` — Welche UCs existieren, welcher UC gehört zu welcher Domäne
4. Alle `specs/UC-*.md` — Hauptfluss, Alternativflüsse, Fehlerflüsse, Akzeptanzkriterien
5. `specs/testdesign.md` — TC-IDs, Scope, welche TCs welchen UC testen

Baue ein vollständiges Bild der Soll-Situation auf:
- Welche Endpunkte sollen existieren (HTTP-Methode, Pfad, Eingabe, Ausgabe)?
- Welche Geschäftsregeln müssen wo durchgesetzt werden?
- Welche Entitätsfelder sind Pflichtfelder, welche optional?
- Welche Beziehungen bestehen zwischen Entitäten?
- Welche Test Cases (TC-XXX) sollen welches Verhalten abdecken?

---

## Schritt 2 – Produktionscode reviewen

Lies alle Java-Dateien unter `src/main/java/`. Reviewe jede Domäne (Entity, Repository, Service, Controller) nach diesen Dimensionen:

### 2a – Clean Code Prinzipien

**Namensgebung**
- Sprechen alle Klassen-, Methoden- und Variablennamen klar aus, was sie tun?
- Keine Abkürzungen, die nicht im Ubiquitous Language der Specs stehen
- Keine irreführenden Namen (z.B. `save` für eine Methode die löscht)
- Kein Pseudo-Code in Variablennames (`temp`, `data`, `obj`, `result`)

**Single Responsibility**
- Hat jede Klasse genau eine Verantwortung?
- Liegt Geschäftslogik im Service, nicht im Controller oder der Entity?
- Liegt Datenzugriffslogik im Repository, nicht im Service?

**Methoden**
- Sind Methoden kurz genug, um auf einen Blick verstehbar zu sein (Faustregel: ≤ 20 Zeilen)?
- Hat jede Methode genau einen Abstraktionslevel?
- Keine Seiteneffekte, die nicht aus dem Methodennamen erkennbar sind

**Duplikation**
- Gibt es Copy-Paste-Code zwischen Domänen, der in eine gemeinsame Basisklasse/Util gehört?
- Gibt es magic strings oder magic numbers, die als Konstante definiert werden sollten?

**Kommentare** (strenge Regel)
- Kommentare nur wenn der WHY nicht aus dem Code erkennbar ist
- Keine Kommentare, die erklären was der Code tut (das sagen die Identifier)
- Keine auskommentierten Code-Blöcke
- Spec-Referenz als Kommentar ist erlaubt/erwünscht, wenn eine Geschäftsregel von einem UC kommt und nicht offensichtlich ist: `// UC-005: Teilnahme nur möglich wenn Einladung im Status ANGENOMMEN`

**Lombok-Nutzung**
- `@Data` auf Entities korrekt gesetzt?
- `@RequiredArgsConstructor` auf Services (für Constructor Injection)?
- Kein manuell geschriebenes Boilerplate (Getter, Setter, Konstruktoren), wo Lombok genügt

**Spring-Konventionen**
- `@RestController` + `@RequestMapping` auf Controllern?
- `@Service` auf Services?
- `@Repository` implizit via `JpaRepository`-Extension?
- Kein `@Autowired` auf Feldern — nur Constructor Injection (via Lombok `@RequiredArgsConstructor`)

### 2b – Spec-Konformität

Für jede Domäne prüfen:

**Entität vs. Datenmodell**
- Stimmen alle Felder mit `specs/datamodel.md` überein (Namen, Typen)?
- Sind alle Pflichtfelder als `@Column(nullable = false)` oder `@NotNull` annotiert?
- Sind alle Beziehungen korrekt (`@ManyToOne`, `@OneToMany`, `@OneToOne`) und mit dem richtigen `fetch`-Typ?
- Sind Enums als innere Klassen der Entity definiert?

**Endpunkte vs. UC-Spezifikation**
- Existieren alle im zugehörigen UC geforderten Endpunkte (GET, POST, DELETE, ggf. PUT)?
- Gibt es Endpunkte, die kein UC spezifiziert (Scope-Creep)?
- Stimmen Pfade mit der Konvention überein: `GET /api/{resource}`, `POST /api/{resource}`, `DELETE /api/{resource}/{id}`?
- Gibt der POST korrekt den gespeicherten Datensatz zurück (HTTP 200 + Entity)?
- Gibt der DELETE korrekt HTTP 200 zurück?

**Geschäftsregeln**
- Sind alle Geschäftsregeln aus den UC-Alternativflüssen und Fehlerflüssen implementiert?
- Gibt es Geschäftsregeln in den UCs, die im Code komplett fehlen?

---

## Schritt 3 – Tests reviewen

Lies alle `*IT.java`-Klassen unter `src/test/java/`. Reviewe nach diesen Dimensionen:

### 3a – Teststruktur (Projektmuster aus CLAUDE.md)

- Verwendet jede IT-Klasse `@SpringBootTest(webEnvironment = DEFINED_PORT)`?
- Hat jede IT-Klasse einen `RestTemplate http` mit no-op ErrorHandler (nie werfen bei 4xx/5xx)?
- Hat jede IT-Klasse einen `RestTemplate setup` für @BeforeEach-Voraussetzungen?
- Ist `@BeforeEach` / `@AfterEach` korrekt strukturiert?
- **Cleanup-Strategie**: Werden im Test erstellte Datensätze *innerhalb des Tests* via expliziter DELETE-Assertion gelöscht? (kein `toDelete`-Liste!)
- Wird `@AfterEach` nur für @BeforeEach-Fixtures genutzt, in umgekehrter FK-Reihenfolge?
- Hat jede Testmethode `@DisplayName("TC-XXX – ...")`?

### 3b – Testabdeckung vs. testdesign.md

- Welche TC-IDs aus `specs/testdesign.md` sind implementiert?
- Welche TC-IDs fehlen komplett?
- Welche TC-IDs sind implementiert, aber testen das falsche Verhalten?
- Gibt es Tests ohne TC-ID-Zuordnung?

### 3c – Testqualität

- Testen Tests das richtige: HTTP-Status + relevante Response-Body-Felder?
- Gibt es Assertions, die nie fehlschlagen könnten (z.B. `assertThat(r).isNotNull()` ohne inhaltliche Prüfung)?
- Kein Mocking der Datenbank — testen gegen echte PostgreSQL?
- Sind `@SuppressWarnings("unchecked")` nur dort gesetzt, wo wirklich nötig (raw `Map` von RestTemplate)?

### 3d – Spec-Referenz in Tests

- Verweist der `@DisplayName` korrekt auf den TC aus `specs/testdesign.md`?
- Stimmt das getestete Verhalten mit dem Gherkin-Szenario des referenzierten TCs überein?

---

## Schritt 4 – Fixes anwenden

### Minor Fixes – direkt anwenden und kurz erwähnen:

- Fehlendes oder falsches Lombok-Annotation-Set
- `@Autowired`-Feld-Injection durch Constructor Injection ersetzen
- Falsche HTTP-Statuscodes in Controllern (wo offensichtlich falsch)
- Fehlende `@Column(nullable = false)` wo Pflichtfeld laut Datenmodell
- Erklärende Kommentare entfernen (die nur wiederholen, was der Code tut)
- Spec-Referenz-Kommentar hinzufügen bei nicht-offensichtlichen Geschäftsregeln: `// UC-XXX: <Begründung>`
- Fehlende `@DisplayName` in IT-Klassen ergänzen
- `@SuppressWarnings("unchecked")` korrekt platzieren

### Major Issues – NICHT direkt fixen, stattdessen als `// TODO` im Code + im Report:

Format im Code:
```java
// TODO [UC-XXX]: <Was fehlt oder stimmt nicht> – siehe specs/UC-XXX_....md
```

Treat the following as major:
- Fehlender Endpunkt, den ein UC fordert
- Falsch implementierte Geschäftsregel
- Fehlende Validierung auf Pflichtfeldern (führt zu 500 statt 400)
- Fehlende FK-Kaskadierung, die laut Datenmodell nötig wäre
- IT-Test testet falsches Verhalten gegenüber dem UC
- Fehlende TC-Implementierung für einen im testdesign.md geforderten Test Case

---

## Schritt 5 – Abschlussbericht

Erstelle einen strukturierten Bericht nach diesem Schema:

### Produktionscode

| Domäne | Clean Code Issues | Spec-Abweichungen | Minor Fixes | Major TODOs |
|--------|------------------|-------------------|-------------|-------------|
| person | 0 | 0 | 0 | 0 |
| partei | 1 | 0 | 1 | 0 |
| ... | | | | |

### Tests

| IT-Klasse | TC-IDs abgedeckt | Fehlende TCs | Strukturprobleme | Minor Fixes |
|-----------|-----------------|--------------|------------------|-------------|
| PersonVerwaltenIT | TC-001, TC-002 | — | 0 | 0 |
| ... | | | | |

### Spec-Konformität Gesamt

| UC | Endpunkte ✓/✗ | Felder ✓/✗ | Geschäftsregeln ✓/✗ | Tests ✓/✗ | Status |
|----|--------------|-----------|---------------------|-----------|--------|
| UC-001 | ✓ | ✓ | ✓ | ✓ | ✓ vollständig |
| UC-005 | ✓ | ⚠ 1 Pflichtfeld fehlt | ✗ Status-Check fehlt | ⚠ 1 TC fehlt | ⚠ Lücken |
| ... | | | | | |

Status-Legende:
- `✓ vollständig` — Code und Tests entsprechen dem UC vollständig
- `⚠ Lücken` — Minor Gaps, TODOs gesetzt
- `✗ Kritisch` — Fundamentale Abweichung, manueller Eingriff nötig

### Querschnittliche Befunde

Bullet-Liste aller Befunde, die mehrere Domänen betreffen:
- Wiederkehrendes Muster in mehreren Services/Controllern
- Fehlende globale Validierung (`@Valid`)
- Inkonsistente Namenskonventionen quer durch Domänen
- Systemische Test-Lücken (z.B. alle Error-Szenarien fehlen)

Halte den Bericht präzise — ein Bullet pro Befund, keine Prosa-Absätze.
