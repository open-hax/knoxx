(ns knoxx.backend.domain.publication-resolver
  "Pure projection from loaded resources to the desired publication topology.

  This is the one query model CMS, translation policy, reconciliation, and
  deploy verification read from. It consumes already-parsed resource maps and
  returns deterministic CLJS data — no HTTP, no Mongo, no filesystem, no
  external publication backend, and no worker/receipt state. Observed
  execution facts belong to reconciliation and receipts, never here.

  Identity is the load-bearing concern. A namespace manifest expands to
  entries carrying a namespace-local id (`:translation-pipeline`) plus the
  manifest's `:namespace`, while a standalone resource file carries an
  already-qualified id and no namespace. Both must land on the same canonical
  keyword before anything is indexed, filtered, compared, or keyed — including
  inside the stored payloads, since the law shapes require qualified ids."
  (:require [clojure.string :as str]
            [knoxx.backend.law.publication :as law]
            [knoxx.backend.shape.resource-identity :as identity]))

;; ── Canonical identity ─────────────────────────────────────────────────────

(def canonical-id
  "The canonical identity rule, owned by `shape.resource-identity` so the
   contract loader and this resolver cannot drift apart. The loader applies it
   before validation; the resolver applies it again because standalone resource
   files never pass through manifest expansion."
  identity/canonical-id)

(defn- resource-namespace
  "The manifest namespace an expanded entry was defined under, if any.
   `katamorph.manifest/entry-definition` stamps `:namespace`; standalone
   resource files have none."
  [resource]
  (:namespace resource))

;; Only declared fields cross into the projection. Malli map schemas are open,
;; so a resource that also carries an execution fact — a receipt timestamp, a
;; worker phase — would otherwise validate happily and leak that fact into the
;; desired-state view. Selecting is what actually enforces "resources declare
;; desired state only"; the schema alone cannot.

(def document-projection-keys
  [:document/id :document/title :document/source-locale :document/source])

(def garden-projection-keys
  [:garden/id :garden/title :garden/status])

(def publication-projection-keys
  [:publication/id :publication/document :publication/garden :publication/locale
   :publication/revision :publication/state :publication/path :translation/review])

