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
             [malli.error :as me]
             [knoxx.backend.law.publication-locale :as locale]))

;; ── Locale ─────────────────────────────────────────────────────────────────

(def Locale
  "Re-exported portable language tag contract."
  locale/Locale)

(def LocaleCatalog
  "A target's explicit, non-empty, duplicate-free catalog of accepted locales."
  locale/LocaleCatalog)

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

(def NonBlankString
  "Named so migration and other boundaries can require the same string law
   rather than restating the predicate inline."
  [:fn nonblank-string?])

(def Document
  [:map
   [:document/id qualified-keyword?]
   [:document/title string?]
   [:document/source-locale Locale]
   [:document/source
    [:map
     [:path NonBlankString]]]
   [:document/translations {:optional true}
    [:map-of Locale
     [:map
      [:path NonBlankString]]]]])

(def Garden
  [:map
   [:garden/id qualified-keyword?]
   [:garden/title string?]
   [:garden/status [:enum :active :archived]]
   [:garden/locales LocaleCatalog]])

(def PublicationRevision
  "A revision *selector*: either a concrete revision or the `:source/current`
   token that resolves to one. Correct for declared desired state, which is
   allowed to say \"whatever is current\"."
  [:or NonBlankString [:enum :source/current]])

(def ConcreteRevision
  "One immutable source state, never a selector.

   Anything keyed by a revision needs this rather than `PublicationRevision`: a
   key meaning \"whatever is current\" is not a key but a moving target, and
   replaying it would publish different content under the same claim while
   reporting the operation as already done. Stated next to the selector law so
   the distinction is visible where either might be reached for."
  NonBlankString)

(def PublicationState
  "Every state a publication resource may declare.

   Named rather than inlined so the two resource shapes below and any boundary
   that validates a state edit read one vocabulary. `reconcilable-publication-states`
   and `publishing-publication-states` further down are deliberately *narrower*
   subsets of this — a state can be lawful to declare and still not be a lawful
   input to reconciliation."
  [:enum :published :withheld :archived])

;; Raw declarative relation stored in resource data.
(def PublicationIntentResource
  [:map
   [:publication/id qualified-keyword?]
   [:publication/document qualified-keyword?]
   [:publication/garden qualified-keyword?]
   [:publication/target {:optional true} qualified-keyword?]
   [:publication/locale Locale]
   [:publication/revision PublicationRevision]
   [:publication/state PublicationState]
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
   [:publication/target {:optional true} qualified-keyword?]
   [:publication/locale Locale]
   [:publication/revision PublicationRevision]
   [:publication/state PublicationState]
   [:publication/path PublicationPath]
   [:translation/review [:enum :none :required]]
   [:document/source-locale Locale]
   [:document/title string?]])

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

(def PublicationGardenView
  "One deployed Garden contract with the publication placements that target it.

   Content does not live here, and presentation does not live here. The view
   only relates a Garden's stable identity/locale catalog to the publication
   intents whose paths are placed in that Garden."
  [:map
   [:garden Garden]
   [:publications [:vector PublicationIntent]]])

(def PublicationGardenListView
  [:map
   [:gardens [:vector PublicationGardenView]]])

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
  "Copy the referenced document's validated source locale and title onto
   intent, so translation laws never guess `:en` and the manifest can name a
   route without a second document lookup at the effect boundary. Throws rather
   than defaulting a locale when the document reference is dangling.

   Both are copied from the document AFTER reference validation rather than
   duplicated as resource truth, so neither can drift from the document that
   owns it."
  [resource-index intent]
  (let [document (get-in resource-index [:documents (:publication/document intent)])]
    (assert-valid! (:publication/document intent) Document document)
    (assoc intent
           :document/source-locale (:document/source-locale document)
           :document/title (:document/title document))))

