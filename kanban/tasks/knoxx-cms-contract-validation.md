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
- writing an absent identity with a non-nil expected version returns
  `{:error/type :resource/conflict :error/reason :resource-absent
  :resource/id <identity> :expected-version <expected> :actual-version nil}`, performs
  no create, and leaves the identity absent. An expected version is a compare-and-swap
  precondition, never permission to create;
- retrying the same identity and canonically equal payload with either no expected version
  or the current version returns `:unchanged`, the existing resource, and the same version
  without another write;
- creating the same identity with a different payload returns
  `{:error/type :resource/conflict :error/reason :identity-exists
  :resource/id <identity> :expected-version nil :actual-version <version>}`;
- replacing with the current version and a changed valid payload returns `:updated` and a
  new version;
- replacing with a stale or unknown version returns
  `{:error/type :resource/conflict :error/reason :stale-version
  :resource/id <identity> :expected-version <expected> :actual-version <actual>}` and
  leaves bytes, semantic payload, provenance, and version unchanged. This precondition
  check takes precedence over payload equality: a stale expected version returns
  `:stale-version` even when the proposed payload equals the current resource;
- replacing with the current version and an equal payload returns `:unchanged` with the
  same version.

The identity-existence/expected-version decision and durable write are one atomic,
linearizable operation, not a separate read followed by an unconditional write. When two
different payloads concurrently create the same absent identity with no expected version,
exactly one request returns `:created`; the other returns the canonical `:identity-exists`
conflict with the winner's version, and the winner remains authoritative. Concurrent equal
creates store one resource and the retry returns `:unchanged`. When two changed payloads
concurrently replace the same resource from version V, exactly one request returns
`:updated`; the other returns the canonical `:stale-version` conflict with the winner's
version as `:actual-version`, and the winning payload remains authoritative.

Providers that store multiple resource identities in one physical manifest must also
preserve accepted sibling writes. Two concurrent writes to different identities in the same
namespace may not pass their resource checks and then erase one another through
last-writer-wins file replacement. The implementation may use atomic sibling preservation or
manifest-level compare-and-swap/retry, but the provider-neutral observable result is that
both accepted writes remain present and re-readable.

Compatibility tests must assert the complete conflict data above, not only that an
exception occurred. They must also re-read after every duplicate or conflict and prove
that no authority changed. Include both the absent-resource/non-nil-precondition case and
the stale-version/equal-payload case so every provider implements the same precedence
rules. Include different-payload and equal-payload concurrent create cases for one absent
identity, a same-identity concurrent replacement case where one request updates and one
returns `:stale-version`, plus a different-identity concurrent write case where both siblings
survive in a shared manifest.

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
- Exact duplicate retries are idempotent; absent-resource precondition failures, identity
  collisions, and stale-version writes return the provider-neutral conflict shape and
  preserve the prior resource or absence.
- Concurrent compare-and-swap replacement is linearizable, and concurrent accepted writes
  to sibling identities cannot lose either resource through a shared-manifest race.
- Concurrent creates for one absent identity store one authoritative resource: a changed
  loser conflicts and an equal retry is unchanged.
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
