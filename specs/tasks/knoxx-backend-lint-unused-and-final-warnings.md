# Knoxx Backend Lint — Final Warning Cleanup

Date: 2026-05-27
Status: todo
Parent epic: `specs/epics/knoxx-backend-cljs-lint-remediation.md`
Story points: 5

## Purpose

Eliminate the remaining lint warnings after hard errors, coverage-backed refactors, route hook checks, function-length extraction, async workflow cleanup, and test-boundary work have reduced the noisy categories.

## Problem

The current lint output includes smaller warning classes that should run late because many will disappear as earlier refactors remove or move code:

- 38 private-var access warnings;
- 7 redundant `let` warnings;
- 4 unresolved namespace warnings;
- 2 complexity warnings;
- 1 duplicate require warning;
- 75 other warnings, many likely unused vars/requires or redundant expressions.

Cleaning these first would create churn without addressing the architectural causes.

## Goals

1. Remove unused requires/refers, unused private vars, and unused bindings.
2. Remove redundant `let`, `do`, and duplicate require forms.
3. Resolve remaining unresolved-var/namespace warnings that did not block as errors.
4. Collapse remaining complexity warnings by extracting named helpers if the earlier function-length task did not touch them.
5. Confirm no Promise-chain or function-length warnings remain after final cleanup.

## Non-goals

1. Large architectural moves; those belong in earlier tasks.
2. Promise-chain conversion except for stragglers.
3. Lowering warning severity.
4. Deleting potentially live code without checking references.

## Cleanup rules

- Run `rg` before deleting private vars that may be macro-referenced or exported indirectly.
- Prefer restructuring bindings over introducing `_`-prefixed names that later get used.
- Keep public vars if they are part of a documented external API, but add usage/tests or move them to a clearer namespace if appropriate.
- Do not add broad `:refer :all` while fixing require warnings.
- Run targeted lint on touched files before the full backend lint gate.

## Verification

Iterate with:

```bash
pnpm -C backend exec clj-kondo --lint <touched-files>
```

Final for this task:

```bash
pnpm -C backend lint
```

If source behavior changes, run:

```bash
pnpm -C backend exec shadow-cljs compile test
```

## Definition of done

- No unused-binding, unused-require, unused-private-var, redundant-expression, missing-else, unresolved-var, unresolved-namespace, duplicate-require, or complexity warnings remain.
- Any public-looking but currently unused API is either covered by tests/references or documented as intentionally retained.
- `pnpm -C backend lint` has no warnings from this category.