(defn canonicalize-document
  [resource]
  (-> resource
      (update :document/id #(canonical-id (resource-namespace resource) %))
      (select-keys document-projection-keys)))

(defn canonicalize-garden
  [resource]
  (-> resource
      (update :garden/id #(canonical-id (resource-namespace resource) %))
      (select-keys garden-projection-keys)))

(defn canonicalize-intent
  "Canonicalize the intent's own id and both of its outbound references, so a
   manifest-local `:publication/document :translation-pipeline` and a
   standalone `:knoxx.docs/translation-pipeline` resolve to one relation."
  [resource]
  (let [ns-value (resource-namespace resource)
        qualify #(canonical-id ns-value %)]
    (-> resource
        (update :publication/id qualify)
        (update :publication/document qualify)
        (update :publication/garden qualify)
        (select-keys publication-projection-keys))))

;; ── Relation identity ──────────────────────────────────────────────────────

(defn publication-key
  "Publication is a relation over document × garden × locale × revision. The
   revision selector is part of identity: two intents targeting different
   revisions of the same document/garden/locale are distinct relations, not a
   conflict."
  [intent]
  [(:publication/document intent)
   (:publication/garden intent)
   (:publication/locale intent)
   (:publication/revision intent)])

(defn publication-sort-key
  "Total order over intents. `pr-str` on the revision keeps string and keyword
   selectors mutually comparable; `:publication/id` breaks remaining ties so
   the order never depends on load order."
  [intent]
  [(pr-str (:publication/document intent))
   (pr-str (:publication/garden intent))
   (pr-str (:publication/locale intent))
   (pr-str (:publication/revision intent))
   (pr-str (:publication/id intent))])

(defn active-publication-intent?
  "Archived intent is retained history, not a live claim on a relation, so it
   never participates in conflict detection."
  [intent]
  (not= :archived (:publication/state intent)))

(defn publication-conflicts
  "Non-archived intents that claim the same relation. Returned in a stable
   order so the same resource graph reports the same conflicts regardless of
   file enumeration order."
  [publications]
  (->> publications
       (filter active-publication-intent?)
       (group-by publication-key)
       (keep (fn [[relation intents]]
               (when (> (count intents) 1)
                 {:publication/key relation
                  :intents (vec (sort-by publication-sort-key intents))})))
       (sort-by #(mapv pr-str (:publication/key %)))
       vec))

;; ── Indexing ───────────────────────────────────────────────────────────────

(defn- stable-payload-key
  "Order-independent rendering of a payload, used only to sort a conflicting
   pair. Normalizing the top-level key order means the rendering does not
   depend on the order the EDN reader happened to build the map in."
  [payload]
  (pr-str (into (sorted-map) payload)))

(defn index-canonical!
  "Insert a canonicalized payload under its canonical id. Byte-equivalent
   duplicates collapse; differing payloads for one canonical id are a hard
   failure rather than a last-writer-wins silent overwrite.

   The conflicting pair is reported as a sorted vector under one key rather
   than as `:existing`/`:incoming`, because which payload is already indexed
   depends purely on loader enumeration order — positional keys would make the
   same conflict throw two different-looking errors across runs."
  [index kind id resource]
  (let [path [kind id]
        existing (get-in index path)]
    (cond
      (nil? existing) (assoc-in index path resource)
      (= existing resource) index
      :else
      (throw
       (ex-info "conflicting canonical resource identity"
                {:resource/kind kind
                 :resource/id id
                 :conflicting-payloads (vec (sort-by stable-payload-key
                                                     [existing resource]))})))))

(defn reference-blockers
  "Intents whose document or garden reference does not resolve.

   Both directions were previously silent. `list-document-views` iterates the
   *documents* it has, so an intent pointing at a missing document simply never
   appeared; and hydration validates only the document, so a missing garden
   passed straight through. Either way the caller received a successful but
   incomplete desired topology, which is exactly the semantic blocker this
   projection is supposed to surface."
  [index]
  (->> (:publications index)
       (mapcat (fn [intent]
                 (cond-> []
                   (not (contains? (:documents index) (:publication/document intent)))
                   (conj {:publication/id (:publication/id intent)
                          :blocker :unresolved-document
                          :reference (:publication/document intent)})

                   (not (contains? (:gardens index) (:publication/garden intent)))
                   (conj {:publication/id (:publication/id intent)
                          :blocker :unresolved-garden
                          :reference (:publication/garden intent)}))))
       (sort-by #(mapv pr-str [(:publication/id %) (:blocker %)]))
       vec))

(defn- index-one
  "Index whichever kinds a single resource registers. A composite manifest entry
   may register more than one, so these are independent `if`s rather than a
   `cond`."
  [index resource]
  (cond-> index
    (:document/id resource)
    (as-> idx (let [document (canonicalize-document resource)]
                (index-canonical! idx :documents (:document/id document) document)))

    (:garden/id resource)
    (as-> idx (let [garden (canonicalize-garden resource)]
                (index-canonical! idx :gardens (:garden/id garden) garden)))

    (:publication/id resource)
    (update :publications conj (canonicalize-intent resource))))

(defn publication-identity-conflicts
  "Intents that share one canonical `:publication/id` while disagreeing on what
   it stands for.

   `publication-conflicts` keys on the *relation* — document × garden × locale ×
   revision — so two intents claiming one id for two different relations slip
   past it. Publications are also accumulated into a vector rather than indexed
   through `index-canonical!`, so nothing else catches the collision either. The
   result would be a single canonical id standing for two different payloads in
   the very view the CMS reads identity from.

   Byte-equal duplicates collapse, exactly as they do for documents and gardens
   — only genuinely unequal payloads are a conflict. The pair is sorted by the
   same order-independent rendering `index-canonical!` uses, so the same graph
   reports the same conflict whatever order the loader enumerated it in."
  [publications]
  (->> publications
       (group-by :publication/id)
       (keep (fn [[id intents]]
               (let [payloads (distinct intents)]
                 (when (> (count payloads) 1)
                   {:resource/kind :publications
                    :resource/id id
                    :conflicting-payloads (vec (sort-by stable-payload-key payloads))}))))
       (sort-by #(pr-str (:resource/id %)))
       vec))

(defn- assert-no-identity-conflicts!
  [index]
  (let [conflicts (publication-identity-conflicts (:publications index))]
    (when (seq conflicts)
      (throw (ex-info "conflicting canonical resource identity"
                      {:resource/kind :publications
                       :conflicting-identities conflicts
                       :conflicting-payloads (into []
                                                   (mapcat :conflicting-payloads)
                                                   conflicts)})))))

(defn- assert-no-conflicts!
  [index]
  (let [conflicts (publication-conflicts (:publications index))]
    (when (seq conflicts)
      (throw (ex-info "conflicting publication intents" {:conflicts conflicts})))))

(defn- assert-references-resolve!
  [index]
  (let [blockers (reference-blockers index)]
    (when (seq blockers)
      (throw (ex-info "unresolved publication references" {:blockers blockers})))))

(defn publication-index
  "Build the canonical index for a resource collection.

   Documents and gardens are keyed by canonical id with canonical ids also
   written back into their payloads, so a payload pulled out of the index
   satisfies the qualified law shapes. Publications stay a sorted vector —
   many intents legitimately share a document."
  [resources]
  (let [index (reduce index-one {:documents {} :gardens {} :publications []} resources)]
    (assert-no-conflicts! index)
    (assert-no-identity-conflicts! index)
    (assert-references-resolve! index)
    (update index :publications #(vec (sort-by publication-sort-key %)))))

;; ── Queries ────────────────────────────────────────────────────────────────

(defn- query-id
  "Callers may ask by qualified keyword or by its `\"namespace/name\"` string.
   Bare strings and keywords are accepted as-is so a standalone resource's
   unqualified id stays reachable."
  [document-id]
  (cond
    (keyword? document-id) document-id
    (and (string? document-id) (str/includes? document-id "/"))
    (let [[ns-part name-part] (str/split document-id #"/" 2)]
      (keyword ns-part name-part))
    (string? document-id) (keyword document-id)
    :else document-id))

(defn desired-publications
  "Hydrated intents targeting one document, in canonical order."
  [index document-id]
  (let [wanted (query-id document-id)]
    (->> (:publications index)
         (filter #(= wanted (:publication/document %)))
         (mapv #(law/hydrate-publication-intent index %)))))

(defn document-view
  "One document plus its desired publication topology, validated before it is
   handed out. The CMS facade reads identity at [:document :document/id], so a
   view whose document never received a canonical id must fail here."
  [index document-id]
  (let [wanted (query-id document-id)
        document (get-in index [:documents wanted])]
    (when-not document
      (throw (ex-info "unknown document" {:document/id wanted})))
    (law/assert-valid! wanted
                       law/PublicationDocumentView
                       {:document document
                        :publications (desired-publications index wanted)})))

(defn list-document-views
  "The full desired topology: every document with its intents, plus every
   garden. Not double-wrapped — `:documents` holds document views directly."
  [index]
  (law/assert-valid!
   :publication/list-view
   law/PublicationListView
   {:documents (->> (keys (:documents index))
                    (sort-by pr-str)
                    (mapv #(document-view index %)))
    :gardens (->> (:gardens index) vals (sort-by #(pr-str (:garden/id %))) vec)}))

(defn target-locales
  "Distinct locales any non-archived intent targets, in stable order."
  [index]
  (->> (:publications index)
       (filter active-publication-intent?)
       (map :publication/locale)
       distinct
       (sort-by pr-str)
       vec))

(defn intended-revisions
  "Distinct revision selectors any non-archived intent targets."
  [index]
  (->> (:publications index)
       (filter active-publication-intent?)
       (map :publication/revision)
       distinct
       (sort-by pr-str)
       vec))
