---
uuid: "knoxx-arch-migration-devel-smoke-verification"
title: "Architecture Migration — Devel Stack Smoke Verification"
status: breakdown
priority: P2
labels: ["tasks", "2sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 2
category: tasks
---
# Architecture Migration — Devel Stack Smoke Verification

> Parent epic: `knoxx-knowledge-ops-architecture-migration`
> Points: 2

## Purpose

Run a structured smoke-test pass against the local devel stack after the architecture migration slices land, confirming that the knoxx backend, frontend, and proxx integration are coherent end-to-end with no regressions from the route retirements or client changes.

## Scope

- Verify `GET /health` and `GET /api/knoxx/health` both return 200 with truthful dependency fields (proxx + openplanner reachable)
- Verify `GET /api/data/health` returns the correct subsystem fan-out response
- Verify the new CMS domain routes respond correctly (create/read/update/delete document round-trip using `curl` or a short script)
- Verify `auto:cephalon` model routing resolves without error through the proxx client (can be confirmed via a one-shot `POST /v1/chat/completions` with `"model": "auto:cephalon"`)
- Document results in a comment on this task (using `eta-mu kanban comment`)

## Definition of done

- All four health-check variants return expected HTTP status codes with non-stub dependency fields
- At least one CMS document can be created and retrieved via the new routes without a 404 or 500
- A proxx chat-completions call with `auto:cephalon` model does not return a model-not-found or routing error
- Findings (pass/fail per check, any errors seen) are recorded as a kanban comment on this task

## Notes

Split from parent epic `knoxx-knowledge-ops-architecture-migration` on 2026-05-30.
