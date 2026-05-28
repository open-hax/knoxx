# Knoxx Backend Lint — Zero Warning Gate

Date: 2026-05-27
Status: todo
Parent epic: `specs/epics/knoxx-backend-cljs-lint-remediation.md`
Story points: 3

## Purpose

Close the lint-remediation epic with a clean, reproducible verification record that proves the backend satisfies Knoxx's zero-warning policy.

## Problem

A lint cleanup is not complete when the most visible errors disappear. Knoxx's house rules require zero warnings in CI, so the final task must rerun the canonical gates from a clean enough working state and record exact evidence.

## Goals

1. Run the canonical backend lint command.
2. Run backend test compile after behavior-affecting changes.
3. Run backend server compile after production-path changes.
4. Record exact command outputs, exit codes, and any remaining blockers.
5. Update the epic/task status notes to reflect completion or remaining work.

## Non-goals

1. Making new code changes beyond final mechanical fixes.
2. Restarting PM2 or runtime processes.
3. Archiving the specs unless the owner explicitly wants archive movement.

## Verification commands

Required:

```bash
pnpm -C backend lint
pnpm -C backend exec shadow-cljs compile test
```

Also required if route registration, bootstrap, production server paths, extern boundaries, stores, or runtime startup code changed:

```bash
pnpm -C backend exec shadow-cljs compile server
```

Recommended evidence capture:

```bash
pnpm -C backend lint 2>&1 | tee /tmp/knoxx-backend-lint-final.log
pnpm -C backend exec shadow-cljs compile test 2>&1 | tee /tmp/knoxx-backend-test-final.log
pnpm -C backend exec shadow-cljs compile server 2>&1 | tee /tmp/knoxx-backend-server-final.log
```

## Acceptance criteria

- `pnpm -C backend lint` exits 0.
- Lint output contains zero errors and zero warnings.
- `shadow-cljs compile test` reports zero test failures and zero test errors.
- `shadow-cljs compile server` completes if required by touched code.
- Any pre-existing unrelated blocker is documented with exact file, line, and command output.

## Definition of done

- Final logs are recorded in task notes, a receipt, or a report artifact.
- The parent epic can be marked `review` or `done` according to project workflow.
- No unverified claim of completion remains in task text.
