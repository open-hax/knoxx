(ns knoxx.backend.extern.memory-session-pages
  "Scoped memory provider pagination and native response rows."
  (:require [clojure.string :as str]
            [knoxx.backend.infra.clients.openplanner :as openplanner-client]
            [knoxx.backend.infra.core-memory :as core-memory]
            [knoxx.backend.infra.openplanner.scope :as planner-scope]
            [knoxx.backend.shape.memory-sessions :as memory-shape]))

(defn interactive-session-id?
  "Memory session operation: interactive-session-id?."
  [session-id]
  (not (str/starts-with? (str session-id) "translation-")))


(defn- fetch-session-filter-rows!
  [fetch-openplanner-session-rows! config session-id]
  (if (identical? fetch-openplanner-session-rows! core-memory/fetch-openplanner-session-rows!)
    (core-memory/fetch-openplanner-session-visibility-rows! config session-id)
    (fetch-openplanner-session-rows! config session-id)))

(defn ^:async filter-page-actor-row!
  "Memory session operation: filter-page-actor-row!."
  [config fetch-openplanner-session-rows! session-matches-page-actor-filter?
   actor-id exclude-actor-ids contract-id row]
  (try
    (let [rows (await (fetch-session-filter-rows! fetch-openplanner-session-rows! config (:session row)))]
      {:row (merge row (core-memory/session-summary-scope-from-rows rows))
       :visible (and (session-matches-page-actor-filter? config rows actor-id exclude-actor-ids)
                     (core-memory/session-matches-contract-filter? config rows contract-id))})
    (catch :default _
      {:row row
       :visible false})))

(defn ^:async filter-page-actor-rows!
  "Memory session operation: filter-page-actor-rows!."
  [config fetch-openplanner-session-rows! session-matches-page-actor-filter? actor-id exclude-actor-ids contract-id page-rows]
  (if (and (str/blank? (str (or actor-id "")))
           (empty? exclude-actor-ids)
           (str/blank? (str (or contract-id ""))))
    (vec page-rows)
    (let [results (await (.all js/Promise
                               (clj->js
                                (mapv (partial filter-page-actor-row!
                                               config
                                               fetch-openplanner-session-rows!
                                               session-matches-page-actor-filter?
                                               actor-id
                                               exclude-actor-ids
                                               contract-id)
                                      page-rows))))]
      (->> (js->clj results :keywordize-keys true)
           (filter :visible)
           (map :row)
           vec))))

