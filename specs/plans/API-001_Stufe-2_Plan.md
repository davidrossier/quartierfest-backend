# API-001 Stufe 2 – DTO-Layer (Request-/Response-Records), Entkopplung vom DB-Schema

> Stand: 2026-09-25, am selben Tag nach kritischer Prüfung revidiert (siehe «Revision») und umgesetzt (siehe «Umsetzungsnotizen»). Umsetzungsplan zu `TODO.md` → API-001 (Stufe 2). Baut auf Stufe 1 auf (`specs/plans/API-001_Stufe-1_Plan.md`).
> Branch (beide Repos): `feature/api-001-dto-stufe-2`

## Ziel

Die JPA-Entities sind nicht mehr der API-Contract. Jeder Endpunkt nimmt ein Request-Record entgegen und liefert ein Response-Record:

1. **Request-/Response-Schemas getrennt.** Die Spec enthält pro Ressource ein `XxxRequest` und ein `XxxResponse`. Das Frontend generiert daraus **auch die Payload-Typen**; `Persisted<>` und alle handgeschriebenen `*Payload`-Interfaces entfallen.
2. **Keine Collections in Referenzen.** Verschachtelte Referenzen werden als schlanke `Kurz`-Records ausgeliefert (z.B. Partei ohne `personen`). Das löst den PERF-001-Rest: kein Lazy-Loading mehr bei der Serialisierung, OSIV kann abgeschaltet werden.
3. **Entities ohne Jackson-Annotationen.** `@JsonIgnore`, `@JsonProperty(WRITE_ONLY)` und die `@Transient`-Hilfsfelder (`Benutzer.passwort`, `Partei.personenIds`) wandern in die Request-Records.
4. **Kein POST-Upsert mehr.** Jede Ressource, die das Frontend bearbeitet, bekommt einen PUT. Das sind neben Person, Partei, Event und Teilnahme neu Einladung und Abrechnung (REST-003) sowie Konsumation, Konsumationsangebot und Allgemeinausgabe (**Erweiterung von REST-003**, bisher nirgends erfasst).
5. **Beifang, der ohnehin dieselben Zeilen anfasst:** CODE-002 (`@Data` → `@Getter`/`@Setter`), REST-002 (PUT auf unbekannte id → 404), Fehler-Schema `ApiError` in der Spec.

**Nicht Teil von Stufe 2:**
- **API-002** (Event-Filter `?eventId=`). Die Responses bekommen aber bereits alles, was der Filter braucht; API-002 ist danach rein additiv und fasst die Response-Typen nicht mehr an.
- **BIZ-001** (Abrechnungs-Berechnung im Backend). Eigener fachlicher Umbau; profitiert vom `AbrechnungRequest`, hängt aber nicht davon ab.
- **REFACT-001** (CRUD-Basisklassen). Mit DTOs ist der Boilerplate pro Controller nicht mehr identisch (Mapping, Referenz-Auflösung). Nach Stufe 2 neu beurteilen.

## Entscheide

