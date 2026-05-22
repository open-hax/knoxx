# Knoxx Backend Lint — Unused Symbols and Final Warning Cleanup

Date: 2026-05-21
Status: todo
Parent epic: `specs/epics/knoxx-backend-cljs-lint-remediation.md`
Story points: 5

## Purpose

Eliminate the remaining non-error lint warnings after hard errors, route symbol drift, function-length errors, Promise chains, and test boundary issues are addressed.

## Problem

The current lint output includes roughly 1605 unused binding/require/private-var diagnostics plus smaller groups of redundant expressions, missing else branches, unresolved vars, and protocol warnings. Many may disappear after earlier refactors, so this task should run late.

## Goals

1. Remove unused requires/refers, unused private vars, and unused bindings.
2. Replace intentionally unused destructured names with `_`-prefixed bindings only where allowed by current lint policy, or restructure the binding.
3. Remove redundant `let`, `do`, and nested `str` forms.
4. Fix missing else branches or make intentional nil returns explicit.
5. Resolve remaining unresolved-var/namespace warnings that did not block as errors.

## Non-goals

1. Large architectural moves; those belong in earlier tasks.
2. Promise-chain conversion except for stragglers.
3. Lowering warning severity.
4. Deleting potentially live code without checking references.

## Cleanup rules

- Run `rg` before deleting private vars that may be macro-referenced or exported indirectly.
- For unused bindings in callbacks, prefer changing arity/destructuring to avoid introducing `used-underscored-binding` warnings.
- Keep public vars if they are part of a documented external API, but add usage/tests or move them to a clearer namespace if appropriate.
- Do not add broad `:refer :all` while fixing require warnings.

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

- No unused-binding, unused-require, unused-private-var, redundant-expression, missing-else, unresolved-var, or protocol-mock warnings remain.
- Any public-looking but currently unused API is either covered by tests/references or documented as intentionally retained.
- `pnpm -C backend lint` has no warnings from this category.
