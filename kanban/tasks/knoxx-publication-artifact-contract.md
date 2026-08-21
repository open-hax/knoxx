---
uuid: knoxx-publication-artifact-contract
title: Publication — say what a materialized artifact is
status: ready
priority: P1
points: 3
labels:
  - tasks
  - publication
  - law
  - has-parent
---

# Publication — say what a materialized artifact is

> Parent epic: `knoxx-translated-publication-to-website`

## Purpose

`IPublicationTarget/publish!` takes an op carrying `:artifact`, and nothing says
what that is. `publication-target-memory` stores it under `:route/artifact` and
hands it back unexamined — correct for a test double, and insufficient for any
adapter that must write bytes somewhere. No real adapter can be written against
an unspecified payload.

## Dependencies

None. This is the first card of W1 and blocks both the registry and the site
adapter.

## Work

- Declare `PublicationArtifact` in `law.publication`: content bytes or string,
  a media type, a declared character encoding, the target locale, and the
  concrete revision it was produced from.
- The revision on the artifact must equal the op's `:concrete-revision`. An
  artifact carrying a different revision is a conflict, not a warning — it means
  the renderer and the planner disagreed about what is being published.
- Reject a selector keyword anywhere in the artifact, for the same reason
  `publish-idempotency-key` refuses one: a selector produces a stable-looking
  identity for a moving target.
- Validate in both directions at the effect boundary, matching the existing
  contract there: the artifact is checked before `publish!`, and an adapter's
  returned receipt is checked before a caller reads fields off it.
- State explicitly whether the artifact is produced above or below the boundary,
  and pin the answer with a test. Producing it below means each adapter renders
  independently and they will diverge; producing it above means one renderer and
  adapters that only transport.
- Update `publication-target-memory` to validate what it is handed, so the E2E
  fails on a malformed artifact rather than storing it.

## Definition of Done

- `PublicationArtifact` is declared in law and consumed by the effect boundary.
- A malformed artifact fails before any effect runs.
- An artifact whose revision disagrees with the op's concrete revision is a
  typed conflict with both revisions in the evidence.
- The memory target validates rather than accepting anything.
- The rendering-side ownership decision is written down and asserted.
