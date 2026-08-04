---
uuid: "knoxx-layer-enforcement-gate"
title: "Make the four-category constitution enforceable in CI"
status: incoming
priority: P1
labels: ["tasks", "5sp", "has-parent", "compliance", "architecture"]
created_at: "2026-08-04T00:00:00Z"
points: 5
category: tasks
---
# Make the four-category constitution enforceable

> Parent epic: `knoxx-decouple-into-katamorph-contracts`
> This is the enabling card. Every other compliance card regresses without it.

## Purpose

AGENTS.md declares the layer split and the code violates it anyway. That is not
a discipline problem, it is a feedback problem, and the upstream cutover epic
already named it:

> contract discipline is only load-bearing in muse (build fails without the data
> pipeline); in sol/knoxx contracts are optional config, so agents defined
> schemas in place. **Fix = cutover + make it enforceable, not more
> documentation.**

Muse complies because non-compliance breaks its build. Knoxx will comply for the
same reason and no other.

## What already exists to build on

Knoxx has four gates in `backend/package.json`, so the mechanism is proven:

| gate | checks |
|---|---|
| `lint` | clj-kondo, with `fn-length/too-long` and `file-length/too-long` as **errors** |
| `boundary:check` | `scripts/check-js-boundary.mjs` — non-extern imports of the generic extern helpers |
| `error-boundaries:check` | silent catch sites must log, return data, or rethrow |
| `lint:size` | file size |

None checks **layer dependencies**, which is the rule most often broken.

## Scope

Add a gate that fails on a layer violation. Rules, in dependency order
(`law → shape → extern → domain → infra`):

- `law.*` may require only `law.*` and malli/clojure.* — no I/O, no shape, no domain
- `shape.*` may require only `law.*` + `shape.*` — pure, domain-agnostic
- `extern.*` is the only place raw JS interop may be born or decoded
- `domain.*` may require `law` + `shape` + `domain` — never `infra.*` or `extern.*`
- `infra.*` may require anything below it

Practical shape:

- Start as a **ratchet, not a cliff.** Emit the current violation count, fail
  only on an increase. A hard gate on day one fails the build immediately and
  gets disabled; a ratchet gets adopted.
- Allowlist existing violations explicitly, with the file and reason, so the
  backlog is legible and shrinking is visible.
- Reuse the existing checker style (`scripts/check-*.mjs`) rather than inventing
  a new tool. `code-quality.yml`-style Python in the infra repo is the sibling
  precedent.

## Known violations to seed the allowlist

- `infra.routes.mcp` — ~160 raw interop expressions in 799 lines, against an
  800-line lint error ceiling; two concerns (OAuth authorization server + MCP
  transport) in one file.
- `domain.actor.credentials` requires `infra.auth.authz` and `infra.db.policy` —
  a domain namespace depending on infra.
- Contract validation flows through `open-hax.contract-runtime`, not
  `katamorph.schema`; knoxx does not reference katamorph at all. Out of scope
  here (see `knoxx-katamorph-cutover` upstream) but the gate should not pretend
  otherwise.

## Done when

- A layer violation in new code fails CI.
- The current violations are enumerated in an allowlist, with counts.
- The count can only go down.
