# Client UI specification

Last reviewed: 2026-11-01 · Status: draft, authoritative for UI, routes and copy

The client is the public face of the ranking: a Brazilian Portuguese web app
with the energy of a comic-book cover and the discipline of a good newspaper
table. It should feel hand-made for Turma da Mônica fans, not like a generic
dashboard.

## Visual direction

**Idea: "capa de gibi".** The site borrows the printed comic's vocabulary —
flat spot colours, heavy ink outlines, halftone dots, oversized display
lettering, panels that look stamped onto the page — and applies it to one
memorable element per screen. Everything around that element stays quiet: clean
paper ground, strong rules, generous spacing, a restrained table for data.

Deliberately rejected defaults (do not drift back to them):

- warm cream page with a high-contrast serif and terracotta accent;
- a near-black page with one acid-green accent;
- a grid of identical rounded cards with soft grey shadows;
- tracked-out all-caps labels above every section;
- numbered markers everywhere (numbers belong to ranks, ballot positions and
  the three-step explainer only — all genuine sequences);
- arrows appended to every button, monospace data labels, and gradients used as
  decoration.

Spend boldness in one place: the home podium. The rest of the product is calm,
legible and consistent.

## Design tokens

Declared once in `client/src/style.css` with Tailwind v4 `@theme`, then used
through utilities and component classes. No component uses a raw hex value.

### Colour

| Token | Value | Role |
| --- | --- | --- |
| `--color-ink` | `#14110E` | Text, borders, icon strokes, hard shadows |
| `--color-paper` | `#FFFDF6` | Page ground, panel fill |
| `--color-sky` | `#1565B8` | Hero and section bands, primary large surfaces |
| `--color-sky-deep` | `#0D4A8B` | Sky shading, hover on sky surfaces |
| `--color-cherry` | `#C92F45` | Primary action, rank highlights, errors |
| `--color-sun` | `#FFC93C` | Focus ring, highlights, halftone accents |
| `--color-leaf` | `#1F7A46` | Success, upward movement, confirmation |

Supporting tones are derived with Tailwind opacity utilities on these tokens
(`text-ink/70`), never new hex values. The allowed text/background pairs are:
ink on paper, white on sky, white on cherry, ink on sun, white on leaf, and
cherry, leaf or sky text on paper. Measured ratios: ink/paper 18.5, white/sky
5.8, white/cherry 5.2, ink/sun 12.3, white/leaf 5.3, white/sky-deep 8.7,
cherry/paper 5.2, leaf/paper 5.3, sky/paper 5.8. Verify any new pair at ≥ 4.5:1
before shipping it.

### Type

| Role | Family | Size / weight |
| --- | --- | --- |
| Display / hero | `Baloo 2` | clamp(2.75rem, 6vw, 4.5rem) / 800, line-height 0.95 |
| Page title | `Baloo 2` | 2.5rem / 800, line-height 1.05 |
| Section title | `Baloo 2` | 1.75rem / 700 |
| Card / row title | `Nunito Sans` | 1.125rem / 700 |
| Body | `Nunito Sans` | 1rem / 400, line-height 1.65, max 68ch |
| UI label, small | `Nunito Sans` | 0.875rem / 600 |
| Rank numeral | `Baloo 2` | 2rem–3.5rem / 800, tabular figures |

- Self-host both families via `@fontsource/baloo-2` and
  `@fontsource/nunito-sans`, importing only the weights used (400, 600, 700,
  800). Fallbacks: `ui-rounded, system-ui, sans-serif` and
  `system-ui, sans-serif`.
- Sentence case everywhere except the wordmark.
- Numbers use `font-variant-numeric: tabular-nums`; points use
  `Intl.NumberFormat('pt-BR')` and dates use `Intl.DateTimeFormat('pt-BR')`.
- Avoid italic display text and letter-spacing tricks; the letterforms carry the
  personality.

### Shape, border, shadow

- Panels: `border: 3px solid ink`, `border-radius: 10px`, hard offset shadow
  `6px 6px 0 ink`. No blur, no soft shadow.
- Buttons: 2–3 px ink border, radius 8 px, offset shadow 4 px that collapses to
  0 on `:active` with a 2 px translate — the page feels pressed, not floated.
- Medallions: circles with 3 px ink border and 3 px offset shadow.
- Inputs: 2 px ink border, 8 px radius, paper background, 3 px sun focus ring
  plus 2 px ink offset.
- Section separation: 3 px ink rule, not a card boundary. Alternate a paper
  section with a sky band; never stack cards inside cards.
