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

- resolve one resource by canonical identity. Tenant-owned identities include their
  organization scope; global and organization scopes are explicit and never inferred from a
  provider path;
- list/query resources without exposing provider storage layout;
- validated writes and explicit rejection of malformed resources;
- deterministic duplicate/conflict handling;
- stable reference resolution;
- identity/version behavior needed by downstream consumers;
- provider failure/error semantics;
- read-after-write behavior where the contract promises it.

Authenticated adapters derive the allowed organization scope from actor context rather than
accepting a client-selected tenant. The same namespace/name fixture in two organizations must
remain isolated across resolve, list, write, reference closure, and version history. A direct
cross-tenant identity returns a non-enumerating `:authorization/forbidden` result before the
provider reveals existence or mutates authority.

An explicit `:global` identity does not grant global write authority. Global resources carry
server-owned read/write capability policy outside caller-controlled payload/provenance, and
adapters authorize it from the authenticated principal before repository access. The existing
global translation configuration requires `platform.translations.manage` for mutation; an
organization administrator without that platform capability may consume the global default
when its read policy permits but cannot create, replace, or delete it. A client cannot change
scope or required capability in the same write it is trying to authorize.

### Capability-policy versioning

Server-owned capability policy is authorization authority, not resource content. Changing that
policy does **not** rotate an otherwise unchanged resource version or reference-closure digest.
Every authorized repository operation nevertheless emits a `RepositoryOperationReceipt`
binding the authenticated principal, effective scope, operation, and a canonical ordered entry
for every authority decision that admitted the complete result. The root entry always binds its
canonical identity, capability, and server-owned `authorization-policy-version`. A read/resolve
root also binds its requested `:current` or exact-version selector and the exact public version
returned when present. A create/write/replace root instead binds the normalized submitted
`expected-version` precondition—including its explicit absence—and the accepted result version.
A direct operation with no reference closure has only that root entry. A write that validates a
pinned closure or a resolve that returns one binds the root first and then every direct/transitive
target in deterministic closure order; each target entry binds
`{resource/coordinate, capability, authorization-policy-version}` with its exact public version,
validated root/version/path reference provenance, and exact server-owned read-policy version.
Equal policy versions do not permit an entry to be omitted, and root authorization never stands
in for target authorization.

For a provider-neutral operation over multiple resource operation coordinates, the same atomic
receipt also binds the complete canonical ordered coordinate set and its ordered authorization
entries. A current coordinate binds the canonical identity and `:current` selector. An
exact-version coordinate also binds its public version and validated root/version/path reference
provenance, so current and retained reads—or two versions of the same identity—cannot collapse,
reorder, or substitute for one another. The receipt is emitted only with the complete successful
result; there is no partial receipt whose allowed subset can be replayed as authority.

Historical operation receipts remain verifiable evidence of what was authorized then, but they
are not reusable capabilities. Every new read/write/resolve reauthorizes against current policy
before repository access. If policy V1 permits a global write and V2 denies it, the V1 receipt
and unchanged resource version remain historical evidence; replay or new mutation under V2
fails without exposing or changing authority. Downstream semantic receipts continue to bind
resource versions/closures, while any claim about who was authorized also binds the operation
receipt.

### Reference-edge authorization

Authorization applies to the requested root **and independently to every direct and transitive
reference edge** before the provider checks target existence or version. A tenant-owned target
must be in the authenticated organization; a global target must pass its server-owned read
policy. Authorization to read or write the root never delegates authority to its targets, and
an explicitly foreign target identity is not a scope bypass.

Both write-time reference validation and read-time closure resolution return the same
non-enumerating `:authorization/forbidden` result for an unauthorized existing target, absent
target, or retained historical version. They expose no target payload, actual version,
reference closure, or digest and do not create/change the root. Apply the check at every hop so
an authorized first edge cannot conceal a foreign transitive edge.

### Provider-neutral conflict contract

Every write/replace request carries a canonical resource identity, a canonical authority
record (validated semantic payload plus domain provenance), and an `expected-version`
precondition. Store-assigned envelope metadata explicitly excluded by the resource contract
does not participate in equality. Providers must return the same semantic outcome and error
shape:

- creating an absent identity with no expected version returns `:created` and an opaque,
  immutable version token;
- writing an absent identity with a non-nil expected version returns
  `{:error/type :resource/conflict :error/reason :resource-absent
  :resource/id <identity> :expected-version <expected> :actual-version nil}`, performs
  no create, and leaves the identity absent. An expected version is a compare-and-swap
  precondition, never permission to create;
- retrying the same identity and canonically equal authority record with either no expected
  version or the current version returns `:unchanged`, the existing resource, and the same
  version without another write;
- creating the same identity with a different authority record returns
  `{:error/type :resource/conflict :error/reason :identity-exists
  :resource/id <identity> :expected-version nil :actual-version <version>}`. A
  provenance-only difference is a different authority record;
- replacing with the current version and a changed valid authority record returns `:updated`
  and a new version, including when only domain provenance changed;
- replacing with a stale or unknown version returns
  `{:error/type :resource/conflict :error/reason :stale-version
  :resource/id <identity> :expected-version <expected> :actual-version <actual>}` and
  leaves bytes, semantic payload, provenance, and version unchanged. This precondition
  check takes precedence over authority-record equality: a stale expected version returns
  `:stale-version` even when the proposed payload and provenance equal the current resource;
