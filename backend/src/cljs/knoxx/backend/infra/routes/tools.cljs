(ns knoxx.backend.infra.routes.tools
  (:require [clojure.string :as str]
            [knoxx.backend.domain.discord.gateway :as dg]
            [knoxx.backend.domain.event.dispatch :as event-dispatch]
            [knoxx.backend.infra.clients.proxx :as proxx-client]
            [knoxx.backend.infra.http :as backend-http]
            [knoxx.backend.macros :refer-macros [defroute]]
            [knoxx.backend.domain.mcp.mcp-bridge :as mcp]
            [knoxx.backend.runtime.state :as runtime-state]
             [knoxx.backend.domain.text :refer [sanitize-svg-content]]
             [knoxx.backend.infra.control-config :as control-config]
             [knoxx.backend.infra.trigger-runner :as trigger-runner]
             ["node:child_process" :refer [execFile]]
            ["node:fs/promises" :as fs]
            ["node:path" :as path]
            ["node:util" :refer [promisify]]
            ["nodemailer" :default nodemailer]))

(def ^:private exec-file-async (promisify execFile))

;; ── Private helpers ─────────────────────────────────────────────────────────

(defn- send-email!
  "Send an email via Gmail SMTP using nodemailer."
  [_runtime config to subject text-body cc bcc]
  (let [email    (:gmail-app-email config)
        password (:gmail-app-password config)]
    (if (or (str/blank? email) (str/blank? password))
      (js/Promise.reject (js/Error. "Gmail credentials not configured"))
      (let [transporter (.createTransport nodemailer
                                          (clj->js {:host   "smtp.gmail.com"
                                                    :port   587
                                                    :secure false
                                                    :auth   {:user email :pass password}}))]
        (.sendMail transporter
                   (clj->js {:from    email
                              :to      (str/join ", " to)
                              :cc      (when (seq cc)  (str/join ", " cc))
                              :bcc     (when (seq bcc) (str/join ", " bcc))
                              :subject subject
                              :text    text-body}))))))

(defn- masked-discord-token [token]
  (if (and (string? token) (> (count token) 8))
    (str (subs token 0 4) "***" (subs token (- (count token) 4)))
    ""))

(defn- event-agents-control-response [config]
  (let [live-config (or @runtime-state/config* config)
        control     (control-config/event-agent-control-config live-config)
        runtime     (event-dispatch/status-snapshot live-config)]
    {:configured       false
     :tokenPreview     ""
     :availableRoles   (control-config/event-agent-role-options live-config)
     :availableSourceKinds (control-config/event-agent-source-kind-options)
     :availableTriggerKinds (control-config/event-agent-trigger-kind-options)
     :control          control
     :runtime          runtime}))

(defn- result-prop
  [result & ks]
  (some (fn [k]
          (cond
            (map? result) (get result k)
            (object? result) (aget result (name k))
            :else nil))
        ks))

(defn- event-agent-result-summary
  [result]
  (when result
    (let [summary (cond-> {}
                    (some? (result-prop result :queued))
                    (assoc :queued (boolean (result-prop result :queued)))
                    (result-prop result :run_id :run-id :runId)
                    (assoc :run_id (str (result-prop result :run_id :run-id :runId)))
                    (result-prop result :conversation_id :conversation-id :conversationId)
                    (assoc :conversation_id (str (result-prop result :conversation_id :conversation-id :conversationId)))
                    (result-prop result :session_id :session-id :sessionId)
                    (assoc :session_id (str (result-prop result :session_id :session-id :sessionId)))
                    (result-prop result :model)
                    (assoc :model (str (result-prop result :model))))]
      (when (seq summary) summary))))

(defn- event-agent-job-run-response!
  [reply job-id result]
  (let [summary (event-agent-result-summary result)]
    (backend-http/json-response! reply 202 (cond-> {:ok true :jobId job-id}
                                            summary (assoc :result summary)))))

(defn- restart-discord-gateway! [token]
  (when (dg/started?)
    (-> (dg/restart! token)
        (.catch (fn [_] nil)))))

;; ── Tool routes ──────────────────────────────────────────────────────────────