- Halftone: a CSS radial-gradient dot pattern at 8 px spacing, only in the hero,
  page-header bands, empty states and the placeholder avatar. Never behind body
  text.

### Spacing and layout

- Content width: 1120 px, gutters 20 px mobile / 32 px tablet / 48 px desktop.
- Vertical rhythm: 8 px base; sections 48–96 px apart.
- Hero: asymmetric two-column grid (58% copy / 42% podium) on `lg`; on smaller
  screens the copy comes first and the podium follows.
- Data lists are full-width and left-aligned; rows share a fixed grid so
  numerals, names, bars and stats align across live and snapshot views.
- Breakpoints: `sm` 640, `md` 768, `lg` 1024, `xl` 1280. Test at 360, 768,
  1024 and 1440.

## Page structure

Shared shell: skip link → `AppHeader` (paper strip, 3 px ink bottom border) →
`<main id="conteudo">` → `AppFooter` (ink text on paper, disclaimer, attribution
link, contact). Header contains the wordmark, primary nav (Ranking, Personagens,
Meu voto), and an account area: "Entrar" when anonymous, avatar/menu when signed
in. Mobile collapses nav into a disclosure panel; the "Votar" action stays
visible.

### Home `/`

```text
┌──────────────────────────────────────────────────────────────┐
│ wordmark        Ranking  Personagens  Meu voto     [Votar]   │
├──────────────────────────────────────────────────────────────┤
│ sky band, halftone                                           │
│  Quem é o personagem                        ┌──────────────┐ │
│  mais amado do Bairro do Limoeiro?          │  portrait    │ │
│                                             │      BIG 1   │ │
│  Vote em até 10 personagens...              │  Mônica      │ │
│  [Montar meu top 10]  [Ver ranking]         └──────────────┘ │
├──────────────────────────────────────────────────────────────┤
│ strip: A contagem de novembro fecha em 29 dias · 412 votos   │
├──────────────────────────────────────────────────────────────┤
│ Rank agora                              Ver ranking completo │
│ 1º  [avatar] Mônica      ███████████████  1.024 pts  ...     │
│ 2º  [avatar] Cebolinha   ████████████     918 pts   ...      │
│ ...                                                          │
├──────────────────────────────────────────────────────────────┤
│ Como funciona:  1 Monte seu top 10 · 2 Confirme seu e-mail · │
│ 3 Acompanhe a contagem do mês                                │
├──────────────────────────────────────────────────────────────┤
│ footer                                                       │
└──────────────────────────────────────────────────────────────┘
```

The hero's podium is the one loud element: the live top three in tilted paper
panels, halftone field behind, a huge outlined numeral overlapping each
portrait, and a small "líder deste mês" tag on the first. #1 gets the large
panel; #2 and #3 sit in smaller panels tucked beside it, and on mobile they
become a two-up row under #1. If there is no ranking yet, the podium becomes an
invitation ("A primeira contagem ainda não aconteceu").

### Ranking `/ranking`

- Sky page-header band with title "Ranking" and a short line: "Como os fãs
  votaram até agora" / "Rank congelado de outubro de 2026".
- Tabs: "Agora" and "Por mês". On mobile these are two links, not a select.
- "Agora" renders `RankList` from `GET /rank`, with a generated-at note and a
  link to the latest snapshot.
- "Por mês" shows `MonthChips` (years grouped) and the selected snapshot, or a
  prompt to choose one.
- Table columns: position, character (avatar + name), points bar, points,
  vezes em 1º, total de votos. On mobile the stats collapse under the name as a
  small line; never horizontal scroll for the whole row.
- Movement badges appear only on snapshot views.

### Snapshot `/ranking/:ano/:mes`

Header: "Rank de outubro de 2026", subline "Votos até 31/10/2026, 23h59
(horário de Brasília)" and "Contagem feita em 01/11/2026, 00h05". Show total
ballots. If the period has no snapshot, a full empty state explains that the
month has not been counted yet with a link to the latest month.

### Year `/ranking/:ano`

Header: "Como foi 2026" when the year is frozen, with a "ano fechado" badge and
the generation date; "Como está 2026" with an "em andamento" badge for the
current year. A 12-slot month strip (empty slots labelled "sem contagem"), then
the year table ordered by average position: year rank, character, months ranked,
best, latest, average. Explain the rule in one sentence below the title, not in
tooltips.

If the year has no monthly snapshots, show an empty state: "Este ano ainda não
teve contagem." If a closed year is awaiting its annual count, show a
processing state with a retry action. Frozen years must not offer a refresh that
implies the data can change.

### Characters `/personagens`

