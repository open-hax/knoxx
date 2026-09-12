(ns knoxx.backend.infra.clio-application-store
  "Canonical accepted operations with disposable, validated reference projections.

  No Mongo query language crosses this boundary. A provider supplies its actual
  protocol methods and memory reference state. Only state-changing, successful
  operations enter the ledger. Every read rebuilds from authoritative facts."
  (:require [clio.extern.js.fs :as fs]
            [clio.extern.js.runtime :as host]
            [clio.infra.event :as event]
            [clio.infra.ledger :as ledger]
            [clio.infra.runtime :as runtime]
            [knoxx.backend.extern.clio-store :as paths]
            [knoxx.backend.law.clio-application-store :as law]))

(defn history
  "Read canonical history, refusing missing, malformed or causally invalid facts."
  [{:keys [file runtime]}]
  (:canonical/events
   (ledger/canonicalize-files (:schema/revisions (runtime/refresh runtime))
                              [file])))

(defn open!
  "Open an isolated ledger with a protocol operation table and reference factory.

  `projection` returns {:store protocol-provider :snapshot (fn [] plain-state)}.
  The snapshot must exclude transient method answers. It is never persisted."
  [{:keys [directory stream projection reads writes before-append]}]
  (when-not (and (string? stream) (seq stream) (fn? projection)
                 (map? reads) (map? writes) (or (nil? before-append) (fn? before-append))
                 (every? qualified-keyword? (concat (keys reads) (keys writes)))
                 (every? fn? (concat (vals reads) (vals writes))))
    (throw (ex-info "invalid Clio application provider"
                    {:cause :clio-application/invalid-provider})))
  (let [directory (paths/resolve-directory directory)
        file (str directory "/events.edn")
        schemas (str directory "/schemas")
        known? (fs/exists? schemas)]
    (fs/ensure-dir! directory)
    (when-not (fs/exists? file)
      (when known?
        (throw (ex-info "Clio application ledger is missing beside known schemas"
                        {:cause :clio-application/missing-ledger :file file})))
      (ledger/create-ledger! file))
    (let [store {:directory directory :file file :stream stream
                 :runtime (runtime/open schemas law/catalog)
                 :projection projection :reads reads :writes writes :before-append before-append}]
      (history store)
      store)))

(defn- ^:async invoke!
  "Invoke exactly one declared method on a disposable reference projection."
  [operations provider method args]
  (law/assert-invocation! operations method args)
  (await (apply (get operations method) provider args)))

(defn- ^:async replay!
  "Replay each accepted operation, refusing answer or state-transition drift."
  [{:keys [projection writes stream]} events]
  (let [{:keys [store snapshot] :as view} (projection)]
    (loop [remaining events seen #{}]
      (if-let [fact (first remaining)]
        (let [{:operation/keys [id method args result] :as operation}
              (law/assert-operation! (:event/data fact))
              before (snapshot)]
          (when (or (not= stream (:event/stream fact)) (contains? seen id))
            (throw (ex-info "Clio application history has a conflicting identity"
                            {:cause :clio-application/history-conflict
                             :operation/id id})))
          (let [actual (await (invoke! writes store method args))]
            (when (or (not= result actual) (= before (snapshot)))
              (throw (ex-info "Clio operation disagrees with its reference semantics"
                              {:cause :clio-application/replay-conflict
                               :operation operation}))))
          (recur (rest remaining) (conj seen id)))
        view))))

(defn ^:async read!
  "Read a declared protocol method against newly replayed canonical facts."
  [store method args]
  (let [view (await (replay! store (history store)))]
    (await (invoke! (:reads store) (:store view) method args))))

(defn- append-operation!
  "Admit one immutable operation at the exact stream slot that was inspected."
  [{:keys [file runtime stream before-append]} events operation]
  (let [previous (last events)
        fact (event/make-event
              (:schema/current runtime) :knoxx.application/operation-accepted
              {:event/stream stream
               :event/seq (inc (or (:event/seq previous) 0))
               :event/causes (if previous [(:event/id previous)] [])
               :event/actor "knoxx/application-store"
               :event/subject stream
               :event/data (law/assert-operation! operation)})]
    ;; The decision is made on a disposable projection. Clio's inode lock and
    ;; stream-slot admission reject another writer winning after that decision;
    ;; a failed append never publishes the staged state or reports success.
    (try
      ;; Optional live admissibility guard runs only for a new append. It never
      ;; rewrites accepted arguments/results and never runs during replay.
      (when before-append (before-append operation))
      (ledger/append-event! (:schema/revisions (runtime/refresh runtime)) file fact)
      (catch :default cause
        (if (= :clio.ledger/concurrent-stream-write (:clio/error (ex-data cause)))
          (throw (ex-info "Clio application state changed; retry against fresh history"
                          (assoc (ex-data cause) :status 409
                                 :code "clio_application_stale_head")
                          cause))
          (throw cause))))))

(defn- existing-result [existing operation-id method args]
  (if (= [method args] [(:operation/method existing) (:operation/args existing)])
    (:operation/result existing)
    (throw (ex-info "Clio operation id is already bound to different arguments"
                    {:cause :clio-application/operation-conflict
                     :status 409 :code "clio_application_operation_conflict"
                     :operation/id operation-id}))))

(defn ^:async write!
  "Validate then durably append a state-changing invocation before answering.

  An optional caller-stable id supports lost-response retries. Reusing it with
  changed method/arguments fails. Protocol-level retries still run their own
  first-fact semantics and append nothing when reference state is unchanged."
  ([store method args]
   (await (write! store (host/random-uuid) method args)))
  ([store operation-id method args]
   (law/assert-invocation! (:writes store) method args)
   (law/assert-operation! {:operation/id operation-id :operation/method method
                          :operation/args args :operation/result nil})
   (let [events (history store)
         {:keys [snapshot] provider :store} (await (replay! store events))
         existing (some #(when (= operation-id (get-in % [:event/data :operation/id]))
                           (:event/data %)) events)]
     (if existing
       (existing-result existing operation-id method args)
       (let [before (snapshot)
             result (await (invoke! (:writes store) provider method args))]
         (when (not= before (snapshot))
           (append-operation! store events
                              {:operation/id operation-id
                               :operation/method method :operation/args args
                               :operation/result result}))
         result)))))
