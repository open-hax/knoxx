(ns knoxx.backend.infra.routes.tools
  (:require [clojure.string :as str]
            [knoxx.backend.domain.event.dispatch :as event-dispatch]
            [knoxx.backend.infra.clients.proxx :as proxx-client]
            [knoxx.backend.infra.http :as backend-http]
            [knoxx.backend.macros :refer-macros [defroute]]
            [knoxx.backend.domain.mcp.mcp-bridge :as mcp]
            [knoxx.backend.runtime.state :as runtime-state]
            [knoxx.backend.domain.text :refer [sanitize-svg-content]]
            [knoxx.backend.infra.control-config :as control-config]
            [knoxx.backend.infra.event-runtime :as event-runtime]
            [knoxx.backend.infra.auth.authz :as authz]
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

(defn- events-control-response [config]
  (let [live-config (or @runtime-state/config* config)
        control     (control-config/event-control-config live-config)
        runtime     (event-dispatch/status-snapshot live-config)]
    {:configured true
     :availableRoles (control-config/event-role-options live-config)
     :availableGeneratorKinds (control-config/event-generator-kind-options live-config)
     :availableTriggerKinds (control-config/event-trigger-kind-options)
     :control control
     :runtime runtime}))

(defn- failed-trigger-results
  [result]
  (->> (:results result)
       (filter :failed)
       vec))

(defn operator-dispatch-event
  "Bind a synthetic event to its authenticated actor.

  Canonical `:event/actor` wins normalization; removing aliases makes the
  boundary explicit and prevents a later precedence change from restoring a
  caller-supplied emitter identity."
  [ctx body]
  (-> body
      (dissoc :actorId :actor-id)
      (assoc :event/actor (authz/ctx-actor-id ctx))))

(defn- trigger-fire-response!
  [reply trigger-id result]
  (let [failures (failed-trigger-results result)
        failed? (seq failures)]
    (backend-http/json-response! reply (if failed? 500 202)
                                 (cond-> {:ok (not failed?)
                                          :triggerId trigger-id
                                          :matchedTriggers (:matchedTriggers result)
                                          :event (:event result)
                                          :results (:results result)}
                                   failed? (assoc :detail "One or more trigger actions failed"
                                                  :failures failures)))))

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
        (try
          (let [result (await (send-email! runtime config to subject markdown cc bcc))]
            (json-response! reply 200 {:ok true :role role
                                       :message_id (aget result "messageId")}))
          (catch :default err
            (json-response! reply 502 {:detail (str "Failed to send email: " (or (aget err "message") (str err)))})))))
    (catch :default err
      (error-response! reply err))))

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
        (try
          (let [resp (await (proxx-client/websearch! (proxx-client/client config)
                                                     {:query query
                                                      :numResults num-results
                                                      :searchContextSize search-context-size
                                                      :allowedDomains allowed-domains
                                                      :model model}))]
            (if (:ok resp)
              (json-response! reply 200 (assoc (:body resp) :role role))
              (json-response! reply (or (:status resp) 502)
                              {:detail (pr-str (:body resp))})))
          (catch :default err
            (json-response! reply 502 {:detail (str err)})))))
    (catch :default err
      (error-response! reply err))))

(defn- ^:async read-directory-response!
  [json-response! reply role path-str clip-text]
  (let [entries (await (.readdir fs path-str (clj->js {:withFileTypes true})))
        content-lines (map (fn [e] (str (aget e "name") (when (.isDirectory e) "/")))
                           (array-seq entries))
        [content truncated] (clip-text (str/join "\n" content-lines))]
    (json-response! reply 200 {:ok true :role role :path path-str
                               :content content :truncated truncated})))

(defn- ^:async read-file-response!
  [json-response! reply role path-str offset limit clip-text]
  (let [text (await (.readFile fs path-str "utf8"))
        lines (str/split-lines text)
        start (dec offset)
        stop (+ start limit)
        numbered (map-indexed (fn [idx line] (str (+ start idx 1) ": " line))
                              (take limit (drop start lines)))
        [content clipped?] (clip-text (str/join "\n" numbered))]
    (json-response! reply 200 {:ok true :role role :path path-str
                               :content content
                               :truncated (or clipped? (< stop (count lines)))})))

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
      (try
        (let [stat (await (.stat fs path-str))]
          (if (.isDirectory stat)
            (read-directory-response! json-response! reply role path-str clip-text)
            (read-file-response! json-response! reply role path-str offset limit clip-text)))
        (catch :default err
          (json-response! reply 404 {:detail (str err)}))))
    (catch :default err
      (error-response! reply err))))

