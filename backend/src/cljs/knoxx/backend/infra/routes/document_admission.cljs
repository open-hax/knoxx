(ns knoxx.backend.infra.routes.document-admission
  "Resource-aware admission of publication document sources.

  This facade freezes one resource snapshot, preflights every selected source,
  appends retry-safe OpenPlanner events, notifies the local trigger runtime,
  and finally delegates translation work to the existing dispatcher. Fastify,
  HTTP, and publication reconciliation are deliberately outside this seam."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.document-admission :as admission]
            [knoxx.backend.domain.event.dispatch :as event-dispatch]
            [knoxx.backend.domain.node.crypto :as crypto]
            [knoxx.backend.domain.publication-resolver :as resolver]
            [knoxx.backend.infra.agent.runner :as agent-runner]
            [knoxx.backend.infra.clients.openplanner :as openplanner-client]
            [knoxx.backend.infra.publication-contract-content :as contract-content]
            [knoxx.backend.infra.publication-draft-store :as draft-store]
            [knoxx.backend.infra.publication-source-revision :as source-revision]
            [knoxx.backend.infra.routes.publications :as publications]
            [knoxx.backend.infra.routes.translation-dispatch :as translation-dispatch]
            [knoxx.backend.law.publication :as publication-law]))

(defn document-resource-paths
  "Originating resource EDN path per canonical document id.

  The reduction order intentionally matches `document-source-roots`, so an
  equal duplicate resolved by the existing provenance machinery names the
  corresponding resource path here too. Conflicting duplicates have already
  been refused while building the publication index."
  [records]
  (into {}
        (keep (fn [record]
                (when (and (:ok? record)
                           (= :document (:resource/kind record)))
                  (let [document (-> record
                                     publications/single-kind-definition
                                     resolver/canonicalize-document)]
                    [(:document/id document) (:resource/file-path record)]))))
        records))

(defn- ^:async repaired-existing-event!
  [client event-id]
  (openplanner-client/assert-event-projection-repair-supported! client)
  {:ok true
   :count 0
   :ids [event-id]
   :existing true
   :index-result (await (openplanner-client/ensure-event-vectors!
                         client [event-id]))})

(defn- ^:async append-event-with-supported-projections!
  [client event]
  (if (openplanner-client/event-projection-repair-supported? client)
    (let [result (await
                  (openplanner-client/ingest-events-awaiting-projections!
                   client [event]))]
      (assoc result
             :index-result
             (await (openplanner-client/ensure-event-vectors!
                     client [(:id event)]))))
    (await (openplanner-client/events! client [event]))))

(defn ^:async persist-openplanner-event!
  "Append one event once, awaiting detached indexing in embedded Mongo mode.

  OpenPlanner's event collection currently has no unique `id` index. Check the
  durable collection before ingestion so a deployment replay does not append a
  second row with the same content-addressed identity. A concurrent-writer
  unique constraint still belongs in OpenPlanner; this read-before-write closes
  Knoxx's serialized deployment/re-admission path without mutating its schema."
  ([config event]
   (await (persist-openplanner-event!
           config (openplanner-client/client config) event)))
  ([_config client event]
   (let [existing (await
                   (openplanner-client/mongo-query!
                    client {:collection "events"
                            :filter {:id (:id event)}
                            :projection {:id 1}
                            :limit 1}))]
     (if (pos? (or (:total existing) 0))
       (await (repaired-existing-event! client (:id event)))
       (await (append-event-with-supported-projections! client event))))))

