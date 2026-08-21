---
uuid: knoxx-publication-live-verification-contract
title: Publication — Authoritative Live Verification Contract
status: accepted
priority: P1
points: 2
labels:
  - tasks
  - publication
  - verification
  - has-parent
---

# Publication — Authoritative Live Verification Contract

> Parent epic: `knoxx-publication-runtime-follow-up`

## Purpose

Consolidate the production-boundary lessons from publication closeout into one repeatable verifier that cannot false-green required surfaces.

## Dependencies

Run after the lossless-state, lifecycle-separation, and Gardens-decoupling slices so the verifier describes the target architecture rather than temporary seams.

## Work

- Cover required publication routes, immutable identity, manifest preservation, HTTP-only startup, and garden publication/viewing.
- Restrict required route success sets explicitly; unexpected 4xx/5xx responses fail.
- Model PASS / WARN / FAIL explicitly. Accepted deferred behavior may WARN but cannot create an unavoidable hard failure.
- Prove which backend checkout/deployment is under test before browser/API assertions run.
- Ensure signal/termination/error paths propagate nonzero status.
- Keep live verification side-effect bounded and leave no publication fixtures/state behind unless explicitly retained for diagnosis.

## Definition of Done

- Required probes fail on unexpected 4xx/5xx responses.
- Verifier proves backend/deployment identity.
- Signal and internal-error paths cannot return false success.
- Deferred checks are explicit WARNs with ownership, not hidden passes or permanent failures.
- A clean live production-boundary run passes against the target deployment.
- Verification evidence is reproducible from documented commands.
