---
uuid: "knoxx-cms-contract-validation"
title: "Validate the resource repository contract independently of CMS provider"
status: incoming
priority: P2
labels: ["tasks", "5sp", "has-parent", "cms", "resources", "repository", "validation"]
created_at: "2026-08-04T00:00:00Z"
points: 5
category: tasks
---
# Validate the resource repository contract independently of CMS provider

> Parent epic: `knoxx-resource-repository-cms`

## Purpose

The old card treated "CMS validation" as proving legacy OpenPlanner REST routes. That is
now the wrong boundary. Validate the resource repository semantics that CMS-like clients
actually depend on, then run the same compatibility suite against the file/EDN reference
provider and any future remote provider.

The production deploy currently conditionally skips the legacy CMS/OpenPlanner surface;
this task should eliminate the architectural reason that a provider-neutral resource
contract cannot be tested unconditionally.

## Scope

Define/verify contract tests for:

- resolve one resource by canonical identity;
- list/query resources without exposing provider storage layout;
- validated writes and explicit rejection of malformed resources;
- deterministic duplicate/conflict handling;
- stable reference resolution;
- identity/version behavior needed by downstream consumers;
- provider failure/error semantics;
- read-after-write behavior where the contract promises it.

### Provider-neutral conflict contract

Every write/replace request carries a canonical resource identity, canonical validated
payload, and an `expected-version` precondition. Providers must return the same semantic
outcome and error shape:

- creating an absent identity with no expected version returns `:created` and an opaque,
  immutable version token;
- retrying the same identity and canonically equal payload returns `:unchanged`, the
  existing resource, and the same version without another write;
- creating the same identity with a different payload returns
  `{:error/type :resource/conflict :error/reason :identity-exists
  :resource/id <identity> :expected-version nil :actual-version <version>}`;
- replacing with the current version and a changed valid payload returns `:updated` and a
  new version;
- replacing with a stale or unknown version returns
  `{:error/type :resource/conflict :error/reason :stale-version
  :resource/id <identity> :expected-version <expected> :actual-version <actual>}` and
  leaves bytes, semantic payload, provenance, and version unchanged;
- replacing with the current version and an equal payload returns `:unchanged` with the
  same version.

Compatibility tests must assert the complete conflict data above, not only that an
exception occurred. They must also re-read after every duplicate or conflict and prove
that no authority changed.

Run the same semantic suite against:

1. an in-memory fake/reference implementation;
2. the file/EDN provider from `knoxx-file-resource-repository-provider`.

A future Optimizely/remote provider should be able to join the suite without changing the
assertions.

## Production verification

Replace the old "CMS exists only when OpenPlanner REST happens to be reachable" proof
with a provider-neutral health/contract proof for the repository implementation Knoxx is
actually configured to use.

Do not require a browser page to prove repository health.

## Explicitly out of scope

- publication reconciliation/effects;
- translation/transduction provider behavior;
- SME evaluation workflow;
- HTML/React/static rendering;
- Optimizely implementation itself.

## Done when

- The repository compatibility suite fails for semantic contract violations regardless of
  provider implementation.
- The file/EDN provider passes the same suite as the fake provider.
- Exact duplicate retries are idempotent; identity collisions and stale-version writes
  return the provider-neutral conflict shape and preserve the prior resource.
- Production verification exercises the configured repository boundary unconditionally,
  rather than skipping because OpenPlanner REST is absent.
- No test defines "CMS correctness" as the continued existence of legacy OpenPlanner
  HTTP routes.

## Related board work

- `knoxx-file-resource-repository-provider` provides the first real implementation.
- `knoxx-cms-resource-backed-publication-ui` remains a publication/UI integration card,
  not the definition of this repository contract.
- `knoxx-folder-backed-visual-cms-design-spec` remains iceboxed; visual editing is not a
  prerequisite for this proof.