(defroute register-tool-catalog-route!
  [tool-catalog ensure-role-can-use!]
  "GET" "/api/tools/catalog"
  [optional-session-guard]
  (let [role              (or (aget request "query" "role") (:knoxx-default-role config))
        agent-contract-id (or (aget request "query" "agent")
                              (aget request "query" "agentId")
                              (aget request "query" "agentContractId"))
        actor-id          (or (aget request "query" "actor")
                              (aget request "query" "actorId"))]
    (when ctx (ensure-permission! ctx "agent.chat.use"))
    (json-response! reply 200 (tool-catalog config role ctx agent-contract-id actor-id))))

(defroute register-email-send-route!
  [ensure-role-can-use!]
  "POST" "/api/tools/email/send"
  [session-guard]
  (try
    (let [body              (or (aget request "body") (js/Object.))
          agent-contract-id (or (aget body "agentContractId") (aget body "agent_contract_id"))
          role              (ensure-role-can-use! ctx (or (aget body "role") (:knoxx-default-role config)) "email.send" agent-contract-id)
          to                (or (aget body "to") (js/Array.))
          cc                (or (aget body "cc") (js/Array.))
          bcc               (or (aget body "bcc") (js/Array.))
          subject           (str (or (aget body "subject") "(no subject)"))
          markdown          (str (or (aget body "markdown") ""))]
      (if (empty? to)
        (json-response! reply 400 {:detail "Missing required field: to array"})
        (-> (send-email! runtime config to subject markdown cc bcc)
            (.then (fn [result]
                     (json-response! reply 200 {:ok true :role role
                                                :message_id (aget result "messageId")})))
            (.catch (fn [err]
                      (json-response! reply 502 {:detail (str "Failed to send email: " (or (aget err "message") (str err)))})))))))
    (catch :default err
      (error-response! reply err)))

(defroute register-websearch-route!
  [ensure-role-can-use!]
  "POST" "/api/tools/websearch"
  [session-guard]
  (try
    (let [body              (or (aget request "body") (js/Object.))
          agent-contract-id (or (aget body "agentContractId") (aget body "agent_contract_id"))
          role              (ensure-role-can-use! ctx (or (aget body "role") (:knoxx-default-role config)) "websearch" agent-contract-id)
          query             (str/trim (str (or (aget body "query") "")))
          num-results       (or (aget body "numResults") 8)
          search-context-size (aget body "searchContextSize")
          allowed-domains   (or (aget body "allowedDomains") [])
          model             (aget body "model")]
      (if (str/blank? query)
        (json-response! reply 400 {:detail "query is required"})
        (-> (proxx-client/websearch! (proxx-client/client config)
                                     {:query query
                                      :numResults num-results
                                      :searchContextSize search-context-size
                                      :allowedDomains allowed-domains
                                      :model model})
            (.then (fn [resp]
                     (if (:ok resp)
                       (json-response! reply 200 (assoc (:body resp) :role role))
                       (json-response! reply (or (:status resp) 502)
                                       {:detail (pr-str (:body resp))}))))
            (.catch (fn [err] (json-response! reply 502 {:detail (str err)}))))))
    (catch :default err
      (error-response! reply err))))

