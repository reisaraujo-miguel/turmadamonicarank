# AGENTS.md

Guidance for AI agents and humans working in this repository. Read this file
first, every session. It is the map, not the manual.

Turma da Mônica Rank is a fan-made popularity ranking: confirmed users build one
ordered top-10 ballot, the live rank is public, and every month a snapshot
freezes the standing. Vue 3 SPA + Spring Boot API + PostgreSQL, served from one
origin. Interface copy is pt-BR; code, docs, commits and identifiers are English.

## Read the right spec before you touch anything

| If the task touches… | Read first | What it decides |
| --- | --- | --- |
| voting, points, order, periods, cutoffs | [docs/overview.md](docs/overview.md) | The normative domain rules |
| an endpoint, payload, status, error code, client fetch | [docs/api.md](docs/api.md) | The HTTP contract |
| a migration, entity, repository, index | [docs/data-model.md](docs/data-model.md) | Tables, constraints, query patterns |
| auth, sessions, mail, the count job, packages, config, tests | [docs/architecture.md](docs/architecture.md) | Module boundaries and mechanics |
| a page, component, token, motion, copy | [docs/ui.md](docs/ui.md) | Routes, visual system, accessibility |
| scope, requirement IDs, acceptance criteria | [docs/requirements.md](docs/requirements.md) | What "done" means per feature |
| changing a settled choice | [docs/decisions.md](docs/decisions.md) | ADRs and open questions |
| what to build next | [docs/roadmap.md](docs/roadmap.md) | Milestones and current gaps |

If a task spans several rows, read each file. The docs are the contract; the
code follows them.

## Repository map

```text
AGENTS.md            this file
README.md            human quickstart
docs/                specifications; update them in the same change as code
.github/workflows/   CI checks for API and client
api/                 Spring Boot 4.1.2 / Java 25 / Maven Wrapper
  src/main/java/rocks/leug/turmadamonicarank/   feature packages
  src/main/resources/db/migration/              Flyway migrations
client/              Vue 3.5 / Vite 8 / TypeScript 6 / Tailwind v4 / Bun
  src/router/        routes and guards
  src/stores/        auth session state
  src/pages/         route components
  src/components/    shell and auth UI
  src/lib/            HTTP, errors, formatting and API types
```

The API and client are separate build tools. Run commands from each directory.

## Commands

Prerequisites: JDK 25, Docker (Postgres, Mailpit and tests), Bun 1.4. Use
`bun`, never `npm` — the lockfile is `bun.lock`.

| Goal | Command |
| --- | --- |
| Run the API (starts Compose Postgres when Docker is up) | `cd api && SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run` |
| API health | `curl -s localhost:8080/actuator/health` |
| API tests | `cd api && ./mvnw test` |
| API full verification | `cd api && ./mvnw verify` |
| One API test class | `cd api && ./mvnw test -Dtest=BallotServiceTest` |
| Run the SPA | `cd client && bun install && bun run dev` → http://localhost:5173 |
| Build/type-check the SPA | `cd client && bun run build` |
| Client tests | `cd client && bun run test` |
| Lint / format check the SPA | `cd client && bun run lint && bun run fmt:check` |
| Format the SPA | `cd client && bun run fmt` |
| Mailpit inbox | http://localhost:8025 |
| Postgres shell | `docker compose -f api/compose.yaml exec postgres psql -U myuser -d mydatabase` |

The Vite dev server proxies `/api` to `localhost:8080`. There is no CORS setup
and there must not be one.

## Non-negotiables

1. **Flyway owns the schema.** New column or table means a new migration;
   applied migrations are never edited. Hibernate runs `ddl-auto: validate`.
2. **One ballot per user.** All ballot writes go through `BallotService`, which
   replaces the ballot atomically, validates 1–10 distinct active characters,
   checks email confirmation, and stamps `updated_at` from the injected `Clock`.
3. **Snapshots are immutable.** Only `SnapshotGenerator` writes them; it is
   idempotent per period and counts only ballots with `updated_at <= cutoff`.
   No update or delete path for snapshot rows.
4. **Rank math lives in `RankCalculator`.** Points, ordering, active filtering
   and tie-breaks exist once, server-side. The client never computes ranks.
5. **Time is centralized.** Store UTC; compute periods and cutoffs with
   `SnapshotPeriods` in `America/Sao_Paulo`. Services take `Clock`; no
   `Instant.now()`, `LocalDate.now()` or `new Date()` inside them.
6. **Authorization state comes from the database.** The JWT identifies the user;
   email-confirmation checks load the user. Never trust claims for writes.
7. **Credentials stay out of JavaScript-readable storage.** Access token in
   memory, refresh token in an `HttpOnly` cookie, no CORS.
8. **API changes are contract changes.** `/api/v1`, problem+json with a stable
   `code`, unknown request fields rejected. Update `docs/api.md` and the client
   types in the same change.
9. **Language split.** English in code, docs, commits and API fields; pt-BR in
   every user-facing string, email and date. Format dates with `Intl` in
   `America/Sao_Paulo`.
10. **Characters are seed-owned.** Inactive instead of deleted; history is
    denormalized in snapshot entries so edits never rewrite the past.
11. **No secrets in the repository.** Configuration through environment
    variables; the local JWT secret is dev-only.
12. **Annual snapshots are frozen too.** Only `YearSnapshotGenerator` writes
    `year_snapshots`/`year_snapshot_entries`, only for closed years with monthly
    data, and only from already-frozen monthly snapshots. The current year is
    served as `IN_PROGRESS`; closed years are immutable.

