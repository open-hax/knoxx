---
uuid: "knoxx-translation-config-publication-dependency-removal"
title: "Remove translation config's transitive dependency on publication"
status: incoming
priority: P1
labels: ["tasks", "5sp", "has-parent", "translations", "transduction", "config", "boundaries"]
created_at: "2026-08-29T18:03:05Z"
points: 5
category: tasks
---
# Remove translation config's transitive dependency on publication

> Parent epic: `knoxx-transduction-provider-pipeline`
> GitHub issue: [#273](https://github.com/open-hax/knoxx/issues/273)

## Purpose

Make the existing Knoxx-owned translation configuration boundary independently usable by
transduction. The dependency is wider than one validation helper:

- `law.translation-config` imports publication law for `NonBlankString` and `Locale`;
- `domain.translation-config` imports it for `assert-valid!`; and
- the existing `infra.routes.translation-config/resolved-config!` adapter reaches publication
  transitively through `domain.resources.loader` -> `domain.contracts.loader` ->
  `law.contracts` -> `law.publication`.

A provider-selection proof with the publication subsystem absent therefore cannot compile
honestly until the complete namespace closure is repaired.

## Scope

- Move `NonBlankString`, `Locale`, and generic schema validation to neutral owners shared by
  publication and translation config, or let translation config own the generic pieces it
  needs. Preserve the current Malli schemas and error shape.
- Remove the eager publication dependency from the existing resource/contract-loader path.
  Use a neutral resource-schema registry, injected kind validator, or equivalent boundary;
  do not replace the loader with a translation-only shadow implementation.
- Update `law.translation-config`, `domain.translation-config`, and their callers without
  creating a second resolved-config adapter.
- Retain `knoxx.backend.infra.routes.translation-config/resolved-config!` as the one existing
  Knoxx-owned integration boundary.
- Preserve valid/invalid configuration outcomes and exact error data.
- Add a transitive namespace-closure regression from each of `law.translation-config`,
  `domain.translation-config`, and `infra.routes.translation-config`. Every publication-owned
  law, runtime/orchestration, route, store, and resource namespace is forbidden.

## TDD / proof

1. Capture current valid and invalid translation-config outcomes, including exact error data.
2. Make the namespace-closure test fail separately on the direct law/domain imports and the
   adapter's transitive loader path.
3. Neutralize the scalar/locale/validation ownership and prove exact behavior parity.
4. Decouple the shared resource-loader graph without bypassing the existing adapter or
   weakening validation of non-translation resource kinds.
5. Compile and exercise the existing `infra.routes.translation-config/resolved-config!`
   adapter with every publication-owned namespace/source unavailable.
6. Run the full resource/contract loader and backend suites so the neutral registry cannot
   silently stop validating publication resources in normal deployments.

## Non-goals

- Changing provider-selection policy or configuration schema.
- Adding provider invocation, publication admission, evaluation, or rendering behavior.
- Introducing a parallel `resolved-translation-config!` adapter.
- Removing publication-resource validation from the shared loader.
- Emitting immutable resolved-config provenance; that separate consumer is
  `knoxx-versioned-resolved-translation-config` (#275).
- Implementing the broader pipeline proofs owned by
  `knoxx-translation-pipeline-validation`.

## Done when

- The complete transitive namespace closure from all three entry points contains no
  publication-owned namespace.
- Valid/invalid behavior and error data match the captured contract.
- The existing `resolved-config!` adapter succeeds with publication entirely absent.
- Normal full-loader behavior still validates publication resources.
- The namespace regression, unit/integration tests, backend compile, and backend suite pass.
- `knoxx-versioned-resolved-translation-config` can evolve the adapter without an implicit
  publication dependency.