| # | Entscheid | Begründung |
|---|---|---|
| E1 | **Java Records im Domain-Package**, Namensschema `XxxRequest`, `XxxResponse`, `XxxKurz`; eigene Update-Requests nur wo PUT fachlich eingeschränkt ist (`TeilnahmeUpdateRequest`, neu `EinladungUpdateRequest`, `AbrechnungUpdateRequest`, `KonsumationUpdateRequest`) | Package-by-feature beibehalten, kein `dto`-Unterpackage. `TeilnahmeUpdateRequest` und `LoginRequest` sind bereits so gebaut. Schema-Namen bleiben in der Spec eindeutig. |
| E2 | **Mapping von Hand**, statische Factory `XxxResponse.von(Xxx entity)` im Record; kein MapStruct | 13 Entities, flache Abbildung. Ein zweiter Annotation-Processor neben Lombok lohnt sich nicht und macht Build-Fehler schwerer lesbar. |
| E3 | **Response-Pfade, die das Frontend heute liest, bleiben erhalten** (z.B. `teilnahme.einladung.event.id`, `abrechnung.teilnahme.einladung.partei.bezeichnung`); Referenzen werden durch `Kurz`-Records ohne Collections ersetzt | Nutzungsanalyse (siehe Anhang): über 40 Template- und Filterstellen lesen verschachtelte Pfade. Pfaderhalt hält die Frontend-Umstellung klein und die E2E-Tests stabil. Die Payload-Grösse sinkt trotzdem stark, weil `personen` und `buffetBeitraege` aus allen Referenzen verschwinden. Die clientseitigen Event-Filter über `…einladung.event.id` fallen mit API-002 ohnehin weg; ein Abflachen lohnt sich erst dann. |
| E4 | **Requests referenzieren über flache IDs** (`eventId`, `parteiId`, `teilnahmeId`, …) statt `{ "event": { "id": 1 } }` | Validierung wird trivial (`@NotNull Long eventId` statt `@NotNull @Valid Ref` + `@NotNull id`). Das Request-Schema ist ohne Hilfstyp ausdrückbar. Prod enthält nur Testdaten, keine Abwärtskompatibilität nötig. |
| E5 | **Referenz-Auflösung im Service**: `repository.findById(id).orElseThrow(→ 400 «Referenzierter Datensatz existiert nicht.»)` | Heute endet eine unbekannte Referenz-id als FK-Verletzung → 409 (ERROR-001, TC-012 und TC-023). 400 beschreibt den Fehler korrekt (ungültige Eingabe) und ist vor dem Insert prüfbar. Die 409 für Unique-Verletzungen (DB-002, TC-042 bis TC-044) und für das Löschen referenzierter Datensätze bleibt. |
| E6 | **Unbekannte JSON-Felder → 400** (`FAIL_ON_UNKNOWN_PROPERTIES=true`) | Records haben kein `id`-Feld; Spring Boot ignoriert unbekannte Felder standardmässig. Ein POST mit `id` würde sonst still einen **neuen** Datensatz anlegen, und TC-041 (REST-001) wäre gebrochen. Strikte Deserialisierung deckt das für alle Ressourcen ab, ohne pro Controller `id` zu prüfen. **Voraussetzung:** Für jede Ressource, die das Frontend heute per POST mit `id` aktualisiert, existiert ein PUT (Ziel 4, A4). Sonst scheitern diese Bearbeitungen mit 400. |
| E7 | **Pflichtfelder in Responses per `OpenApiCustomizer`**: alle Properties von `*Response`/`*Kurz`-Schemas sind `required`, nullbare Felder sind mit `@Nullable` (JSpecify) markiert und bekommen `type: [T, "null"]` | Ohne das sind alle Record-Komponenten in der Spec optional, und das Frontend bräuchte weiter `Persisted<>`. Jackson serialisiert `null`-Werte mit, «required + nullable» ist also die korrekte Beschreibung. Spec ist OpenAPI 3.1.0. |
| E8 | **Transaktionen in allen Service-Methoden**: lesende mit `@Transactional(readOnly = true)`, schreibende mit `@Transactional`. Controller sehen nur Records. Die Eigentümerprüfung in `TeilnahmeZugriff` wird eine Repository-Abfrage (`existsByIdAndEinladungParteiId`) statt einer Navigation über Entity-Beziehungen | Heute gibt es im Backend kein einziges `@Transactional`; alles hängt an OSIV. Ohne OSIV scheitern sonst das Teilnahme-Update (Leeren der lazy `buffetBeitraege`) und nach E9 die Eigentümerprüfung (`t.getEinladung().getPartei()`). Beides träfe den PARTEI-Weg aus UC-016. Die Repository-Abfrage braucht keine Transaktion und nur ein Statement. |
| E9 | **Alle `@ManyToOne`/`@OneToOne` auf `FetchType.LAZY`**, benötigte Pfade per `join fetch` in den `findAll`-Queries. **Erst nach der letzten umgestellten Ressource**, zusammen mit `open-in-view=false` (A5) | Der JPA-Default `EAGER` lädt z.B. für `GET /api/zahlungen` die ganze Kette Abrechnung → Teilnahme → Einladung → Event/Partei nach, obwohl die Response nur `abrechnung.id` braucht. `getId()` auf einem Hibernate-Proxy initialisiert ihn nicht. **Nicht früher:** Das Backend hat kein Jackson-Modul für Hibernate. Eine noch nicht umgestellte Entity würde Hibernate-Proxies serialisieren und mit 500 scheitern. |
| E10 | **Ein Branch-Paar, ein Merge-Paar**, intern ein Commit pro Ressource | Jede Contract-Änderung erzwingt einen Frontend-PR (Drift-Check, Stufe 1 E4). Drei Slice-Paare hiessen dreimal Contract-Bruch zwischen Backend- und Frontend-Merge. |

## Reihenfolge

Wie Stufe 1: Backend-PR zuerst mergen, Frontend-PR direkt danach. **Neu zu beachten:** Der nächtliche E2E-Lauf (CI-001, 03:00 UTC) kombiniert Frontend-`main` mit Backend-`main`. Liegt ein Nachtlauf zwischen den beiden Merges, ist er rot. Deshalb:

1. Vor dem Merge den E2E-Workflow vom Frontend-Branch gegen den Backend-Branch starten: `gh workflow run e2e.yml --ref feature/api-001-dto-stufe-2 -f backend_ref=feature/api-001-dto-stufe-2`.
2. Beide PRs unmittelbar nacheinander mergen, ohne Nachtlauf dazwischen.

Innerhalb des Backends gilt: **Jeder Schritt endet mit grünem `./mvnw verify`.** Deshalb stehen Lazy Loading und das Abschalten von OSIV am Schluss (A5), nicht im Querschnitt.

---

## Teil A – Backend (`quartierfest-backend`)

### A0 Spike (vor allem anderen, ca. 1 h)

Mit **einer** Ressource (`Konsumationsangebot`: eine Referenz, keine Collection) den ganzen Weg durchspielen und drei offene Technikfragen klären. Ergebnis unten in den Umsetzungsnotizen festhalten.

