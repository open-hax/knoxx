# Π Fork Tax 2026-06-08T00:00:00Z — infra fixes + frontend refactor

## Snapshot
- **Branch:** `fix/frontend-es2022-lib`
- **Base target:** `origin/fix/frontend-es2022-lib` (2 commits ahead)
- **Scope:** Backend infra fixes, frontend ES2022 lib + source doc refactor, contract updates, kanban task hygiene, ingestion clj-kondo imports

## Changed

1. **backend-infra** — 5 modified files in `backend/src/cljs/knoxx/backend/infra/` covering agent turn, openplanner memory, and mongo stores (memory sessions, session store, session titles).
2. **contracts** — 8 modified EDN files across `contracts/agents/`, `contracts/capabilities/`, `contracts/fork-tales/`, `contracts/roles/`.
3. **frontend** — Extensive changes:
   - `package.json`, `shadow-cljs.edn` build config updates
   - `bridge/index.ts`, `core.cljs` runtime wiring
   - Admin page components, workspace-context utils + tests
   - API admin module + tests
   - Document links removed (deleted `document-links.ts` + test)
   - `SourceDocPage.tsx` refactored, `ForumThreadView.tsx` deleted
   - New `frontend/src/cljs/knoxx/frontend/lib/` directory
   - New `frontend/src/cljs/knoxx/frontend/pages/source_doc/` directory
   - New `SourceDocPage.test.tsx`, `frontend/test/` directory
4. **kanban** — 33 task files updated for hygiene.
5. **ingestion-clj-kondo** — New clj-kondo import configs (malli, next.jdbc, babashka/fs, rewrite-clj).

## Verification
- Prior Π commit `4a241e71` verified tests passing. No test command run this session.

## Concurrent Dirt
None. All working tree changes are owned by this snapshot.

## Blocked
- `.claude/` — agent runtime state (`scheduled_tasks.lock`); intentionally excluded.

## Follow-up
- Push branch + Π tag to origin.
