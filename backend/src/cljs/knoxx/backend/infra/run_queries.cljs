(ns knoxx.backend.infra.run-queries
  "Authenticated run and reconnect reads from the selected persistence port."
  (:require [clojure.string :as str]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.run-events :as events]
            [knoxx.backend.infra.stores.session-store-registry :as registry]
            [knoxx.backend.shape.run-directory :as directory]
            [knoxx.backend.shape.session-persistence :as persistence]))

(defn- authorize! [ctx]
  (when-not ctx (throw (ex-info "Authentication required" {:status 401})))
  (authz/ensure-permission! ctx "agent.chat.use"))

(defn- visible? [ctx run]
  (and (or (authz/system-admin? ctx)
           (authz/ctx-permitted? ctx "agent.runs.read_all")
           (and (some? (authz/ctx-org-id ctx))
                (= (authz/ctx-org-id ctx) (authz/record-org-id run))))
       (authz/run-visible? ctx run)))

(defn ^:async read!
  "Read an existing run with fresh tenant and principal authorization."
  [ctx run-id]
  (authorize! ctx)
  (let [store @registry/session-store*
        _ (when-not store
            (throw (ex-info "A durable run provider is required"
                            {:status 503 :code "run_provider_unavailable"})))
        run (await (persistence/get-run store run-id))]
    (when-not run (throw (ex-info "Run not found" {:status 404 :code "run_not_found"})))
    (when-not (visible? ctx run)
      (throw (ex-info "Access denied" {:status 403 :code "run_access_denied"})))
    run))

(defn- cursor [since]
  (cond
    (or (nil? since) (= "" since)) nil
    (and (string? since) (re-matches #"[0-9]+" since))
    (let [value (parse-long since)]
      (when-not value (throw (ex-info "Invalid event cursor" {:status 400}))) value)
    (and (string? since) (not (str/blank? since))) since
    :else (throw (ex-info "Invalid event cursor" {:status 400}))))

(defn ^:async events-since!
  "Authorize the run before exposing any event, including after restart."
  [ctx run-id since]
  (await (read! ctx run-id))
  (let [store @registry/session-store*]
    (when-not (satisfies? persistence/IRunEventStore store)
      (throw (ex-info "The selected run provider does not support durable events"
                      {:status 503 :code "run_event_provider_unsupported"})))
    (await (events/flush! run-id))
    (await (persistence/events-since store run-id (cursor since)))))

(defn ^:async list!
  "List authorized durable snapshots, including after process restart."
  [ctx limit]
  (authorize! ctx)
  (when-not (and (integer? limit) (<= 1 limit 500))
    (throw (ex-info "Run list limit must be an integer between 1 and 500" {:status 400})))
  (let [store @registry/session-store*
        scope (if (or (authz/system-admin? ctx) (authz/ctx-permitted? ctx "agent.runs.read_all"))
                {:all? true} {:org-id (authz/ctx-org-id ctx)})]
    (when-not (satisfies? directory/IRunDirectoryStore store)
      (throw (ex-info "The selected run provider does not support durable listing"
                      {:status 503 :code "run_directory_provider_unsupported"})))
    (->> (await (directory/list-runs store scope))
         (filter #(visible? ctx %)) (take limit) vec)))