- **E7:** Erzeugt springdoc 3.1.1 für Records mit dem Customizer `required` und `type: [T, "null"]` so, dass openapi-typescript `field: T | null` generiert? Falls springdoc `@Nullable` nicht kennt: Customizer liest die Annotation über `Class.getRecordComponents()` selbst.
- **E6:** Greift `spring.jackson.deserialization.fail-on-unknown-properties=true` unter Boot 4 / Jackson 3? Und liefert der `GlobalExceptionHandler` dafür eine 400 im `ApiError`-Format, ohne DB-/Klassendetails in der `message`?
- **E6, Rückwirkung:** Solange andere Ressourcen noch Entities entgegennehmen, lehnt die strikte Deserialisierung dort auch heute tolerierte Felder ab. Prüfen, ob die ITs oder das Frontend solche Felder senden (z.B. ganze Response-Objekte zurückschicken). Falls ja, die Property erst in A5 setzen und bis dahin nur die umgestellten Ressourcen per Record-Annotation `@JsonIgnoreProperties(ignoreUnknown = false)` strikt machen.

### A1 Querschnitt

- `application.properties` (vorbehaltlich Spike, siehe A0):
  ```properties
  # API-001 Stufe 2: unbekannte Felder (z.B. id im POST) → 400 statt stillem Ignorieren
  spring.jackson.deserialization.fail-on-unknown-properties=true
  ```
  `spring.jpa.open-in-view=false` kommt **nicht** hier, sondern in A5.
- `OpenApiConfig`: `OpenApiCustomizer` für E7 und Registrierung von `GlobalExceptionHandler.ApiError` als Schema. Default-Responses 400/401/403/404/409 mit `ApiError` an alle Operationen hängen (Stufe-1-Open-Item «Fehler-Schema in die Spec»).
- Controller: `produces = MediaType.APPLICATION_JSON_VALUE` auf Klassenebene. Die Spec zeigt dann `application/json` statt `*/*` (Stufe-1-Befund).
- Gemeinsamer Helper für E5, z.B. `Referenzen.aufloesen(repository, id, "Event")` im Root-Package. Kein generischer Service, nur diese eine Methode.

### A2 Entities bereinigen (CODE-002)

Ein Commit vor den DTOs, damit der DTO-Diff lesbar bleibt:

- `@Data` → `@Getter` + `@Setter` auf allen 13 Entities. `equals`/`hashCode` weglassen (Objektidentität). Kein Test stützt sich auf Entity-Gleichheit; `BenutzerServiceTest` prüft `save(benutzer)` mit derselben Instanz. `TeilnahmeBuffetBeitrag` als `@Embeddable` behält `@Data` (Wertobjekt ohne Beziehungen).
- Fetch-Typen und Jackson-Annotationen bleiben hier **unverändert**: Solange Controller Entities ausliefern, schützen die Annotationen `passwortHash`, und `EAGER` verhindert Proxies in der Serialisierung (E9).
- `./mvnw verify` muss unverändert grün sein.

### A3 Records, Mapping und Transaktionen pro Ressource

Reihenfolge von flach nach tief. Jede Zeile ist ein Commit mit grünem `./mvnw verify` und nachgeführtem `specs/openapi.json` (`OPENAPI_UPDATE=true ./mvnw verify -Dit.test=OpenApiContractIT`).

| Schritt | Ressource | Request | Response (Referenzen) |
|---|---|---|---|
| 1 | Person | `PersonRequest` (vorname, name, telefonnummer, mobilenummer, email) | `PersonResponse` |
| 2 | Event | `EventRequest` | `EventResponse` |
| 3 | Konsumationsangebot | `…Request` (`eventId`, bezeichnung, preis), auch für PUT | `…Response` (`event: EventResponse`) · `KonsumationsangebotKurz(id, bezeichnung, preis)` |
| 4 | Allgemeinausgabe | `…Request` (`eventId`, …), auch für PUT | `…Response` (`event: EventResponse`) |
| 5 | Partei | `ParteiRequest` (…, `personenIds`) | `ParteiResponse` (`personen: List<PersonResponse>`) · `ParteiKurz(id, bezeichnung, adresse, twintAktiv, twintMobilenummer)` |
| 6 | Benutzer | `BenutzerRequest` (email, passwort, rolle, `parteiId`) | `BenutzerResponse` (`partei: ParteiKurz \| null`), nie `passwort`/`passwortHash` |
| 7 | Einladung | `EinladungRequest` (`eventId`, `parteiId`, …), `EinladungUpdateRequest` | `EinladungResponse` (`event: EventResponse`, `partei: ParteiKurz`) · `EinladungKurz(id, status, anzahlPersonen, event: EventResponse, partei: ParteiKurz)` |
| 8 | Teilnahme | `TeilnahmeRequest` (`einladungId`, …), `TeilnahmeUpdateRequest` bleibt | `TeilnahmeResponse` (`einladung: EinladungKurz`, `buffetBeitraege`) · `TeilnahmeKurz(id, einladung: EinladungKurz)` |
| 9 | Konsumation | `…Request` (`teilnahmeId`, `konsumationsangebotId`, anzahl), `KonsumationUpdateRequest` (anzahl) | `…Response` (`teilnahme: TeilnahmeKurz`, `konsumationsangebot: KonsumationsangebotKurz`) |
| 10 | Abrechnung | `AbrechnungRequest` (`teilnahmeId`, …), `AbrechnungUpdateRequest` | `AbrechnungResponse` (`teilnahme: TeilnahmeKurz`) · `AbrechnungKurz(id)` |
| 11 | Zahlung, Mahnung | `…Request` (`abrechnungId`, …) | `…Response` (`abrechnung: AbrechnungKurz`) |
| 12 | Auth | `LoginRequest`/`LoginResponse` bestehen bereits | unverändert |

