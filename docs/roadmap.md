# Roadmap

Last reviewed: 2026-11-01 · Status: draft, authoritative for sequencing

This is the working plan. Pick the first unchecked task in the earliest
incomplete milestone; do not start a later milestone's work just because it is
more interesting. Update this file when a task lands or the order changes.

## Where the code is today

| Area | Exists | Missing (before it is usable) |
| --- | --- | --- |
| Docs | This `docs/` suite, `AGENTS.md`, `README.md` | — |
| API | Spring Boot 4.1.2 on Java 25; Flyway auth schema; JWT/refresh session service; registration, confirmation, reset; Mailpit; problem+json; integration tests; period helpers | Ranking, ballots, character catalog, snapshots, annual snapshots, rate limiting, SPA packaging |
| Client | Vue 3.5 + Vite 8 + TS 6 + Tailwind v4 + Router 5 + Pinia 4; shell; HTTP/refresh client; full auth flow; Vitest coverage; formatter, linter, CI | Rank/character/vote pages, ballot editor, history chart, production build integration |
| Data | `users`, `auth_tokens`, `refresh_tokens` | Characters, ballots, monthly and annual snapshots, seed catalog |
| Delivery | Local dev, Docker Compose, GitHub Actions CI | Production config, deploy, account deletion/export |

The sample `/hello` endpoint is gone and the main class is thin.

## Milestone M0 — Foundations

Goal: a clean, buildable base that every later milestone can lean on.

- [x] Client: add `vue-router`, `pinia`, `tailwindcss` and
      `@tailwindcss/vite`; configure the Vite plugin, proxy `/api` to
      `localhost:8080`, and declare the design tokens from [ui.md](ui.md) in
      `style.css`.
