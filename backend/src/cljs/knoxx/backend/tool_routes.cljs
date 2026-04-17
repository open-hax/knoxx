(ns knoxx.backend.tool-routes
  (:require [clojure.string :as str]
            [knoxx.backend.discord-gateway :as dg]
            [knoxx.backend.event-agents :as event-agents]
            [knoxx.backend.http :as backend-http]
            [knoxx.backend.mcp-bridge :as mcp]
            [knoxx.backend.runtime-config :as runtime-config]))

(defn send-email!
  "Send an email via Gmail SMTP using nodemailer.
   Returns a promise that resolves with the result on success or rejects on failure."
  [runtime config to subject text-body cc bcc]
  (let [email (:gmail-app-email config)
        password (:gmail-app-password config)
        nodemailer (aget runtime "nodemailer")]
    (if (or (str/blank? email) (str/blank? password))
      (js/Promise.reject (js/Error. "Gmail credentials not configured"))
      (let [transporter (.createTransport nodemailer
                                           #js {:host "smtp.gmail.com"
                                                :port 587
                                                :secure false
                                                :auth #js {:user email
                                                           :pass password}})]
        (.sendMail transporter
                   #js {:from email
                        :to (str/join ", " to)
                        :cc (when (seq cc) (str/join ", " cc))
                        :bcc (when (seq bcc) (str/join ", " bcc))
                        :subject subject
                        :text text-body})))))

(defn- masked-discord-token
  [token]
  (if (and (string? token) (> (count token) 8))
    (str (subs token 0 4) "***" (subs token (- (count token) 4)))
    ""))

(defn- event-agents-control-response
  [config]
  (let [live-config (or @runtime-config/config* config)
        token (:discord-bot-token live-config)
        configured (not (str/blank? token))
        control (runtime-config/event-agent-control-config live-config)
        runtime (event-agents/status-snapshot live-config)]
    {:configured configured
     :tokenPreview (if configured (masked-discord-token token) "")
     :availableRoles (runtime-config/event-agent-role-options)
     :availableSourceKinds (runtime-config/event-agent-source-kind-options)
     :availableTriggerKinds (runtime-config/event-agent-trigger-kind-options)
     :control control
     :runtime runtime}))

(defn- restart-discord-gateway!
  [token]
  (when (dg/started?)
    (-> (dg/restart! token)
        (.catch (fn [_] nil)))))