Pro Schritt:
- Controller-Signaturen auf Records umstellen, Entity-Typen verschwinden aus dem Controller.
- Service (E8): `findAll()` liefert `List<XxxResponse>` in `@Transactional(readOnly = true)`. `create(XxxRequest)` und `update(id, …)` sind `@Transactional`, lösen Referenzen auf (E5), bauen oder ändern die Entity, speichern und mappen noch innerhalb der Transaktion.
- PUT-Endpunkte: `update(id, request)` lädt die Entity per `findById` → 404 und überträgt die Felder (**REST-002** für Person, Partei, Event). Das bisherige `setId(id)` + `save()` entfällt. Die neuen PUTs aus A4 entstehen im selben Schritt wie ihre Ressource.
- Listen in Request-Records mit `@Valid` annotieren (`buffetBeitraege`), sonst greift die Validierung der Elemente nicht. `TeilnahmeBuffetBeitrag.art` bekommt `@NotNull`.
- Jackson-Annotationen und `@Transient`-Felder aus der Entity entfernen, sobald die Ressource umgestellt ist (Benutzer: `passwort`-Validierung `@NotBlank @Size(min = 10)` wandert in `BenutzerRequest`; `BenutzerService` muss das Passwort nicht mehr nach dem `save()` leeren).
- Teilnahme (Schritt 8): `TeilnahmeZugriff.darfBearbeiten` auf `teilnahmeRepository.existsByIdAndEinladungParteiId(teilnahmeId, parteiId)` umstellen (E8). Die explizite `id`-Prüfung im POST entfällt zugunsten von E6.
- Repository-Fetch-Joins kommen erst in A5, wenn die Beziehungen lazy werden.

### A4 PUT statt POST-Upsert (REST-003, erweitert)

Heute aktualisiert das Frontend fünf Ressourcen per POST mit gesetzter `id`. Mit E6 würde das 400 liefern, ohne E6 still Duplikate anlegen. Alle fünf bekommen einen PUT mit Existenzprüfung → 404:

| Endpunkt | Body | Nicht änderbar | Frontend-Aufrufer heute |
|---|---|---|---|
| `PUT /api/einladungen/{id}` | `EinladungUpdateRequest`: status, anzahlPersonen, hilftAufstellen, hilftAufraumen, buffetBeitrag, buffetBeitragBeschreibung, bestaetigungVersendet | event, partei | `einladungen-verwaltung` (Rückmeldung), `bestaetigung-uebersicht` (`markiereVersendet()`, `alleMarkieren()`) |
| `PUT /api/abrechnungen/{id}` | `AbrechnungUpdateRequest`: zustellungskanal, zustellungsDatum, die drei Beträge | teilnahme | `abrechnungen-verwaltung` (Kanal ändern, `alsZugestelltMarkieren()`) |
| `PUT /api/konsumationen/{id}` | `KonsumationUpdateRequest`: anzahl | teilnahme, konsumationsangebot | `konsumationen-verwaltung` (Matrix, bestehende Zelle) |
| `PUT /api/konsumationsangebote/{id}` | `KonsumationsangebotRequest` | – | `konsumationsangebote-verwaltung` (Bearbeiten) |
| `PUT /api/allgemeinausgaben/{id}` | `AllgemeinausgabeRequest` | – | `allgemeinausgaben-verwaltung` (Bearbeiten) |

- **Korrektur gegenüber der TODO-Empfehlung zu REST-003:** Die Einladungs-Whitelist braucht nicht nur `status` und `bestaetigungVersendet`, sondern auch die Rückmeldungsfelder aus UC-004.
- Die Validierung `totalBetrag == anteil + konsumation` aus BIZ-001 **nicht** hier einbauen, sie gehört zu BIZ-001.
- Autorisierung: Die neuen PUTs fallen unter die Regel `/api/**` → nur ORGANISATOR in `SecurityConfig`. Keine Konfigurationsänderung nötig, aber in TC-040 (A6) explizit prüfen.

### A5 Abschluss: Lazy Loading und OSIV (E9)

Erst wenn kein Controller mehr eine Entity serialisiert:

- Alle `@ManyToOne`/`@OneToOne` → `fetch = FetchType.LAZY`.
- `application.properties`:
  ```properties
  # API-001 Stufe 2: Mapping in Service-Transaktionen, keine Lazy-Loads bei der Serialisierung
  spring.jpa.open-in-view=false
  ```
- `findAll()` mit `join fetch` genau auf die Pfade, die die Response liest: Einladung (`event`, `partei`), Teilnahme (`einladung.event`, `einladung.partei`, `buffetBeitraege`), Konsumation (`teilnahme.einladung.event/partei`, `konsumationsangebot`), Abrechnung (`teilnahme.einladung.event/partei`), `findEigeneAbStichtag` (UC-016). Partei behält den bestehenden Fetch-Join auf `personen`.
- `./mvnw verify` grün. Eine `LazyInitializationException` zeigt eine vergessene Transaktion oder einen fehlenden Fetch-Join.

