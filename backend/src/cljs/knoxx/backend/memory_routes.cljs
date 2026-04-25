(ns knoxx.backend.memory-routes
  (:require-macros [knoxx.backend.macros :refer [defroute]])
  (:require [clojure.string :as str]
            [knoxx.backend.app-shapes :refer [route!]]
            [knoxx.backend.redis-client :as redis]
            [knoxx.backend.session-store :as session-store]
            [shadow.cljs.modern :refer [js-await]]))

(defn interactive-session-id?
  [session-id]
  (not (str/starts-with? (str session-id) "translation-")))

(defn- normalized-actor-id
  [value]
  (some-> value str str/trim not-empty))

(defn- normalized-actor-ids
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

(defn- filter-page-actor-rows!
  [config fetch-openplanner-session-rows! session-matches-page-actor-filter? actor-id exclude-actor-ids page-rows]
  (if (and (str/blank? (str (or actor-id "")))
           (empty? exclude-actor-ids))
    (js/Promise.resolve (vec page-rows))
    (-> (.all js/Promise
              (clj->js
               (mapv (fn [row]
                       (-> (fetch-openplanner-session-rows! config (:session row))
                           (.then (fn [rows]
                                    {:row row
                                     :visible (session-matches-page-actor-filter? config rows actor-id exclude-actor-ids)}))
                           (.catch (fn [_]
                                     {:row row
                                      :visible false}))))
                     page-rows)))
        (.then (fn [results]
                 (->> (js->clj results :keywordize-keys true)
                      (filter :visible)
                      (map :row)
                      vec))))))

(defn fetch-authorized-session-pages!
  [config ctx actor-id exclude-actor-ids openplanner-request! authorized-session-ids! fetch-openplanner-session-rows! session-matches-page-actor-filter? upstream-page-size upstream-offset acc needed-count]
  (-> (openplanner-request! config "GET"
                            (str "/v1/sessions?project="
                                 (js/encodeURIComponent (:session-project-name config))
                                 "&limit=" upstream-page-size
                                 "&offset=" upstream-offset))
      (.then
       (fn [body]
         (let [page-rows (vec (or (:rows body) []))
               fetched-count (count page-rows)
               next-offset (+ upstream-offset fetched-count)
               upstream-has-more (boolean (:has_more body))]
           (-> (authorized-session-ids! config ctx (map :session page-rows))
               (.then
                (fn [allowed]
                  (let [authorized-rows (->> page-rows
                                             (filter #(contains? allowed (str (:session %))))
                                             (filter #(interactive-session-id? (:session %)))
                                             vec)]
                    (-> (filter-page-actor-rows! config fetch-openplanner-session-rows! session-matches-page-actor-filter? actor-id exclude-actor-ids authorized-rows)
                        (.then
                         (fn [actor-visible-rows]
                           (let [next-acc (into acc actor-visible-rows)
                                 reached-target? (and (number? needed-count)
                                                      (>= (count next-acc) needed-count))]
                             (cond
                               reached-target?
                               (js/Promise.resolve {:rows (vec (take needed-count next-acc))
                                                    :has_more true})

                               (and upstream-has-more (pos? fetched-count))
                               (fetch-authorized-session-pages! config ctx actor-id exclude-actor-ids openplanner-request! authorized-session-ids! fetch-openplanner-session-rows! session-matches-page-actor-filter? upstream-page-size next-offset next-acc needed-count)

                               :else
                               (js/Promise.resolve {:rows next-acc
                                                    :has_more false}))))))))))))))

(defn- hit-session-id
  [hit]
  (or (:session hit)
      (get-in hit [:metadata :session])
      (get-in hit [:extra :session])))

(defn- filter-search-hits-by-actor!
  [config fetch-openplanner-session-rows! session-matches-page-actor-filter? actor-id exclude-actor-ids hits]
  (let [actor-id (normalized-actor-id actor-id)
        exclude-actor-ids (normalized-actor-ids exclude-actor-ids)
        hits (vec hits)]
    (if (and (str/blank? (str (or actor-id "")))
             (empty? exclude-actor-ids))
      (js/Promise.resolve hits)
      (let [session-ids (->> hits
                             (keep hit-session-id)
                             (map str)
                             (remove str/blank?)
                             distinct
                             vec)]
        (-> (.all js/Promise
                  (clj->js
                   (mapv (fn [session-id]
                           (-> (fetch-openplanner-session-rows! config session-id)
                               (.then (fn [rows]
                                        {:session session-id
                                         :visible (session-matches-page-actor-filter? config rows actor-id exclude-actor-ids)}))
                               (.catch (fn [_]
                                         {:session session-id
                                          :visible false}))))
                         session-ids)))
            (.then (fn [results]
                     (let [allowed-sessions (->> (js->clj results :keywordize-keys true)
                                                 (filter :visible)
                                                 (map (comp str :session))
                                                 set)]
                       (->> hits
                            (filter (fn [hit]
                                      (contains? allowed-sessions (str (or (hit-session-id hit) "")))))
                            vec)))))))))

(defroute memory-sessions-route! [openplanner-enabled?]
  "GET" "/api/memory/sessions"
  (if-not (openplanner-enabled? config)
    (json-response! reply 503 {:detail "OpenPlanner is not configured"})
    (let [limit-raw (aget request "query" "limit")
          limit (or (parse-positive-int limit-raw) 12)
          actor-id (some-> (or (aget request "query" "actorId")
                               (aget request "query" "actor"))
                           normalized-actor-id)
          exclude-actor-ids (normalized-actor-ids
                             (or (aget request "query" "excludeActorIds")
                                 (aget request "query" "excludeActorId")
                                 (aget request "query" "excludeActors")))
          offset-raw (aget request "query" "offset")
          offset-parsed (js/parseInt (str (or offset-raw "0")) 10)
          offset (if (and (js/Number.isFinite offset-parsed) (>= offset-parsed 0)) offset-parsed 0)
          upstream-page-size 200
          needed-count (+ offset (max 1 limit) 1)]
      (-> (fetch-authorized-session-pages! config ctx actor-id exclude-actor-ids openplanner-request! authorized-session-ids! fetch-openplanner-session-rows! session-matches-page-actor-filter? upstream-page-size 0 [] needed-count)
          (.then
           (fn [{:keys [rows has_more]}]
             (let [all-rows (vec rows)
                   visible-total (count all-rows)
                   page-rows (->> all-rows (drop offset) (take (max 1 limit)) vec)
                   page-has-more (or has_more
                                     (> visible-total (+ offset (count page-rows))))
                   redis-client (redis/get-client)
                   inactive-row (fn [row]
                                  (assoc row
                                         :is_active false
                                         :active_status "inactive"
                                         :has_active_stream false))
                   warm-title-cache! (fn [session-id]
                                       (when-not (contains? @session-titles* session-id)
                                         (-> (fetch-openplanner-session-rows! config session-id)
                                             (.then
                                              (fn [title-rows]
                                                (let [seed-text (session-title-seed-text title-rows)
                                                      fallback-title (heuristic-session-title seed-text)]
                                                  (-> (resolve-session-title! config seed-text)
                                                      (.then (fn [entry]
                                                               (cache-session-title! runtime config session-id
                                                                                     (or (normalize-session-title (:title entry) fallback-title)
                                                                                         fallback-title)
                                                                                     (:title_model entry))))
                                                      (.catch (fn [_]
                                                                (cache-session-title! runtime config session-id fallback-title nil)))))))
                                             (.catch (fn [_]
                                                       (cache-session-title! runtime config session-id "Untitled session" nil))))))
                   enrich-row (fn [row]
                                (let [session-id (str (:session row))
                                      titled-row (if-let [title-entry (get @session-titles* session-id)]
                                                   (assoc row
                                                          :title (:title title-entry)
                                                          :title_model (:title_model title-entry))
                                                   row)]
                                  (if-not redis-client
                                    (js/Promise.resolve (inactive-row titled-row))
                                    (-> (session-store/get-conversation-active-session redis-client session-id)
                                        (.then (fn [active-session-id]
                                                 (if (str/blank? (str active-session-id))
                                                   (inactive-row titled-row)
                                                   (-> (session-store/get-session redis-client active-session-id)
                                                       (.then (fn [active-session]
                                                                (let [status (or (:status active-session) "inactive")
                                                                      is-active (contains? #{"running" "waiting_input"} status)]
                                                                  (assoc titled-row
                                                                         :active_session_id active-session-id
                                                                         :is_active is-active
                                                                         :active_status status
                                                                         :has_active_stream (boolean (:has_active_stream active-session))))))
                                                       (.catch (fn [_]
                                                                 (inactive-row titled-row)))))))
                                        (.catch (fn [_]
                                                  (inactive-row titled-row)))))))
                   enrich-promises (mapv enrich-row page-rows)]
               (if-not redis-client
                 (do (doseq [row page-rows] (warm-title-cache! (str (:session row))))
                     (-> (.all js/Promise (clj->js enrich-promises))
                         (.then (fn [enriched-rows]
                                  (json-response! reply 200
                                                  (cond-> {:ok true
                                                           :rows (vec (js->clj enriched-rows :keywordize-keys true))
                                                           :offset offset
                                                           :limit limit
                                                           :has_more page-has-more}
                                                    (not page-has-more) (assoc :total visible-total)))
                                  nil))
                         (.catch (fn [err] (error-response! reply err 502) nil))))
                 (-> (session-store/list-active-sessions redis-client)
                     (.then
                      (fn [live-ids]
                        (-> (.all js/Promise (clj->js (mapv #(session-store/get-session redis-client %) (vec live-ids))))
                            (.then
                             (fn [live-js]
                               (let [live (vec (js->clj live-js :keywordize-keys true))
                                     op-ids (set (map #(str (:session %)) page-rows))
                                     synthetic (->> live
                                                    (filter #(and (:conversation_id %)
                                                                  (not (op-ids (str (:conversation_id %))))
                                                                  (contains? #{"running" "waiting_input"} (:status %))))
                                                    (map (fn [s]
                                                           {:session (:conversation_id s)
                                                            :is_active true
                                                            :active_status (:status s)
                                                            :has_active_stream (boolean (:has_active_stream s))
                                                            :title (str "Running · " (or (:run_id s) (:conversation_id s)))
                                                            :event_count 0
                                                            :last_ts (:updated_at s)
                                                            :actor_id (:actor_id s)
                                                            :contract_id (get-in s [:agent_spec :contract_id])})))
                                     all-rows (vec (concat synthetic page-rows))]
                                 (doseq [row all-rows] (warm-title-cache! (str (:session row))))
                                 (-> (.all js/Promise (clj->js (mapv enrich-row all-rows)))
                                     (.then (fn [enriched-rows]
                                              (json-response! reply 200
                                                              (cond-> {:ok true
                                                                       :rows (vec (js->clj enriched-rows :keywordize-keys true))
                                                                       :offset offset
                                                                       :limit limit
                                                                       :has_more page-has-more}
                                                                (not page-has-more) (assoc :total visible-total)))
                                              nil))
                                     (.catch (fn [err] (error-response! reply err 502) nil)))))
                             (.catch (fn [err] (error-response! reply err 502) nil)))))
                     (.catch (fn [_]
                                (doseq [row page-rows] (warm-title-cache! (str (:session row))))
                                (-> (.all js/Promise (clj->js enrich-promises))
                                    (.then (fn [enriched-rows]
                                             (json-response! reply 200
                                                             (cond-> {:ok true
                                                                      :rows (vec (js->clj enriched-rows :keywordize-keys true))
                                                                      :offset offset
                                                                      :limit limit
                                                                      :has_more page-has-more}
                                                               (not page-has-more) (assoc :total visible-total)))
                                             nil))
                                    (.catch (fn [err] (error-response! reply err 502) nil))))))))))))))))

(defroute memory-session-titles-status-route! []
  "GET" "/api/memory/session-titles/status"
  (if-not (openplanner-enabled? config)
    (json-response! reply 503 {:detail "OpenPlanner is not configured"})
    (json-response! reply 200 {:ok true
                               :status @session-title-backfill*
                               :cached_count (count @session-titles*)})))

(defroute memory-backfill-titles-route! []
  "POST" "/api/memory/sessions/backfill-titles"
  (if-not (openplanner-enabled? config)
    (json-response! reply 503 {:detail "OpenPlanner is not configured"})
    (let [body (or (aget request "body") #js {})
          limit (or (parse-positive-int (aget body "limit"))
                    (parse-positive-int (aget request "query" "limit")))
          force? (or (truthy-param? (aget body "force"))
                     (truthy-param? (aget request "query" "force")))]
      (-> (start-session-title-backfill! runtime config {:force force?
                                                         :limit limit})
          (.then (fn [status]
                   (json-response! reply 202 {:ok true
                                              :status status
                                              :cached_count (count @session-titles*)})))
          (.catch (fn [err]
                    (error-response! reply err 502)))))))

(defroute memory-import-titles-route! []
  "POST" "/api/memory/sessions/import-titles"
  (if-not (openplanner-enabled? config)
    (json-response! reply 503 {:detail "OpenPlanner is not configured"})
    (let [body (js->clj (or (aget request "body") #js {}))
          titles (or (get body "titles") {})
          updated (reduce-kv (fn [total session-id entry]
                               (let [session-id (str session-id)
                                     raw-title (if (map? entry) (or (get entry "title") (get entry :title)) entry)
                                     title-model (when (map? entry)
                                                   (or (get entry "title_model")
                                                       (get entry "title-model")
                                                       (get entry "model")
                                                       (:title_model entry)
                                                       (:title-model entry)
                                                       (:model entry)))
                                     normalized (normalize-session-title raw-title)]
                                 (if (or (str/blank? session-id) (nil? normalized))
                                   total
                                   (do
                                     (cache-session-title! runtime config session-id normalized (or title-model "retro:heuristic"))
                                     (inc total)))))
                             0
                             titles)]
      (json-response! reply 200 {:ok true
                                 :updated updated
                                 :cached_count (count @session-titles*)}))))

(defroute memory-session-by-id-route! []
  "GET" "/api/memory/sessions/:sessionId"
  (if-not (openplanner-enabled? config)
    (json-response! reply 503 {:detail "OpenPlanner is not configured"})
    (let [session-id (or (aget request "params" "sessionId") "")]
      (if (str/blank? session-id)
        (json-response! reply 400 {:detail "sessionId is required"})
        (-> (fetch-openplanner-session-rows! config session-id)
            (.then (fn [rows]
                     (if (session-visible? ctx rows)
                       (json-response! reply 200 {:ok true
                                                  :session session-id
                                                  :rows rows})
                       (error-response! reply (http-error 403 "memory_scope_denied" "Session is outside the current Knoxx scope")))))
            (.catch (fn [err]
                      (error-response! reply err 502))))))))

(defroute memory-search-route! []
  "POST" "/api/memory/search"
  (if-not (openplanner-enabled? config)
    (json-response! reply 503 {:detail "OpenPlanner is not configured"})
    (let [body (or (aget request "body") #js {})
          query (or (aget body "query") "")
          k (aget body "k")
          actor-id (normalized-actor-id (or (aget body "actorId")
                                            (aget body "actor_id")
                                            (aget body "actor")))
          exclude-actor-ids (normalized-actor-ids
                             (or (aget body "excludeActorIds")
                                 (aget body "exclude_actor_ids")
                                 (aget body "excludeActorId")
                                 (aget body "exclude_actor_id")))
          session-id (or (aget body "sessionId") (aget body "session_id") "")]
      (ensure-permission! ctx "agent.memory.read")
      (when (and (str/blank? (str session-id))
                 (not (ctx-permitted? ctx "agent.memory.cross_session"))
                 (not (system-admin? ctx)))
        (throw (http-error 403 "memory_scope_denied" "Cross-session memory search is outside the current Knoxx scope")))
      (-> (openplanner-memory-search! config {:query query
                                              :k k
                                              :session-id session-id})
          (.then (fn [result]
                   (-> (filter-authorized-memory-hits! config ctx (:hits result))
                       (.then (fn [hits]
                                (-> (filter-search-hits-by-actor! config fetch-openplanner-session-rows! session-matches-page-actor-filter? actor-id exclude-actor-ids hits)
                                    (.then (fn [filtered-hits]
                                             (json-response! reply 200 (assoc result :ok true :hits filtered-hits))))))))))
          (.catch (fn [err]
                    (error-response! reply err 502)))))))

(defroute lounge-messages-list-route! []
  "GET" "/api/lounge/messages"
  (json-response! reply 200 {:messages @lounge-messages*}))

(defroute lounge-messages-create-route! []
  "POST" "/api/lounge/messages"
  (let [body (or (aget request "body") #js {})
        session-id (str (or (aget body "session_id") ""))
        alias (str/trim (str (or (aget body "alias") "anonymous")))
        text (str/trim (str (or (aget body "text") "")))]
    (cond
      (str/blank? session-id) (json-response! reply 400 {:detail "session_id is required"})
      (str/blank? text) (json-response! reply 400 {:detail "text is required"})
      :else (let [msg {:id (str (.randomUUID (aget runtime "crypto")))
                       :timestamp (now-iso)
                       :session_id session-id
                       :alias (if (str/blank? alias) "anonymous" alias)
                       :text text}]
              (swap! lounge-messages* #(->> (conj (vec %) msg) (take-last 100) vec))
              (broadcast-ws! "lounge" msg)
              (json-response! reply 200 {:ok true :message msg})))))

(defn register-memory-routes!
  [app runtime config deps]
  (memory-sessions-route! app runtime config deps)
  (memory-session-titles-status-route! app runtime config deps)
  (memory-backfill-titles-route! app runtime config deps)
  (memory-import-titles-route! app runtime config deps)
  (memory-session-by-id-route! app runtime config deps)
  (memory-search-route! app runtime config deps)
  (lounge-messages-list-route! app runtime config deps)
  (lounge-messages-create-route! app runtime config deps))
