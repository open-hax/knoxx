# Π Fork Tax 2026-06-03

## Snapshot
- **Base:** `283d28f1` (pi/fork-tax/20260529T022118Z-main-softreset-all-dirt-knoxx)
- **Scope:** Backend error observability, local password auth, trigger/action task prompt migration, async/await modernization, event normalization, provider tool disable removal, frontend auth, CI/CD workflows
- **Tests:** 456 tests, 1346 assertions, 0 failures, 0 errors
- **Typecheck:** 0 warnings

## Commit Groups

1. **frontend-local-auth** — LoginPage/SignupPage local password UI, shadow-cljs dev proxy fix
2. **backend-local-password-auth** — Auth routes + DB policy for local scrypt password signup/login
3. **error-observability-system** — New error_observatory domain, boundary check script, allowlist baseline
4. **error-surface-integration** — Discord source, event dispatch, source runtime, HTTP, app/tools route error logging
5. **agent-runtime-empty-turn** — Empty turn detection, run reconstruction from Redis, async spawn error recording
6. **provider-tool-disable-removal** — Remove Knoxx-side provider tool model disable; delegate to Proxx
7. **trigger-action-task-prompt-migration** — Move task prompts from agent contracts to trigger/action inputs with audit events
8. **event-normalization** — Dotted JSON event type preservation, fixture-based contract policy tests
9. **async-await-domain** — audio labels, sandbox container, session mycology Promise→async conversion
10. **async-await-infra-routes** — MCP, models, resources, discord-scan, tools, proxy routes Promise→async conversion
11. **ci-cd-workflows** — GitHub Actions deploy production/staging workflows
12. **process-artifacts** — Kanban updates, new task files, docs note, receipts

## Concurrent Dirt
None. All working tree changes are owned by this snapshot.

## Blockers
None.
