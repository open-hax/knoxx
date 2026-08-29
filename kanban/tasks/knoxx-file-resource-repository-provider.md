---
uuid: "knoxx-file-resource-repository-provider"
title: "Prove a file/EDN resource repository provider behind a provider-neutral contract"
status: incoming
priority: P2
labels: ["tasks", "5sp", "has-parent", "cms", "resources", "repository", "contracts"]
created_at: "2026-08-13T00:00:00Z"
points: 5
category: tasks
---
# Prove a file/EDN resource repository provider behind a provider-neutral contract

> Parent epic: `knoxx-resource-repository-cms`

## Purpose

Make the open-source CMS workflow useful without requiring a bespoke visual editor or a
specific remote CMS. The reference provider stores canonical resource contracts in EDN
files while consumers depend only on a repository boundary.

## Scope

- Define the smallest provider-neutral operations required by current consumers: resolve
  one, list/query, and validated write/replace with an `expected-version` precondition.
- Keep these operations on the same repository authority extended by
  `knoxx-resource-repository-snapshot-observation` (#282). Its `observe-many` operation must
  reuse the fake/file providers and their public versions/authorization rather than wrapping
  sequential resolves in a translation-specific adapter.
- Reuse the existing namespace/resource EDN contract shape rather than introducing a
  second content language.
- Keep filesystem/Git path details inside the provider implementation.
- Treat organization scope as part of every tenant-owned canonical identity. File paths may
  partition scopes internally, but adapters derive scope from authenticated actor context and
  neither list nor resolve may fall back across scopes.
- Keep global-resource authorization policy outside editable resource bytes and file paths.
  The adapter checks authenticated platform capabilities before calling write/replace; the
  global translation config specifically requires `platform.translations.manage`, and a tenant
  administrator cannot self-promote by submitting a global identity or altered policy field.
- Validate writes before they become repository authority.
- Implement the conflict contract from `knoxx-cms-contract-validation`: equality compares the
  complete canonical authority record (validated payload plus domain provenance, excluding
  only contract-declared store envelope metadata). An exact retry is `:unchanged`; a different
  record for an existing identity with no expected version is `:identity-exists`; a changed
  valid record with the current expected version is `:updated`; and a replace whose expected
  version is not current is a `:stale-version` conflict. Both conflicts return the canonical
  `:resource/conflict` error data and leave the file, payload, provenance, and version
  unchanged.
- Make the identity-existence/expected-version decision and durable write one atomic,
  linearizable operation. If two different creates race for one absent identity, exactly one
  returns `:created` and the loser returns `:identity-exists`; equal concurrent creates store
  once and the retry is `:unchanged`. If two replacements race from the same version, exactly
  one may return `:updated`; the loser must observe the new actual version, return
  `:stale-version`, and change no bytes.
- Preserve concurrent writes to different resource identities that share one namespace
  manifest. Use atomic sibling preservation or manifest-level compare-and-swap/retry so a
  read-modify-write cycle cannot silently erase another resource's accepted update.
- Make manifest/revision publication crash-safe and cross-process, not merely protected by an
  in-memory lock. Stage and validate complete bytes, durably publish one atomic replacement,
  and acknowledge success only after the new authority is readable; an injected crash may
  expose the old or new complete revision, never a partial/corrupt hybrid.
- Expose resource-scoped versions, not namespace-file hashes, mtimes, or manifest revisions.
  Updating one sibling may rotate internal storage metadata but must leave every untouched
  sibling's public version token identical along with its payload and provenance.
- Persist immutable resource revisions so references pin `{resource/id, version}` rather than
  floating current values. Resolve the complete transitive closure in deterministic order,
  return its digest, retain older targets after updates, and reject missing pinned versions or
  cycles with the provider-neutral invalid-reference data before modifying authority.
- Authorize every direct/transitive reference target under the authenticated actor context
  before checking its existence/version: tenant targets must match the actor's organization and
  global targets must pass server-owned read policy. Root authorization never delegates target
  access. Existing/missing foreign targets return the same non-enumerating denial on write and
  resolve, expose no closure/version facts, and leave root/current/history authority unchanged.
- Preserve enough version/provenance information for downstream publication,
  transduction, and evaluation consumers to bind to immutable revisions where required.
- Prove the boundary using an in-memory/fake provider plus the real file provider.

## Provider compatibility

A future Optimizely provider should be able to implement the same semantic operations.
Compatibility tests must therefore assert behavior and contract shape, not filesystem
layout. The multi-resource observation extension in #282 adds its concurrency/absence suite
to this same provider-compatibility boundary.

## Non-goals

- Implementing Optimizely in this card.
- Building a visual page editor.
- Owning publication effects, translation/transduction, evaluation, or HTML rendering.
- Exposing raw EDN parsing requirements to browser clients.

## Done when

- A resource consumer can run the same contract tests against a fake and the file/EDN
  provider.
- Same-name resources in two organization scopes remain isolated for list, resolve, writes,
  history, and reference closure; authenticated adapters reject cross-tenant access without
  revealing whether the target exists.
- Global translation-config tests distinguish permitted reads from platform-only writes and
  prove a denied tenant-admin mutation leaves bytes, provenance, and version unchanged.
- Canonical resource identities and validation behavior are provider-independent.
- File-backed resources can be edited through a narrow write operation and immediately
  re-read through the same repository contract.
- Fake and file providers return identical outcomes for create, equal retry, valid
  compare-and-swap replace, provenance-only change, identity collision, and stale replace.
- Same-identity concurrent create cases prove changed payloads yield one `:created` plus one
  `:identity-exists`, while equal payloads store once and return one `:unchanged`. A concurrent
  replacement case proves exactly one update wins and one returns `:stale-version`; a
  different-identity concurrent case proves both accepted sibling writes survive and can be
  re-read from the shared namespace manifest, with each untouched sibling retaining its exact
  prior resource version.
- A pins B1, B advances to B2, and A@A1 still resolves the exact B1 closure/digest. Missing
  pinned revisions and cycles fail without changing either current or historical authority.
- Direct and transitive foreign-reference fixtures (both existing and missing) are denied
  before lookup with identical observable results; permitted/denied global reference targets
  follow server-owned read policy and no denial leaks closure/version facts.
- Cross-process race and injected-crash tests observe only complete old/new manifests and
  never acknowledge a write before its resource revision and closure can be re-read.
- No consumer needs to know a resource came from disk in order to use it.
