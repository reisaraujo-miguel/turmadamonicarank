# Requirements

Last reviewed: 2026-11-01 · Status: draft, authoritative for scope

This file is the requirement baseline. Every feature, endpoint, migration and
page must trace to an ID here. When behaviour changes, change the requirement
first and list the ID in the commit message (`FR-VOTE-04`).

Keywords: **MUST** is required for the milestone that delivers it, **SHOULD** is
the expected default, **MAY** is optional. Domain rules referenced below live
in [overview.md](overview.md#how-the-ranking-works).

## Functional requirements

### Accounts and sessions (`FR-AUTH`)

| ID | Requirement |
| --- | --- |
| FR-AUTH-01 | A visitor MUST register with an email, a password and an optional display name. Email is trimmed, lowercased and unique. Password MUST be 10–128 characters and stored only as a slow hash. The account starts unconfirmed and a confirmation email is sent. |
| FR-AUTH-02 | A confirmation link MUST mark the account confirmed, be single-use and expire after 24 hours. Expired, reused or unknown tokens produce one generic token error, with a resend path in the UI. |
| FR-AUTH-03 | A user MUST be able to request a new confirmation email. The response MUST be identical whether or not the email is registered. A new request invalidates outstanding confirmation tokens. |
| FR-AUTH-04 | A registered user MUST log in with email and password. Bad credentials MUST return one generic message that does not reveal whether the email exists. Unconfirmed users MAY log in but see a confirmation prompt. Disabled accounts MUST be refused. |
| FR-AUTH-05 | Login MUST return a short-lived access token (10 minutes) and set a long-lived rotating refresh token in an HttpOnly cookie (30 days). Tokens MUST NOT be stored in `localStorage` or `sessionStorage`. |
| FR-AUTH-06 | Refreshing MUST rotate the refresh token. Presenting an already-rotated token MUST revoke the token family and return an authentication error. |
| FR-AUTH-07 | Logout MUST revoke the current refresh-token family, clear the cookie and leave the client without usable tokens. |
| FR-AUTH-08 | A user MUST be able to request a password reset. The response MUST NOT reveal whether the email exists. The reset link MUST be single-use and expire after 1 hour. |
| FR-AUTH-09 | Completing a password reset MUST set the new password and revoke all refresh tokens for that user. |
| FR-AUTH-10 | A signed-in user MUST see their email, display name, confirmation status and the option to log out. |
| FR-AUTH-11 | Creating or editing a ballot MUST require a confirmed account; unconfirmed accounts receive a specific error that the client turns into a "confirm your email" prompt with resend. |
| FR-AUTH-12 | Sign-up, login, resend, forgot-password and token endpoints MUST be rate limited per IP, and login also per email (M5). |
| FR-AUTH-13 | The client MUST silently refresh an expired access token once and retry the original request; a failed refresh MUST return the user to the login page without losing the current route. |
| FR-ACC-01 | A signed-in user MUST be able to export their profile, ballot and account timestamps as a portable JSON file before public launch. |
| FR-ACC-02 | A signed-in user MUST be able to delete their account before public launch. Deletion removes the user, auth tokens and ballot; snapshot aggregates are unaffected and remain as history. |

### Ballot (`FR-VOTE`)

| ID | Requirement |
| --- | --- |
| FR-VOTE-01 | A confirmed user MUST be able to open a ballot editor that shows their current ballot, or an empty editor. |
| FR-VOTE-02 | A ballot MUST contain 1–10 distinct characters. Adding an 11th or a duplicate MUST be rejected in the UI and by the API. |
| FR-VOTE-03 | The user MUST be able to reorder entries, remove entries, and search characters by name while adding. Reordering MUST be possible with keyboard actions, not drag alone. |
| FR-VOTE-04 | Saving MUST replace the whole ballot atomically. A failed save MUST leave the previous stored ballot intact. |
| FR-VOTE-05 | A user MUST be able to edit a saved ballot at any time, in any month. |
| FR-VOTE-06 | A saved ballot MUST be shown with its last-saved time and a plain-language note that changes apply to the next count. |
| FR-VOTE-07 | A user MUST be able to withdraw their ballot; the account keeps working and their earlier snapshots stay intact. |
| FR-VOTE-08 | The API MUST reject characters that do not exist or are inactive, with a field-level error naming the offending entry. |
| FR-VOTE-09 | Leaving the editor with unsaved changes MUST warn before discarding them. |
| FR-VOTE-10 | The ballot editor MUST work on a 360 px wide screen and with a screen reader. |

### Live ranking (`FR-RANK`)

| ID | Requirement |
| --- | --- |
| FR-RANK-01 | The current live rank MUST be public, paginated and ordered by the rank math in [overview.md](overview.md#rank-math). |
| FR-RANK-02 | Each entry MUST expose position, character identity, points, first-place count and ballot count. |
| FR-RANK-03 | Characters with zero points MUST NOT appear, inactive characters MUST NOT earn points, and ballots owned by disabled accounts MUST NOT count. |
| FR-RANK-04 | The live rank response MUST include the instant of the next cutoff and a summary of the latest snapshot, so the client does not duplicate calendar logic. |
| FR-RANK-05 | Live rank reads MAY be cached for up to 60 seconds; the response MUST state when it was generated. |
| FR-RANK-06 | The live rank MUST change immediately (within the cache window) after a ballot is saved. |

### Snapshots and history (`FR-SNAP`)

| ID | Requirement |
| --- | --- |
| FR-SNAP-01 | The system MUST create exactly one snapshot per closed month, after the cutoff, counted only from ballots last updated at or before that cutoff and owned by non-disabled accounts. |
| FR-SNAP-02 | Snapshot generation MUST be idempotent; a second run for the same period MUST succeed without changing data. |
| FR-SNAP-03 | Snapshot rows and their entries MUST be immutable once written. |
| FR-SNAP-04 | On startup and once a day, the system MUST generate any missing snapshots for already-closed periods, oldest first. A failure is logged with the period and retried on the next run; it MUST NOT stop the API from serving reads. |
| FR-SNAP-05 | The public MUST be able to list snapshots, newest first, optionally filtered by year. |
| FR-SNAP-06 | The public MUST be able to open one snapshot and see the frozen ranking, cutoff time, generation time and total ballots counted. |
| FR-SNAP-07 | The public MUST be able to open the latest snapshot directly. |
| FR-SNAP-08 | A closed year MUST become its own frozen snapshot built from that year's monthly snapshots, with months ranked, best, latest, average and an ordinal year rank. The current year MUST be shown as a provisional rollup explicitly marked "em andamento". |
| FR-SNAP-13 | Annual snapshot generation MUST run after the December monthly snapshot, be idempotent per year, immutable, and included in startup/daily backfill. A year with no monthly snapshots does not get an annual snapshot. |
| FR-SNAP-09 | A month with no ballots MUST still have a snapshot, displayed as an empty month rather than a missing one. |
| FR-SNAP-10 | Each snapshot entry MUST carry the character's previous position when a preceding snapshot exists, enabling movement display without cross-period joins. |
| FR-SNAP-11 | Editing or withdrawing a ballot MUST NOT alter any existing snapshot. |
| FR-SNAP-12 | A development-only trigger MAY generate a snapshot on demand; it MUST be unavailable outside the `local` profile. |

### Characters (`FR-CHAR`)

| ID | Requirement |
| --- | --- |
| FR-CHAR-01 | The public MUST be able to list characters alphabetically and search by name or alias, case- and accent-insensitively ("monica" finds "Mônica"). |
| FR-CHAR-02 | A character page MUST show name, full name, short bio, aliases, first appearance, image with credit, current position and whether it is ranked. |
| FR-CHAR-03 | A character page MUST show position history across snapshots in the requested range, including months where the character was not ranked as gaps. |
| FR-CHAR-04 | Unknown character slugs MUST return a not-found response and a helpful page. |
| FR-CHAR-05 | Inactive characters MUST remain browsable and keep their history, but MUST be labelled as outside the current rank. |
| FR-CHAR-06 | Character data MUST be read-only through the public API; it ships as seed data in this phase. |
| FR-CHAR-07 | Character slugs MUST be stable URLs; renames keep the slug unless a redirect is added. |
| FR-CHAR-08 | Search MUST tolerate typos well enough to find a character from 3 characters typed (trigram similarity or prefix match). |

### Interface and platform (`FR-UX`)

| ID | Requirement |
| --- | --- |
| FR-UX-01 | All user-facing copy MUST be Brazilian Portuguese; dates, numbers and plurals MUST use `Intl` with `pt-BR`. |
| FR-UX-02 | Every page MUST be usable from 360 px to 1440 px wide. |
| FR-UX-03 | Every data view MUST have loading, empty, error-with-retry and not-found states. |
| FR-UX-04 | Every interactive element MUST be reachable by keyboard with a visible focus indicator, and the app MUST meet WCAG 2.2 AA. |
| FR-UX-05 | Rank, snapshot, year and character pages MUST have shareable URLs that survive reload and browser back/forward. |
| FR-UX-06 | A signed-out user pressing a vote call-to-action MUST log in and return to the intended page. |
| FR-UX-07 | The UI MUST show the time zone used for the count ("horário de Brasília") next to countdowns and cutoff times. |
| FR-UX-08 | The interface MUST state clearly that it is a fan project, unaffiliated with Mauricio de Sousa Produções, with attribution and a takedown contact. |
| FR-UX-09 | The app MUST support the current and previous major versions of Chrome, Firefox, Safari and Edge, including mobile Safari 16+. |

## Acceptance scenarios

Each scenario is a test to write when the milestone lands.

1. **Register, confirm, vote** — A visitor registers with a new email, receives
   a confirmation link, confirms, logs in, orders three characters and saves.
   The live rank reflects the ballot within the cache window, and their ballot
   returns on reload.
2. **Unconfirmed cannot vote** — An unconfirmed user logs in and calls
   `PUT /ballot`; the API answers `403 AUTH_EMAIL_NOT_CONFIRMED` and no ballot
   row is written. In the client, the `/votar` guard redirects to `/conta`,
   which shows the confirmation banner and the resend action.
3. **Edits are not historical** — A user votes in November, the November
   snapshot is generated, the user edits the ballot in December and the
   November snapshot is byte-for-byte unchanged.
4. **Cutoff honesty** — A ballot is edited after the cutoff but before the job
   runs; the closed month's snapshot excludes that edit, and the next snapshot
   includes it.
5. **Idempotent count** — Running the count twice for the same period produces
   one snapshot and identical entries.
6. **Search accents** — Searching "cebolinha" and "Cebolinha" both find the
   character; searching "magali" works with a typo of one letter.
7. **History gaps** — A character ranked in October, absent from the November
   snapshot and ranked again in December shows a gap in the series, not a line
   between October and December.
8. **Session refresh** — With an expired access token, saving a ballot triggers
   one refresh, retries the save once and succeeds; with a revoked refresh
   token, the user is sent to login and keeps the editor draft in memory.
9. **Empty month** — A month with no ballots still appears in the timeline,
   labelled as a month without votes.
10. **Mobile ballot** — A 360 px viewport can reorder ten entries using only
    the keyboard-accessible controls and save.

## Non-functional requirements

### Security (`NFR-SEC`)

- Passwords hashed with BCrypt cost 12 (or Argon2id if adopted), never logged,
  never returned. Password length 10–128; no composition rules.
- Refresh tokens are opaque random values (≥256 bits), stored only as SHA-256
  hashes, delivered in an `HttpOnly`, `SameSite=Lax`, `Secure` (production)
  cookie scoped to `/api/v1/auth`.
- Access tokens are signed JWTs (HS256), 10-minute lifetime, with `iss`, `aud`,
  `sub`, `jti` and `exp` claims. The secret comes from the environment and is at
  least 32 bytes.
- Authorization reads verification state from the database on every write; JWT
  claims may only inform the UI.
- All input validated at the boundary; all SQL parameterized; no stack traces
  or internal messages in responses.
- Security headers: CSP limited to self-hosted assets, `X-Content-Type-Options:
  nosniff`, `Referrer-Policy: strict-origin-when-cross-origin`,
  `Permissions-Policy` minimal, HSTS in production.
- Rate limiting on authentication endpoints; generic responses for account
  existence on login, resend and forgot-password.
- Dependency and secret scanning before release (M5).

### Performance and capacity (`NFR-PERF`)

- p95 response time under 300 ms for rank, character and snapshot reads with
  10,000 ballots and 500 characters, measured locally against Postgres.
- Rank queries use indexed access at most once per 60 seconds per instance.
- Initial SPA JavaScript under 250 KB gzip; no chart or UI-kit dependency for
  the MVP.
- Client target: Lighthouse performance ≥ 90 on a mid-range mobile profile for
  the home page with throttling.

### Accessibility (`NFR-A11Y`)

- WCAG 2.2 AA: contrast ≥ 4.5:1 for text, visible focus, labelled controls,
  semantic landmarks, skip link, no keyboard traps.
- Ballot ordering works without pointer drag; announcements via an ARIA live
  region when entries move or save.
- Charts have a table-equivalent view of the same data.
- `prefers-reduced-motion` removes non-essential motion.

### Observability and operations (`NFR-OBS`)

- `/actuator/health` reports database reachability and is public; other actuator
  endpoints are not exposed.
- Structured logs with request ID, route, status and duration. No email, token,
  password or raw cookie in logs.
- The count job logs start, period, eligible ballots, created snapshot id and
  duration; failures log an actionable message.
- Local development runs with Docker Compose (Postgres, Mailpit) and no cloud
  dependency.

### Data (`NFR-DATA`)

- Flyway is the only schema authority (`ddl-auto: validate`).
- Migrations are forward-only; applied migrations are never edited.
- Expired auth tokens are deleted by a daily cleanup job after 30 days.
- Ballots cascade-delete with their user; snapshots outlive users and are never
  recomputed.
- Database backup and restore are an operational concern of the deployment,
  documented before launch (M5).

### Legal and content (`NFR-LEGAL`)

- Bios are short factual summaries written in our own words, with a source link.
- Character images are self-hosted only when cleared; every image records a
  credit and a source URL. Use placeholder artwork until rights are cleared.
- The about page states the fan, non-commercial nature of the project, names the
  rights holder, and provides a contact for corrections and takedown.
- Only email and display name are collected; no analytics or third-party
  trackers in the MVP.

### Maintainability (`NFR-MAINT`)

- Every endpoint has a contract test; every migration is exercised by the
  integration test suite against Postgres.
- Public behaviour is documented in `docs/` in the same change that alters it.
- CI runs the API test suite and the client type-check/build on every pull
  request (M0/M5).
- Code follows the module boundaries and naming in
  [architecture.md](architecture.md); new dependencies need a decision entry.

## Deferred requirements

These are acknowledged and intentionally not built yet; keep them in
[roadmap.md](roadmap.md) rather than implementing them ad hoc.

- Admin UI or authenticated admin API for character data.
- Social login, email change, multi-factor authentication.
- Notification email at month end.
- Server-side rendering / Open Graph previews.
- Public read API for third parties.
- Character image upload pipeline.
- Moderated bios or user contributions.