Search field first (autofocus off) with the hint "Digite pelo menos 3 letras",
then a responsive grid of character tiles: square portrait with halftone
placeholder, name, current position badge ("1º no rank" / "fora do rank").
Inactive characters show a muted paper badge "fora do rank". Empty search state
suggests three character names. Fewer than 3 typed characters does not call the
API; the list stays on the default alphabetical view.

### Character `/personagens/:slug`

Two-column hero: portrait panel (square, ink border) and detail column with
name, full name, aliases, first appearance, current position sticker, bio, image
credit and source link. Below, a full-width history panel: title "Histórico de
posições", period range chips, the SVG chart (position 1 at the top), and a
`<details>` "Ver como tabela" with the same data. If the character has no
snapshot history, show an invitation to vote instead of an empty chart.

### Vote `/votar`

The functional heart; keep it quiet and confident.

```text
┌──────────────────────────────────────────────────────────────┐
│ Meu top 10                                         3 de 10   │
│ Seu voto vale a partir da próxima contagem.                  │
│                                                              │
│ ┌─ 1 ──────────────────────────────────────────── [↑][↓][×]  │
│ │ [avatar] Mônica                         Arrastar 1º        │
│ └────────────────────────────────────────────────────────────┘
│ ┌─ 2 ──────────────────────────────────────────── [↑][↓][×]  │
│ │ [avatar] Cebolinha                                         │
│ └────────────────────────────────────────────────────────────┘
│ + Adicionar personagem  (search combobox)                    │
│                                                              │
│ [Salvar voto]   [Descartar alterações]                       │
└──────────────────────────────────────────────────────────────┘
```

- Empty state for a new ballot: "Seu top 10 está vazio. Escolha até 10
  personagens — a ordem importa."
- If a saved entry is now inactive, mark the row "não está mais no rank" and
  require its removal before saving; the API rejects inactive entries.
- Reorder controls: up/down buttons (always available, 44 px), plus optional
  pointer drag with a visible drag handle. Each button's accessible name
  includes the character and direction.
- Position labels are pt-BR ordinals ("1º", "2º", …).
- Sticky action bar on mobile with the count and the save button; the discard
  action is secondary and confirms.
- Unsaved changes: `beforeunload` prompt and an in-app confirm dialog on route
  change. The draft lives in `stores/ballot.ts` so an auth round-trip keeps it.
- Success: a stamped "Voto salvo" panel with the last-saved time and the next
  count date. Keep it calm: one stamp, no confetti or celebratory clutter.
- Unconfirmed users are redirected to `/conta` with the confirmation banner. If
  a save is rejected with `AUTH_EMAIL_NOT_CONFIRMED`, the editor keeps the draft
  and shows the same banner with a resend action.

### Auth pages

`/entrar`, `/criar-conta`, `/confirmar-email`, `/esqueci-senha`,
`/redefinir-senha` share `AuthCard`: centered paper panel (max 440 px), ink
border, display title, one-sentence intro, form, inline errors, and a footer
link to the alternative action. The right side of the page on `lg` is a sky
panel with halftone and a short line of copy — no character art.

- `/confirmar-email` reads `?token=`, posts it, shows one of three states:
  confirming, confirmed (link to login), invalid (resend form).
- Registration success navigates to a "Confira seu e-mail" state showing the
  address and resend with a 60-second cooldown.
- Password fields have a show/hide toggle and a length hint, not a strength
  meter.
- Login errors never say whether the email exists.

### Account `/conta`

Requires auth. Shows email, display name, confirmation status, member since,
"Meu voto" link, and logout. When unconfirmed, a prominent banner with resend.
Account deletion and data export are planned for M5, before public launch
(ADR-017). Email change and display-name editing remain deferred.

### About `/sobre`

Fan project disclaimer, what the ranking is, how points work in three
sentences, image credits, data sources, contact for corrections/takedown, and
the technical note that snapshots are immutable.

### Not found `/:pathMatch(.*)*`

Friendly paper panel: "Essa página saiu do gibi." Link home and to the ranking.
Server-side API 404s keep their problem+json shape; the SPA fallback only serves
this page for routes without an API prefix.

## Routes and guards