### A6 Tests

- **Controller-Tests** (`*ControllerTest`, 13 Klassen, `@WebMvcTest`): Service-Mocks liefern Records statt Entities, Request-Bodies mit flachen IDs. Mechanisch.
- **ITs** (15 Dateien senden `{ "event": { "id": … } }`-Maps): auf flache IDs umstellen. Wo die IT ohnehin angefasst wird, das Request-Record statt `Map` senden (**TEST-002**, teilweise). Die Assertions lesen fast nur Top-Level-Felder (`$.id`, `$.status`, …) und bleiben.
- **Geänderte Erwartungen**, im Testdesign nachführen:
  - **TC-012** (Teilnahme mit unbekannter Einladung) und **TC-023** (Abrechnung mit unbekannter Teilnahme) wechseln von 409 auf 400 (E5). Die Kommentare «FK-Verletzung → 409» in `TeilnahmeVerwaltenIT` und `AbrechnungErstellenIT` anpassen.
  - TC-042 bis TC-044 (Unique-Verletzung → 409) bleiben unverändert.
  - TC-041 (POST Teilnahme mit `id`) bleibt 400, jetzt über E6.
  - TC-013 und TC-032 laufen über den neuen PUT (REST-003).
- **Neue TCs** (ab TC-048):
  - PUT-Happy-Path für Einladung, Abrechnung, Konsumation, Konsumationsangebot und Allgemeinausgabe. Die Aktualisierung der letzten drei ist heute durch keinen Integrationstest abgedeckt; TC-010 legt eine Einladung nur an.
  - PUT auf nicht-existente id → 404 für alle PUT-Endpunkte (REST-002/003).
  - PUT auf Einladung und Konsumation ignoriert keine Referenzfelder, sondern lehnt sie mit 400 ab (E6: `eventId` ist im Update-Request unbekannt).
  - POST mit `id` → 400, exemplarisch auf zwei Ressourcen.
  - `BenutzerResponse` enthält weder `passwort` noch `passwortHash` (bestehende Assertion prüfen, ggf. übernehmen).
  - **Query-Anzahl** (PERF-001-Rest, nach A5): `GET /api/abrechnungen` und `GET /api/einladungen` mit je drei Datensätzen lösen höchstens zwei SQL-Statements aus. Die Hibernate-Statistik **im Test programmatisch einschalten** (`entityManagerFactory.unwrap(SessionFactory.class).getStatistics().setStatisticsEnabled(true)`, danach `clear()` und `getPrepareStatementCount()`). Kein eigenes Testprofil: Ein zweiter Spring-Kontext kollidiert mit dem festen Port der 17 dev-ITs, genau deshalb nutzt der Security-Test einen zufälligen Port.
- **PARTEI-Weg nach A5:** TC-036 (PARTEI bestätigt eigene Teilnahme → 200) und TC-037 (fremde Teilnahme → 403) in `TeilnahmeBestaetigenIT` decken E8 bereits ab. Sie müssen nach A5 unverändert grün sein; kein neuer TC nötig.
- **TC-040** (`SecurityMatrixIT`): Zeilen für die fünf neuen PUTs ergänzen, PARTEI → 403, ohne Token → 401.
- `OpenApiContractIT` (TC-046) bleibt unverändert und friert den neuen Contract ein.

### A7 Sichtkontrolle der Spec

Im neuen `specs/openapi.json` prüfen:
- Keine Entity-Schemas mehr (`Einladung`, `Partei`, …), nur noch `*Request`, `*Response`, `*Kurz`, `ApiError`, Enums.
- `*Response`/`*Kurz`: alle Properties in `required`, nullbare mit `"null"` im Typ.
- `*Request`: `required` nur für `@NotNull`/`@NotBlank`; keine `id`.
- `BenutzerResponse` ohne `passwort`/`passwortHash`; `ParteiKurz` ohne `personen`.
- Enums als eigene Schemas: Wenn springdoc sie weiterhin inline pro Record erzeugt, `@Schema(enumAsRef = true)` an den Enums setzen, damit das Frontend einen gemeinsamen Typ bekommt.

### A8 Dokumentation nachführen

- `CLAUDE.md`: Abschnitt «API-Contract» auf Stufe 2 (Records, Mapping-Konvention, E4–E6, E8); Hinweise zu OSIV und `FAIL_ON_UNKNOWN_PROPERTIES`; IT-/TC-Zahlen.
- `README.md`: Endpunkt-Tabelle um die fünf neuen PUTs; Request-Format mit flachen IDs.
- `specs/architecture.md`: API-001 → erledigt; REST-Endpunkte; Traceability-Matrix für neue TCs.
- `specs/testdesign.md`: TC-048 ff., geänderte Erwartungen (TC-012, TC-023, TC-041), TC-013/TC-032.
- `specs/TODO.md`: API-001, CODE-002, REST-002, REST-003 nach «Behoben»; bei REST-003 die Erweiterung auf Konsumation, Konsumationsangebot und Allgemeinausgabe vermerken; PERF-001-«Bewusst offen» als erledigt markieren; Priorisierung Punkt 10 kürzen; API-002 um «Response-Typen bereits vorbereitet» ergänzen.
- `specs/UC-006`, `UC-012`: Open Items zum POST-Upsert schliessen. `UC-007`, `UC-008`, `UC-010`: Bearbeiten-Flows auf PUT prüfen und nachführen.

