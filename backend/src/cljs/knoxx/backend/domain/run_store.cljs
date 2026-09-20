(ns knoxx.backend.domain.run-store
  "Pure run and ordered event transitions. Expiry hides views, never accepted facts."
  (:require [knoxx.backend.law.run-store :as law]
            [knoxx.backend.shape.session-persistence :as contract]))

(def empty-state {:runs {} :events {} :bindings {}})
(def identity-fields [:run_id :session_id :conversation_id :org_id :user_id])
(def active-statuses #{"queued" "running" "waiting_input"})

(defn visible-run
  "Return only a currently visible run at one explicit clock sample."
  [state run-id at-ms]
  (law/require! law/NonBlank run-id)
  (law/require! law/Milliseconds at-ms)
  (let [entry (get-in state [:runs run-id])]
    (when (and entry (< at-ms (:expires-ms entry))) (:run entry))))

(defn active-runs
  "Read active runs in deterministic identity order."
  [state session-id at-ms]
  (law/require! law/NonBlank session-id)
  (->> (:runs state) keys sort (keep #(visible-run state % at-ms))
       (filter #(and (= session-id (:session_id %)) (active-statuses (:status %)))) vec))

(defn events-since
  "Read a strict ordered cursor; expiry never falls back to cached events."
  [state run-id since at-ms]
  (law/require! [:or :nil [:int {:min 0}] law/Instant] since)
  (if (visible-run state run-id at-ms)
    (filterv #(cond (nil? since) true (number? since) (> (:sequence %) since)
                    :else (pos? (compare (:at %) since))) (get-in state [:events run-id] []))
    []))

(defn- assert-identity! [state run-id run]
  (when-not (= run-id (:run_id run)) (law/conflict! "A run cannot be renamed"))
  (when-let [binding (get-in state [:bindings run-id])]
    (when-not (= binding (select-keys run identity-fields))
      (law/conflict! "Run owner, tenant and conversation coordinates are immutable"))))

(defn- install [state run-id run stamp]
  (contract/assert-run! run "ClioRunStore")
  (when (some #(contains? run %) [:events :run_events :sequence])
    (law/conflict! "Ordered events require their dedicated admission method"))
  (assert-identity! state run-id run)
  (let [stored (assoc run :system_instance_id (:instance-id stamp))]
    [(-> state (assoc-in [:runs run-id] {:run stored :expires-ms (:expires-ms stamp)})
         (assoc-in [:bindings run-id] (select-keys run identity-fields))) stored]))

(defn- current! [state run-id stamp]
  (or (visible-run state run-id (:at-ms stamp))
      (throw (ex-info "Run is absent or expired" {:status 404 :code "run_store_not_found"}))))

(defn- append-event [state {:keys [run-id event event-id stamp]}]
  (let [run (current! state run-id stamp)
        events (get-in state [:events run-id] [])
        existing (some #(when (= event-id (:event_id %)) %) events)]
    (law/require! law/Event event)
    (law/require! law/NonBlank event-id)
    (when-not (= (select-keys run [:run_id :session_id :conversation_id])
                 (select-keys event [:run_id :session_id :conversation_id]))
      (law/conflict! "Run event has a different conversation or session"))
    (when (or (contains? event :sequence) (contains? event :run_events))
      (law/conflict! "Event sequence is assigned by the durable provider"))
    (if existing
      (if (= (dissoc existing :sequence) (assoc event :event_id event-id)) [state existing]
        (law/conflict! "Run event ID is already bound to another payload"))
      (let [accepted (assoc event :event_id event-id :sequence (inc (count events)))]
        [(-> state (update-in [:events run-id] (fnil conj []) accepted)
             (assoc-in [:runs run-id :expires-ms] (:expires-ms stamp))) accepted]))))

(defn transition
  "Admit one finite stamped operation with no host effects or implicit upserts."
  [state {:keys [kind run-id stamp run patch] :as operation}]
  (law/require! law/Operation operation)
  (when-not (= law/ttl-ms (- (:expires-ms stamp) (:at-ms stamp)))
    (law/conflict! "Run expiry differs from its declared TTL"))
  (case kind
    :put (install state run-id run stamp)
    :patch (do (when (some #(contains? patch %) [:events :run_events :sequence])
                 (law/conflict! "Ordered events require their dedicated admission method"))
               (install state run-id (merge (current! state run-id stamp) patch {:updated_at (:at stamp)}) stamp))
    :event (append-event state operation)
    :delete [(update state :runs dissoc run-id) true]))
