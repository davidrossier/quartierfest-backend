# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./mvnw clean install

# Run application
./mvnw spring-boot:run

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=BackendApplicationTests

# Run a single test method
./mvnw test -Dtest=BackendApplicationTests#contextLoads
```

## Database

PostgreSQL is required. The app connects to:
- Host: `localhost:5432`
- Database: `quartierfest`
- Username: `qfuser` / Password: `qfpass`

Schema is auto-managed via `spring.jpa.hibernate.ddl-auto=update`.

## Architecture

Spring Boot 4 / Java 21 REST backend. Code is organized by domain under `ch.quartierfest.backend`. Each domain follows a four-layer pattern:

- **Entity** (`@Entity`, Lombok `@Data`) — JPA-mapped table
- **Repository** (`JpaRepository<Entity, Long>`) — data access
- **Service** (`@Service`, `@RequiredArgsConstructor`) — business logic, calls repository
- **Controller** (`@RestController`, `@RequestMapping("/api/...")`) — HTTP endpoints, calls service

All domains are implemented. Each follows the same package-per-domain structure `ch.quartierfest.backend.<domain>/` with 4 files: Entity, Repository, Service, Controller.

| Domain | Endpoint | Beziehungen |
|---|---|---|
| `person` | `/api/persons` | — |
| `partei` | `/api/parteien` | `@OneToMany` → Person |
| `event` | `/api/events` | — |
| `einladung` | `/api/einladungen` | `@ManyToOne` → Event, Partei |
| `teilnahme` | `/api/teilnahmen` | `@OneToOne` → Einladung |
| `konsumationsangebot` | `/api/konsumationsangebote` | `@ManyToOne` → Event |
| `konsumation` | `/api/konsumationen` | `@ManyToOne` → Teilnahme, Konsumationsangebot |
| `allgemeinausgabe` | `/api/allgemeinausgaben` | `@ManyToOne` → Event |
| `abrechnung` | `/api/abrechnungen` | `@OneToOne` → Teilnahme |
| `zahlung` | `/api/zahlungen` | `@ManyToOne` → Abrechnung |
| `mahnung` | `/api/mahnungen` | `@ManyToOne` → Abrechnung |

Enums sind als innere Klassen in der jeweiligen Entity definiert (`Einladung.EinladungStatus`, `Einladung.BuffetBeitrag`, `Abrechnung.Zustellungskanal`, `Zahlung.Zahlungskanal`).

Die Spezifikationen liegen unter `specs/`: `use-cases.md`, `datamodel.md`, `architecture.md`.

## Key Libraries

- **Lombok** — used for `@Data`, `@RequiredArgsConstructor`; avoid writing boilerplate manually
- **Spring Data JPA** — repositories extend `JpaRepository`
- **Spring WebMVC** — not WebFlux; controllers are synchronous