### A9 Verifikation Backend

```bash
./mvnw verify                                        # Unit + alle ITs grün, inkl. TC-046 und neue TCs
./mvnw spring-boot:run                               # dann:
curl -s localhost:8080/v3/api-docs | jq '.components.schemas | keys'   # nur Request/Response/Kurz/ApiError/Enums
```

---

## Teil B – Frontend (`quartierfest-frontend`)

### B1 Typen neu generieren

`npm run api:generate` gegen den Backend-Branch. `src/app/api/types.ts` (`Persisted<>`) löschen, sobald keine Referenz mehr existiert.

### B2 Modelle

Die Modellnamen bleiben, damit Komponenten und Services ihre Imports behalten. Jedes `*.model.ts` wird zu Aliasen:

```ts
import type { EinladungRequest, EinladungResponse, EinladungUpdateRequest } from '../api/schema';

export type Einladung = EinladungResponse;
export type EinladungPayload = EinladungRequest;
export type EinladungUpdatePayload = EinladungUpdateRequest;
export type EinladungStatus = EinladungResponse['status'];
```

- Die `Omit<…> & { event: Event; partei: Partei }`-Konstrukte entfallen.
- Wo eine Komponente heute `Partei` erwartet, aber nur eine `ParteiKurz` bekommt (Einladung, Teilnahme, Benutzer), meldet der Compiler die Stellen. Erwartung: keine, weil kein Template `einladung.partei.personen` liest.
- `BuffetBeitragEintrag`: Die Präzisierung «`art` immer gesetzt» entfällt, weil das Backend `art` mit `@NotNull` markiert (A3).

### B3 Services und Komponenten

- **Payload-Aufbau auf flache IDs** (E4): gut 20 Stellen `x: { id: … }` in 11 Komponenten, am meisten in `einladungen-verwaltung`, `bestaetigung-uebersicht` und `abrechnungen-verwaltung`.
- **Updates über PUT** (A4): Die Services für Einladung, Abrechnung, Konsumation, Konsumationsangebot und Allgemeinausgabe bekommen `update(id, payload)`. Umstellen:
  - `einladungen-verwaltung` (Rückmeldung) und `bestaetigung-uebersicht` (`markiereVersendet()`, `alleMarkieren()`).
  - `abrechnungen-verwaltung` (Kanal ändern, `alsZugestelltMarkieren()`).
  - `konsumationen-verwaltung`: Matrix-Zelle mit bestehender Konsumation → `update(id, { anzahl })`, sonst `save()`.
  - `konsumationsangebote-verwaltung` und `allgemeinausgaben-verwaltung`: Bearbeiten → `update()`, Erfassen → `save()`.
  - Die `id?`-Felder verschwinden aus allen Payloads.
- **Templates:** Keine Pfadänderung erwartet (E3). Der Compiler mit `strictTemplates` zeigt Abweichungen.

### B4 Tests und E2E

- Vitest-Fixtures (z.B. `meine-teilnahme.component.spec.ts`) an die neuen Typen anpassen. Der Compiler meldet fehlende oder überzählige Felder.
- `e2e/helpers/api-helpers.ts`: Request-Bodies auf flache IDs; die lokalen Antwort-Typen durch Importe aus `schema.d.ts` ersetzen, damit die E2E-Helfer künftig vom Drift-Check mitgeschützt sind.
- `e2e/specs/UC-005_Teilnahme-Verwalten.spec.ts:83` liest `x.einladung.id`; bleibt dank `EinladungKurz` gültig.
- Die E2E-Specs für UC-007, UC-008 und UC-010 prüfen, ob sie das Bearbeiten abdecken. Das ist nach A4 der einzige End-to-End-Nachweis für die drei neuen PUTs im Frontend.

### B5 Dokumentation nachführen

- `CLAUDE.md`: Abschnitt «API-Contract» auf Stufe 2 (Modelle sind Aliase, Payloads generiert, kein `Persisted<>` mehr); Modell-Liste kürzen.
- `README.md`: Abschnitt «API-Typen» anpassen.

### B6 Verifikation Frontend

```bash
npm run api:check                                    # Exit 0
npm test -- --watch=false
npm run build -- --configuration production
npm run e2e                                          # lokal gegen Backend-Branch (dev-Profil); Kaltstart-Flakiness UC-004/UC-015 bekannt
gh workflow run e2e.yml --ref feature/api-001-dto-stufe-2 -f backend_ref=feature/api-001-dto-stufe-2
```

---

## Abnahme (Definition of Done)