## How to make a change

1. Find the requirement ID in [docs/requirements.md](docs/requirements.md) and
   the milestone in [docs/roadmap.md](docs/roadmap.md). If neither exists, stop
   and ask — do not invent scope.
2. Read the files the task touches from the table above. Note the invariants.
3. Schema change? Add the next `Vn__…sql`, then update
   [docs/data-model.md](docs/data-model.md). Done when the migration applies on
   a fresh database and the doc matches.
4. Backend change? Implement in the owning feature package: repository query,
   service rule, DTO record, controller. Done when no entity leaves the HTTP
   boundary and the service owns the transaction.
5. Test at the right layer: pure unit for rules, `@WebMvcTest` for contract and
   authorization, Testcontainers integration for SQL, migrations and the count.
   Done when the success path, every documented error code and the 401/403 cases
   are asserted.
6. Client change? Types in `lib/types.ts`, call through `lib/http.ts`, state in
   Pinia, page in `pages/`. Done when loading, empty and error-with-retry states
   exist, keyboard works, and 360 px is usable.
7. Update the docs that describe the behaviour. Done when the endpoint, table,
   route or rule appears in exactly one authoritative place.
8. Run `cd api && ./mvnw verify`, `cd client && bun run test` and
   `cd client && bun run build`. All must pass; fix failures instead of skipping
   checks.
9. Tick the roadmap task. Commit in Conventional Commits style, scoped, with the
   requirement ID: `feat(ballot): replace ballot atomically (FR-VOTE-04)`.

## Definition of done

**Every change**

- [ ] The relevant doc is updated in the same commit; no duplicated rules.
- [ ] No new runtime dependency without an ADR entry.
- [ ] No secret, token, password or personal data in code, logs or fixtures.
- [ ] Formatting and build commands pass; generated output is not committed.

**Backend**

- [ ] Migration added for every schema change; no edits to applied migrations.
- [ ] Ballot/rank/snapshot invariants from the list above still hold.
- [ ] New endpoint is in `docs/api.md` and has contract + authorization tests.
- [ ] Errors use the advice and documented codes; no stack traces leak.
- [ ] Time comes from `Clock`; new period logic is tested across month/year ends.
- [ ] Query is paginated in SQL and indexed; no N+1.

**Client**

- [ ] New route is in `docs/ui.md`, lazy-loaded, titled in pt-BR, guarded as
      documented.
- [ ] States covered: loading skeleton, empty, error with retry, not found.
- [ ] Keyboard-only path works; focus visible; chart has a table alternative.
- [ ] Only design tokens are used; no raw hex values or soft shadows.
- [ ] No rank, point or cutoff computation in the client.
- [ ] `bun run test` and `bun run build` pass.

**Data**

- [ ] Seed changes are idempotent by slug and carry credits/sources.
- [ ] No user, ballot or snapshot data is added to migrations or seeds.

## Conventions

- Java 25, records for DTOs and value objects, feature packages, constructor
  injection, no Lombok (not on the classpath). Use `JdbcClient` for read
  projections, Spring Data for entity access. Jakarta Validation at the edge.
- Client: `<script setup lang="ts">`, Composition API, Pinia for shared state,
  colocated small components, 2-space indent. Tailwind utilities + tokens; no
  component library. 4-space indent and 120 columns in Java.
- Conventional Commits, small focused commits, one vertical slice at a time.
- Tests are named for behaviour, not implementation (`rejectsDuplicateCharacter`),
  and use the fixed test clock rather than sleeps.
- `.editorconfig` owns indentation and line endings. GitHub Actions
  (`.github/workflows/ci.yml`) runs the API and client checks on every pull
  request.

## Gotchas

- Spring Boot 4 / Spring Security 7 APIs differ from most tutorial code.
  Verify against the resolved version (`./mvnw dependency:tree`) before writing
  from memory.
- Spring Boot 4 uses Jackson 3: import `tools.jackson.databind.ObjectMapper`, not
  `com.fasterxml.jackson.databind`. `MimeMessageHelper` needs its multipart
  constructor to send both text and HTML.
- Boot 4 moved `@AutoConfigureMockMvc` to
  `org.springframework.boot.webmvc.test.autoconfigure`.
- Testcontainers needs a running Docker daemon. Integration tests run on
  Postgres only — no H2, ever. Mail tests use the recording `EmailSender`; a real
  mail send is exercised through Mailpit in local development.
- `unaccent()` is not immutable, so search indexes need the
  `immutable_unaccent(text)` wrapper from the first migration.
- A ballot edited after a missed cutoff cannot be reconstructed during backfill;
  the snapshot excludes it. This is documented, not a bug.
- Never enable `POST /dev/snapshots` outside the `local` profile.
- Do not treat the current-year year page as frozen; only `FROZEN` responses are
  immutable. The `IN_PROGRESS` rollup may change at the next monthly count.
- Never store the access token in `localStorage`/`sessionStorage`; a reload is
  supposed to refresh from the cookie.
- Never add CORS configuration to "fix" a local call; the Vite proxy is the fix.
- Never display server `title`/`detail` in the UI; map `code` to pt-BR copy.

## When docs and code disagree

The docs are the intended contract. If the code is right, update the doc in the
same change. If the change alters a settled choice (ADR), add a superseding ADR
and update every affected spec. If the choice is not recorded anywhere, propose
an ADR with options and ask a human before implementing it. Silent divergence is
the one thing that makes this project unmaintainable.
