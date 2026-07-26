---
title: "Session Persistence and Knowledge-Service Boundary"
kind: note
status: draft
created: "2026-07-26"
description: "Synthesizes Knoxx's early OpenPlanner/session-storage note against the current eta-mu, Sol, event-ledger, Epiphany, and OpenPlanner boundaries."
sources:
  - "docs/notes/2026.06.04.09.47.41.md"
  - "open-hax/eta-mu:docs/architecture/contract-dialect-and-data-authority.md"
labels: [sessions, openplanner, epiphany, sol, event-ledger, architecture]
---

# Session Persistence and Knowledge-Service Boundary

## Source observation

The 2026-06-04 source note says:

> it might be a better idea to save the sessions to mongo directly rather than
> sending them to openplanner. Openplanner is just the api we expose to the data.
> Mongo is the real interface to that data.

This is a useful early boundary observation, not a complete design. It correctly
questions whether session continuity should depend on OpenPlanner, but it still
frames the database as the interface and authority.

## What later implementation did

Knoxx later added an in-process OpenPlanner SDK/Mongo client with a REST fallback.
That reduced network indirection and exposed the data plane directly, but it did
not resolve ownership. It made Knoxx more tightly aware of OpenPlanner's storage
and SDK shape.

## Current synthesis

The cleaner boundary is:

```text
Knoxx product session UI
  -> Sol session/turn application contract
  -> event-ledger append/query/replay boundary
  -> configured durable ledger storage

Epiphany source adapter
  <- consumes Sol/session/tool ledger events
  -> produces evidence-aware cross-session and workspace context

OpenPlanner adapter
  <- optional projection/compatibility surface over derived knowledge views
```

### Session authority

- Sol owns agent session and turn behavior.
- The producing runtime appends canonical session/tool events through
  event-ledger.
- Durable storage is an implementation behind that protocol; MongoDB is not the
  domain interface and should not be reached directly by product routes.
- Knoxx resumes ordinary product sessions without requiring Epiphany or
  OpenPlanner.

### Knowledge and context

- Epiphany may consume session ledgers and correlate them with repositories,
  contracts, work items, receipts, and evidence.
- Epiphany returns bounded context/query products; it does not become the writer
  of the original session events.
- OpenPlanner may remain a graph/search API or projection adapter where useful,
  but it does not own basic session memory or canonical knowledge.

## Migration implications

1. Inventory Knoxx routes and clients that use OpenPlanner for session or memory
   behavior.
2. Separate operations into:
   - session command/query;
   - workspace/context query;
   - graph/search projection;
   - product-specific data.
3. Move session command/query behind the Sol/event-ledger contract.
4. Add an optional Epiphany context-source adapter.
5. Retain an OpenPlanner adapter only for consumers that require its current API
   or graph algorithms.
6. Remove direct Mongo writes across service ownership boundaries.

## Failure and optionality laws

- Knoxx can create, append, and resume a session while Epiphany and OpenPlanner
  are unavailable.
- Enabling Epiphany enriches context but does not change core session
  correctness.
- A failed context lookup remains `unavailable`; it does not become an empty
  memory result.
- Replaying a session event does not duplicate material state.
- OpenPlanner projections can be rebuilt from their declared inputs.

## Open questions

- Which current Knoxx session records are events, snapshots, or product state?
- Does event-ledger currently provide sufficient retention and query semantics
  for long-lived sessions, or is a dedicated Sol session projection required?
- Which OpenPlanner APIs have real current consumers after the refactor?
- What is the migration adapter for existing Mongo/OpenPlanner-backed sessions?

## Disposition

`note`, suitable as input to a Knoxx/Sol session-decoupling design or ADR. The
2026-06-04 source remains preserved and should not be rewritten as if it had
already decided the current architecture.
