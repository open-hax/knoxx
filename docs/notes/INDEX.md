# Notes Index

Knoxx notes are preserved as non-authoritative working material. Timestamped
source notes remain unchanged; synthesized notes link to them and state their
current disposition.

| Note | Kind/status | Disposition | Summary |
|---|---|---|---|
| [2026.06.04.09.47.41.md](2026.06.04.09.47.41.md) | raw working note | `extract` + retain | Early observation that session persistence should not depend on OpenPlanner and that OpenPlanner is an API over data. Extracted into the session-boundary synthesis below. |
| [session-persistence-and-knowledge-service-boundary.md](session-persistence-and-knowledge-service-boundary.md) | note / draft | current synthesis | Separates Sol/event-ledger session authority, optional Epiphany context, and optional OpenPlanner projection compatibility. Decision and implementation remain open. |
| [2026.06.03.09.09.14.md](2026.06.03.09.09.14.md) | raw operations snippet | `closed-no-extraction` + retain | One `gh variable set TESTING_ALLOWED_OWNER_LOGINS` command for another repository. Useful only as historical shell context unless a related deployment incident or policy is identified. |
| [2026.05.21.14.26.30.md](2026.05.21.14.26.30.md) | raw agent transcript | `historical` + retain | Proposed splitting `infra/control_config.cljs`. As of 2026-09-30 that file is 53 lines with no `event-agent` names; contract catalogs live in `domain/control/catalog.cljs`. Cited `docs/notes/architecture/data-oriented-patterns.md` moved to Foresight on 2026-09-30: [`docs/architecture/data-oriented-patterns.md`](https://github.com/open-hax/foresight/blob/main/docs/architecture/data-oriented-patterns.md). |
| [2026.05.21.14.30.11.md](2026.05.21.14.30.11.md) | raw licensing draft | `historical` + retain | Claims LGPL-3.0-only; as of 2026-09-30 `LICENSE` is GPL-3.0 and `package.json`/`backend/package.json` declare `GPL-3.0-or-later`. Not current licensing authority. |
| [2026.05.21.14.30.35.md](2026.05.21.14.30.35.md) | feature snippet | `current` | Audio labels API; routes verified in `backend/src/cljs/knoxx/backend/infra/routes/studio.cljs` and `domain/label/audio.cljs`. |
| [2026.05.21.14.44.34.md](2026.05.21.14.44.34.md) | house-rules draft | `superseded` + retain | Earlier copy of the rules now maintained in `AGENTS.md`. |
| [2026.05.21.15.11.30.md](2026.05.21.15.11.30.md) | raw prompt | `open` | Asks for a lint warning on direct `route!`; as of 2026-09-30 `infra/routes/users/admin.cljs` still calls `route!` directly and no such hook exists. |
| [2026.05.21.15.38.19.md](2026.05.21.15.38.19.md), [2026.05.21.16.10.10.md](2026.05.21.16.10.10.md) | raw design constraints | `historical` + retain | Event-agent removal constraints; cited `specs/` and `docs/notes/architecture/*` paths now live under `kanban/epics/` and `docs/design/`. |
| [2026.05.21.16.48.20.md](2026.05.21.16.48.20.md) | raw agent transcript | `closed` | defroute hook now models the `^:async` handler (`backend/.clj-kondo/hooks/defroute.clj`). |
| [2026.05.22.12.27.09.md](2026.05.22.12.27.09.md) | raw agent transcript | `closed` | policyDb JS facade removal; as of 2026-09-30 no `policyDb`/`build-facade` references remain in backend source. |
| [2026.05.22.12.27.27.md](2026.05.22.12.27.27.md), [2026.05.22.12.30.28.md](2026.05.22.12.30.28.md) | fragments / log paste | `historical` | Unfinished action definition; contract-load log from the pre-extraction OpenPlanner checkout path. |
| [2026.05.22.20.00.58.md](2026.05.22.20.00.58.md), [2026.05.22.21.21.35.md](2026.05.22.21.21.35.md) | raw design transcript | `historical` + retain | Source/driver model. Drivers remain code in `domain/driver/builtin.cljs`; source resources now live in `contracts/namespaces/*.edn` (no `contracts/sources/`). |
| [2026.05.25.09.41.59.md](2026.05.25.09.41.59.md) | raw transcript | `current` in part | `:infer-externs false` on all builds in `backend/shadow-cljs.edn`; no `:advanced` build yet. |
| [2026.05.25.10.11.16.md](2026.05.25.10.11.16.md) | raw incident transcript | `historical` | Redis/OpenPlanner composite session-store bug; Redis stores were later removed (Mongo/Clio stores under `infra/stores/`). |
| [2026.05.25.11.54.51.md](2026.05.25.11.54.51.md) | raw ops transcript | `historical` | Frontend bridge rewrap; later fixed by `frontend/scripts/watch-shadow-bridge-rebuild.mjs`; root `ecosystem.config.cjs` is now a deprecated delegating shim. |
| [2026.05.25.13.04.06.md](2026.05.25.13.04.06.md) | empty file | `closed-no-extraction` | Zero bytes. |
| 2026.05.28.17.30.58.md | board UI feedback | `extracted` 2026-09-30 | About the kanban board UI (now Rheos), not Knoxx. Moved to Foresight [`docs/notes/rheos-board-ui-frontmatter-comments-feedback.md`](https://github.com/open-hax/foresight/blob/main/docs/notes/rheos-board-ui-frontmatter-comments-feedback.md). |
| [2026.06.10.11.10.27.md](2026.06.10.11.10.27.md) | raw design sketch | `historical` + retain | Namespace-composed resource files; realized as `contracts/namespaces/*.edn` (`{:namespace ... :resources [...]}`). |

> 2026-09-30 audit: rows for the remaining top-level timestamped notes were added
> above. Notes in subdirectories are not yet indexed here.

## Processing rules

- Raw notes remain source records and are not rewritten merely to add current
  terminology.
- A note does not become architecture because implementation later resembled it.
- Implementation/status snippets become stale by revision and time.
- Durable design or decision artifacts must link to their source notes and name
  the accepting authority.
- Cross-repository relations should point to eta-mu, Epiphany, Muse, or
  OpenPlanner artifacts rather than copying their text into Knoxx.

## Highest-value next pass

Inventory the Knoxx OpenPlanner clients, routes, and session stores against
[session-persistence-and-knowledge-service-boundary.md](session-persistence-and-knowledge-service-boundary.md), then produce a bounded decoupling design with migration and compatibility tests.
