(ns kms-ingestion.drivers.opencode-sessions
  "Driver for ingesting OpenCode sessions through the OpenCode HTTP API.

   Preferred path: run one OpenCode server for the user account and read sessions
   via /experimental/session plus /session/:id/message. The upstream server stores
   sessions in the user's global OpenCode database, while the experimental session
   list endpoint uses Session.listGlobal, so it can enumerate sessions across
   projects that have been opened by OpenCode."
  (:require
   [cheshire.core :as json]
   [clj-http.client :as http]
   [clojure.string :as str]
   [kms-ingestion.drivers.protocol :as protocol])
  (:import
   [java.net URLEncoder]
   [java.time Instant]))

(def ^:private default-base-url
  (or (System/getenv "OPENCODE_SERVER_URL")
      "http://127.0.0.1:4096"))

(def ^:private default-session-project
  (or (System/getenv "OPENCODE_SESSION_PROJECT")
      "knoxx-session"))

(def ^:private max-text-length 12000)
(def ^:private preview-length 2000)

(declare fetch-opencode-sessions)

(defn- cfg
  [m & ks]
  (some #(get m %) ks))

(defn- parse-int*
  [value default]
  (try
    (cond
      (nil? value) default
      (number? value) (long value)
      (string? value) (Long/parseLong value)
      :else default)
    (catch Exception _ default)))

(defn- parse-bool*
  [value default]
  (cond
    (nil? value) default
    (boolean? value) value
    (string? value) (contains? #{"1" "true" "yes" "y" "on"}
                               (str/lower-case (str/trim value)))
    :else (boolean value)))

(defn- clean-base-url
  [base-url]
  (str/replace (or base-url default-base-url) #"/+$" ""))

(defn- truncate-text
  ([text] (truncate-text text max-text-length))
  ([text max-len]
   (let [s (when (some? text) (str text))]
     (if (and s (> (count s) max-len))
       (str (subs s 0 max-len) "\n... [truncated " (- (count s) max-len) " chars]")
       s))))

(defn- json-preview
  ([value] (json-preview value preview-length))
  ([value max-len]
   (truncate-text
    (try
      (if (string? value) value (json/generate-string value))
      (catch Exception _ (str value)))
    max-len)))

(defn- ms->iso
  [value]
  (try
    (cond
      (number? value) (str (Instant/ofEpochMilli (long value)))
      (string? value) value
      :else (str (Instant/now)))
    (catch Exception _
      (str (Instant/now)))))

(defn- parse-json-body
  [body]
  (cond
    (nil? body) nil
    (string? body) (if (str/blank? body) nil (json/parse-string body true))
    :else body))

(defn- url-encode
  [s]
  (URLEncoder/encode (str s) "UTF-8"))

(defn- keep-query-params
  [params]
  (into {}
        (keep (fn [[k v]]
                (when-not (or (nil? v) (and (string? v) (str/blank? v)))
                  [k v])))
        params))

(defn- response-header
  [resp header-name]
  (let [headers (:headers resp)
        lower (str/lower-case header-name)]
    (or (get headers header-name)
        (get headers lower)
        (get headers (keyword header-name))
        (get headers (keyword lower)))))

(defn- auth-options
  [{:keys [username password]}]
  (let [password (or password (System/getenv "OPENCODE_SERVER_PASSWORD"))
        username (or username (System/getenv "OPENCODE_SERVER_USERNAME") "opencode")]
    (cond-> {}
      (not (str/blank? password))
      (assoc :basic-auth [username password]))))

(defn- opencode-get!
  [{:keys [base-url username password connection-timeout-ms socket-timeout-ms]} path query-params]
  (let [url (str base-url path)
        opts (merge
              {:headers {"Accept" "application/json"}
               :query-params (keep-query-params query-params)
               :as :text
               :throw-exceptions false
               :connection-timeout connection-timeout-ms
               :socket-timeout socket-timeout-ms}
              (auth-options {:username username :password password}))
        resp (http/get url opts)
        status (:status resp)]
    (if (<= 200 status 299)
      (assoc resp :body-json (parse-json-body (:body resp)))
      (throw (ex-info (str "OpenCode HTTP " status " for " path)
                      {:status status
                       :path path
                       :body (:body resp)})))))

(defn- session-content-hash
  [session]
  (str (:id session)
       ":" (or (get-in session [:time :updated]) 0)
       ":" (or (:title session) "")
       ":" (or (:parentID session) "")
       ":" (or (get-in session [:time :archived]) "")))

(defn- session-file-id
  [session-id]
  (str "opencode-session:" session-id))

(defn- file-id->session-id
  [file-id]
  (str/replace-first (str file-id) #"^opencode-session:" ""))

(defn- existing-content-hash
  [row]
  (or (:content_hash row) (:content-hash row)))

(defn- session->file-entry
  [session]
  (let [updated-ms (get-in session [:time :updated])
        session-id (:id session)]
    {:id (session-file-id session-id)
     :path (str "opencode://session/" session-id)
     :session-id session-id
     :content-hash (session-content-hash session)
     :size 0
     :modified-at (if updated-ms
                    (Instant/ofEpochMilli (long updated-ms))
                    (Instant/now))
     :metadata {:opencode_session_id session-id
                :opencode_project_id (:projectID session)
                :directory (:directory session)
                :title (:title session)
                :updated updated-ms}}))

(defn- model-label
  [info]
  (let [provider (or (:providerID info) (get-in info [:model :providerID]))
        model (or (:modelID info) (get-in info [:model :modelID]))]
    (str (or provider "unknown") "/" (or model "unknown"))))

(defn- base-message-extra
  [session info]
  (cond-> {:opencode_session_id (:id session)
           :opencode_message_id (:id info)
           :opencode_project_id (:projectID session)
           :workspace (:directory session)
           :role (:role info)
           :agent (:agent info)
           :variant (:variant info)}
    (:parentID info) (assoc :opencode_parent_id (:parentID info))
    (:providerID info) (assoc :provider_id (:providerID info))
    (:modelID info) (assoc :model_id (:modelID info))
    (get-in info [:model :providerID]) (assoc :provider_id (get-in info [:model :providerID]))
    (get-in info [:model :modelID]) (assoc :model_id (get-in info [:model :modelID]))
    (:tokens info) (assoc :tokens (:tokens info))
    (:cost info) (assoc :cost (:cost info))
    (:finish info) (assoc :finish (:finish info))
    (:summary info) (assoc :summary (:summary info))))

(defn- make-event
  [session-project id ts kind session-id message-id text meta extra]
  {:schema "openplanner.event.v1"
   :id id
   :ts (ms->iso ts)
   :source "opencode-session-ingester"
   :kind kind
   :source_ref (cond-> {:project session-project
                        :session session-id}
                 message-id (assoc :message message-id))
   :text (truncate-text text)
   :meta meta
   :extra extra})

(defn- map-session-event
  [session-project session]
  (let [session-id (:id session)
        project (:project session)]
    [(make-event
      session-project
      (str "opencode:" session-id ":session")
      (get-in session [:time :created])
      "opencode.session_start"
      session-id
      nil
      (str "OpenCode session started in " (or (:directory session) (:worktree project) "unknown")
           (when-let [title (:title session)] (str "\nTitle: " title)))
      {:role "system" :author "opencode"}
      {:opencode_session_id session-id
       :opencode_slug (:slug session)
       :opencode_project_id (:projectID session)
       :opencode_project project
       :workspace (:directory session)
       :title (:title session)
       :parent_id (:parentID session)
       :time (:time session)})]))

(defn- text-content
  [parts]
  (->> parts
       (filter #(= "text" (:type %)))
       (map :text)
       (remove str/blank?)
       (str/join "\n")))

(defn- message-meta
  [role info]
  {:role role
   :author (if (= "user" role) "user" "opencode")
   :model (model-label info)})

(defn- map-text-event
  [session-project session info parts]
  (let [text (text-content parts)
        role (:role info)
        message-id (:id info)]
    (when-not (str/blank? text)
      (make-event
       session-project
       (str "opencode:" (:id session) ":msg:" message-id)
       (get-in info [:time :created])
       "opencode.message"
       (:id session)
       message-id
       text
       (message-meta role info)
       (base-message-extra session info)))))

(defn- map-error-event
  [session-project session info]
  (when-let [error (:error info)]
    (make-event
     session-project
     (str "opencode:" (:id session) ":error:" (:id info))
     (or (get-in info [:time :completed]) (get-in info [:time :created]))
     "opencode.error"
     (:id session)
     (:id info)
     (str "OpenCode assistant error: " (or (:message error) (:name error) (json-preview error 500)))
     {:role "system" :author "opencode" :model (model-label info)}
     (assoc (base-message-extra session info)
            :error error))))

(defn- map-reasoning-event
  [session-project session info part]
  (when-not (str/blank? (:text part))
    (make-event
     session-project
     (str "opencode:" (:id session) ":reasoning:" (:id part))
     (or (get-in part [:time :start]) (get-in info [:time :created]))
     "opencode.reasoning"
     (:id session)
     (:id info)
     (:text part)
     {:role "system" :author "opencode" :model (model-label info)}
     (assoc (base-message-extra session info)
            :part_id (:id part)
            :part_type "reasoning"))))

(defn- tool-event-text
  [part]
  (let [state (:state part)
        status (:status state)
        input-preview (json-preview (:input state) preview-length)
        output-preview (or (some-> (:output state) (truncate-text preview-length))
                           (some-> (:error state) (truncate-text preview-length)))]
    (str "Tool: " (or (:tool part) "unknown")
         "\nStatus: " (or status "unknown")
         (when-let [title (:title state)] (str "\nTitle: " title))
         (when-not (str/blank? input-preview) (str "\nInput:\n" input-preview))
         (when-not (str/blank? output-preview) (str "\nOutput:\n" output-preview)))))

(defn- map-tool-event
  [session-project session info part]
  (let [state (:state part)]
    (make-event
     session-project
     (str "opencode:" (:id session) ":tool:" (:id part))
     (or (get-in state [:time :start]) (get-in info [:time :created]))
     "opencode.tool_call"
     (:id session)
     (:id info)
     (tool-event-text part)
     {:role "system" :author "opencode" :model (model-label info)}
     (assoc (base-message-extra session info)
            :part_id (:id part)
            :part_type "tool"
            :tool_name (:tool part)
            :tool_call_id (:callID part)
            :tool_status (:status state)
            :tool_input_preview (json-preview (:input state) preview-length)
            :tool_output_preview (or (some-> (:output state) (truncate-text preview-length))
                                     (some-> (:error state) (truncate-text preview-length)))))))

(defn- interesting-part-text
  [part]
  (case (:type part)
    "step-finish" (str "Step finished: " (:reason part)
                       "\nCost: " (or (:cost part) 0)
                       "\nTokens: " (json-preview (:tokens part) 500))
    "patch" (str "Patch: " (str/join ", " (:files part)))
    "file" (str "File attachment: " (or (:filename part) (:url part) "file")
                "\nMIME: " (:mime part))
    "agent" (str "Agent mention: " (:name part))
    "subtask" (str "Subtask: " (or (:description part) (:prompt part))
                   (when-let [agent (:agent part)] (str "\nAgent: " agent)))
    "compaction" (str "Compaction marker" (when (contains? part :auto) (str ": auto=" (:auto part))))
    "retry" (str "Retry attempt " (:attempt part) "\n" (json-preview (:error part) preview-length))
    nil))

(defn- map-interesting-part-event
  [session-project session info part]
  (when-let [text (interesting-part-text part)]
    (make-event
     session-project
     (str "opencode:" (:id session) ":part:" (:id part))
     (or (get-in part [:time :created])
         (get-in info [:time :completed])
         (get-in info [:time :created]))
     (str "opencode.part." (:type part))
     (:id session)
     (:id info)
     text
     {:role "system" :author "opencode" :model (model-label info)}
     (assoc (base-message-extra session info)
            :part_id (:id part)
            :part_type (:type part)
            :part (dissoc part :text :url :snapshot)))))

(defn- map-message-events
  [session-project session message]
  (let [info (:info message)
        parts (vec (:parts message))
        text-event (map-text-event session-project session info parts)
        error-event (map-error-event session-project session info)
        part-events (mapcat
                     (fn [part]
                       (case (:type part)
                         "reasoning" (keep identity [(map-reasoning-event session-project session info part)])
                         "tool" [(map-tool-event session-project session info part)]
                         (keep identity [(map-interesting-part-event session-project session info part)])))
                     parts)]
    (vec (keep identity (concat [text-event error-event] part-events)))))

(defn- map-opencode-session-events
  [session-project session messages]
  (vec (concat
        (map-session-event session-project session)
        (mapcat #(map-message-events session-project session %) messages))))

(defrecord OpenCodeSessionsDriver [config state]
  protocol/Driver

  (discover [this opts]
    (let [existing-state (or (:existing-state opts) {})
          sessions (fetch-opencode-sessions this)
          entries (map session->file-entry sessions)
          tagged (map (fn [entry]
                        (let [existing (get existing-state (:id entry))
                              old-hash (existing-content-hash existing)
                              status (cond
                                       (nil? existing) :new
                                       (not= old-hash (:content-hash entry)) :changed
                                       :else :unchanged)]
                          (assoc entry :status status)))
                      entries)
          by-status (group-by :status tagged)
          changed (concat (:new by-status) (:changed by-status))]
      {:total-files (count tagged)
       :new-files (count (:new by-status))
       :changed-files (count (:changed by-status))
       :deleted-files 0
       :unchanged-files (count (:unchanged by-status))
       :files changed}))

  (extract [this file-id]
    (try
      (let [session-id (file-id->session-id file-id)
            session-resp (opencode-get! config (str "/session/" (url-encode session-id)) {})
            session (:body-json session-resp)
            message-limit (cfg config :message-limit :message_limit)
            messages-resp (opencode-get! config
                                         (str "/session/" (url-encode session-id) "/message")
                                         (cond-> {}
                                           message-limit (assoc :limit message-limit)))
            messages (or (:body-json messages-resp) [])
            events (map-opencode-session-events (:session-project config) session messages)]
        {:id file-id
         :path (str "opencode://session/" session-id)
         :content (json/generate-string {:session-id session-id
                                          :directory (:directory session)
                                          :events events})
         :content-hash (session-content-hash session)})
      (catch Exception e
        {:id file-id
         :path (str "opencode://session/" (file-id->session-id file-id))
         :content nil
         :error (.getMessage e)})))

  (extract-batch [this file-ids]
    (map #(protocol/extract this %) file-ids))

  (get-state [_]
    @state)

  (set-state [_ new-state]
    (reset! state new-state))

  (close [_]))

(defn fetch-opencode-sessions
  "Fetch OpenCode sessions across projects using /experimental/session."
  [^OpenCodeSessionsDriver driver]
  (let [{:keys [config]} driver
        page-size (parse-int* (:page-size config) 100)
        max-sessions (parse-int* (:max-sessions config) 1000)
        include-archived? (parse-bool* (:include-archived? config) true)
        roots? (:roots? config)
        directory (:directory config)
        search (:search config)]
    (loop [cursor nil
           acc []]
      (let [remaining (- max-sessions (count acc))]
        (if (<= remaining 0)
          acc
          (let [limit (max 1 (min page-size remaining))
                resp (opencode-get! config
                                    "/experimental/session"
                                    (cond-> {:limit limit
                                             :archived include-archived?}
                                      directory (assoc :directory directory)
                                      (some? roots?) (assoc :roots roots?)
                                      search (assoc :search search)
                                      cursor (assoc :cursor cursor)))
                page (vec (or (:body-json resp) []))
                next-cursor (response-header resp "x-next-cursor")
                acc* (into acc page)]
            (if (and next-cursor (seq page) (< (count acc*) max-sessions))
              (recur next-cursor acc*)
              acc*)))))))

(defn create-driver
  "Create an OpenCode sessions driver from config."
  [config]
  (let [base-url (clean-base-url (cfg config :base-url :base_url))
        username (or (cfg config :username :user) (System/getenv "OPENCODE_SERVER_USERNAME"))
        password (or (cfg config :password) (System/getenv "OPENCODE_SERVER_PASSWORD"))
        page-size (parse-int* (cfg config :page-size :page_size) 100)
        max-sessions (parse-int* (cfg config :max-sessions :max_sessions) 1000)
        include-archived? (parse-bool* (cfg config :include-archived? :include_archived :archived) true)
        roots? (cfg config :roots? :roots)
        directory (cfg config :directory)
        search (cfg config :search)
        message-limit (cfg config :message-limit :message_limit)
        session-project (or (cfg config :session-project :session_project) default-session-project)
        connection-timeout-ms (parse-int* (cfg config :connection-timeout-ms :connection_timeout_ms) 10000)
        socket-timeout-ms (parse-int* (cfg config :socket-timeout-ms :socket_timeout_ms) 120000)]
    (->OpenCodeSessionsDriver
     {:base-url base-url
      :username username
      :password password
      :page-size page-size
      :max-sessions max-sessions
      :include-archived? include-archived?
      :roots? roots?
      :directory directory
      :search search
      :message-limit message-limit
      :session-project session-project
      :connection-timeout-ms connection-timeout-ms
      :socket-timeout-ms socket-timeout-ms}
     (atom {}))))
