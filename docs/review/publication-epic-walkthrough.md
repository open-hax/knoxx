# Author's walkthrough — contract-owned publication epic

> **Status: draft, not posted.** These are written to be posted as inline PR
> comments, one anchor each. Nothing has been sent to GitHub.

The practice has a name. IEEE 1028 distinguishes a **walkthrough** — author-led,
the author drives and explains — from an **inspection**, where a moderator drives
and the author stays quiet. What follows is a walkthrough: I am presenting the
diff, not summarizing it.

Each entry is an anchor (`file:line`) plus what I would say standing at that
line. The rule I applied for what earns a comment: *would a reviewer's first
reaction be "why is it like that", or "that looks wrong"?* Mechanical hunks are
skipped.

---

## #230 — publication intent resolver

**`backend/src/cljs/knoxx/backend/domain/publication_resolver.cljs:131`**
> `index-canonical!` throws on a same-id/different-payload collision rather
> than last-write-wins. That is the whole reason the facade in #239 reads the
> *undeduped* record list — see the comment there. If you read these two PRs in
> order, this throw looks like defensive paranoia; it isn't, it's the only thing
> standing between us and a topology that depends on `readdir` order.

**`publication_resolver.cljs:154`**
> Acceptance criterion: *"a dangling reference is a blocker, not a silent
> omission."* Both directions were silently passing before. `list-document-views`
> iterates the documents it *has*, so an intent pointing at a missing document
> simply never appeared in the output — a successful, confident, incomplete
> answer. Worth reading the docstring; it names the exact prior behavior.

**`publication_resolver.cljs:223`**
> `query-id` accepts a keyword, a `"namespace/name"` string, or a bare string.
> That looks over-permissive. It exists because the HTTP layer necessarily hands
> down a string (`:documentId` from the route) while every internal caller holds
> a keyword, and I did not want the adapter to own identity parsing. If you'd
> rather the adapter decode and the resolver take keywords only, say so — it's a
> small move and I don't feel strongly.

---

## #232 — state migration

**`backend/src/cljs/knoxx/backend/domain/publication_migration_identity.cljs:53`**
> **Risk I'm carrying.** Canonical ids are derived from legacy names. Two legacy
> gardens whose names normalize to the same component collide, and the migration
> refuses rather than merging. That is deliberate — a silent merge here publishes
> one garden's content under another's identity — but it means a real migration
> can halt on data we haven't seen. There is no dry-run mode. If you want one,
> that's a follow-up, not a blocker.

**Migration test fixture — commit `b2b8aa2e`**
> Flagging this because the review caught it and it's the kind of thing that
> stays caught only if someone remembers: the original fixture overrode the very
> field the test was meant to verify, so the happy path was never exercised.
> Every legacy membership would have conflicted on path in production while the
> suite stayed green. The fix is in the same commit. If you read one test in this
> PR, read that one.

---

## #233 — translation pipeline config resource

**`backend/src/cljs/knoxx/backend/domain/translation_config.cljs`**
> **The card was wrong here and I did not follow it.** It assumed model ids were
> keywords and asked for keywordization at the resource boundary. They are
> strings — `"xiaomi/mimo-v2-pro"`, `"gemma4:31b"` — and `:gemma4:31b` is not a
> readable keyword. Keywordizing would have corrupted the catalog. Ids stay
> strings and the catalog validates against the string set.

**`backend/src/cljs/knoxx/backend/infra/routes/translation_config.cljs:48`**
> The patch is a **whole-file rewrite** of the owning manifest, not a merge into
> an override file. That is the point: one authority, no shadow layer that can
> drift from it. The cost is that concurrent writers last-write-wins. Acceptable
> for an admin-edited config; would not be for anything hot.

---

## #234 — translation gate

**`backend/src/cljs/knoxx/backend/domain/publication_gate.cljs`**
> Read `admissible-publication?` in `law/publication.cljs:174` alongside this.
> The state allow-list there is `#{:published :withheld}` rather than
> `(not= :archived)` on purpose — an intent that has not been validated, or
> carries an unrecognized state, must fail closed. A denial list would have let
> a typo'd state through to reconciliation.

---

## #235 — reconcile plan laws

**`backend/src/cljs/knoxx/backend/domain/publication_plan.cljs`**
> Pure. No I/O, no clock, no store. Every test here builds an index literal and
> asserts on a returned plan. If you find yourself needing a mock to test
> something in this namespace, something has leaked in and it should be caught in
> review.

---

## #236 — adapter effects and idempotency

**`backend/src/cljs/knoxx/backend/infra/publication_effects.cljs:31`**
> `reserve!` must be **atomic** — claim-or-report, one operation. The protocol
> docstring says so in capitals because a check-then-set implementation compiles
> fine and passes the single-threaded tests, then double-publishes under
> concurrency. `publication_target_memory.cljs` is the reference implementation;
> any real adapter has to provide the same guarantee, and there is nothing in the
> type system that will make it.

**`publication_effects.cljs:57`**
> The idempotency key **refuses `:source/current`** and demands a concrete
> revision. That is why `publish-once!` can be replayed safely: a key that means
> "whatever is current" is not a key, it's a moving target, and replaying it
> would publish different content under the same claim.

---

## #237 — receipts and the fake-adapter proof

**`backend/src/cljs/knoxx/backend/domain/publication_receipts.cljs:47`**
> `observed-materialization` is the only path by which `observed` reaches the
> wire. Desired never falls back to observed and observed never defaults to
> desired. When they disagree the CMS shows both, because that disagreement is
> drift and collapsing it would be the single most useful lie this system could
> tell.

