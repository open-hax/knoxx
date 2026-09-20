(ns knoxx.backend.domain.local-openplanner-events
  "Immutable event admission, scoped session views and exact vector projections."
  (:require [cljs.math :as math]
            [knoxx.backend.domain.node.crypto :as crypto]
            [knoxx.backend.law.local-openplanner :as law]))

(defn event-org "Organization owned by an event." [event] (get-in event [:extra :org_id]))
(defn event-project "Explicit event project, if any." [event]
  (or (get-in event [:source_ref :project]) (get-in event [:extra :project])))
(defn event-session "Canonical conversation reference." [event]
  (or (get-in event [:source_ref :session]) (get-in event [:source_ref :session_id])
      (get-in event [:extra :session_id]) (get-in event [:extra :conversation_id])))
(defn content-digest "Digest binding a projection to immutable text." [event]
  (crypto/sha256-hex (:text event)))

(defn scoped? "Exact tenant plus optional explicit project filter." [opts event]
  (and (= (:org_id opts) (event-org event))
       (or (nil? (:project opts)) (= (:project opts) (event-project event)))))

(defn admit-events
  "Bind each id to one immutable event. Equal retries append no new facts."
  [state events]
  (law/assert-valid! [:vector law/Event] events)
  (let [next-state
        (reduce (fn [acc event]
                  (if-let [existing (get-in acc [:events (:id event)])]
                    (if (= existing event) acc
                        (throw (ex-info "OpenPlanner event id is already bound"
                                        {:status 409 :code "openplanner_event_conflict" :id (:id event)})))
                    (-> acc (assoc-in [:events (:id event)] event)
                        (update :event-order (fnil conj []) (:id event))))) state events)]
    [next-state {:ok true :count (- (count (:events next-state)) (count (:events state)))
                 :ids (mapv :id events)}]))

(defn repair-extra
  "Add missing derived query metadata without changing canonical event bytes."
  [state id required]
  (law/assert-valid! [:map-of keyword? any?] required)
  (let [event (get-in state [:events id])
        actual (merge (:extra event) (get-in state [:event-extra id]))]
    (when-not event (throw (ex-info "OpenPlanner event not found" {:status 404})))
    (doseq [[key value] required]
      (when (and (contains? actual key) (not= value (get actual key)))
        (throw (ex-info "Derived metadata disagrees with event authority"
                        {:status 409 :code "openplanner_event_extra_conflict" :id id :key key}))))
    [(assoc-in state [:event-extra id] (merge (get-in state [:event-extra id]) required))
     {:ok true :event-id id :required required}]))

(defn project-vectors
  "Admit real vectors only for the exact existing event text."
  [state projections]
  [(reduce (fn [acc row]
             (law/projection! row)
             (let [event (get-in acc [:events (:id row)])]
               (when-not (and event (= (:digest row) (content-digest event)))
                 (throw (ex-info "OpenPlanner vector is detached from its event"
                                 {:status 409 :code "openplanner_vector_digest_conflict"})))
               (assoc-in acc [:vectors (:id row)] row))) state projections)
   {:ok true :event-ids (mapv :id projections) :vector-count (count projections)}])

(defn rows
  "Read ordered event rows after validating scope even for an empty ledger."
  [state opts]
  (law/scope! opts)
  (->> (:event-order state)
       (map #(get-in state [:events %]))
       (filter #(scoped? opts %))
       (mapv #(assoc % :role (or (:role %) (get-in % [:meta :role]))
                       :extra (merge (:extra %) (get-in state [:event-extra (:id %)]))))))

(defn session
  "Read only the caller's explicitly scoped conversation events."
  [state session-id opts]
  (law/assert-valid! law/NonBlank session-id)
  {:session_id session-id
   :rows (filterv #(= session-id (event-session %)) (rows state opts))})

(defn sessions
  "Build inspectable conversation summaries from real event history."
  [state opts]
  (let [groups (group-by event-session (filter #(some? (event-session %)) (rows state opts)))
        summaries (->> groups
                       (map (fn [[id events]]
                              {:session_id id :session id :id id :org_id (:org_id opts)
                               :project (event-project (first events))
                               :count (count events) :event_count (count events)
                               :first_ts (:ts (first events)) :last_ts (:ts (last events))
                               :title (or (get-in (last events) [:extra :session_title]) id)}))
                       (sort-by (juxt :last_ts :session_id)) reverse vec)
        limit (min 500 (max 1 (or (:limit opts) 50)))
        offset (max 0 (or (:offset opts) 0))]
    {:sessions (vec (take limit (drop offset summaries)))
     :rows (vec (take limit (drop offset summaries))) :total (count summaries)
     :has_more (< (+ offset limit) (count summaries))}))

(defn- cosine-distance [a b]
  (let [dot (reduce + (map * a b))
        norm-a (math/sqrt (reduce + (map #(* % %) a)))
        norm-b (math/sqrt (reduce + (map #(* % %) b)))]
    (if (or (zero? norm-a) (zero? norm-b)) 1
        (- 1 (max -1 (min 1 (/ dot (* norm-a norm-b))))))))

(defn vector-search
  "Search exact tenant/project rows with verified vectors from one model."
  [state opts query-vector model dimensions]
  (law/scope! opts)
  (law/projection! {:id "query" :digest "query" :model model :dimensions dimensions :embedding query-vector})
  (let [candidates (filter (fn [event]
                             (and (or (nil? (:kind opts)) (= (:kind opts) (:kind event)))
                                  (or (nil? (:source opts)) (= (:source opts) (:source event)))
                                  (or (nil? (:session opts)) (= (:session opts) (event-session event)))))
                           (rows state opts))
        hits (->> candidates
                  (keep (fn [event]
                          (when-let [row (get-in state [:vectors (:id event)])]
                            (law/projection! row)
                            (when (and (= model (:model row)) (= dimensions (:dimensions row))
                                       (= (content-digest event) (:digest row)))
                              {:id (:id event) :document (:text event)
                               :metadata (merge (:extra event) (:source_ref event)
                                                {:kind (:kind event) :source (:source event)})
                               :distance (cosine-distance query-vector (:embedding row))}))))
                  (sort-by (juxt :distance :id)) (take (min 100 (max 1 (or (:k opts) 10)))) vec)]
    {:ids [(mapv :id hits)] :documents [(mapv :document hits)]
     :metadatas [(mapv :metadata hits)] :distances [(mapv :distance hits)]
     :hits hits}))
