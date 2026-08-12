---
category: "tasks"
labels: ["tasks", "5sp", "has-parent", "cms", "publication", "contracts"]
write-id: "1786565774217-0.xvewz36ji1eoetarnvl"
points: "5"
title: "Define document, garden, and publication resource contracts"
priority: "P0"
status: "ready"
uuid: "knoxx-publication-resource-contracts"
created_at: "2026-08-12T00:00:00Z"
---

# Define document, garden, and publication resource contracts

> Parent epic: `knoxx-contract-owned-publication-pipeline`

## Purpose

Make document identity and publication intent fully describable from Knoxx resources, without consulting OpenPlanner or any mutable projection.

Publication is a relation over document, garden, locale, and revision. A document-level `published` boolean is explicitly insufficient.

## Scope

- Add Malli-backed law shapes for first-class `document`, `garden`, and `publication` resource facets/kinds.
- Keep desired state declarative: source location, source locale, title/identity, target garden, route/path, locale, revision selector, requested publication state, translation/review policy.
- Keep runtime observations out of the resource shape.
- Define canonical identity and reference resolution rules for cross-namespace document/garden/publication references.
- Define one authoritative publication-path predicate in the resource law and reuse it in migration/adapters; directly authored resources cannot bypass route validation that migrated resources must satisfy.
- Add resource-loader validation and focused law tests.
- Hydrate the resolved publication intent with the owning document's validated source locale so translation laws never guess `:en`.

## CLJS pseudocode

```clojure
(ns knoxx.backend.law.publication
  (:require [clojure.string :as str]))

(def Locale
  [:and :keyword [:fn qualified-or-language-keyword?]])

(defn valid-publication-path? [path]
  (and (string? path)
       (seq path)
       (str/starts-with? path "/")
       (not (str/includes? path "?"))
       (not (str/includes? path "#"))
       (not (str/includes? path "\u0000"))))

(def PublicationPath
  [:and :string [:fn valid-publication-path?]])

(def Document
  [:map
   [:document/id qualified-keyword?]
   [:document/title :string]
   [:document/source-locale Locale]
   [:document/source
    [:map
     [:path :string]]]])

(def Garden
  [:map
   [:garden/id qualified-keyword?]
   [:garden/title :string]
   [:garden/status [:enum :active :archived]]])

;; Raw declarative relation stored in resource data.
(def PublicationIntentResource
  [:map
   [:publication/id qualified-keyword?]
   [:publication/document qualified-keyword?]
   [:publication/garden qualified-keyword?]
   [:publication/locale Locale]
   [:publication/revision [:or :string [:enum :source/current]]]
   [:publication/state [:enum :published :withheld :archived]]
   [:publication/path PublicationPath]
   [:translation/review [:enum :none :required]]])

;; Resolved pure-domain view. Source locale is copied from the referenced
;; document after reference validation; it is not duplicated as resource truth.
(def PublicationIntent
  [:merge PublicationIntentResource
   [:map [:document/source-locale Locale]]])

(defn hydrate-publication-intent [resource-index intent]
  (let [document (get-in resource-index
                         [:documents (:publication/document intent)])]
    (law/assert! Document document)
    (assoc intent :document/source-locale
                  (:document/source-locale document))))

(defn admissible-publication? [resource-index intent]
  (and (contains? (:documents resource-index) (:publication/document intent))
       (contains? (:gardens resource-index) (:publication/garden intent))
       (not= :archived
             (:garden/status
              (get-in resource-index [:gardens (:publication/garden intent)])))))
```

Example manifest intent:

```clojure
{:namespace :knoxx.docs
 :resources
 [{:document/id :translation-pipeline
   :document/title "Translation Pipeline"
   :document/source-locale :en
   :document/source {:path "docs/translation-pipeline.md"}}

  {:publication/id :translation-pipeline-es
   :publication/document :knoxx.docs/translation-pipeline
   :publication/garden :gardens/promethean
   :publication/locale :es
   :publication/revision :source/current
   :publication/state :published
   :publication/path "/translation-pipeline"
   :translation/review :required}]}
```

## Contract obligations