(defn- write-body-params
  [body resolve-workspace-path runtime config]
  (let [path-str (resolve-workspace-path runtime config (or (aget body "path") ""))
        raw-content (str (or (aget body "content") ""))
        content (if (re-find #"(?i)\.svg$" path-str)
                  (sanitize-svg-content raw-content)
                  raw-content)]
    {:path-str path-str
     :content content
     :overwrite (not= false (aget body "overwrite"))
     :create-parents (not= false (aget body "create_parents"))
     :parent (.dirname path path-str)}))

(defroute register-write-route!
  [ensure-role-can-use! resolve-workspace-path]
  "POST" "/api/tools/write"
  [session-guard]
  (try
    (let [body    (or (aget request "body") (js/Object.))
          agent-contract-id (or (aget body "agentContractId") (aget body "agent_contract_id"))
          role    (ensure-role-can-use! ctx (or (aget body "role") (:knoxx-default-role config)) "write" agent-contract-id)
          {:keys [path-str content overwrite create-parents parent]} (write-body-params body resolve-workspace-path runtime config)]
      (try
        (when-not overwrite
          (try
            (await (.stat fs path-str))
            (json-response! reply 409 {:detail (str "File exists and overwrite is false: " path-str)})
            (catch :default _
              nil)))
        (when create-parents
          (await (.mkdir fs parent (clj->js {:recursive true}))))
        (await (.writeFile fs path-str content "utf8"))
        (json-response! reply 200 {:ok true :role role :path path-str
                                   :bytes_written (.-length (.from js/Buffer content "utf8"))})
        (catch :default err
          (json-response! reply 409 {:detail (str err)}))))
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
      (try
        (let [current (await (.readFile fs path-str "utf8"))]
          (if (= (.indexOf current old-string) -1)
            (json-response! reply 409 {:detail "old_string not found in file"})
            (let [replacements (if replace-all (count-occurrences current old-string) 1)
                  updated      (if replace-all
                                 (str/replace current old-string new-string)
                                 (replace-first current old-string new-string))]
              (await (.writeFile fs path-str updated "utf8"))
              (json-response! reply 200 {:ok true :role role :path path-str
                                         :replacements replacements}))))
        (catch :default err
          (json-response! reply 409 {:detail (str err)}))))
    (catch :default err
      (error-response! reply err))))

(defn- bash-success-response!
  [json-response! reply role body result clip-text]
  (let [[stdout _]  (clip-text (or (aget result "stdout") "") 24000)
        [stderr __] (clip-text (or (aget result "stderr") "") 12000)]
    (json-response! reply 200 {:ok true :role role
                               :command  (or (aget body "command") "")
                               :exit_code 0
                               :stdout stdout :stderr stderr})))

(defn- bash-error-response!
  [json-response! reply role body timeout-ms err clip-text]
  (if (and (aget err "killed") (not (number? (aget err "code"))))
    (json-response! reply 408 {:detail (str "Command timed out after " (/ timeout-ms 1000) "s")})
    (let [[stdout _]  (clip-text (or (aget err "stdout") "") 24000)
          [stderr __] (clip-text (or (aget err "stderr") "") 12000)]
      (json-response! reply 200 {:ok false :role role
                                 :command   (or (aget body "command") "")
                                 :exit_code (if (number? (aget err "code")) (aget err "code") 1)
                                 :stdout stdout :stderr stderr}))))

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
      (try
        (let [result (await (exec-file-async "/bin/bash"
                                              (clj->js ["-lc" (or (aget body "command") "")])
                                              (clj->js {:cwd workdir
                                                        :timeout timeout-ms
                                                        :killSignal "SIGKILL"
                                                        :maxBuffer 1048576})))]
          (bash-success-response! json-response! reply role body result clip-text))
        (catch :default err
          (bash-error-response! json-response! reply role body timeout-ms err clip-text))))
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

;; Events aliases — preferred vocabulary going forward.

(defroute register-events-get-route!
  []
  "GET" "/api/admin/config/events"
  [session-guard]
  (ensure-permission! ctx "org.events.control")
  (json-response! reply 200 (events-control-response config)))

(defroute register-events-put-route!
  []
  "PUT" "/api/admin/config/events"
  [session-guard]
  (try
    (ensure-permission! ctx "org.events.control")
    (let [body        (js->clj (or (aget request "body") (js/Object.)) :keywordize-keys true)
          live-config (or @runtime-state/config* config)
          next-control (control-config/event-control-config
                        (assoc live-config :event-control body))]
      (swap! runtime-state/config* (fn [c] (assoc (or c config) :event-control next-control)))
      (control-config/persist-event-control! next-control)
      ;; `:ok true` refers to persisting the control config, which did happen.
      ;; The reload is a side effect and can be refused on a flagged process, so
      ;; its outcome is reported rather than absorbed — otherwise the response
      ;; implies the runtime picked up the change when it did not.
      (let [reload (await (event-runtime/reload! live-config))]
        (json-response! reply 200 (assoc (events-control-response config)
                                         :ok true
                                         :reload reload))))
    (catch :default err
      (error-response! reply err))))

