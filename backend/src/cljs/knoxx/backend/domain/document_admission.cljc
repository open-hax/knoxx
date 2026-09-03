(ns knoxx.backend.domain.document-admission
  "Pure selection and event projection for publication document admission.

  Admission has two durable representations. A `docs` event is the searchable
  source snapshot, while a `publication.document.indexed` event is the stable
  fact that wakes contract-native triggers. Both are derived from the same
  immutable payload and neither accepts policy coordinates from HTTP."
  (:require [clojure.string :as str]))

(def indexed-event-type :publication/document-indexed)
(def indexed-event-kind "publication.document.indexed")
(def snapshot-event-kind "docs")

(defn wire-id
  "Encode a resource keyword without dropping its namespace."
  [value]
  (if (keyword? value)
    (if-let [ns-part (namespace value)]
      (str ns-part "/" (name value))
      (name value))
    (str value)))

(def ^:private selection-keys
  #{:document :anchors? :generate-drafts?})

(defn- selection-map
  [selection]
  (cond
    (nil? selection) {}
    (qualified-keyword? selection) {:document selection}
    (map? selection) selection
    :else (throw (ex-info "invalid document admission selection"
                          {:status 400
                           :code "document_admission_selection_invalid"
                           :selection selection}))))

(defn- valid-selection-shape?
  [selection]
  (and (every? selection-keys (keys selection))
       (or (nil? (:document selection))
           (qualified-keyword? (:document selection)))
       (or (not (contains? selection :anchors?))
           (boolean? (:anchors? selection)))
       (or (not (contains? selection :generate-drafts?))
           (boolean? (:generate-drafts? selection)))))

(defn- assert-selector!
  [selection document anchors?]
  (when (and document anchors?)
    (throw (ex-info "document admission selects anchors or one document, not both"
                    {:status 400
                     :code "document_admission_selector_conflict"
                     :selection selection})))
  (when (and (nil? document) (false? anchors?))
    (throw (ex-info "document admission must select anchors or one document"
                    {:status 400
                     :code "document_admission_selector_required"
                     :selection selection})))
  selection)

(defn normalize-selection
  "Normalize the reusable admission selector.

  Absence means the anchor sweep. A qualified keyword is the compact exact
  document form used by internal callers. `:generate-drafts?` is only an
  operator override; document policy remains the default."
  [value]
  (let [selection (selection-map value)
        document (:document selection)
        anchors? (if (contains? selection :anchors?)
                   (:anchors? selection)
                   (nil? document))]
    (when-not (valid-selection-shape? selection)
      (throw (ex-info "invalid document admission selection"
                      {:status 400
                       :code "document_admission_selection_invalid"
                       :selection selection})))
    (assert-selector! selection document anchors?)
    (assoc selection :anchors? (if document false anchors?))))

(defn document-visible-to-org?
  "True only for an explicit public document or an exact request-org owner."
  [scope document]
  (or (= :public (:document/visibility document))
      (and (contains? document :document/org-id)
           (= (:org-id scope) (:document/org-id document)))))

(defn visible-publication-index
  "Restrict one resolved publication index to documents visible to `scope`.

  Publications are filtered with their owning document. Keeping a relation
  whose document was removed would either disclose its identity or make later
  hydration fail on a deliberately hidden reference. Gardens are shared
  topology and carry no document content, so they remain intact."
  [index scope]
  (let [documents (into {}
                        (filter (fn [[_ document]]
                                  (document-visible-to-org? scope document)))
                        (:documents index))
        document-ids (set (keys documents))]
    (-> index
        (assoc :documents documents)
        (update :publications
                (fn [publications]
                  (filterv #(contains? document-ids
                                       (:publication/document %))
                           publications))))))

(defn select-documents
  "Select documents visible to the request organization, stably.

  A document is admissible only when its declared owner equals the request
  organization or it is explicitly public. Missing ownership never means
  public: legacy private/generated resources therefore fail closed. Exact
  denials use the same 404 as absence so this boundary does not disclose an
  out-of-scope document's existence."
  [index scope selection]
  (let [{:keys [document]} (normalize-selection selection)]
    (if document
      (let [selected (get-in index [:documents document])]
        (if (and selected (document-visible-to-org? scope selected))
          [selected]
          (throw (ex-info "document admission selection was not found"
                          {:status 404
                           :code "document_admission_document_not_found"
                           :document/id document}))))
      (->> (:documents index)
           vals
           (filter #(and (true? (:document/anchor? %))
                         (document-visible-to-org? scope %)))
           (sort-by #(pr-str (:document/id %)))
           vec))))

(defn generate-drafts?
  "Resolve the trusted draft policy for one selected document.

   Derived documents are terminal inputs to this derivation step. An operator
   override may request drafts for authored anchors, but must not turn a
   generated post into the source of another generated post on every sweep."
  [selection document]
  (and (nil? (:document/derived-from document))
       (boolean
        (if (contains? selection :generate-drafts?)
          (:generate-drafts? selection)
          (:document/generate-drafts? document)))))

(defn document-gardens
  "Stable garden/target-locale coordinates for eligible document relations.

  The caller supplies the publication law predicate because eligibility needs
  the whole index, while this namespace remains a portable pure projection."
  [index document-id eligible?]
  (->> (:publications index)
       (filter #(and (= document-id (:publication/document %))
                     (eligible? %)))
       (group-by :publication/garden)
       (map (fn [[garden-id intents]]
              {:garden/id garden-id
               :garden/locales (->> intents
                                    (map :publication/locale)
                                    distinct
                                    (sort-by pr-str)
                                    vec)}))
       (sort-by #(pr-str (:garden/id %)))
       vec))