- Resource shape says what **should** be true, never whether a deployment side effect succeeded.
- `document × garden × locale × revision` must not resolve to two conflicting non-archived publication intents for the same relation.
- Unknown document/garden refs fail validation rather than becoming dangling runtime work.
- Every document declares its source locale; translation gating receives that resolved value and never supplies a language default.
- Publication paths are route paths: non-empty, rooted at `/`, and free of query/fragment/NUL components; the same predicate is used by migration and direct resource validation.
- Archive semantics must be explicit: archived garden or publication intent cannot reconcile to a public materialization.

## TDD plan

Test namespace: `knoxx.backend.law.publication-test`
(`backend/test/cljs/knoxx/backend/law/publication_test.cljs`).

Write these tests first; each must fail for the absence of the law, not for a typo.

1. `valid-publication-path?-accepts-rooted-route` — `"/translation-pipeline"` and
   `"/docs/nested/route"` pass.
2. `valid-publication-path?-rejects-malformed-routes` — table-driven over `""`,
   `"translation-pipeline"` (unrooted), `"/docs?x=1"`, `"/docs#frag"`,
   `"/docs\u0000"`, and non-string input. Each must be rejected by the one
   authoritative predicate.
3. `document-shape-requires-source-locale` — a document without
   `:document/source-locale`, and one whose locale is a string rather than a
   keyword, both fail `Document`.
4. `garden-shape-enumerates-status` — `:active` / `:archived` pass, `"active"`
   and `:deleted` fail.
5. `publication-intent-resource-validates-relation` — the full example manifest
   intent passes; each of a missing garden ref, a non-keyword document ref, a
   `:publication/state` of `"published"`, and a malformed
   `:publication/path` fails.
6. `publication-intent-accepts-both-revision-forms` — `"abc123"` and
   `:source/current` both satisfy `:publication/revision`; `nil` and `42` fail.
7. `hydrate-publication-intent-copies-document-source-locale` — hydration
   yields `:document/source-locale` taken from the referenced document and the
   result satisfies `PublicationIntent`.
8. `hydrate-publication-intent-rejects-dangling-document` — hydrating against a
   resource index without the referenced document throws rather than defaulting
   a locale.
9. `admissible-publication?-requires-active-garden` — true for an active
   garden; false for an archived garden and for unknown document/garden refs.
10. `resource-fixture-describes-mixed-publication-topology` — one
    resource-only fixture with two documents, two gardens, and mixed
    published/withheld/archived intents across locales and revisions loads and
    validates with no OpenPlanner namespace required (assert the fixture's
    transitive requires contain no `openplanner` segment).

Only after all ten fail do we add `knoxx.backend.law.publication`, then the
loader validation hook, then re-run to green.

## Done when

- A resource-only test fixture can describe multiple documents and mixed publication states across gardens/locales/revisions.
- Invalid references, invalid/missing source locales, malformed publication paths, and conflicting publication intents fail before effectful reconciliation.
- Directly authored `""`, non-rooted, query-bearing, fragment-bearing, and NUL-bearing publication paths fail the same authoritative predicate used by migration.
- The translation gate consumes the source locale resolved from the owning document.
- No OpenPlanner type, route, collection, or identifier is required by the laws.

---
Ready gate 2026-08-12: sized 5sp (<=5, eligible to implement). Scope, laws and acceptance criteria confirmed on the card; TDD plan section names the failing tests to write first. Walked accepted -> breakdown -> ready via the Rheos promethean FSM.
---
Implementation 2026-08-12: added `knoxx.backend.law.publication`
(`backend/src/cljs/knoxx/backend/law/publication.cljs`) with `Locale`,
`PublicationPath`/`valid-publication-path?`, `Document`, `Garden`,
`PublicationIntentResource`/`PublicationIntent`, `hydrate-publication-intent`,
`admissible-publication?`, and an `index-resources` helper, plus all ten TDD
tests from this card's plan in
`backend/test/cljs/knoxx/backend/law/publication_test.cljs`.
Could not run `pnpm -C backend test:shadow` / `lint:kondo` in this session:
the sandbox's egress policy denies `repo.clojars.org` (403), which
shadow-cljs needs to resolve its own JVM dependency before it can compile
anything. Status left at `ready` rather than hand-forged to `in_progress` —
direct frontmatter edits bypass the Rheos ledger and read as drift. Next
session with real network/CLI access should run the test suite, then drive
`ready -> todo -> in_progress -> testing` through
`node packages/rheos/dist/cli.cjs status-update knoxx-publication-resource-contracts --to <status>`.
---