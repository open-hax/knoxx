# Notes Index

Knoxx notes are preserved as non-authoritative working material. Timestamped
source notes remain unchanged; synthesized notes link to them and state their
current disposition.

| Note | Kind/status | Disposition | Summary |
|---|---|---|---|
| [2026.06.04.09.47.41.md](2026.06.04.09.47.41.md) | raw working note | `extract` + retain | Early observation that session persistence should not depend on OpenPlanner and that OpenPlanner is an API over data. Extracted into the session-boundary synthesis below. |
| [session-persistence-and-knowledge-service-boundary.md](session-persistence-and-knowledge-service-boundary.md) | note / draft | current synthesis | Separates Sol/event-ledger session authority, optional Epiphany context, and optional OpenPlanner projection compatibility. Decision and implementation remain open. |
| [2026.06.03.09.09.14.md](2026.06.03.09.09.14.md) | raw operations snippet | `closed-no-extraction` + retain | One `gh variable set TESTING_ALLOWED_OWNER_LOGINS` command for another repository. Useful only as historical shell context unless a related deployment incident or policy is identified. |

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
