(ns knoxx.backend.routes.actors
  "HTTP routes for actor mailbox delivery projection operations."
  (:require [clojure.string :as str]
            [knoxx.backend.actor-mailbox :as actor-mailbox]
            [knoxx.backend.events.dispatch :as events-dispatch]
            [knoxx.backend.macros :refer-macros [defroute]]))

(defn- query-param
  [request & names]
  (some (fn [name]
          (let [value (aget request "query" name)]
            (when-not (str/blank? (str (or value "")))
              value)))
        names))

(defn- body-map
  [request]
  (js->clj (or (aget request "body") #js {}) :keywordize-keys true))

(defn- statuses-from-body
  [body]
  (let [raw (or (:statuses body) (:status body))]
    (cond
      (sequential? raw) (mapv actor-mailbox/normalize-status raw)
      (string? raw) [(actor-mailbox/normalize-status raw)]
      :else ["pending" "failed"])))

(defn- current-actor-id
  [ctx]
  (or (get-in ctx [:actor :id])
      (get-in ctx [:membership :actorId])
      (:actorId ctx)))

(defn- api-entry
  [entry]
  {:id (:mailbox/id entry)
   :kind (:mailbox/kind entry)
   :status (:mailbox/status entry)
   :source (:mailbox/source entry)
   :target (:mailbox/target entry)
   :delivery (:mailbox/delivery entry)
   :contentRef (:mailbox/content-ref entry)
   :metadata (:mailbox/metadata entry)
   :preview (:mailbox/preview entry)
   :lastError (:mailbox/last-error entry)
   :createdAt (:mailbox/created-at entry)
   :updatedAt (:mailbox/updated-at entry)
   :deliveredAt (:mailbox/delivered-at entry)
   :acknowledgedAt (:mailbox/acknowledged-at entry)
   :expiresAt (:mailbox/expires-at entry)})

(defn- api-result
  [result]
  (-> result
      (assoc :entries (mapv api-entry (:entries result))
             :durable (boolean (:durable? result)))
      (dissoc :durable?)))

(defn- retry-dispatches!
  [entries]
  (.all js/Promise
        (clj->js
         (mapv (fn [entry]
                 (events-dispatch/dispatch! (actor-mailbox/retry-request-event entry)))
               entries))))

(defroute actor-mailbox-list-route!
  []
  "GET" "/api/admin/config/actors/mailbox"
  [session-guard]
  (try
    (ensure-permission! ctx "org.event_agents.control")
    (-> (actor-mailbox/list-entries!
         runtime
         {:status (query-param request "status")
          :target-actor-id (query-param request "target_actor_id" "targetActorId" "actor_id" "actorId")
          :target-session-id (query-param request "target_session_id" "targetSessionId" "session_id" "sessionId")
          :source-actor-id (query-param request "source_actor_id" "sourceActorId")
          :source-run-id (query-param request "source_run_id" "sourceRunId" "run_id" "runId")
          :limit (query-param request "limit")})
        (.then (fn [result]
                 (json-response! reply 200 (assoc (api-result result) :ok true))))
        (.catch (fn [err]
                  (error-response! reply err))))
    (catch :default err
      (error-response! reply err))))

(defn- acknowledge-mailbox!
  ([runtime reply error-response! json-response! mailbox-id]
   (acknowledge-mailbox! runtime reply error-response! json-response! mailbox-id nil))
  ([runtime reply error-response! json-response! mailbox-id target-actor-id]
   (-> (actor-mailbox/acknowledge-entry! runtime mailbox-id target-actor-id)
       (.then (fn [entry]
                (if entry
                  (json-response! reply 200 {:ok true :entry (api-entry entry)})
                  (json-response! reply 404 {:ok false :detail "mailbox entry not found"}))))
       (.catch (fn [err]
                 (error-response! reply err))))))

(defroute actor-mailbox-ack-route!
  []
  "POST" "/api/admin/config/actors/mailbox/:mailboxId/ack"
  [session-guard]
  (try
    (ensure-permission! ctx "org.event_agents.control")
    (acknowledge-mailbox! runtime reply error-response! json-response! (aget request "params" "mailboxId"))
    (catch :default err
      (error-response! reply err))))

(defroute actor-mailbox-retry-route!
  []
  "POST" "/api/admin/config/actors/mailbox/retry"
  [session-guard]
  (try
    (ensure-permission! ctx "org.event_agents.control")
    (let [body (body-map request)
          dispatch-events? (not= false (:dispatch_events body))]
      (-> (actor-mailbox/retry-eligible!
           runtime
           {:mailbox-id (or (:mailbox_id body) (:mailboxId body))
            :statuses (statuses-from-body body)
            :max-attempts (or (:max_attempts body) (:maxAttempts body))
            :limit (:limit body)
            :delay-seconds (or (:delay_seconds body) (:delaySeconds body))})
          (.then (fn [result]
                   (let [entries (:entries result)]
                     (if (and dispatch-events? (seq entries))
                       (-> (retry-dispatches! entries)
                           (.then (fn [dispatch-results]
                                    (json-response! reply 202
                                                    (assoc (api-result result)
                                                           :ok true
                                                           :retry_event_count (count entries)
                                                           :dispatches (js->clj dispatch-results :keywordize-keys true))))))
                       (json-response! reply 202
                                       (assoc (api-result result)
                                              :ok true
                                              :retry_event_count 0))))))
          (.catch (fn [err]
                    (error-response! reply err)))))
    (catch :default err
      (error-response! reply err))))

(defroute actor-mailbox-self-list-route!
  []
  "GET" "/api/actors/mailbox"
  [session-guard]
  (try
    (ensure-permission! ctx "agent.chat.use")
    (let [actor-id (current-actor-id ctx)
          box (some-> (or (query-param request "box") "inbox") str str/lower-case)
          filters (cond-> {:status (query-param request "status")
                           :limit (query-param request "limit")}
                    (= box "outbox") (assoc :source-actor-id actor-id)
                    (not= box "outbox") (assoc :target-actor-id actor-id))]
      (if (str/blank? (str actor-id))
        (json-response! reply 403 {:ok false :detail "current actor is not available"})
        (-> (actor-mailbox/list-entries! runtime filters)
            (.then (fn [result]
                     (json-response! reply 200 (assoc (api-result result)
                                                      :ok true
                                                      :box (if (= box "outbox") "outbox" "inbox")
                                                      :actorId actor-id))))
            (.catch (fn [err]
                      (error-response! reply err))))))
    (catch :default err
      (error-response! reply err))))

(defroute actor-mailbox-self-ack-route!
  []
  "POST" "/api/actors/mailbox/:mailboxId/ack"
  [session-guard]
  (try
    (ensure-permission! ctx "agent.chat.use")
    (let [actor-id (current-actor-id ctx)]
      (if (str/blank? (str actor-id))
        (json-response! reply 403 {:ok false :detail "current actor is not available"})
        (acknowledge-mailbox! runtime reply error-response! json-response! (aget request "params" "mailboxId") actor-id)))
    (catch :default err
      (error-response! reply err))))

(defn register-actor-routes!
  [app runtime config deps]
  (actor-mailbox-list-route! app runtime config deps)
  (actor-mailbox-ack-route! app runtime config deps)
  (actor-mailbox-retry-route! app runtime config deps)
  (actor-mailbox-self-list-route! app runtime config deps)
  (actor-mailbox-self-ack-route! app runtime config deps)
  nil)