- [x] Kein Controller nimmt oder liefert eine JPA-Entity: `grep -rn "@RequestBody" src/main/java` zeigt nur `*Request`-Records (plus `LoginRequest`, `PasswortReset`), die Rückgabetypen sind `*Response` oder `void`.
- [x] Entities ohne Jackson-Annotationen und ohne `@Transient`-Felder; kein `@Data` mehr auf Entities.
- [x] Alle Service-Methoden transaktional; `TeilnahmeZugriff` ohne Navigation über Entity-Beziehungen.
- [x] Alle Beziehungen lazy, `spring.jpa.open-in-view=false`, alle ITs grün.
- [x] Query-Anzahl-Test (TC-054) für Einladungen, Teilnahmen, Abrechnungen, Zahlungen und Parteien grün (PERF-001-Rest behoben).
- [x] Kein POST-Upsert mehr: fünf neue PUTs, PUT auf unbekannte id → 404 auf allen PUT-Endpunkten (TC-052), POST mit `id` → 400 auf allen zwölf Ressourcen (TC-053).
- [x] TC-040 deckt die neuen PUTs ab; TC-036 und TC-037 (PARTEI-Weg) nach A5 grün.
- [x] `specs/openapi.json` enthält Request- und Response-Schemas getrennt, `ApiError` und nur noch `application/json`.
- [x] Frontend: `Persisted<>` und alle handgeschriebenen `*Payload`-Interfaces gelöscht; `api:check` (inkl. Negativtest), Vitest 27/27, Production-Build grün.
- [ ] E2E-Workflow auf den Feature-Branches grün, danach beide PRs ohne Nachtlauf dazwischen gemergt. Lokal ist die Suite grün (61 passed, 1 skipped); CI-Lauf und Merge stehen aus.
- [x] Doku in beiden Repos nachgeführt (A8, B5).

## Umsetzungsnotizen (2026-09-25)

**Ergebnis:** Backend 83 Unit-Tests und 74 IT-Ausführungen (52 Methoden, 20 Klassen) grün; Frontend Vitest 27/27, Production-Build, Drift-Check und Playwright lokal 61 passed / 1 skipped.

**Spike (A0)** — im ersten Umsetzungsschritt mit Person, Event und Konsumationsangebot beantwortet statt separat:
- **E7 angepasst:** springdoc 3.1.1 setzt für Records ohne Bean-Validation kein `required`. Der Customizer findet die Records per Classpath-Scan und macht alle Komponenten ohne JSpecify-`@Nullable` zu Pflichtfeldern. Nullbare Felder bleiben **optional** statt «required + `type: [T, "null"]`», und `spring.jackson.default-property-inclusion=non_null` lässt sie in der Antwort weg. Damit stimmt der TypeScript-Typ `feld?: T` exakt mit der Laufzeit überein, ohne `oneOf`-Konstrukte für nullbare `$ref`-Felder.
- **E6 bestätigt:** `spring.jackson.deserialization.fail-on-unknown-properties=true` greift unter Boot 4 / Jackson 3. Die Standardmeldung war «Failed to read request»; `GlobalExceptionHandler.handleHttpMessageNotReadable` meldet jetzt «Unbekanntes Feld: <name>». Rückwirkung auf noch nicht umgestellte Ressourcen: keine, die Property konnte direkt global gesetzt werden.
- **E8 bestätigt:** OSIV ließ sich nach der letzten Ressource ohne weitere Anpassung abschalten; `LazyInitializationException` trat nicht auf.

**Abweichungen vom Plan:**
- **Jackson 3 und primitive Booleans:** Ein fehlendes primitives `boolean` in einem Record-Request scheitert unter Jackson 3 mit einer unspezifischen 400, noch bevor unbekannte Felder gemeldet werden. `ParteiRequest.twintAktiv` sowie `bestaetigungVersendet` in den Einladungs-Requests sind deshalb `@NotNull Boolean` (sauberer Validierungsfehler, Pflichtfeld im Schema). Aufgefallen durch TC-053.
- **Fehler-Schema:** `ApiError` hängt als eine `4XX`-Antwort an allen Operationen, nicht als fünf einzelne Codes. Welche Codes konkret auftreten, dokumentiert das Testdesign; die Spec bleibt kompakter.
- **Commits:** fünf Ressourcen-Commits statt elf (Querschnitt + Person/Event/Konsumationsangebot; Allgemeinausgabe/Partei/Benutzer; Einladung; Teilnahme; Konsumation/Abrechnung/Zahlung/Mahnung), jeder mit grünem `./mvnw verify`.
- **Zusätzlich:** `GET /api/benutzer` mit Fetch-Join auf die Partei; unbekannte `personenIds` einer Partei → 400 statt stillem Verwerfen (E5 konsequent angewendet); `/api/auth/login` liefert ebenfalls `application/json`.
- **Testnummern:** TC-048 (Einladung-Rückmeldung per PUT), TC-049 bis TC-051 (PUT Konsumationsangebot, Allgemeinausgabe, Konsumation), TC-052 (PUT → 404, acht Endpunkte), TC-053 (POST mit `id` → 400, zwölf Ressourcen), TC-054 (Query-Anzahl). Der Einladungs-PUT-Happy-Path steckt in TC-013 und TC-048; ein separater Test für den Abrechnungs-PUT war neben TC-032 nicht nötig.

**Messwerte TC-054** (lokale Dev-DB mit Testdaten, vor A5 → nach A5):

| Endpunkt | Statements vorher | nachher |
|---|---|---|
| `GET /api/einladungen` | 35 | ≤ 2 |
| `GET /api/teilnahmen` | 19 | ≤ 2 |
| `GET /api/abrechnungen` | 16 | ≤ 2 |
| `GET /api/zahlungen` | 13 | ≤ 2 |

