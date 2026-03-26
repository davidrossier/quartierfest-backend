# Quartierfest – Requirements

## Übersicht
Software zur Unterstützung bei Planung, Durchführung und Nachbearbeitung von Quartierfesten (Buchlenfest).

---

## UC-01: Personendaten verwalten
**Beschreibung:** Die Daten der Empfänger der Einladung können erfasst, aktualisiert oder gelöscht werden.

**Details:**
- Daten können für mehrere Events wiederverwendet werden
- Felder: Name, Vorname, Telefonnummer, Mobilenummer, E-Mail-Adresse

---

## UC-02: Parteien verwalten
**Beschreibung:** Personen werden zu einer Partei gruppiert. Typischerweise gilt als Partei ein Haushalt.

**Details:**
- Eine Partei erhält eine Einladung
- Felder: Adresse, Twint-Angabe (wenn ja: Mobilenummer)
- Eine Partei besteht aus einer oder mehreren Personen

---

## UC-03: Event anlegen
**Beschreibung:** Die Durchführung eines Events wird geplant.

**Details:**
- Felder: Datum, Startzeit, Standort, Alternativer Standort (bei schlechtem Wetter), Zeit für Aufstellen, Zeit für Aufräumen
- Ein Event hat Beziehungen zu: teilnehmende Personen, Konsumationsangebot, effektive Konsumation, Allgemeinkosten, individuelle Abrechnungen

---

## UC-04: Einladung erstellen
**Beschreibung:** Für ein Event wird für alle möglichen Parteien eine Einladung erstellt.

**Details:**
- Enthält alle relevanten Angaben zum Event
- Partei kann sich an- oder abmelden
- Partei gibt an wieviele Personen teilnehmen
- Partei gibt an ob sie beim Aufstellen/Aufräumen hilft
- Partei gibt an ob und was sie zum Essensbuffet beiträgt (Salate, Brot/Zopf, Dessert, weitere Beilagen)

---

## UC-05: Teilnahmen verwalten
**Beschreibung:** Die Rückmeldungen aus den Einladungen werden übernommen und verwaltet.

**Details:**
- Konsolidierte Sicht der teilnehmenden Personen und deren Angaben
- Rückmeldungen können nachträglich editiert werden

---

## UC-06: Bestätigung erstellen
**Beschreibung:** Einige Tage vor dem Event wird eine Bestätigung an die teilnehmenden Parteien zugestellt.

**Details:**
- Enthält: definitiver Standort, Erinnerung der Zeiten, Zusammenstellung Buffet, Konsumationsangebot

---

## UC-07: Allgemeinausgaben verwalten
**Beschreibung:** Ausgaben die durch alle Teilnehmenden gleichermassen getragen werden.

**Beispiele:** Kühlschrankmiete, Festbankgarnituren, Servietten, Tischtücher

**Details:**
- Pro Ausgabe: was, woher beschafft
- Ausgaben werden je Event zusammengetragen
- Werden bei der Schlussabrechnung berücksichtigt

---

## UC-08: Konsumationsangebot verwalten
**Beschreibung:** Angebot an Getränken, welche bei Konsumation individuell bezahlt werden.

**Details:**
- Angebot besteht aus verschiedenen Einträgen mit je einem Preis
- Angebot wird bei der Bestätigung kommuniziert

---

## UC-09: Konsumationsliste erstellen
**Beschreibung:** Liste zur Erfassung der Getränkekonsumation am Event.

**Details:**
- X-Achse: Konsumationsangebot inkl. Preis
- Y-Achse: teilnehmende Parteien
- Konsumation wird handschriftlich mit Strichen festgehalten
- Liste wird gross ausgedruckt und am Kühlschrank montiert
- Je Partei: Möglichkeit zur definitiven Angabe der effektiven Teilnehmerzahl (relevant für Abrechnung)

---

## UC-10: Konsumation übernehmen
**Beschreibung:** Die handschriftlich ausgefüllte Konsumationsliste wird digital erfasst.

**Details:**
- Konsumation je Partei wird ins System übertragen

---

## UC-11: Abrechnung erstellen
**Beschreibung:** Die Abrechnung wird je Partei zusammengestellt.

**Details:**
- Allgemeinkosten werden auf Anzahl Personen aufgeteilt
- Konsumationsanteil der Partei wird addiert

---

## UC-12: Abrechnung zustellen
**Beschreibung:** Die Abrechnung wird den Parteien zugestellt.

**Details:**
- Mit Twint: Betrag wird via Twint angefordert
- Ohne Twint: Schreiben via E-Mail oder in Papierform
- Zahlungsmöglichkeiten: Twint, Überweisung, Bar

---

## UC-13: Inkasso sicherstellen
**Beschreibung:** Die erfolgte Zahlung wird kontinuierlich kontrolliert und festgehalten.

**Details:**
- Erfasst wird: Zahlungskanal, Datum, bezahlter Betrag
- Mahnungen werden ebenfalls festgehalten