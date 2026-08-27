# Publication epic — review findings

Every open review thread across the eleven-PR stack, read and triaged on
**2026-08-13**. Written because merging is gated on these threads and because
several of them are load-bearing.

Sources: `chatgpt-codex-connector` (58 threads) and `coderabbitai` (3). My own
25 walkthrough threads are excluded — they are explanation, not objection, and
are listed separately at the end.

> **Read the "Reconciliation is not wired up" section first.** It is the finding
> that changes what merging this stack means.

---

## Verdict summary

| | |
|---|---|
| Open bot threads | **61** across 12 PRs |
| Flagged P1 | **40** |
| Flagged P2 | **18** |
| Confirmed by an independent live run | **3** (two now fixed) |
| Blocking `review-resolution-gate` | **all of them** — the gate runs `strict: true` |

---

## Reconciliation is not wired up

Three separate reviews claim that parts of this epic have no production caller.
I verified the namespace graph directly rather than taking their word:

```
domain.publication-plan            <- nothing in backend/src
domain.publication-gate            <- only publication-plan (itself orphaned)
infra.publication-effects          <- only publication-target-memory (itself orphaned)
infra.publication-target-memory    <- nothing in backend/src
infra.publication-surface-verify   <- nothing in backend/src
infra.publication-migration        <- nothing in backend/src
```

What that means concretely:

- **#232** — `migrate-publication-records!` is defined and tested but never
  called. There is no way to actually run the legacy migration.
- **#234, #235, #236, #237** — the gate, the plan laws, the effect boundary,
  the idempotency store, and receipts form a complete, well-tested library that
  nothing invokes. No reconciler exists. Missing translation or review evidence
  cannot block a publication, because nothing consults the gate at runtime.