(defroute register-events-trigger-fire-route!
  []
  "POST" "/api/admin/config/events/triggers/:triggerId/fire"
  [session-guard]
  (try
    (ensure-permission! ctx "org.events.control")
    (let [trigger-id (or (aget request "params" "triggerId") "")]
      (if (str/blank? trigger-id)
        (json-response! reply 400 {:detail "triggerId is required"})
        (try
          (let [result (await (event-runtime/fire-trigger-external! config trigger-id))]
            (trigger-fire-response! reply trigger-id result))
          (catch :default err
            (error-response! reply err)))))
    (catch :default err
      (error-response! reply err))))

(defroute register-events-dispatch-route!
  []
  "POST" "/api/admin/config/events/dispatch"
  [session-guard]
  (try
    (ensure-permission! ctx "org.events.control")
    (let [body (js->clj (or (aget request "body") (js/Object.)) :keywordize-keys true)]
      (try
        (let [result (await (event-dispatch/dispatch-external!
                             config (operator-dispatch-event ctx body)))
              failures (failed-trigger-results result)
              failed? (seq failures)]
          (json-response! reply (if failed? 500 202)
                          (cond-> {:ok (not failed?)
                                   :matchedTriggers (:matchedTriggers result)
                                   :event (:event result)
                                   :results (:results result)}
                            failed? (assoc :detail "One or more trigger actions failed"
                                           :failures failures))))
        (catch :default err
          (error-response! reply err))))
    (catch :default err
      (error-response! reply err))))

(defroute register-events-runtime-stop-route!
  []
  "POST" "/api/admin/config/events/runtime/stop"
  [session-guard]
  (ensure-permission! ctx "org.events.control")
  (event-runtime/stop!)
  (json-response! reply 200 (assoc (events-control-response config) :ok true :action "stopped")))

(defroute register-events-runtime-start-route!
  []
  "POST" "/api/admin/config/events/runtime/start"
  [session-guard]
  (ensure-permission! ctx "org.events.control")
  ;; `start!` refuses when the process is flagged, so reporting ok/started
  ;; unconditionally would tell an operator the runtime came up when nothing
  ;; did. Answer 409 instead: the request is well-formed, the process state
  ;; forbids it.
  (if (= :disabled (event-runtime/start! config))
    (json-response! reply 409
                    (assoc (events-control-response config)
                           :ok false
                           :action "refused"
                           :reason "KNOXX_DISABLE_EVENT_RUNTIMES is set on this process"))
    (json-response! reply 200 (assoc (events-control-response config) :ok true :action "started"))))

(defroute register-events-runtime-reset-route!
  []
  "POST" "/api/admin/config/events/runtime/reset"
  [session-guard]
  (try
    (ensure-permission! ctx "org.events.control")
    (try
      ;; Same contract as the start route: a disabled process must not report a
      ;; successful reset. `reload!` declines and says so; this surfaces it
      ;; rather than merging :ok true over the top of a refusal.
      (let [summary (await (event-runtime/reset-runtime! config))]
        (if (= :disabled (:status summary))
          (json-response! reply 409
                          (merge (events-control-response config)
                                 {:ok false
                                  :action "refused"
                                  :reason "KNOXX_DISABLE_EVENT_RUNTIMES is set on this process"
                                  :reset summary}))
          (json-response! reply 200
                          (merge (events-control-response config)
                                 {:ok true
                                  :action "reset"
                                  :reset summary}))))
      (catch :default err
        (error-response! reply err)))
    (catch :default err
      (error-response! reply err))))

;; Legacy aliases

(defroute register-trigger-fire-route!
  []
  "POST" "/api/admin/triggers/:triggerId/fire"
  [session-guard]
  (try
    (ensure-permission! ctx "org.events.control")
    (let [trigger-id (or (aget request "params" "triggerId") "")]
      (if (str/blank? trigger-id)
        (json-response! reply 400 {:detail "triggerId is required"})
        (try
          (let [result (await (event-runtime/fire-trigger-external!
                               config trigger-id))]
            (trigger-fire-response! reply trigger-id result))
          (catch :default err
            (error-response! reply err)))))
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
        (try
          (let [result (await (mcp/call-tool! tool-id args))]
            (json-response! reply 200 result))
          (catch :default err
            (json-response! reply 502 {:detail (str "MCP tool call failed: " (or (aget err "message") (str err)))})))))
    (catch :default err
      (error-response! reply err))))

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
  (register-events-get-route!            app runtime config deps)
  (register-events-put-route!            app runtime config deps)
  (register-events-trigger-fire-route!        app runtime config deps)
  (register-events-dispatch-route!       app runtime config deps)
  (register-events-runtime-stop-route!   app runtime config deps)
  (register-events-runtime-start-route!  app runtime config deps)
  (register-events-runtime-reset-route!  app runtime config deps)
  (register-trigger-fire-route!          app runtime config deps)
  (register-mcp-status-route!            app runtime config deps)
  (register-mcp-catalog-route!           app runtime config deps)
  (register-mcp-call-route!              app runtime config deps)
  nil)