**Frontend:**
- Alle `*.model.ts` sind Aliase; die Komponenten-Templates brauchten keine Pfadänderung (E3 hat gehalten). Geändert haben sich nur die Payload-Aufbauten (flache IDs) und die Update-Aufrufe in fünf Komponenten.
- **Neue E2E-Tests** für das Bearbeiten in UC-007, UC-008 und UC-010 — vorher gab es für diese Wege weder IT noch E2E.
- **Befund:** Die E2E-Dateien werden von keiner CI-Stufe typgeprüft (Playwright transpiliert nur). Die Hilfen nutzen jetzt `satisfies XxxRequest`, das greift aber erst mit einem `tsc`-Schritt über `e2e/`. Ein lokaler Lauf zeigt einen bestehenden Typfehler in `e2e/specs/UC-005_Teilnahme-Verwalten.spec.ts:108`. Als Beifang bei QUAL-001 in `specs/TODO.md` erfasst.
- **Möglicher Folgeschritt:** `abrechnungen-verwaltung` lädt weiterhin alle Parteien nur für `twintAktiv`; seit Stufe 2 steckt das Feld bereits in `teilnahme.einladung.partei`. Bewusst nicht mitgeändert, gehört zu BIZ-001, wenn die Berechnung ins Backend wandert.


## Aufwand (Schätzung)

| Teil | Aufwand |
|---|---|
| A0 Spike | 1 h |
| A1 Querschnitt, A2 CODE-002 | 1–2 h |
| A3 Records, Services, Controller, Transaktionen (11 Ressourcen) | 6–7 h |
| A4 fünf PUT-Endpunkte | 2–3 h |
| A5 Lazy Loading, OSIV, Fetch-Joins | 1–2 h |
| A6 Controller-Tests, ITs, neue TCs | 6–7 h (grösster Posten) |
| A7–A9 Spec-Kontrolle, Doku, Verifikation | 2 h |
| B1–B3 Typen, Modelle, Payloads, PUT-Umstellung in fünf Komponenten | 4–5 h |
| B4–B6 Tests, E2E-Helfer, Doku, Verifikation | 2–3 h |
| **Total** | **ca. 25–30 h** |

Grösste Unsicherheiten:
- **E7** (Pflichtfelder in Response-Schemas). Scheitert der Customizer im Spike, fällt das Frontend auf einen dünnen `Required<>`-Helper zurück; der Rest des Plans bleibt gültig.
- **A5** (Lazy Loading). Vergessene Transaktionen oder Fetch-Joins zeigen sich erst hier. Der Schritt ist bewusst klein und am Ende, damit die Ursache eindeutig ist.

## Revision (2026-09-25, nach kritischer Prüfung)

Gegenüber der ersten Fassung desselben Tages:

1. **PUT für Konsumation, Konsumationsangebot und Allgemeinausgabe ergänzt** (Ziel 4, A4, B3). Das Frontend aktualisiert diese drei Ressourcen per POST mit `id`; mit E6 wären diese Bearbeitungen gescheitert.
2. **Lazy Loading und OSIV an den Schluss verschoben** (E9, neuer Schritt A5). In der ersten Fassung standen beide vor der Umstellung der Ressourcen; mangels Jackson-Modul für Hibernate hätte das Serialisierungsfehler ausgelöst.
3. **Transaktionen für alle Service-Methoden und Repository-Abfrage für `TeilnahmeZugriff`** (E8). Ohne OSIV wären das Teilnahme-Update und die Eigentümerprüfung für PARTEI-Benutzer gescheitert.
4. **Query-Anzahl-Test ohne eigenes Testprofil** (A6). Ein zweiter Kontext hätte mit dem festen Port der dev-ITs kollidiert.
5. **TC-040 um die neuen PUTs erweitert, betroffene TCs konkret benannt** (TC-012, TC-023), `@Valid` auf Listen in Request-Records, Aufwand von 20–25 h auf 25–30 h.

## Anhang – Nutzungsanalyse Frontend (2026-09-25)

Verschachtelte Pfade, die Templates und Komponenten heute lesen (ohne Specs). Grundlage für E3 und die `Kurz`-Records.

| Pfad | Stellen | Wird geliefert von |
|---|---|---|
| `…event.id` | 12 | `EventResponse` |
| `einladung.partei.bezeichnung` | 9 | `ParteiKurz` |
| `abrechnung.id` | 8 | `AbrechnungKurz` |
| `partei.bezeichnung` | 7 | `ParteiKurz` |
| `teilnahme.einladung.partei.bezeichnung` | 5 | `TeilnahmeKurz` → `EinladungKurz` → `ParteiKurz` |
| `einladung.event.id`, `teilnahme.einladung.event.id` | 9 | `EinladungKurz.event` |
| `teilnahme.einladung.partei.twintMobilenummer`, `.adresse` | 3 | `ParteiKurz` |
| `teilnahme.einladung.anzahlPersonen` | 1 | `EinladungKurz` |
| `einladung.event.datum`, `.standort` (UC-016) | 2 | `EinladungKurz.event` |
| `konsumationsangebot.preis` | 1 | `KonsumationsangebotKurz` |
| `partei.personen` | 2 | nur `ParteiResponse` (Parteien-Verwaltung) |
