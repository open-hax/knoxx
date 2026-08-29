---
uuid: knoxx-publication-runtime-follow-up
title: Publication Runtime Follow-up — Lifecycle Separation, Lossless State, and Gardens Decoupling
status: accepted
priority: P1
points: 13
labels:
  - epics
  - publication
  - cms
---

# Publication Runtime Follow-up — Lifecycle Separation, Lossless State, and Gardens Decoupling

GitHub: #246

## Purpose

Own the deliberately deferred architectural work exposed by publication closeout: lossless EDN state mutation, HTTP/event-runtime lifecycle separation, Gardens/OpenPlanner REST decoupling, and authoritative production-boundary verification.

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

## Definition of Done

- All four child slices are complete.
- A final live production-boundary verification passes after the migrations.
- No supported Gardens publication/viewer path directly depends on `/api/openplanner/v1/gardens`.
- Human-maintained publication/resource manifests are mutated losslessly outside the targeted change.
- HTTP-only backend startup has zero event-runtime side effects.
