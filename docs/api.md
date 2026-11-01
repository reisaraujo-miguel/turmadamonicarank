# API contract

Last reviewed: 2026-11-01 · Status: draft, authoritative for HTTP behaviour

Base path: `/api/v1`. The single-page client and any future consumer follow this
file. Changes to this contract update this file in the same commit.

- Media type: `application/json; charset=utf-8` unless stated.
- Property names: `camelCase`. Enum values: `UPPER_SNAKE_CASE`.
- Instants: ISO-8601 UTC with a `Z` suffix (`2026-11-30T23:59:59.999Z`).
  Periods in responses are objects `{"year": 2026, "month": 11}`; path and query
  periods use `2026/11` and `2026-11` respectively.
- Identifiers: UUID strings.
- Text: pt-BR content, UTF-8.
- Unknown query parameters and unknown JSON properties are rejected with
  `400 VALIDATION_FAILED` (fail loud, so contract drift surfaces in tests).
- List endpoints return `items`, `limit`, `offset` and `total`; the character
  history endpoint returns a bounded `series` instead.
- Authentication and ballot responses are `Cache-Control: no-store`; public
  read caching is documented per endpoint.
- Missing or malformed credentials: `401`. Authenticated but not allowed
  (unconfirmed email): `403`. Not found: `404`. Semantic validation: `422`.

## Authentication

- Access token: JWT in `Authorization: Bearer <token>`, 10-minute lifetime.
  Claims: `iss`, `aud`, `sub` (user id), `jti`, `iat`, `exp`. Do not add roles,
  emails or mutable authorization state to the token.
- Refresh token: opaque random value in an `HttpOnly` cookie named `tmr_refresh`,
  `Path=/api/v1/auth`, `SameSite=Lax`, `Secure` outside local development,
  `Max-Age=2592000` (30 days).
- Rotation: every `POST /auth/refresh` returns a new refresh cookie and
  invalidates the presented one. Reuse of an invalidated token revokes the whole
  family and answers `401 AUTH_REFRESH_INVALID`.
- The client keeps the access token in memory only. Never in `localStorage`,
  `sessionStorage`, cookies readable by JavaScript, or URLs.
- Authorization decisions that depend on email confirmation read the database.
  The token is not trusted for that decision.

### Authorization matrix

| Endpoint group | Anonymous | Unconfirmed | Confirmed |
| --- | --- | --- | --- |
| `GET /rank`, `GET /snapshots/**`, `GET /characters/**` | allowed | allowed | allowed |
| `POST /auth/register`, `/confirm-email`, `/resend-confirmation`, `/login`, `/refresh`, `/logout`, `/forgot-password`, `/reset-password` | allowed | allowed | allowed |
| `GET /auth/me` | 401 | allowed | allowed |
| `GET /ballot` | 401 | allowed | allowed |
| `PUT /ballot`, `DELETE /ballot` | 401 | `403 AUTH_EMAIL_NOT_CONFIRMED` | allowed |
| `POST /dev/snapshots` | 404 outside the `local` profile | — | — |

## Endpoint index

| Method | Path | Auth | Purpose |
| --- | --- | --- | --- |
| POST | `/auth/register` | — | Create an unconfirmed account |
| POST | `/auth/confirm-email` | — | Consume a confirmation token |
| POST | `/auth/resend-confirmation` | — | Send a fresh confirmation link |
| POST | `/auth/login` | — | Authenticate and start a session |
| POST | `/auth/refresh` | cookie | Rotate the session and get a new access token |
| POST | `/auth/logout` | cookie | Revoke the session |
| POST | `/auth/forgot-password` | — | Request a password-reset link |
| POST | `/auth/reset-password` | — | Consume a reset token and set a new password |
| GET | `/auth/me` | Bearer | Current user |
| GET | `/rank` | — | Live ranking |
| GET | `/snapshots` | — | List snapshots, newest first |
| GET | `/snapshots/latest` | — | Latest snapshot |
| GET | `/snapshots/{year}/{month}` | — | One snapshot |
| GET | `/snapshots/{year}` | — | Year rollup |
| GET | `/characters` | — | List/search characters |
| GET | `/characters/{slug}` | — | Character detail |
| GET | `/characters/{slug}/history` | — | Character positions across snapshots |
| GET | `/ballot` | Bearer | Current user's ballot |
| PUT | `/ballot` | Bearer (confirmed) | Replace the current user's ballot |
| DELETE | `/ballot` | Bearer (confirmed) | Withdraw the current user's ballot |
| POST | `/dev/snapshots` | local profile only | Generate a snapshot on demand |