| Path | Name | Guard | Data |
| --- | --- | --- | --- |
| `/` | home | — | `/rank`, `/snapshots/latest` |
| `/ranking` | ranking | — | `/rank`, `/snapshots` |
| `/ranking/:ano` | ranking-year | — | `/snapshots/{year}` |
| `/ranking/:ano/:mes` | ranking-month | — | `/snapshots/{year}/{month}` |
| `/personagens` | characters | — | `/characters?q=` |
| `/personagens/:slug` | character | — | `/characters/{slug}`, `/characters/{slug}/history` |
| `/votar` | vote | `requiresAuth` + `requiresConfirmed` | `/ballot`, `/characters` |
| `/entrar` | login | `guestOnly` | `/auth/login` |
| `/criar-conta` | register | `guestOnly` | `/auth/register` |
| `/confirmar-email` | confirm-email | — | `/auth/confirm-email` |
| `/esqueci-senha` | forgot-password | `guestOnly` | `/auth/forgot-password` |
| `/redefinir-senha` | reset-password | — | `/auth/reset-password` |
| `/conta` | account | `requiresAuth` | `/auth/me` |
| `/sobre` | about | — | static |
| `/:pathMatch(.*)*` | not-found | — | — |

- `:ano` matches four digits and `:mes` one or two; invalid values render the
  not-found page without calling the API.
- `guestOnly` redirects signed-in users away from login/register.
- Guards wait for session restoration; no login flash on reload.
- `requiresConfirmed` redirects unconfirmed users to `/conta` with a banner.
- Every route sets `document.title` in pt-BR and announces the new page title
  for assistive tech after navigation.
- All page components are lazy-loaded (`() => import(...)`); the shell is the
  only eagerly loaded UI.

## Component inventory

| Component | Responsibility |
| --- | --- |
| `AppHeader`, `AppFooter`, `SkipLink` | Shell, nav, disclosure menu, legal disclaimer |
| `Button`, `IconButton` | Variants: primary (cherry), secondary (outlined), ghost, danger; loading state keeps width |
| `Panel` | Paper surface with ink border and hard shadow |
| `RankMedallion` | Position numeral; 1st cherry, 2nd sun, 3rd leaf, the rest paper, all with ink borders |
| `Podium`, `PodiumPanel` | Home top-three hero; each panel links to its character page |
| `CharacterAvatar` | 1:1 image, lazy loading, initials placeholder, `alt` from API |
| `RankList`, `RankRow` | Live and snapshot leaderboards, responsive grid |
| `MovementBadge` | Arrow + signed number + visually hidden sentence |
| `PointsBar` | Proportional bar with accessible label |
| `MonthChips`, `YearNav` | Period navigation with real links |
| `CountdownStrip` | Days/hours until `nextCutoffAt`; no ticking seconds |
| `SearchCombobox` | Accessible character picker (listbox pattern), active characters only |
| `BallotRow`, `BallotEditor` | Ordered entries, reorder/remove, validation |
| `HistoryChart` | SVG line chart, inverted y-axis, dots, no library |
| `DataTableToggle` | `<details>` table alternative for the chart |
| `AuthCard`, `FormField` | Auth layouts, labels, hints, errors |
| `EmptyState`, `ErrorState`, `LoadingSkeleton` | State handling; error includes retry |
| `ToastStack`, `ConfirmDialog` | Feedback and destructive confirmation |

Behaviour rules:

- Every async view renders a skeleton with the final layout, not a spinner-only
  page.
- Empty states are invitations with a primary action.
- Errors state what failed and what to do; they never show server `detail` or
  stack traces.
- Toasts are polite (`aria-live="polite"`) and disappear after 6 s; errors stay
  until dismissed or retried.
- Destructive actions (withdraw ballot, discard changes) require confirmation.
- No component fetches directly in `setup()`; pages call composables, components
  receive props and emit intentions.

## Copy rules

- pt-BR, fan-to-fan tone: warm, plain, never corporate. "A gente" only if it
  reads naturally; "você" throughout.
- Actions name their result: "Salvar voto", "Montar meu top 10", "Reenviar
  confirmação", "Tentar de novo". Never "Enviar", "Submeter" or "OK".
- Keep copy consistent across the flow: button "Salvar voto" → toast "Voto
  salvo" → note "Seu voto vale a partir da próxima contagem".
- Dates: `Intl.DateTimeFormat('pt-BR')`, `America/Sao_Paulo`; always append
  "(horário de Brasília)" when showing a deadline. Rank labels: `1º`, `2º`, `3º`.
- Errors: state what happened, then the next step. No apologies, no exclamation.
- Empty ranking: "A primeira contagem ainda não aconteceu. Vote para ajudar a
  escrever o primeiro rank."
- Unconfirmed banner: "Confirme seu e-mail para votar. Enviamos um link para
  {email}." + "Reenviar confirmação".
