---
uuid: knoxx-publication-runtime-follow-up
title: Publication Runtime Follow-up — Lifecycle Separation, Lossless State, and Gardens Decoupling
status: accepted
priority: P1
points: 13
labels: epics, publication, cms
---

# Publication Runtime Follow-up — Lifecycle Separation, Lossless State, and Gardens Decoupling

> GitHub issue: [#246](https://github.com/open-hax/knoxx/issues/246)

## Context

Publication closeout proved the production path and repaired the immediate boundary failures.
It also exposed architectural debt that should remain explicit follow-up work instead of being
smuggled back into the already-green closeout stack.

## Purpose

Own the deliberately deferred architectural work exposed by publication closeout: lossless EDN state mutation, HTTP/event-runtime lifecycle separation, Gardens/OpenPlanner REST decoupling, and authoritative production-boundary verification.

## Scope

1. Replace whole-value EDN rewrites on human-maintained publication/resource manifests with
   syntax-preserving targeted edits that retain comments, unrelated resources, ordering, and
   surrounding layout where practical.
2. Separate HTTP/backend startup from event-runtime startup. Define explicit, truthful,
   idempotent `start`, `stop`, `reset`, and `status` behavior, including disabled mode and an
   HTTP-only boot with zero external event effects.
3. Inventory and remove supported Gardens consumers' direct dependency on
   `/api/openplanner/v1/gardens` and `OPENPLANNER_API_KEY` by moving them to one Knoxx-owned
   Gardens contract.
4. Preserve an executable production-boundary verifier for publication routes, immutable
   identity, manifest preservation, HTTP-only startup, and garden publication/viewing. Required
   probes reject unexpected 4xx/5xx responses and prove the checkout/deployment under test.

## Non-goals

- Reopening publication/translation closeout work that is already green.
- Redesigning publication content or block schemas.
- Broad event-system redesign beyond lifecycle ownership and side-effect separation.
- Migrating unrelated OpenPlanner APIs.

## Laws

1. Targeted publication mutation cannot delete or rewrite unrelated manifest resources.
2. Comments and unrelated syntax in human-maintained EDN survive targeted edits.
3. HTTP startup cannot implicitly connect gateways or dispatch event/agent work.
4. Disabled or failed lifecycle operations cannot report false success.
5. Publication garden/revision/path/locale identity remains immutable across transitions.
6. Unexpected 4xx/5xx responses on required production surfaces fail verification.
7. Gardens consumers target a domain contract, not a legacy OpenPlanner REST transport detail.

## Children

- `knoxx-publication-lossless-edn-state-edits` — 3sp
- `knoxx-http-event-runtime-lifecycle-separation` — 5sp
- `knoxx-gardens-openplanner-rest-decoupling` — 3sp
- `knoxx-publication-live-verification-contract` — 2sp

## Sequencing

Begin from the verified publication-closeout baseline. The lossless-state, lifecycle, and Gardens
slices remain independently reviewable; consolidate the final live verifier after those target
boundaries are authoritative. A newly discovered closeout regression may block this epic, but the
deferred architecture work does not retroactively block the landed closeout stack.

## Definition of Done

- All four child slices are complete.
- A final live production-boundary verification passes after the migrations.
- No supported Gardens publication/viewer path directly depends on `/api/openplanner/v1/gardens`.
- Human-maintained publication/resource manifests are mutated losslessly outside the targeted change.
- HTTP-only backend startup has zero event-runtime side effects.
- Event-runtime lifecycle operations are explicit and truthful in disabled and failure modes.
- `OPENPLANNER_API_KEY` is removed when the caller inventory proves no supported surface needs it.
- Required live probes reject unexpected 4xx/5xx responses and prove deployment identity.
