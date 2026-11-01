# Turma da Mônica Rank

A fan-made popularity ranking for Turma da Mônica characters. Fans confirm an
email account, build one ordered top-10 ballot, and every month the site freezes
the standing into a snapshot that becomes part of a browsable history.

This is a fan project, not affiliated with Mauricio de Sousa Produções.

The interface is in Brazilian Portuguese. Code, docs and commits are in English.

## Status

The account loop is implemented and working against a local Postgres and
Mailpit: register, confirm e-mail, log in, refresh, reset password and log out.
The ranking, ballots and snapshots are still specified but not built; the next
slice is tracked in [the roadmap](docs/roadmap.md). Nothing is deployed yet.

## Stack

| Layer | Technology |
| --- | --- |
| API | Java 25, Spring Boot 4.1.2, Spring Security (JWT resource server), Spring Data JPA, Flyway |
| Database | PostgreSQL (Testcontainers for tests) |
| Client | Vue 3.5, TypeScript 6, Vite 8, Bun, Tailwind CSS v4, Vue Router 5, Pinia 4 |
| Tooling | Maven Wrapper 3.10.0, Bun 1.4, Docker Compose |

## Requirements

- JDK 25
- Docker (Postgres, Mailpit and Testcontainers)
- Bun 1.4 (the client lockfile is `bun.lock`; do not use npm)

## Quick start

```bash
# 1. Postgres + Mailpit
docker compose -f api/compose.yaml up -d

# 2. API — the local profile uses the dev mail/cookie settings
cd api
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run

# 3. SPA — in another terminal
cd client
bun install
bun run dev
```

The SPA runs at http://localhost:5173 and proxies `/api` to the API at
http://localhost:8080. Health check:

```bash
curl -s http://localhost:8080/actuator/health
```

Mailpit captures confirmation and reset e-mails at http://localhost:8025.
Register at http://localhost:5173/criar-conta, open the link from Mailpit and
then log in.

Client checks:

```bash
cd client
bun run lint
bun run fmt:check
bun run build
```

## Documentation

| Document | Contents |
| --- | --- |
| [docs/overview.md](docs/overview.md) | Product, glossary and the normative ranking rules |
| [docs/requirements.md](docs/requirements.md) | Functional and non-functional requirements with IDs |
| [docs/architecture.md](docs/architecture.md) | Modules, auth flows, the monthly count, configuration, testing |
| [docs/data-model.md](docs/data-model.md) | Schema, constraints, indexes, migrations and seed contract |
| [docs/api.md](docs/api.md) | REST contract, payloads and error codes |
| [docs/ui.md](docs/ui.md) | Visual direction, routes, components, accessibility and copy |
| [docs/decisions.md](docs/decisions.md) | Accepted ADRs and open questions |
| [docs/roadmap.md](docs/roadmap.md) | Milestones, gaps and the character-catalog plan |

Agents and contributors: read [`AGENTS.md`](AGENTS.md) before changing code.

## Repository layout

```text
api/      Spring Boot application (Maven)
client/   Vue single-page application (Vite, Bun)
docs/     Project specifications
```

## License

[GNU Affero General Public License v3.0](LICENSE).