(defn register-tool-routes!
  [app runtime config {:keys [route!
                              json-response!
                              error-response!
                              with-request-context!
                              ensure-permission!
                              tool-catalog
                              ensure-role-can-use!
                              resolve-workspace-path
                              count-occurrences
                              replace-first
                              clip-text]}]
  (route! app "GET" "/api/tools/catalog"
          (fn [request reply]
            (let [role (or (aget request "query" "role") (:knoxx-default-role config))]
              (with-request-context! runtime request reply
                (fn [ctx]
                  (when ctx
                    (ensure-permission! ctx "agent.chat.use"))
                  (json-response! reply 200 (tool-catalog config role ctx)))))))

  (route! app "POST" "/api/tools/email/send"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (try
                  (let [body (or (aget request "body") #js {})
                        role (ensure-role-can-use! ctx (or (aget body "role") (:knoxx-default-role config)) "email.send")
                        to (or (aget body "to") #js [])
                        cc (or (aget body "cc") #js [])
                        bcc (or (aget body "bcc") #js [])
                        subject (str (or (aget body "subject") "(no subject)"))
                        markdown (str (or (aget body "markdown") ""))]
                    (if (empty? to)
                      (json-response! reply 400 {:detail "Missing required field: to array"})
                      (-> (send-email! runtime config to subject markdown cc bcc)
                          (.then (fn [result]
                                   (json-response! reply 200 {:ok true
                                                              :role role
                                                              :message_id (aget result "messageId")})))
                          (.catch (fn [err]
                                    (json-response! reply 502 {:detail (str "Failed to send email: " (or (aget err "message") (str err)))}))))))
                  (catch :default err
                    (error-response! reply err)))))))

  (route! app "POST" "/api/tools/websearch"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (try
                  (let [body (or (aget request "body") #js {})
                        role (ensure-role-can-use! ctx (or (aget body "role") (:knoxx-default-role config)) "websearch")
                        query (str/trim (str (or (aget body "query") "")))
                        num-results (or (aget body "numResults") 8)
                        search-context-size (aget body "searchContextSize")
                        allowed-domains (or (aget body "allowedDomains") #js [])
                        model (aget body "model")]
                    (if (str/blank? query)
                      (json-response! reply 400 {:detail "query is required"})
                      (-> (backend-http/fetch-json (str (:proxx-base-url config) "/api/tools/websearch")
                                                   #js {:method "POST"
                                                        :headers (backend-http/bearer-headers (:proxx-auth-token config))
                                                        :body (.stringify js/JSON
                                                                          #js {:query query
                                                                               :numResults num-results
                                                                               :searchContextSize search-context-size
                                                                               :allowedDomains allowed-domains
                                                                               :model model})})
                          (.then (fn [resp]
                                   (if (aget resp "ok")
                                     (json-response! reply 200 (assoc (js->clj (aget resp "body") :keywordize-keys true) :role role))
                                     (json-response! reply (or (aget resp "status") 502)
                                                     {:detail (pr-str (js->clj (aget resp "body") :keywordize-keys true))}))))
                          (.catch (fn [err]
                                    (json-response! reply 502 {:detail (str err)}))))))
                  (catch :default err
                    (error-response! reply err)))))))

  (route! app "POST" "/api/tools/read"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (try
                  (let [body (or (aget request "body") #js {})
                        role (ensure-role-can-use! ctx (or (aget body "role") (:knoxx-default-role config)) "read")
                        node-fs (aget runtime "fs")
                        path-str (resolve-workspace-path runtime config (or (aget body "path") ""))
                        offset (max 1 (or (aget body "offset") 1))
                        limit (max 1 (or (aget body "limit") 400))]
                    (-> (.stat node-fs path-str)
                        (.then (fn [stat]
                                 (if (.isDirectory stat)
                                   (-> (.readdir node-fs path-str #js {:withFileTypes true})
                                       (.then (fn [entries]
                                                (let [content-lines (map (fn [entry]
                                                                           (str (aget entry "name")
                                                                                (when (.isDirectory entry) "/")))
                                                                         (array-seq entries))
                                                      [content truncated] (clip-text (str/join "\n" content-lines))]
                                                  (json-response! reply 200 {:ok true
                                                                             :role role
                                                                             :path path-str
                                                                             :content content
                                                                             :truncated truncated})))))
                                   (-> (.readFile node-fs path-str "utf8")
                                       (.then (fn [text]
                                                (let [lines (str/split-lines text)
                                                      start (dec offset)
                                                      stop (+ start limit)
                                                      numbered (map-indexed (fn [idx line]
                                                                              (str (+ start idx 1) ": " line))
                                                                            (take limit (drop start lines)))
                                                      [content clipped?] (clip-text (str/join "\n" numbered))]
                                                  (json-response! reply 200 {:ok true
                                                                             :role role
                                                                             :path path-str
                                                                             :content content
                                                                             :truncated (or clipped? (< stop (count lines)))}))))))))
                        (.catch (fn [err]
                                  (json-response! reply 404 {:detail (str err)})))))
                  (catch :default err
                    (error-response! reply err)))))))

  (route! app "POST" "/api/tools/write"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (try
                  (let [body (or (aget request "body") #js {})
                        role (ensure-role-can-use! ctx (or (aget body "role") (:knoxx-default-role config)) "write")
                        node-fs (aget runtime "fs")
                        node-path (aget runtime "path")
                        path-str (resolve-workspace-path runtime config (or (aget body "path") ""))
                        content (str (or (aget body "content") ""))
                        overwrite (not= false (aget body "overwrite"))
                        create-parents (not= false (aget body "create_parents"))
                        parent (.dirname node-path path-str)
                        check-promise (if overwrite
                                        (js/Promise.resolve nil)
                                        (-> (.stat node-fs path-str)
                                            (.then (fn [_]
                                                     (js/Promise.reject (js/Error. (str "File exists and overwrite is false: " path-str)))))
                                            (.catch (fn [_]
                                                      (js/Promise.resolve nil)))))]
                    (-> check-promise
                        (.then (fn []
                                 (if create-parents
                                   (.mkdir node-fs parent #js {:recursive true})
                                   (js/Promise.resolve nil))))
                        (.then (fn []
                                 (.writeFile node-fs path-str content "utf8")))
                        (.then (fn []
                                 (json-response! reply 200 {:ok true
                                                            :role role
                                                            :path path-str
                                                            :bytes_written (.-length (.from js/Buffer content "utf8"))})))
                        (.catch (fn [err]
                                  (json-response! reply 409 {:detail (str err)})))))
                  (catch :default err
                    (error-response! reply err)))))))

  (route! app "POST" "/api/tools/edit"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (try
                  (let [body (or (aget request "body") #js {})
                        role (ensure-role-can-use! ctx (or (aget body "role") (:knoxx-default-role config)) "edit")
                        node-fs (aget runtime "fs")
                        path-str (resolve-workspace-path runtime config (or (aget body "path") ""))
                        old-string (str (or (aget body "old_string") ""))
                        new-string (str (or (aget body "new_string") ""))
                        replace-all (true? (aget body "replace_all"))]
                    (-> (.readFile node-fs path-str "utf8")
                        (.then (fn [current]
                                 (if (= (.indexOf current old-string) -1)
                                   (js/Promise.reject (js/Error. "old_string not found in file"))
                                   (let [replacements (if replace-all
                                                        (count-occurrences current old-string)
                                                        1)
                                         updated (if replace-all
                                                   (str/replace current old-string new-string)
                                                   (replace-first current old-string new-string))]
                                     (-> (.writeFile node-fs path-str updated "utf8")
                                         (.then (fn []
                                                  (json-response! reply 200 {:ok true
                                                                             :role role
                                                                             :path path-str
                                                                             :replacements replacements}))))))))
                        (.catch (fn [err]
                                  (json-response! reply 409 {:detail (str err)})))))
                  (catch :default err
                    (error-response! reply err)))))))

  (route! app "POST" "/api/tools/bash"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (try
                  (let [body (or (aget request "body") #js {})
                        role (ensure-role-can-use! ctx (or (aget body "role") (:knoxx-default-role config)) "bash")
                        timeout-ms (min (max (or (aget body "timeout_ms") 120000) 1000) 300000)
                        workdir (if-let [raw-workdir (aget body "workdir")]
                                  (resolve-workspace-path runtime config raw-workdir)
                                  (.resolve (aget runtime "path") (:workspace-root config)))
                        exec-file-async (aget runtime "execFileAsync")]
                    (-> (exec-file-async "/bin/bash"
                                         #js ["-lc" (or (aget body "command") "")]
                                         #js {:cwd workdir
                                              :timeout timeout-ms
                                              :maxBuffer 1048576})
                        (.then (fn [result]
                                 (let [[stdout _] (clip-text (or (aget result "stdout") "") 24000)
                                       [stderr __] (clip-text (or (aget result "stderr") "") 12000)]
                                   (json-response! reply 200 {:ok true
                                                              :role role
                                                              :command (or (aget body "command") "")
                                                              :exit_code 0
                                                              :stdout stdout
                                                              :stderr stderr}))))
                        (.catch (fn [err]
                                  (if (and (aget err "killed") (not (number? (aget err "code"))))
                                    (json-response! reply 408 {:detail (str "Command timed out after " (/ timeout-ms 1000) "s")})
                                    (let [[stdout _] (clip-text (or (aget err "stdout") "") 24000)
                                          [stderr __] (clip-text (or (aget err "stderr") "") 12000)]
                                      (json-response! reply 200 {:ok false
                                                                 :role role
                                                                 :command (or (aget body "command") "")
                                                                 :exit_code (if (number? (aget err "code")) (aget err "code") 1)
                                                                 :stdout stdout
                                                                 :stderr stderr})))))))
                  (catch :default err
                    (error-response! reply err)))))))

  ;; Discord publish route
  (route! app "POST" "/api/tools/discord/publish"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (try
                  (let [body (or (aget request "body") #js {})
                        role (ensure-role-can-use! ctx (or (aget body "role") (:knoxx-default-role config)) "discord.publish")
                        channel-id (str/trim (str (or (aget body "channelId") "")))
                        content (str/trim (str (or (aget body "content") "")))
                        token (:discord-bot-token (or @runtime-config/config* config))
                        validation-error (cond
                                           (str/blank? token) "Discord bot token not configured. Set DISCORD_BOT_TOKEN env var or configure it in the admin panel."
                                           (str/blank? channel-id) "Missing required field: channelId"
                                           (str/blank? content) "Missing required field: content"
                                           :else nil)]
                    (if validation-error
                      (json-response! reply 400 {:detail validation-error})
                      (-> (js/fetch (str "https://discord.com/api/v10/channels/" channel-id "/messages")
                                   #js {:method "POST"
                                        :headers #js {"Authorization" (str "Bot " token)
                                                      "Content-Type" "application/json"}
                                        :body (.stringify js/JSON #js {:content content})})
                          (.then (fn [resp]
                                   (if (.-ok resp)
                                     (-> (.json resp)
                                         (.then (fn [result]
                                                  (json-response! reply 200 {:ok true
                                                                             :role role
                                                                             :channelId channel-id
                                                                             :messageId (aget result "id")}))))
                                     (-> (.text resp)
                                         (.then (fn [text]
                                                  (json-response! reply 502 {:detail (str "Discord API error: " text)})))))))
                          (.catch (fn [err]
                                    (json-response! reply 502 {:detail (str "Discord request failed: " (or (aget err "message") (str err)))}))))))
                  (catch :default err
                    (error-response! reply err)))))))

  ;; Discord bot token admin route
  (route! app "GET" "/api/admin/config/discord"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (ensure-permission! ctx "platform.org.read")
                (let [live-config (or @runtime-config/config* config)
                      token (:discord-bot-token live-config)
                      configured (not (str/blank? token))
                      masked (if configured (masked-discord-token token) "")]
                  (json-response! reply 200 {:configured configured
                                             :tokenPreview masked}))))))

  (route! app "PUT" "/api/admin/config/discord"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (try
                  (ensure-permission! ctx "platform.org.create")
                  (let [body (or (aget request "body") #js {})
                        new-token (str/trim (str (or (aget body "discordBotToken") "")))]
                    (if (str/blank? new-token)
                      (json-response! reply 400 {:detail "discordBotToken must not be blank"})
                      (do
                        ;; Update in-memory config + env var so the running server picks it up
                        (aset js/process.env "DISCORD_BOT_TOKEN" new-token)
                        (swap! runtime-config/config* (fn [current-cfg]
                                                       (assoc (or current-cfg (runtime-config/cfg))
                                                              :discord-bot-token new-token)))
                        (restart-discord-gateway! new-token)
                        (event-agents/reload!)
                        (let [masked (masked-discord-token new-token)]
                          (json-response! reply 200 {:ok true
                                                     :configured true
                                                     :tokenPreview masked})))))
                  (catch :default err
                    (error-response! reply err)))))))

  (route! app "GET" "/api/admin/config/event-agents"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (ensure-permission! ctx "platform.org.read")
                (json-response! reply 200 (event-agents-control-response config))))))

  (route! app "PUT" "/api/admin/config/event-agents"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (try
                  (ensure-permission! ctx "platform.org.create")
                  (let [body (js->clj (or (aget request "body") #js {}) :keywordize-keys true)
                        live-config (or @runtime-config/config* config)
                        next-control (runtime-config/event-agent-control-config
                                      (assoc live-config :event-agent-control body))]
                    (swap! runtime-config/config* (fn [current-cfg]
                                                   (assoc (or current-cfg (runtime-config/cfg))
                                                          :event-agent-control next-control)))
                    (event-agents/reload!)
                    (json-response! reply 200 (assoc (event-agents-control-response config) :ok true)))
                  (catch :default err
                    (error-response! reply err)))))))

  (route! app "POST" "/api/admin/config/event-agents/jobs/:jobId/run"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (try
                  (ensure-permission! ctx "platform.org.create")
                  (let [job-id (or (aget request "params" "jobId") "")]
                    (if (str/blank? job-id)
                      (json-response! reply 400 {:detail "jobId is required"})
                      (-> (event-agents/run-job! job-id)
                          (.then (fn [_]
                                   (json-response! reply 202 {:ok true
                                                              :jobId job-id})))
                          (.catch (fn [err]
                                    (error-response! reply err))))))
                  (catch :default err
                    (error-response! reply err)))))))

  (route! app "POST" "/api/admin/config/event-agents/events/dispatch"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (try
                  (ensure-permission! ctx "platform.org.create")
                  (let [body (js->clj (or (aget request "body") #js {}) :keywordize-keys true)]
                    (-> (event-agents/dispatch-event! body)
                        (.then (fn [result]
                                 (json-response! reply 202 {:ok true
                                                            :matchedJobs (:matchedJobs result)
                                                            :event (:event result)})))
                        (.catch (fn [err]
                                  (error-response! reply err)))))
                  (catch :default err
                    (error-response! reply err)))))))

  ;; Legacy aliases while the browser/proxy surface catches up.
  (route! app "GET" "/api/admin/config/discord/control"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (ensure-permission! ctx "platform.org.read")
                (json-response! reply 200 (event-agents-control-response config))))))

  (route! app "PUT" "/api/admin/config/discord/control"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (try
                  (ensure-permission! ctx "platform.org.create")
                  (let [body (js->clj (or (aget request "body") #js {}) :keywordize-keys true)
                        live-config (or @runtime-config/config* config)
                        next-control (runtime-config/event-agent-control-config
                                      (assoc live-config :event-agent-control body))]
                    (swap! runtime-config/config* (fn [current-cfg]
                                                   (assoc (or current-cfg (runtime-config/cfg))
                                                          :event-agent-control next-control)))
                    (event-agents/reload!)
                    (json-response! reply 200 (assoc (event-agents-control-response config) :ok true)))
                  (catch :default err
                    (error-response! reply err)))))))

  (route! app "POST" "/api/admin/config/discord/control/jobs/:jobId/run"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (try
                  (ensure-permission! ctx "platform.org.create")
                  (let [job-id (or (aget request "params" "jobId") "")]
                    (if (str/blank? job-id)
                      (json-response! reply 400 {:detail "jobId is required"})
                      (-> (event-agents/run-job! job-id)
                          (.then (fn [_]
                                   (json-response! reply 202 {:ok true
                                                              :jobId job-id})))
                          (.catch (fn [err]
                                    (error-response! reply err))))))
                  (catch :default err
                    (error-response! reply err)))))))

  ;; Discord cron status route
  (route! app "GET" "/api/admin/config/discord/cron"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (ensure-permission! ctx "platform.org.read")
                (json-response! reply 200 (:runtime (event-agents-control-response config)))))))

  ;; MCP (Model Context Protocol) routes
  (route! app "GET" "/api/mcp/status"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx
                  (ensure-permission! ctx "agent.chat.use"))
                (json-response! reply 200 (mcp/status))))))

  (route! app "GET" "/api/mcp/catalog"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx
                  (ensure-permission! ctx "agent.chat.use"))
                (json-response! reply 200 {:tools (mcp/catalog)
                                           :enabled (mcp/enabled?)})))))

  (route! app "POST" "/api/mcp/call"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (try
                  (when ctx
                    (ensure-permission! ctx "agent.chat.use"))
                  (let [body (or (aget request "body") #js {})
                        tool-id (str (or (aget body "toolId") ""))
                        args (js->clj (or (aget body "arguments") #js {}) :keywordize-keys true)]
                    (if (str/blank? tool-id)
                      (json-response! reply 400 {:detail "toolId is required"})
                      (-> (mcp/call-tool! tool-id args)
                          (.then (fn [result]
                                   (json-response! reply 200 result)))
                          (.catch (fn [err]
                                    (json-response! reply 502 {:detail (str "MCP tool call failed: " (or (aget err "message") (str err)))}))))))
                  (catch :default err
                    (error-response! reply err)))))))

  nil)