(def reconcilable-publication-states
  "Desired states that may take part in reconciliation. `:published` reconciles
   toward a materialization and `:withheld` reconciles toward its removal, so
   both are lawful inputs to a plan. `:archived` is terminal and never
   reconciles. Membership is an allow-list rather than a `not= :archived`
   denial so that a missing or unrecognized state on an intent that has not
   been validated against `PublicationIntentResource` fails closed."
  #{:published :withheld})

(def publishing-publication-states
  "The desired states that ask for a public materialization.

   A strict subset of `reconcilable-publication-states`: `:withheld` reconciles
   toward *removal*, which is a lawful plan but not a request to publish. Stated
   here, next to the set it refines, so the contract layer and the evidence gate
   in `domain.publication-gate` cannot end up disagreeing about which state means
   \"publish\" — they read one vocabulary rather than each restating it."
  #{:published})

(defn publishes?
  "True when the intent asks for a public materialization at all.

   This answers only \"what does the resource want\". Whether that want may be
   satisfied *now* is two further questions, deliberately kept apart:
   `admissible-publication?` below decides structural admissibility from the
   resource graph, and `domain.publication-gate/admissible?` decides evidential
   admissibility from receipts. Receipts never enter this layer."
  [intent]
  (contains? publishing-publication-states (:publication/state intent)))

(defn admissible-publication?
  "True when the intent's desired state is reconcilable, both the document and
   garden references resolve, and the garden is active. Archived gardens,
   archived or unrecognized intent states, and dangling references can never
   reconcile to a public materialization.

   Structural admissibility only — it reads the resource graph and no receipt.
   The evidential half lives in `domain.publication-gate`."
  [resource-index intent]
  (boolean
   (and (contains? reconcilable-publication-states (:publication/state intent))
        (contains? (:documents resource-index) (:publication/document intent))
         (contains? (:gardens resource-index) (:publication/garden intent))
         (= :active
            (:garden/status
             (get-in resource-index [:gardens (:publication/garden intent)])))
         (contains? (set (:garden/locales
                          (get-in resource-index [:gardens (:publication/garden intent)])))
                    (:publication/locale intent)))))

(defn publication-locale-blocker
  "Return the explicit reconciliation blocker when `intent` asks a target for an
   unsupported locale, otherwise nil. This names the failed relation rather than
   returning a generic inadmissible result so reconciliation records why it
   declined to materialize a route."
  [resource-index intent]
  (let [target (get-in resource-index [:gardens (:publication/garden intent)])]
    (when (and target
               (not (contains? (set (:garden/locales target))
                               (:publication/locale intent))))
      :publication-locale-unsupported)))

;; ── The materialized artifact ──────────────────────────────────────────────

(defn artifact-bytes?
  "Bytes as this runtime spells them. A *predicate* over a value arriving from
   above — not interop that decodes, encodes, or mutates anything — and the one
   thing here with no portable spelling, so it is also why this namespace stays
   `.cljs`. Non-empty, matching `nonblank-string?` on the string side: an
   artifact carrying no content is not something to publish."
  [value]
  (and (instance? js/Uint8Array value)
       (pos? (alength value))))

(def ArtifactContent
  "Bytes or a string. Both are content; the difference is only whether the
   renderer already applied `:artifact/encoding` or left it to the adapter."
  [:or NonBlankString [:fn artifact-bytes?]])

(defn valid-media-type?
  "`type/subtype`, with no parameters.

   `text/html; charset=utf-8` is refused on purpose: the character encoding is
   its own declared field, and accepting it in two places is accepting that the
   two can disagree, after which nothing decides which one the adapter honours."
  [value]
  (boolean
   (and (string? value)
        (re-matches #"[A-Za-z0-9][A-Za-z0-9!#$&^_.+-]*/[A-Za-z0-9][A-Za-z0-9!#$&^_.+-]*"
                    value))))

(def MediaType
  [:and string? [:fn valid-media-type?]])

(defn valid-character-encoding?
  "An IANA charset name (`utf-8`), declared rather than defaulted.

   A static file server told nothing serves bytes and lets the reader guess, and
   a guess that lands wrong corrupts exactly the accented text a translation
   pipeline exists to deliver — only in the locales nobody proofreads."
  [value]
  (boolean
   (and (string? value)
        (re-matches #"[A-Za-z0-9][A-Za-z0-9._:+-]*" value))))

(def CharacterEncoding
  [:and string? [:fn valid-character-encoding?]])

(def revision-selector-namespace
  "The keyword namespace every revision *selector* lives in. Named as a
   namespace rather than as the single member `:source/current`, so a sibling
   selector cannot be introduced without inheriting the refusal below."
  "source")

(defn revision-selector?
  [value]
  (and (keyword? value)
       (= revision-selector-namespace (namespace value))))

(defn free-of-revision-selectors?
  "No selector keyword anywhere in `value` — nested, and in map keys too.

   Refused for the same reason `publish-idempotency-key` refuses one: a selector
   gives a stable-looking identity to a moving target. `:artifact/revision` is
   already `ConcreteRevision`, so this covers everything *else* an artifact may
   carry — an extra key holding `:source/current` would otherwise reach an
   adapter and be written beside the bytes as \"whatever is current\", forever."
  [value]
  (not-any? revision-selector? (tree-seq coll? seq value)))

(def PublicationArtifact
  "The bytes a publication materializes, plus everything an adapter needs to put
   them somewhere without re-deciding anything.

   Produced ABOVE the effect boundary. `infra.publication-effects/execute-plan!`
   receives one as an argument; no `IPublicationTarget` method returns one and no
   adapter constructs one. One renderer, adapters that only transport. The
   rejected alternative — producing it below, each adapter rendering from the
   intent it was handed — reads cheaper and is the shape that diverges: two
   targets asked to publish the same intent at the same revision can then serve
   different bytes, and nothing in the receipt chain can say which is right.
   Pinned by `the-artifact-is-produced-above-the-effect-boundary`, not left as
   prose.

    `:artifact/revision` is cross-checked against the op, by
   `artifact-revision-conflict` below: it is the axis `publish-idempotency-key`
   is built from, so a disagreement there is the difference between replay-safe
   and republishing other content under a key that already reports `done`.
    `:artifact/locale` is cross-checked against intent at the effect boundary
    before an adapter can derive an artifact path from it."
  [:and
   [:map
    [:artifact/content ArtifactContent]
    [:artifact/media-type MediaType]
    [:artifact/encoding CharacterEncoding]
    [:artifact/locale Locale]
    [:artifact/revision ConcreteRevision]]
   [:fn {:error/message "a publication artifact must carry no revision selector"}
    free-of-revision-selectors?]])

(def ArtifactRevisionConflict
  "The renderer and the planner disagreeing about what is being published. Both
   revisions are `:any` on purpose: this record exists to carry values that
   FAILED the revision law, so typing them as `ConcreteRevision` would make the
   evidence unrepresentable in its own contract."
  [:map
   [:conflict/type [:= :publication/artifact-revision-conflict]]
   [:conflict/artifact-revision :any]
   [:conflict/concrete-revision :any]])

(def artifact-locale-identity-decision
  "The locale identity decision for publication adapters: cross-check the
   renderer artifact against publication intent before effects. Adapters may use
   `:artifact/locale` to address bytes only after this boundary has established
   equality, preventing wrong-language bytes behind an intent-derived route."
  :cross-check)

(def ArtifactLocaleConflict
  "Re-exported portable artifact-versus-intent locale conflict contract."
  locale/ArtifactLocaleConflict)

(defn artifact-revision-conflict
  "nil when the artifact and the op agree about what is being published;
   otherwise the typed conflict, carrying BOTH revisions.

   A conflict rather than a warning, because there is no safe way to pick a
   winner: the bytes were rendered from one source state and the idempotency key
   names another, so publishing either way records a materialization that did
   not happen."
  [artifact concrete-revision]
  (let [artifact-revision (:artifact/revision artifact)]
    (when (not= artifact-revision concrete-revision)
      {:conflict/type :publication/artifact-revision-conflict
       :conflict/artifact-revision artifact-revision
       :conflict/concrete-revision concrete-revision})))

(defn artifact-revision-conflict?
  "True when `value` is the record `artifact-revision-conflict` produces, so the
   effect boundary can carry the conflict onto a receipt instead of flattening it
   into a message string where both revisions would be lost."
  [value]
  (m/validate ArtifactRevisionConflict value))

(defn artifact-locale-conflict
  "nil when `artifact` and `intent` name the same locale; otherwise return the
   typed conflict that preserves both values for a failed receipt."
  [artifact intent]
  (locale/artifact-locale-conflict artifact intent))

(defn artifact-locale-conflict?
  "True when `value` is the typed locale-identity conflict emitted at the
   publication effect boundary."
  [value]
  (locale/artifact-locale-conflict? value))

(defn assert-artifact!
  "Return the artifact when it is publishable at `concrete-revision`; otherwise
   throw.

   Shape first, then agreement — a malformed artifact has no revision worth
   comparing. The three-argument form additionally cross-checks locale identity
   before adapter effects. Called above any effect, so nothing is reserved,
   written, or recorded on behalf of a publication that cannot lawfully happen."
  ([artifact concrete-revision]
   (assert-artifact! artifact nil concrete-revision))
  ([artifact intent concrete-revision]
   (assert-valid! :publication/artifact PublicationArtifact artifact)
   (when-let [conflict (artifact-revision-conflict artifact concrete-revision)]
     (throw
      (ex-info (str "Publication artifact revision conflict: the artifact was "
                    "rendered from a different source state than the plan publishes")
               conflict)))
   (when (some? intent)
     (when-let [conflict (artifact-locale-conflict artifact intent)]
       (throw
        (ex-info (str "Publication artifact locale conflict: the artifact locale "
                      "differs from the publication intent")
                 conflict))))
   artifact))
