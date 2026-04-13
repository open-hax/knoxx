(ns knoxx.backend.memory-routes
  (:require [clojure.string :as str]
            [knoxx.backend.app-shapes :refer [route!]]
            [knoxx.backend.redis-client :as redis]
            [knoxx.backend.session-store :as session-store]))

(defn interactive-session-id?
  [session-id]
  (not (str/starts-with? (str session-id) "translation-")))

(defn register-memory-routes!
  [app runtime config {:keys [json-response!
                              error-response!
                              with-request-context!
                              ensure-permission!
                              parse-positive-int
                              truthy-param?
                              start-session-title-backfill!
                              session-title-backfill*
                              session-titles*
                              openplanner-enabled?
                              openplanner-request!
                              fetch-openplanner-session-rows!
                              session-title-seed-text
                              heuristic-session-title
                              resolve-session-title!
                              cache-session-title!
                              normalize-session-title
                              session-visible?
                              openplanner-memory-search!
                              filter-authorized-memory-hits!
                              ctx-permitted?
                              system-admin?
                              http-error
                              now-iso
                              broadcast-ws!
                              lounge-messages*
                              authorized-session-ids!]}]
  (route! app "GET" "/api/memory/sessions"
          (fn [request reply]
            (if-not (openplanner-enabled? config)
              (json-response! reply 503 {:detail "OpenPlanner is not configured"})
              (with-request-context! runtime request reply
                (fn [ctx]
                  (ensure-permission! ctx "agent.memory.cross_session")
                  (let [limit-raw (aget request "query" "limit")
                        limit (or (parse-positive-int limit-raw) 12)
                        offset-raw (aget request "query" "offset")
                        offset-parsed (js/parseInt (str (or offset-raw "0")) 10)
                        offset (if (and (js/Number.isFinite offset-parsed) (>= offset-parsed 0)) offset-parsed 0)
                        upstream-page-size 200
                        fetch-authorized-pages!
                        (fn fetch-authorized-pages! [upstream-offset acc]
                          (-> (openplanner-request! config "GET"
                                                    (str "/v1/sessions?project="
                                                         (js/encodeURIComponent (:session-project-name config))
                                                         "&limit=" upstream-page-size
                                                         "&offset=" upstream-offset))
                              (.then (fn [body]
                                       (let [page-rows (vec (or (:rows body) []))
                                             fetched-count (count page-rows)
                                             next-offset (+ upstream-offset fetched-count)
                                             upstream-has-more (boolean (:has_more body))]
                                         (-> (authorized-session-ids! config ctx (map :session page-rows))
                                             (.then (fn [allowed]
                                                      (let [authorized-rows (->> page-rows
                                                                                 (filter #(contains? allowed (str (:session %))))
                                                                                 (filter #(interactive-session-id? (:session %)))
                                                                                 vec)
                                                            next-acc (into acc authorized-rows)]
                                                        (if (and upstream-has-more (pos? fetched-count))
                                                          (fetch-authorized-pages! next-offset next-acc)
                                                          next-acc))))))))))]
                    (-> (fetch-authorized-pages! 0 [])
                        (.then (fn [all-rows]
                                 (let [all-rows (vec all-rows)
                                       total (count all-rows)
                                       rows (->> all-rows
                                                 (drop offset)
                                                 (take (max 1 limit))
                                                 vec)
                                                    redis-client (redis/get-client)
                                                    inactive-row (fn [row]
                                                                   (assoc row
                                                                          :is_active false
                                                                          :active_status "inactive"
                                                                          :has_active_stream false))
                                                    warm-title-cache! (fn [session-id]
                                                                        (when-not (contains? @session-titles* session-id)
                                                                          (-> (fetch-openplanner-session-rows! config session-id)
                                                                              (.then (fn [title-rows]
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
                                                    enrich-promises (mapv enrich-row rows)]
                                           (doseq [row rows]
                                             (warm-title-cache! (str (:session row))))
                                           (-> (.all js/Promise (clj->js enrich-promises))
                                               (.then (fn [enriched-rows]
                                                        (json-response! reply 200 {:ok true
                                                                                   :rows (vec (js->clj enriched-rows :keywordize-keys true))
                                                                                   :total total
                                                                                   :offset offset
                                                                                   :limit limit
                                                                                   :has_more (> total (+ offset (count rows)))})
                                                        nil))
                                               (.catch (fn [err]
                                                         (error-response! reply err 502)
                                                         nil))))))
                        (.catch (fn [err]
                                  (error-response! reply err 502)
                                  nil))))))))

  (route! app "GET" "/api/memory/session-titles/status"
          (fn [request reply]
            (if-not (openplanner-enabled? config)
              (json-response! reply 503 {:detail "OpenPlanner is not configured"})
              (with-request-context! runtime request reply
                (fn [ctx]
                  (ensure-permission! ctx "agent.memory.cross_session")
                  (json-response! reply 200 {:ok true
                                             :status @session-title-backfill*
                                             :cached_count (count @session-titles*)}))))))

  (route! app "POST" "/api/memory/sessions/backfill-titles"
          (fn [request reply]
            (if-not (openplanner-enabled? config)
              (json-response! reply 503 {:detail "OpenPlanner is not configured"})
              (with-request-context! runtime request reply
                (fn [ctx]
                  (ensure-permission! ctx "agent.memory.cross_session")
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
                                  (error-response! reply err 502))))))))))

  (route! app "POST" "/api/memory/sessions/import-titles"
          (fn [request reply]
            (if-not (openplanner-enabled? config)
              (json-response! reply 503 {:detail "OpenPlanner is not configured"})
              (with-request-context! runtime request reply
                (fn [ctx]
                  (ensure-permission! ctx "agent.memory.cross_session")
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
                                               :cached_count (count @session-titles*)})))))))

  (route! app "GET" "/api/memory/sessions/:sessionId"
          (fn [request reply]
            (if-not (openplanner-enabled? config)
              (json-response! reply 503 {:detail "OpenPlanner is not configured"})
              (with-request-context! runtime request reply
                (fn [ctx]
                  (ensure-permission! ctx "agent.memory.read")
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
                                    (error-response! reply err 502)))))))))))

  (route! app "POST" "/api/memory/search"
          (fn [request reply]
            (if-not (openplanner-enabled? config)
              (json-response! reply 503 {:detail "OpenPlanner is not configured"})
              (with-request-context! runtime request reply
                (fn [ctx]
                  (let [body (or (aget request "body") #js {})
                        query (or (aget body "query") "")
                        k (aget body "k")
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
                                              (json-response! reply 200 (assoc result :ok true :hits hits)))))))
                        (.catch (fn [err]
                                  (error-response! reply err 502))))))))))

  (route! app "GET" "/api/lounge/messages"
          (fn [_request reply]
            (json-response! reply 200 {:messages @lounge-messages*})))

  (route! app "POST" "/api/lounge/messages"
          (fn [request reply]
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
                        (json-response! reply 200 {:ok true :message msg}))))))))