(defn- snapshot-binding
  [payload]
  [1
   (:org/id payload)
   (:membership/id payload)
   (:project payload)
   (:document/id payload)
   (:document/title payload)
   (:document/source-locale payload)
   (:document/source-revision payload)
   (:document/source-path payload)
   (:document/resource-path payload)
   (:document/anchor? payload)
   (:document/generate-drafts? payload)
   (:document/gardens payload)])

(defn- event-id
  [prefix digest-hex payload]
  (str prefix (digest-hex (pr-str (snapshot-binding payload)))))

(defn- wire-gardens
  [gardens]
  (mapv (fn [garden]
          {:garden_id (wire-id (:garden/id garden))
           :locales (mapv name (:garden/locales garden))})
        gardens))

(defn- wire-resource-policies
  [payload]
  (let [policies (:resource-policies payload)]
    {:publication_draft true
     :source_document_id (wire-id (:source-document-id policies))
     :source_revision (:source-revision policies)
     :source_locale (name (:source-locale policies))
     :gardens (wire-gardens (:gardens policies))
     :org_id (:org-id policies)
     :membership_id (:membership-id policies)
     :project (:project policies)}))

(defn- durable-extra
  [payload snapshot-event-id]
  {:tenant_id (:org/id payload)
   :org_id (:org/id payload)
   :membership_id (:membership/id payload)
   :project (:project payload)
   :document_id (wire-id (:document/id payload))
   :title (:document/title payload)
   :language (name (:document/source-locale payload))
   :source_locale (name (:document/source-locale payload))
   :source_revision (:document/source-revision payload)
   :content (:document/source-content payload)
   :source_path (:document/source-path payload)
   :resource_path (:document/resource-path payload)
   :anchor (boolean (:document/anchor? payload))
   :generate_drafts (boolean (:document/generate-drafts? payload))
   :gardens (wire-gardens (:document/gardens payload))
   :garden_ids (mapv (comp wire-id :garden/id) (:document/gardens payload))
   :target_locales (->> (:document/gardens payload)
                        (mapcat :garden/locales)
                        distinct
                        (sort-by pr-str)
                        (mapv name))
   :visibility "internal"
   :source_event_id snapshot-event-id
   :resource_policies (wire-resource-policies payload)})

(defn- resource-policies
  [scope document revision gardens]
  {:publication-draft? true
   :source-document-id (:document/id document)
   :source-revision revision
   :source-locale (:document/source-locale document)
   :gardens gardens
   :org-id (:org-id scope)
   :membership-id (:membership-id scope)
   :project (:project scope)})

(defn- event-payload
  [scope document provenance content revision gardens generate-drafts?]
  {:document/id (:document/id document)
   :document/title (:document/title document)
   :document/source-locale (:document/source-locale document)
   :document/source-revision revision
   :document/source-content content
   :document/source-path (:source-path provenance)
   :document/resource-path (:resource-path provenance)
   :document/anchor? (boolean (:document/anchor? document))
   :document/generate-drafts? (boolean generate-drafts?)
   :document/gardens gardens
   :document/garden-ids (mapv :garden/id gardens)
   :document/target-locales (->> gardens (mapcat :garden/locales) distinct
                                 (sort-by pr-str) vec)
   :org/id (:org-id scope)
   :membership/id (:membership-id scope)
   :project (:project scope)
   ;; `start-agent-session/render-start-message` consumes this compatibility
   ;; key. The qualified key remains the semantic source coordinate.
   :content content
   :resource-policies (resource-policies scope document revision gardens)})

(defn- source-ref
  [scope document]
  (cond-> {:message (wire-id (:document/id document))}
    (some-> (:project scope) str str/trim not-empty)
    (assoc :project (:project scope))))

(defn- openplanner-event
  [id timestamp kind source-ref text tags extra]
  {:schema "openplanner.event.v1"
   :id id
   :ts timestamp
   :source "knoxx-publication"
   :kind kind
   :source_ref source-ref
   :text text
   :meta {:role "system" :author "knoxx" :tags tags}
   :extra extra})

(defn- local-indexed-event
  [indexed-id timestamp payload snapshot-id]
  {:event/id indexed-id
   :event/type indexed-event-type
   :event/types [indexed-event-type]
   :event/actor "knoxx-publication"
   :event/generator {:kind :document-admission}
   :event/timestamp timestamp
   :event/payload (assoc payload :index/event-id indexed-id
                                :index/source-event-id snapshot-id)})

(defn admission-events
  "Build the searchable snapshot, durable signal, and local trigger event.

  Event ids omit wall-clock time and address the complete trusted snapshot.
  An unchanged retry therefore reuses both ids; changing content, placement,
  provenance, scope, or draft policy creates a new immutable fact."
  [digest-hex timestamp scope document provenance content revision gardens
   generate-drafts?]
  (let [payload (event-payload scope document provenance content revision
                               gardens generate-drafts?)
        snapshot-id (event-id "knoxx-document-snapshot-" digest-hex payload)
        indexed-id (event-id "knoxx-publication-document-indexed-" digest-hex payload)
        extra (durable-extra payload snapshot-id)
        ref (source-ref scope document)
        indexed-text (str "Indexed publication document "
                          (wire-id (:document/id document)) " at " revision)]
    {:payload (get-in (local-indexed-event indexed-id timestamp payload snapshot-id)
                      [:event/payload])
     :snapshot-event (openplanner-event
                      snapshot-id timestamp snapshot-event-kind ref content
                      ["knoxx" "publication" "document" "source-snapshot"] extra)
     :indexed-event (openplanner-event
                     indexed-id timestamp indexed-event-kind ref indexed-text
                     ["knoxx" "publication" "document" "indexed"] extra)
     :runtime-event (local-indexed-event indexed-id timestamp payload snapshot-id)}))