(defn- grouped-session-rows
  [rows]
  (reduce (fn [state row]
            (let [session-id (str (:session row))]
              (if (or (str/blank? session-id)
                      (not (interactive-session-id? session-id)))
                state
                (-> state
                    (update :groups update session-id (fnil conj []) row)
                    (update :order (fn [order]
                                     (if (contains? (:seen state) session-id)
                                       order
                                       (conj order session-id))))
                    (update :seen conj session-id)))))
          {:groups {} :order [] :seen #{}}
          rows))

(defn- session-summary-title
  [session-id scope]
  (let [event-type (:event_type scope)
        trigger-id (:trigger_id scope)]
    (cond
      (and event-type trigger-id) (str "event: " event-type " trigger: " trigger-id)
      trigger-id (str "trigger: " trigger-id)
      event-type (str "event: " event-type)
      :else session-id)))

(defn- mongo-session-summary
  [session-id rows]
  (let [latest (first rows)
        scope (core-memory/session-summary-scope-from-rows rows)]
    (merge {:session session-id
            :project (:project latest)
            :last_ts (:ts latest)
            :event_count (count rows)
            :title (session-summary-title session-id scope)}
           scope)))

(defn ^:async fetch-contract-session-pages-from-mongo!
  "Memory session operation: fetch-contract-session-pages-from-mongo!."
  [config contract-id needed-count]
  (let [target (some-> contract-id str str/trim not-empty)
        query-limit 500]
    (when target
      (let [body (await (openplanner-client/mongo-query! (or (:openplanner-client config)
                                                             (openplanner-client/client config))
                                                         {:collection "events"
                                                          :filter {"project" (:session-project-name config)
                                                                   "session" {"$type" "string" "$ne" ""}
                                                                   "extra.contract_id" target}
                                                          :projection {"_id" 0
                                                                       "project" 1
                                                                       "session" 1
                                                                       "ts" 1
                                                                       "extra" 1}
                                                          :sort {"ts" -1}
                                                          :limit query-limit}))
            rows (vec (or (:rows body) []))
            {:keys [groups order]} (grouped-session-rows rows)
            summaries (mapv #(mongo-session-summary % (get groups %)) order)
            wanted-count (max 1 (or needed-count 1))
            selected (vec (take wanted-count summaries))
            total-events (or (:total body) 0)
            fetched-events (count rows)
            more-events? (> total-events fetched-events)
            more-sessions? (> (count summaries) wanted-count)]
        {:rows selected
         :has_more (or more-sessions? more-events?)}))))

(defn ^:async fetch-authorized-session-pages!
  "Memory session operation: fetch-authorized-session-pages!."
  [config ctx actor-id exclude-actor-ids contract-id authorized-session-ids! fetch-openplanner-session-rows! session-matches-page-actor-filter? upstream-page-size upstream-offset acc needed-count]
  (let [config (planner-scope/scoped-config config ctx)
        body (await (openplanner-client/sessions! (or (:openplanner-client config)
                                                      (openplanner-client/client config))
                                                  {:org_id (planner-scope/org-id! config)
                                                   :project (:session-project-name config)
                                                   :limit upstream-page-size
                                                   :offset upstream-offset}))
        page-rows (vec (or (:rows body) []))
        fetched-count (count page-rows)
        next-offset (+ upstream-offset fetched-count)
        upstream-has-more (boolean (:has_more body))
        allowed (await (authorized-session-ids! config ctx (map :session page-rows)))
        authorized-rows (->> page-rows
                             (filter #(contains? allowed (str (:session %))))
                             (filter #(interactive-session-id? (:session %)))
                             vec)
        actor-visible-rows (await (filter-page-actor-rows! config fetch-openplanner-session-rows! session-matches-page-actor-filter? actor-id exclude-actor-ids contract-id authorized-rows))
        next-acc (into acc actor-visible-rows)
        reached-target? (and (number? needed-count)
                             (>= (count next-acc) needed-count))]
    (cond
      reached-target? {:rows (vec (take needed-count next-acc))
                       :has_more true}
      (and upstream-has-more (pos? fetched-count))
      (await (fetch-authorized-session-pages! config ctx actor-id exclude-actor-ids contract-id authorized-session-ids! fetch-openplanner-session-rows! session-matches-page-actor-filter? upstream-page-size next-offset next-acc needed-count))
      :else {:rows next-acc
             :has_more false})))

(defn- hit-session-id
  [hit]
  (or (:session hit)
      (get-in hit [:metadata :session])
      (get-in hit [:extra :session])))

(defn ^:async search-hit-session-visibility!
  "Memory session operation: search-hit-session-visibility!."
  [config fetch-openplanner-session-rows! session-matches-page-actor-filter? actor-id exclude-actor-ids session-id]
  (try
    (let [rows (await (fetch-session-filter-rows! fetch-openplanner-session-rows! config session-id))]
      {:session session-id
       :visible (session-matches-page-actor-filter? config rows actor-id exclude-actor-ids)})
    (catch :default _
      {:session session-id
       :visible false})))

(defn ^:async filter-search-hits-by-actor!
  "Memory session operation: filter-search-hits-by-actor!."
  [config fetch-openplanner-session-rows! session-matches-page-actor-filter? actor-id exclude-actor-ids hits]
  (let [actor-id (memory-shape/normalized-actor-id actor-id)
        exclude-actor-ids (memory-shape/normalized-actor-ids exclude-actor-ids)
        hits (vec hits)]
    (if (and (str/blank? (str (or actor-id "")))
             (empty? exclude-actor-ids))
      hits
      (let [visibility! (partial search-hit-session-visibility!
                                 config fetch-openplanner-session-rows!
                                 session-matches-page-actor-filter? actor-id exclude-actor-ids)
            session-ids (->> hits
                             (keep hit-session-id)
                             (map str)
                             (remove str/blank?)
                             distinct
                             vec)
            results (await (.all js/Promise (clj->js (mapv visibility! session-ids))))
            allowed-sessions (->> (js->clj results :keywordize-keys true)
                                  (filter :visible)
                                  (map (comp str :session))
                                  set)]
        (->> hits
             (filter (fn [hit]
                       (contains? allowed-sessions (str (or (hit-session-id hit) "")))))
             vec)))))

