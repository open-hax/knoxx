(ns knoxx.backend.infra.execution.client
  "Actor-scoped execution transport; no caller environment crosses this port."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.contracts.loader :as contracts]
            [knoxx.backend.extern.crypto :as crypto]
            [knoxx.backend.extern.fetch :as fetch]
            [knoxx.backend.extern.node-fs :as fs]
            [knoxx.backend.infra.openplanner.knowledge-scope :as identity]
            [knoxx.backend.law.execution :as law]))

(defn effective-config
  "An actor can require isolated coding regardless of the serving host's defaults."
  [config agent-spec]
  (let [actor-id (:actor-id agent-spec)
        required? (and (seq actor-id)
                       (some (fn [{:keys [contract]}]
                               (and (= actor-id (:actor/id contract))
                                    (true? (get-in contract [:actor/metadata :execution/isolation-required]))))
                             (contracts/load-all-contracts-sync config)))]
    (if (and required? (not (law/isolated? config)))
      (assoc config :execution-mode "disabled")
      config)))

(defn principal
  "Use the same server-resolved actor binding as graph knowledge."
  [config auth-context]
  (law/assert-valid! law/Principal
                     (select-keys (identity/principal config auth-context) [:actor-id :org-slug])))

(defn workspace
  "Stable exact actor workspace shared by its authorized turns, never another actor."
  [config principal]
  (str (str/replace (or (:execution-workspace-root config) "/state/execution-workspaces") #"/$" "")
       "/" (crypto/sha256-hex (law/scope-material principal))))

(defn ^:async workspace-config!
  "Align trusted media paths with the actor workspace without copying service state."
  [config auth-context actor-id]
  (if-not (law/isolated? config)
    config
    (let [config (assoc config :graph-actor-id actor-id)
          identity (principal config auth-context)
          root (if (= (:actor-id identity) (:execution-legacy-actor config))
                 (:workspace-root config)
                 (workspace config identity))]
      (law/require-client! config)
      (await (fs/ensure-private-sandbox-directory! root 448))
      (assoc config :workspace-root root))))

(defn- ^:async request! [config auth-context tool parameters protocol]
  (law/require-client! config)
  (let [request (law/assert-valid! law/Request
                                   (cond-> {:principal (principal config auth-context)
                                            :tool tool :parameters parameters}
                                     protocol (assoc :protocol protocol)))
        response (await (fetch/json! fetch/default-client
                                     {:url (str (str/replace (:execution-base-url config) #"/$" "") "/execute")
                                      :method "POST"
                                      :headers {"authorization" (str "Bearer " (:execution-auth-key config))}
                                      :json request :timeout-ms 315000}))]
    (when-not (:ok response)
      (throw (ex-info "Isolated execution controller refused the call"
                      {:status 503 :code "execution_refused" :upstream-status (:status response)})))
    (:body response)))

(defn ^:async execute!
  "Send an allowlisted SDK tool call to the controller, with no environment data."
  [config auth-context tool parameters]
  (let [result (law/assert-valid! law/Result (await (request! config auth-context tool parameters nil)))]
      (when (:isError result)
        (throw (ex-info (or (get-in result [:content 0 :text]) "Isolated tool failed")
                        {:code "execution_tool_failed"})))
    result))

(defn ^:async http!
  "Preserve the HTTP file/shell protocol inside the actor worker, never the backend."
  [config auth-context tool parameters]
  (law/assert-valid! law/HttpResult (await (request! config auth-context tool parameters "http"))))