(defroute register-read-route!
  [ensure-role-can-use! resolve-workspace-path clip-text]
  "POST" "/api/tools/read"
  [session-guard]
  (try
    (let [body    (or (aget request "body") (js/Object.))
          agent-contract-id (or (aget body "agentContractId") (aget body "agent_contract_id"))
          role    (ensure-role-can-use! ctx (or (aget body "role") (:knoxx-default-role config)) "read" agent-contract-id)
          path-str (resolve-workspace-path runtime config (or (aget body "path") ""))
          offset  (max 1 (or (aget body "offset") 1))
          limit   (max 1 (or (aget body "limit") 400))]
      (-> (.stat fs path-str)
          (.then (fn [stat]
                   (if (.isDirectory stat)
                     (-> (.readdir fs path-str (clj->js {:withFileTypes true}))
                         (.then (fn [entries]
                                  (let [content-lines (map (fn [e] (str (aget e "name") (when (.isDirectory e) "/")))
                                                           (array-seq entries))
                                        [content truncated] (clip-text (str/join "\n" content-lines))]
                                    (json-response! reply 200 {:ok true :role role :path path-str
                                                               :content content :truncated truncated})))))
                     (-> (.readFile fs path-str "utf8")
                         (.then (fn [text]
                                  (let [lines   (str/split-lines text)
                                        start   (dec offset)
                                        stop    (+ start limit)
                                        numbered (map-indexed (fn [idx line] (str (+ start idx 1) ": " line))
                                                              (take limit (drop start lines)))
                                        [content clipped?] (clip-text (str/join "\n" numbered))]
                                    (json-response! reply 200 {:ok true :role role :path path-str
                                                               :content content
                                                               :truncated (or clipped? (< stop (count lines)))}))))))
                   ))
          (.catch (fn [err] (json-response! reply 404 {:detail (str err)})))))
    (catch :default err
      (error-response! reply err))))

