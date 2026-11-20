---
uuid: "knoxx-371-build-provenance"
title: "Bind Wiki browser evidence to source build"
status: "incoming"
type: "task"
priority: "P1"
labels: "wiki, verification, provenance"
parent: "knoxx-publication-runtime-follow-up"
dependency: []
category: "tasks"
write-id: "1793664169091-0.bltxfykzfchej0g8vkk"
created_at: "2026-11-03T00:02:49.091Z"
---

# Bind browser evidence to the built source

## Outcome

Browser evidence identifies the exact source and build inputs that produced the artifacts actually served during the supervised tour.

## Scope

Destination: Wiki stack supervisor and build-provenance verification, under the publication runtime follow-up epic.

## Non-goals

Changing authentication, credentials, production services, or treating byte stability as source provenance.

## Acceptance criteria

- Artifacts built from checkout A cannot be accepted as evidence for checkout B or a changed working tree.
- Build input and output provenance is captured by a controlled producer and rechecked around the tour.
- Tests cover changed commits, dirty source, modified/added/deleted build files, and interrupted builds.
- The supervisor uses isolated owned output and preserves existing shared production artifacts.

## Verification

Run synthetic provenance regressions and the seeded, cleaned exact-checkout live browser tour, and report any unavailable integration honestly.
