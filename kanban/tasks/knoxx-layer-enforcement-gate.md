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

- Start as a **ratchet, not a cliff.** A hard gate on day one fails the build
  immediately and gets disabled; a ratchet gets adopted.
- **Ratchet on violation identity, not on a count.** A count-only gate passes
  when one allowlisted violation is removed and a different one is added in the
  same commit — a new violation lands green, which is the exact failure the gate
  exists to prevent. So: fail on any violation absent from the allowlist, and
  separately require the allowlist to shrink or hold. The count is then a
  progress number for humans, never the pass condition.
- A violation's identity has to survive edits that are not violations. Key it on
  `[namespace, required-namespace]` rather than on a line number or a hash of
  surrounding code, or every unrelated edit churns the allowlist and the gate
  becomes noise people silence.
- Allowlist existing violations explicitly, with the file and reason, so the
  backlog is legible and shrinking is visible.
- Reuse the existing checker style (`scripts/check-*.mjs`) rather than inventing
  a new tool. `code-quality.yml`-style Python in the infra repo is the sibling
  precedent.

## Known violations to seed the allowlist

- `infra.routes.mcp` — ~160 raw interop expressions. Partly addressed: the
  consent page and the transport are now their own namespaces and the file is
  734 lines, but the raw interop density is untouched. Worth knowing which
  ceiling is real here: clj-kondo enforces file error at 800 and **function
  error at 60**, and it was the function rule that actually bit. A separate
  `size-lint.config.mjs` documents warn 350 / error 500, but that config was
  deleted by a fork-tax commit (`b3348eb1`), so `lint:size` cannot run at all —
  recovering it reports **50 errors repo-wide**, which is itself an argument for
  the ratchet rather than a cliff.
- **The tool implementations are effectful namespaces filed under `domain.*`.**
  `domain.bluesky.bluesky`, `domain.discord.tools`, `domain.media` and
  `domain.twitch` now require `infra.actor.credentials`, because the credential
  resolver moved to `infra` where it belongs — it reads the policy database. Those
  four `domain -> infra` edges are **newly visible, not newly created**: they
  already called a function that performed a database read, through a namespace
  named `domain.actor.credentials` that hid it. Seed them in the allowlist and
  fix them by moving the tool implementations to `infra`, or by passing resolved
  credentials in, rather than by moving the resolver back.

- ~~`domain.actor.credentials` requires `infra.auth.authz` and `infra.db.policy`~~
  — resolved 2026-08-05 by moving it to `infra.actor.credentials`. Kept here as
  the worked example: a violation can be *hidden* by a namespace name, and the
  gate must key on the actual require graph, not on where someone filed a file.

- Contract validation flows through `open-hax.contract-runtime`, not
  `katamorph.schema`; knoxx does not reference katamorph at all. Out of scope
  here (see `knoxx-katamorph-cutover` upstream) but the gate should not pretend
  otherwise.

## Done when

- A layer violation in new code fails CI.
- The current violations are enumerated in an allowlist, with counts.
- The count can only go down.