- [x] Client: replace the demo page with the app shell (header, footer, skip
      link, `<RouterView>`), pages for every route in
      [ui.md](ui.md#routes-and-guards), and `lang="pt-BR"` + title in
      `index.html`.
- [x] Client: `lib/http.ts`, `lib/errors.ts`, `lib/format.ts` and `stores/auth.ts`.
- [x] Client: add `vitest`, `@vue/test-utils` and `jsdom`; tests cover error
      mapping, formatting, HTTP retry and refresh single-flight.
- [x] API: delete the sample endpoint; add `application-local.yaml`; add
      Mailpit to `api/compose.yaml`; pin the Postgres image (in Compose and
      `TestcontainersConfiguration`) and map `5432:5432`; configure
      `ddl-auto: validate`, `open-in-view: false`, UTC.
- [x] API: `Clock` bean.
- [x] API: `SnapshotPeriods` with unit tests for cutoffs, current period, next
      cutoff and year boundaries in `America/Sao_Paulo`.
- [x] API: problem+json advice and the `ApiErrorCode` enum.
- [x] Add `.editorconfig`; the client uses `oxfmt` and `oxlint`
      (`bun run fmt`, `bun run fmt:check`, `bun run lint`).
- [x] CI: GitHub Actions runs API `./mvnw -B verify` (JDK 25 + Docker) and the
      client lint, format, test and build checks.

**Exit criteria:** `bun run build`, `bun run test` and `./mvnw verify` pass from
a clean clone; the shell renders every route; the API serves only the auth
routes, `/actuator/health` and problem+json 404s; CI is green. Done
(2026-11-01).

## Milestone M1 — Accounts and sessions

Goal: a person can create an account, confirm it by email, log in, refresh,
reset a password and log out.

- [x] Migrations `V1__extensions_and_helpers.sql` and
      `V2__users_and_auth.sql` per [data-model.md](data-model.md).
- [x] `user` module: entity, repository, profile query.
- [x] `mail` module: `EmailSender` seam, SMTP adapter, recording adapter,
      confirmation and reset templates (plain text + simple HTML).
- [x] `auth` module: token issue/consume, registration, confirmation, resend,
      login, refresh rotation with reuse detection, logout, forgot/reset.
- [x] `security`: stateless chain, HS256 decoder, current-user resolution,
      `Origin` check on cookie endpoints, security headers, method rules.
- [x] Endpoints in [api.md](api.md#authentication-endpoints) with contract
      tests, including the authorization matrix and generic responses.
- [x] Dev tooling: Mailpit in the Compose stack; links point at the Vite origin
      and the full flow was exercised against Mailpit.
- [x] Client: auth store, boot refresh, guards, login/register/confirm/forgot/
      reset/account pages, error-code copy.
- [x] Cleanup job for expired auth and refresh tokens.

**Exit criteria:** a new person can register, receive the link in Mailpit,
confirm, log in, reload the SPA without logging in again, reset the password and
log out; the main documented error paths have a test; no tokens are stored in
browser storage. Done (2026-11-01); rate limiting stays in M5.

## Milestone M2 — Characters, ballots and the live rank

Goal: the core loop works end to end.

- [ ] `V3__characters.sql` + `R__seed_characters.sql` with the five sample
      characters and generated placeholders for images.
- [ ] `character` module: search (accent- and typo-tolerant), detail, aliases.
- [ ] `V4__ballots.sql`, `ballot` module: read, atomic replace, withdraw,
      confirmation gate, inactive-character rejection.
- [ ] `rank` module: `RankCalculator` shared SQL, live endpoint with pagination,
      cache header, `nextCutoffAt` and latest-snapshot summary.
- [ ] Tests: ballot validation, atomicity, points ordering, tie-breaks,
      inactive filtering, rank contract, search accents.
- [ ] Client: character list/detail placeholders, search combobox, ballot
      editor with keyboard reorder and dirty-state guard, live rank page.
      History chart lands with M3 because there is no history yet.

**Exit criteria:** a confirmed user can search a character, build and save a
top-10, edit it, withdraw it, and see the live ranking change accordingly;
unconfirmed users cannot write; search finds "monica" for "Mônica".

## Milestone M3 — Monthly count and snapshots

Goal: history becomes real and immutable.

- [ ] `V5__snapshots.sql` per [data-model.md](data-model.md).
- [ ] `V6__year_snapshots.sql` and `YearSnapshotGenerator`: closed years only,
      idempotent, immutable, built from monthly snapshots; tests for frozen,
      in-progress and empty years.
- [ ] `SnapshotGenerator`: advisory lock, idempotency, cutoff filtering,
      previous ranks, single transaction; unit and integration tests
      (empty month, first snapshot, movement, repeat run, cutoff boundary).
- [ ] Scheduler at `0 5 0 1 * *` America/Sao_Paulo; startup backfill and daily
      catch-up from the earliest ballot month, oldest first; failures logged
      without stopping reads; scheduler and backfill disabled in tests.
- [ ] Local-only `POST /dev/snapshots` guarded by profile and contract-tested.
- [ ] Snapshot endpoints: list, latest, month, frozen/provisional/empty year
      view, and character history series with null gaps; contract tests for each.
- [ ] Client: snapshot and year pages, movement badges, history SVG chart with
      table fallback, period chips and empty months.
- [ ] Manual verification: run the dev endpoint for a closed period, compare
      values against a hand-computed fixture.

**Exit criteria:** with ballots pinned before and after a cutoff, the snapshot
contains exactly the pre-cutoff population; the API serves month, year and
history data; editing a ballot after the count leaves the snapshot unchanged.

## Milestone M4 — Full client experience

Goal: the product feels finished and accessible.

- [ ] Complete every page in [ui.md](ui.md): home podium, live rank, snapshot,
      year, characters, character, vote success, account, about, 404.
- [ ] Loading skeletons, empty states, error-with-retry, offline/network error
      message, toasts and confirm dialogs.
- [ ] Accessibility pass: keyboard-only walkthrough of every flow, visible
      focus, screen-reader announcements, reduced motion, 200% zoom, contrast
      check.
- [ ] Responsive pass at 360, 768, 1024 and 1440.
- [ ] Client tests: http refresh single-flight, error mapping, ballot draft
      reducer/reorder, formatting.
- [ ] Copy review in pt-BR (tone, consistency, legal disclaimer).

**Exit criteria:** all acceptance scenarios in
[requirements.md](requirements.md#acceptance-scenarios) pass in a browser;
keyboard-only users can vote; Lighthouse accessibility ≥ 95 on home and rank.

## Milestone M5 — Hardening and launch

Goal: safe to publish.

- [ ] Rate limiting on auth endpoints (IP, plus email for login/resend/forgot).
- [ ] Security review: headers, cookie flags, JWT config, Origin check, error
      leakage, dependency and secret scanning.
- [ ] `ui` Maven profile packaging verified; SPA fallback keeps API 404s.
- [ ] Production config: env vars, pinned Postgres, TLS/forwarded headers,
      backup and restore runbook, health checks.
- [ ] Structured logs with request IDs; no PII in logs.
- [ ] LGPD launch blockers: account deletion and data export (FR-ACC-01/02,
      ADR-017).
- [ ] CI gates: API tests, client build, type-check, format check; deploy
      workflow or documented release steps.
- [ ] Seed content review, `/sobre` credits and takedown contact live.

**Exit criteria:** one artifact deploys with a single URL; a security checklist
passes; deleting an account removes personal data while snapshots remain; CI is
green on every pull request.

## Milestone M6 — Character catalog

Goal: the ranking is interesting because it has a real cast.

The hard part, deliberately postponed until the machinery works.

- [ ] Scope decision: which series/turmas are eligible. Record it as an ADR.
- [ ] Target list of 100–150 characters with canonical name, slug, aliases,
      first appearance, short bio in our own words, and a source link.
- [ ] Image rights process (ADR-016): self-host only cleared files under
      `client/public/images/characters/`, with credits, alt text and a fallback
      placeholder.
- [ ] `tools/characters/` validator and generator: CSV → validated, idempotent
      seed SQL. Checks: unique ASCII slug, required fields, bio length, credit
      present when image present, no duplicate aliases.
- [ ] Import the catalog, review pt-BR copy, verify search across accents and
      common misspellings. Define a duplicate/merge policy before importing;
      snapshots keep their frozen rows and never follow a merge.
- [ ] Update `/sobre` credits; confirm no copied text or unlicensed media.

**Exit criteria:** the catalog validates in CI, every character page renders,
search finds any character by name or common alias from three typed characters,
and the rights/credits record is complete.

## Suggested first vertical slices

Do not build the whole backend before touching the client. The contract in
[api.md](api.md) lets both sides advance in one slice:

1. M0 shell + health + docs.
2. "Register → confirm → login" through Mailpit, with the auth pages styled.
3. "Search character → save ballot → see rank".
4. "Close a month → open the snapshot → see history".

Each slice means: migration, module, contract test, client page, manual
walkthrough, docs update.

## Known gaps to keep visible

- Ranking, ballots, character data, monthly snapshots and annual snapshots are
  not implemented.
- The client still has placeholder ranking, character and vote pages.
- Account deletion and export are launch requirements but not built.
- Rate limiting and production deployment are not built.
- Character content and image rights are unresolved; images stay placeholders
  until cleared.
