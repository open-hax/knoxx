---
priority: "P4"
---

# Knoxx Kanban

Board state for the Knoxx agent. These markdown cards are the **source of truth**
for Knoxx work items — they are edited in place, not regenerated.

> History: this board was originally imported from a `specs/` tree via
> `scripts/import-kanban-specs.mjs`. As of 2026-05-28 the `specs/` tree and the
> importer were retired; the cards' `source:` frontmatter now points at paths that
> only exist in git history / fork-tax tags. Edit cards directly going forward.

## Layout

- `epics/` — epic wrappers / design specs
- `tasks/` — executable work items (with `tasks/ingestion/` for ingestion work)
- `workbench/` — workbench UX child tasks
- `openhax.kanban.json` — kanban config

## Managing the board

Board state (status, frontmatter, comments, transitions) is owned by **Rheos**,
driven through the `eta-mu kanban` CLI. Point `--tasks-dir` at this `kanban/`
directory, not at the repository root: at the root the scan also picks up
`docs/` and reports several hundred non-card files.

```bash
eta-mu kanban list   --tasks-dir kanban
eta-mu kanban count  --tasks-dir kanban
eta-mu kanban update-status <uuid> <status> --tasks-dir kanban
eta-mu kanban comment <uuid> "note" --tasks-dir kanban
```

`eta-mu-beta` is an older alias of the same CLI; prefer `eta-mu`.

The board uses the `promethean` FSM (`openhax.kanban.json`). Valid statuses:
`icebox`, `incoming`, `accepted`, `breakdown`, `blocked`, `ready`, `todo`,
`in_progress`, `testing`, `review`, `document`, `done`, `rejected`.

Rheos records transitions and comments in `.events/ledger.edn`, an append-only,
tracked, one-EDN-event-per-line ledger. Never rewrite or reorder it.

## Run the board UI

The board server and browser UI ship with Rheos. In the Foresight workspace this
is `eta-mu/packages/rheos`, which carries its own PM2 `ecosystem.config.cjs`
(and the standalone `open-hax/rheos` repository). Serve this board by pointing
Rheos at this directory with `--tasks-dir`.

## Cards that belong elsewhere

Some cards here describe work owned by other repositories (osmos, chat-ui, uxx,
services, OpenPlanner, and the retired predecessor product line). They are
listed, with the recommended Rheos action for each, in Foresight's
[`docs/lineage/knoxx-documentation-extraction.md`](https://github.com/open-hax/foresight/blob/main/docs/lineage/knoxx-documentation-extraction.md).
None were moved; carry out the moves or closures through Rheos.

---
Triage 2026-05-29: The "readme" uuid resolves to the kanban board's own README.md, which is a meta/layout document describing board structure, CLI usage, and PM2 setup — not an actionable work item. Verdict: rejected (P4). --tasks-dir /home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/kanban
---
