(ns knoxx.backend.shape.memory-sessions
  "Pure shape helpers for memory session list route inputs and responses."
  (:require [clojure.string :as str]
            [knoxx.backend.shape.parse :refer [parse-positive-int]]))

(def max-session-list-page-size 80)
(def max-session-list-upstream-page-size 50)

(defn session-list-limit
  [value]
  (min max-session-list-page-size
       (max 1 (or (parse-positive-int value) 12))))

(defn session-list-upstream-page-size
  [limit offset]
  (min max-session-list-upstream-page-size
       (max 10 (+ (max 0 offset) (max 1 limit) 1))))

(defn normalized-actor-id
  [value]
  (some-> value str str/trim not-empty))

(defn normalized-actor-ids
  [value]
  (let [items (cond
                (nil? value) []
                (string? value) (str/split value #",")
                (array? value) (js->clj value)
                (sequential? value) value
                :else [value])]
    (->> items
         (keep normalized-actor-id)
         distinct
         vec)))

(defn query-options
  [query]
  (let [limit (session-list-limit (aget query "limit"))
        actor-id (some-> (or (aget query "actorId") (aget query "actor"))
                         normalized-actor-id)
        exclude-actor-ids (normalized-actor-ids
                           (or (aget query "excludeActorIds")
                               (aget query "excludeActorId")
                               (aget query "excludeActors")))
        contract-id (some-> (or (aget query "contractId")
                                (aget query "contract_id")
                                (aget query "contract"))
                           normalized-actor-id)
        offset-raw (aget query "offset")
        offset-parsed (js/parseInt (str (or offset-raw "0")) 10)
        offset (if (and (js/Number.isFinite offset-parsed) (>= offset-parsed 0)) offset-parsed 0)]
    {:limit limit
     :offset offset
     :actor-id actor-id
     :exclude-actor-ids exclude-actor-ids
     :contract-id contract-id
     :upstream-page-size (session-list-upstream-page-size limit offset)
     :needed-count (+ offset (max 1 limit) 1)}))

(defn page-state
  [value {:keys [offset limit actor-id exclude-actor-ids contract-id]} cache]
  (let [{:keys [rows has_more]} value
        all-rows (vec rows)
        visible-total (count all-rows)
        page-rows (->> all-rows (drop offset) (take (max 1 limit)) vec)
        page-has-more (or has_more (> visible-total (+ offset (count page-rows))))]
    {:page-rows page-rows
     :visible-total visible-total
     :page-has-more page-has-more
     :offset offset
     :limit limit
     :cache cache
     :actor-id actor-id
     :exclude-actor-ids exclude-actor-ids
     :contract-id contract-id}))

(defn response-payload
  [{:keys [rows offset limit page-has-more visible-total cache]}]
  (cond-> {:ok true
           :rows rows
           :offset offset
           :limit limit
           :has_more page-has-more
           :cache cache}
    (not page-has-more) (assoc :total visible-total)))
