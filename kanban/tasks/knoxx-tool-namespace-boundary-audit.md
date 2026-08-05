---
uuid: "knoxx-tool-namespace-boundary-audit"
title: "Audit the tool namespaces against the four-category split before extracting anything"
status: incoming
priority: P2
labels: ["tasks", "8sp", "has-parent", "decouple", "architecture"]
created_at: "2026-08-04T00:00:00Z"
points: 8
category: tasks
---
# Audit the tool namespaces against the four-category split

> Parent epic: `knoxx-decouple-into-katamorph-contracts`
> This is the enabling card. Extraction of any tool set should wait on its slice.

## Purpose

AGENTS.md declares a four-category split — `domain` (pure), `infra` (effectful),
`shape` (structure-only), `law` (contracts) — plus `extern.*` as the only place
raw JS interop may be born. The tool code mixes these. Nothing can be extracted
into a Katamorph contract or an eta-mu package until each tool set's boundary is
named, because a namespace that only has behaviour cannot be moved by anyone but
its author.

## Evidence that this is the live problem

Getting the MCP endpoint working surfaced eight defects, and **five were the same
shape**: a writer and a reader that only ever ran together against live
infrastructure, so their contract drifted silently.

- `set-client!` wrapped its payload in `:client_data`; `get-client!` returned the
  envelope. Every registered client's `redirect_uri` was rejected.
- The writers stored `:expiresAt`; the readers asked `:expires-at`. Every code
  and token read as expired.
- The consent page read a CLJS map with `aget`, so it 500'd.
- `clj->js {:sessionIdGenerator js/undefined}` emits `null`, which selects the
  SDK's *stateful* mode. Every request after `initialize` was rejected.
- A nested tool-parameter object was handed to `registerTool` as a bare field
  shape, so tool registration threw.

Each was a boundary nobody had declared. The existing `law.*` and `shape.*`
namespaces are broadly fine — the problem is what never reached them.

## Scope

Per tool set (discord, bluesky, sandbox, voice, translations, memory/graph, CMS):

- List its namespaces and classify each as domain / infra / shape / law / extern.
- Name the violations concretely — a pure decision living in `infra`, raw interop
  outside `extern`, a contract implied but never written.
- For each boundary the set crosses, say what the contract *would* be. Writing it
  is a follow-up; naming it is this card.
- Produce a per-set extraction order, cheapest first.

## Known, already named

- `infra.routes.mcp` holds ~160 raw interop expressions in 799 lines, against an
  800-line lint **error** ceiling — comments have been trimmed three times to
  fit. It is two concerns in one file (OAuth authorization server + MCP
  transport) and wants splitting.
- A URL adapter and a TypeBox→Zod extern were both raised in review and deferred
  as out of scope for a hotfix. Both belong here.
- `mcp-sessions*` is read by `GET /mcp` and `DELETE /mcp` and **written by
  nothing** (`git log -S'swap! mcp-sessions*'` returns no commits). Those two
  routes cannot work. Either implement stateful sessions or delete the routes —
  leaving them is a trap.
- `set-token!`/`get-token!` still pass JSON strings where the rest of the store
  now speaks CLJS data.
- The SDK is unpinned: `backend/Dockerfile` installs with
  `--no-frozen-lockfile` against `^1.29.0`, so production ran **1.30.0** while
  the lockfile pinned **1.29.0**. That is the mechanism for "it broke and nobody
  changed anything" and should be closed regardless of this audit.

## Done when

- Every tool set has a written classification and a named list of violations.
- Every boundary it crosses has a stated contract, written or explicitly deferred.
- There is an extraction order, and the first slice is small enough to ship alone.

## Prior art on this board

- **`knoxx-runtime-decomposition-inventory`** (pending) — *Decomposition
  Inventory: Manifest / Driver / Protocol / Library*. Substantial overlap. Read
  it before starting; this card should extend that inventory with the tool sets
  and the four-category classification rather than start a second one.
- **`knoxx-contract-runtime-extraction`** (pending) — *Extract the Contract
  Runtime Core as a Package*. That is the eta-mu packaging step this audit
  unblocks; sequence after.
