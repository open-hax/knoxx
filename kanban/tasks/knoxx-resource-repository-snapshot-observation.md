---
uuid: "knoxx-resource-repository-snapshot-observation"
title: "Add provider-neutral multi-resource snapshot observations"
status: incoming
priority: P1
labels: ["tasks", "5sp", "has-parent", "cms", "resources", "repository", "snapshots"]
created_at: "2026-08-29T19:05:49Z"
points: 5
category: tasks
---
# Add provider-neutral multi-resource snapshot observations

> Parent epic: `knoxx-resource-repository-cms`
> GitHub issue: [#282](https://github.com/open-hax/knoxx/issues/282)
> Depends on: `knoxx-file-resource-repository-provider`
> Consumer: `knoxx-versioned-resolved-translation-config` (#275)

## Purpose

A sequence of single-resource resolves is not one repository observation. If a requested
resource changes between reads, a consumer can receive a combination that never existed.
An absent optional resource also needs a repository-authoritative observation rather than a
caller-supplied marker.

Extend the existing provider-neutral repository boundary once. Translation config consumes
this generic operation but does not own a second read path, shadow store, or precedence
implementation.

## Contract

Add one `observe-many` operation over authenticated actor context and a non-empty canonical
set of `ResourceObservationCoordinate` values. Each coordinate contains one canonical resource
identity and exactly one provider-neutral selector: `:current`, or an exact retained public
resource version from a validated immutable reference, including the root/version/path
provenance required by the repository's canonical reference-error law. Bare identities normalize
only to `:current`; a resolver may not silently turn a pinned coordinate into a current read.
The operation returns a typed result containing a `ResourceObservation` and the exact composite
`RepositoryOperationReceipt` that authorized it. The observation contains:

- the canonical ordered coordinate set;
- for each `:current` coordinate, either its complete canonical authority record and
  resource-scoped version, or an authoritative absence entry for that exact identity;
- for each exact-version coordinate, that byte-identical retained authority record and public
  version—never the identity's newer current revision;
- the repository contract/schema version; and
- an immutable observation identity over the repository contract/schema version plus only
  those ordered coordinates and results.

An unavailable exact version fails with the repository contract's canonical
`:resource/invalid-reference` / `:referenced-version-absent` result (or the same
non-enumerating authorization denial for a forbidden identity). It is not an absence witness.
No missing, denied, malformed, or selector/version-mismatched coordinate returns a partial
observation or operation receipt.

The operation receipt follows `knoxx-cms-contract-validation`: it binds the authenticated
principal, effective scope/delegation, `observe-many` operation and required capability, the
exact canonical ordered coordinate set, the returned observation identity, and an ordered
authorization entry with the server-owned `authorization-policy-version` used for every
requested coordinate/identity. It is immutable historical evidence, not a reusable read
capability.
Policy versions and the receipt are deliberately outside semantic observation identity:
changing only authorization policy cannot pretend resource state changed, while every new
operation still reauthorizes before repository access and emits current policy evidence.

The provider linearizes all requested reads at one repository state. A returned combination
must therefore have existed, even when writers race the operation. A retained exact version is
part of that state even when it is no longer current. The observation identity is stable when
the requested results are unchanged: internal manifest generations and unrelated resource
writes cannot rotate it. Updating a `:current` coordinate or changing one current identity from
absent to present changes the next observation; advancing current state does not rotate an
observation that requested only an unchanged exact historical version.
Changing the repository contract/schema version also changes observation identity even when
all requested resource values remain equal; consumers never interpret one identity under two
repository contracts.

The operation authorizes every requested coordinate/identity under the server-authenticated
actor before disclosing any existence, version, or partial result. Existing and missing foreign
identities produce the same non-enumerating denial, and a denied request returns no observation.

The fake and file/EDN providers implement the same operation on the same repository authority
used by `resolve one`, list/query, history, and writes. The file provider may use a
cross-process repository read/write fence, a provider transaction, or a generation-checked
retry, but it may not expose filesystem locks, manifest hashes, or a repository-global
generation as semantic resource versions.

## TDD / proof

1. One-coordinate `:current` observations exactly match `resolve one`, and exact-version
   observations exactly match retained revision resolution, for canonical record, public
   version, authorization, and errors in both providers.
2. A deterministic writer barrier forces requested-resource changes between the two
   would-be single reads. `observe-many` returns one complete before-state or one complete
   after-state that existed, never a torn combination.
3. An optional identity changes from absent to present while observation is blocked. The result
   is either authoritative absence or the new present revision at the operation's linearization
   point, never a fabricated/mixed entry.
4. An unrelated resource write preserves the observation identity and every requested public
   version; updating a requested identity changes the next observation.
5. Existing/missing cross-tenant identities in any requested position deny before any partial
   observation or version/existence disclosure. Permitted and denied global identities follow
   server-owned read policy.
6. Fake and file providers pass the identical operation, shape, error, ordering, concurrency,
   and absence suite. File-provider races use separate processes rather than only promises or
   an in-memory lock.
7. A future remote-provider compatibility fixture can satisfy the same suite without consumers
   branching on provider identity.
8. Keeping resources equal while changing the repository contract/schema version rotates the
   observation identity, and the dependent config artifact/attestation names the new version.
9. With unchanged resources, policy V1 allow then V2 allow preserves resource versions and
   observation identity but emits a different operation receipt with V2 bindings. V2 deny
   returns no observation; replaying V1 or substituting a receipt from another principal,
   effective scope, capability, requested coordinate set, or observation is rejected. The final
   fixed-point consumer cannot attest a provisional smaller-set receipt.
10. Observe a current config revision that pins policy P1 while that policy identity is current
    at P2. One mixed-coordinate observation returns the current config and retained P1 exactly;
    it never substitutes P2 or exhausts a stable closure. Updating policy current to P3 leaves
    the exact-P1 result stable. Missing P1 produces the canonical referenced-version error with
    no partial observation/receipt, and current optional absence remains distinguishable from
    unavailable pinned history.

## Non-goals

- Adding translation-specific config resolution, precedence, attempt admission, or attestation.
- Replacing single-resource resolve/list/write operations.
- Exposing storage transactions or filesystem layout to consumers.
- Treating a repository-global manifest revision as a resource version or semantic observation
  identity.
- Implementing a remote CMS provider.

## Done when

- One provider-neutral `observe-many` contract returns only repository states that existed.
- Current versions, retained exact versions, and current-only absence observations are
  mechanically attributable to one immutable observation.
- Authorization fails closed without partial results or existence signals.
- Every successful observation carries its exact composite operation receipt and ordered
  authorization-policy versions without making those policy versions semantic resource state.
- Unrelated writes cannot rotate scoped observation identity.
- Fake and real file/EDN providers pass the same cross-process compatibility suite.
- #275 can consume the observation without sequential reads or a translation-only repository
  adapter.
