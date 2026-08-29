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
- Reuse the existing namespace/resource EDN contract shape rather than introducing a
  second content language.
- Keep filesystem/Git path details inside the provider implementation.
- Validate writes before they become repository authority.
- Implement the conflict contract from `knoxx-cms-contract-validation`: an exact-payload
  retry is `:unchanged` with the existing version; a different payload for an existing
  identity with no expected version is an `:identity-exists` conflict; a changed valid
  payload with the current expected version is `:updated`; and a replace whose expected
  version is not current is a `:stale-version` conflict. Both conflicts return the canonical
  `:resource/conflict` error data and leave the file, payload, provenance, and version
  unchanged.
- Make the expected-version decision and durable write one atomic, linearizable operation.
  If two replacements race from the same version, exactly one may return `:updated`; the
  loser must observe the new actual version, return `:stale-version`, and change no bytes.
- Preserve concurrent writes to different resource identities that share one namespace
  manifest. Use atomic sibling preservation or manifest-level compare-and-swap/retry so a
  read-modify-write cycle cannot silently erase another resource's accepted update.
- Preserve enough version/provenance information for downstream publication,
  transduction, and evaluation consumers to bind to immutable revisions where required.
- Prove the boundary using an in-memory/fake provider plus the real file provider.

## Provider compatibility

A future Optimizely provider should be able to implement the same semantic operations.
Compatibility tests must therefore assert behavior and contract shape, not filesystem
layout.

## Non-goals

- Implementing Optimizely in this card.
- Building a visual page editor.
- Owning publication effects, translation/transduction, evaluation, or HTML rendering.
- Exposing raw EDN parsing requirements to browser clients.

## Done when

- A resource consumer can run the same contract tests against a fake and the file/EDN
  provider.
- Canonical resource identities and validation behavior are provider-independent.
- File-backed resources can be edited through a narrow write operation and immediately
  re-read through the same repository contract.
- Fake and file providers return identical outcomes for create, equal retry, valid
  compare-and-swap replace, identity collision, and stale replace.
- A same-identity concurrent replacement case proves exactly one update wins and one returns
  `:stale-version`; a different-identity concurrent case proves both accepted sibling writes
  survive and can be re-read from the shared namespace manifest.
- No consumer needs to know a resource came from disk in order to use it.