(defroute register-write-route!
  [ensure-role-can-use! resolve-workspace-path]
  "POST" "/api/tools/write"
  [session-guard]
  (try
    (let [body    (or (aget request "body") (js/Object.))
          agent-contract-id (or (aget body "agentContractId") (aget body "agent_contract_id"))
          role    (ensure-role-can-use! ctx (or (aget body "role") (:knoxx-default-role config)) "write" agent-contract-id)
          path-str  (resolve-workspace-path runtime config (or (aget body "path") ""))
          raw-content (str (or (aget body "content") ""))
          content (if (re-find #"(?i)\.svg$" path-str)
                    (sanitize-svg-content raw-content)
                    raw-content)
          overwrite (not= false (aget body "overwrite"))
          create-parents (not= false (aget body "create_parents"))
          parent    (.dirname path path-str)
          check-promise (if overwrite
                          (js/Promise.resolve nil)
                          (-> (.stat fs path-str)
                              (.then (fn [_] (js/Promise.reject (js/Error. (str "File exists and overwrite is false: " path-str)))))
                              (.catch (fn [_] (js/Promise.resolve nil)))))]
      (-> check-promise
          (.then (fn [] (if create-parents
                          (.mkdir fs parent (clj->js {:recursive true}))
                          (js/Promise.resolve nil))))
          (.then (fn [] (.writeFile fs path-str content "utf8")))
          (.then (fn [] (json-response! reply 200 {:ok true :role role :path path-str
                                                   :bytes_written (.-length (.from js/Buffer content "utf8"))})))
          (.catch (fn [err] (json-response! reply 409 {:detail (str err)})))))
    (catch :default err
      (error-response! reply err))))

(defroute register-edit-route!
  [ensure-role-can-use! resolve-workspace-path count-occurrences replace-first]
  "POST" "/api/tools/edit"
  [session-guard]
  (try
    (let [body    (or (aget request "body") (js/Object.))
          agent-contract-id (or (aget body "agentContractId") (aget body "agent_contract_id"))
          role    (ensure-role-can-use! ctx (or (aget body "role") (:knoxx-default-role config)) "edit" agent-contract-id)
          path-str  (resolve-workspace-path runtime config (or (aget body "path") ""))
          old-string (str (or (aget body "old_string") ""))
          new-string (str (or (aget body "new_string") ""))
          replace-all (true? (aget body "replace_all"))]
      (-> (.readFile fs path-str "utf8")
          (.then (fn [current]
                   (if (= (.indexOf current old-string) -1)
                     (js/Promise.reject (js/Error. "old_string not found in file"))
                     (let [replacements (if replace-all (count-occurrences current old-string) 1)
                           updated      (if replace-all
                                          (str/replace current old-string new-string)
                                          (replace-first current old-string new-string))]
                       (-> (.writeFile fs path-str updated "utf8")
                           (.then (fn [] (json-response! reply 200 {:ok true :role role :path path-str
                                                                    :replacements replacements}))))))))
          (.catch (fn [err] (json-response! reply 409 {:detail (str err)})))))
    (catch :default err
      (error-response! reply err))))

(defroute register-bash-route!
  [ensure-role-can-use! resolve-workspace-path clip-text]
  "POST" "/api/tools/bash"
  [session-guard]
  (try
    (let [body     (or (aget request "body") (js/Object.))
          agent-contract-id (or (aget body "agentContractId") (aget body "agent_contract_id"))
          role     (ensure-role-can-use! ctx (or (aget body "role") (:knoxx-default-role config)) "bash" agent-contract-id)
          ;; Keep live-server shell tools short. Long recursive scans have left
          ;; child processes stuck under knoxx-backend; heavy work belongs in a
          ;; sandbox or an explicit operator terminal, not the always-on API.
          timeout-ms (min (max (or (aget body "timeout_ms") 60000) 1000) 120000)
          workdir  (if-let [raw-wd (aget body "workdir")]
                     (resolve-workspace-path runtime config raw-wd)
                     (.resolve path (:workspace-root config)))]
      (-> (exec-file-async "/bin/bash"
                            (clj->js ["-lc" (or (aget body "command") "")])
                            (clj->js {:cwd workdir
                                      :timeout timeout-ms
                                      :killSignal "SIGKILL"
                                      :maxBuffer 1048576}))
          (.then (fn [result]
                   (let [[stdout _]  (clip-text (or (aget result "stdout") "") 24000)
                         [stderr __] (clip-text (or (aget result "stderr") "") 12000)]
                     (json-response! reply 200 {:ok true :role role
                                                :command  (or (aget body "command") "")
                                                :exit_code 0
                                                :stdout stdout :stderr stderr}))))
          (.catch (fn [err]
                    (if (and (aget err "killed") (not (number? (aget err "code"))))
                      (json-response! reply 408 {:detail (str "Command timed out after " (/ timeout-ms 1000) "s")})
                      (let [[stdout _]  (clip-text (or (aget err "stdout") "") 24000)
                            [stderr __] (clip-text (or (aget err "stderr") "") 12000)]
                        (json-response! reply 200 {:ok false :role role
                                                   :command   (or (aget body "command") "")
                                                   :exit_code (if (number? (aget err "code")) (aget err "code") 1)
                                                   :stdout stdout :stderr stderr})))))))
    (catch :default err
      (error-response! reply err))))

(defroute register-discord-publish-route!
  [ensure-role-can-use!]
  "POST" "/api/tools/discord/publish"
  [session-guard]
  (try
    (let [body (or (aget request "body") (js/Object.))
          agent-contract-id (or (aget body "agentContractId") (aget body "agent_contract_id"))]
      (ensure-role-can-use! ctx (or (aget body "role") (:knoxx-default-role config)) "discord.publish" agent-contract-id)
      (json-response! reply 410 {:ok false
                                 :detail "Global Discord publish is disabled. Use actor-owned Discord credentials via Admin → Actors and the discord.send tool."}))
    (catch :default err
      (error-response! reply err))))

;; ── Admin / config routes ───────────────────────────────────────────────────

(defroute register-discord-token-get-route!
  []
  "GET" "/api/admin/config/discord"
  [session-guard]
  (ensure-permission! ctx "org.events.control")
  (json-response! reply 200 {:configured false
                            :tokenPreview ""
                            :credentialSource "actor_credentials"
                            :detail "Discord bot keys are configured per actor in Admin → Actors."}))

(defroute register-discord-token-put-route!
  []
  "PUT" "/api/admin/config/discord"
  [session-guard]
  (try
    (ensure-permission! ctx "org.events.control")
    (json-response! reply 410 {:ok false
                               :configured false
                               :credentialSource "actor_credentials"
                               :detail "Global Discord token configuration has been migrated. Store Discord bot credentials on an actor in Admin → Actors."})
    (catch :default err
      (error-response! reply err))))

(defroute register-event-agents-get-route!
  []
  "GET" "/api/admin/config/event-agents"
  [session-guard]
  (ensure-permission! ctx "org.events.control")
  (json-response! reply 200 (event-agents-control-response config)))

(defroute register-event-agents-put-route!
  []
  "PUT" "/api/admin/config/event-agents"
  [session-guard]
  (try
    (ensure-permission! ctx "org.events.control")
    (let [body        (js->clj (or (aget request "body") (js/Object.)) :keywordize-keys true)
          live-config (or @runtime-state/config* config)
          next-control (control-config/event-agent-control-config
                        (assoc live-config :event-agent-control body))]
      (swap! runtime-state/config* (fn [c] (assoc (or c config) :event-agent-control next-control)))
      (control-config/persist-event-agent-control! next-control)
      (trigger-runner/reload! live-config)
      (json-response! reply 200 (assoc (event-agents-control-response config) :ok true)))
    (catch :default err
      (error-response! reply err))))

(defroute register-event-agents-job-run-route!
  []
  "POST" "/api/admin/config/event-agents/jobs/:jobId/run"
  [session-guard]
  (try
    (ensure-permission! ctx "org.events.control")
    (let [job-id (or (aget request "params" "jobId") "")]
      (if (str/blank? job-id)
        (json-response! reply 400 {:detail "jobId is required"})
        (-> (trigger-runner/run-job! job-id)
            (.then (fn [result] (event-agent-job-run-response! reply job-id result)))
            (.catch (fn [err] (error-response! reply err))))))
    (catch :default err
      (error-response! reply err))))

(defroute register-event-agents-dispatch-route!
  []
  "POST" "/api/admin/config/event-agents/events/dispatch"
  [session-guard]
  (try
    (ensure-permission! ctx "org.events.control")
    (let [body (js->clj (or (aget request "body") (js/Object.)) :keywordize-keys true)]
      (-> (event-dispatch/dispatch! config body)
          (.then (fn [result]
                    (json-response! reply 202 {:ok true
                                               :matchedTriggers (:matchedTriggers result)
                                               :event (:event result)})))
          (.catch (fn [err] (error-response! reply err)))))
    (catch :default err
      (error-response! reply err))))

(defroute register-event-agents-runtime-stop-route!
  []
  "POST" "/api/admin/config/event-agents/runtime/stop"
  [session-guard]
  (ensure-permission! ctx "org.events.control")
  (trigger-runner/stop!)
  (json-response! reply 200 (assoc (event-agents-control-response config) :ok true :action "stopped")))

(defroute register-event-agents-runtime-start-route!
  []
  "POST" "/api/admin/config/event-agents/runtime/start"
  [session-guard]
  (ensure-permission! ctx "org.events.control")
  (trigger-runner/start! config)
  (json-response! reply 200 (assoc (event-agents-control-response config) :ok true :action "started")))

(defroute register-event-agents-runtime-reset-route!
  []
  "POST" "/api/admin/config/event-agents/runtime/reset"
  [session-guard]
  (try
    (ensure-permission! ctx "org.events.control")
    (-> (trigger-runner/reset-runtime! config)
        (.then (fn [summary]
                 (json-response! reply 200
                                 (merge (event-agents-control-response config)
                                        {:ok true
                                         :action "reset"
                                         :reset summary}))))
        (.catch (fn [err]
                  (error-response! reply err))))
    (catch :default err
      (error-response! reply err))))

;; Events aliases — preferred vocabulary going forward.

(defroute register-events-get-route!
  []
  "GET" "/api/admin/config/events"
  [session-guard]
  (ensure-permission! ctx "org.events.control")
  (json-response! reply 200 (event-agents-control-response config)))

(defroute register-events-put-route!
  []
  "PUT" "/api/admin/config/events"
  [session-guard]
  (try
    (ensure-permission! ctx "org.events.control")
    (let [body        (js->clj (or (aget request "body") (js/Object.)) :keywordize-keys true)
          live-config (or @runtime-state/config* config)
          next-control (control-config/event-agent-control-config
                        (assoc live-config :event-agent-control body))]
      (swap! runtime-state/config* (fn [c] (assoc (or c config) :event-agent-control next-control)))
      (control-config/persist-event-agent-control! next-control)
      (trigger-runner/reload! live-config)
      (json-response! reply 200 (assoc (event-agents-control-response config) :ok true)))
    (catch :default err
      (error-response! reply err))))

(defroute register-events-job-run-route!
  []
  "POST" "/api/admin/config/events/jobs/:jobId/run"
  [session-guard]
  (try
    (ensure-permission! ctx "org.events.control")
    (let [job-id (or (aget request "params" "jobId") "")]
      (if (str/blank? job-id)
        (json-response! reply 400 {:detail "jobId is required"})
        (-> (trigger-runner/run-job! job-id)
            (.then (fn [result] (event-agent-job-run-response! reply job-id result)))
            (.catch (fn [err] (error-response! reply err))))))
    (catch :default err
      (error-response! reply err))))

(defroute register-events-dispatch-route!
  []
  "POST" "/api/admin/config/events/dispatch"
  [session-guard]
  (try
    (ensure-permission! ctx "org.events.control")
    (let [body (js->clj (or (aget request "body") (js/Object.)) :keywordize-keys true)]
      (-> (event-dispatch/dispatch! config body)
          (.then (fn [result]
                    (json-response! reply 202 {:ok true
                                               :matchedTriggers (:matchedTriggers result)
                                               :event (:event result)})))
          (.catch (fn [err] (error-response! reply err)))))
    (catch :default err
      (error-response! reply err))))

(defroute register-events-runtime-stop-route!
  []
  "POST" "/api/admin/config/events/runtime/stop"
  [session-guard]
  (ensure-permission! ctx "org.events.control")
  (trigger-runner/stop!)
  (json-response! reply 200 (assoc (event-agents-control-response config) :ok true :action "stopped")))

(defroute register-events-runtime-start-route!
  []
  "POST" "/api/admin/config/events/runtime/start"
  [session-guard]
  (ensure-permission! ctx "org.events.control")
  (trigger-runner/start! config)
  (json-response! reply 200 (assoc (event-agents-control-response config) :ok true :action "started")))

(defroute register-events-runtime-reset-route!
  []
  "POST" "/api/admin/config/events/runtime/reset"
  [session-guard]
  (try
    (ensure-permission! ctx "org.events.control")
    (-> (trigger-runner/reset-runtime! config)
        (.then (fn [summary]
                 (json-response! reply 200
                                 (merge (event-agents-control-response config)
                                        {:ok true
                                         :action "reset"
                                         :reset summary}))))
        (.catch (fn [err]
                  (error-response! reply err))))
    (catch :default err
      (error-response! reply err))))

;; Legacy aliases

(defroute register-discord-control-get-route!
  []
  "GET" "/api/admin/config/discord/control"
  [session-guard]
  (ensure-permission! ctx "org.events.control")
  (json-response! reply 200 (event-agents-control-response config)))

(defroute register-discord-control-put-route!
  []
  "PUT" "/api/admin/config/discord/control"
  [session-guard]
  (try
    (ensure-permission! ctx "org.events.control")
    (let [body        (js->clj (or (aget request "body") (js/Object.)) :keywordize-keys true)
          live-config (or @runtime-state/config* config)
          next-control (control-config/event-agent-control-config
                        (assoc live-config :event-agent-control body))]
      (swap! runtime-state/config* (fn [c] (assoc (or c config) :event-agent-control next-control)))
      (control-config/persist-event-agent-control! next-control)
      (trigger-runner/reload! live-config)
      (json-response! reply 200 (assoc (event-agents-control-response config) :ok true)))
    (catch :default err
      (error-response! reply err))))

(defroute register-discord-control-job-run-route!
  []
  "POST" "/api/admin/config/discord/control/jobs/:jobId/run"
  [session-guard]
  (try
    (ensure-permission! ctx "org.events.control")
    (let [job-id (or (aget request "params" "jobId") "")]
      (if (str/blank? job-id)
        (json-response! reply 400 {:detail "jobId is required"})
        (-> (trigger-runner/run-job! job-id)
            (.then (fn [result] (event-agent-job-run-response! reply job-id result)))
            (.catch (fn [err] (error-response! reply err))))))
    (catch :default err
      (error-response! reply err))))

(defroute register-discord-cron-get-route!
  []
  "GET" "/api/admin/config/discord/cron"
  [session-guard]
  (ensure-permission! ctx "org.events.control")
  (json-response! reply 200 (:runtime (event-agents-control-response config))))

(defroute register-trigger-fire-route!
  []
  "POST" "/api/admin/triggers/:triggerId/fire"
  [session-guard]
  (try
    (ensure-permission! ctx "org.events.control")
    (let [trigger-id (or (aget request "params" "triggerId") "")]
      (if (str/blank? trigger-id)
        (json-response! reply 400 {:detail "triggerId is required"})
        (-> (trigger-runner/fire! trigger-id)
            (.then (fn [result]
                     (json-response! reply 202 {:ok true
                                                :triggerId trigger-id
                                                :result result})))
            (.catch (fn [err] (error-response! reply err))))))
    (catch :default err
      (error-response! reply err))))

;; ── MCP routes ──────────────────────────────────────────────────────────────────

(defroute register-mcp-status-route!
  []
  "GET" "/api/mcp/status"
  [optional-session-guard]
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (json-response! reply 200 (mcp/status)))

(defroute register-mcp-catalog-route!
  []
  "GET" "/api/mcp/catalog"
  [optional-session-guard]
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (json-response! reply 200 {:tools (mcp/catalog) :enabled (mcp/enabled?)}))

(defroute register-mcp-call-route!
  []
  "POST" "/api/mcp/call"
  [session-guard]
  (try
    (ensure-permission! ctx "agent.chat.use")
    (let [body    (or (aget request "body") (js/Object.))
          tool-id (str (or (aget body "toolId") ""))
          args    (js->clj (or (aget body "arguments") (js/Object.)) :keywordize-keys true)]
      (if (str/blank? tool-id)
        (json-response! reply 400 {:detail "toolId is required"})
        (-> (mcp/call-tool! tool-id args)
            (.then (fn [result] (json-response! reply 200 result)))
            (.catch (fn [err]
                      (json-response! reply 502 {:detail (str "MCP tool call failed: " (or (aget err "message") (str err)))})))))))
    (catch :default err
      (error-response! reply err)))

;; ── Top-level registration ────────────────────────────────────────────────────

(defn register-tool-routes!
  [app runtime config deps]
  (register-tool-catalog-route!          app runtime config deps)
  (register-email-send-route!            app runtime config deps)
  (register-websearch-route!             app runtime config deps)
  (register-read-route!                  app runtime config deps)
  (register-write-route!                 app runtime config deps)
  (register-edit-route!                  app runtime config deps)
  (register-bash-route!                  app runtime config deps)
  (register-discord-publish-route!       app runtime config deps)
  (register-discord-token-get-route!     app runtime config deps)
  (register-discord-token-put-route!     app runtime config deps)
  (register-event-agents-get-route!      app runtime config deps)
  (register-event-agents-put-route!      app runtime config deps)
  (register-event-agents-job-run-route!  app runtime config deps)
  (register-event-agents-dispatch-route! app runtime config deps)
  (register-event-agents-runtime-stop-route!  app runtime config deps)
  (register-event-agents-runtime-start-route! app runtime config deps)
  (register-event-agents-runtime-reset-route! app runtime config deps)
  (register-events-get-route!            app runtime config deps)
  (register-events-put-route!            app runtime config deps)
  (register-events-job-run-route!        app runtime config deps)
  (register-events-dispatch-route!       app runtime config deps)
  (register-events-runtime-stop-route!   app runtime config deps)
  (register-events-runtime-start-route!  app runtime config deps)
  (register-events-runtime-reset-route!  app runtime config deps)
  (register-discord-control-get-route!   app runtime config deps)
  (register-discord-control-put-route!   app runtime config deps)
  (register-discord-control-job-run-route! app runtime config deps)
  (register-discord-cron-get-route!      app runtime config deps)
  (register-trigger-fire-route!          app runtime config deps)
  (register-mcp-status-route!            app runtime config deps)
  (register-mcp-catalog-route!           app runtime config deps)
  (register-mcp-call-route!              app runtime config deps)
  nil)
