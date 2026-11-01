# Project overview

Last reviewed: 2026-11-01 · Status: draft, authoritative for domain rules

Turma da Mônica Rank is a fan-made popularity ranking for characters from the
Turma da Mônica comics. Registered fans build one personal top-10 ballot, and
every month the site freezes that month's ranking into a snapshot so the
history of the ranking becomes a browsable record.

The project is a learning-friendly, small-scope web app: a Vue SPA served by a
Spring Boot API, with PostgreSQL as the only store. The client interface and
all user-facing copy are in Brazilian Portuguese. Code, documentation, commit
messages and identifiers are in English.

## What people do here

| Person | Job to be done |
| --- | --- |
| Fan with an account | Vote and edit a top-10 ranking, then watch it move over time. |
| Visitor | See who is winning now and how the ranking changed month by month. |
| Curious reader | Open a character page for a short bio and that character's position history. |
| Maintainer | Keep character data accurate, run the monthly count, and trust the history. |

Reading is public. Voting and editing a ballot require a confirmed account;
account pages require being signed in (confirmation happens there).

## Glossary

Use these terms in code, docs and copy.

| Term | Meaning |
| --- | --- |
| **Character** | A Turma da Mônica character that can receive votes. |
| **Ballot** (pt-BR: *voto*) | One user's ordered list of 1 to 10 distinct characters. A user has at most one ballot. |
| **Position** | A character's slot on a ballot, 1 (favourite) through 10. |
| **Points** | Score a character earns from one position: `11 - position`. |
| **Live rank** | Ranking computed from all current ballots at request time. |
| **Snapshot** (pt-BR: *rank do mês*, *contagem*) | The immutable, numbered ranking frozen for one calendar month. |
| **Period** | A calendar month, identified by year and month (`2026-11`). |
| **Cutoff** | The instant a period closes: the last day of the month at 23:59:59.999 in `America/Sao_Paulo`. |
| **The count** | The operational event that produces a snapshot at the cutoff. |
| **History series** | The rank of one character across the snapshots that exist. |

## How the ranking works

These rules are normative. Every rank calculation, snapshot, endpoint and UI
label follows them. Do not re-implement them elsewhere.

### Ballot

- A ballot holds between 1 and 10 characters, ordered best first.
- Characters cannot repeat on one ballot.
- A ballot is per user, not per month. It stays current until the user edits
  it, and it can be edited at any time.
- Saving a ballot replaces the previous one wholesale; there is no partial
  update.
- A ballot owned by a disabled account counts for nothing: the live rank and
  future snapshots ignore it. Existing snapshots are untouched, as always.

### Rank math

- Position `p` (1 to 10) awards `11 - p` points. A full ballot awards 55 points
  in total; a shorter ballot awards fewer.
- **Live rank** sums points over every current ballot from active accounts, for
  every active character.
- Ordering is deterministic, in this order:
  1. total points, descending;
  2. number of first-place votes, descending;
  3. number of ballots that include the character, descending;
  4. character slug, ascending (final, stable tie-break).
- Positions shown are ordinal: `1, 2, 3, ...` down the ordered list. Ties are
  broken by the chain above rather than displayed as shared positions.
- Only characters with **at least one point** appear.
- Only `ACTIVE` characters can earn points. An inactive character stays on
  existing ballots (the user's data is not rewritten) but awards no points and
  is rejected by new ballot submissions.

### Snapshot

- One snapshot per period, created at or after that period's cutoff.
- The snapshot freezes: each ranked character's ordinal position, points,
  first-place count, ballot count, the total number of ballots counted, and the
  instant of the cutoff.
- It counts only ballots whose last edit happened at or before the cutoff. A
  ballot edited after the cutoff can never change a closed period. This is why
  the snapshot generator filters on `ballots.updated_at <= cutoff` instead of
  running exactly at midnight and hoping no write races it.
- A snapshot is immutable. No code path updates or deletes snapshot rows.
- Snapshot generation is idempotent per period: running it twice creates
  nothing the second time and reports success.
- The snapshot also stores each character's previous position (from the
  preceding snapshot, when one exists) so the UI can show movement without a
  join across periods.