- Session expired: "Sua sessão expirou. Entre de novo para continuar."
- Keep user-facing strings in components close to their use; if a string is
  reused in three places, extract it to `lib/copy.ts`. No i18n framework in the
  MVP.

## Accessibility requirements

Checklist for every page:

- [ ] One `<h1>`; heading levels never skip.
- [ ] Landmarks: `header`, `nav`, `main`, `footer`; skip link first in focus
      order.
- [ ] All controls have accessible names; icons are `aria-hidden`.
- [ ] Visible focus ring on every focusable element (sun ring + ink offset).
- [ ] Colour is never the only signal: movement uses arrow + number + hidden
      text; status uses words.
- [ ] Target size ≥ 44×44 px for touch controls.
- [ ] Table-equivalent `<details>` for the history chart.
- [ ] Ballot reorder works with Tab/Enter/Space only; drag is an enhancement.
- [ ] Form errors: `aria-describedby`, `aria-invalid`, focus moves to the first
      invalid field after submit.
- [ ] Live regions: save confirmation, validation summary; countdowns are not
      live.
- [ ] Motion respects `prefers-reduced-motion`; no parallax, no auto-carousel.
- [ ] Text contrast ≥ 4.5:1 and non-text contrast ≥ 3:1; verify the pairs above.
- [ ] Page zoom to 200% does not clip content or require horizontal scrolling.
- [ ] Copy is understandable without the chart (chart is enhancement).

## Motion

- One orchestrated moment: the home podium panels stamp in on first load
  (translate/rotate settle, 320–480 ms, 60 ms stagger). It runs once per page
  load and is disabled under reduced motion.
- Hover on rank rows: 2 px upward translate and shadow collapse, 120 ms,
  `motion-safe` only.
- Saving a ballot: the "Voto salvo" panel stamps once.
- Route transitions: no page-wide fades; content appears with the layout.
- Never animate on scroll, on every card, or on typography.

## Client implementation notes

- Tailwind v4 via `@tailwindcss/vite`; tokens in `@theme` in `src/style.css`.
- Vue Router 5 with `createWebHistory()`; the server must serve `index.html` for
  unknown non-API routes.
- Pinia stores: `auth` (user, access token, boot state) and `ballot` (draft,
  dirty flag). Keep the access token in memory only.
- `lib/http.ts` owns refresh single-flight, `ApiError` (status, code, field
  errors) and `credentials: 'same-origin'`.
- API error `code` → pt-BR message mapping lives in `lib/errors.ts`; unknown
  codes fall back to a generic message.
- Dates, numbers and rank labels come from `lib/format.ts`; countdown and period
  labels come from `lib/periods.ts`. No date library.
- The chart is a hand-written SVG component with a fixed viewBox that iterates
  the `series`, with a minimum of two points; a single point renders as a dot
  plus a sentence.
- Images use fixed `aspect-square`, `loading="lazy"`, `decoding="async"` and
  explicit width/height attributes; no layout shift.
- Placeholder avatars are generated from initials on a sun or sky field; no
  invented character likenesses.
- `index.html`: `lang="pt-BR"`, title "Turma da Mônica Rank", description,
  `theme-color` matching sky, and self-hosted font preloads.
- No analytics or third-party scripts in the MVP.

## Responsive rules

- 360 px: single column, 20 px gutters, header nav in a disclosure panel, hero
  podium after the copy, rank stats under the name, sticky ballot action bar.
- 768 px: two-column character grid, rank table shows all stats.
- 1024 px: hero splits, character page splits, year strip fits in one row.
- 1440 px: content capped at 1120 px, extra space stays as paper margin.
- Test with 200% zoom and with long character names from the seed; truncate with
  two lines maximum and a full name in `title`/accessible text.

## Implementation status

M1 shipped the design tokens, Baloo/Nunito typography, the app shell, the auth
pages, account page, home/placeholder pages, about and 404, router guards and the
HTTP/auth stores. Still target-only: the rank/character/vote content, history
chart, ballot editor, mobile nav disclosure, client test suite and the full
loading/empty/error matrix for data pages. Keep this spec as the destination; see
[roadmap.md](roadmap.md) for the sequence.

## Legal and content guardrails

- Footer: "Projeto de fã, sem vínculo com Mauricio de Sousa Produções." plus a
  link to `/sobre`.
- `/sobre` lists credits for every image, the source of each bio and a contact
  for corrections and takedown.
- Bios are original summaries; never paste copyrighted text or infringe
  trademark-styled logos.
- Self-host only images we have permission to use. Until rights are cleared, use
  geometric placeholder panels with initials and an explicit "imagem
  ilustrativa" credit.
