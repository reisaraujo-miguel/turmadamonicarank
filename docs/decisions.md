# Decisions and open questions

Last reviewed: 2026-11-01

This is the durable memory of the project. Write down *why* a choice was made so
the next agent does not relitigate it by accident. Do not rewrite accepted
decisions; supersede them with a new one and mark the old one as superseded.

## Accepted decisions

### ADR-001 — Cumulative ballots with monthly snapshots

**Date:** 2026-11-01 · **Status:** accepted

**Context:** Votes could reset monthly (an election every month) or persist
while snapshots record each month's standing. The product is about watching
popularity move, not electing a monthly champion.

**Decision:** One persistent ballot per user, editable at any time. Each month's
snapshot freezes the ranking computed from all ballots whose last edit was at or
before the cutoff.

**Consequences:** Users vote once and only update when they change their mind.
Month-to-month movement reflects changes in the whole electorate, not a fresh
campaign. Ballot history is not stored, so a missed cutoff cannot be
reconstructed exactly (see OQ-004). The live rank is the same computation without
a cutoff.

### ADR-002 — Positional points 10→1 with a fixed tie-break chain

**Date:** 2026-11-01 · **Status:** accepted

**Context:** Ordered top-10 ballots carry more information than a single
favourite. The rule must be explainable on the about page and deterministic for
snapshots.

**Decision:** Position `p` earns `11 - p` points. Order by points, then
first-place votes, then ballot count, then character slug. Positions are ordinal;
ties are never displayed as equal.

**Consequences:** A #1 pick is worth 10 points and a #10 pick 1 point. Short
ballots award fewer total points but their #1 is still worth the maximum. The
slug fallback exists only to make output stable, and it is effectively invisible.

### ADR-003 — First-party JWT with a rotating HttpOnly refresh cookie

**Date:** 2026-11-01 · **Status:** accepted

**Context:** The client is a same-origin SPA; the API already depends on the
OAuth2 resource server starter. Options were server sessions, first-party JWTs,
or social login.

**Decision:** The API issues 10-minute HS256 access tokens in the response body
and a 30-day opaque refresh token in an `HttpOnly`, `SameSite=Lax` cookie scoped
to `/api/v1/auth`. Refresh rotates; reuse revokes the family. Password reset
revokes all sessions.

**Consequences:** The API stays stateless and horizontal scaling does not need a
session store. The refresh token is stateful in Postgres and must be cleaned up.
The access token lives in memory, so a page reload always performs one refresh.
CSRF exposure is limited by `SameSite` plus an `Origin` check on cookie
endpoints. Concurrent refreshes from two tabs can race and revoke the family
(OQ-011).

### ADR-004 — Single origin; the API serves the built SPA

**Date:** 2026-11-01 · **Status:** accepted

**Context:** Two deployables create CORS, cookie-domain and release-ordering
work that a project this size does not need.

**Decision:** Build the SPA, copy `client/dist` into the jar with the `ui` Maven
profile, and serve `index.html` as a fallback for non-API routes. No CORS
configuration exists.

**Consequences:** One artifact, one URL, relative API paths, no cookie-domain
configuration. The client build becomes part of the release pipeline (CI builds
it before `./mvnw -Pui package`). The fallback controller must not intercept
`/api/**`, `/actuator/**` or asset paths.

### ADR-005 — Seed-owned characters, scheduled count, local-only trigger

**Date:** 2026-11-01 · **Status:** accepted

**Context:** Character data collection is the hardest open task and should not
block the voting loop. Admin tooling is out of scope for now.

**Decision:** Characters and bios ship as an idempotent repeatable Flyway seed.
The count runs on a schedule plus startup/daily backfill. A development-only
endpoint generates a snapshot on demand in the `local` profile; there is no
production admin API.

**Consequences:** No write path for character data means the seed is the source
of truth until an importer exists (M6). Fixing a character means editing the
seed and restarting. The dev endpoint is guarded by profile and contract-tested
to 404 outside `local`.

### ADR-006 — Tailwind v4, Vue Router, Pinia and a hand-written HTTP client

**Date:** 2026-11-01 · **Status:** accepted

**Context:** The scaffold has Vue + Vite + TypeScript only. The client needs
routing, session state and a Tailwind setup.

**Decision:** Add `tailwindcss` + `@tailwindcss/vite` (v4, CSS-first tokens),
`vue-router`, `pinia`, and a small typed `fetch` wrapper. No UI kit, no axios,
no chart library, no component framework.

**Consequences:** Design tokens live in `@theme` in `style.css`, so components
never hardcode colours. The HTTP client owns refresh single-flight and error
normalization, which keeps stores and pages simple. Charting is a small SVG
component and needs an accessible table fallback.