---

## #239 — CMS cutover

**`backend/src/cljs/knoxx/backend/infra/routes/publications.cljs:49`**
> **Read this docstring before anything else in the PR.** `resource-records!`
> deliberately calls `load-all-resource-records!` — the *undeduped* loader —
> rather than `load-all-resources!`. `dedup-contracts` applies first-wins dedup
> on `[kind id]`, which would collapse two files declaring the same canonical id
> into whichever the filesystem enumerated first, making #230's conflict
> detection unreachable and the topology dependent on directory order. This looks
> like using the wrong function. It is the right one.

**`routes/publications.cljs:33`**
> Acceptance criterion: *"a rejected resource is a blocker, not an omission."*
> The loader logs and drops a schema-invalid record. Without this the projection
> answers `200` with that publication silently absent. `scripts/verify-publication-epic.sh`
> §5 seeds exactly that case if you want to watch it.

**`routes/publications.cljs:16`**
> `single-kind-definition` is the fiddliest function in the PR. A composite
> manifest entry expands to one record per registered kind, and every expanded
> definition keeps **all** the composite keys. Without projecting each record
> onto its own facet, an entry registering both a document and a publication gets
> indexed twice — the document collapses harmlessly as a byte-equal duplicate,
> but the publication is appended twice and the projection then reports a *false*
> duplicate-relation conflict. Took me a while to see; flagging so it doesn't
> take you as long.

**`backend/src/cljs/knoxx/backend/shape/resource_identity.cljs:91`**
> `encode-wire-values` is hand-rolled recursion rather than `postwalk`
> specifically because `postwalk` visits map entries bottom-up and would encode
> the **keys** along with the values. Keys must stay unqualified — that is this
> codebase's wire convention. Values carry identity and must keep their
> namespace, or `:tenant-a/foo` and `:tenant-b/foo` both arrive as `"foo"`.

**Inconsistency I introduced and would like an opinion on**
> `extern/fastify/publications.cljs:52` builds an error body as
> `{:error <message> :detail <data>}`. `extern/fastify/cms_publication.cljs:53`
> builds it as `{:detail <message> :error <data>}` — the keys are swapped. Both
> shipped. A client cannot write one error handler for the publication path. I
> did not unify them because it changes a wire contract the frontend already
> consumes, but it should be one shape. Tell me which one wins and I'll follow
> up.

**`backend/src/cljs/knoxx/backend/infra/routes/app.cljs:1542`**
> `register-resource-and-media-routes!` crossed clj-kondo's 60-line hard error
> once the publication registration was added, so I split it. That function was
> already at the ceiling before this epic — the split is forced, not a drive-by
> refactor, but it does touch code outside the epic.

**`frontend/src/pages/CmsPage.tsx:781`**
> **Known incompleteness.** `handlePublishToggle` still posts to
> `/api/openplanner/v1/cms/publish/:docId/:gardenId` and only calls
> `setPublicationState` afterwards. Publication *intent* is cut over; the
> surrounding CMS document flow is not. With no OpenPlanner running this control
> cannot complete. That is why the browser tour drives the API rather than
> clicking the toggle.

---

## #240 — REST retirement

**`backend/src/cljs/knoxx/backend/law/publication_surface.cljs:13`**
> Read the NOTE. The card specified `/api/publications/gardens` and
> `/api/publications/health`. **Neither was built** — gardens arrive inside the
> topology response, and there is no health route on this path. Rather than
> quietly shipping a shorter list, the list states what actually exists and
> `surface-count` is asserted against it so silently shortening it fails a test.

**`publication_surface.cljs:74`**
> **This docstring overclaims and I'd rather say so than have you find it.** It
> says retired paths must have "NO shipped caller".
> `/api/openplanner/v1/gardens` still has four, in
> `frontend/src/cljs/knoxx/frontend/pages/gardens/{api,logic}.cljs`. The guard in
> `publication_surface_verify_test.cljs:139` walks an explicit **eight-file
> list** that does not include them, so it passes. This is not a regression — the
> Gardens page was always outside this epic's CMS/translation scope — but the
> wording should either be narrowed to those surfaces or the guard made
> repo-wide. `scripts/verify-publication-epic.sh` §9 prints it as a WARN on every
> run so it stays visible.

**`publication_surface.cljs:81`**
> `retired-deploy-flags` names `KNOXX_EXPECT_OPENPLANNER_REST`, which does not
> exist in this repo — it lives in `open-hax/services`. The flag is retired there
> in open-hax/services#47. This declaration is the contract; that PR is the
> enforcement.

---

## #241 — end-to-end proof

**`backend/test/cljs/knoxx/backend/e2e/contract_publication_test.cljs:30`**
> The no-network harness replaces `js/fetch` with something that **throws**,
> rather than stubbing a response. A stub would let a hidden HTTP call be
> accidentally satisfied and the test would pass while proving the opposite of
> what it claims. The harness is also deliberately generic — it names no backend,
> because importing the dependency you are proving absent defeats the proof.

**`contract_publication_test.cljs:320`**
> This is the eight-file grep list referenced under #240. It is an allow-list,
> not a sweep. That is a deliberate limitation — a repo-wide grep would fail on
> the Gardens page, which is out of scope — but it does mean this test proves
> less than its name suggests. Worth knowing before you rely on it.

---

## How to check any of this

```bash
scripts/verify-publication-epic.sh    # 35 checks incl. §5 and §6 above
scripts/verify-publication-tour.sh    # browser + screenshots
```

See `docs/verification/publication-epic.md`.