(defn- duplicate-event-error?
  [err]
  (let [data (ex-data err)
        native-code (when err (aget err "code"))
        code (or (:code data) native-code)
        message (or (when err (aget err "message")) (str err))]
    (boolean
     (or (= 11000 code)
         (= "11000" (str code))
         (re-find #"(?i)(E11000|duplicate key)" (str message))))))

(defn- ^:async persist-one!
  [persist-event! document-id phase event]
  (try
    (let [result (await (persist-event! event))]
      {:event/id (:id event)
       :event/status (if (:existing result) :existing :recorded)
       :event/result result})
    (catch :default err
      (if (duplicate-event-error? err)
        {:event/id (:id event)
         :event/status :existing}
        (throw
         (ex-info "publication document event persistence failed"
                  {:status 502
                   :code "document_admission_event_failed"
                   :document/id document-id
                   :document/admission-phase phase
                   :event/id (:id event)}
                  err))))))

(defn- ^:async ensure-provenance!
  [canonical-document-path! document source-root resource-path]
  (let [document-id (:document/id document)
        declared-path (get-in document [:document/source :path])]
    (when (str/blank? (str resource-path))
      (throw (ex-info "publication document resource provenance is missing"
                      {:status 409
                       :code "document_resource_provenance_missing"
                       :document/id document-id})))
    (when (nil? source-root)
      (throw (ex-info "publication document source root cannot be resolved from resource provenance"
                      {:status 409
                       :code "document_source_provenance_unresolved"
                       :document/id document-id
                       :document/source-path declared-path
                       :document/resource-path resource-path})))
    {:source-path (await (canonical-document-path! source-root document))
     :resource-path resource-path}))

(defn- assert-source-content!
  [document-id provenance content]
  (cond
    (nil? content)
    (throw (ex-info "publication document source is missing or unreadable"
                    {:status 409
                     :code "document_source_missing"
                     :document/id document-id
                     :document/source-path (:source-path provenance)
                     :document/resource-path (:resource-path provenance)}))

    (str/blank? content)
    (throw (ex-info "publication document source is blank"
                    {:status 409
                     :code "document_source_blank"
                     :document/id document-id
                     :document/source-path (:source-path provenance)
                     :document/resource-path (:resource-path provenance)}))

    :else content))

(defn- runtime-generation-decision
  "Keep immutable requested policy in durable events, but give the local
   trigger the transient decision about whether generation remains necessary."
  [events needs-generation?]
  (assoc-in events
            [:runtime-event :event/payload :document/generate-drafts?]
            needs-generation?))

(defn- generation-policy
  [scope document revision gardens]
  {:source-document-id (:document/id document)
   :source-revision revision
   :source-locale (:document/source-locale document)
   :gardens gardens
   :org-id (:org-id scope)
   :project (:project scope)})

(defn- ^:async read-admission-source!
  [canonical-document-path! read-source! roots resource-paths document]
  (let [document-id (:document/id document)
        source-root (get roots document-id)
        provenance (await (ensure-provenance!
                           canonical-document-path! document source-root
                           (get resource-paths document-id)))
        content (assert-source-content!
                 document-id provenance
                 (await (read-source! source-root document)))]
    {:provenance provenance :content content}))

(defn- ^:async preflight-document!
  [canonical-document-path! read-source! draft-complete-fn digest-hex timestamp
   scope selection index roots
   resource-paths document]
  (let [document-id (:document/id document)
        {:keys [provenance content]}
        (await (read-admission-source!
                canonical-document-path! read-source! roots resource-paths
                document))
        revision (source-revision/content-revision content)
        gardens (admission/document-gardens
                 index document-id
                 #(publication-law/translatable-publication? index %))
        generate-requested? (admission/generate-drafts? selection document)
        draft-policy (generation-policy scope document revision gardens)
        generation-complete? (and generate-requested?
                                  (await (draft-complete-fn draft-policy)))
        needs-generation? (and generate-requested? (not generation-complete?))
        events (admission/admission-events
                digest-hex timestamp scope document provenance content revision
                gardens generate-requested?)]
    (merge {:document document
            :revision revision
            :generate-drafts? generate-requested?
            :draft/policy draft-policy
            :draft/needs-generation? needs-generation?
            :draft/generation-complete? (boolean generation-complete?)}
           (runtime-generation-decision events needs-generation?))))

(defn- ^:async preflight-documents!
  [canonical-document-path! read-source! draft-complete? digest-hex timestamp
   scope selection index roots
   resource-paths documents]
  (loop [pending documents
         prepared []]
    (if-let [document (first pending)]
      (recur (next pending)
             (conj prepared
                   (await (preflight-document!
                           canonical-document-path! read-source! draft-complete?
                           digest-hex timestamp scope selection index roots
                           resource-paths document))))
      prepared)))

(defn- ^:async settle-draft-generation!
  [draft-complete? release-indexed-event! item _settlement]
  (let [event-id (get-in item [:runtime-event :event/id])]
    (when-not (await (draft-complete? (:draft/policy item)))
      (await (release-indexed-event! event-id)))
    true))

(defn- ^:async register-draft-terminal-owner!
  [runtime item]
  (when (:draft/needs-generation? item)
    (let [event-id (get-in item [:runtime-event :event/id])
          registration
          (await
           ((:register-turn-settler! runtime)
            event-id
            (fn [settlement]
              (settle-draft-generation!
               (:draft-complete? runtime) (:release-indexed-event! runtime)
               item settlement))))]
      {:event-id event-id
       :registration registration})))

(defn- unregister-draft-terminal-owner!
  [runtime event-id]
  (when event-id
    ((:unregister-turn-settler! runtime) event-id)))

(defn- current-draft-owner-state
  [runtime event-id]
  ((:draft-event-owner-state runtime) event-id))

(defn- current-indexed-event-state
  [runtime event-id]
  ((:indexed-event-state runtime) event-id))

(defn- ^:async release-stale-completed-draft!
  [runtime item event-id owner-state dispatch-state]
  (when (and (:draft/needs-generation? item)
             (= :completed dispatch-state)
             (nil? owner-state))
    (await ((:release-indexed-event! runtime) event-id))))

(defn- pending-settlement-redelivery?
  [owner-state registration]
  (and (= :settled owner-state)
       (true? (:event-turn/redelivered? registration))
       (false? (:event-turn/redelivery-accepted? registration))))

(defn- live-draft-owner-result
  []
  {:matchedTriggers []
   :skipped true
   :dedup/status :in-flight
   :draft-owner/existing? true})

(defn- ^:async prepare-draft-owner!
  [runtime item]
  (let [event-id (get-in item [:runtime-event :event/id])
        generation? (:draft/needs-generation? item)
        owner-state (when generation?
                      (current-draft-owner-state runtime event-id))
        dispatch-state (when generation?
                         (current-indexed-event-state runtime event-id))]
    (await (release-stale-completed-draft!
            runtime item event-id owner-state dispatch-state))
    (if (= :in-flight owner-state)
      {:result (live-draft-owner-result)}
      (let [{:keys [registration] :as owner}
            (await (register-draft-terminal-owner! runtime item))]
        (if (pending-settlement-redelivery? owner-state registration)
          ;; The prior turn is terminal and no replacement was enqueued. Keep
          ;; its cached settlement for a later retry, but fail this admission;
          ;; calling it in-flight would let deployment pass with no draft owner.
          (throw
           (ex-info "publication post draft settlement could not be reconciled"
                    {:status 503
                     :code "document_post_draft_settlement_redelivery_failed"
                     :document/id (get-in item [:document :document/id])
                     :event/id event-id}))
          owner)))))

(defn- ^:async emit-runtime-indexed-event!
  [runtime item document-id event-id]
  (try
    (await ((:emit-indexed! runtime) (:runtime-event item)))
    (catch :default err
      (unregister-draft-terminal-owner! runtime event-id)
      (throw
       (ex-info "publication document indexed event dispatch failed"
                {:status 500
                 :code "document_indexed_dispatch_failed"
                 :document/id document-id
                 :event/id (get-in item [:indexed-event :id])}
                err)))))

(defn- assert-draft-dispatch!
  [runtime item document-id event-id result]
  (when (and (:draft/needs-generation? item)
             (empty? (:matchedTriggers result))
             (not (and (:skipped result)
                       (= :in-flight (:dedup/status result)))))
    (unregister-draft-terminal-owner! runtime event-id)
    (throw
     (ex-info (if (:skipped result)
                "publication post draft dispatch is stale"
                "publication post draft trigger is not enabled")
              {:status 503
               :code (if (:skipped result)
                       "document_post_draft_dispatch_stale"
                       "document_post_draft_trigger_missing")
               :document/id document-id
               :event/id (get-in item [:indexed-event :id])
               :dedup/status (:dedup/status result)})))
  result)

(defn- ^:async emit-indexed-event!
  [runtime item document-id]
  (let [{:keys [event-id result]} (await (prepare-draft-owner! runtime item))
        result (or result
                   (await (emit-runtime-indexed-event!
                           runtime item document-id event-id)))]
    (assert-draft-dispatch! runtime item document-id event-id result)))

(defn- ^:async persist-prepared!
  [runtime prepared]
  (loop [pending prepared
         persisted []]
    (if-let [item (first pending)]
      (let [document-id (get-in item [:document :document/id])
            source-result (await (persist-one! (:persist-event! runtime) document-id
                                               :source-snapshot
                                               (:snapshot-event item)))
            indexed-result (await (persist-one! (:persist-event! runtime) document-id
                                                :document-indexed
                                                (:indexed-event item)))
            runtime-result (await (emit-indexed-event! runtime item document-id))]
        (recur (next pending)
               (conj persisted
                     (assoc item
                            :source-event-result source-result
                            :indexed-event-result indexed-result
                            :runtime-event-result runtime-result))))
      persisted)))

(defn- translation-failure?
  [result]
  (or (:failed result)
      (contains? #{:dispatch/failed :dispatch/unreachable}
                 (:dispatch/outcome result))
      (some? (:translation/refusal result))
      (= "failed" (:status result))))

(defn- translation-summary
  [results]
  (let [dispatches (mapcat #(or (:dispatched %) []) results)]
    {:documents (count results)
     :considered (reduce + 0 (map #(or (:considered %) 0) results))
     :admissible (reduce + 0 (map #(or (:admissible %) 0) results))
     :dispatched (count dispatches)
     :failed (count (filter translation-failure? dispatches))}))

(defn- response-document
  [item translation]
  (let [failed (count (filter translation-failure?
                              (or (:dispatched translation) [])))]
    {:ok (zero? failed)
     :failed failed
     :document/id (get-in item [:document :document/id])
     :document/source-revision (:revision item)
     :document/source-path (get-in item [:payload :document/source-path])
     :document/resource-path (get-in item [:payload :document/resource-path])
     :document/generate-drafts? (:generate-drafts? item)
     :document/draft-generation-needed? (:draft/needs-generation? item)
     :document/draft-generation-complete? (:draft/generation-complete? item)
     :index/source-event-id (get-in item [:snapshot-event :id])
     :index/source-event-status (get-in item [:source-event-result :event/status])
     :index/event-id (get-in item [:indexed-event :id])
     :index/event-status (get-in item [:indexed-event-result :event/status])
     :translation translation}))

(defn- draft-terminal-dependencies
  [deps]
  {:register-turn-settler!
   (or (:register-turn-settler! deps)
       agent-runner/register-event-turn-settler!)
   :unregister-turn-settler!
   (or (:unregister-turn-settler! deps)
       agent-runner/unregister-event-turn-settler!)
   :release-indexed-event!
   (or (:release-indexed-event! deps)
       event-dispatch/release-exact-event!)
   :indexed-event-state
   (or (:indexed-event-state deps)
       event-dispatch/event-state)
   :draft-event-owner-state
   (or (:draft-event-owner-state deps)
       agent-runner/event-turn-owner-state)})

(defn- runtime-dependencies
  [config deps]
  (let [client (or (:client deps)
                   (openplanner-client/client config))]
    (merge
     {:load-records! (or (:resource-records! deps)
                         publications/resource-records!)
      :build-index (or (:publication-index deps)
                       publications/publication-index)
      :roots-for (or (:document-source-roots deps)
                     translation-dispatch/document-source-roots)
      :paths-for (or (:document-resource-paths deps)
                     document-resource-paths)
      :canonical-document-path!
      (or (:canonical-document-path! deps)
          source-revision/canonical-document-path!)
      :read-source! (or (:source-content! deps)
                        contract-content/source-content!)
      :draft-complete? (or (:draft-complete? deps)
                           (fn [policy]
                             (draft-store/draft-complete? config policy)))
      :digest-hex (or (:digest-hex deps) crypto/sha256-hex)
      :clock (or (:clock deps) (fn [] (.toISOString (js/Date.))))
      :persist-event! (or (:persist-event! deps)
                          (partial persist-openplanner-event! config client))
      :emit-indexed! (or (:emit-indexed! deps)
                         (fn [event] (event-dispatch/dispatch! config event)))
      :repair-translation-events! (:repair-translation-events! deps)
      :dispatch-document! (:dispatch-document! deps)}
     (draft-terminal-dependencies deps))))

(defn- ^:async load-admission-snapshot!
  [config runtime scope selection]
  (let [records (await ((:load-records! runtime) config))
        index ((:build-index runtime) records)
        documents (admission/select-documents index selection)
        roots ((:roots-for runtime) config records)
        resource-paths ((:paths-for runtime) records)
        prepared (await (preflight-documents!
                         (:canonical-document-path! runtime)
                         (:read-source! runtime) (:draft-complete? runtime)
                         (:digest-hex runtime)
                         ((:clock runtime)) scope selection index roots
                         resource-paths documents))]
    {:records records
     :index index
     :documents documents
     :prepared prepared}))

(defn- pinned-dispatch-dependencies
  [records index persisted]
  (let [revisions (into {}
                        (map (fn [item]
                               [(get-in item [:document :document/id])
                                (:revision item)]))
                        persisted)]
    {:resource-records! (fn [_] (js/Promise.resolve records))
     :publication-index (fn [_] index)
     :source-revisions! (fn [_ _ _] (js/Promise.resolve revisions))}))

(defn- ^:async dispatch-admitted!
  [dispatch-document! snapshot-deps persisted]
  (loop [pending persisted
         results []]
    (if-let [item (first pending)]
      (recur (next pending)
             (conj results
                   (await (dispatch-document!
                           (get-in item [:document :document/id])
                           snapshot-deps))))
      results)))

(defn- admission-response
  [documents persisted translations]
  (let [summary (translation-summary translations)
        results (mapv response-document persisted translations)
        failed (:failed summary)]
    {:ok (zero? failed)
     :selected (count documents)
     :admitted (count persisted)
     :failed failed
     :indexed-events (* 2 (count persisted))
     :results results
     ;; Compatibility for callers already reading the more explicit name.
     :documents results
     :translations summary}))

(defonce ^:private document-admission-tail* (atom nil))

(defn- ^:async run-after-document-admission!
  [previous task-fn]
  (when previous
    (try
      (await previous)
      ;; knoxx-lint/allow-silent-catch — an earlier admission must not poison the queue.
      (catch :default _
        nil)))
  (await (task-fn)))

(defn- ^:async recover-document-admission-tail!
  [task]
  (try
    (await task)
    ;; knoxx-lint/allow-silent-catch — only the stored recovery tail consumes this rejection.
    (catch :default _
      nil)))

(defn- enqueue-document-admission!
  "Serialize one complete admission pass behind the process-wide tail."
  [task-fn]
  ;; No await occurs between reading and replacing the tail, so the single JS
  ;; event loop gives every caller an exact predecessor. The recovery promise
  ;; prevents one failed admission from poisoning every later deployment.
  (let [task (run-after-document-admission!
              @document-admission-tail* task-fn)]
    (reset! document-admission-tail*
            (recover-document-admission-tail! task))
    task))

(defn- ^:async admit-documents-once!
  "Run one admission pass while the process-wide event writer is owned."
  [config deps scope selection]
  (let [selection (admission/normalize-selection selection)
        runtime (runtime-dependencies config deps)
        dispatch-document! (:dispatch-document! runtime)]
    ;; Preflight the whole selection before the first durable write. One broken
    ;; anchor therefore fails the deployment without a misleading partial
    ;; admission of later documents.
    (when-not dispatch-document!
      (throw (ex-info "document admission translation dispatcher is not configured"
                      {:status 503
                       :code "document_admission_dispatch_unavailable"})))
    (let [{:keys [records index documents prepared]}
          (await (load-admission-snapshot! config runtime scope selection))
          ;; A completed receipt is the semantic fact; its searchable event and
          ;; vector are recoverable projections. Repair the tenant once per
          ;; admission pass, after every selected source has passed preflight
          ;; and before appending this pass's first durable event.
          _ (when-let [repair! (:repair-translation-events! runtime)]
              (await (repair! scope)))
          persisted (await (persist-prepared! runtime prepared))
          snapshot-deps (pinned-dispatch-dependencies records index persisted)
          translations (await (dispatch-admitted! dispatch-document!
                                                  snapshot-deps persisted))]
      (admission-response documents persisted translations))))

(defn ^:async admit-documents!
  "Serialize admission of selected documents and dispatch their translations.

  `deps` is registration-scoped composition, never global mutable state. The
  important internal hook is `:dispatch-document!`, called as
  `(dispatch-document! document-id snapshot-deps)`. Snapshot deps pin the exact
  records, publication index, and content revisions loaded by this admission.
  This makes the function reusable by a generated-document tool without an HTTP
  loopback or a tools/dispatch require cycle.

  OpenPlanner's base event collection does not yet enforce unique `id` values,
  so every pass shares one process-wide tail across HTTP, resource writes, and
  generated-document re-entry. Trigger dispatch accepts an agent turn before
  that turn can save its generated draft, allowing the outer admission to
  release this tail before the generated document re-enters it."
  [config deps scope selection]
  (await
   (enqueue-document-admission!
    (fn []
      (admit-documents-once! config deps scope selection)))))

(defn admission-hook
  "Close over app-owned config/dependencies for low-level internal admission."
  [config deps]
  (fn [scope selection]
    (admit-documents! config deps scope selection)))

(defn admit-document!
  "Compact exact-document entry point for internal generated-resource callers."
  ([config deps scope document-id]
   (admit-documents! config deps scope {:document document-id}))
  ([config deps scope document-id generate-drafts?]
   (admit-documents! config deps scope {:document document-id
                                        :generate-drafts? generate-drafts?})))
