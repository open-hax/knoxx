---
uuid: "knoxx-publication-resource-contracts"
title: "Define document, garden, and publication resource contracts"
status: breakdown
priority: P0
labels: ["tasks", "5sp", "has-parent", "cms", "publication", "contracts"]
created_at: "2026-08-12T00:00:00Z"
points: 5
category: tasks
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
- Add resource-loader validation and focused law tests.
- Hydrate the resolved publication intent with the owning document's validated source locale so translation laws never guess `:en`.

## CLJS pseudocode

```clojure
(ns knoxx.backend.law.publication)

(def Locale
  [:and :keyword [:fn qualified-or-language-keyword?]])

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
   [:publication/path :string]
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
- `document × garden × locale` must not resolve to two conflicting active publication intents for the same revision selector.
- Unknown document/garden refs fail validation rather than becoming dangling runtime work.
- Every document declares its source locale; translation gating receives that resolved value and never supplies a language default.
- Archive semantics must be explicit: archived garden or publication intent cannot reconcile to a public materialization.

## Done when

- A resource-only test fixture can describe multiple documents and mixed publication states across gardens/locales.
- Invalid references, invalid/missing source locales, and conflicting publication intents fail before effectful reconciliation.
- The translation gate consumes the source locale resolved from the owning document.
- No OpenPlanner type, route, collection, or identifier is required by the laws.