- replacing with the current version and an equal authority record returns `:unchanged` with
  the same version.

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
both accepted writes remain present and re-readable. Public version tokens are scoped to one
resource identity and its canonical authority-record revision; an internal manifest hash,
mtime, or storage revision may not leak out as that token. Writing resource B therefore leaves
resource A's version byte-for-byte identical when A itself did not change.

### Revision-pinned reference resolution

Every canonical resource reference names the target resource identity **and exact public
version**. Providers retain each referenced immutable revision as an addressable value; an
update creates a new version and cannot make an older pinned target disappear. A write that
introduces an absent target version fails before authority changes with
`{:error/type :resource/invalid-reference :error/reason :referenced-version-absent
:resource/id <root-id> :reference/path <path> :reference/id <target-id>
:reference/version <target-version>}`. A cyclic closure likewise fails with
`:error/type :resource/invalid-reference`, `:error/reason :reference-cycle`, and the complete
cycle in `:reference/path`.

Resolving `root-id` at `root-version` returns the immutable root authority record plus a
deterministically ordered closure containing every transitive `{resource/id, version}` pair
and a digest of that closure. Effective/resolved content and downstream evaluation or
publication receipts bind to the root pair **and** this closure, never to a floating current
target. After referenced resource B advances from B1 to B2, resolving the old A revision still
returns B1. Consuming B2 requires a new A revision whose canonical reference explicitly pins
B2.

Batch observation preserves the same rule. A current observation coordinate resolves the
revision current at the operation's linearization point or returns authoritative absence; an
exact-version coordinate resolves that retained revision and never falls forward to current.
After coordinate-shape validation, the provider authorizes the complete canonical coordinate
set before looking up any identity or version. If any member is unauthorized, the whole batch
returns exactly `{:error/type :authorization/forbidden :error/reason
:resource-observation-denied}`, without a member identity/position, requested/actual version,
existence fact, partial observation, or operation receipt. Providers cannot let request order
reveal which member failed.

Only after every coordinate is authorized does the provider resolve the batch at one
linearization point. A permitted current coordinate may return authoritative absence. A
permitted exact-version coordinate whose requested revision is unavailable fails the whole batch
with the existing canonical reference error
`{:error/type :resource/invalid-reference :error/reason :referenced-version-absent
:resource/id <root-id> :reference/path <path> :reference/id <target-id>
:reference/version <target-version>}` from its validated coordinate provenance and no observation
or operation receipt. If several coordinates are unavailable, the canonical first coordinate
determines the error. It exposes no current/actual version, payload, closure, or sibling result.
Authorization denial takes precedence over this authorized absence, so existing and missing
foreign revisions remain indistinguishable while authorized callers receive one deterministic
exact-version contract.

Compatibility tests must assert the complete conflict data above, not only that an
exception occurred. They must also re-read after every duplicate or conflict and prove
that no authority changed. Include both the absent-resource/non-nil-precondition case and
the stale-version/equal-authority-record case so every provider implements the same precedence
rules. Include provenance-only create/replace cases, different-record and equal-record
concurrent create cases for one absent
identity, a same-identity concurrent replacement case where one request updates and one
returns `:stale-version`, plus a different-identity concurrent write case where both siblings
survive in a shared manifest and each untouched sibling retains its prior resource version.
Create A pinned to B1, update B to B2, and prove A@A1 still resolves the same ordered B1
closure and digest; missing versions and cycles must return the exact invalid-reference data
without changing authority. In separate fixtures, make an organization-A root reference both
an existing and a missing organization-B target, and place the foreign edge one hop deeper.
Every write/resolve attempt must produce the same non-enumerating denial and reveal no closure
facts. Exercise permitted and denied global targets through their server-owned read policy too.
Change a direct or transitive target policy from allowed T1 to allowed T2 without changing any
resource: assert the root/target versions and closure digest stay byte-identical while the new
operation receipt rotates only that target's ordered policy entry. Dropping, reordering, or
substituting a target entry fails verification. For read/resolve, changing the root identity,
selector, or returned version invalidates the receipt; for create/write/replace, changing the root
identity, normalized expected-version precondition, or accepted result version does likewise.
Change T2 to denied T3 and prove the T1/T2 receipts remain historically verifiable but grant no
current access; the T3 attempt returns
the same non-enumerating denial with no operation receipt or mutation.

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
- Provenance participates in canonical resource equality and versioning, while excluded
  store envelope metadata does not.
- Concurrent compare-and-swap replacement is linearizable, and concurrent accepted writes
  to sibling identities cannot lose either resource or rotate an untouched sibling's
  resource-scoped version through a shared-manifest race.
- Concurrent creates for one absent identity store one authoritative resource: a changed
  loser conflicts and an equal retry is unchanged.
- An organization administrator cannot mutate an explicitly global translation configuration;
  a principal with `platform.translations.manage` can, and denied attempts return the same
  non-enumerating authorization result without rotating payload, provenance, or version.
- Capability-policy changes leave semantic resource versions unchanged but rotate the policy
  version on operation receipts; historical receipts remain evidence and never authorize a
  later operation.
- Single-resource and multi-resource operation receipts bind every authorization decision and
  cannot drop, reorder, or substitute an identity/selector/version/policy coordinate without
  failing verification.
- Resolution exposes an immutable version-pinned transitive reference closure; target updates,
  missing revisions, and cycles cannot silently change an older root revision's meaning.
- Every direct/transitive reference target is independently authorized before lookup; foreign
  existing/missing targets and denied global targets are non-enumerating and cannot change the
  root or disclose closure/version facts.
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