### ADR-007 — English docs and code, pt-BR interface

**Date:** 2026-11-01 · **Status:** accepted

**Context:** The audience is Brazilian; the project is also a learning vehicle
for a Java/Spring developer.

**Decision:** Identifiers, code, migrations, API fields, commit messages and
docs are English. Every user-facing string, email and date is pt-BR. API error
codes are mapped to pt-BR in the client; server `detail` text is not displayed.

**Consequences:** No i18n framework; strings stay local to their components
until reused. Dates must always be formatted explicitly in
`America/Sao_Paulo`; browser-locale defaults are not acceptable.

### ADR-008 — PostgreSQL only, tested with Testcontainers

**Date:** 2026-11-01 · **Status:** accepted

**Context:** The design uses `unaccent`, `pg_trgm`, expression and trigram
indexes, advisory locks and aggregate SQL. An H2 substitute would not execute
them.

**Decision:** Postgres is the only supported database. Integration tests run
against a Testcontainers Postgres. Flyway owns the schema; Hibernate only
validates.

**Consequences:** Tests need Docker. Queries may use Postgres-specific syntax
deliberately. Local dev uses `api/compose.yaml`; the image version should be
pinned rather than `latest`.

### ADR-009 — Contract-first API, RFC 9457 errors, `/api/v1`

**Date:** 2026-11-01 · **Status:** accepted

**Context:** The server and client are developed together, often by one agent at
a time. A written contract prevents drift and lets tests assert behaviour.

**Decision:** `docs/api.md` is the source of truth for HTTP behaviour. Errors are
`application/problem+json` with a stable `code`. Requests reject unknown fields;
responses may add fields. Breaking changes require `/api/v2`.

**Consequences:** Every endpoint change edits `docs/api.md` in the same commit. A
generated OpenAPI file is optional and subordinate to the doc. Contract tests
cover the documented success and failure shapes.

### ADR-010 — UTC storage, São Paulo periods, one period module

**Date:** 2026-11-01 · **Status:** accepted

**Context:** The month boundary must be computed in the audience's zone, not the
server's. The offset can change if daylight saving returns, so it must never be
hardcoded into scattered date math, and a UTC server must still label months
correctly.

**Decision:** Store and return instants in UTC; compute cutoffs, periods and
labels in `America/Sao_Paulo` through `SnapshotPeriods` with an injected
`Clock`. The API returns `Z` instants; the client formats them in pt-BR for the
São Paulo zone.

**Consequences:** `LocalDate.now()` and ad-hoc `ZoneId` calls are defects. Tests
control time through the mutable clock. The count job runs at 00:05 local, which
is currently 03:05 UTC.

### ADR-011 — Ranking is server-side only

**Date:** 2026-11-01 · **Status:** accepted

**Context:** Points, ordering and cutoff filtering must be identical in the live
rank, snapshots and history. Client-side computation would create a second
implementation.

**Decision:** `RankCalculator` computes live ranks and snapshot ranks from the
same SQL and ordering. The client never computes points, positions or periods;
it renders server values and only counts down to a server-provided
`nextCutoffAt`.

**Consequences:** New ranking views require a server endpoint or projection.
The UI cannot invent a different sort for display without a contract change.

### ADR-012 — App-managed `updated_at`, cutoff-filtered snapshots

**Date:** 2026-11-01 · **Status:** accepted

**Context:** The count job cannot run exactly at the millisecond of the cutoff,
so "what counted" must be explicit and testable.

**Decision:** Ballot mutations set `updated_at` from the injected `Clock`. The
snapshot query includes only ballots with `updated_at <= cutoff`, so a later job
run still produces the cutoff-accurate ranking.

**Consequences:** All ballot writes go through `BallotService`; bulk updates are
forbidden. Backfill after a missed cutoff excludes ballots edited since (see
OQ-004). Tests can move the clock across the cutoff to prove the boundary.

### ADR-013 — Derived year view, no yearly snapshot table

**Date:** 2026-11-01 · **Status:** superseded by ADR-015

**Context:** The product promises browsing previous months and years, but the
year concept is a summary, not a vote event.

**Decision:** The year endpoint rolls up the year's monthly snapshots with
months ranked, best, latest and average position. Nothing is frozen per year.

**Consequences:** The year view changes only when its monthly snapshots do. If a
frozen yearly archive is ever required, it becomes a new snapshot type and a new
decision.

### ADR-014 — No production admin surface in this phase

**Date:** 2026-11-01 · **Status:** accepted

**Context:** Content moderation and character management are real needs, but an
admin role expands the auth, audit and UI surface well beyond the current goal.

