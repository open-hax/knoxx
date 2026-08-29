# Roadmap — knoxx's slice

> Hub: **[eta-mu/ROADMAP.md](https://github.com/open-hax/eta-mu/blob/main/ROADMAP.md)** — read that for the seam, the ownership
> table, and the sequencing rule. This file is only knoxx's slice.
> Board: `kanban/{epics,tasks}/`. Last surveyed: 2026-08-29.
>
> That link 404s until [eta-mu#167](https://github.com/open-hax/eta-mu/pull/167)
> merges — the hub is written and on that branch, not yet on `main`. The path is
> the one it will land at, so it is left as-is rather than pointed at a branch
> that would rot; delete this note when the PR merges.

## What knoxx is, on this roadmap

**A later application composition using stable upstream parts.** It does **not**
own the upstream seams while they are still moving.

## The rule to not break

> Knoxx is a downstream composition target. It remains deferred until these parts
> work independently; **code should not be migrated into or out of Knoxx to prove
> this architecture.**
> — `muse/docs/design/contract-ownership-and-host-translation.md`

Knoxx cuts over **last** — `eta-mu:knoxx-katamorph-cutover`, iceboxed by design.
Two extractions out of knoxx have already succeeded (`eta-mu:sol-extraction`,
`eta-mu:chat-ui-extraction`), so the temptation to do a third is real. Resist it
until the upstream criteria close.

## What knoxx should do now

Everything achievable **without moving code**: become internally lawful and
actor-aware, so extraction later is mechanical rather than archaeological.

The compliance catch-all and the domain capability work are separate. Epic
**`knoxx-decouple-into-katamorph-contracts`** keeps its misleading name for
card-link stability, but owns only constitutional compliance; extraction is a
listed non-goal.

| Card | Why |
|---|---|
| **`knoxx-layer-enforcement-gate`** (P1) | The lever. Nothing else sticks without it. Knoxx has 4 gates and none checks layer dependencies. Ratchet, not cliff. |
| `knoxx-mcp-actor-ascription` (P1) | Discord/Bluesky over MCP have **no owning actor**, so credentials throw. Fails safe, but those tools are non-functional remotely. |
| `knoxx-deploy-actor-owning-local-credentials` (P1) | The above is useless in production without an actor that holds credentials. |
| `knoxx-tool-namespace-boundary-audit` | Name each tool set's boundary before anything moves. |
| `knoxx-voice-tools-remote-transport` | Written for an owned realtime harness; MCP cannot steer. |
| `knoxx-tool-vocabulary-rename` | "semantic" names a technique, not a subject. Do after the boundaries exist. |
| `knoxx-mcp-consent-permission-groups` | **Blocked** on `eta-mu:capability-schema-reconciliation` — tool groups *are* capabilities. |

### Parallel bounded capability work

These epics may consume one another's immutable artifacts and receipts, but none owns
another's semantics. In particular, candidate generation is not review, repository
storage is not publication, and concrete layout/rendering is not content authority.

| Capability epic | First bounded cards | Owns |
|---|---|---|
| **`knoxx-transduction-provider-pipeline`** (P1) | `knoxx-translation-transduction-boundary`, `knoxx-translation-config-publication-dependency-removal`, `knoxx-translation-config-trusted-auth-context`, `knoxx-versioned-resolved-translation-config`, `knoxx-translation-pipeline-validation`, `knoxx-translations-event-sourced` | Typed candidate generation, trusted scope, provider policy, immutable attempts, and provenance. |
| **`knoxx-evaluation-review-system`** (P1) | `knoxx-evaluation-case-contracts`, `knoxx-evaluation-mcp-review-flow`; UI adapter deferred | Rubrics, SME judgments/corrections, adjudication, and durable evaluation receipts. |
| **`knoxx-resource-repository-cms`** (P2) | `knoxx-cms-contract-validation`, `knoxx-file-resource-repository-provider`, `knoxx-resource-repository-snapshot-observation` | Provider-neutral resource identity, validation, versioned CRUD, and atomic observations. |
| **`knoxx-representation-output-boundary`** (P3) | `knoxx-react-ssr-representation-provider` | Conversion of resolved semantic/view artifacts into HTML, React, Markdown, PDF, or other concrete forms. |

Publication remains an integration consumer: it resolves publication intent, requires
the relevant evaluation evidence, selects representations, performs effects, and emits
its own receipts without absorbing transduction, evaluation, repository, or layout law.

## Knoxx's position in the drift ledger

- **Consumes katamorph as a pinned Git dependency.** `backend/deps.edn` pins
  `io.github.open-hax/katamorph` at `v0.2.0`, and `backend/shadow-cljs.edn`
  takes its classpath from that alias. The `open-hax.contract-runtime.*`
  requires are now `katamorph.*`; the injected config key is still
  `:contract-runtime/deps`, which katamorph reads under that name.
- Carries `open-hax.contracts.schema` — *byte-identical lineage; katamorph was
  extracted from it; still not cut over.*
- **No longer builds `contract-runtime` from the openplanner copy.** The
  `../../contract-runtime/src/cljs` source path and its CI symlink from
  `openplanner/packages/` are gone. The remaining openplanner sibling link is
  the JS SDK (`backend/package.json`), not the contract runtime.

## Why compliance has not happened by itself

`AGENTS.md` declares the layer split; the code violates it anyway. Contracts here
are **optional config**, whereas in muse the build fails without them. Fix is
enforcement, not documentation — hence the gate card being P1.

The evidence, from getting `/mcp` working: eight defects, **five the same shape** —
a writer and a reader that only ran together against live infrastructure, so
their contract drifted unseen. Every one was an undeclared boundary.

## Postgres

Verified clean: **zero** postgres references in `backend/package.json` or
`backend/src/cljs`. The compose comment claiming pg/redis remain a dependency of
`knoxx-ingestion` appears stale — no pg dep found there. Deleting that comment
is a two-minute win.