## Shared objects

### `CharacterSummary`

```json
{
  "id": "6f1c3a7e-0f4f-4a5e-9d4e-4a8d2d0e9b21",
  "slug": "monica",
  "name": "Mônica",
  "imageUrl": "/images/characters/monica.webp",
  "imageAlt": "Mônica de vestido vermelho sorrindo",
  "status": "ACTIVE"
}
```

`imageUrl` and `imageAlt` may be `null`. When `imageUrl` is set, the seed must
provide `imageAlt`; the UI never invents a description for a real image.
`status` is always present.

### `UserSelf`

```json
{
  "id": "0b8d6b0e-6f5a-4b0e-9c1b-3d7c5c2a8f10",
  "email": "ana@example.com",
  "displayName": "Ana",
  "emailConfirmed": true,
  "createdAt": "2026-10-12T18:04:21Z"
}
```

### `TokenResponse`

Returned by login and refresh.

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 600,
  "user": { "…": "UserSelf" }
}
```

`expiresIn` is in seconds.

### `Ballot`

```json
{
  "id": "3f2c1c58-6a49-4a0e-9a62-5bb1f8d4c0aa",
  "updatedAt": "2026-11-14T20:12:45Z",
  "entries": [
    {
      "position": 1,
      "character": { "…": "CharacterSummary" }
    }
  ]
}
```

Entries are ordered by `position` and positions are contiguous from 1.

### `SnapshotSummary`

```json
{
  "year": 2026,
  "month": 10,
  "cutoffAt": "2026-11-01T02:59:59.999Z",
  "generatedAt": "2026-11-01T03:05:00.120Z",
  "totalBallots": 412,
  "totalCharacters": 58
}
```

## Authentication endpoints

### `POST /auth/register`

```json
{ "email": "ana@example.com", "password": "copacabana-2026", "displayName": "Ana" }
```

- `201` with `{ "user": UserSelf }`. `emailConfirmed` is false.
- `409 AUTH_EMAIL_ALREADY_REGISTERED` when the email is taken. This is a
  deliberate, documented tradeoff: sign-up reveals existence for a better error
  message, while login, resend and forgot-password stay generic.
- `422 VALIDATION_FAILED` for a malformed email, password outside 10–128, or a
  display name outside 2–60.
- Sends an `EMAIL_CONFIRMATION` email. If mail delivery fails, the account is
  still created and the failure is logged; the user can request a resend.

### `POST /auth/confirm-email`

```json
{ "token": "raw-token-from-the-email" }
```

- `204` on success, `400 AUTH_TOKEN_INVALID` for unknown, expired or consumed
  tokens (one code for all three, on purpose).
- Confirming does not log the user in; the client redirects to login.

### `POST /auth/resend-confirmation`

```json
{ "email": "ana@example.com" }
```

- `202` with no body, always, registered or not, confirmed or not.
- Invalidates outstanding confirmation tokens and sends a new one (24 h).
- `429 RATE_LIMITED` (1 request per minute per email and per IP, M5).

### `POST /auth/login`

```json
{ "email": "ana@example.com", "password": "copacabana-2026" }
```

- `200 TokenResponse` plus `Set-Cookie: tmr_refresh=…`.
- `401 AUTH_INVALID_CREDENTIALS` for a wrong email or password, one message.
- `403 AUTH_ACCOUNT_DISABLED` for a disabled account (only reachable with valid
  credentials).
- Unconfirmed accounts authenticate successfully; the client uses
  `user.emailConfirmed` to prompt for confirmation.

### `POST /auth/refresh`

No request body; uses the `tmr_refresh` cookie.

- `200 TokenResponse` with a rotated cookie.
- `401 AUTH_REFRESH_INVALID` when the cookie is missing, expired, revoked, or
  reused. Reuse revokes the family.
- `401` also clears the cookie.

### `POST /auth/logout`

No body. Accepts a bearer token, a refresh cookie, or both.

- `204`, revokes the refresh family when present, and clears the cookie. Always
  succeeds, even with an invalid session. The current access token stays valid
  until it expires (at most 10 minutes); that is the accepted JWT tradeoff.

### `POST /auth/forgot-password`

```json
{ "email": "ana@example.com" }
```

- `202` with no body, always, to avoid revealing registered emails.
- Sends a `PASSWORD_RESET` email (1 h) when the account exists and is active.
- `429 RATE_LIMITED` (3 per hour per email and per IP, M5).

### `POST /auth/reset-password`

```json
{ "token": "raw-token-from-the-email", "password": "nova-senha-forte" }
```

- `204` on success. Revokes every refresh token for the user.
- `400 AUTH_TOKEN_INVALID` for unknown, expired or consumed tokens.
- `422 VALIDATION_FAILED` for a password outside 10–128 characters.

### `GET /auth/me`

Requires a bearer token. Returns `UserSelf`. `401` when the token is missing,
expired or invalid.

## Ranking endpoints

### `GET /rank`

Live ranking. Public.

| Query | Type | Default | Rules |
| --- | --- | --- | --- |
| `limit` | int | 50 | 1–100 |
| `offset` | int | 0 | ≥ 0 |

```json
{
  "generatedAt": "2026-11-14T20:15:02Z",
  "nextCutoffAt": "2026-12-01T02:59:59.999Z",
  "totalBallots": 412,
  "latestSnapshot": { "…": "SnapshotSummary" },
  "items": [
    {
      "rank": 1,
      "character": { "…": "CharacterSummary" },
      "points": 1024,
      "firstPlaceVotes": 31,
      "ballotCount": 140
    }
  ],
  "limit": 50,
  "offset": 0,
  "total": 58
}
```

- `generatedAt` is when this ranking was computed; it may be up to 60 seconds
  old because of caching. `total` is the number of ranked characters.
- `latestSnapshot` is `null` before the first snapshot exists.
- `nextCutoffAt` is the cutoff of the period currently open (the end of the
  current month in `America/Sao_Paulo`).
- Entries are ordered by the rank math in [overview.md](overview.md#rank-math).
- Response headers: `Cache-Control: public, max-age=30,
  stale-while-revalidate=30`.

### `GET /snapshots`

| Query | Type | Default | Rules |
| --- | --- | --- | --- |
| `year` | int | none | 2000–2100, optional filter |
| `limit` | int | 24 | 1–120 |
| `offset` | int | 0 | ≥ 0 |

```json
{
  "items": [{ "…": "SnapshotSummary" }],
  "limit": 24,
  "offset": 0,
  "total": 3
}
```

Newest first. `Cache-Control: public, max-age=60`.

### `GET /snapshots/latest`

- `200` with a snapshot detail (below).
- `404 SNAPSHOT_NOT_FOUND` when no snapshot exists yet.
- `Cache-Control: public, max-age=30`.

### `GET /snapshots/{year}/{month}`

`{year}` matches `[0-9]{4}`, `{month}` matches `[0-9]{1,2}`. A year outside
2000–2100 or a month outside 1–12 is `404 NOT_FOUND`.

| Query | Type | Default | Rules |
| --- | --- | --- | --- |
| `limit` | int | 50 | 1–100 |
| `offset` | int | 0 | ≥ 0 |

```json
{
  "year": 2026,
  "month": 10,
  "cutoffAt": "2026-11-01T02:59:59.999Z",
  "generatedAt": "2026-11-01T03:05:00.120Z",
  "totalBallots": 412,
  "items": [
    {
      "rank": 1,
      "previousRank": 2,
      "movement": 1,
      "character": { "…": "CharacterSummary" },
      "points": 1024,
      "firstPlaceVotes": 31,
      "ballotCount": 140
    }
  ],
  "limit": 50,
  "offset": 0,
  "total": 58
}
```

- `movement = previousRank - rank`: positive means climbed, negative means fell,
  `null` when `previousRank` is `null` (first snapshot or not ranked before).
- `total` is the number of characters in the snapshot.
- `404 SNAPSHOT_NOT_FOUND` when the period has no snapshot.
- Because the URL identifies immutable data, respond with
  `Cache-Control: public, max-age=31536000, immutable`.

### `GET /snapshots/{year}`

Year view. A closed year is read from its frozen annual snapshot; the current
year is a provisional rollup.

`{year}` matches `[0-9]{4}`; the literal `/snapshots/latest` route is matched
first. `/snapshots/2026` and `/snapshots/2026/11` are different resources.

```json
{
  "year": 2026,
  "status": "FROZEN",
  "cutoffAt": "2027-01-01T02:59:59.999Z",
  "generatedAt": "2027-01-01T03:06:00.000Z",
  "snapshots": [{ "…": "SnapshotSummary" }],
  "items": [
    {
      "rank": 1,
      "character": { "…": "CharacterSummary" },
      "monthsRanked": 12,
      "bestRank": 1,
      "latestRank": 2,
      "averageRank": 1.83
    }
  ],
  "limit": 50,
  "offset": 0,
  "total": 41
}
```

- `status` is `FROZEN` for a closed year with an annual snapshot, `IN_PROGRESS`
  for the current open year, and `EMPTY` for a closed year with no monthly
  snapshots.
- `FROZEN`: `items` come from `year_snapshot_entries`, including the stored
  ordinal `rank`; `cutoffAt` and `generatedAt` are present; respond with
  `Cache-Control: public, max-age=31536000, immutable`.
- `IN_PROGRESS`: `items` are computed from that year's monthly snapshots,
  `rank` is provisional, `cutoffAt` and `generatedAt` are `null`, and the
  response may change at the next monthly count. Cache for 60 seconds.
- `EMPTY`: `items` and `snapshots` are empty, `cutoffAt` is the end of that year
  and `generatedAt` is `null`. `total` is 0.
- `monthsRanked` counts the snapshots in which the character appears;
  `latestRank` is its position in the most recent month of the year where it
  appears; `averageRank` is the average of those monthly positions, rounded to
  two decimals for display.
- Items are ordered by unrounded average rank ascending, then latest rank
  ascending, then `lower(immutable_unaccent(name))` ascending, so pagination is
  stable.
- `limit`/`offset` apply to `items`; `snapshots` is bounded by 12.
- `404 SNAPSHOT_NOT_FOUND` when a closed year has monthly snapshots but its
  annual snapshot has not been generated yet. The backfill repairs this; the UI
  shows a "contagem em processamento" state.
- Years after the current year are `404 NOT_FOUND`.

## Character endpoints

### `GET /characters`

| Query | Type | Default | Rules |
| --- | --- | --- | --- |
| `q` | string | none | 3–100 chars, trimmed; accent/case-insensitive; may include trigram matches |
| `limit` | int | 50 | 1–100 |
| `offset` | int | 0 | ≥ 0 |

```json
{
  "items": [{ "…": "CharacterSummary" }],
  "limit": 50,
  "offset": 0,
  "total": 120
}
```

Includes inactive characters so old links still work; the UI labels them.
Ordered by `lower(immutable_unaccent(name))` in SQL when no `q` is given; by
relevance then that normalized name when `q` is given. A `q` shorter than 3
non-space characters is `422 VALIDATION_FAILED`. `Cache-Control: public,
max-age=300`.

### `GET /characters/{slug}`

```json
{
  "id": "6f1c3a7e-0f4f-4a5e-9d4e-4a8d2d0e9b21",
  "slug": "monica",
  "name": "Mônica",
  "fullName": null,
  "bio": "Baixinha, dentuça e dona de uma força fora do comum…",
  "imageUrl": "/images/characters/monica.webp",
  "imageAlt": "Mônica de vestido vermelho sorrindo",
  "imageCredit": "Ilustração de placeholder",
  "sourceUrl": "https://example.org/source",
  "firstAppearance": { "year": 1963, "work": "Turma da Mônica" },
  "aliases": ["Monica"],
  "status": "ACTIVE",
  "currentRank": 1
}
```

- `currentRank` is the position in the live rank, `null` when unranked or
  inactive. Computed server-side.
- `404 CHARACTER_NOT_FOUND` for an unknown slug.
- `Cache-Control: public, max-age=60`.

### `GET /characters/{slug}/history`

| Query | Type | Default | Rules |
| --- | --- | --- | --- |
| `from` | `YYYY-MM` | 24 snapshots back | inclusive |
| `to` | `YYYY-MM` | latest snapshot | inclusive, ≥ `from` |

```json
{
  "character": { "…": "CharacterSummary" },
  "from": { "year": 2026, "month": 1 },
  "to": { "year": 2026, "month": 10 },
  "series": [
    {
      "year": 2026,
      "month": 9,
      "rank": null,
      "points": null,
      "firstPlaceVotes": null,
      "ballotCount": null
    },
    {
      "year": 2026,
      "month": 10,
      "rank": 3,
      "points": 740,
      "firstPlaceVotes": 12,
      "ballotCount": 98
    }
  ]
}
```

- One item per existing snapshot in range, ordered by period. A month with no
  snapshot does not appear; a month where the character was not ranked appears
  with `rank: null`.
- Maximum 120 items per request; `422 VALIDATION_FAILED` beyond that.
- `404 CHARACTER_NOT_FOUND` for an unknown slug.
- `Cache-Control: public, max-age=60`.

## Ballot endpoints

All ballot endpoints require authentication. Writes additionally require a
confirmed email.

### `GET /ballot`

```json
{ "ballot": { "…": "Ballot" } }
```

`{ "ballot": null }` with `200` when the user has not voted. The response is
never cached (`Cache-Control: no-store`).

### `PUT /ballot`

Replaces the whole ballot.

```json
{ "characterIds": ["uuid-1", "uuid-2", "uuid-3"] }
```

- `200` with the saved `Ballot`.
- `422 VALIDATION_FAILED` when the list is empty, has more than 10 entries, has
  duplicates, or an entry names an unknown or inactive character. Field errors
  carry `BALLOT_INVALID_SIZE`, `BALLOT_DUPLICATE_CHARACTER`,
  `BALLOT_CHARACTER_UNKNOWN` or `BALLOT_CHARACTER_INACTIVE` and name the
  offending index (`characterIds[2]`).
- `403 AUTH_EMAIL_NOT_CONFIRMED` for unconfirmed accounts.
- `401` for anonymous callers.
- Atomic: on any error the previous ballot stays untouched.
- Concurrent saves from two tabs are last-write-wins; there is no ETag or version
  check in the MVP.
- `updatedAt` is set by the server; balloting changes affect only future
  snapshots.

### `DELETE /ballot`

- `204` on success, `204` when the user has no ballot (idempotent). Future
  snapshots no longer count that user; existing snapshots are unchanged.

## Development-only endpoint

### `POST /dev/snapshots`

Available only with the `local` Spring profile. Outside that profile the route
does not exist and the usual API `404 NOT_FOUND` problem is returned.

```json
{ "year": 2026, "month": 10 }
```

- `201` with the snapshot detail when it creates one.
- `409 SNAPSHOT_ALREADY_EXISTS` when the period already has a snapshot.
- `422 VALIDATION_FAILED` for a period that has not closed yet.
- Never expose this route in production; the profile guard is the protection and
  a contract test asserts the route is absent outside `local`.

## Errors

Errors use RFC 9457 `application/problem+json`.

```json
{
  "type": "https://turmadamonicarank.example/errors/validation-failed",
  "title": "Validation failed",
  "status": 422,
  "detail": "One or more fields are invalid.",
  "instance": "/api/v1/ballot",
  "code": "VALIDATION_FAILED",
  "errors": [
    {
      "field": "characterIds[2]",
      "code": "BALLOT_CHARACTER_INACTIVE",
      "message": "Character is not active."
    }
  ]
}
```

- `title` and `detail` are English, for logs and developers. The client maps
  `code` (and `errors[].code`) to pt-BR copy; it does not display server
  messages.
- `errors` is present only for validation failures.
- No stack traces, SQL, tokens or internal identifiers in any field.

| `code` | HTTP | When |
| --- | --- | --- |
| `VALIDATION_FAILED` | 400/422 | Malformed body/query (400) or semantic validation (422) |
| `AUTH_INVALID_CREDENTIALS` | 401 | Wrong email or password |
| `AUTH_EMAIL_NOT_CONFIRMED` | 403 | Ballot write by an unconfirmed account |
| `AUTH_ACCOUNT_DISABLED` | 403 | Disabled account with valid credentials |
| `AUTH_TOKEN_INVALID` | 400 | Confirmation or reset token unknown, expired or used |
| `AUTH_REFRESH_INVALID` | 401 | Refresh cookie missing, expired, revoked or reused |
| `AUTH_REQUIRED` | 401 | No bearer token on a protected route |
| `AUTH_EMAIL_ALREADY_REGISTERED` | 409 | Registration with a taken email |
| `CHARACTER_NOT_FOUND` | 404 | Unknown slug or id |
| `NOT_FOUND` | 404 | Unknown route or resource without a more specific code |
| `SNAPSHOT_NOT_FOUND` | 404 | No snapshot for the period (or none at all) |
| `SNAPSHOT_ALREADY_EXISTS` | 409 | Dev endpoint for a period that already has a snapshot |
| `BALLOT_CHARACTER_UNKNOWN` | 422 | Ballot entry references an unknown character id |
| `BALLOT_CHARACTER_INACTIVE` | 422 | Ballot entry references an inactive character |
| `BALLOT_DUPLICATE_CHARACTER` | 422 | Same character twice on a ballot |
| `BALLOT_INVALID_SIZE` | 422 | Empty ballot or more than 10 entries |
| `RATE_LIMITED` | 429 | Too many attempts; includes `Retry-After` |
| `INTERNAL_ERROR` | 500 | Unexpected failure; no details |

`404` for an unknown route also uses problem+json with `code: "NOT_FOUND"`.
Unhandled exceptions return `500 INTERNAL_ERROR` and log the full detail
server-side.

## Versioning and compatibility

- `/api/v1` is frozen per the rules above. Additive fields and new endpoints are
  non-breaking; removing, renaming, retyping or changing the meaning of a field
  requires `/api/v2`.
- The client must ignore unknown response fields (forward compatibility) even
  though the server rejects unknown request fields (fail loud).
- An optional generated OpenAPI document may be added later, but this file stays
  the source of truth and must match it.

## Requirement traceability

| Endpoint group | Requirements |
| --- | --- |
| `/auth/**` | FR-AUTH-01 … FR-AUTH-13 |
| `/rank` | FR-RANK-01 … FR-RANK-06 |
| `/snapshots/**` | FR-SNAP-01 … FR-SNAP-13 |
| `/characters/**` | FR-CHAR-01 … FR-CHAR-08 |
| `/ballot` | FR-VOTE-01 … FR-VOTE-10, FR-AUTH-11 |
| `/dev/snapshots` | FR-SNAP-12 |

## Contract tests

Every endpoint above needs a Spring MVC test asserting: status, content type,
response shape, error code for the documented failure cases, and authorization
behaviour from the matrix. Public responses are asserted against fixtures with
fixed timestamps from the test `Clock`.
