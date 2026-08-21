(ns knoxx.backend.law.publication
  "Malli contracts for document, garden, and publication resources.

  Publication is a relation over document, garden, locale, and revision — a
  document-level `published` boolean cannot describe it. These resources
  declare desired state only (source location, target garden/locale/revision,
  requested publication state, translation/review policy); runtime
  observations of whether a deployment effect actually succeeded live in
  reconciliation/receipts, not here. No external publication-backend type,
  route, collection, or identifier is required to satisfy any schema in this
  namespace."
  (:require [clojure.string :as str]
            [malli.core :as m]
            [malli.error :as me]))

;; ── Locale ─────────────────────────────────────────────────────────────────

(defn locale-keyword?
  "A bare language tag (`:en`) or dashed variant (`:en-US`), as a keyword.
   Namespaced document/garden ids are validated separately via
   `qualified-keyword?` — locales are never namespaced."
  [value]
  (and (keyword? value)
       (nil? (namespace value))
       (boolean (re-matches #"[A-Za-z]{2,3}(-[A-Za-z0-9]{1,8})*" (name value)))))

(def Locale
  [:and keyword? [:fn locale-keyword?]])

;; ── Publication path ───────────────────────────────────────────────────────

(defn valid-publication-path?
  "The one authoritative publication-path predicate. Reused by migration and
   direct resource validation so directly authored resources cannot bypass
   the route validation that migrated resources must satisfy."
  [path]
  (boolean
   (and (string? path)
        (seq path)
        (str/starts-with? path "/")
        (not (str/includes? path "?"))
        (not (str/includes? path "#"))
        (not (str/includes? path "\u0000")))))

(def PublicationPath
  [:and string? [:fn valid-publication-path?]])

;; ── Resource shapes ────────────────────────────────────────────────────────

(defn nonblank-string?
  [value]
  (and (string? value) (seq (str/trim value))))

(def Document
  [:map
   [:document/id qualified-keyword?]
   [:document/title string?]
   [:document/source-locale Locale]
   [:document/source
    [:map
     [:path [:fn nonblank-string?]]]]])

(def Garden
  [:map
   [:garden/id qualified-keyword?]
   [:garden/title string?]
   [:garden/status [:enum :active :archived]]])

(def PublicationRevision
  [:or [:fn nonblank-string?] [:enum :source/current]])

;; Raw declarative relation stored in resource data.
(def PublicationIntentResource
  [:map
   [:publication/id qualified-keyword?]
   [:publication/document qualified-keyword?]
   [:publication/garden qualified-keyword?]
   [:publication/locale Locale]
   [:publication/revision PublicationRevision]
   [:publication/state [:enum :published :withheld :archived]]
   [:publication/path PublicationPath]
   [:translation/review [:enum :none :required]]])

;; Resolved pure-domain view. Source locale is copied from the referenced
;; document after reference validation; it is not duplicated as resource
;; truth, so translation gating never has to guess a default. Fields are
;; repeated from PublicationIntentResource rather than composed via
;; `:merge` — this project's malli setup throws :malli.core/invalid-schema
;; for `:merge` children whose entries hold bare predicate fns.
(def PublicationIntent
  [:map
   [:publication/id qualified-keyword?]
   [:publication/document qualified-keyword?]
   [:publication/garden qualified-keyword?]
   [:publication/locale Locale]
   [:publication/revision PublicationRevision]
   [:publication/state [:enum :published :withheld :archived]]
   [:publication/path PublicationPath]
   [:translation/review [:enum :none :required]]
   [:document/source-locale Locale]])

;; ── Projection views ───────────────────────────────────────────────────────

;; Facade shapes for the resolver's desired-topology projection. Deliberately
;; concrete rather than `[:vector :map]`: the CMS facade reads a document's
;; identity at [:document :document/id], so a view assembled from a document
;; that never got a canonical id must fail here rather than reaching the wire
;; encoder. Publications are the hydrated `PublicationIntent`, not the raw
;; resource, because a view is only assembled after reference resolution.
(def PublicationDocumentView
  [:map
   [:document Document]
   [:publications [:vector PublicationIntent]]])

(def PublicationListView
  [:map
   [:documents [:vector PublicationDocumentView]]
   [:gardens [:vector Garden]]])

;; ── Boundary helpers ───────────────────────────────────────────────────────

(defn assert-valid!
  "Return value when it satisfies schema; otherwise throw a named boundary
   contract violation."
  [contract-id schema value]
  (if (m/validate schema value)
    value
    (throw
     (ex-info (str "Publication resource contract violation: " contract-id)
              {:contract contract-id
               :errors (me/humanize (m/explain schema value))}))))

(defn index-resources
  "Build a `{:documents {...} :gardens {...} :publications {...}}` index from a
   flat collection of resource maps, keyed by each resource's own id."
  [resources]
  (reduce
   (fn [index resource]
     (cond-> index
       (contains? resource :document/id)
       (assoc-in [:documents (:document/id resource)] resource)

       (contains? resource :garden/id)
       (assoc-in [:gardens (:garden/id resource)] resource)

       (contains? resource :publication/id)
       (assoc-in [:publications (:publication/id resource)] resource)))
   {:documents {} :gardens {} :publications {}}
   resources))

(defn hydrate-publication-intent
  "Copy the referenced document's validated source locale onto intent, so
   translation laws never guess `:en`. Throws rather than defaulting a locale
   when the document reference is dangling."
  [resource-index intent]
  (let [document (get-in resource-index [:documents (:publication/document intent)])]
    (assert-valid! (:publication/document intent) Document document)
    (assoc intent :document/source-locale (:document/source-locale document))))

(def reconcilable-publication-states
  "Desired states that may take part in reconciliation. `:published` reconciles
   toward a materialization and `:withheld` reconciles toward its removal, so
   both are lawful inputs to a plan. `:archived` is terminal and never
   reconciles. Membership is an allow-list rather than a `not= :archived`
   denial so that a missing or unrecognized state on an intent that has not
   been validated against `PublicationIntentResource` fails closed."
  #{:published :withheld})

(defn admissible-publication?
  "True when the intent's desired state is reconcilable, both the document and
   garden references resolve, and the garden is active. Archived gardens,
   archived or unrecognized intent states, and dangling references can never
   reconcile to a public materialization."
  [resource-index intent]
  (boolean
   (and (contains? reconcilable-publication-states (:publication/state intent))
        (contains? (:documents resource-index) (:publication/document intent))
        (contains? (:gardens resource-index) (:publication/garden intent))
        (= :active
           (:garden/status
            (get-in resource-index [:gardens (:publication/garden intent)]))))))