- **#240** — `verify-required-surface!` is invoked by no workflow. The claim
  that deploy verification and the E2E "check the same list" is half-true: the
  E2E checks the list, and `open-hax/services` re-implements the same checks in
  bash (services#47). The shared *verifier* is unused.

**What IS live:** the resource-backed projection, the CMS read/write surface,
and translation config — `#230`, `#233`, `#239`. That is also exactly where the
live run found real defects, which is not a coincidence: it is the part that
actually executes.

This does not make the merged code wrong. It means the epic delivered roughly
half a working feature plus half a library, and the cards read as though both
halves shipped. That is a scope conversation, not a code fix.

---

## Findings confirmed independently

Three bot findings were reproduced by running the code against a live backend,
which is stronger evidence than either the reviews or the test suite had:

1. **#239 / #242 — a state PATCH destroyed sibling resources.** Codex predicted
   it on both PRs before it was observed. Reproduced live: publishing deleted
   the document and garden declared in the same manifest. **Fixed in #243.**
2. **#230 (coderabbit) — conflicting canonical `:publication/id` is not
   rejected.** `publication-conflicts` keys on the relation, not the id.
   Reproduced live; §6 of the verification script was red on it.
   **Fixed in #230** — `publication-identity-conflicts` reports a shared id with
   unequal payloads, byte-equal duplicates still fail the relation check, and
   the reported pair is ordered independently of file enumeration. Carded as
   `knoxx-publication-duplicate-identity`, now in review rather than incoming.
3. **#242 — the fixture trap can delete data it did not create.** The `EXIT`
   trap is installed before the "fixture already exists" preflight, so dying on
   an existing fixture deletes someone else's. **Fixed** in this branch.

A fourth defect — every publication route returning 500 on null-prototype
`request.params` — was found only by the live run. No review caught it, and no
test could, because the tests build params with `clj->js`.

---

## Recurring themes

Worth reading as classes rather than 61 individual items:

- **HTTP error status is swallowed.** `ensure-permission!` throws a 403, but the
  `error-status` helpers in `extern/fastify/publications.cljs`,
  `cms_publication.cljs` and `translation_config.cljs` ignore it and answer
  **500**. Every authorization denial is reported as an outage. Raised on #230
  and twice on #233.
- **Fail-open when the policy DB is absent.** Both projection routes skip
  `ensure-permission!` when `ctx` is nil, and the supplied
  `with-request-context!` passes nil when the policy database is disabled — so
  filesystem-backed publication data is served unauthenticated. (#230,
  coderabbit, Major.)
- **Blockers are computed over the whole contracts tree.** One invalid *agent*
  or *role* file makes both publication routes answer 409. (#230 P2.)
- **Guards live in `domain.*` instead of `law.*`.** `publication-gate/admissible?`
  is a second admissibility authority alongside
  `law.publication/admissible-publication?`; they can diverge. (#234 P1, cites
  AGENTS.md's architecture split.)
- **Idempotency has ownership gaps.** Three P1s on #236: an in-flight claim can
  be cleared by a second caller, an adapter error releases a reservation that
  may have succeeded, and a completed receipt is treated as permanent proof the
  artifact still exists.
- **Tests assert against fixtures rather than boundaries.** #241 has four P1s
  saying the E2E's translation and approval stages only mutate a local atom,
  `converge!` runs outside the no-network guard, and the "no retired caller"
  grep is a hand-maintained list rather than the transitive graph.
- **Deployment reality.** #233 P2: the backend Dockerfile creates an empty
  `/app/contracts` and compose neither copies nor mounts the repo `contracts/`
  tree, so translation config would resolve to "missing" in the shipped image.

---

## Full inventory

Ordered by PR. `(outdated)` means the diff moved under the comment; the finding
may or may not still apply.

### PR #230 — 15 open

- **`backend/src/cljs/knoxx/backend/extern/fastify/publications.cljs:50`** [P1]  
  When the policy database is enabled and the caller lacks `org.publications.read`, `ensure-permission!` throws an error carrying HTTP status 403, but this mapping ignores both `:status` and the error's `statusCode`, so `send-projection!` responds with 500. This misclassifies every denied request as an internal failure; preserve the status from recognized HTTP errors before applying projection-specific 404/409 mappings
- **`backend/src/cljs/knoxx/backend/infra/routes/publications.cljs:47`** [P2]  
  Because `resource-records!` loads the entire contracts tree, this predicate turns every rejected agent, role, policy, or other unrelated resource into a publication blocker. A single invalid non-publication contract therefore makes both new projection routes return 409 even when all documents, gardens, and publication intents are valid; filter blockers to the three publication topology kinds.
- **`backend/src/cljs/knoxx/backend/domain/contracts/loader.cljs:380`** [P1]  
  Although schema-invalid resources now produce blocker records, fresh inspection shows that `parse-contract-file-records!` still converts malformed EDN to `[]` and `read-contract-file!` converts read failures to `nil`; this aggregation removes both. Consequently a malformed or unreadable publication file leaves no evidence for `invalid-resource-blockers`, and the facade returns a successful but incomplete desired topo
- **`backend/src/cljs/knoxx/backend/domain/publication_resolver.cljs:194`** [Major]  
  **Reject conflicting canonical `:publication/id` values.** Line 194 appends every publication intent without indexing its canonical `:publication/id`. Two different intents can use the same ID and different relation keys. They bypass `publication-conflicts` and produce colliding identities in the returned view. Track publication IDs separately. Collapse equal payloads. Throw `conflicting canonical resource identity` 
- **`backend/src/cljs/knoxx/backend/extern/fastify/publications.cljs:87`** [Major]  
  **Fail closed when request context is unavailable.** Line 86 skips `ensure-permission!` when `ctx` is nil. The supplied `with-request-context!` implementation passes nil when the policy database is disabled. Both routes then read and return filesystem-backed publication data without `org.publications.read`. Reject a nil request context before `operation` runs. Map that rejection to an explicit authorization or servic
- **`kanban/tasks/knoxx-publication-intent-resolver.md:280`** [Minor]  
  **Use an unambiguous horizontal rule.** Line 280 makes line 279 a Setext heading. Replace `---` with `***`, or add the intended heading marker to line 279. This removes the Markdown lint warning.
- **`backend/src/cljs/knoxx/backend/infra/routes/publications.cljs:?`** [P1] _(outdated)_  
  When a namespace manifest uses the supported namespace-local document, garden, or publication IDs, `load-all-resources!` never returns those definitions: `namespace-resource-record` validates the expanded definition first, while the publication schemas require qualified IDs, and failed definitions are dropped. Consequently the resolver's canonicalization cannot run and the facade silently serves an empty or incomplet
- **`backend/src/cljs/knoxx/backend/infra/routes/publications.cljs:?`** [P1] _(outdated)_  
  When two files define the same document or garden ID with different payloads, this loader has already passed them through `dedup-contracts`, whose `[contractClass id]` collision handling keeps the first record and discards the other. The resolver therefore sees only one payload, so `index-canonical!` can never report the promised conflict and the selected topology depends on filesystem enumeration order; the facade n
- **`backend/src/cljs/knoxx/backend/extern/fastify/publications.cljs:?`** [P1] _(outdated)_  
  For every nonempty projection, `fastify/send-json!` serializes the CLJS body with `clj->js`, which converts keywords using their unqualified name. Thus a canonical value such as `:knoxx.docs/translation-pipeline` reaches the CMS as `"translation-pipeline"`, and namespaced map keys similarly lose their namespace, defeating canonical identity and allowing different namespaces to collide on the wire. Encode publication 
- **`backend/src/cljs/knoxx/backend/domain/publication_resolver.cljs:?`** [P1] _(outdated)_  
  When an intent references a missing document or garden, this finalization checks only relation conflicts. `list-document-views` iterates existing documents, so an intent with a dangling document is silently omitted, while hydration validates only the document and therefore allows a dangling garden through; both cases return a successful but incomplete desired topology instead of the required semantic blocker. Validat
- **`backend/src/cljs/knoxx/backend/domain/publication_resolver.cljs:?`** [P2] _(outdated)_  
  If a validated document carries an extra execution field such as `:receipt/published-at` or worker state, this update preserves the entire resource map and the projection returns that field. Malli map schemas are open by default, so `PublicationDocumentView` does not remove or reject such extras; the added test only supplies a separate receipt resource, which is ignored before this path and does not cover runtime key
- **`backend/src/cljs/knoxx/backend/extern/fastify/publications.cljs:?`** [P1] _(outdated)_  
  When authentication is enabled, the session hook only hydrates request headers; this new list handler ignores the request and calls the filesystem-backed projection directly without `with-request-context!` or a permission check. An anonymous caller can therefore enumerate document titles and source paths, gardens, publication paths, locales, and requested states; require an authenticated publication-read permission b
- **`backend/src/cljs/knoxx/backend/infra/routes/publications.cljs:?`** [P1] _(outdated)_  
  With the new undeduped loading path, a legal composite namespace entry is expanded into one record per registered kind while every expanded definition retains the composite keys; stripping `:resource/kind` here makes `index-one` register every facet once for every expanded record. A single entry containing document, garden, and publication IDs consequently appends the same active publication three times and is reject
- **`backend/src/cljs/knoxx/backend/infra/routes/publications.cljs:?`** [P1] _(outdated)_  
  When a publication resource is schema-invalid—for example, its path lacks the required leading slash—the contract loader logs the validation failure and omits the record entirely, so this facade receives no evidence of it and returns a successful topology with that intent missing. Consumers such as CMS or reconciliation can therefore treat malformed desired state as an intentional absence; use a loading result that p
- **`backend/src/cljs/knoxx/backend/extern/fastify/publications.cljs:?`** [P1] _(outdated)_  
  `decode-request` crosses from a raw Fastify handle into CLJS data but returns the decoded map without naming or invoking any request schema, so malformed or changed parameter shapes flow directly into document lookup rather than failing the boundary contract. Add an explicit publication-request schema/validator here, including the required `documentId` shape for the detail route.

### PR #232 — 11 open

- **`backend/src/cljs/knoxx/backend/domain/publication_migration.cljs:275`** [P1]  
  When `:publications` is assembled from the same raw legacy documents consumed by the new document phase, a bare ID such as `:translation-pipeline` is qualified when the document resource is written but copied unchanged here. `migrate-record` then validates the publication against `PublicationIntentResource`, where the bare reference fails `qualified-keyword?`, aborting the batch instead of migrating it. Resolve the r
- **`backend/src/cljs/knoxx/backend/infra/publication_migration.cljs:106`** [P1]  
  If two legacy document or garden rows canonicalize to the same ID but differ in source path, locale, title, or status, the second candidate reaches this write branch instead of becoming a conflict. A filesystem-backed writer can therefore overwrite the first resource, making the result input-order-dependent and silently implementing last-write-wins for precisely the ambiguous rows this migration is supposed to report
- **`backend/src/cljs/knoxx/backend/infra/publication_migration.cljs:43`** [P1]  
  For rows whose identity is itself malformed, all fallbacks can be nil; for example, two garden rows missing `:garden-id` both produce `[:publication/migration :gardens :gardens nil]`. The `seen` check then suppresses the second receipt, and the cross-run append-once store permanently retains only one conflict, so not all malformed legacy rows are available for explicit resolution. Include a stable reader-provided row
- **`backend/src/cljs/knoxx/backend/infra/publication_migration.cljs:70`** [P1]  
  When `:write!` resolves to nil, an acknowledgement map, or an altered resource rather than the saved domain resource, this code records the value as written and either leaves the index unchanged or indexes malformed state; subsequent rows can then be written blindly instead of reconciling against the successful write. Validate the returned value with the schema selected for the resource kind before indexing it, as re
- **`backend/src/cljs/knoxx/backend/infra/publication_migration.cljs:125`** [P1]  
  A repo-wide search across `backend/src`, `frontend/src`, `ingestion/src`, `shared/src`, and `scripts` finds no caller of `migrate-publication-records!` and no production implementation of its three context effects; only tests invoke it. Consequently the server build and repository scripts provide no way to read OpenPlanner data, persist resources, or run this claimed one-time authority transfer before cutover.
- **`backend/src/cljs/knoxx/backend/domain/publication_migration.cljs:?`** [P1] _(outdated)_  
  For the actual CMS shape, `source_path` is a repository path such as `docs/existing.md` (including the commit's own `legacy-document` fixture), while `valid-publication-path?` requires a leading `/`. Copying it verbatim means every such garden membership normalizes successfully and then becomes `:invalid-publication-path`, so the migration cannot reconstruct the existing published topology. The test helper currently 
- **`backend/src/cljs/knoxx/backend/infra/publication_migration.cljs:?`** [P1] _(outdated)_  
  The fold always classifies a publication and never invokes `document->decision` or `garden->decision`. A repo-wide reference search shows those two functions have no production callers, so starting from `empty-index` writes only publication resources and leaves their document and garden references dangling; `hydrate-publication-intent` subsequently rejects the missing document, and the desired topology cannot be used
- **`backend/src/cljs/knoxx/backend/infra/publication_migration.cljs:?`** [P1] _(outdated)_  
  The legacy read crosses an effect boundary but its result is consumed without any named schema or validator. If a production record is missing the `:document`/`:row` wrapper or contains the wrong top-level shape, the fold destructures nil values and eventually throws from publication validation, aborting the entire batch instead of producing the promised per-row conflict evidence. Validate/decode the reader result at
- **`backend/src/cljs/knoxx/backend/domain/publication_migration.cljs:?`** [P1] _(outdated)_  
  `legacy-name` stringifies every non-keyword value, so malformed document or garden identities such as numbers and maps are silently converted into invented keyword IDs; nil or blank document IDs can also reach `document->decision` as invalid candidate resources rather than conflicts. This violates the migration's fail-closed behavior and can persist resources under identities unrelated to the legacy source. Validate 
- **`backend/src/cljs/knoxx/backend/domain/publication_migration.cljs:?`** [P2] _(outdated)_  
  Concatenating document, garden, and locale names with `-` is not injective: for example document `a-b` with garden `c` and document `a` with garden `b-c` both produce `a-b-c-en`. Those are distinct publication relations, but the second is classified as an ID conflict and cannot migrate. Use an unambiguous component encoding or a stable digest of the structured tuple.
- **`backend/src/cljs/knoxx/backend/domain/publication_migration.cljs:?`** [P2] _(outdated)_  
  For `:explicit-publication-row` inputs, two valid records for the same document, garden, and locale but different revisions generate the same ID because this tuple stops at locale. The existing resolver explicitly includes revision in `publication-key` and permits both relations (`publication_resolver.cljs` lines 79-88 and its revision test), so after the first record is written, the second is treated as a same-ID/di

### PR #233 — 5 open

- **`backend/src/cljs/knoxx/backend/infra/routes/translation_config.cljs:65`** [P1]  
  When an organization administrator calls this route, `apply-patch` resolves that organization's effective configuration, but `config-resource` always merges the result into `global-config-id` and this write targets the global manifest. Consequently, any tenant holding `org.translations.manage` can change the model for every organization; if that tenant already has an override, a later GET may also ignore the reported
- **`ingestion/src/kms_ingestion/translation/worker.clj:119`** [P2]  
  For a batch whose organization defines the supported per-org translation override, this lookup sends only the generic worker identity and `resolve-translation-model` stores the result in one process-wide cache. The endpoint therefore resolves the worker's/default context rather than the batch's `org_id`, so batches can run with the global model while their review UI displays the tenant override. Pass the resolved bat
- **`backend/src/cljs/knoxx/backend/extern/fastify/translation_config.cljs:31`** [P2]  
  When policy enforcement is enabled and a caller lacks either permission, `ensure-permission!` throws an HTTP error with `{:status 403 :code ...}`, but this fallback ignores that status and returns 500. Both unauthorized GET and PATCH requests are consequently reported as server failures, which prevents clients and monitoring from distinguishing access denial from an outage. Honor the HTTP status carried by the except
- **`backend/src/cljs/knoxx/backend/extern/fastify/translation_config.cljs:31`** [P2]  
  When a PATCH body is missing `model`, supplies a blank/non-string value, or includes an extra key, `decode-config-patch` throws contract data shaped like `{:contract ... :errors ...}`. None of these branches match, so the route reports 500 even though the failure is entirely client-controlled; reserve 500 for internal resolution/write failures and map patch-wire contract violations to 400 or 422.
- **`ingestion/src/kms_ingestion/translation/worker.clj:119`** [P2]  
  In the checked-in Docker deployment, this request cannot succeed: `backend/Dockerfile` copies only `package.json`, `dist`, and `docker`, then creates an empty `/app/contracts`, while `docker-compose.yml` neither copies nor mounts the repository-level `contracts/` tree. Because this change also removes the worker fallback, enabling translation with that deployment makes this fetch receive the missing-config response a

### PR #234 — 2 open

- **`backend/src/cljs/knoxx/backend/domain/publication_gate.cljs:132`** [P1]  
  A repo-wide usage search shows that production code never imports this namespace; only its new tests call it. In particular, `infra/routes/publications.cljs` still returns the resolver's desired-state views without blockers, and no reconciler invokes `gate`, so missing translation/review evidence neither appears in the CMS nor prevents or queues publication work. Connect this entry point to the publication facade and
- **`backend/src/cljs/knoxx/backend/domain/publication_gate.cljs:84`** [P1]  
  `admissible?` decides whether an intent is currently allowed to publish, while the blocker and approval checks provide its evidence requirements, so this is contract enforcement rather than domain transition vocabulary. Defining it in `domain.*` also creates another admissibility authority alongside `law.publication/admissible-publication?`, making it easy for the two sets of publication guards to diverge; move or co

### PR #235 — 1 open

- **`backend/src/cljs/knoxx/backend/domain/publication_plan.cljs:73`** [P2]  
  When `reconcile-plan` receives an active-garden intent whose state is missing or unrecognized—for example `nil`, `:deleted`, or a wire string—this condition is false and execution falls through to `converge`; with clean evidence it returns `:publish`. This contradicts the fail-closed allow-list in `backend/src/cljs/knoxx/backend/law/publication.cljs:165-181` and can turn malformed desired state into a public effect. 

### PR #236 — 6 open

- **`backend/src/cljs/knoxx/backend/infra/publication_effects.cljs:106`** [P1]  
  When two executions overlap on the same key, the second caller sees `:in-flight` while the first publish is still pending; it immediately observes no artifact, releases the first caller's reservation, and recursively publishes. Both callers can therefore invoke `publish!` and create duplicate artifacts despite an atomic `reserve!`. The in-flight state needs ownership/lease or wait semantics so another caller cannot c
- **`backend/src/cljs/knoxx/backend/infra/publication_effects.cljs:115`** [P1]  
  When the external publish succeeds but its response is lost or rejected, this catch unconditionally releases the reservation. The retry is then reported as freshly `:reserved`, skips `observe!`, and calls `publish!` again, so a target that does not independently deduplicate can create another artifact. Preserve an unknown/in-flight outcome for reconciliation rather than releasing it on every adapter error.
- **`backend/src/cljs/knoxx/backend/infra/publication_effects.cljs:102`** [P1]  
  A completed receipt is treated as permanent proof that the artifact is still current. For example, after publishing path A, moving to B removes A; rolling back to A reuses A's old key and this branch returns its old receipt without restoring A or removing B. External deletion causes the same failure to heal. Revalidate the target or invalidate superseded logical-materialization keys before returning a historical rece
- **`backend/src/cljs/knoxx/backend/infra/publication_effects.cljs:112`** [P2]  
  If an adapter returns a structurally valid materialized receipt with the wrong path, revision, or idempotency key, it passes this assertion and is recorded as `:done` under the requested key. Every replay then reports convergence even though the requested artifact may never have been materialized. Validate these receipt fields against `op` before completing the reservation, rather than checking only the generic recei
- **`backend/src/cljs/knoxx/backend/law/publication_receipts.cljs:27`** [P2]  
  Because `:observed` and `:intent` are optional and the conditional predicate only constrains `:publish`, a `{:op :remove}` plan with either field missing passes validation and invokes `remove!` with nil arguments. That allows malformed boundary input to reach an effect instead of failing before effects run; use op-specific plan schemas that require both values for removal.
- **`backend/src/cljs/knoxx/backend/law/publication_receipts.cljs:30`** [P2]  
  `PublicationRevision` also accepts the selector `:source/current`, so this schema permits a supposedly concrete publish plan to carry that moving token. Such a plan gets a stable key and may later replay its completed receipt even after the actual current source revision changes. Define a concrete-revision schema that excludes selectors for plans and successful receipts.

### PR #237 — 2 open

- **`backend/src/cljs/knoxx/backend/infra/publication_target_memory.cljs:91`** [P1]  
  When `observed-for` consumes real execution receipts, the receipt emitted here cannot be associated with the publication because it omits `:publication/id`; the projection filters receipts by that field. Thus a history containing a successful materialization followed by this removal still reports the old route as materialized, and if the intent is later republished the planner can incorrectly emit `:noop`. The test m
- **`backend/src/cljs/knoxx/backend/domain/publication_receipts.cljs:53`** [P2]  
  When this projection receives an adapter or persisted receipt directly, checking only the discriminator accepts malformed values such as `{:receipt/type :publication/materialized}` and returns an empty or partial observation. The newly defined `PublicationMaterializedReceipt` is referenced only by tests, while the effect-layer contract validates a smaller shape, so required publication, adapter, document, target, and

### PR #239 — 3 open

- **`backend/src/cljs/knoxx/backend/infra/routes/cms_publication.cljs:63`** [P1]  
  When the publication comes from a namespace manifest or composite entry, `:resource/file-path` points to the entire manifest, not a standalone publication file. Replacing that file with `next-intent` deletes its `:namespace`/`:resources` wrapper, sibling resources, and any document or garden facets in the same entry; the next topology load can therefore lose resources or fail validation after a single state edit. Rea
- **`frontend/src/pages/CmsPage.tsx:816`** [P1]  
  When the selected document/garden has no existing intent, `selectedPublication` is null and this branch silently skips the only Knoxx write after the legacy OpenPlanner publish endpoint has already run; newly created documents or garden relations can consequently become public while the resource topology remains unchanged. Even when an intent exists, a failed PATCH occurs after the legacy side effect, producing the s
- **`frontend/src/pages/CmsPage.tsx:1059`** [P2]  
  Although `loadPublicationTopology` computes an active-only `publishable` list for the default selection, the dropdown renders every garden from the topology. An archived garden can therefore be selected and sent through the publish handler; because that handler still calls the legacy publish endpoint directly, it can bypass the planner's archived-garden block. Render only active gardens, as the previous garden-loadin

### PR #240 — 3 open

- **`backend/test/cljs/knoxx/backend/infra/publication_surface_verify_test.cljs:126`** [P1]  
  The fixed `shipped-sources` allowlist omits active production callers found by a repo-wide search, including `frontend/src/cljs/knoxx/frontend/pages/gardens/api.cljs`, its request-building `logic.cljs`, `BroadcastStudioPage.tsx`, and the `/api/openplanner/v1/cms/publish/...` call in `CmsPage.tsx`. The Gardens page is still registered in `frontend/src/cljs/knoxx/frontend/app.cljs`, and `CmsPage` aborts before updating
- **`backend/src/cljs/knoxx/backend/infra/publication_surface_verify.cljs:30`** [P1]  
  When this verifier is connected to a real HTTP transport, it passes literal template paths such as `/api/publications/documents/:documentId` and `/api/cms/publications/intents/:publicationId`, and it supplies no body for either PATCH surface. Those authorized requests therefore produce 404/validation responses rather than `<400`, making the required-surface gate fail even when all routes are healthy. Add contract-val
- **`backend/src/cljs/knoxx/backend/infra/publication_surface_verify.cljs:30`** [P2]  
  Although each surface declares a specific `:permission`, the callback receives only method, path, and an `authorized?` boolean. A route guarded by the wrong capability still passes if the authorized probe uses a broad credential and the anonymous probe gets 401/403, so this does not verify the advertised read/manage authorization contract. Pass `permission` to `request!` and probe with credentials scoped to that exac

### PR #241 — 5 open

- **`backend/test/cljs/knoxx/backend/e2e/contract_publication_test.cljs:201`** [P1]  
  The calls that actually materialize the publication, such as this `converge!`, run outside `with-no-network`; moreover, that helper restores `js/fetch` immediately when its body returns rather than awaiting asynchronous work. A hosted request added to `execute-plan!`, the adapter, or a later asynchronous step would therefore run with the original fetch implementation and would not be recorded by the advertised fail-f
- **`backend/test/cljs/knoxx/backend/e2e/contract_publication_test.cljs:160`** [P1]  
  The supposed translation and review stages only mutate a test-local atom with fixture constants; the derived translation action is never dispatched through a Knoxx translation boundary, and no persisted translation or approval receipt is read back. Consequently, a broken translation dispatcher, persistence codec, or approval recorder would still leave this E2E green, so it does not prove the publication journey descr
- **`backend/test/cljs/knoxx/backend/e2e/contract_publication_test.cljs:322`** [P2]  
  This hard-coded source list is not the scenario's transitive graph. For example, `publication_resolver.cljs` requires `knoxx.backend.shape.resource-identity`, and `cms_publication.cljs` requires both that namespace and `open-hax.publication-wire`, but none of those dependencies are inspected. A hosted-authority import can therefore move into any omitted dependency while this test continues to pass; derive the reachab
- **`backend/test/cljs/knoxx/backend/e2e/contract_publication_test.cljs:195`** [P1]  
  When publishing the translated locale, `converge!` always supplies `nil` as the artifact. The memory target ignores the artifact and stores only receipt metadata, which `public-read-returns-the-materialized-translation` then reads directly from its routes map; consequently, dropping or corrupting the translated body—or failing to serve it through the public read boundary—still leaves this E2E green. Materialize disti
- **`backend/test/cljs/knoxx/backend/e2e/contract_publication_test.cljs:298`** [P1]  
  In the inspected production, staging, and testing workflows, and across all non-test repository references, nothing invokes `verify-required-surface!`; this test only validates the shared data var inside the test process. A deployment can therefore finish without probing any required route or its authorization behavior while this purported E2E/deploy-sharing assertion remains green. Add an actual post-deploy invocati

### PR #242 — 5 open

- **`scripts/lib/publication-fixture.sh:97`** [P1]  
  Both verification scripts install their `EXIT` trap before completing preflight, while this helper unconditionally removes any existing `_verify` directory. Consequently, encountering an existing fixture—or even an earlier error such as a missing `agent-browser`—calls `die`, runs the trap, and deletes data that this invocation did not create; this also allows one concurrent run to erase another's fixture. Track owner
- **`scripts/lib/publication-fixture.sh:45`** [P1]  
  When either script PATCHes this publication, `set-publication-state!` uses the record's file path and replaces that entire file with `pr-str` of only the updated publication (`backend/src/cljs/knoxx/backend/infra/routes/cms_publication.cljs:55-64`). Because this publication shares `probe.edn` with the document and garden above, the PATCH erases both dependencies; the subsequent topology read reports unresolved refere
- **`docs/verification/publication-epic.md:153`** [P1]  
  The documented `KNOXX_DISABLE_EVENT_RUNTIMES=true` setting has no effect: a repo-wide search of this commit finds no consumer for that variable, while `backend/src/cljs/knoxx/backend/bootstrap.cljs:197` still creates the Discord gateway manager and `backend/src/cljs/knoxx/backend/infra/core.cljs:170` unconditionally starts the event runtime. A reviewer following this “without side effects” command can therefore arm s
- **`scripts/verify-publication-epic.sh:291`** [P1]  
  With this fixture’s differing revision, `publication-conflicts` groups the two records under different relation keys (`publication_resolver.cljs:107-119`), so the current backend returns 200; the new card and documentation explicitly confirm that §6 remains red until later work. Keeping the 409 assertion here, followed by another conflict assertion, increments `FAIL_COUNT` on every otherwise healthy run and makes the
- **`scripts/verify-publication-tour.sh:283`** [P2]  
  When `KNOXX_DEV_EMAIL` and `KNOXX_DEV_PASSWORD` are supplied so the tour can render the real CMS, this same-origin `fetch` still sends the login session cookie by default. Blank identity headers cause the session hook to hydrate authentication from that cookie (`backend/src/cljs/knoxx/backend/infra/auth/session.cljs:527-552`), so the request returns an authorized response rather than testing an unauthenticated caller

### PR #243 — 3 open

- **`backend/src/cljs/knoxx/backend/infra/core.cljs:150`** [P1]  
  When consumers use the `start` export in `backend/shadow-cljs.edn`, `knoxx.backend.infra.core/start!` bypasses this gated background-startup path and still calls `event-runtime/start!` unconditionally. Consequently, setting `KNOXX_DISABLE_EVENT_RUNTIMES=true` does not prevent schedules and triggers from running for that supported entrypoint, defeating the safety switch in exactly the environment where outbound effect
- **`backend/src/cljs/knoxx/backend/infra/core.cljs:109`** [P2]  
  In the `server-dev` hot-reload flow, every HTTP restart calls `register-app-routes!` and eventually invokes this function again, but each invocation creates an interval whose handle is neither retained nor cleared. Repeated reloads therefore accumulate permanent timers and duplicate the warning every minute; if the flag is later disabled in-process, the stale timers also continue reporting that event runtimes are dis
- **`backend/src/cljs/knoxx/backend/infra/routes/cms_publication.cljs:61`** [P1]  
  This new filesystem boundary writes the reconstructed manifest after checking only that an entry with the requested ID exists; it never validates the resulting manifest or `next-state`. Thus a direct call with an inadmissible state, or malformed data read at this boundary, can persist an invalid resource file instead of failing before the write. Add an explicit manifest/publication contract assertion over the patched

---

## My own walkthrough threads

25 further open threads are mine, posted as the author's walkthrough described
in **AGENTS.md → Author's Walkthrough on Your Own PRs**. They explain decisions
rather than request changes, and are reproduced in
`docs/review/publication-epic-walkthrough.md`.

Three of them do ask for a decision and should not be resolved without one:

- **#230** `extern/fastify/publications.cljs:52` — the two adapters build error
  bodies with swapped keys (`{:error msg :detail data}` vs the reverse). Which
  shape wins?
- **#230** `publication_resolver.cljs:223` — should `query-id` keep accepting
  strings, or should the adapter decode and the resolver take keywords only?
- **#240** `law/publication_surface.cljs:74` — narrow the "no shipped caller"
  docstring to the CMS/translation surfaces, or migrate the Gardens page and
  make the guard repo-wide?

---

## On merging

`review-resolution-gate` runs with `strict: true` and is **failing on every PR
in the stack**, including on threads I opened myself. `main` carries no branch
protection, so a merge is mechanically possible, but merging with 40 open P1
threads would discard the review rather than answer it.