**Decision:** No `ROLE_ADMIN`, no admin endpoints, no admin UI. Only the
profile-guarded dev snapshot trigger exists, and it is local-only.

**Consequences:** Operational fixes happen through migrations/seed edits and a
restart. This must be revisited before the character catalog grows (M6).

### ADR-015 — Year pages are frozen aggregate snapshots

**Date:** 2026-11-01 · **Status:** accepted (supersedes ADR-013)

**Context:** The derived year rollup was recomputed on every request and was not
an archive. The product needs each year to become a record of its own once the
year closes, while the current year still needs a useful provisional view.

**Decision:** Add `year_snapshots` and `year_snapshot_entries`. For every closed
year that has monthly snapshots, `YearSnapshotGenerator` freezes each character's
months ranked, best rank, latest rank and average rank, plus an ordinal year
rank ordered by average rank. Annual snapshots are generated after the December
snapshot, are idempotent per year and immutable like monthly snapshots. The
current year remains a provisional rollup, explicitly marked `IN_PROGRESS`.

**Consequences:** `GET /snapshots/{year}` reports `FROZEN` or `IN_PROGRESS` and
returns frozen data only for closed years. The year page cannot be affected by a
query-time aggregation change. Monthly snapshots remain the source of the frozen
year. A missed annual run is caught by the same startup and daily backfill. OQ-001
is resolved.

### ADR-016 — Character images are self-hosted

**Date:** 2026-11-01 · **Status:** accepted

**Context:** Hotlinking external images is fragile and makes credits hard to
enforce; the project can serve static assets from the single origin.

**Decision:** Host cleared character images under
`client/public/images/characters/{slug}.webp` and store that relative path in
`characters.image_url`. Every image has alt text, a credit and a source URL.
Until rights are cleared, use the geometric placeholder and label it as
illustrative.

**Consequences:** The SPA build carries character media and a single origin
serves it. The M6 importer validates that each image file exists, has alt text
and a credit, and keeps a rights record. OQ-002 is resolved.

### ADR-017 — Account deletion and export are launch requirements

**Date:** 2026-11-01 · **Status:** accepted

**Context:** LGPD gives people a right to their data and to erasure. The project
should not open voting to the public while those paths are undefined.

**Decision:** Ship account deletion and a data export before launch (M5), not as
a post-launch nice-to-have. Deletion removes the user, auth tokens and ballot;
snapshots contain no user identifiers and remain. Export returns the profile,
ballot and account timestamps in a portable JSON file. Both actions are available
from `/conta` and explained on `/sobre`.

**Consequences:** M5 exit criteria include completing both flows and their tests.
The account page reserves the space now and says the feature arrives before the
public launch. OQ-003 is resolved.

## Open questions

These are known and deliberately unresolved. Do not invent an answer inside a
feature; propose one in this section and get it confirmed. OQ-001, OQ-002 and
OQ-003 were resolved by ADR-015, ADR-016 and ADR-017.

| ID | Question | Working assumption | Needed by |
| --- | --- | --- | --- |
| OQ-004 | Do we need ballot revision history for exact historical backfills? | Not now; normal operation is exact and backfills only undercount edited ballots. | After first missed count |
| OQ-005 | Which anti-abuse measures are worth it (CAPTCHA, disposable-email blocking, stricter rate limits)? | Email confirmation plus per-IP/per-email rate limits first. | M5 |
| OQ-006 | Which domain sends mail, and who owns SPF/DKIM/DMARC? | `no-reply@` on the deployed domain; Mailpit locally. | M5 |
| OQ-007 | Do rank pages need Open Graph previews? | No SSR in the MVP; prerender later if sharing matters. | M6 |
| OQ-008 | Is a dark theme required? | Light only; tokens would need an accessibility review. | Never assumed |
| OQ-009 | How is the scheduler kept single-instance in deployment? | One instance; document the constraint, add ShedLock when scaling. | Deployment |
| OQ-010 | At what size does the live rank need materialization? | Not before ~100k ballots; measure first. | Scale |
| OQ-011 | How are refresh-token races between tabs handled? | Accept the rare family revocation; add a short reuse grace window if users complain. | After launch |
| OQ-012 | What is the correction procedure for a wrong snapshot? | Define an ADR (likely a reviewed one-off replacement script) before any data is touched; never edit rows ad hoc. | Before launch |

## How to record a decision

1. Add a new numbered ADR with date, status (`proposed`, `accepted`,
   `superseded`), context, decision and consequences.
2. If it replaces an earlier decision, mark that one `superseded by ADR-xxx`.
3. Update the affected specs (`overview.md`, `api.md`, `data-model.md`,
   `architecture.md`, `ui.md`) in the same change.
4. Include the ADR number in the commit message when the change implements it.
