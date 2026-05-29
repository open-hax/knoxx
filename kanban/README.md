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

Use the `eta-mu-beta kanban` CLI (the kanban-capable binary), pointing `--tasks-dir`
at the repo root so it resolves `kanban/openhax.kanban.json`:

```bash
eta-mu-beta kanban list   --tasks-dir <knoxx-repo-root>
eta-mu-beta kanban count  --tasks-dir <knoxx-repo-root>
eta-mu-beta kanban update-status <uuid> <status> --tasks-dir <knoxx-repo-root>
eta-mu-beta kanban comment <uuid> "note" --tasks-dir <knoxx-repo-root>
```

Valid statuses: `icebox`, `incoming`, `accepted`, `breakdown`, `ready`, `todo`,
`in_progress`, `review`, `document`, `done`, `rejected`.

## Run the board UI

The kanban service is managed by PM2:

```bash
cd services/eta-mu/kanban
pm2 start ecosystem.config.cjs
# http://127.0.0.1:8787
```

Or run directly:

```bash
cd orgs/open-hax/eta-mu/packages/kanban
pnpm build
node dist/cli.js serve --tasks-dir /home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/kanban
```
