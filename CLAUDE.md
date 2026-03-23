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

The only domain currently implemented is `person` (`/api/persons` — GET all, POST create, DELETE by id).

New domains follow the same package-per-domain structure: `ch.quartierfest.backend.<domain>/`.

## Key Libraries

- **Lombok** — used for `@Data`, `@RequiredArgsConstructor`; avoid writing boilerplate manually
- **Spring Data JPA** — repositories extend `JpaRepository`
- **Spring WebMVC** — not WebFlux; controllers are synchronous
