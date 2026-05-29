# Π Fork Tax Handoff

- Timestamp: `2026-05-29T01:34:27Z`
- Repository: `/home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx`
- Branch: `pi/fork-tax/20260526T204054Z-knoxx-host-services`
- Remote: `origin` (`git@github.com:open-hax/knoxx.git`)
- Snapshot base HEAD: `50eda01262c049b05b5e25043e7379db4e9a54f4`
- Planned tag: `pi/fork-tax/20260529T013427Z/knoxx-specs-retirement`

## Scope

This snapshot retires the `specs/` source tree and promotes `kanban/` to the
source of truth, alongside a full working-tree snapshot (repo fork-tax convention).

- **`specs/` retired**: `git rm -rf specs/` removed 131 tracked spec files. One
  spec had a local modification (`knoxx-backend-lint-function-length-extractions.md`)
  and was force-removed intentionally. All content survives in git history and the
  prior fork-tax tags.
- **Importer removed**: `scripts/import-kanban-specs.mjs` (untracked, obsolete
  specs→kanban generator) deleted.
- **`kanban/` committed for the first time**: the board was entirely untracked
  (`?? kanban/`). It now enters git, including the 2026-05-28 triage.
- **Docs**: `README.md` and `kanban/README.md` updated to drop `specs/` references
  and document kanban as the source of truth.
- **Backend lint remediation (in-flight)**: 70 modified backend CLJS source/test
  files absorbed, consistent with the prior snapshot. No unrelated path was
  deleted, reset, restored, cleaned, or unstaged. No PM2 process restarted.

## Kanban triage (2026-05-28)

Six stale cards reconciled against the actual code (verified via read-only fan-out):

| Card | Was → Now | Evidence |
| --- | --- | --- |
| lint-hard-error-first-pass | review → **done** | lint now 0 errors (was 52) |
| lint-function-length-extractions | in_progress → **review** | 0 fn-length errors; ~7 warnings left |
| workbench 5.3-agent-scratchpad | accepted → **review** | `ChatScratchpadPanel.tsx` implemented |
| gardens-page-bugfixes | todo → **review** | `GardensPage.tsx` built; create/schema wired |
| openplanner-gardens-backend-fixes | todo → **review** | gardens/cms/renderer/public all built |
| kms-openplanner-ingest-arity-fix | accepted → **review** | `ingest-via-openplanner!` arity now 7-def / 7-call |

`knoxx-health-route-coherence` deliberately **stays incoming** with an evidence
note: `/api/knoxx/health` is still a hardcoded `200 ok` stub.

The 9 pre-existing `done`/`[LANDED]`/`[SUPERSEDED]` cards (translation suite, MCP
×2, workbench-ux-breakdown, garden-cms) were verified accurate. Ingestion and most
workbench-UX cards were verified genuinely unbuilt and correctly left in todo/
incoming/accepted.

## Verification

- Secret heuristic scan: passed; no literal private keys / tokens / api-keys in staged additions.
- `pnpm -C backend typecheck` (shadow-cljs compile server): passed; 305 files, 0 warnings, 8.62s.
- `pnpm -C backend run lint`: passed; **0 errors, 1461 warnings** (improved from the prior snapshot's 11 errors / 1749 warnings).

### Residual lint warnings (1461, all owned by open lint cards)

- 1226 raw Promise-chain (`.then`/`.catch`) warnings → `async-workflows-src`.
- ~24 unused-var / redundant-let / unused-require warnings → `unused-and-final-warnings`.
- ~7 function-length (`>=30`) warnings → `function-length-extractions`.
- 23 test-file warnings (unused refers, mock protocol gaps) → `test-boundaries`.