- A period with zero ballots still gets a snapshot with zero entries, so the
  timeline has no silent holes. Backfill covers every closed month from the
  first month that has ballots through the most recent closed month.
- Ranking history is read from snapshots, never recomputed from ballots.
- Annual snapshots are generated from the monthly snapshots after the year
  closes; monthly snapshots remain their only source.

### Year view

- Each closed year becomes its own frozen snapshot, built from that year's
  monthly snapshots. It is stored separately from the monthly snapshots and is
  immutable once written.
- Per character, the year snapshot records: months ranked, best position, latest
  position (in the most recent month of that year where the character appears),
  average position across the months it was ranked, and an ordinal year rank.
- Characters with no appearance in the year are omitted.
- Order the year rank by average position ascending, then latest position, then
  normalized name.
- The current, still-open year is shown as a provisional rollup marked
  "em andamento". It becomes a frozen snapshot only after the year closes and
  its annual count runs.

### Time

- Instants are stored in UTC (`timestamptz`).
- Period boundaries, labels and cutoffs use `America/Sao_Paulo`.
- A snapshot of November 2026 is labeled `2026-11` and is created on 2026-12-01
  at 00:05 local time, from ballots last edited at or before 2026-11-30 at
  23:59:59.999 local time — both in `America/Sao_Paulo`.
- The API returns ISO-8601 instants (`...Z`); the client formats them in
  `America/Sao_Paulo`.

## Scope

**In scope now**

- Email-and-password accounts with email confirmation, login, refresh,
  logout, and password reset.
- Account deletion and data export before public launch (LGPD readiness).
- One editable top-10 ballot per confirmed user.
- Public live ranking.
- Monthly snapshots plus browsing past months and years.
- Character search, bio page, and position history.
- Responsive, accessible, pt-BR interface.

**Explicitly out of scope for now**

- Admin UI and character CRUD endpoints; character data ships as seed data.
- Social login and multi-factor authentication.
- Comments, reactions, notifications, badges, or paid features.
- Native apps, server-side rendering, i18n beyond pt-BR.
- Automatic scraping of character data.

## Constraints and principles

- **History is the product.** Snapshots are immutable, the rank math has one
  implementation, and timestamps are truthful. If a snapshot is wrong, stop and
  decide a correction path (ADR) before touching data; never edit snapshot rows
  in place.
- **Confirmed humans, one ballot.** Email confirmation gates voting; the
  database enforces one ballot per user.
- **The public rank is open; the ballot is accountable.** Anyone reads the
  rank; changing a ballot requires an authenticated, confirmed account.
- **One contract.** `docs/api.md` defines the HTTP interface; the client and
  server both follow it. Additive changes are fine; breaking changes mean a new
  API version.
- **Postgres is the only database.** Migrations are the only schema authority;
  tests run against Postgres, not an in-memory substitute.
- **Single origin.** The API serves the built SPA in production, so there is no
  CORS configuration anywhere.
- **Respect the source material.** Character names, bios and artwork belong to
  Mauricio de Sousa Produções. Write bios in our own words, host only images we
  have permission to use, credit every image, keep media minimal, and provide
  attribution and a takedown contact on the about page.

## Known constraints

- The scheduler assumes a single application instance. Multiple instances
  require a distributed lock (for example ShedLock) and a new decision entry.
- The live rank is computed per request. This is fine for thousands of ballots
  and is revisited only when measured, not pre-emptively.
- The SPA has no server-side rendering, so rich link previews need a separate
  prerender step if they are ever required.

## Documentation map

| Read this | When |
| --- | --- |
| [requirements.md](requirements.md) | You need the numbered requirement behind a feature, or its acceptance criteria. |
| [architecture.md](architecture.md) | You are changing backend structure, authentication, the count job or config. |
| [data-model.md](data-model.md) | You are writing a migration, entity or repository. |
| [api.md](api.md) | You are adding or consuming an HTTP endpoint. |
| [ui.md](ui.md) | You are building a page, component or design token. |
| [decisions.md](decisions.md) | You want to know why something is the way it is, or you need to record a new choice. |
| [roadmap.md](roadmap.md) | You need the next milestone, or the current gaps. |
