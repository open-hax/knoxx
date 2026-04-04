(ns knoxx.backend.core
  (:require [clojure.string :as str]))

(defonce server* (atom nil))
(defonce sdk-runtime* (atom nil))
(defonce agent-sessions* (atom {}))
(defonce conversation-access* (atom {}))
(defonce ws-clients* (atom {}))
(defonce ws-stats-interval* (atom nil))
(defonce lounge-messages* (atom []))
(defonce runs* (atom {}))
(defonce run-order* (atom []))
(defonce settings-state* (atom nil))
(defonce database-state* (atom nil))
(defonce retrieval-stats* (atom {:samples []
                                 :avgRetrievalMs 0
                                 :p95RetrievalMs 0
                                 :recentSamples 0
                                 :modeCounts {:dense 0 :hybrid 0 :hybrid_rerank 0}}))

(def role-tools
  {"system_admin" [["read" "Read" "Read files and retrieved context"]
                    ["write" "Write" "Create new markdown drafts and artifacts"]
                    ["edit" "Edit" "Revise existing documents and drafts"]
                    ["bash" "Shell" "Run controlled shell commands"]
                    ["canvas" "Canvas" "Open long-form markdown drafting canvas"]
                    ["email.send" "Email" "Send drafts through configured email account"]
                    ["discord.publish" "Discord" "Publish updates to Discord"]
                    ["bluesky.publish" "Bluesky" "Publish updates to Bluesky"]]
   "org_admin" [["read" "Read" "Read files and retrieved context"]
                 ["write" "Write" "Create new markdown drafts and artifacts"]
                 ["edit" "Edit" "Revise existing documents and drafts"]
                 ["bash" "Shell" "Run controlled shell commands"]
                 ["canvas" "Canvas" "Open long-form markdown drafting canvas"]
                 ["email.send" "Email" "Send drafts through configured email account"]
                 ["discord.publish" "Discord" "Publish updates to Discord"]
                 ["bluesky.publish" "Bluesky" "Publish updates to Bluesky"]]
   "knowledge_worker" [["read" "Read" "Read files and retrieved context"]
                        ["canvas" "Canvas" "Open long-form markdown drafting canvas"]]
   "data_analyst" [["read" "Read" "Read files and retrieved context"]
                    ["write" "Write" "Create new markdown drafts and artifacts"]
                    ["edit" "Edit" "Revise existing documents and drafts"]
                    ["canvas" "Canvas" "Open long-form markdown drafting canvas"]]
   "developer" [["read" "Read" "Read files and retrieved context"]
                 ["write" "Write" "Create new markdown drafts and artifacts"]
                 ["edit" "Edit" "Revise existing documents and drafts"]
                 ["bash" "Shell" "Run controlled shell commands"]
                 ["canvas" "Canvas" "Open long-form markdown drafting canvas"]]
   ;; compatibility aliases while older frontend/default-role values still exist
   "executive" [["read" "Read" "Read files and retrieved context"]
                 ["canvas" "Canvas" "Open long-form markdown drafting canvas"]]
   "principal_architect" [["read" "Read" "Read files and retrieved context"]
                          ["write" "Write" "Create new markdown drafts and artifacts"]
                          ["edit" "Edit" "Revise existing documents and drafts"]
                          ["bash" "Shell" "Run controlled shell commands"]
                          ["canvas" "Canvas" "Open long-form markdown drafting canvas"]]
   "junior_dev" [["read" "Read" "Read files and retrieved context"]
                 ["write" "Write" "Create new markdown drafts and notes"]
                 ["canvas" "Canvas" "Open long-form markdown drafting canvas"]]})

(def role-aliases
  {"executive" "knowledge_worker"
   "principal_architect" "developer"
   "junior_dev" "knowledge_worker"})

(defn env
  [k default]
  (or (aget js/process.env k) default))

(defn cfg []
  {:app-name (env "APP_NAME" "Knoxx Backend CLJS")
   :host (env "HOST" "0.0.0.0")
   :port (js/parseInt (env "PORT" "8000") 10)
   :workspace-root (env "WORKSPACE_ROOT" "/app/workspace/devel")
   :project-name (env "WORKSPACE_PROJECT_NAME" "devel")
   :collection-name (env "KNOXX_COLLECTION_NAME" "devel_docs")
   :proxx-base-url (env "PROXX_BASE_URL" "http://proxx:8789")
   :proxx-auth-token (env "PROXX_AUTH_TOKEN" "")
   :proxx-default-model (env "PROXX_DEFAULT_MODEL" "glm-5")
   :proxx-embed-model (env "PROXX_EMBED_MODEL" "nomic-embed-text:latest")
   :openplanner-base-url (or (aget js/process.env "OPENPLANNER_BASE_URL")
                             (aget js/process.env "OPENPLANNER_URL")
                             "http://host.docker.internal:7777")
   :openplanner-api-key (env "OPENPLANNER_API_KEY" "")
   :model-lab-openai-api-key (env "MODEL_LAB_OPENAI_API_KEY" "")
   :knoxx-admin-url (env "KNOXX_ADMIN_URL" "http://localhost")
   :knoxx-base-url (env "KNOXX_BASE_URL" "http://localhost:8000")
   :knoxx-api-key (env "KNOXX_API_KEY" "")
   :shibboleth-base-url (env "SHIBBOLETH_BASE_URL" "")
   :shibboleth-ui-url (env "SHIBBOLETH_UI_URL" "")
   :knoxx-default-role (env "KNOXX_DEFAULT_ROLE" "executive")
   :gmail-app-email (env "GMAIL_APP_EMAIL" "")
   :gmail-app-password (env "GMAIL_APP_PASSWORD" "")
   :agent-dir (env "KNOXX_AGENT_DIR" "/tmp/knoxx-agent")
   :agent-system-prompt (env "KNOXX_AGENT_SYSTEM_PROMPT"
                             "You are Knoxx, the grounded workspace assistant for the devel corpus. Preserve multi-turn context within the active conversation, use workspace tools when needed, cite file paths when they matter, and prefer grounded synthesis over shallow enumeration. Treat passive semantic hydration as helpful but incomplete; when corpus grounding matters, use semantic_query and semantic_read instead of guessing. Long-term conversational memory lives in OpenPlanner; when the user asks about previous sessions, prior decisions, or your own earlier actions, use memory_search and memory_session instead of pretending to remember.")})

(defn now-iso []
  (.toISOString (js/Date.)))

(defn json-response!
  [reply status body]
  (-> (.code reply status)
      (.type "application/json")
      (.send (clj->js body))))

