---
uuid: "knoxx-translation-config-publication-dependency-removal"
title: "Remove the translation-config dependency on publication law"
status: incoming
priority: P1
labels: ["tasks", "3sp", "has-parent", "translations", "transduction", "config", "boundaries"]
created_at: "2026-08-29T18:03:05Z"
points: 3
category: tasks
---
# Remove the translation-config dependency on publication law

> Parent epic: `knoxx-transduction-provider-pipeline`
> GitHub issue: [#273](https://github.com/open-hax/knoxx/issues/273)

## Purpose

Make the existing Knoxx-owned translation configuration boundary independently usable by
transduction. `knoxx.backend.domain.translation-config` currently requires
`knoxx.backend.law.publication` only for the generic `assert-valid!` helper, so a proof that
provider selection works with publication absent cannot compile honestly.

## Scope

- Move the generic validation helper to a domain-neutral law/validation boundary, or make
  translation-config own the validation call.
- Update `knoxx.backend.domain.translation-config` and its callers without creating a second
  resolved-config adapter.
- Retain `knoxx.backend.infra.routes.translation-config/resolved-config!` as the one existing
  Knoxx-owned integration boundary.
- Preserve valid/invalid configuration outcomes and exact error data.
- Add a namespace-graph regression test that forbids translation-config from requiring
  publication law, runtime/orchestration, routes, stores, or resources.

## TDD / proof

1. Capture current valid and invalid translation-config outcomes, including exact error data.
2. Make the namespace-dependency test fail on the current publication-law require.
3. Move/own validation at the neutral translation boundary and make both tests pass.
4. Exercise the existing `infra.routes.translation-config/resolved-config!` adapter with all
   publication namespaces/resources absent.
5. Compile and run backend tests so callers cannot retain a hidden dependency.

## Non-goals

- Changing provider-selection policy or configuration schema.
- Adding provider invocation, publication admission, evaluation, or rendering behavior.
- Introducing a parallel `resolved-translation-config!` adapter.
- Implementing the broader pipeline proofs owned by
  `knoxx-translation-pipeline-validation`.

## Done when

- Translation configuration validation has no publication namespace dependency.
- Valid/invalid behavior and error data match the captured contract.
- The existing `resolved-config!` adapter succeeds with publication entirely absent.
- The namespace regression, unit tests, backend compile, and backend suite pass.
- `knoxx-translation-pipeline-validation` can begin proof 2 without an implicit prerequisite.
