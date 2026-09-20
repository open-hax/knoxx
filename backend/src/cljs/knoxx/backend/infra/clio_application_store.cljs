(ns knoxx.backend.infra.clio-application-store
  "Canonical accepted operations with disposable, validated reference projections.

  No Mongo query language crosses this boundary. A provider supplies its actual
  protocol methods and memory reference state. Only state-changing, successful
  operations enter the ledger; explicit-ID no-op receipts also bind retry answers.
  Every read rebuilds from authoritative facts."
  (:require [clio.extern.js.fs :as fs]
            [clio.extern.js.runtime :as host]
            [clio.infra.event :as event]
            [clio.infra.ledger :as ledger]
            [clio.infra.runtime :as runtime]
            [knoxx.backend.extern.clio-store :as paths]
            [knoxx.backend.law.clio-application-store :as law]))

(defonce ^:private change-listeners (atom {}))

(defn subscribe!
  "Observe successful state changes in this process; returns an unsubscribe function.
   Legacy listeners receive no arguments. Selected listeners receive only a stream
   name, never operation facts. :scope matches provider-declared scope internally;
   a provider without a scope extractor cannot notify a scoped subscription."
  ([listener]
   (law/assert-subscription! {} listener)
   (subscribe! {} (fn [_stream] (listener))))
  ([selection listener]
   (law/assert-subscription! selection listener)
   (let [id (gensym "clio-change-")]
     (swap! change-listeners assoc id (assoc selection :listener listener))
     (fn [] (swap! change-listeners dissoc id) nil))))

(defn- selected-change?
  [{:keys [streams scope]} {:keys [stream change-scope]} operation]
  (and (or (nil? streams) (contains? streams stream))
       (or (nil? scope)
           (when change-scope
             (let [actual (change-scope operation)]
               (when-not (map? actual)
                 (throw (ex-info "Clio change scope must be a map"
                                 {:cause :clio-application/invalid-change-scope})))
               (= scope (select-keys actual (keys scope))))))))

(defn- notify-changed! [store operation]
  (doseq [{:keys [listener] :as selection} (vals @change-listeners)]
    ;; Delivery deliberately cannot turn an accepted durable write into a
    ;; refusal. Scope extraction and sync/async observer failures are contained
    ;; after admission; failed extraction skips this observer and is reported.
    (paths/notify-subscriber! #(when (selected-change? selection store operation)
                                (listener (:stream store))))))

(defn history
  "Read canonical history, refusing missing, malformed or causally invalid facts."
  [{:keys [file runtime]}]
  (:canonical/events
   (ledger/canonicalize-files (:schema/revisions (runtime/refresh runtime))
                              [file])))

(defn- assert-provider-options!
  [{:keys [stream projection reads writes before-append after-append change-scope]}]
  (when-not (and (string? stream) (seq stream) (fn? projection)
                 (map? reads) (map? writes) (or (nil? before-append) (fn? before-append))
                 (or (nil? after-append) (fn? after-append))
                 (or (nil? change-scope) (fn? change-scope))
                 (every? qualified-keyword? (concat (keys reads) (keys writes)))
                 (every? fn? (concat (vals reads) (vals writes))))
    (throw (ex-info "invalid Clio application provider"
                    {:cause :clio-application/invalid-provider}))))

(defn open!
  "Open an isolated ledger with a protocol operation table and reference factory.

  `projection` returns {:store protocol-provider :snapshot (fn [] plain-state)}.
  The snapshot must exclude transient method answers. It is never persisted.
  Optional change-scope synchronously selects a scope map for observer filtering;
  it receives an accepted operation privately and must not perform effects."
  [{:keys [directory stream projection reads writes before-append after-append change-scope] :as options}]
  (assert-provider-options! options)
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
                 :projection projection :reads reads :writes writes
                 :before-append before-append :after-append after-append
                 :change-scope change-scope}]
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
            (when (or (not= result actual)
                      (not= (not= false (:operation/state-changed? operation))
                            (not= before (snapshot))))
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

(defn- ^:async append-operation!
  "Admit one immutable operation at the exact stream slot that was inspected."
  [{:keys [file runtime stream before-append after-append] :as store} events operation]
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
      (when before-append (await (before-append operation)))
      (ledger/append-event! (:schema/revisions (runtime/refresh runtime)) file fact)
      (when (not= false (:operation/state-changed? operation))
        (notify-changed! store operation)
        (when after-append (paths/notify-subscriber! #(after-append operation))))
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

(defn- ^:async write-with-id!
  [store operation-id method args persist-no-op?]
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
             result (await (invoke! (:writes store) provider method args))
             changed? (not= before (snapshot))]
         (when (or changed? persist-no-op?)
           (await (append-operation! store events
                                     (cond-> {:operation/id operation-id
                                              :operation/method method :operation/args args
                                              :operation/result result}
                                       (not changed?) (assoc :operation/state-changed? false)))))
         result))))

(defn ^:async write!
  "Admit a successful invocation before answering.

  An explicit stable id binds arguments and result even for a no-op; its receipt
  replays as unchanged state and emits no state-change callbacks. Reusing that id
  with changed arguments fails. Implicit random-ID no-ops remain unrecorded."
  ([store method args]
   (await (write-with-id! store (host/random-uuid) method args false)))
  ([store operation-id method args]
   (await (write-with-id! store operation-id method args true))))