(defn request-hostname
  [request]
  (let [forwarded (some-> (aget request "headers" "x-forwarded-host") (str/split #",") first str/trim)
        raw-host (or forwarded (aget request "headers" "host") "")]
    (if (str/blank? raw-host)
      (or (aget request "hostname") "localhost")
      (-> raw-host
          (str/replace #":.*$" "")))))

(defn request-scheme
  [request]
  (let [forwarded (some-> (aget request "headers" "x-forwarded-proto") (str/split #",") first str/trim)]
    (if (str/blank? forwarded) "http" forwarded)))

(defn rewrite-localhost-url
  [url request]
  (try
    (let [parsed (js/URL. url)
          host (.-hostname parsed)]
      (if (contains? #{"localhost" "127.0.0.1" "::1"} host)
        (let [req-host (request-hostname request)
              scheme (request-scheme request)]
          (set! (.-protocol parsed) (str scheme ":"))
          (set! (.-hostname parsed) req-host)
          (.toString parsed))
        url))
    (catch :default _
      url)))

(defn with-query-param
  [url key value]
  (try
    (let [parsed (js/URL. url)]
      (.set (.-searchParams parsed) key value)
      (.toString parsed))
    (catch :default _
      url)))

(defn bearer-headers
  [token]
  (let [headers #js {"Content-Type" "application/json"}]
    (when-not (str/blank? token)
      (aset headers "Authorization" (str "Bearer " token)))
    headers))

(defn openai-auth-error
  [reply status-code message code]
  (json-response! reply status-code {:error {:message message
                                             :type "invalid_request_error"
                                             :param nil
                                             :code code}}))

(defn require-openai-key!
  [config request reply]
  (let [expected (:model-lab-openai-api-key config)
        auth-header (str (or (aget request "headers" "authorization") ""))]
    (cond
      (str/blank? expected)
      (do (openai-auth-error reply 503 "MODEL_LAB_OPENAI_API_KEY is not configured" "service_unavailable") false)

      (not (str/starts-with? (str/lower-case auth-header) "bearer "))
      (do (openai-auth-error reply 401 "Invalid API key" "invalid_api_key") false)

      (not= (subs auth-header 7) expected)
      (do (openai-auth-error reply 401 "Invalid API key" "invalid_api_key") false)

      :else true)))

(defn fetch-json
  [url opts]
  (-> (js/fetch url opts)
      (.then (fn [resp]
               (-> (.text resp)
                   (.then (fn [text]
                            (let [body (if (str/blank? text)
                                         #js {}
                                         (try
                                           (.parse js/JSON text)
                                           (catch :default _ #js {:raw text})))]
                              #js {:ok (.-ok resp)
                                   :status (.-status resp)
                                   :body body
                                   :headers resp.headers}))))))))

(defn trim-trailing-slashes
  [s]
  (str/replace (str (or s "")) #"/+$" ""))

(defn openplanner-enabled?
  [config]
  (and (not (str/blank? (:openplanner-base-url config)))
       (not (str/blank? (:openplanner-api-key config)))))

(defn openplanner-url
  [config suffix]
  (str (trim-trailing-slashes (:openplanner-base-url config)) suffix))

(defn openplanner-headers
  [config]
  #js {"Content-Type" "application/json"
       "Authorization" (str "Bearer " (:openplanner-api-key config))})

(defn openplanner-request!
  ([config method suffix] (openplanner-request! config method suffix nil))
  ([config method suffix body]
   (if-not (openplanner-enabled? config)
     (js/Promise.reject (js/Error. "OpenPlanner is not configured"))
     (let [opts #js {:method method
                     :headers (openplanner-headers config)}]
       (when body
         (aset opts "body" (.stringify js/JSON (clj->js body))))
       (-> (fetch-json (openplanner-url config suffix) opts)
           (.then (fn [resp]
                    (if (aget resp "ok")
                      (js->clj (aget resp "body") :keywordize-keys true)
                      (throw (js/Error. (str "OpenPlanner request failed ("
                                             (aget resp "status")
                                             "): "
                                             (pr-str (js->clj (aget resp "body") :keywordize-keys true)))))))))))))

(defn policy-db
  [runtime]
  (aget runtime "policyDb"))

(defn policy-db-enabled?
  [runtime]
  (some? (policy-db runtime)))

(defn policy-db-promise
  [runtime reply status promise]
  (if-not (policy-db-enabled? runtime)
    (json-response! reply 503 {:detail "Knoxx policy database is not configured"})
    (-> promise
        (.then (fn [result]
                 (json-response! reply status (js->clj result :keywordize-keys true))))
        (.catch (fn [err]
                  (error-response! reply err))))))

(defn http-error
  [status code message]
  (doto (js/Error. message)
    (aset "statusCode" status)
    (aset "code" code)))

(defn error-status
  [err default-status]
  (or (aget err "statusCode")
      (aget err "status")
      default-status))

(defn error-message
  [err]
  (or (aget err "message") (str err)))

(defn error-response!
  ([reply err] (error-response! reply err 500))
  ([reply err default-status]
   (json-response! reply (error-status err default-status)
                   (cond-> {:detail (error-message err)}
                     (aget err "code") (assoc :error_code (aget err "code"))))))

(defn resolve-request-context!
  [runtime request]
  (if-not (policy-db-enabled? runtime)
    (js/Promise.resolve nil)
    (if-let [cached (aget request "__knoxxRequestContext")]
      (js/Promise.resolve cached)
      (-> (.resolveRequestContext (policy-db runtime) (or (aget request "headers") #js {}))
          (.then (fn [ctx]
                   (let [clj-ctx (js->clj ctx :keywordize-keys true)]
                     (aset request "__knoxxRequestContext" clj-ctx)
                     clj-ctx)))))))

(defn with-request-context!
  [runtime request reply f]
  (if-not (policy-db-enabled? runtime)
    (f nil)
    (-> (resolve-request-context! runtime request)
        (.then f)
        (.catch (fn [err]
                  (error-response! reply err))))))

(defn ctx-org-id [ctx] (or (:orgId ctx) (get-in ctx [:org :id])))
(defn ctx-org-slug [ctx] (or (:orgSlug ctx) (get-in ctx [:org :slug])))
(defn ctx-user-id [ctx] (or (:userId ctx) (get-in ctx [:user :id])))
(defn ctx-user-email [ctx] (or (:userEmail ctx) (get-in ctx [:user :email])))
(defn ctx-membership-id [ctx] (or (:membershipId ctx) (get-in ctx [:membership :id])))

(defn ctx-role-slugs
  [ctx]
  (into #{}
        (or (:roleSlugs ctx)
            (keep :slug (:roles ctx)))))

(defn primary-context-role
  [ctx]
  (or (first (:roleSlugs ctx))
      (some->> (:roles ctx) (map :slug) first)
      "knowledge_worker"))

(defn system-admin?
  [ctx]
  (contains? (ctx-role-slugs ctx) "system_admin"))

(defn ctx-permissions
  [ctx]
  (into #{} (or (:permissions ctx) [])))

(defn ctx-permitted?
  [ctx permission]
  (contains? (ctx-permissions ctx) permission))

(defn ctx-any-permission?
  [ctx permissions]
  (boolean (some #(ctx-permitted? ctx %) permissions)))

(defn ctx-tool-effect
  [ctx tool-id]
  (some (fn [policy]
          (when (= (str (:toolId policy)) (str tool-id))
            (:effect policy)))
        (:toolPolicies ctx)))

(defn ctx-tool-allowed?
  [ctx tool-id]
  (= "allow" (ctx-tool-effect ctx tool-id)))

(defn ensure-permission!
  [ctx permission]
  (when-not (or (system-admin? ctx)
                (ctx-permitted? ctx permission))
    (throw (http-error 403 "permission_denied" (str "Permission '" permission "' is required"))))
  ctx)

(defn ensure-any-permission!
  [ctx permissions code message]
  (when-not (or (system-admin? ctx)
                (ctx-any-permission? ctx permissions))
    (throw (http-error 403 code message)))
  ctx)

(defn ensure-org-scope!
  [ctx org-id permission]
  (ensure-permission! ctx permission)
  (when-not (or (system-admin? ctx)
                (= (str (ctx-org-id ctx)) (str org-id)))
    (throw (http-error 403 "org_scope_denied" "Requested org is outside the current Knoxx scope")))
  ctx)

(defn record-org-id [record] (or (:org_id record) (:orgId record)))
(defn record-user-id [record] (or (:user_id record) (:userId record)))
(defn record-user-email [record] (or (:user_email record) (:userEmail record)))
(defn record-membership-id [record] (or (:membership_id record) (:membershipId record)))

(defn principal-match?
  [ctx record]
  (let [ctx-membership (str (or (ctx-membership-id ctx) ""))
        record-membership (str (or (record-membership-id record) ""))
        ctx-user (str (or (ctx-user-id ctx) ""))
        record-user (str (or (record-user-id record) ""))
        ctx-email (str/lower-case (str (or (ctx-user-email ctx) "")))
        record-email (str/lower-case (str (or (record-user-email record) "")))]
    (cond
      (system-admin? ctx) true
      (and (not (str/blank? ctx-membership))
           (not (str/blank? record-membership)))
      (= ctx-membership record-membership)
      (and (not (str/blank? ctx-user))
           (not (str/blank? record-user)))
      (= ctx-user record-user)
      :else
      (and (not (str/blank? ctx-email))
           (= ctx-email record-email)))))

(defn auth-snapshot
  [ctx]
  {:org_id (ctx-org-id ctx)
   :org_slug (ctx-org-slug ctx)
   :user_id (ctx-user-id ctx)
   :user_email (ctx-user-email ctx)
   :membership_id (ctx-membership-id ctx)
   :role_slugs (vec (or (:roleSlugs ctx) []))})

(defn ensure-conversation-access!
  [ctx conversation-id]
  (when (and ctx (not (str/blank? (str conversation-id))))
    (when-let [existing (get @conversation-access* conversation-id)]
      (when-not (principal-match? ctx existing)
        (throw (http-error 403 "conversation_scope_denied" "Conversation belongs to another Knoxx user")))))
  ctx)

(defn remember-conversation-access!
  [ctx conversation-id]
  (when (and ctx (not (str/blank? (str conversation-id))))
    (ensure-conversation-access! ctx conversation-id)
    (swap! conversation-access* assoc conversation-id (auth-snapshot ctx))))

(defn run-visible?
  [ctx run]
  (cond
    (nil? ctx) true
    (system-admin? ctx) true
    (ctx-permitted? ctx "agent.runs.read_all") true
    (and (= (str (ctx-org-id ctx)) (str (record-org-id run)))
         (ctx-permitted? ctx "agent.runs.read_org")) true
    (and (ctx-permitted? ctx "agent.runs.read_own")
         (principal-match? ctx run)) true
    :else false))

(defn parse-json-object
  [value]
  (cond
    (map? value) value
    (string? value) (try
                      (js->clj (.parse js/JSON value) :keywordize-keys true)
                      (catch :default _ nil))
    :else nil))

(defn row-extra-map
  [row]
  (or (parse-json-object (:extra row)) {}))

(defn session-visible?
  [ctx rows]
  (cond
    (nil? ctx) true
    (system-admin? ctx) true
    :else
    (let [extras (map row-extra-map rows)
          org-ids (into #{} (keep #(some-> % :org_id str not-empty)) extras)
          membership-ids (into #{} (keep #(some-> % :membership_id str not-empty)) extras)
          user-ids (into #{} (keep #(some-> % :user_id str not-empty)) extras)
          same-org? (contains? org-ids (str (ctx-org-id ctx)))]
      (cond
        (empty? org-ids) false
        (not same-org?) false
        (ctx-permitted? ctx "agent.memory.cross_session") true
        :else (or (contains? membership-ids (str (ctx-membership-id ctx)))
                  (contains? user-ids (str (ctx-user-id ctx))))))))

(defn fetch-openplanner-session-rows!
  [config session-id]
  (-> (openplanner-request! config "GET" (str "/v1/sessions/" (js/encodeURIComponent (str session-id))))
      (.then (fn [body]
               (vec (or (:rows body) []))))))

(defn authorized-session-ids!
  [config ctx session-ids]
  (let [session-ids (->> session-ids
                         (map str)
                         (remove str/blank?)
                         distinct
                         vec)]
    (if (or (nil? ctx) (system-admin? ctx) (empty? session-ids))
      (js/Promise.resolve (set session-ids))
      (-> (js/Promise.all
           (clj->js
            (map (fn [session-id]
                   (-> (fetch-openplanner-session-rows! config session-id)
                       (.then (fn [rows]
                                {:session session-id
                                 :allowed (session-visible? ctx rows)}))
                       (.catch (fn [_]
                                 {:session session-id
                                  :allowed false}))))
                 session-ids)))
          (.then (fn [results]
                   (->> (js-array-seq results)
                        (filter :allowed)
                        (map :session)
                        set)))))))

(defn hit-session-id
  [hit]
  (or (:session hit)
      (get-in hit [:metadata :session])
      (get-in hit [:extra :session])))

(defn filter-authorized-memory-hits!
  [config ctx hits]
  (let [hits (vec hits)
        session-ids (map hit-session-id hits)]
    (-> (authorized-session-ids! config ctx session-ids)
        (.then (fn [allowed]
                 (->> hits
                      (filter (fn [hit]
                                (contains? allowed (str (or (hit-session-id hit) "")))))
                      vec))))))

(defn copy-response-headers!
  [reply headers]
  (.forEach headers
            (fn [value key]
              (when-not (contains? #{"connection" "content-length" "content-encoding" "transfer-encoding"} (str/lower-case key))
                (.header reply key value)))))

(defn send-fetch-response!
  [reply resp]
  (copy-response-headers! reply (.-headers resp))
  (-> (.arrayBuffer resp)
      (.then (fn [buf]
               (-> (.code reply (.-status resp))
                   (.send (.from js/Buffer buf)))))))

(defn request-query-string
  [request]
  (let [params (js/URLSearchParams.)
        query (or (aget request "query") #js {})]
    (doseq [key (js-array-seq (.keys js/Object query))]
      (let [value (aget query key)]
        (cond
          (no-content? value) nil
          (array? value) (doseq [item (array-seq value)]
                           (.append params key (str item)))
          :else (.append params key (str value)))))
    (let [encoded (.toString params)]
      (if (str/blank? encoded) "" (str "?" encoded)))))

(defn request-forward-headers
  [request extra]
  (let [headers (js/Headers.)
        source (or (aget request "headers") #js {})]
    (doseq [key (js-array-seq (.keys js/Object source))]
      (let [lower (str/lower-case key)
            value (aget source key)]
        (when (and (not (contains? #{"host" "connection" "content-length" "transfer-encoding"} lower))
                   (not (no-content? value)))
          (.set headers key (str value)))))
    (doseq [[key value] extra]
      (if (nil? value)
        (.delete headers key)
        (.set headers key (str value))))
    headers))

(defn request-forward-body
  [request]
  (let [method (str/upper-case (or (aget request "method") "GET"))
        body (aget request "body")]
    (cond
      (contains? #{"GET" "HEAD"} method) nil
      (or (string? body)
          (instance? js/Uint8Array body)
          (instance? js/ArrayBuffer body)
          (instance? js/Buffer body)) body
      (no-content? body) nil
      :else (.stringify js/JSON body))))

(defn no-content?
  [x]
  (or (nil? x) (= js/undefined x)))

(defn tool-cost []
  {:input 0 :output 0 :cacheRead 0 :cacheWrite 0})

(defn provider-model-config
  [model-id]
  {:id model-id
   :name model-id
   :reasoning false
   :input ["text"]
   :contextWindow 128000
   :maxTokens 8192
   :cost (tool-cost)})

(defn proxx-openai-base-url
  [config]
  (let [base (:proxx-base-url config)]
    (cond
      (str/ends-with? base "/v1") base
      (str/ends-with? base "/") (str base "v1")
      :else (str base "/v1"))))

(defn models-config
  [config]
  {:providers
   {:proxx
    {:baseUrl (proxx-openai-base-url config)
     :apiKey "PROXX_AUTH_TOKEN"
     :authHeader true
     :api "openai-completions"
     :compat {:supportsDeveloperRole false
              :supportsReasoningEffort false}
     :models [(provider-model-config (:proxx-default-model config))]}}})

(defn default-settings
  [config]
  {:llmModel (:proxx-default-model config)
   :embedModel (:proxx-embed-model config)
   :maxContextTokens 128000
   :llmMaxTokens 8192
   :llmBaseUrl (proxx-openai-base-url config)
   :embedBaseUrl (:proxx-base-url config)
   :retrievalMode "dense"
   :retrievalTopK 6
   :hybridTopKDense 12
   :hybridTopKSparse 20
   :hybridTopKFinal 6
   :hybridFusion "rrf"
   :hybridRrfK 60
   :vectorDim 1024
   :chunkTargetTokens 500
   :chunkMaxTokens 700
   :projectName (:project-name config)
   :qdrantCollection (:collection-name config)
   :docsPath (str (:workspace-root config) "/.knoxx/databases/default/docs")
   :docsExtensions ".md,.mdx,.txt,.json,.org,.html,.csv,.pdf"})

(defn ensure-settings!
  [config]
  (when-not @settings-state*
    (reset! settings-state* (default-settings config)))
  @settings-state*)

(defn request-session-id
  [request]
  (str (or (aget request "headers" "x-knoxx-session-id") "")))

(defn database-root-dir
  [runtime config]
  (.resolve (aget runtime "path") (:workspace-root config) ".knoxx" "databases"))

(defn database-docs-dir
  [runtime config db-id]
  (.join (aget runtime "path") (database-root-dir runtime config) db-id "docs"))

(defn database-owner-key
  [auth-context]
  (or (some-> (ctx-org-id auth-context) str not-empty) "__global__"))

(defn default-database-id
  [auth-context]
  (if-let [org-id (some-> (ctx-org-id auth-context) str not-empty)]
    (str "default:" org-id)
    "default"))

(defn default-database-profile
  ([runtime config] (default-database-profile runtime config nil))
  ([runtime config auth-context]
   (let [db-id (default-database-id auth-context)]
     {:id db-id
      :name "Workspace Docs"
      :orgId (ctx-org-id auth-context)
      :orgSlug (ctx-org-slug auth-context)
      :docsPath (database-docs-dir runtime config db-id)
      :qdrantCollection (:collection-name config)
      :publicDocsBaseUrl ""
      :useLocalDocsBaseUrl true
      :forumMode false
      :privateToSession false
      :ownerSessionId nil
      :ownerUserId (ctx-user-id auth-context)
      :ownerMembershipId (ctx-membership-id auth-context)
      :createdAt (now-iso)})))

(defn default-database-record
  []
  {:indexed {}
   :history []
   :progress nil
   :lastRequest nil})

(defn ensure-database-state!
  ([runtime config] (ensure-database-state! runtime config nil))
  ([runtime config auth-context]
   (when-not @database-state*
     (reset! database-state* {:active-id "default"
                              :active-ids {}
                              :profiles {}
                              :records {}}))
   (swap! database-state*
          (fn [state]
            (let [state (merge {:active-id "default"
                                :active-ids {}
                                :profiles {}
                                :records {}}
                               state)
                  global-default (default-database-profile runtime config nil)
                  state (-> state
                            (update :profiles #(if (contains? % (:id global-default)) % (assoc % (:id global-default) global-default)))
                            (update :records #(if (contains? % (:id global-default)) % (assoc % (:id global-default) (default-database-record)))))
                  owner-key (database-owner-key auth-context)
                  scoped-default (default-database-profile runtime config auth-context)]
              (cond-> state
                auth-context (-> (update :profiles #(if (contains? % (:id scoped-default)) % (assoc % (:id scoped-default) scoped-default)))
                                 (update :records #(if (contains? % (:id scoped-default)) % (assoc % (:id scoped-default) (default-database-record))))
                                 (update :active-ids #(if (contains? % owner-key) % (assoc % owner-key (:id scoped-default)))))
                (nil? auth-context) (assoc :active-id (or (:active-id state) (:id global-default)))))))
   @database-state*))

(defn ensure-dir!
  [runtime dir-path]
  (.mkdir (aget runtime "fs") dir-path #js {:recursive true}))

(defn profile-can-access?
  ([profile session-id] (profile-can-access? profile nil session-id))
  ([profile auth-context session-id]
   (let [org-id (some-> (:orgId profile) str not-empty)
         org-allowed? (if org-id
                        (or (nil? auth-context)
                            (system-admin? auth-context)
                            (= org-id (str (ctx-org-id auth-context))))
                        (or (nil? auth-context)
                            (system-admin? auth-context)))
         session-allowed? (or (not (:privateToSession profile))
                              (str/blank? (str (:ownerSessionId profile)))
                              (= (str (:ownerSessionId profile)) (str session-id)))]
     (and org-allowed? session-allowed?))))

(defn effective-active-database-id
  ([runtime config request] (effective-active-database-id runtime config request nil))
  ([runtime config request auth-context]
   (let [state (ensure-database-state! runtime config auth-context)
         session-id (request-session-id request)
         owner-key (database-owner-key auth-context)
         default-id (default-database-id auth-context)
         active-id (if auth-context
                     (or (get-in state [:active-ids owner-key]) default-id)
                     (or (:active-id state) default-id))
         active-profile (get-in state [:profiles active-id])]
     (if (profile-can-access? active-profile auth-context session-id)
       active-id
       (or (some (fn [[db-id profile]]
                   (when (profile-can-access? profile auth-context session-id) db-id))
                 (:profiles state))
           default-id)))))

(defn active-database-profile
  ([runtime config request] (active-database-profile runtime config request nil))
  ([runtime config request auth-context]
   (let [state (ensure-database-state! runtime config auth-context)
         db-id (effective-active-database-id runtime config request auth-context)]
     (get-in state [:profiles db-id]))))

(defn normalize-relative-path
  [value]
  (-> (str value)
      (str/replace #"\\" "/")
      (str/replace #"^/+" "")))

(defn sanitize-upload-name
  [name]
  (let [trimmed (str/trim (str name))
        cleaned (-> trimmed
                    (str/replace #"[\\/]+" "-")
                    (str/replace #"[^A-Za-z0-9._ -]" "-")
                    (str/replace #"\s+" " "))]
    (if (str/blank? cleaned) "upload.bin" cleaned)))

(defn create-db-id
  [runtime name]
  (let [base (-> (str/lower-case (str name))
                 (str/replace #"[^a-z0-9]+" "-")
                 (str/replace #"^-+|-+$" ""))
        prefix (if (str/blank? base) "db" base)]
    (str prefix "-" (.slice (.randomUUID (aget runtime "crypto")) 0 8))))

(defn list-files-recursive!
  [runtime dir-path]
  (let [node-fs (aget runtime "fs")
        node-path (aget runtime "path")
        read-promise (.readdir node-fs dir-path #js {:withFileTypes true})]
    (-> read-promise
        (.then (fn [entries]
                 (.then (js/Promise.all
                         (clj->js
                          (for [entry (js-array-seq entries)]
                            (let [full-path (.join node-path dir-path (.-name entry))]
                              (if (.isDirectory entry)
                                (list-files-recursive! runtime full-path)
                                (js/Promise.resolve #js [full-path]))))))
                        (fn [nested]
                          (into [] (mapcat js-array-seq) (js-array-seq nested))))))
        (.catch (fn [err]
                  (if (= (aget err "code") "ENOENT")
                    (js/Promise.resolve [])
                    (js/Promise.reject err)))))))

(defn file-chunk-count
  [text]
  (max 1 (js/Math.ceil (/ (max 1 (count (str text))) 1800))))

(defn indexed-meta
  [runtime config db-id rel-path]
  (get-in (ensure-database-state! runtime config) [:records db-id :indexed rel-path]))

(defn document-entry!
  [runtime config profile db-id abs-path]
  (let [node-fs (aget runtime "fs")
        node-path (aget runtime "path")
        docs-path (:docsPath profile)]
    (-> (.stat node-fs abs-path)
        (.then (fn [stats]
                 (let [rel-path (normalize-relative-path (.relative node-path docs-path abs-path))
                       meta (indexed-meta runtime config db-id rel-path)]
                   {:name (.basename node-path abs-path)
                    :relativePath rel-path
                    :size (or (aget stats "size") 0)
                    :indexed (boolean meta)
                    :chunkCount (or (:chunkCount meta) 0)
                    :indexedAt (:indexedAt meta)}))))))

(defn list-documents!
  ([runtime config request] (list-documents! runtime config request nil))
  ([runtime config request auth-context]
   (let [profile (active-database-profile runtime config request auth-context)
        db-id (:id profile)]
     (-> (ensure-dir! runtime (:docsPath profile))
         (.then (fn [] (list-files-recursive! runtime (:docsPath profile))))
         (.then (fn [paths]
                  (-> (js/Promise.all
                       (clj->js (map #(document-entry! runtime config profile db-id %) paths)))
                      (.then (fn [items]
                               {:documents (->> (js-array-seq items)
                                                (sort-by :relativePath)
                                                vec)})))))))))

(defn active-record
  ([runtime config request] (active-record runtime config request nil))
  ([runtime config request auth-context]
   (let [db-id (effective-active-database-id runtime config request auth-context)]
     (get-in (ensure-database-state! runtime config auth-context) [:records db-id]))))

(defn active-agent-profile
  ([runtime config] (active-agent-profile runtime config nil))
  ([runtime config auth-context]
   (let [state (ensure-database-state! runtime config auth-context)
         owner-key (database-owner-key auth-context)
         active-id (if auth-context
                     (or (get-in state [:active-ids owner-key]) (default-database-id auth-context))
                     (or (:active-id state) "default"))]
     (or (get-in state [:profiles active-id])
         (get-in state [:profiles (default-database-id auth-context)])
         (get-in state [:profiles "default"])))))

(defn compact-whitespace
  [text]
  (-> (str text)
      (str/replace #"\s+" " ")
      str/trim))

(defn search-tokens
  [text]
  (->> (re-seq #"[A-Za-z0-9][A-Za-z0-9_./:-]*" (str/lower-case (str text)))
       (remove #(<= (count %) 1))
       distinct
       vec))

(def text-like-exts
  #{".md" ".mdx" ".txt" ".json" ".org" ".html" ".htm" ".csv" ".edn" ".clj" ".cljs" ".ts" ".tsx" ".js" ".jsx" ".yaml" ".yml" ".xml" ".log" ".sql"})

(defn text-like-path?
  [path-str]
  (let [lower (str/lower-case (str path-str))
        idx (.lastIndexOf lower ".")]
    (or (= idx -1)
        (contains? text-like-exts (.slice lower idx)))))

(defn best-match-index
  [haystack query tokens]
  (let [phrase-idx (if (str/blank? query) -1 (.indexOf haystack query))]
    (if (>= phrase-idx 0)
      phrase-idx
      (or (some (fn [token]
                  (let [idx (.indexOf haystack token)]
                    (when (>= idx 0) idx)))
                tokens)
          0))))

(defn snippet-around
  [text query tokens max-chars]
  (let [raw (str text)
        lowered (str/lower-case raw)
        idx (best-match-index lowered query tokens)
        radius (max 80 (js/Math.floor (/ max-chars 2)))
        start (max 0 (- idx radius))
        end (min (count raw) (+ idx radius))
        prefix (if (> start 0) "…" "")
        suffix (if (< end (count raw)) "…" "")]
    (str prefix (compact-whitespace (.slice raw start end)) suffix)))

(defn semantic-score
  [{:keys [query tokens rel-path name text indexed]}]
  (let [query (str/lower-case (str query))
        rel-lower (str/lower-case (str rel-path))
        name-lower (str/lower-case (str name))
        text-lower (str/lower-case (str text))
        phrase-score (+ (if (and (not (str/blank? query)) (str/includes? name-lower query)) 10 0)
                        (if (and (not (str/blank? query)) (str/includes? rel-lower query)) 8 0)
                        (if (and (not (str/blank? query)) (str/includes? text-lower query)) 6 0))
        token-score (reduce (fn [total token]
                              (+ total
                                 (if (str/includes? name-lower token) 3 0)
                                 (if (str/includes? rel-lower token) 2 0)
                                 (min 3 (* 0.6 (count-occurrences text-lower token)))))
                            0
                            tokens)
        indexed-bonus (if indexed 0.75 0)]
    (+ phrase-score token-score indexed-bonus)))

(defn tool-text-result
  [text details]
  #js {:content #js [#js {:type "text" :text text}]
       :details (clj->js details)})

(defn maybe-tool-update!
  [f text]
  (when (fn? f)
    (f #js {:content #js [#js {:type "text" :text text}]})))

(defn semantic-search-documents!
  ([runtime config opts] (semantic-search-documents! runtime config opts nil))
  ([runtime config {:keys [query top-k max-snippet-chars]} auth-context]
  (let [profile (active-agent-profile runtime config auth-context)
        db-id (:id profile)
        docs-path (:docsPath profile)
        node-fs (aget runtime "fs")
        node-path (aget runtime "path")
        tokens (search-tokens query)
        top-k (max 1 (min 10 (or top-k 5)))
        max-snippet-chars (max 160 (min 1200 (or max-snippet-chars 320)))]
    (-> (ensure-dir! runtime docs-path)
        (.then (fn [] (list-files-recursive! runtime docs-path)))
        (.then (fn [paths]
                 (.then (js/Promise.all
                         (clj->js
                          (for [abs-path paths]
                            (let [rel-path (normalize-relative-path (.relative node-path docs-path abs-path))
                                  name (.basename node-path abs-path)
                                  indexed-meta (indexed-meta runtime config db-id rel-path)]
                              (if (text-like-path? rel-path)
                                (-> (.readFile node-fs abs-path "utf8")
                                    (.then (fn [content]
                                             (let [[clipped _] (clip-text content 20000)
                                                   score (semantic-score {:query query
                                                                          :tokens tokens
                                                                          :rel-path rel-path
                                                                          :name name
                                                                          :text clipped
                                                                          :indexed (boolean indexed-meta)})]
                                               {:path rel-path
                                                :name name
                                                :score score
                                                :indexed (boolean indexed-meta)
                                                :chunkCount (or (:chunkCount indexed-meta) 0)
                                                :snippet (snippet-around clipped (str/lower-case (str query)) tokens max-snippet-chars)})))
                                    (.catch (fn [_err]
                                              {:path rel-path
                                               :name name
                                               :score 0
                                               :indexed false
                                               :chunkCount 0
                                               :snippet ""})))
                                (js/Promise.resolve {:path rel-path
                                                     :name name
                                                     :score 0
                                                     :indexed false
                                                     :chunkCount 0
                                                     :snippet ""}))))))
                        (fn [results]
                          (let [ranked (->> (js-array-seq results)
                                            (filter #(pos? (:score %)))
                                            (sort-by (juxt (comp - :score) :path))
                                            (take top-k)
                                            vec)]
                            {:database {:id (:id profile)
                                        :name (:name profile)
                                        :docsPath docs-path
                                        :qdrantCollection (:qdrantCollection profile)}
                             :query query
                             :tokens tokens
                             :results ranked})))))))))

(defn semantic-search-result-text
  [{:keys [database query results]}]
  (if (seq results)
    (str "Active corpus: " (:name database) " (" (:qdrantCollection database) ")\n"
         "Query: " query "\n\n"
         (str/join
          "\n\n"
          (map-indexed (fn [idx result]
                         (str (inc idx) ". " (:path result)
                              "\n   score: " (.toFixed (js/Number. (:score result)) 2)
                              (when (:indexed result)
                                (str ", indexed chunks: " (:chunkCount result)))
                              "\n   snippet: " (:snippet result)))
                       results)))
    (str "Active corpus: " (:name database) " (" (:qdrantCollection database) ")\n"
         "Query: " query "\n\nNo strong semantic matches found.")))

(defn semantic-read-document!
  ([runtime config opts] (semantic-read-document! runtime config opts nil))
  ([runtime config {:keys [path max-chars]} auth-context]
  (let [profile (active-agent-profile runtime config auth-context)
        node-fs (aget runtime "fs")
        node-path (aget runtime "path")
        rel-path (normalize-relative-path path)
        abs-path (.resolve node-path (:docsPath profile) rel-path)
        rel-to-root (.relative node-path (:docsPath profile) abs-path)
        max-chars (max 500 (min 20000 (or max-chars 6000)))]
    (if (or (str/starts-with? rel-to-root "..") (.isAbsolute node-path rel-to-root))
      (js/Promise.reject (js/Error. "Path escapes active docs root"))
      (-> (.readFile node-fs abs-path "utf8")
          (.then (fn [content]
                   (let [[clipped truncated?] (clip-text content max-chars)]
                     {:database {:id (:id profile)
                                 :name (:name profile)
                                 :qdrantCollection (:qdrantCollection profile)}
                     :path rel-path
                     :truncated truncated?
                     :content clipped}))))))))

(defn semantic-read-result-text
  [{:keys [database path content truncated]}]
  (str "Active corpus: " (:name database) " (" (:qdrantCollection database) ")\n"
       "Path: " path
       (when truncated "\nNote: content truncated for tool safety.")
       "\n\n"
       content))

(defn passive-hydration!
  ([runtime config mode message] (passive-hydration! runtime config mode message nil))
  ([runtime config mode message auth-context]
   (if (= mode "rag")
     (let [started-ms (.now js/Date)
           top-k (max 1 (min 4 (or (:retrievalTopK @settings-state*) 3)))]
       (-> (semantic-search-documents! runtime config {:query message
                                                      :top-k top-k
                                                      :max-snippet-chars 240} auth-context)
           (.then (fn [result]
                    (assoc result :elapsedMs (- (.now js/Date) started-ms))))))
     (js/Promise.resolve nil))))

(defn memory-hydration-trigger?
  [message]
  (boolean (re-find #"(?i)\b(previous|earlier|before|remember|last time|prior|session|you said|you did|we talked|we discussed)\b"
                    (or message ""))))

(defn passive-memory-hydration!
  ([config conversation-id message] (passive-memory-hydration! config conversation-id message nil))
  ([config conversation-id message auth-context]
   (if (and (openplanner-enabled? config)
            (memory-hydration-trigger? message))
     (let [started-ms (.now js/Date)]
       (-> (openplanner-memory-search! config {:query message :k 4})
           (.then (fn [result]
                    (-> (filter-authorized-memory-hits! config auth-context (:hits result))
                        (.then (fn [hits]
                                 (assoc result :hits hits
                                               :elapsedMs (- (.now js/Date) started-ms)
                                               :conversationId conversation-id))))))))
     (js/Promise.resolve nil))))

(defn passive-hydration-text
  [hydration]
  (when (seq (:results hydration))
    (str "Passive semantic hydration from the active Knoxx corpus follows. This context is automatic and may be incomplete. Use semantic_query or semantic_read if more grounding is needed.\n\n"
         (str/join
          "\n\n"
          (map-indexed (fn [idx result]
                         (str (inc idx) ". " (:path result)
                              "\n   relevance: " (.toFixed (js/Number. (:score result)) 2)
                              (when (:indexed result)
                              (str ", indexed chunks: " (:chunkCount result)))
                              "\n   snippet: " (:snippet result)))
                       (:results hydration))))))

(defn passive-memory-hydration-text
  [memory]
  (when (seq (:hits memory))
    (str "Passive conversational memory hydration from OpenPlanner follows. This is prior Knoxx session memory and action history; verify with memory_search or memory_session if precision matters.\n\n"
         (str/join
          "\n\n"
          (map-indexed
           (fn [idx hit]
             (let [metadata (or (:metadata hit) hit)
                   session (or (:session hit) (:session metadata) "unknown-session")
                   role (or (:role hit) (:role metadata) "memory")
                   snippet (or (:snippet hit) (:document hit) (:text hit) "")]
               (str (inc idx) ". session=" session ", role=" role
                    "\n   snippet: " (or (value->preview-text snippet 260) ""))))
           (:hits memory))))))

(defn build-agent-user-message
  [message hydration memory]
  (let [parts (cond-> [(str "User request:\n" message)]
                (passive-hydration-text hydration) (conj (passive-hydration-text hydration))
                (passive-memory-hydration-text memory) (conj (passive-memory-hydration-text memory)))]
    (str/join "\n\n" parts)))

(defn hydration-sources
  [hydration]
  (if (seq (:results hydration))
    (mapv (fn [result]
            {:title (:name result)
             :url (:path result)
             :section (:snippet result)})
          (:results hydration))
    []))

(defn create-semantic-custom-tools
  ([runtime config] (create-semantic-custom-tools runtime config nil))
  ([runtime config auth-context]
   (let [Type (aget runtime "Type")
         query-params (.Object Type
                               #js {:query (.String Type #js {:description "Natural-language semantic search query for the active Knoxx corpus."})
                                    :topK (.Optional Type (.Number Type #js {:description "Maximum number of matches to return." :minimum 1 :maximum 10}))
                                    :maxSnippetChars (.Optional Type (.Number Type #js {:description "Maximum snippet length per hit." :minimum 160 :maximum 1200}))})
         read-params (.Object Type
                              #js {:path (.String Type #js {:description "Relative document path returned by semantic_query or visible in the active corpus."})
                                   :maxChars (.Optional Type (.Number Type #js {:description "Maximum characters of document content to return." :minimum 500 :maximum 20000}))})
         semantic-query-tool #js {:name "semantic_query"
                                  :label "Semantic Query"
                                  :description "Search the active Knoxx knowledge corpus for semantically relevant documents and snippets."
                                  :promptSnippet "Search the active Knoxx corpus by meaning and retrieve the most relevant documents/snippets."
                                  :promptGuidelines #js ["Use semantic_query when you need grounded workspace knowledge beyond what passive hydration already exposed."
                                                         "Prefer semantic_query over guessing when the answer may live in notes, uploaded documents, or indexed corpus files."
                                                         "Follow semantic_query with semantic_read when one result looks promising and you need exact source text."]
                                  :parameters query-params
                                  :execute (fn [_tool-call-id params a b c]
                                             (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                                                   query (or (aget params "query") "")
                                                   top-k (aget params "topK")
                                                   max-snippet-chars (aget params "maxSnippetChars")]
                                               (maybe-tool-update! on-update "Searching active Knoxx corpus…")
                                               (-> (semantic-search-documents! runtime config {:query query
                                                                                              :top-k top-k
                                                                                              :max-snippet-chars max-snippet-chars} auth-context)
                                                   (.then (fn [result]
                                                            (tool-text-result (semantic-search-result-text result) result))))))}
         semantic-read-tool #js {:name "semantic_read"
                                 :label "Semantic Read"
                                 :description "Read the full text of a specific document from the active Knoxx corpus by relative path."
                                 :promptSnippet "Read a specific Knoxx corpus document by relative path after semantic_query identifies a likely hit."
                                 :promptGuidelines #js ["Use semantic_read after semantic_query when you need exact source text instead of summaries or snippets."
                                                        "Pass a relative document path from semantic_query results."]
                                 :parameters read-params
                                 :execute (fn [_tool-call-id params a b c]
                                            (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                                                  path (or (aget params "path") "")
                                                  max-chars (aget params "maxChars")]
                                              (maybe-tool-update! on-update "Reading corpus document…")
                                              (-> (semantic-read-document! runtime config {:path path :max-chars max-chars} auth-context)
                                                  (.then (fn [result]
                                                           (tool-text-result (semantic-read-result-text result) result))))))}]
     (clj->js
      (vec
       (remove nil?
               [(when (or (nil? auth-context)
                          (ctx-tool-allowed? auth-context "semantic_query"))
                  semantic-query-tool)
                (when (or (nil? auth-context)
                          (ctx-tool-allowed? auth-context "read")
                          (ctx-tool-allowed? auth-context "semantic_query"))
                  semantic-read-tool)]))))))

(defn create-openplanner-custom-tools
  ([runtime config] (create-openplanner-custom-tools runtime config nil))
  ([runtime config auth-context]
   (let [Type (aget runtime "Type")
         search-params (.Object Type
                                #js {:query (.String Type #js {:description "Semantic memory search across prior Knoxx sessions and actions indexed in OpenPlanner."})
                                     :k (.Optional Type (.Number Type #js {:description "Maximum number of memory hits to return." :minimum 1 :maximum 8}))
                                     :sessionId (.Optional Type (.String Type #js {:description "Optional conversation/session id to scope the search."}))})
         session-params (.Object Type
                                 #js {:sessionId (.String Type #js {:description "Knoxx conversation/session id stored in OpenPlanner."})})
         memory-search-execute (fn [_tool-call-id params a b c]
                                 (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                                       query (or (aget params "query") "")
                                       k (aget params "k")
                                       session-id (or (aget params "sessionId") "")]
                                   (maybe-tool-update! on-update "Searching Knoxx memory in OpenPlanner…")
                                   (-> (openplanner-memory-search! config {:query query :k k :session-id session-id})
                                       (.then (fn [result]
                                                (-> (filter-authorized-memory-hits! config auth-context (:hits result))
                                                    (.then (fn [hits]
                                                             (let [filtered (assoc result :hits hits)]
                                                               (tool-text-result (openplanner-memory-search-text filtered) filtered))))))))))
         memory-session-execute (fn [_tool-call-id params a b c]
                                  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                                        session-id (or (aget params "sessionId") "")]
                                    (maybe-tool-update! on-update "Loading Knoxx session from OpenPlanner…")
                                    (-> (fetch-openplanner-session-rows! config session-id)
                                        (.then (fn [rows]
                                                 (when-not (session-visible? auth-context rows)
                                                   (throw (http-error 403 "memory_scope_denied" "OpenPlanner session is outside the current Knoxx scope")))
                                                 (let [payload (doto (js-obj)
                                                                 (aset "sessionId" session-id)
                                                                 (aset "rows" (clj->js rows)))]
                                                   (tool-text-result (openplanner-session-text session-id rows) payload)))))))
         memory-search-tool (doto (js-obj)
                              (aset "name" "memory_search")
                              (aset "label" "Memory Search")
                              (aset "description" "Search prior Knoxx sessions, answers, and tool/action receipts stored in OpenPlanner.")
                              (aset "promptSnippet" "Search Knoxx long-term memory in OpenPlanner when the user asks about earlier sessions, prior decisions, or the agent's own past actions.")
                              (aset "promptGuidelines" (clj->js ["Use memory_search when the user references previous sessions, past work, or asks you to remember what happened before."
                                                                 "Prefer memory_search over guessing about prior conversations or actions."
                                                                 "If one session looks relevant, follow with memory_session to inspect the full transcript slice."]))
                              (aset "parameters" search-params)
                              (aset "execute" memory-search-execute))
         memory-session-tool (doto (js-obj)
                               (aset "name" "memory_session")
                               (aset "label" "Memory Session")
                               (aset "description" "Load the indexed transcript/events for a specific Knoxx session from OpenPlanner.")
                               (aset "promptSnippet" "Load a specific Knoxx OpenPlanner session when you need the exact previous transcript or action trace.")
                               (aset "promptGuidelines" (clj->js ["Use memory_session after memory_search identifies a promising session id."
                                                                  "memory_session is the exact transcript/action drill-down companion to memory_search."]))
                               (aset "parameters" session-params)
                               (aset "execute" memory-session-execute))]
     (clj->js
      (vec
       (remove nil?
               [(when (or (nil? auth-context)
                          (ctx-tool-allowed? auth-context "memory_search"))
                  memory-search-tool)
                (when (or (nil? auth-context)
                          (ctx-tool-allowed? auth-context "memory_session"))
                  memory-session-tool)]))))))

(defn create-knoxx-custom-tools
  ([runtime config] (create-knoxx-custom-tools runtime config nil))
  ([runtime config auth-context]
   (.concat (create-semantic-custom-tools runtime config auth-context)
            (create-openplanner-custom-tools runtime config auth-context))))

(defn start-document-ingestion!
  [runtime config profile {:keys [full selected-files]}]
  (let [node-fs (aget runtime "fs")
        node-path (aget runtime "path")
        node-crypto (aget runtime "crypto")
        db-id (:id profile)
        docs-path (:docsPath profile)]
    (-> (list-files-recursive! runtime docs-path)
        (.then (fn [all-abs]
                 (let [wanted (when-not full
                                (into #{} (map normalize-relative-path) selected-files))
                       queue (->> all-abs
                                  (map (fn [abs]
                                         (let [rel (normalize-relative-path (.relative node-path docs-path abs))]
                                           {:abs abs :rel rel})))
                                  (filter (fn [{:keys [rel]}]
                                            (or full (contains? wanted rel))))
                                  vec)
                       started-at (now-iso)
                       total (count queue)
                       mode (if full "full" "selected")]
                   (swap! database-state* assoc-in [:records db-id :progress]
                          {:active true
                           :startedAt started-at
                           :mode mode
                           :currentFile (some-> queue first :rel)
                           :processedChunks 0
                           :totalChunks total
                           :percent 0
                           :percentPrecise 0
                           :filesUpdated 0
                           :errors 0
                           :stale false})
                   (swap! database-state* assoc-in [:records db-id :lastRequest]
                          {:full (boolean full)
                           :selectedFiles (vec (map :rel queue))})
                   (.then (js/Promise.all
                           (clj->js
                            (map (fn [{:keys [abs rel]}]
                                   (-> (.readFile node-fs abs "utf8")
                                       (.then (fn [content]
                                                {:rel rel
                                                 :error false
                                                 :chunkCount (file-chunk-count content)}))
                                       (.catch (fn [err]
                                                 {:rel rel
                                                  :error true
                                                  :detail (str err)
                                                  :chunkCount 0}))))
                                 queue)))
                          (fn [results]
                            (let [items (vec (js-array-seq results))
                                  files-updated (count (remove :error items))
                                  errors (count (filter :error items))
                                  total-indexed (reduce + 0 (map :chunkCount items))
                                  started-ms (.getTime (js/Date. started-at))
                                  duration-seconds (max 0 (js/Math.round (/ (- (.now js/Date) started-ms) 1000)))
                                  history-item {:id (.randomUUID node-crypto)
                                                :completedAt (now-iso)
                                                :mode mode
                                                :chunksUpserted total-indexed
                                                :processedChunks total
                                                :filesUpdated files-updated
                                                :durationSeconds duration-seconds
                                                :errors errors}]
                              (swap! database-state*
                                     (fn [state]
                                       (let [state-with-index (reduce (fn [acc {:keys [rel error chunkCount]}]
                                                                        (if error
                                                                          acc
                                                                          (assoc-in acc [:records db-id :indexed rel]
                                                                                    {:chunkCount chunkCount
                                                                                     :indexedAt (now-iso)})))
                                                                      state
                                                                      items)]
                                         (-> state-with-index
                                             (assoc-in [:records db-id :progress]
                                                       {:active false
                                                        :startedAt started-at
                                                        :mode mode
                                                        :currentFile nil
                                                        :processedChunks total
                                                        :totalChunks total
                                                        :percent 100
                                                        :percentPrecise 100
                                                        :filesUpdated files-updated
                                                        :errors errors
                                                        :stale false})
                                             (update-in [:records db-id :history]
                                                        (fn [history]
                                                          (->> (conj (vec history) history-item)
                                                               (take-last 50)
                                                               vec))))))
                              {:ok true
                               :started true
                               :mode mode
                               :selectedFiles (vec (map :rel queue))}))))))))))

(defn request-stream-body
  [request]
  (let [method (str/upper-case (or (aget request "method") "GET"))
        body (request-forward-body request)
        content-type (str/lower-case (str (or (aget request "headers" "content-type") "")))]
    (cond
      (contains? #{"GET" "HEAD"} method) #js {}
      (some? body) #js {:body body}
      (str/includes? content-type "multipart/form-data") #js {:body (aget request "raw")
                                                               :duplex "half"}
      :else #js {})))

(defn forward-knoxx-request!
  [config request method path extra]
  (let [target-url (str (:knoxx-base-url config) "/api/" path (request-query-string request))
        base #js {:method method
                  :headers (request-forward-headers request {"x-api-key" (when-not (str/blank? (:knoxx-api-key config)) (:knoxx-api-key config))})}
        stream-opts (request-stream-body request)]
    (js/fetch target-url (.assign js/Object base stream-opts (clj->js extra)))))

(defn clip-text
  ([text] (clip-text text 20000))
  ([text limit]
   (if (<= (count text) limit)
     [text false]
     [(subs text 0 limit) true])))

(defn normalize-role
  [role]
  (let [role (str (or role ""))
        canonical (or (get role-aliases role) role)]
    (if (contains? role-tools canonical) canonical "knowledge_worker")))

(defn email-enabled?
  [config]
  (and (not (str/blank? (:gmail-app-email config)))
       (not (str/blank? (:gmail-app-password config)))))

(defn auth-tool-ids
  [auth-context]
  (into #{}
        (comp (filter #(= "allow" (:effect %)))
              (map :toolId))
        (:toolPolicies auth-context)))

(defn ensure-role-can-use!
  ([role tool-id]
   (let [normalized (normalize-role role)
         allowed (into #{} (map first) (get role-tools normalized))]
     (when-not (contains? allowed tool-id)
       (throw (js/Error. (str "Role '" normalized "' cannot use tool '" tool-id "'"))))
     normalized))
  ([auth-context role tool-id]
   (if auth-context
     (let [allowed (auth-tool-ids auth-context)]
       (when-not (contains? allowed tool-id)
         (throw (http-error 403 "tool_denied" (str "Current Knoxx policy does not allow tool '" tool-id "'"))))
       (primary-context-role auth-context))
     (ensure-role-can-use! role tool-id))))

(defn tool-catalog
  ([config role]
   (let [normalized (normalize-role role)
         email? (email-enabled? config)]
     {:role normalized
      :email_enabled email?
      :tools (mapv (fn [[tool-id label description]]
                     {:id tool-id
                      :label label
                      :description description
                      :enabled (if (= tool-id "email.send") email? true)})
                   (get role-tools normalized))}))
  ([config role auth-context]
   (if auth-context
     (let [email? (email-enabled? config)
           tools (->> (:toolPolicies auth-context)
                      (filter #(= "allow" (:effect %)))
                      (map (fn [policy]
                             (let [tool-id (:toolId policy)]
                               {:id tool-id
                                :label (case tool-id
                                         "read" "Read"
                                         "write" "Write"
                                         "edit" "Edit"
                                         "bash" "Shell"
                                         "canvas" "Canvas"
                                         "email.send" "Email"
                                         "discord.publish" "Discord"
                                         "bluesky.publish" "Bluesky"
                                         "semantic_query" "Semantic Query"
                                         "memory_search" "Memory Search"
                                         "memory_session" "Memory Session"
                                         tool-id)
                                :description (case tool-id
                                               "read" "Read files and retrieved context"
                                               "write" "Create new markdown drafts and artifacts"
                                               "edit" "Revise existing documents and drafts"
                                               "bash" "Run controlled shell commands"
                                               "canvas" "Open long-form markdown drafting canvas"
                                               "email.send" "Send drafts through configured email account"
                                               "discord.publish" "Publish updates to Discord"
                                               "bluesky.publish" "Publish updates to Bluesky"
                                               "semantic_query" "Query semantic context in the active corpus"
                                               "memory_search" "Search prior Knoxx sessions in OpenPlanner"
                                               "memory_session" "Load a specific Knoxx session from OpenPlanner"
                                               tool-id)
                                :enabled (if (= tool-id "email.send") email? true)})))
                      vec)]
       {:role (primary-context-role auth-context)
        :email_enabled email?
        :tools tools})
     (tool-catalog config role))))

(defn create-runtime-tools
  [runtime config auth-context]
  (let [sdk (aget runtime "sdk")
        cwd (:workspace-root config)
        read-tool (aget sdk "createReadTool")
        write-tool (aget sdk "createWriteTool")
        edit-tool (aget sdk "createEditTool")
        bash-tool (aget sdk "createBashTool")
        allowed? (fn [tool-id]
                   (or (nil? auth-context)
                       (ctx-tool-allowed? auth-context tool-id)))]
    (vec
     (remove nil?
             [(when (and read-tool (allowed? "read")) (read-tool cwd))
              (when (and write-tool (allowed? "write")) (write-tool cwd))
              (when (and edit-tool (allowed? "edit")) (edit-tool cwd))
              (when (and bash-tool (allowed? "bash")) (bash-tool cwd))]))))

(defn resolve-workspace-path
  [runtime config raw-path]
  (let [node-path (aget runtime "path")
        workspace-root (.resolve node-path (:workspace-root config))
        candidate (if (.isAbsolute node-path raw-path)
                    (.resolve node-path raw-path)
                    (.resolve node-path workspace-root raw-path))
        rel (.relative node-path workspace-root candidate)]
    (when (or (str/starts-with? rel "..") (.isAbsolute node-path rel))
      (throw (js/Error. "Path escapes workspace root")))
    candidate))

(defn count-occurrences
  [text needle]
  (loop [idx 0
         total 0]
    (let [hit (.indexOf text needle idx)]
      (if (= hit -1)
        total
        (recur (+ hit (max 1 (count needle))) (inc total))))))

(defn replace-first
  [text old new]
  (let [idx (.indexOf text old)]
    (if (= idx -1)
      text
      (str (.slice text 0 idx)
           new
           (.slice text (+ idx (count old)))))))

(defn js-array-seq
  [value]
  (if (array? value) (array-seq value) []))

(defn content-part-text
  [part]
  (cond
    (string? part) part
    (= (aget part "type") "text") (or (aget part "text") "")
    (= (aget part "type") "output_text") (or (aget part "text") "")
    :else ""))

(defn assistant-message-text
  [message]
  (let [content (aget message "content")
        merged (cond
                 (string? content) content
                 (array? content) (apply str (map content-part-text (array-seq content)))
                 :else "")]
    (cond
      (not (str/blank? merged)) merged
      (string? (aget message "text")) (aget message "text")
      (string? (aget message "errorMessage")) (aget message "errorMessage")
      :else "")))

(defn ws-envelope
  [channel payload]
  {:channel channel
   :timestamp (now-iso)
   :payload payload})

(defn safe-ws-send!
  [socket payload]
  (when (= (aget socket "readyState") 1)
    (.send socket (.stringify js/JSON (clj->js payload)))))

(defn system-stats
  [runtime]
  (let [node-os (aget runtime "os")
        cpu-count (max 1 (.availableParallelism node-os))
        load1 (or (aget (.loadavg node-os) 0) 0)
        total-mem (or (.totalmem node-os) 1)
        free-mem (or (.freemem node-os) 0)
        cpu-percent (min 100 (* 100 (/ load1 cpu-count)))
        mem-percent (min 100 (* 100 (- 1 (/ free-mem total-mem))))]
    {:timestamp (now-iso)
     :cpu_percent cpu-percent
     :memory_percent mem-percent
     :gpu []
     :network {:total_bytes_per_sec 0}}))

(defn broadcast-ws!
  [channel payload]
  (doseq [[client-id client] @ws-clients*]
    (try
      (safe-ws-send! (aget client "socket") (ws-envelope channel payload))
      (catch :default _
        (swap! ws-clients* dissoc client-id)))))

(defn broadcast-ws-session!
  [session-id channel payload]
  (doseq [[client-id client] @ws-clients*]
    (let [client-session-id (or (aget client "sessionId") "")]
      (when (or (str/blank? session-id)
                (= session-id client-session-id))
        (try
          (safe-ws-send! (aget client "socket") (ws-envelope channel payload))
          (catch :default _
            (swap! ws-clients* dissoc client-id)))))))

(defn ensure-ws-stats-loop!
  [runtime]
  (when-not @ws-stats-interval*
    (reset! ws-stats-interval*
            (js/setInterval
             (fn []
               (when (seq @ws-clients*)
                 (broadcast-ws! "stats" (system-stats runtime))))
             5000))))

(defn register-ws-routes!
  [runtime app]
  (ensure-ws-stats-loop! runtime)
  (.route app
          #js {:method "GET"
               :url "/ws/stream"
               :handler (fn [_request reply]
                          (-> (.code reply 426)
                              (.type "application/json")
                              (.send #js {:error "WebSocket upgrade required"})))
               :wsHandler (fn [socket request]
                            (let [ws (or (aget socket "socket") socket)
                                  client-id (.randomUUID (aget runtime "crypto"))
                                  session-id (try
                                               (or (.get (.-searchParams (js/URL. (str "http://localhost" (or (aget request "url") "/ws/stream")))) "session_id") "")
                                               (catch :default _ ""))]
                              (swap! ws-clients* assoc client-id #js {:socket ws :sessionId session-id})
                              (.on ws "close" (fn [] (swap! ws-clients* dissoc client-id)))
                              (.on ws "error" (fn [] (swap! ws-clients* dissoc client-id)))
                              (safe-ws-send! ws (ws-envelope "stats" (system-stats runtime)))
                              (doseq [msg (take-last 20 @lounge-messages*)]
                                (safe-ws-send! ws (ws-envelope "lounge" msg)))))}))

(defn latest-assistant-message
  [session]
  (let [messages (js-array-seq (aget session "messages"))]
    (last (filter #(= (aget % "role") "assistant") messages))))

(defn usage-map
  [usage]
  {:input_tokens (or (aget usage "input") 0)
   :output_tokens (or (aget usage "output") 0)})

(defn store-run!
  [run-id run]
  (swap! runs* assoc run-id run)
  (swap! run-order* (fn [order]
                      (->> (cons run-id (remove #{run-id} order))
                           (take 200)
                           vec)))
  run)

(defn summarize-run
  [run]
  (select-keys run [:run_id :created_at :updated_at :status :model :ttft_ms :total_time_ms :input_tokens :output_tokens :tokens_per_s :error]))

(defn append-limited
  [items item limit]
  (->> (conj (vec items) item)
       (take-last limit)
       vec))

(defn value->preview-text
  ([value] (value->preview-text value 800))
  ([value max-chars]
   (let [raw (cond
               (no-content? value) ""
               (string? value) value
               (or (map? value) (vector? value) (seq? value))
               (try
                 (.stringify js/JSON (clj->js value) nil 2)
                 (catch :default _
                   (pr-str value)))
               :else
               (try
                 (let [json (.stringify js/JSON value nil 2)]
                   (if (string? json) json (str value)))
                 (catch :default _
                   (str value))))
         [text truncated] (clip-text raw max-chars)]
     (when-not (str/blank? text)
       (if truncated
         (str text "…")
         text)))))

(defn update-run!
  [run-id f]
  (let [state (swap! runs* update run-id (fn [run]
                                           (when run
                                             (f run))))]
    (get state run-id)))

(defn append-run-event!
  [run-id event]
  (update-run! run-id
               (fn [run]
                 (-> run
                     (assoc :updated_at (now-iso))
                     (update :events #(append-limited % event 200))))))

(defn update-run-tool-receipt!
  [run-id receipt-id default-receipt f]
  (update-run! run-id
               (fn [run]
                 (update run :tool_receipts
                         (fn [receipts]
                           (let [items (vec receipts)
                                 idx (first (keep-indexed (fn [i item]
                                                            (when (= (:id item) receipt-id)
                                                              i))
                                                          items))
                                 base (merge {:id receipt-id} default-receipt)]
                             (if (nil? idx)
                               (append-limited items (f base) 40)
                               (assoc items idx (f (merge base (nth items idx)))))))))))

(defn tool-event-payload
  [run-id conversation-id session-id type extra]
  (merge {:run_id run-id
          :conversation_id conversation-id
          :session_id session-id
          :type type
          :at (now-iso)}
         extra))

(defn percentile-95
  [values]
  (if (seq values)
    (let [sorted (sort values)
          idx (js/Math.floor (* 0.95 (dec (count sorted))))]
      (nth sorted idx 0))
    0))

(defn record-retrieval-sample!
  [mode elapsed-ms]
  (swap! retrieval-stats*
         (fn [stats]
           (let [samples (->> (conj (vec (:samples stats)) elapsed-ms)
                              (take-last 100)
                              vec)
                 count-samples (count samples)
                 avg (if (pos? count-samples)
                       (/ (reduce + samples) count-samples)
                       0)
                 current-modes (or (:modeCounts stats) {:dense 0 :hybrid 0 :hybrid_rerank 0})]
             {:samples samples
             :avgRetrievalMs (js/Math.round avg)
             :p95RetrievalMs (js/Math.round (percentile-95 samples))
             :recentSamples count-samples
             :modeCounts (update current-modes (keyword (or mode "dense")) (fnil inc 0))}))))

(defn planner-row-timestamp-ms
  [row]
  (let [ts (:ts row)]
    (cond
      (number? ts) ts
      (string? ts) (let [parsed (js/Date.parse ts)]
                     (if (js/isNaN parsed) (.now js/Date) parsed))
      :else (.now js/Date))))

(defn planner-row->agent-message
  [row]
  (let [role (some-> (:role row) str)
        text (some-> (:text row) str)]
    (when (and (contains? #{"user" "assistant" "system"} role)
               (not (str/blank? text)))
      #js {:role role
           :content #js [#js {:type "text" :text text}]
           :timestamp (planner-row-timestamp-ms row)})))

(defn rehydrate-session-manager!
  [config session-manager conversation-id model-id]
  (if (or (str/blank? conversation-id)
          (not (openplanner-enabled? config)))
    (js/Promise.resolve session-manager)
    (-> (openplanner-request! config "GET" (str "/v1/sessions/" conversation-id))
        (.then (fn [body]
                 (doseq [row (or (:rows body) [])]
                   (when-let [message (planner-row->agent-message row)]
                     (.appendMessage session-manager message)))
                 session-manager))
        (.catch (fn [err]
                  (.warn js/console "[knoxx] failed to rehydrate session from OpenPlanner" err)
                  session-manager)))))

(defn first-result-array
  [value]
  (let [items (or value [])
        first-item (first items)]
    (if (sequential? first-item)
      (vec first-item)
      [])))

(defn vector-result-hits
  [result]
  (let [ids (first-result-array (:ids result))
        docs (first-result-array (:documents result))
        metas (first-result-array (:metadatas result))
        distances (first-result-array (:distances result))]
    (mapv (fn [idx id]
            {:id id
             :document (nth docs idx "")
             :metadata (nth metas idx {})
             :distance (nth distances idx nil)})
          (range (count ids))
          ids)))

(defn openplanner-recent-session-summaries!
  [config]
  (-> (openplanner-request! config "GET" "/v1/sessions")
      (.then (fn [body]
               (let [session-ids (->> (or (:rows body) [])
                                      (map :session)
                                      (remove str/blank?)
                                      distinct
                                      (take 4)
                                      vec)]
                 (if (seq session-ids)
                   (-> (.all js/Promise
                             (clj->js
                              (map (fn [session-id]
                                     (-> (openplanner-request! config "GET" (str "/v1/sessions/" session-id))
                                         (.then (fn [session-body]
                                                  (let [rows (or (:rows session-body) [])
                                                        row (or (last (filter #(contains? #{"assistant" "system"} (:role %)) rows))
                                                                (last rows))]
                                                    (when row
                                                      {:session session-id
                                                       :role (:role row)
                                                       :text (:text row)}))))
                                         (.catch (fn [_] nil))))
                                   session-ids)))
                       (.then (fn [results]
                                (->> (js->clj results :keywordize-keys true)
                                     (remove nil?)
                                     vec))))
                   (js/Promise.resolve [])))))
      (.catch (fn [_] (js/Promise.resolve [])))))

(defn openplanner-memory-search!
  [config {:keys [query k session-id]}]
  (let [query (str/trim (or query ""))
        k (max 1 (min 8 (or k 5)))]
    (if (str/blank? query)
      (js/Promise.resolve {:query "" :hits [] :mode :none})
      (-> (openplanner-request! config "POST" "/v1/search/vector"
                                (cond-> {:q query
                                         :k k
                                         :source "knoxx"
                                         :project (:project-name config)}
                                  (not (str/blank? session-id)) (assoc :session session-id)))
          (.then (fn [body]
                   {:query query
                    :mode :vector
                    :hits (vector-result-hits (:result body))}))
          (.catch (fn [_]
                    (-> (openplanner-request! config "POST" "/v1/search/fts"
                                              (cond-> {:q query
                                                       :limit k
                                                       :source "knoxx"
                                                       :project (:project-name config)}
                                                (not (str/blank? session-id)) (assoc :session session-id)))
                        (.then (fn [body]
                                 (let [hits (vec (or (:rows body) []))]
                                   (if (seq hits)
                                     {:query query
                                      :mode :fts
                                      :hits hits}
                                     (-> (openplanner-recent-session-summaries! config)
                                         (.then (fn [recent]
                                                  {:query query
                                                   :mode :recent
                                                   :hits recent}))))))))))))))

(defn openplanner-memory-search-text
  [{:keys [query mode hits]}]
  (if (seq hits)
    (str "OpenPlanner memory search for: " query
         "\nMode: " (name mode)
         "\n\n"
         (str/join
          "\n\n"
          (map-indexed
           (fn [idx hit]
             (let [metadata (or (:metadata hit) hit)
                   session (or (:session hit) (:session metadata) "unknown-session")
                   role (or (:role hit) (:role metadata) "memory")
                    snippet (or (:snippet hit) (:document hit) (:text hit) "")
                    distance (:distance hit)]
                (str (inc idx) ". session=" session
                     ", role=" role
                     (when (number? distance)
                       (str ", distance=" (.toFixed (js/Number. distance) 3)))
                    "\n   " (or (value->preview-text snippet 280) ""))))
           hits)))
    (str "OpenPlanner memory search for: " query "\nNo prior Knoxx memory hits found.")))

(defn openplanner-session-text
  [session-id rows]
  (if (seq rows)
    (str "OpenPlanner session " session-id
         "\n\n"
         (str/join
          "\n\n"
          (map-indexed
           (fn [idx row]
             (str (inc idx) ". [" (or (:role row) "event") "] "
                  (or (value->preview-text (or (:text row) "") 320) "")))
           rows)))
    (str "OpenPlanner session " session-id " is empty or missing.")))

(defn openplanner-event
  [config {:keys [id ts kind session message role model text extra]}]
  {:schema "openplanner.event.v1"
   :id id
   :ts (or ts (now-iso))
   :source "knoxx"
   :kind kind
   :source_ref {:project (:project-name config)
                :session session
                :message message}
   :text text
   :meta {:role role
          :author (if (= role "user") "user" "knoxx")
          :model model
          :tags ["knoxx" kind role]}
   :extra extra})

(defn tool-receipt-summary-text
  [receipt]
  (str "Tool: " (or (:tool_name receipt) (:id receipt) "tool")
       "\nStatus: " (or (:status receipt) "unknown")
       (when-let [input (:input_preview receipt)]
         (str "\nInput:\n" input))
       (when-let [result (:result_preview receipt)]
         (str "\nOutput:\n" result))))

(defn run-summary-text
  [run]
  (str "Run " (:run_id run)
       "\nMode: " (get-in run [:settings :mode])
       "\nModel: " (:model run)
       "\nStatus: " (:status run)
       (when-let [answer (:answer run)]
         (str "\nAnswer:\n" answer))
       (when-let [error (:error run)]
         (str "\nError:\n" error))))

(defn run-scope-extra
  [run]
  (select-keys run [:org_id :org_slug :user_id :user_email :membership_id]))

(defn index-run-memory!
  [config run]
  (if-not (openplanner-enabled? config)
    (js/Promise.resolve nil)
    (let [conversation-id (:conversation_id run)
          session-id (:session_id run)
          scope-extra (run-scope-extra run)
          request-text (or (some-> (:request_messages run) first :content) "")
          answer (:answer run)
          base-events [(openplanner-event config {:id (str (:run_id run) ":user")
                                                  :ts (:created_at run)
                                                  :kind "knoxx.message"
                                                  :session conversation-id
                                                  :message (str (:run_id run) ":user")
                                                  :role "user"
                                                  :model (:model run)
                                                  :text request-text
                                                  :extra (merge {:run_id (:run_id run)
                                                                 :conversation_id conversation-id
                                                                 :session_id session-id
                                                                 :mode (get-in run [:settings :mode])}
                                                                scope-extra)})
                       (openplanner-event config {:id (str (:run_id run) ":summary")
                                                  :ts (:updated_at run)
                                                  :kind "knoxx.run"
                                                  :session conversation-id
                                                  :message (str (:run_id run) ":summary")
                                                  :role "system"
                                                  :model (:model run)
                                                  :text (run-summary-text run)
                                                  :extra (merge {:run_id (:run_id run)
                                                                 :conversation_id conversation-id
                                                                 :session_id session-id
                                                                 :status (:status run)
                                                                 :ttft_ms (:ttft_ms run)
                                                                 :total_time_ms (:total_time_ms run)}
                                                                scope-extra)})]
          events (if (str/blank? (or answer ""))
                   base-events
                   (conj base-events
                         (openplanner-event config {:id (str (:run_id run) ":assistant")
                                                    :ts (:updated_at run)
                                                    :kind "knoxx.message"
                                                    :session conversation-id
                                                    :message (str (:run_id run) ":assistant")
                                                    :role "assistant"
                                                    :model (:model run)
                                                    :text answer
                                                    :extra (merge {:run_id (:run_id run)
                                                                   :conversation_id conversation-id
                                                                   :session_id session-id
                                                                   :status (:status run)}
                                                                  scope-extra)})))
          tool-events (map (fn [receipt]
                             (openplanner-event config {:id (str (:run_id run) ":tool:" (:id receipt))
                                                        :ts (or (:ended_at receipt) (:started_at receipt) (:updated_at run))
                                                        :kind "knoxx.tool_receipt"
                                                        :session conversation-id
                                                        :message (str (:run_id run) ":tool:" (:id receipt))
                                                        :role "system"
                                                        :model (:model run)
                                                        :text (tool-receipt-summary-text receipt)
                                                        :extra (merge {:run_id (:run_id run)
                                                                       :conversation_id conversation-id
                                                                       :session_id session-id
                                                                       :receipt receipt}
                                                                      scope-extra)}))
                           (:tool_receipts run))]
      (-> (openplanner-request! config "POST" "/v1/events"
                                {:events (into events tool-events)})
          (.catch (fn [err]
                    (.warn js/console "[knoxx] failed to index run memory into OpenPlanner" err)
                    nil))))))

(defn ensure-sdk-runtime!
  [runtime config]
  (if-let [p @sdk-runtime*]
    p
    (let [node-fs (aget runtime "fs")
          node-path (aget runtime "path")
          sdk (aget runtime "sdk")
          runtime-dir (:agent-dir config)
          models-file (.join node-path runtime-dir "models.json")
          auth-file (.join node-path runtime-dir "auth.json")
          SettingsManager (aget sdk "SettingsManager")
          AuthStorage (aget sdk "AuthStorage")
          ModelRegistry (aget sdk "ModelRegistry")
          DefaultResourceLoader (aget sdk "DefaultResourceLoader")
          settings-manager (.inMemory SettingsManager (clj->js {:compaction {:enabled false}
                                                                :retry {:enabled true :maxRetries 1}}))
          p (-> (.mkdir node-fs runtime-dir #js {:recursive true})
                (.then (fn []
                         (.writeFile node-fs
                                     models-file
                                     (.stringify js/JSON (clj->js (models-config config)) nil 2)
                                     "utf8")))
                (.then (fn []
                         (let [auth-storage (.create AuthStorage auth-file)
                               _ (when-not (str/blank? (:proxx-auth-token config))
                                   (.setRuntimeApiKey auth-storage "proxx" (:proxx-auth-token config)))
                               model-registry (ModelRegistry. auth-storage models-file)
                               loader (DefaultResourceLoader.
                                       #js {:cwd (:workspace-root config)
                                            :agentDir runtime-dir
                                            :settingsManager settings-manager
                                            :systemPromptOverride (fn [_] (:agent-system-prompt config))})]
                           (-> (.reload loader)
                               (.then (fn []
                                        #js {:authStorage auth-storage
                                             :modelRegistry model-registry
                                             :settingsManager settings-manager
                                             :loader loader
                                             :runtimeDir runtime-dir})))))))]
      (reset! sdk-runtime* p)
      p)))

(defn create-agent-session!
  ([runtime config conversation-id model-id] (create-agent-session! runtime config conversation-id model-id nil))
  ([runtime config conversation-id model-id auth-context]
  (-> (ensure-sdk-runtime! runtime config)
      (.then
       (fn [sdk-runtime]
         (let [sdk (aget runtime "sdk")
               SessionManager (aget sdk "SessionManager")
               createAgentSession (aget sdk "createAgentSession")
                model-registry (aget sdk-runtime "modelRegistry")
                auth-storage (aget sdk-runtime "authStorage")
                loader (aget sdk-runtime "loader")
                settings-manager (aget sdk-runtime "settingsManager")
                model (or (.find model-registry "proxx" model-id)
                          (.find model-registry "proxx" (:proxx-default-model config)))
               create-session (fn [session-manager]
                                (-> (createAgentSession
                                     #js {:cwd (:workspace-root config)
                                          :agentDir (aget sdk-runtime "runtimeDir")
                                          :authStorage auth-storage
                                          :modelRegistry model-registry
                                          :resourceLoader loader
                                          :settingsManager settings-manager
                                          :sessionManager session-manager
                                          :model model
                                          :thinkingLevel "off"
                                          :tools (clj->js (create-runtime-tools runtime config auth-context))
                                          :customTools (create-knoxx-custom-tools runtime config auth-context)})
                                    (.then (fn [result]
                                             (aget result "session")))))]
           (if (no-content? model)
             (js/Promise.reject (js/Error. (str "No pi model configured for " model-id)))
             (let [session-manager (.inMemory SessionManager (:workspace-root config))]
               (.appendModelChange session-manager "proxx" model-id)
               (.appendThinkingLevelChange session-manager "off")
               (-> (rehydrate-session-manager! config session-manager conversation-id model-id)
                   (.then (fn [hydrated-manager]
                            (create-session hydrated-manager))))))))))))

(defn ensure-agent-session!
  ([runtime config conversation-id model-id] (ensure-agent-session! runtime config conversation-id model-id nil))
  ([runtime config conversation-id model-id auth-context]
  (if-let [session (get @agent-sessions* conversation-id)]
    (js/Promise.resolve session)
    (-> (create-agent-session! runtime config conversation-id model-id auth-context)
        (.then (fn [session]
                 (swap! agent-sessions* assoc conversation-id session)
                 session))))))

(defn active-agent-session
  [conversation-id]
  (get @agent-sessions* conversation-id))

(defn queue-agent-control!
  [_runtime _config {:keys [conversation-id session-id run-id message kind]}]
  (cond
    (str/blank? conversation-id)
    (js/Promise.reject (js/Error. "conversation_id is required for live controls"))

    (str/blank? message)
    (js/Promise.reject (js/Error. "message is required for live controls"))

    :else
    (if-let [session (active-agent-session conversation-id)]
      (if-not (true? (aget session "isStreaming"))
        (js/Promise.reject (js/Error. "No active running turn is available for live controls"))
        (let [preview (if (> (count message) 240)
                        (str (subs message 0 240) "…")
                        message)
              event-type (if (= kind "follow_up") "follow_up_queued" "steer_queued")
              failure-type (if (= kind "follow_up") "follow_up_failed" "steer_failed")
              invoke (if (= kind "follow_up")
                       #(.followUp session message)
                       #(.steer session message))]
          (-> (invoke)
              (.then (fn []
                       (let [event (tool-event-payload run-id conversation-id session-id event-type
                                                       {:status "queued"
                                                        :preview preview})]
                         (when run-id
                           (append-run-event! run-id event))
                         (broadcast-ws-session! session-id "events" event)
                         {:ok true
                          :conversation_id conversation-id
                          :session_id session-id
                          :run_id run-id
                          :kind kind})))
              (.catch (fn [err]
                        (let [event (tool-event-payload run-id conversation-id session-id failure-type
                                                        {:status "failed"
                                                         :error (str err)
                                                         :preview preview})]
                          (when run-id
                            (append-run-event! run-id event))
                          (broadcast-ws-session! session-id "events" event))
                        (throw err))))))
      (js/Promise.reject (js/Error. "Conversation is not active in the agent runtime")))))

(defn send-agent-turn!
  [runtime config {:keys [conversation-id session-id message model mode run-id auth-context]}]
  (let [node-crypto (aget runtime "crypto")
        conversation-id (or conversation-id (.randomUUID node-crypto))
        _ (ensure-conversation-access! auth-context conversation-id)
        _ (remember-conversation-access! auth-context conversation-id)
        mode (or mode "direct")
        model-id (or model (:llmModel (ensure-settings! config)) (:proxx-default-model config))
        run-id (or run-id (.randomUUID node-crypto))
        started-at (now-iso)
        started-ms (.now js/Date)
        request-messages [{:role "user" :content message}]
        auth-extra (auth-snapshot auth-context)
        base-run (merge {:run_id run-id
                         :session_id session-id
                         :conversation_id conversation-id
                         :created_at started-at
                         :updated_at started-at
                         :status "running"
                         :model model-id
                         :ttft_ms nil
                         :total_time_ms nil
                         :input_tokens nil
                         :output_tokens nil
                         :tokens_per_s nil
                         :error nil
                         :answer nil
                         :events []
                         :tool_receipts []
                         :request_messages request-messages
                         :settings {:sessionId session-id
                                    :conversationId conversation-id
                                    :mode mode
                                    :workspaceRoot (:workspace-root config)}
                         :resources {:provider "proxx"
                                     :collection (:collection-name config)}}
                        auth-extra)
        _ (store-run! run-id base-run)
        initial-event (tool-event-payload run-id conversation-id session-id "run_started"
                                          {:status "running"
                                           :mode mode
                                           :model model-id})
        _ (append-run-event! run-id initial-event)
        _ (broadcast-ws-session! session-id "events" initial-event)
        chunks (atom [])]
    (-> (.all js/Promise
              #js [(passive-hydration! runtime config mode message auth-context)
                   (passive-memory-hydration! config conversation-id message auth-context)])
        (.then (fn [results]
                 (let [hydration (aget results 0)
                       memory-hydration (aget results 1)]
                   (when hydration
                     (let [hydration-event (tool-event-payload run-id conversation-id session-id "passive_hydration"
                                                               {:status "ok"
                                                                :hits (count (:results hydration))
                                                                :elapsed_ms (:elapsedMs hydration)})]
                       (update-run! run-id
                                    (fn [run]
                                      (-> run
                                          (update :resources merge {:passiveHydration (select-keys hydration [:query :tokens :database :elapsedMs :results])})
                                          (assoc :updated_at (now-iso)))))
                       (append-run-event! run-id hydration-event)
                       (broadcast-ws-session! session-id "events" hydration-event)))
                   (when (seq (:hits memory-hydration))
                     (let [memory-event (tool-event-payload run-id conversation-id session-id "memory_hydration"
                                                            {:status "ok"
                                                             :hits (count (:hits memory-hydration))
                                                             :elapsed_ms (:elapsedMs memory-hydration)})]
                       (update-run! run-id
                                    (fn [run]
                                      (-> run
                                          (update :resources merge {:memoryHydration (select-keys memory-hydration [:query :mode :hits :elapsedMs :conversationId])})
                                          (assoc :updated_at (now-iso)))))
                       (append-run-event! run-id memory-event)
                       (broadcast-ws-session! session-id "events" memory-event)))
                   (-> (ensure-agent-session! runtime config conversation-id model-id auth-context)
                     (.then (fn [session]
                              (let [ttft-recorded? (atom false)
                                    unsubscribe (.subscribe session
                                                          (fn [event]
                                                            (let [event-type (aget event "type")]
                                                              (cond
                                                                (= event-type "message_update")
                                                                (let [assistant-event (aget event "assistantMessageEvent")]
                                                                  (when (= (aget assistant-event "type") "text_delta")
                                                                    (let [delta (or (aget assistant-event "delta") "")]
                                                                      (when-not @ttft-recorded?
                                                                        (reset! ttft-recorded? true)
                                                                        (let [ttft-ms (- (.now js/Date) started-ms)
                                                                              ttft-event (tool-event-payload run-id conversation-id session-id "assistant_first_token"
                                                                                                             {:status "streaming"
                                                                                                              :ttft_ms ttft-ms})]
                                                                          (update-run! run-id #(assoc % :ttft_ms ttft-ms))
                                                                          (append-run-event! run-id ttft-event)
                                                                          (broadcast-ws-session! session-id "events" ttft-event)))
                                                                      (swap! chunks conj delta)
                                                                      (when-not (str/blank? delta)
                                                                        (broadcast-ws-session! session-id "tokens"
                                                                                               {:run_id run-id
                                                                                                :conversation_id conversation-id
                                                                                                :session_id session-id
                                                                                                :token delta})))))

                                                                (= event-type "tool_execution_start")
                                                                (let [tool-name (or (aget event "toolName") "tool")
                                                                      tool-call-id (or (aget event "toolCallId") (.randomUUID node-crypto))
                                                                      input-preview (or (value->preview-text (aget event "params") 600)
                                                                                        (value->preview-text (aget event "toolArgs") 600)
                                                                                        (value->preview-text (aget event "args") 600))
                                                                      tool-event (tool-event-payload run-id conversation-id session-id "tool_start"
                                                                                                     {:status "running"
                                                                                                      :tool_name tool-name
                                                                                                      :tool_call_id tool-call-id
                                                                                                      :preview input-preview})]
                                                                  (update-run-tool-receipt! run-id tool-call-id {:tool_name tool-name}
                                                                                            (fn [receipt]
                                                                                              (cond-> (merge receipt {:tool_name tool-name
                                                                                                                      :status "running"
                                                                                                                      :started_at (or (:started_at receipt) (now-iso))})
                                                                                                input-preview (assoc :input_preview input-preview))))
                                                                  (append-run-event! run-id tool-event)
                                                                  (broadcast-ws-session! session-id "events" tool-event))

                                                                (= event-type "tool_execution_update")
                                                                (let [tool-name (or (aget event "toolName") "tool")
                                                                      tool-call-id (or (aget event "toolCallId") (str tool-name "-update"))
                                                                      preview (or (value->preview-text (aget event "delta") 400)
                                                                                  (value->preview-text (aget event "update") 400)
                                                                                  (value->preview-text (aget event "message") 400)
                                                                                  (value->preview-text (aget event "statusMessage") 400))]
                                                                  (update-run-tool-receipt! run-id tool-call-id {:tool_name tool-name}
                                                                                            (fn [receipt]
                                                                                              (cond-> (merge receipt {:tool_name tool-name
                                                                                                                      :status "running"})
                                                                                                preview (update :updates #(append-limited % preview 8)))))
                                                                  (when preview
                                                                    (let [tool-event (tool-event-payload run-id conversation-id session-id "tool_update"
                                                                                                         {:status "running"
                                                                                                          :tool_name tool-name
                                                                                                          :tool_call_id tool-call-id
                                                                                                          :preview preview})]
                                                                      (append-run-event! run-id tool-event)
                                                                      (broadcast-ws-session! session-id "events" tool-event))))

                                                                (= event-type "tool_execution_end")
                                                                (let [tool-name (or (aget event "toolName") "tool")
                                                                      tool-call-id (or (aget event "toolCallId") (.randomUUID node-crypto))
                                                                      is-error (boolean (aget event "isError"))
                                                                      result-preview (or (value->preview-text (aget event "result") 800)
                                                                                         (value->preview-text (aget event "toolResult") 800)
                                                                                         (value->preview-text (aget event "output") 800))
                                                                      tool-event (tool-event-payload run-id conversation-id session-id "tool_end"
                                                                                                     {:status (if is-error "failed" "completed")
                                                                                                      :tool_name tool-name
                                                                                                      :tool_call_id tool-call-id
                                                                                                      :is_error is-error
                                                                                                      :preview result-preview})]
                                                                  (update-run-tool-receipt! run-id tool-call-id {:tool_name tool-name}
                                                                                            (fn [receipt]
                                                                                              (cond-> (merge receipt {:tool_name tool-name
                                                                                                                      :status (if is-error "failed" "completed")
                                                                                                                      :ended_at (now-iso)
                                                                                                                      :is_error is-error})
                                                                                                result-preview (assoc :result_preview result-preview))))
                                                                  (append-run-event! run-id tool-event)
                                                                  (broadcast-ws-session! session-id "events" tool-event))

                                                                (= event-type "turn_end")
                                                                (let [tool-results (or (aget event "toolResults") #js [])
                                                                      turn-event (tool-event-payload run-id conversation-id session-id "turn_end"
                                                                                                     {:status "completed"
                                                                                                      :tool_result_count (or (.-length tool-results) 0)})]
                                                                  (append-run-event! run-id turn-event)
                                                                  (broadcast-ws-session! session-id "events" turn-event))

                                                                (= event-type "agent_end")
                                                                (broadcast-ws-session! session-id "events"
                                                                                       (tool-event-payload run-id conversation-id session-id "agent_end"
                                                                                                           {:status "completed"}))))))]
                                (let [prompt-promise (.prompt session (build-agent-user-message message hydration memory-hydration))]
                                  (.catch
                                   (.then prompt-promise
                                          (fn []
                                            (unsubscribe)
                                            (let [assistant-message (latest-assistant-message session)
                                                  answer (let [chunked (apply str @chunks)]
                                                           (if (str/blank? chunked)
                                                             (assistant-message-text assistant-message)
                                                             chunked))
                                                  usage (or (aget assistant-message "usage") #js {})
                                                  elapsed (- (.now js/Date) started-ms)
                                                  output-tokens (or (aget usage "output") 0)
                                                  tokens-per-second (if (and (pos? output-tokens) (pos? elapsed))
                                                                      (* 1000 (/ output-tokens elapsed))
                                                                      nil)
                                                  sources (hydration-sources hydration)
                                                  response {:answer answer
                                                            :run_id run-id
                                                            :runId run-id
                                                            :conversation_id conversation-id
                                                            :conversationId conversation-id
                                                            :session_id session-id
                                                            :model model-id
                                                            :sources sources
                                                            :compare nil}
                                                  completed-event (tool-event-payload run-id conversation-id session-id "run_completed"
                                                                                      {:status "completed"
                                                                                       :model model-id
                                                                                       :sources_count (count sources)})]
                                              (when (= mode "rag")
                                                (record-retrieval-sample! (:retrievalMode @settings-state*) elapsed))
                                              (let [completed-run (update-run! run-id
                                                                               (fn [run]
                                                                                 (let [resource-patch (cond-> {:sources sources}
                                                                                                        hydration (assoc :passiveHydration (select-keys hydration [:query :tokens :database :elapsedMs :results]))
                                                                                                        memory-hydration (assoc :memoryHydration (select-keys memory-hydration [:query :mode :hits :elapsedMs :conversationId])))]
                                                                                   (-> run
                                                                                       (assoc :updated_at (now-iso)
                                                                                              :status "completed"
                                                                                              :total_time_ms elapsed
                                                                                              :input_tokens (or (aget usage "input") 0)
                                                                                              :output_tokens output-tokens
                                                                                              :tokens_per_s tokens-per-second
                                                                                              :answer answer
                                                                                              :sources sources)
                                                                                       (update :resources merge resource-patch)))))
                                                    _ (when completed-run
                                                        (index-run-memory! config completed-run))]
                                                (append-run-event! run-id completed-event)
                                                (broadcast-ws-session! session-id "events" completed-event)
                                                response)))
                                   (fn [err]
                                     (unsubscribe)
                                     (let [error-event (tool-event-payload run-id conversation-id session-id "run_failed"
                                                                           {:status "failed"
                                                                            :error (str err)})]
                                       (let [failed-run (update-run! run-id
                                                                     (fn [run]
                                                                       (let [resource-patch (cond-> {}
                                                                                              hydration (assoc :passiveHydration (select-keys hydration [:query :tokens :database :elapsedMs :results]))
                                                                                              memory-hydration (assoc :memoryHydration (select-keys memory-hydration [:query :mode :hits :elapsedMs :conversationId])))]
                                                                         (-> run
                                                                             (assoc :updated_at (now-iso)
                                                                                    :status "failed"
                                                                                    :total_time_ms (- (.now js/Date) started-ms)
                                                                                    :error (str err))
                                                                             (update :resources merge resource-patch)))))
                                             _ (when failed-run
                                                 (index-run-memory! config failed-run))]
                                         (append-run-event! run-id error-event)
                                         (broadcast-ws-session! session-id "events" error-event))
                                     (throw err))))))))))))))))

(defn normalize-chat-body
  [body]
  {:message (or (aget body "message") "")
   :conversation-id (or (aget body "conversationId")
                        (aget body "conversation_id"))
   :session-id (or (aget body "sessionId")
                   (aget body "session_id"))
   :run-id (or (aget body "runId")
               (aget body "run_id"))
   :model (or (aget body "model") nil)
   :mode (or (aget body "mode") "direct")})

(defn normalize-control-body
  [body]
  {:message (or (aget body "message") "")
   :conversation-id (or (aget body "conversationId")
                        (aget body "conversation_id"))
   :session-id (or (aget body "sessionId")
                   (aget body "session_id"))
   :run-id (or (aget body "runId")
               (aget body "run_id"))})

(defn route!
  [app method url handler]
  (.route app #js {:method method
                   :url url
                   :handler handler}))

(defn register-routes!
  [runtime app config]
  (ensure-settings! config)

  (route! app "GET" "/health"
          (fn [_request reply]
            (json-response! reply 200 {:status "ok" :service "knoxx-backend-cljs"})))

  (route! app "GET" "/api/config"
          (fn [request reply]
            (json-response!
             reply
             200
             {:knoxx_admin_url (rewrite-localhost-url (:knoxx-admin-url config) request)
              :knoxx_base_url (rewrite-localhost-url (:knoxx-base-url config) request)
              :knoxx_enabled true
              :proxx_enabled (and (not (str/blank? (:proxx-base-url config)))
                                  (not (str/blank? (:proxx-auth-token config))))
              :proxx_default_model (:llmModel @settings-state*)
              :shibboleth_ui_url (if (str/blank? (:shibboleth-ui-url config))
                                   ""
                                   (rewrite-localhost-url (:shibboleth-ui-url config) request))
             :shibboleth_enabled (and (not (str/blank? (:shibboleth-base-url config)))
                                      (not (str/blank? (:shibboleth-ui-url config))))
              :default_role (:knoxx-default-role config)
              :email_enabled (email-enabled? config)
              :rbac_enabled (policy-db-enabled? runtime)})))

  (route! app "GET" "/api/admin/bootstrap"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (with-request-context! runtime request reply
                (fn [ctx]
                  (ensure-permission! ctx "platform.org.read")
                  (policy-db-promise runtime reply 200 (.getBootstrapContext db))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"}))))

  (route! app "GET" "/api/admin/permissions"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (with-request-context! runtime request reply
                (fn [ctx]
                  (ensure-any-permission! ctx ["platform.roles.manage" "org.roles.read"] "permission_denied" "Role permission metadata is outside the current Knoxx scope")
                  (policy-db-promise runtime reply 200 (.listPermissions db))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"}))))

  (route! app "GET" "/api/admin/tools"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (with-request-context! runtime request reply
                (fn [ctx]
                  (ensure-any-permission! ctx ["platform.roles.manage" "org.tool_policy.read" "org.user_policy.read"] "permission_denied" "Tool policy metadata is outside the current Knoxx scope")
                  (policy-db-promise runtime reply 200 (.listTools db))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"}))))

  (route! app "GET" "/api/admin/orgs"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (with-request-context! runtime request reply
                (fn [ctx]
                  (ensure-permission! ctx "platform.org.read")
                  (policy-db-promise runtime reply 200 (.listOrgs db))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"}))))

  (route! app "POST" "/api/admin/orgs"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (with-request-context! runtime request reply
                (fn [ctx]
                  (ensure-permission! ctx "platform.org.create")
                  (policy-db-promise runtime reply 201 (.createOrg db (or (aget request "body") #js {})))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"}))))

  (route! app "GET" "/api/admin/users"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [org-id (or (aget request "query" "orgId")
                               (aget request "query" "org_id")
                               nil)]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (if org-id
                      (ensure-org-scope! ctx org-id "org.users.read")
                      (ensure-permission! ctx "platform.org.read"))
                    (policy-db-promise runtime reply 200 (.listUsers db (clj->js {:orgId org-id}))))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"}))))

  (route! app "POST" "/api/admin/users"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [body (or (aget request "body") #js {})
                    org-id (or (aget body "orgId") (aget body "org_id") "")]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (when (str/blank? (str org-id))
                      (throw (http-error 400 "org_required" "orgId is required")))
                    (ensure-org-scope! ctx org-id "org.users.create")
                    (policy-db-promise runtime reply 201 (.createUser db body)))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"}))))

  (route! app "GET" "/api/admin/orgs/:orgId/users"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [org-id (or (aget request "params" "orgId") "")]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (ensure-org-scope! ctx org-id "org.users.read")
                    (policy-db-promise runtime reply 200 (.listUsers db (clj->js {:orgId org-id}))))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"}))))

  (route! app "POST" "/api/admin/orgs/:orgId/users"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [org-id (or (aget request "params" "orgId") "")
                    body (or (aget request "body") #js {})
                    payload (.assign js/Object #js {} body (clj->js {:orgId org-id}))]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (ensure-org-scope! ctx org-id "org.users.create")
                    (policy-db-promise runtime reply 201 (.createUser db payload)))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"}))))

  (route! app "GET" "/api/admin/orgs/:orgId/memberships"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [org-id (or (aget request "params" "orgId") "")]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (ensure-org-scope! ctx org-id "org.members.read")
                    (policy-db-promise runtime reply 200 (.listMemberships db (clj->js {:orgId org-id}))))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"}))))

  (route! app "PATCH" "/api/admin/memberships/:membershipId/roles"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [membership-id (or (aget request "params" "membershipId") "")]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (policy-db-promise
                     runtime
                     reply
                     200
                     (-> (.getMembership db membership-id)
                         (.then (fn [result]
                                  (let [membership (js->clj (aget result "membership") :keywordize-keys true)]
                                    (when-not membership
                                      (throw (http-error 404 "membership_not_found" "membership not found")))
                                    (ensure-org-scope! ctx (:orgId membership) "org.members.update")
                                    (.setMembershipRoles db membership-id (or (aget request "body") #js {})))))))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"}))))

  (route! app "PATCH" "/api/admin/memberships/:membershipId/tool-policies"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [membership-id (or (aget request "params" "membershipId") "")]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (policy-db-promise
                     runtime
                     reply
                     200
                     (-> (.getMembership db membership-id)
                         (.then (fn [result]
                                  (let [membership (js->clj (aget result "membership") :keywordize-keys true)]
                                    (when-not membership
                                      (throw (http-error 404 "membership_not_found" "membership not found")))
                                    (ensure-org-scope! ctx (:orgId membership) "org.user_policy.update")
                                    (.setMembershipToolPolicies db membership-id (or (aget request "body") #js {})))))))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"}))))

  (route! app "GET" "/api/admin/orgs/:orgId/roles"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [org-id (or (aget request "params" "orgId") "")]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (ensure-org-scope! ctx org-id "org.roles.read")
                    (policy-db-promise runtime reply 200 (.listRoles db (clj->js {:orgId org-id}))))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"}))))

  (route! app "POST" "/api/admin/orgs/:orgId/roles"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [org-id (or (aget request "params" "orgId") "")
                    body (or (aget request "body") #js {})
                    payload (.assign js/Object #js {} body (clj->js {:orgId org-id}))]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (ensure-org-scope! ctx org-id "org.roles.create")
                    (policy-db-promise runtime reply 201 (.createRole db payload)))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"}))))

  (route! app "PATCH" "/api/admin/roles/:roleId/tool-policies"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [role-id (or (aget request "params" "roleId") "")]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (policy-db-promise
                     runtime
                     reply
                     200
                     (-> (.getRole db role-id)
                         (.then (fn [result]
                                  (let [role (js->clj (aget result "role") :keywordize-keys true)]
                                    (when-not role
                                      (throw (http-error 404 "role_not_found" "role not found")))
                                    (ensure-org-scope! ctx (:orgId role) "org.tool_policy.update")
                                    (.setRoleToolPolicies db role-id (or (aget request "body") #js {})))))))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"}))))

  (route! app "GET" "/api/admin/orgs/:orgId/data-lakes"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [org-id (or (aget request "params" "orgId") "")]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (ensure-org-scope! ctx org-id "org.datalakes.read")
                    (policy-db-promise runtime reply 200 (.listDataLakes db (clj->js {:orgId org-id}))))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"}))))

  (route! app "POST" "/api/admin/orgs/:orgId/data-lakes"
          (fn [request reply]
            (if-let [db (policy-db runtime)]
              (let [org-id (or (aget request "params" "orgId") "")
                    body (or (aget request "body") #js {})
                    payload (.assign js/Object #js {} body (clj->js {:orgId org-id}))]
                (with-request-context! runtime request reply
                  (fn [ctx]
                    (ensure-org-scope! ctx org-id "org.datalakes.create")
                    (policy-db-promise runtime reply 201 (.createDataLake db payload)))))
              (json-response! reply 503 {:detail "Knoxx policy database is not configured"}))))

  (route! app "GET" "/api/settings"
          (fn [_request reply]
            (json-response! reply 200 @settings-state*)))

  (route! app "PUT" "/api/settings"
          (fn [request reply]
            (let [body (js->clj (or (aget request "body") #js {}) :keywordize-keys true)]
              (swap! settings-state* merge body)
              (json-response! reply 200 @settings-state*))))

  (route! app "GET" "/api/settings/knoxx-status"
          (fn [_request reply]
            (-> (fetch-json (str (:proxx-base-url config) "/v1/models")
                            #js {:headers (bearer-headers (:proxx-auth-token config))})
                (.then (fn [resp]
                         (let [items (or (aget (aget resp "body") "data") #js [])]
                           (json-response! reply 200 {:usingKnoxx true
                                                      :modelsReachable (aget resp "ok")
                                                      :embedReachable (aget resp "ok")
                                                      :modelsCount (.-length items)}))))
                (.catch (fn [_err]
                          (json-response! reply 200 {:usingKnoxx true
                                                     :modelsReachable false
                                                     :embedReachable false
                                                     :modelsCount 0}))))))

  (route! app "GET" "/api/retrieval/stats"
          (fn [_request reply]
            (json-response! reply 200 {:retrieval (dissoc @retrieval-stats* :samples)})))

  (route! app "GET" "/api/proxx/health"
          (fn [_request reply]
            (-> (js/Promise.all
                 #js [(fetch-json (str (:proxx-base-url config) "/health") #js {:headers (bearer-headers (:proxx-auth-token config))})
                      (fetch-json (str (:proxx-base-url config) "/v1/models") #js {:headers (bearer-headers (:proxx-auth-token config))})])
                (.then (fn [parts]
                         (let [health (aget parts 0)
                               models (aget parts 1)
                               model-items (or (aget (aget models "body") "data") #js [])]
                           (json-response!
                            reply
                            200
                            {:reachable (and (aget health "ok") (aget models "ok"))
                             :configured (and (not (str/blank? (:proxx-base-url config)))
                                              (not (str/blank? (:proxx-auth-token config))))
                             :base_url (:proxx-base-url config)
                             :status_code (aget health "status")
                             :model_count (.-length model-items)
                             :default_model (:llmModel @settings-state*)}))))
                (.catch (fn [err]
                          (json-response! reply 502 {:error (str err)
                                                     :reachable false
                                                     :configured true
                                                     :base_url (:proxx-base-url config)
                                                     :default_model (:llmModel @settings-state*)}))))))

  (route! app "GET" "/api/proxx/models"
          (fn [_request reply]
            (-> (fetch-json (str (:proxx-base-url config) "/v1/models")
                            #js {:headers (bearer-headers (:proxx-auth-token config))})
                (.then (fn [resp]
                         (if (aget resp "ok")
                           (json-response! reply 200 {:models (or (aget (aget resp "body") "data") #js [])})
                           (json-response! reply 502 {:error "Proxx model list failed"
                                                      :details (js->clj (aget resp "body") :keywordize-keys true)}))))
                (.catch (fn [err]
                          (json-response! reply 502 {:error (str err)}))))))

  (route! app "POST" "/api/proxx/chat"
          (fn [request reply]
            (let [body (or (aget request "body") #js {})
                  payload #js {:model (or (aget body "model") (:llmModel @settings-state*))
                               :messages (or (aget body "messages") #js [])
                               :temperature (aget body "temperature")
                               :top_p (aget body "top_p")
                               :max_tokens (aget body "max_tokens")
                               :stop (aget body "stop")
                               :stream false}]
              (-> (fetch-json (str (:proxx-base-url config) "/v1/chat/completions")
                              #js {:method "POST"
                                   :headers (bearer-headers (:proxx-auth-token config))
                                   :body (.stringify js/JSON payload)})
                  (.then (fn [resp]
                           (if (aget resp "ok")
                             (let [data (aget resp "body")
                                   choices (or (aget data "choices") #js [])
                                   first-choice (aget choices 0)
                                   message (or (aget first-choice "message") #js {})
                                   content (or (aget message "content")
                                               (aget first-choice "text")
                                               "")]
                               (json-response! reply 200 {:answer content
                                                          :model (or (aget data "model") (aget payload "model"))
                                                          :rag_context nil}))
                             (json-response! reply 502 {:error "Proxx chat failed"
                                                        :details (js->clj (aget resp "body") :keywordize-keys true)}))))
                  (.catch (fn [err]
                            (json-response! reply 502 {:error (str err)})))))))

  (route! app "GET" "/api/models"
          (fn [_request reply]
            (-> (fetch-json (str (:proxx-base-url config) "/v1/models")
                            #js {:headers (bearer-headers (:proxx-auth-token config))})
                (.then (fn [resp]
                         (if (aget resp "ok")
                           (let [items (js-array-seq (or (aget (aget resp "body") "data") #js []))
                                 models (mapv (fn [item]
                                                {:id (str (or (aget item "id") ""))
                                                 :name (str (or (aget item "id") ""))
                                                 :path ""
                                                 :size_bytes 0
                                                 :modified_at (now-iso)
                                                 :hash16mb ""
                                                 :suggested_ctx 128000})
                                              items)]
                             (json-response! reply 200 {:models models}))
                           (json-response! reply 502 {:detail "Model list failed"}))))
                (.catch (fn [err]
                          (json-response! reply 502 {:detail (str err)}))))))

  (route! app "GET" "/api/runs"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (let [limit-raw (aget request "query" "limit")
                      limit (if (string? limit-raw)
                              (js/parseInt limit-raw 10)
                              100)
                      items (->> @run-order*
                                 (map #(get @runs* %))
                                 (filter some?)
                                 (filter #(run-visible? ctx %))
                                 (take (max 1 (or limit 100)))
                                 (map summarize-run)
                                 vec)]
                  (json-response! reply 200 {:runs items}))))))

  (route! app "GET" "/api/runs/:runId"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (let [run-id (aget request "params" "runId")
                      run (get @runs* run-id)]
                  (cond
                    (nil? run) (json-response! reply 404 {:detail "Run not found"})
                    (not (run-visible? ctx run)) (error-response! reply (http-error 403 "run_scope_denied" "Run is outside the current Knoxx scope"))
                    :else (json-response! reply 200 run)))))))

  (route! app "GET" "/api/memory/sessions"
          (fn [request reply]
            (if-not (openplanner-enabled? config)
              (json-response! reply 503 {:detail "OpenPlanner is not configured"})
              (with-request-context! runtime request reply
                (fn [ctx]
                  (ensure-permission! ctx "agent.memory.cross_session")
                  (let [limit-raw (aget request "query" "limit")
                        limit (cond
                                (string? limit-raw) (or (js/parseInt limit-raw 10) 12)
                                (number? limit-raw) limit-raw
                                :else 12)]
                    (-> (openplanner-request! config "GET" "/v1/sessions")
                        (.then (fn [body]
                                 (-> (authorized-session-ids! config ctx (map :session (or (:rows body) [])))
                                     (.then (fn [allowed]
                                              (let [rows (->> (or (:rows body) [])
                                                              (filter #(contains? allowed (str (:session %))))
                                                              (take (max 1 limit))
                                                              vec)]
                                                (json-response! reply 200 {:ok true
                                                                           :rows rows})))))))
                        (.catch (fn [err]
                                  (error-response! reply err 502)))))))))

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
                        (json-response! reply 200 {:ok true :message msg}))))))

  (route! app "GET" "/api/tools/catalog"
          (fn [request reply]
            (let [role (or (aget request "query" "role") (:knoxx-default-role config))]
              (with-request-context! runtime request reply
                (fn [ctx]
                  (when ctx
                    (ensure-permission! ctx "agent.chat.use"))
                  (json-response! reply 200 (tool-catalog config role ctx)))))))

  (route! app "POST" "/api/tools/email/send"
          (fn [_request reply]
            (json-response! reply 503 {:detail "Email send is not implemented yet in the CLJS backend"})))

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
                    (error-response! reply err))))))

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
                    (error-response! reply err))))))

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
                    (error-response! reply err))))))

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

  (route! app "GET" "/v1/models"
          (fn [request reply]
            (when (require-openai-key! config request reply)
              (-> (fetch-json (str (:proxx-base-url config) "/v1/models")
                              #js {:headers (bearer-headers (:proxx-auth-token config))})
                  (.then (fn [resp]
                           (if (aget resp "ok")
                             (json-response! reply 200 (js->clj (aget resp "body") :keywordize-keys true))
                             (openai-auth-error reply 502 "Upstream model list failed" "upstream_error"))))
                  (.catch (fn [err]
                            (openai-auth-error reply 502 (str "Upstream model list failed: " err) "upstream_error")))))))

  (route! app "POST" "/v1/chat/completions"
          (fn [request reply]
            (when (require-openai-key! config request reply)
              (let [payload (or (aget request "body") #js {})]
                (-> (js/fetch (str (:proxx-base-url config) "/v1/chat/completions")
                              #js {:method "POST"
                                   :headers (bearer-headers (:proxx-auth-token config))
                                   :body (.stringify js/JSON payload)})
                    (.then (fn [resp]
                             (send-fetch-response! reply resp)))
                    (.catch (fn [err]
                              (openai-auth-error reply 502 (str "Upstream chat request failed: " err) "upstream_error"))))))))

  (route! app "POST" "/v1/embeddings"
          (fn [request reply]
            (when (require-openai-key! config request reply)
              (let [body (or (aget request "body") #js {})
                    payload (doto (.assign js/Object #js {} body)
                              (aset "model" (or (aget body "model") (:embedModel @settings-state*) (:proxx-embed-model config))))]
                (-> (js/fetch (str (:proxx-base-url config) "/v1/embeddings")
                              #js {:method "POST"
                                   :headers (bearer-headers (:proxx-auth-token config))
                                   :body (.stringify js/JSON payload)})
                    (.then (fn [resp]
                             (send-fetch-response! reply resp)))
                    (.catch (fn [err]
                              (openai-auth-error reply 502 (str "Embedding generation failed: " err) "upstream_error"))))))))

  (route! app "GET" "/api/documents"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "datalake.read"))
                (-> (list-documents! runtime config request ctx)
                    (.then (fn [resp]
                             (json-response! reply 200 resp)))
                    (.catch (fn [err]
                              (json-response! reply 500 {:detail (str "Failed to list documents: " err)}))))))))

  (route! app "GET" "/api/documents/content/*"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "datalake.read"))
                (let [profile (active-database-profile runtime config request ctx)
                      node-fs (aget runtime "fs")
                      node-path (aget runtime "path")
                      rel-path (normalize-relative-path (aget request "params" "*"))
                      abs-path (.resolve node-path (:docsPath profile) rel-path)
                      rel-to-root (.relative node-path (:docsPath profile) abs-path)]
                  (if (or (str/starts-with? rel-to-root "..") (.isAbsolute node-path rel-to-root))
                    (json-response! reply 403 {:detail "Path escapes active docs root"})
                    (-> (.readFile node-fs abs-path "utf8")
                        (.then (fn [content]
                                 (json-response! reply 200 {:path rel-path
                                                            :content content})))
                        (.catch (fn [err]
                                  (json-response! reply 404 {:detail (str "Failed to read document: " err)}))))))))))

  (route! app "POST" "/api/documents/upload"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "datalake.write"))
                (let [profile (active-database-profile runtime config request ctx)
                      docs-path (:docsPath profile)
                      node-fs (aget runtime "fs")
                      node-path (aget runtime "path")
                      promise (.then
                               (.then (ensure-dir! runtime docs-path)
                                      (fn [] (.fromAsync js/Array (.parts request))))
                               (fn [parts]
                                 (let [part-seq (js-array-seq parts)
                                       auto-ingest? (boolean (some (fn [part]
                                                                     (and (= (aget part "type") "field")
                                                                          (= (aget part "fieldname") "autoIngest")
                                                                          (= (str/lower-case (str (aget part "value"))) "true")))
                                                                   part-seq))
                                       file-parts (filter #(= (aget % "type") "file") part-seq)
                                       write-promises (mapv (fn [part]
                                                             (let [safe-name (sanitize-upload-name (or (aget part "filename") "upload.bin"))
                                                                   abs-path (.join node-path docs-path safe-name)
                                                                   rel-path (normalize-relative-path (.relative node-path docs-path abs-path))]
                                                               (.then (.arrayBuffer (js/Response. (aget part "file")))
                                                                      (fn [buf]
                                                                        (.then (.writeFile node-fs abs-path (.from js/Buffer buf))
                                                                               (fn [] rel-path))))))
                                                           file-parts)]
                                   (.then (js/Promise.all (clj->js write-promises))
                                          (fn [written]
                                            (let [files (vec (js-array-seq written))]
                                              (if auto-ingest?
                                                (do
                                                  (start-document-ingestion! runtime config profile {:full false
                                                                                                     :selected-files files})
                                                  (json-response! reply 200 {:ok true
                                                                             :uploaded files
                                                                             :autoIngest true}))
                                                (json-response! reply 200 {:ok true
                                                                           :uploaded files
                                                                           :autoIngest false}))))))))]
                  (.catch promise
                          (fn [err]
                            (json-response! reply 500 {:detail (str "Upload failed: " err)}))))))))

  (route! app "DELETE" "/api/documents/*"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "datalake.write"))
                (let [profile (active-database-profile runtime config request ctx)
                      node-fs (aget runtime "fs")
                      node-path (aget runtime "path")
                      rel-path (normalize-relative-path (aget request "params" "*"))
                      abs-path (.resolve node-path (:docsPath profile) rel-path)
                      rel-to-root (.relative node-path (:docsPath profile) abs-path)
                      db-id (:id profile)]
                  (if (or (str/starts-with? rel-to-root "..") (.isAbsolute node-path rel-to-root))
                    (json-response! reply 403 {:detail "Path escapes active docs root"})
                    (-> (.rm node-fs abs-path #js {:force true})
                        (.then (fn []
                                 (swap! database-state* update-in [:records db-id :indexed] dissoc rel-path)
                                 (json-response! reply 200 {:ok true
                                                            :path rel-path})))
                        (.catch (fn [err]
                                  (json-response! reply 500 {:detail (str "Delete failed: " err)}))))))))))

  (route! app "POST" "/api/documents/ingest"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "datalake.ingest"))
                (let [profile (active-database-profile runtime config request ctx)
                      body (js->clj (or (aget request "body") #js {}) :keywordize-keys true)]
                  (-> (start-document-ingestion! runtime config profile body)
                      (.then (fn [resp]
                               (json-response! reply 200 resp)))
                      (.catch (fn [err]
                                (json-response! reply 500 {:detail (str "Ingestion failed to start: " err)})))))))))

  (route! app "POST" "/api/documents/ingest/restart"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "datalake.ingest"))
                (let [profile (active-database-profile runtime config request ctx)
                      db-id (:id profile)
                      last-request (get-in (ensure-database-state! runtime config ctx) [:records db-id :lastRequest])]
                  (if (nil? last-request)
                    (json-response! reply 400 {:detail "No active ingestion to restart"
                                               :resumed false})
                    (-> (start-document-ingestion! runtime config profile {:full (:full last-request)
                                                                           :selected-files (:selectedFiles last-request)})
                        (.then (fn [resp]
                                 (json-response! reply 200 (assoc resp :resumed true))))
                        (.catch (fn [err]
                                  (json-response! reply 500 {:detail (str "Restart failed: " err)
                                                             :resumed false}))))))))))

  (route! app "GET" "/api/documents/ingestion-status"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "datalake.read"))
                (let [record (active-record runtime config request ctx)
                      progress (:progress record)]
                  (json-response! reply 200 {:active (boolean (:active progress))
                                             :progress progress
                                             :canResumeForum false
                                             :stale false}))))))

  (route! app "GET" "/api/documents/ingestion-progress"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "datalake.read"))
                (let [record (active-record runtime config request ctx)
                      progress (:progress record)]
                  (json-response! reply 200 {:active (boolean (:active progress))
                                             :progress progress
                                             :canResumeForum false
                                             :stale false}))))))

  (route! app "GET" "/api/documents/ingestion-history"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "datalake.read"))
                (let [profile (active-database-profile runtime config request ctx)
                      record (active-record runtime config request ctx)]
                  (json-response! reply 200 {:collection (:qdrantCollection profile)
                                             :items (:history record)}))))))

  (route! app "POST" "/api/chat/retrieval-debug"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "datalake.query"))
                (let [body (js->clj (or (aget request "body") #js {}) :keywordize-keys true)
                      query (str/trim (str (:message body)))
                      top-k (or (:topK body) 5)]
                  (if (str/blank? query)
                    (json-response! reply 400 {:detail "message is required"})
                    (-> (list-documents! runtime config request ctx)
                        (.then (fn [resp]
                                 (let [documents (:documents resp)
                                       lowered (str/lower-case query)
                                       results (->> documents
                                                    (map (fn [doc]
                                                           (let [path (str (:relativePath doc))
                                                                 name (str (:name doc))
                                                                 hay (str/lower-case (str path " " name))
                                                                 score (cond
                                                                         (str/includes? hay lowered) 1
                                                                         (str/includes? lowered (str/lower-case name)) 0.5
                                                                         :else 0)]
                                                             (assoc doc :score score))))
                                                    (filter #(pos? (:score %)))
                                                    (sort-by :score >)
                                                    (take top-k)
                                                    vec)]
                                   (json-response! reply 200 {:query query
                                                              :topK top-k
                                                              :results results}))))
                        (.catch (fn [err]
                                  (json-response! reply 500 {:detail (str "Retrieval debug failed: " err)}))))))))))

  (route! app "GET" "/api/settings/databases"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "org.datalakes.read"))
                (let [state (ensure-database-state! runtime config ctx)
                      session-id (request-session-id request)
                      active-id (effective-active-database-id runtime config request ctx)
                      active-profile (get-in state [:profiles active-id])
                      profiles (->> (:profiles state)
                                    vals
                                    (filter #(profile-can-access? % ctx session-id))
                                    (sort-by :createdAt)
                                    (mapv (fn [profile]
                                            (assoc profile :canAccess (profile-can-access? profile ctx session-id)))))]
                  (json-response! reply 200 {:activeDatabaseId active-id
                                             :databases profiles
                                             :activeRuntime {:projectName (:project-name config)
                                                             :docsPath (:docsPath active-profile)
                                                             :qdrantCollection (:qdrantCollection active-profile)}}))))))

  (route! app "POST" "/api/settings/databases"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "org.datalakes.create"))
                (let [body (js->clj (or (aget request "body") #js {}) :keywordize-keys true)
                      name (str/trim (str (:name body)))
                      session-id (request-session-id request)]
                  (if (str/blank? name)
                    (json-response! reply 400 {:detail "name is required"})
                    (let [db-id (create-db-id runtime name)
                          docs-path (or (:docsPath body) (database-docs-dir runtime config db-id))
                          profile {:id db-id
                                   :name name
                                   :orgId (ctx-org-id ctx)
                                   :orgSlug (ctx-org-slug ctx)
                                   :ownerUserId (ctx-user-id ctx)
                                   :ownerMembershipId (ctx-membership-id ctx)
                                   :docsPath docs-path
                                   :qdrantCollection (or (:qdrantCollection body) (str (:collection-name config) "_" db-id))
                                   :publicDocsBaseUrl (or (:publicDocsBaseUrl body) "")
                                   :useLocalDocsBaseUrl (not= false (:useLocalDocsBaseUrl body))
                                   :forumMode (boolean (:forumMode body))
                                   :privateToSession (boolean (:privateToSession body))
                                   :ownerSessionId (when (:privateToSession body) session-id)
                                   :createdAt (now-iso)}]
                      (-> (ensure-dir! runtime docs-path)
                          (.then (fn []
                                   (swap! database-state*
                                          (fn [state]
                                            (let [owner-key (database-owner-key ctx)]
                                              (-> state
                                                  (assoc-in [:profiles db-id] profile)
                                                  (assoc-in [:records db-id] (default-database-record))
                                                  ((fn [s]
                                                     (if (:activate body)
                                                       (assoc-in s [:active-ids owner-key] db-id)
                                                       s)))))))
                                   (json-response! reply 200 profile)))
                          (.catch (fn [err]
                                    (json-response! reply 500 {:detail (str "Failed to create database profile: " err)})))))))))))

  (route! app "POST" "/api/settings/databases/activate"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "org.datalakes.read"))
                (let [body (js->clj (or (aget request "body") #js {}) :keywordize-keys true)
                      db-id (str (:id body))
                      session-id (request-session-id request)
                      profile (get-in (ensure-database-state! runtime config ctx) [:profiles db-id])]
                  (cond
                    (nil? profile) (json-response! reply 404 {:detail "Database profile not found"})
                    (not (profile-can-access? profile ctx session-id)) (json-response! reply 403 {:detail "Database profile is outside the current Knoxx scope"})
                    :else (do
                            (swap! database-state* assoc-in [:active-ids (database-owner-key ctx)] db-id)
                            (json-response! reply 200 {:ok true
                                                       :activeDatabaseId db-id}))))))))

  (route! app "PATCH" "/api/settings/databases/:id"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "org.datalakes.update"))
                (let [db-id (str (aget request "params" "id"))
                      body (js->clj (or (aget request "body") #js {}) :keywordize-keys true)
                      session-id (request-session-id request)
                      profile (get-in (ensure-database-state! runtime config ctx) [:profiles db-id])]
                  (cond
                    (nil? profile) (json-response! reply 404 {:detail "Database profile not found"})
                    (not (profile-can-access? profile ctx session-id)) (json-response! reply 403 {:detail "Database profile is outside the current Knoxx scope"})
                    :else (let [updated (merge profile
                                               (select-keys body [:name :publicDocsBaseUrl :useLocalDocsBaseUrl :forumMode]))]
                            (swap! database-state* assoc-in [:profiles db-id] updated)
                            (json-response! reply 200 updated))))))))

  (route! app "DELETE" "/api/settings/databases/:id"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "org.datalakes.delete"))
                (let [db-id (str (aget request "params" "id"))
                      session-id (request-session-id request)
                      profile (get-in (ensure-database-state! runtime config ctx) [:profiles db-id])]
                  (cond
                    (nil? profile) (json-response! reply 404 {:detail "Database profile not found"})
                    (= db-id (default-database-id ctx)) (json-response! reply 400 {:detail "Default database cannot be deleted"})
                    (not (profile-can-access? profile ctx session-id)) (json-response! reply 403 {:detail "Database profile is outside the current Knoxx scope"})
                    :else (do
                            (swap! database-state*
                                   (fn [state]
                                     (let [owner-key (database-owner-key ctx)]
                                       (-> state
                                           (update :profiles dissoc db-id)
                                           (update :records dissoc db-id)
                                           ((fn [s]
                                              (if (= (get-in s [:active-ids owner-key]) db-id)
                                                (assoc-in s [:active-ids owner-key] (default-database-id ctx))
                                                s)))))))
                            (json-response! reply 200 {:ok true
                                                       :deleted db-id}))))))))

  (route! app "POST" "/api/settings/databases/:id/make-private"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "org.datalakes.update"))
                (let [db-id (str (aget request "params" "id"))
                      session-id (request-session-id request)
                      profile (get-in (ensure-database-state! runtime config ctx) [:profiles db-id])]
                  (if (nil? profile)
                    (json-response! reply 404 {:detail "Database profile not found"})
                    (if-not (profile-can-access? profile ctx session-id)
                      (json-response! reply 403 {:detail "Database profile is outside the current Knoxx scope"})
                      (let [updated (assoc profile
                                           :privateToSession true
                                           :ownerSessionId session-id)]
                        (swap! database-state* assoc-in [:profiles db-id] updated)
                        (json-response! reply 200 updated))))))))

  (route! app "GET" "/api/knoxx/proxy/*"
          (fn [request reply]
            (let [path (aget request "params" "*")
                  target-url (str (:knoxx-base-url config) "/api/" path (request-query-string request))]
              (-> (forward-knoxx-request! config request "GET" path nil)
                  (.then (fn [resp]
                           (send-fetch-response! reply resp)))
                  (.catch (fn [err]
                            (json-response! reply 502 {:detail (str "Proxy request failed: " err)})))))))

  (route! app "POST" "/api/knoxx/proxy/*"
          (fn [request reply]
            (let [path (aget request "params" "*")]
              (-> (forward-knoxx-request! config request "POST" path nil)
                  (.then (fn [resp]
                           (send-fetch-response! reply resp)))
                  (.catch (fn [err]
                            (json-response! reply 502 {:detail (str "Proxy request failed: " err)})))))))

  (route! app "PUT" "/api/knoxx/proxy/*"
          (fn [request reply]
            (let [path (aget request "params" "*")]
              (-> (forward-knoxx-request! config request "PUT" path nil)
                  (.then (fn [resp]
                           (send-fetch-response! reply resp)))
                  (.catch (fn [err]
                            (json-response! reply 502 {:detail (str "Proxy request failed: " err)})))))))

  (route! app "PATCH" "/api/knoxx/proxy/*"
          (fn [request reply]
            (let [path (aget request "params" "*")]
              (-> (forward-knoxx-request! config request "PATCH" path nil)
                  (.then (fn [resp]
                           (send-fetch-response! reply resp)))
                  (.catch (fn [err]
                            (json-response! reply 502 {:detail (str "Proxy request failed: " err)})))))))

  (route! app "DELETE" "/api/knoxx/proxy/*"
          (fn [request reply]
            (let [path (aget request "params" "*")]
              (-> (forward-knoxx-request! config request "DELETE" path nil)
                  (.then (fn [resp]
                           (send-fetch-response! reply resp)))
                  (.catch (fn [err]
                            (json-response! reply 502 {:detail (str "Proxy request failed: " err)})))))))

  (route! app "GET" "/api/knoxx/health"
          (fn [_request reply]
            (json-response! reply 200 {:reachable true
                                       :configured true
                                       :base_url (:knoxx-base-url config)
                                       :status_code 200
                                       :details {:mode "shadow-cljs-pi-sdk"
                                                 :status "ok"
                                                 :project (:project-name config)
                                                 :collection {:name (:collection-name config)
                                                              :pointsCount nil}}})))

  (route! app "POST" "/api/knoxx/chat"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "agent.chat.use"))
                (let [body (assoc (normalize-chat-body (or (aget request "body") #js {}))
                                  :mode "rag"
                                  :auth-context ctx)]
                  (-> (send-agent-turn! runtime config body)
                      (.then (fn [resp]
                               (json-response! reply 200 resp)))
                      (.catch (fn [err]
                                (error-response! reply err 502)))))))))

  (route! app "POST" "/api/knoxx/chat/start"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "agent.chat.use"))
                (let [node-crypto (aget runtime "crypto")
                      parsed (normalize-chat-body (or (aget request "body") #js {}))
                      conversation-id (or (:conversation-id parsed) (.randomUUID node-crypto))
                      run-id (or (:run-id parsed) (.randomUUID node-crypto))
                      body (assoc parsed :conversation-id conversation-id :run-id run-id :mode "rag" :auth-context ctx)]
                  (-> (send-agent-turn! runtime config body)
                      (.then (fn [_] nil))
                      (.catch (fn [err]
                                (.error js/console "Async agent chat failed" err))))
                  (json-response! reply 202 {:ok true
                                             :queued true
                                             :run_id run-id
                                             :conversation_id conversation-id
                                             :session_id (:session-id body)
                                             :model (or (:model body) (:llmModel @settings-state*))}))))))

  (route! app "POST" "/api/knoxx/direct"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "agent.chat.use"))
                (let [body (assoc (normalize-chat-body (or (aget request "body") #js {}))
                                  :mode "direct"
                                  :auth-context ctx)]
                  (-> (send-agent-turn! runtime config body)
                      (.then (fn [resp]
                               (json-response! reply 200 resp)))
                      (.catch (fn [err]
                                (error-response! reply err 502)))))))))

  (route! app "POST" "/api/knoxx/direct/start"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "agent.chat.use"))
                (let [node-crypto (aget runtime "crypto")
                      parsed (normalize-chat-body (or (aget request "body") #js {}))
                      conversation-id (or (:conversation-id parsed) (.randomUUID node-crypto))
                      run-id (or (:run-id parsed) (.randomUUID node-crypto))
                      body (assoc parsed :conversation-id conversation-id :run-id run-id :mode "direct" :auth-context ctx)]
                  (-> (send-agent-turn! runtime config body)
                      (.then (fn [_] nil))
                      (.catch (fn [err]
                                (.error js/console "Async direct agent chat failed" err))))
                  (json-response! reply 202 {:ok true
                                             :queued true
                                             :run_id run-id
                                             :conversation_id conversation-id
                                             :session_id (:session-id body)
                                             :model (or (:model body) (:llmModel @settings-state*))}))))))

  (route! app "POST" "/api/knoxx/steer"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "agent.controls.steer"))
                (let [body (assoc (normalize-control-body (or (aget request "body") #js {})) :kind "steer")]
                  (ensure-conversation-access! ctx (:conversation-id body))
                  (-> (queue-agent-control! runtime config body)
                      (.then (fn [resp]
                               (json-response! reply 200 resp)))
                      (.catch (fn [err]
                                (error-response! reply err 409)))))))))

  (route! app "POST" "/api/knoxx/follow-up"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "agent.controls.follow_up"))
                (let [body (assoc (normalize-control-body (or (aget request "body") #js {})) :kind "follow_up")]
                  (ensure-conversation-access! ctx (:conversation-id body))
                  (-> (queue-agent-control! runtime config body)
                      (.then (fn [resp]
                               (json-response! reply 200 resp)))
                      (.catch (fn [err]
                                (error-response! reply err 409)))))))))

  (route! app "POST" "/api/shibboleth/handoff"
          (fn [request reply]
            (let [body (or (aget request "body") #js {})]
              (if (str/blank? (:shibboleth-base-url config))
                (json-response! reply 503 {:detail "SHIBBOLETH_BASE_URL is not configured"})
                (let [payload #js {:source_app "knoxx"
                                   :model (aget body "model")
                                   :system_prompt (aget body "system_prompt")
                                   :provider (aget body "provider")
                                   :conversation_id (aget body "conversation_id")
                                   :fake_tools_enabled (boolean (aget body "fake_tools_enabled"))
                                   :items (or (aget body "items") #js [])}]
                  (-> (fetch-json (str (:shibboleth-base-url config) "/api/chat/import")
                                  #js {:method "POST"
                                       :headers #js {"Content-Type" "application/json"}
                                       :body (.stringify js/JSON payload)})
                      (.then (fn [resp]
                               (if (aget resp "ok")
                                 (let [data (aget resp "body")
                                       session (or (aget data "session") #js {})
                                       session-id (str (or (aget session "id") ""))
                                       ui-url (if (and (not (str/blank? session-id))
                                                       (not (str/blank? (:shibboleth-ui-url config))))
                                                (with-query-param (rewrite-localhost-url (:shibboleth-ui-url config) request)
                                                  "session"
                                                  session-id)
                                                "")]
                                   (if (str/blank? session-id)
                                     (json-response! reply 502 {:detail "Shibboleth import did not return a session id"})
                                     (json-response! reply 200 {:ok true
                                                                :session_id session-id
                                                                :ui_url ui-url
                                                                :imported_item_count (count (js-array-seq (aget body "items")))})))
                                 (json-response! reply 502 {:detail (str "Shibboleth import failed: " (or (aget (aget resp "body") "raw") (js/JSON.stringify (aget resp "body"))))}))))
                      (.catch (fn [err]
                                (json-response! reply 502 {:detail (str "Shibboleth is unreachable: " err)})))))))))))

(defn config-js
  []
  (clj->js (cfg)))

(defn register-app-routes!
  [runtime app]
  (let [config (cfg)]
    (ensure-settings! config)
    (register-routes! runtime app config)
    (clj->js config)))

(defn start!
  [runtime]
  (when-not @server*
    (let [config (cfg)
          Fastify (aget runtime "Fastify")
          fastify-cors (aget runtime "fastifyCors")
          app (Fastify #js {:logger true})]
      (ensure-settings! config)
      (-> (.register app fastify-cors #js {:origin true})
          (.then (fn []
                   (.register app (aget runtime "fastifyWebsocket"))))
          (.then (fn []
                   (.register app
                              (fn [instance _opts done]
                                (register-ws-routes! runtime instance)
                                (done)))))
          (.then (fn []
                   (register-routes! runtime app config)
                   (.listen app #js {:host (:host config)
                                     :port (:port config)})))
          (.then (fn [_]
                   (reset! server* app)
                   (.log.info app (str "Knoxx backend CLJS listening on " (:host config) ":" (:port config)))))
          (.catch (fn [err]
                    (.error js/console "Knoxx backend CLJS failed to start" err)
                    (js/process.exit 1)))))))
