(ns knoxx.backend.agent-hydration
  (:require [clojure.string :as str]
            [knoxx.backend.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.core-memory :refer [fetch-openplanner-session-rows! filter-authorized-memory-hits! session-visible?]]
            [knoxx.backend.document-state :refer [active-agent-profile ensure-dir! list-files-recursive! normalize-relative-path indexed-meta]]
            [knoxx.backend.event-agents :as event-agents]
            [knoxx.backend.http :as backend-http :refer [openplanner-enabled? http-error js-array-seq]]
            [knoxx.backend.openplanner-memory :refer [openplanner-memory-search! openplanner-graph-query! openplanner-semantic-search!]]
            [knoxx.backend.runtime-config :as runtime-config :refer [default-settings]]
            [knoxx.backend.text :refer [search-tokens text-like-path? clip-text semantic-score snippet-around value->preview-text tool-text-result semantic-search-result-text semantic-read-result-text openplanner-memory-search-text openplanner-semantic-search-text openplanner-session-text graph-query-result-text websearch-result-text]]))

(defonce settings-state* (atom nil))

(defn ensure-settings!
  [config]
  (when-not @settings-state*
    (reset! settings-state* (default-settings config)))
  @settings-state*)

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
                                        :docsPath docs-path}
                             :query query
                             :tokens tokens
                             :results ranked})))))))))

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
                                 :name (:name profile)}
                     :path rel-path
                     :truncated truncated?
                     :content clipped}))))))))

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
                                               (maybe-tool-update! on-update "Searching corpus via OpenPlanner…")
                                               (if (openplanner-enabled? config)
                                                 (-> (openplanner-semantic-search! config {:query query
                                                                                           :k (or top-k 5)})
                                                     (.then (fn [result]
                                                              (tool-text-result (openplanner-semantic-search-text result) result))))
                                                 (-> (semantic-search-documents! runtime config {:query query
                                                                                                :top-k top-k
                                                                                                :max-snippet-chars max-snippet-chars} auth-context)
                                                     (.then (fn [result]
                                                              (tool-text-result (semantic-search-result-text result) result)))))))}

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

(defn- live-config
  [config]
  (or @runtime-config/config* config))

(defn- discord-gateway-manager
  []
  (aget js/globalThis "knoxxDiscordGateway"))

(defn- discord-gateway-started?
  []
  (let [manager (discord-gateway-manager)]
    (when (some? manager)
      (let [status (.status manager)]
        (boolean (or (aget status "ready")
                     (aget status "started")))))))

(defn- discord-token
  [config]
  (let [token (:discord-bot-token (live-config config))]
    (when (str/blank? token)
      (throw (js/Error. "Discord bot token not configured. Set DISCORD_BOT_TOKEN in the admin panel.")))
    token))

(defn- discord-rest-headers
  [token]
  #js {"Authorization" (str "Bot " token)
       "Content-Type" "application/json"})

(defn- discord-query-url
  [base params]
  (let [search (js/URLSearchParams.)]
    (doseq [[k v] params]
      (when-not (or (nil? v) (and (string? v) (str/blank? v)))
        (.append search (name k) (str v))))
    (let [query (.toString search)]
      (if (str/blank? query)
        base
        (str base "?" query)))))

(defn- discord-fetch-json!
  [url options]
  (-> (js/fetch url options)
      (.then (fn [resp]
               (if (.-ok resp)
                 (.json resp)
                 (-> (.text resp)
                     (.then (fn [text]
                              (throw (js/Error. (str "Discord API error " (.-status resp) ": " text)))))))))))

(defn- discord-attachments
  [message]
  (->> (if (array? (aget message "attachments")) (array-seq (aget message "attachments")) [])
       (mapv (fn [attachment]
               {:id (or (aget attachment "id") "")
                :filename (or (aget attachment "filename") "")
                :contentType (aget attachment "content_type")
                :size (or (aget attachment "size") 0)
                :url (or (aget attachment "url") "")}))))

(defn- discord-embeds
  [message]
  (->> (if (array? (aget message "embeds")) (array-seq (aget message "embeds")) [])
       (mapv (fn [embed]
               {:title (aget embed "title")
                :description (aget embed "description")
                :url (aget embed "url")}))))

(defn- discord-message->map
  [message]
  {:id (or (aget message "id") "")
   :channelId (or (aget message "channel_id") "")
   :content (or (aget message "content") "")
   :authorId (or (aget message "author" "id") "")
   :authorUsername (or (aget message "author" "username") "unknown")
   :authorIsBot (boolean (aget message "author" "bot"))
   :timestamp (or (aget message "timestamp") "")
   :attachments (discord-attachments message)
   :embeds (discord-embeds message)})

(defn- discord-message-line
  [message]
  (let [content (str/trim (str (or (:content message) "")))
        attachments (:attachments message)
        attachment-text (when (seq attachments)
                          (str " attachments=" (str/join ", " (map :filename attachments))))
        embeds (:embeds message)
        embed-text (when (seq embeds)
                     (str " embeds=" (count embeds)))]
    (str "<" (:authorUsername message) "> "
         (if (str/blank? content) "[no text]" content)
         (or attachment-text "")
         (or embed-text ""))))

(defn- discord-messages-text
  [heading messages]
  (str heading
       (if (seq messages)
         (str "\n\n" (str/join "\n\n" (map discord-message-line messages)))
         "\n\nNo messages found.")))

(defn- discord-fetch-channel-messages!
  [config channel-id {:keys [limit before after around]}]
  (let [manager (discord-gateway-manager)]
    (when (str/blank? channel-id)
      (throw (js/Error. "channel_id is required")))
    (if (discord-gateway-started?)
      (-> (.fetchChannelMessages manager channel-id (clj->js {:limit (max 1 (min 100 (or limit 50)))
                                                              :before before
                                                              :after after
                                                              :around around}))
          (.then (fn [messages]
                   {:messages (js->clj messages :keywordize-keys true)
                    :count (count (js->clj messages))
                    :channelId channel-id})))
      (let [token (discord-token config)]
        (-> (discord-fetch-json!
             (discord-query-url (str "https://discord.com/api/v10/channels/" channel-id "/messages")
                                {:limit (max 1 (min 100 (or limit 50)))
                                 :before before
                                 :after after
                                 :around around})
             #js {:method "GET"
                  :headers (discord-rest-headers token)})
            (.then (fn [payload]
                     (let [messages (->> (if (array? payload) (array-seq payload) [])
                                         (map discord-message->map)
                                         vec)]
                       {:messages messages
                        :count (count messages)
                        :channelId channel-id}))))))))

(defn- discord-scroll-channel-messages!
  [config channel-id oldest-seen-id limit]
  (-> (discord-fetch-channel-messages! config channel-id {:limit limit :before oldest-seen-id})
      (.then (fn [result]
               (assoc result :oldestSeenId oldest-seen-id)))))

(defn- discord-open-dm-channel!
  [config user-id]
  (let [token (discord-token config)]
    (when (str/blank? user-id)
      (throw (js/Error. "user_id is required")))
    (discord-fetch-json!
     "https://discord.com/api/v10/users/@me/channels"
     #js {:method "POST"
          :headers (discord-rest-headers token)
          :body (.stringify js/JSON #js {:recipient_id user-id})})))

(defn- discord-fetch-dm-messages!
  [config user-id {:keys [limit before]}]
  (if (discord-gateway-started?)
    (-> (.fetchDmMessages (discord-gateway-manager) user-id (clj->js {:limit (or limit 50)
                                                                      :before before}))
        (.then (fn [result]
                 (let [mapped (js->clj result :keywordize-keys true)]
                   {:messages (:messages mapped)
                    :count (count (:messages mapped))
                    :dmChannelId (:dmChannelId mapped)
                    :userId user-id}))))
    (-> (discord-open-dm-channel! config user-id)
        (.then (fn [channel]
                 (let [channel-id (or (aget channel "id") "")]
                   (-> (discord-fetch-channel-messages! config channel-id {:limit limit :before before})
                       (.then (fn [result]
                                (assoc result :dmChannelId channel-id :userId user-id))))))))))

(defn- discord-search-messages!
  [config scope {:keys [channel-id user-id query limit before after]}]
  (let [scope (str/lower-case (str (or scope "channel")))]
    (-> (if (= scope "dm")
          (discord-fetch-dm-messages! config user-id {:limit 100 :before before})
          (discord-fetch-channel-messages! config channel-id {:limit 100 :before before :after after}))
        (.then (fn [result]
                 (let [needle (some-> query str str/lower-case)
                       author-id (some-> user-id str not-empty)
                       filtered (->> (:messages result)
                                     (filter (fn [message]
                                               (and (or (str/blank? (str (or needle "")))
                                                        (str/includes? (str/lower-case (str (:content message))) needle))
                                                    (or (nil? author-id)
                                                        (= author-id (:authorId message))))))
                                     (take (or limit 50))
                                     vec)]
                   {:messages filtered
                    :count (count filtered)
                    :scope scope
                    :channelId (:channelId result)
                    :dmChannelId (:dmChannelId result)
                    :source "client_side_filter"}))))))

(defn- discord-send-message!
  [config channel-id text reply-to]
  (let [manager (discord-gateway-manager)
        normalized (str/trim (str (or text "")))]
    (when (str/blank? channel-id)
      (throw (js/Error. "channel_id is required")))
    (when (str/blank? normalized)
      (throw (js/Error. "text is required")))
    (if (discord-gateway-started?)
      (-> (.sendMessage manager channel-id normalized reply-to)
          (.then (fn [result]
                   (js->clj result :keywordize-keys true))))
      (let [token (discord-token config)
            chunk-size 2000
            chunks (loop [remaining normalized acc []]
                     (if (<= (count remaining) chunk-size)
                       (conj acc remaining)
                       (let [slice (.lastIndexOf remaining "\n\n" chunk-size)
                             split-at (cond
                                        (> slice (int (* chunk-size 0.5))) slice
                                        :else chunk-size)]
                         (recur (str/trim (subs remaining split-at))
                                (conj acc (str/trim (subs remaining 0 split-at)))))))]
        (-> (reduce (fn [promise chunk]
                      (.then promise
                             (fn [state]
                               (discord-fetch-json!
                                (str "https://discord.com/api/v10/channels/" channel-id "/messages")
                                #js {:method "POST"
                                     :headers (discord-rest-headers token)
                                     :body (.stringify js/JSON
                                                       (clj->js (cond-> {:content chunk}
                                                                  (and reply-to (nil? (:messageId state)))
                                                                  (assoc :message_reference {:message_id reply-to}))))}))))
                    (js/Promise.resolve nil)
                    chunks)
            (.then (fn [result]
                     {:channelId channel-id
                      :messageId (or (aget result "id") "")
                      :sent true
                      :timestamp (or (aget result "timestamp") "")
                      :chunkCount (count chunks)})))))))

(defn- discord-list-guilds!
  [config]
  (if (discord-gateway-started?)
    (-> (.listServers (discord-gateway-manager))
        (.then (fn [servers]
                 (let [mapped (js->clj servers :keywordize-keys true)]
                   {:servers mapped
                    :count (count mapped)}))))
    (let [token (discord-token config)]
      (-> (discord-fetch-json! "https://discord.com/api/v10/users/@me/guilds"
                               #js {:method "GET"
                                    :headers (discord-rest-headers token)})
          (.then (fn [payload]
                   (let [servers (->> (if (array? payload) (array-seq payload) [])
                                      (mapv (fn [guild]
                                              {:id (or (aget guild "id") "")
                                               :name (or (aget guild "name") "")
                                               :memberCount (aget guild "approximate_member_count")})))]
                     {:servers servers
                      :count (count servers)})))))))

(defn- text-channel-type?
  [channel]
  (contains? #{0 5 11 12} (aget channel "type")))

(defn- discord-list-guild-channels!
  [config guild-id]
  (if (discord-gateway-started?)
    (-> (.listChannels (discord-gateway-manager) guild-id)
        (.then (fn [channels]
                 (let [mapped (js->clj channels :keywordize-keys true)]
                   {:channels mapped
                    :count (count mapped)}))))
    (let [token (discord-token config)]
      (-> (discord-fetch-json! (str "https://discord.com/api/v10/guilds/" guild-id "/channels")
                               #js {:method "GET"
                                    :headers (discord-rest-headers token)})
          (.then (fn [payload]
                   (let [channels (->> (if (array? payload) (array-seq payload) [])
                                       (filter text-channel-type?)
                                       (mapv (fn [channel]
                                               {:id (or (aget channel "id") "")
                                                :name (or (aget channel "name") "")
                                                :guildId guild-id
                                                :type (str (or (aget channel "type") ""))})))]
                     {:channels channels
                      :count (count channels)})))))))

(defn- discord-list-channels!
  [config guild-id]
  (if (str/blank? (str (or guild-id "")))
    (-> (discord-list-guilds! config)
        (.then (fn [result]
                 (-> (js/Promise.all
                      (clj->js (mapv (fn [server]
                                       (discord-list-guild-channels! config (:id server)))
                                     (:servers result))))
                     (.then (fn [payloads]
                              (let [channels (->> (js-array-seq payloads)
                                                  (mapcat :channels)
                                                  vec)]
                                {:channels channels
                                 :count (count channels)})))))))
    (discord-list-guild-channels! config guild-id)))

(defn- event-agent-status!
  [config]
  (let [live (live-config config)
        control (runtime-config/event-agent-control-config live)
        runtime (event-agents/status-snapshot live)]
    {:control control
     :runtime runtime}))

(defn- event-agent-dispatch!
  [source-kind event-kind payload]
  (event-agents/dispatch-event! {:sourceKind source-kind
                                 :eventKind event-kind
                                 :payload payload}))

(defn- event-agent-upsert-job!
  [config job-id job-patch]
  (let [live (live-config config)
        current-control (runtime-config/event-agent-control-config live)
        existing (some #(when (= (:id %) job-id) %) (:jobs current-control))
        next-job (merge existing job-patch {:id job-id})
        next-jobs (->> (:jobs current-control)
                       (remove #(= (:id %) job-id))
                       (cons next-job)
                       vec)
        next-control (runtime-config/event-agent-control-config (assoc live :event-agent-control (assoc current-control :jobs next-jobs)))]
    (swap! runtime-config/config*
           (fn [current]
             (assoc (or current (runtime-config/cfg))
                    :event-agent-control next-control)))
    (event-agents/reload!)
    {:job (some #(when (= (:id %) job-id) %) (:jobs next-control))
     :control next-control}))

(defn create-discord-custom-tools
  ([runtime config] (create-discord-custom-tools runtime config nil))
  ([runtime config auth-context]
   (let [Type (aget runtime "Type")
         allowed? (fn [tool-id]
                    (or (nil? auth-context)
                        (ctx-tool-allowed? auth-context tool-id)))
         json-parse (fn [text]
                      (try
                        (js->clj (.parse js/JSON text) :keywordize-keys true)
                        (catch :default err
                          (throw (js/Error. (str "Invalid JSON: " (.-message err)))))))
         send-params (.Object Type
                              #js {:channel_id (.String Type #js {:description "Discord channel ID to send the message to. Use discord.list.channels to discover IDs."})
                                   :text (.String Type #js {:description "Message content to send. Long messages will be chunked automatically."})
                                   :reply_to (.Optional Type (.String Type #js {:description "Optional message ID to reply to."}))})
         channel-messages-params (.Object Type
                                          #js {:channel_id (.String Type #js {:description "Discord channel ID to fetch messages from."})
                                               :limit (.Optional Type (.Number Type #js {:description "Maximum number of messages to fetch (default 50, max 100)." :minimum 1 :maximum 100}))
                                               :before (.Optional Type (.String Type #js {:description "Fetch messages before this message ID."}))
                                               :after (.Optional Type (.String Type #js {:description "Fetch messages after this message ID."}))
                                               :around (.Optional Type (.String Type #js {:description "Fetch messages around this message ID."}))})
         channel-scroll-params (.Object Type
                                        #js {:channel_id (.String Type #js {:description "Discord channel ID to fetch older messages from."})
                                             :oldest_seen_id (.String Type #js {:description "Oldest message ID already seen; fetch messages before this."})
                                             :limit (.Optional Type (.Number Type #js {:description "Maximum number of older messages to fetch." :minimum 1 :maximum 100}))})
         dm-messages-params (.Object Type
                                     #js {:user_id (.String Type #js {:description "Discord user ID whose DM channel should be read."})
                                          :limit (.Optional Type (.Number Type #js {:description "Maximum number of DM messages to fetch." :minimum 1 :maximum 100}))
                                          :before (.Optional Type (.String Type #js {:description "Fetch DM messages before this message ID."}))})
         search-params (.Object Type
                                #js {:scope (.String Type #js {:description "Search scope: channel or dm."})
                                     :channel_id (.Optional Type (.String Type #js {:description "Discord channel ID to search when scope=channel."}))
                                     :user_id (.Optional Type (.String Type #js {:description "Discord user ID to search against when scope=dm or to filter author."}))
                                     :query (.Optional Type (.String Type #js {:description "Optional substring query to filter messages by content."}))
                                     :limit (.Optional Type (.Number Type #js {:description "Maximum number of matching messages to return." :minimum 1 :maximum 100}))
                                     :before (.Optional Type (.String Type #js {:description "Fetch messages before this message ID."}))
                                     :after (.Optional Type (.String Type #js {:description "Fetch messages after this message ID."}))})
         list-channels-params (.Object Type
                                       #js {:guild_id (.Optional Type (.String Type #js {:description "Optional guild/server ID. If omitted, returns channels across all visible guilds."}))})
         empty-params (.Object Type #js {})
         status-params (.Object Type #js {})
         run-job-params (.Object Type
                                 #js {:job_id (.String Type #js {:description "Event-agent job id to run immediately."})})
         dispatch-params (.Object Type
                                  #js {:source_kind (.String Type #js {:description "Event source kind such as manual, discord, github, or cron."})
                                       :event_kind (.String Type #js {:description "Event kind string such as manual.note or discord.message.keyword."})
                                       :payload_json (.Optional Type (.String Type #js {:description "Optional JSON object payload for the event."}))})
         upsert-job-params (.Object Type
                                    #js {:job_id (.String Type #js {:description "Unique event-agent job id."})
                                         :job_json (.String Type #js {:description "JSON object describing the event-agent job. Fields may include name, description, enabled, trigger, source, filters, and agentSpec."})})

         discord-send-execute (fn [_tool-call-id params a b c]
                                (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                                      channel-id (or (aget params "channel_id") (aget params "channelId") "")
                                      text (or (aget params "text") (aget params "content") "")
                                      reply-to (or (aget params "reply_to") (aget params "replyTo"))]
                                  (maybe-tool-update! on-update (str "Sending Discord message to " channel-id "…"))
                                  (-> (discord-send-message! config channel-id text reply-to)
                                      (.then (fn [result]
                                               (tool-text-result (str "Sent Discord message " (:messageId result) " to channel " channel-id)
                                                                 result))))))

         discord-channel-messages-execute (fn [_tool-call-id params a b c]
                                            (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                                                  channel-id (or (aget params "channel_id") (aget params "channelId") "")]
                                              (maybe-tool-update! on-update (str "Fetching Discord messages from channel " channel-id "…"))
                                              (-> (discord-fetch-channel-messages! config channel-id {:limit (aget params "limit")
                                                                                                    :before (aget params "before")
                                                                                                    :after (aget params "after")
                                                                                                    :around (aget params "around")})
                                                  (.then (fn [result]
                                                           (tool-text-result (discord-messages-text (str "Fetched " (:count result) " messages from channel " channel-id ".") (:messages result))
                                                                             result))))))

         discord-channel-scroll-execute (fn [_tool-call-id params a b c]
                                          (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                                                channel-id (or (aget params "channel_id") (aget params "channelId") "")
                                                oldest-seen-id (or (aget params "oldest_seen_id") (aget params "oldestSeenId") "")]
                                            (maybe-tool-update! on-update (str "Scrolling older Discord messages in channel " channel-id "…"))
                                            (-> (discord-scroll-channel-messages! config channel-id oldest-seen-id (aget params "limit"))
                                                (.then (fn [result]
                                                         (tool-text-result (discord-messages-text (str "Fetched older messages before " oldest-seen-id ".") (:messages result))
                                                                           result))))))

         discord-dm-messages-execute (fn [_tool-call-id params a b c]
                                       (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                                             user-id (or (aget params "user_id") (aget params "userId") "")]
                                         (maybe-tool-update! on-update (str "Fetching Discord DM messages for user " user-id "…"))
                                         (-> (discord-fetch-dm-messages! config user-id {:limit (aget params "limit")
                                                                                         :before (aget params "before")})
                                             (.then (fn [result]
                                                      (tool-text-result (discord-messages-text (str "Fetched " (:count result) " DM messages for user " user-id ".") (:messages result))
                                                                        result))))))

         discord-search-execute (fn [_tool-call-id params a b c]
                                  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                                        scope (or (aget params "scope") "channel")
                                        channel-id (or (aget params "channel_id") (aget params "channelId"))
                                        user-id (or (aget params "user_id") (aget params "userId"))
                                        query (or (aget params "query") "")]
                                    (maybe-tool-update! on-update (str "Searching Discord messages in scope " scope "…"))
                                    (-> (discord-search-messages! config scope {:channel-id channel-id
                                                                                :user-id user-id
                                                                                :query query
                                                                                :limit (aget params "limit")
                                                                                :before (aget params "before")
                                                                                :after (aget params "after")})
                                        (.then (fn [result]
                                                 (tool-text-result (discord-messages-text (str "Found " (:count result) " matching Discord messages.") (:messages result))
                                                                   result))))))

         discord-list-servers-execute (fn [_tool-call-id _params a b c]
                                        (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))]
                                          (maybe-tool-update! on-update "Listing Discord servers…")
                                          (-> (discord-list-guilds! config)
                                              (.then (fn [result]
                                                       (let [lines (->> (:servers result)
                                                                        (map (fn [server] (str (:name server) " (" (:id server) ")")))
                                                                        (str/join "\n"))]
                                                         (tool-text-result (str "Discord servers:\n" lines) result)))))))

         discord-list-channels-execute (fn [_tool-call-id params a b c]
                                         (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                                               guild-id (or (aget params "guild_id") (aget params "guildId"))]
                                           (maybe-tool-update! on-update "Listing Discord channels…")
                                           (-> (discord-list-channels! config guild-id)
                                               (.then (fn [result]
                                                        (let [lines (->> (:channels result)
                                                                         (map (fn [channel] (str "#" (:name channel) " (" (:id channel) ") guild=" (:guildId channel))))
                                                                         (str/join "\n"))]
                                                          (tool-text-result (str "Discord channels:\n" lines) result)))))))

         event-agent-status-execute (fn [_tool-call-id _params a b c]
                                      (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                                            result (event-agent-status! config)
                                            control (:control result)
                                            runtime (:runtime result)]
                                        (maybe-tool-update! on-update "Reading event-agent runtime state…")
                                        (tool-text-result
                                         (str "Event-agent runtime running=" (:running runtime)
                                              ", jobs=" (count (:jobs runtime))
                                              "\n\n"
                                              (str/join "\n"
                                                        (map (fn [job]
                                                               (str (:id job) " :: trigger=" (get-in job [:trigger :kind])
                                                                    " cadence=" (get-in job [:trigger :cadenceMinutes])
                                                                    " enabled=" (:enabled job)))
                                                             (:jobs control))))
                                         result)))

         event-agent-run-job-execute (fn [_tool-call-id params a b c]
                                       (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                                             job-id (or (aget params "job_id") (aget params "jobId") "")]
                                         (when (str/blank? job-id)
                                           (throw (js/Error. "job_id is required")))
                                         (maybe-tool-update! on-update (str "Running event-agent job " job-id "…"))
                                         (-> (event-agents/run-job! job-id)
                                             (.then (fn [_]
                                                      (tool-text-result (str "Triggered event-agent job " job-id)
                                                                        {:jobId job-id
                                                                         :ok true}))))))

         event-agent-dispatch-execute (fn [_tool-call-id params a b c]
                                        (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                                              source-kind (or (aget params "source_kind") (aget params "sourceKind") "manual")
                                              event-kind (or (aget params "event_kind") (aget params "eventKind") "manual.event")
                                              payload-json (or (aget params "payload_json") (aget params "payloadJson") "")
                                              payload (if (str/blank? (str payload-json)) {} (json-parse (str payload-json)))]
                                          (maybe-tool-update! on-update (str "Dispatching event " event-kind "…"))
                                          (-> (event-agent-dispatch! source-kind event-kind payload)
                                              (.then (fn [result]
                                                       (tool-text-result (str "Dispatched event " event-kind " matched jobs: " (str/join ", " (:matchedJobs result)))
                                                                         result))))))

         ]

     (clj->js
      (vec
       (remove nil?
               [(when (allowed? "discord.publish")
                  (doto (js-obj)
                    (aset "name" "discord.publish")
                    (aset "label" "Discord Publish")
                    (aset "description" "Post a message to a Discord channel using the configured Knoxx Discord bot.")
                    (aset "promptSnippet" "Post updates, summaries, or notifications to Discord channels.")
                    (aset "promptGuidelines" (clj->js ["Use discord.publish or discord.send to share results in a Discord channel."
                                                       "Provide channelId/channel_id and content/text."
                                                       "If replying in-thread, prefer discord.send with reply_to."]))
                    (aset "parameters" (.Object Type #js {:channelId (.String Type #js {:description "Discord channel ID to post the message to."})
                                                           :content (.String Type #js {:description "Message content to post to the Discord channel."})}))
                    (aset "execute" (fn [tool-call-id params a b c]
                                       (discord-send-execute tool-call-id #js {:channel_id (aget params "channelId")
                                                                               :text (aget params "content")}
                                                             a b c)))))
                (when (allowed? "discord.send")
                  (doto (js-obj)
                    (aset "name" "discord.send")
                    (aset "label" "Discord Send")
                    (aset "description" "Send a message to a Discord channel, optionally as a reply to an existing message.")
                    (aset "promptSnippet" "Send a Discord message or reply to a specific message id.")
                    (aset "promptGuidelines" (clj->js ["Use discord.send to post or reply in channels once you know the channel id."
                                                       "Use discord.list.servers and discord.list.channels first if you need discovery."]))
                    (aset "parameters" send-params)
                    (aset "execute" discord-send-execute)))
                (when (allowed? "discord.read")
                  (doto (js-obj)
                    (aset "name" "discord.read")
                    (aset "label" "Discord Read")
                    (aset "description" "Read recent messages from a Discord channel.")
                    (aset "promptSnippet" "Read recent messages from a Discord channel to understand context.")
                    (aset "promptGuidelines" (clj->js ["Use discord.read as a simple alias for discord.channel.messages."
                                                       "For pagination or cursors, use discord.channel.messages or discord.channel.scroll directly."]))
                    (aset "parameters" (.Object Type #js {:channelId (.String Type #js {:description "Discord channel ID to read messages from."})
                                                           :limit (.Optional Type (.Number Type #js {:description "Maximum number of messages to return." :minimum 1 :maximum 100}))}))
                    (aset "execute" (fn [tool-call-id params a b c]
                                       (discord-channel-messages-execute tool-call-id #js {:channel_id (aget params "channelId")
                                                                                           :limit (aget params "limit")}
                                                                         a b c)))))
                (when (allowed? "discord.channel.messages")
                  (doto (js-obj)
                    (aset "name" "discord.channel.messages")
                    (aset "label" "Discord Channel Messages")
                    (aset "description" "Fetch messages from a Discord channel with before/after/around cursors.")
                    (aset "promptSnippet" "Fetch channel messages from Discord with pagination cursors when you need exact transcript context.")
                    (aset "promptGuidelines" (clj->js ["Use this when you know the channel id and need exact message history."
                                                       "Use before/after/around for precise pagination."]))
                    (aset "parameters" channel-messages-params)
                    (aset "execute" discord-channel-messages-execute)))
                (when (allowed? "discord.channel.scroll")
                  (doto (js-obj)
                    (aset "name" "discord.channel.scroll")
                    (aset "label" "Discord Channel Scroll")
                    (aset "description" "Scroll older channel messages by fetching messages before the oldest already-seen message id.")
                    (aset "promptSnippet" "Scroll backwards in a Discord channel once you already know the oldest seen id.")
                    (aset "promptGuidelines" (clj->js ["Use discord.channel.scroll as sugar over discord.channel.messages before=oldest_seen_id."
                                                       "Useful for paging backward through long histories."]))
                    (aset "parameters" channel-scroll-params)
                    (aset "execute" discord-channel-scroll-execute)))
                (when (allowed? "discord.dm.messages")
                  (doto (js-obj)
                    (aset "name" "discord.dm.messages")
                    (aset "label" "Discord DM Messages")
                    (aset "description" "Fetch messages from the DM channel shared with a Discord user.")
                    (aset "promptSnippet" "Read DM history with a Discord user by user id.")
                    (aset "promptGuidelines" (clj->js ["Use this when the relevant conversation is in DMs rather than a guild channel."
                                                       "Provide the target user id."]))
                    (aset "parameters" dm-messages-params)
                    (aset "execute" discord-dm-messages-execute)))
                (when (allowed? "discord.search")
                  (doto (js-obj)
                    (aset "name" "discord.search")
                    (aset "label" "Discord Search")
                    (aset "description" "Search channel or DM messages by content and/or author using client-side filtering.")
                    (aset "promptSnippet" "Search Discord messages by text and scope to find relevant discussion quickly.")
                    (aset "promptGuidelines" (clj->js ["Use scope=channel with channel_id for guild channels or scope=dm with user_id for DMs."
                                                       "This falls back to client-side filtering when native search is unavailable."]))
                    (aset "parameters" search-params)
                    (aset "execute" discord-search-execute)))
                (when (allowed? "discord.guilds")
                  (doto (js-obj)
                    (aset "name" "discord.guilds")
                    (aset "label" "Discord Guilds")
                    (aset "description" "List Discord guilds/servers the bot is in.")
                    (aset "promptSnippet" "List Discord guilds to discover available servers.")
                    (aset "promptGuidelines" (clj->js ["Alias for discord.list.servers."
                                                       "Use before listing channels or posting to a specific server."]))
                    (aset "parameters" empty-params)
                    (aset "execute" discord-list-servers-execute)))
                (when (allowed? "discord.list.servers")
                  (doto (js-obj)
                    (aset "name" "discord.list.servers")
                    (aset "label" "Discord List Servers")
                    (aset "description" "List all Discord servers/guilds the bot can access.")
                    (aset "promptSnippet" "List Discord servers before choosing channels or replying into a guild.")
                    (aset "promptGuidelines" (clj->js ["Use this before discord.list.channels when you need discovery."
                                                       "Do not guess guild ids."]))
                    (aset "parameters" empty-params)
                    (aset "execute" discord-list-servers-execute)))
                (when (allowed? "discord.channels")
                  (doto (js-obj)
                    (aset "name" "discord.channels")
                    (aset "label" "Discord Channels")
                    (aset "description" "List channels in a Discord guild.")
                    (aset "promptSnippet" "List channels in a Discord guild to find the right channel for reading or posting.")
                    (aset "promptGuidelines" (clj->js ["Alias for discord.list.channels."
                                                       "Use guildId/guild_id when you already know the server."]))
                    (aset "parameters" (.Object Type #js {:guildId (.String Type #js {:description "Discord guild ID to list channels for."})}))
                    (aset "execute" (fn [tool-call-id params a b c]
                                       (discord-list-channels-execute tool-call-id #js {:guild_id (aget params "guildId")}
                                                                      a b c)))))
                (when (allowed? "discord.list.channels")
                  (doto (js-obj)
                    (aset "name" "discord.list.channels")
                    (aset "label" "Discord List Channels")
                    (aset "description" "List channels in one Discord guild or across all visible guilds.")
                    (aset "promptSnippet" "List Discord channels to discover readable/postable targets.")
                    (aset "promptGuidelines" (clj->js ["If guild_id is omitted, returns channels across all visible guilds."
                                                       "Use returned channel ids with discord.channel.messages or discord.send."]))
                    (aset "parameters" list-channels-params)
                    (aset "execute" discord-list-channels-execute)))
                (when (allowed? "event_agents.status")
                  (doto (js-obj)
                    (aset "name" "event_agents.status")
                    (aset "label" "Event Agent Status")
                    (aset "description" "Inspect the current scheduled event-agent runtime configuration and live state.")
                    (aset "promptSnippet" "Inspect event-agent jobs, sources, and runtime state before changing schedules or dispatching events.")
                    (aset "promptGuidelines" (clj->js ["Use this before mutating jobs so you understand the current state."
                                                       "Helpful for debugging cron/event behavior or checking recent events."]))
                    (aset "parameters" status-params)
                    (aset "execute" event-agent-status-execute)))
                (when (allowed? "event_agents.dispatch")
                  (doto (js-obj)
                    (aset "name" "event_agents.dispatch")
                    (aset "label" "Event Agent Dispatch")
                    (aset "description" "Dispatch a structured event into the generic event-agent runtime.")
                    (aset "promptSnippet" "Dispatch manual or synthetic events so matching event-agent jobs can react immediately.")
                    (aset "promptGuidelines" (clj->js ["Use source_kind/manual for synthetic triggers you want to test immediately."
                                                       "Put complex payload fields into payload_json as a JSON object string."]))
                    (aset "parameters" dispatch-params)
                    (aset "execute" event-agent-dispatch-execute)))
                (when (allowed? "event_agents.run_job")
                  (doto (js-obj)
                    (aset "name" "event_agents.run_job")
                    (aset "label" "Event Agent Run Job")
                    (aset "description" "Run a configured event-agent job immediately without waiting for its schedule.")
                    (aset "promptSnippet" "Trigger an event-agent job now.")
                    (aset "promptGuidelines" (clj->js ["Use this for manual patrol/synthesis/response runs after inspecting status."
                                                       "Provide the exact job id."]))
                    (aset "parameters" run-job-params)
                    (aset "execute" event-agent-run-job-execute)))
                (when (allowed? "event_agents.upsert_job")
                  (let [tool (js-obj)]
                    (aset tool "name" "event_agents.upsert_job")
                    (aset tool "label" "Event Agent Upsert Job")
                    (aset tool "description" "Create or update a scheduled event-agent job, then reload the runtime.")
                    (aset tool "promptSnippet" "Create or update a generic scheduled event-agent job using JSON job config.")
                    (aset tool "promptGuidelines" (clj->js ["Use this to create new cron/event-driven agents or revise existing jobs."
                                                            "Pass a full JSON job object in job_json; include trigger, source, filters, and agentSpec when you need precise control."]))
                    (aset tool "parameters" upsert-job-params)
                    (aset tool "execute"
                          (fn [_tool-call-id params a b c]
                            (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                                  job-id (or (aget params "job_id") (aget params "jobId") "")
                                  job-json (or (aget params "job_json") (aget params "jobJson") "")]
                              (when (str/blank? job-id)
                                (throw (js/Error. "job_id is required")))
                              (when (str/blank? (str job-json))
                                (throw (js/Error. "job_json is required")))
                              (maybe-tool-update! on-update (str "Upserting event-agent job " job-id "…"))
                              (let [result (event-agent-upsert-job! config job-id (json-parse (str job-json)))]
                                (tool-text-result (str "Upserted event-agent job " job-id)
                                                  result)))))
                    tool))
                (when (allowed? "schedule_event_agent")
                  (let [tool (js-obj)]
                    (aset tool "name" "schedule_event_agent")
                    (aset tool "label" "Schedule Event Agent")
                    (aset tool "description" "Create or update a scheduled event-agent job with explicit trigger, source, prompts, and tool policies.")
                    (aset tool "promptSnippet" "Schedule an event-driven agent job that can react to Discord, GitHub, cron, or manual events.")
                    (aset tool "promptGuidelines" (clj->js ["Use this when the user wants to create or revise an event-based agent from conversation."
                                                            "Provide a full job object in job_json, including trigger, source, filters, and agentSpec."
                                                            "Use role slugs like translator, system_admin, or executive and include explicit toolPolicies so the scheduled agent has exactly the tools it needs."]))
                    (aset tool "parameters" upsert-job-params)
                    (aset tool "execute"
                          (fn [_tool-call-id params a b c]
                            (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                                  job-id (or (aget params "job_id") (aget params "jobId") "")
                                  job-json (or (aget params "job_json") (aget params "jobJson") "")]
                              (when (str/blank? job-id)
                                (throw (js/Error. "job_id is required")))
                              (when (str/blank? (str job-json))
                                (throw (js/Error. "job_json is required")))
                              (maybe-tool-update! on-update (str "Scheduling event-agent job " job-id "…"))
                              (let [result (event-agent-upsert-job! config job-id (json-parse (str job-json)))]
                                (tool-text-result (str "Scheduled event-agent job " job-id)
                                                  result)))))
                    tool))]))))))

(defn create-event-agent-custom-tools
  ([runtime config] (create-event-agent-custom-tools runtime config nil))
  ([runtime config auth-context]
   ;; Event-agent tools are created inside create-discord-custom-tools so the full Discord + scheduling suite
   ;; is loaded together. This helper remains for composition symmetry and future extraction.
   (clj->js [])))

(defn create-openplanner-custom-tools
  ([runtime config] (create-openplanner-custom-tools runtime config nil))
  ([runtime config auth-context]
   (let [Type (aget runtime "Type")
         search-params (.Object Type
                                #js {:query (.String Type #js {:description "Semantic memory search across prior Knoxx sessions and actions indexed in OpenPlanner."})
                                     :k (.Optional Type (.Number Type #js {:description "Maximum number of memory hits to return." :minimum 1 :maximum 8}))
                                     :sessionId (.Optional Type (.String Type #js {:description "Optional conversation/session id to scope the search."}))})
         graph-params (.Object Type
                               #js {:query (.String Type #js {:description "Search text for canonical graph nodes across OpenPlanner lakes."})
                                    :lake (.Optional Type (.String Type #js {:description "Optional lake/project filter such as devel, web, bluesky, or knoxx-session."}))
                                    :nodeType (.Optional Type (.String Type #js {:description "Optional node_type filter such as docs, code, visited, assistant_message, tool_result, or reasoning."}))
                                    :limit (.Optional Type (.Number Type #js {:description "Maximum number of graph nodes to return." :minimum 1 :maximum 20}))
                                    :edgeLimit (.Optional Type (.Number Type #js {:description "Maximum number of incident edges to include." :minimum 0 :maximum 60}))})
         websearch-params (.Object Type
                                   #js {:query (.String Type #js {:description "Live web search query routed through Proxx websearch."})
                                        :numResults (.Optional Type (.Number Type #js {:description "Maximum number of results to return." :minimum 1 :maximum 20}))
                                        :searchContextSize (.Optional Type (.String Type #js {:description "Search context size: low, medium, or high."}))
                                        :allowedDomains (.Optional Type (.Array Type (.String Type) #js {:description "Optional domain allowlist."}))
                                        :model (.Optional Type (.String Type #js {:description "Optional Proxx/OpenAI model override for search."}))})
         session-params (.Object Type
                                 #js {:sessionId (.String Type #js {:description "Knoxx conversation/session id stored in OpenPlanner."})})
         ;; save_translation params
         translation-params (.Object Type
                                     #js {:source_text (.String Type #js {:description "Original source text"})
                                          :translated_text (.String Type #js {:description "Translated text"})
                                          :source_lang (.String Type #js {:description "Source language code (e.g. 'en')"})
                                          :target_lang (.String Type #js {:description "Target language code (e.g. 'es')"})
                                          :document_id (.String Type #js {:description "Document ID being translated"})
                                          :garden_id (.Optional Type (.String Type #js {:description "Garden ID"}))
                                          :project (.Optional Type (.String Type #js {:description "Project name"}))
                                          :segment_index (.Number Type #js {:description "0-based segment index"})})
         create-file-params (.Object Type
                                    #js {:title (.Optional Type (.String Type #js {:description "Human-readable title for the new artifact."}))
                                         :path (.Optional Type (.String Type #js {:description "Relative path for the new file inside the active docs root."}))
                                         :content (.Optional Type (.String Type #js {:description "Initial markdown content to write into the new file."}))})
         node-fs (aget runtime "fs")
         node-path (aget runtime "path")
         slugify (fn [value]
                   (let [raw (-> (str (or value "untitled-canvas"))
                                 str/lower-case
                                 (str/replace #"[^a-z0-9]+" "-")
                                 (str/replace #"^-+|-+$" ""))]
                     (if (str/blank? raw) "untitled-canvas" raw)))
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
         graph-query-execute (fn [_tool-call-id params a b c]
                               (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                                     query (or (aget params "query") "")
                                     lake (or (aget params "lake") "")
                                     node-type (or (aget params "nodeType") "")
                                     limit (aget params "limit")
                                     edge-limit (aget params "edgeLimit")]
                                 (maybe-tool-update! on-update "Querying canonical knowledge graph…")
                                 (-> (openplanner-graph-query! config {:query query
                                                                       :lake lake
                                                                       :node-type node-type
                                                                       :limit limit
                                                                       :edge-limit edge-limit})
                                     (.then (fn [result]
                                              (tool-text-result (graph-query-result-text result) result))))))
         websearch-execute (fn [_tool-call-id params a b c]
                             (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                                   query (or (aget params "query") "")
                                   num-results (or (aget params "numResults") 8)
                                   search-context-size (aget params "searchContextSize")
                                   allowed-domains (or (aget params "allowedDomains") #js [])
                                   model (aget params "model")]
                               (maybe-tool-update! on-update "Searching the live web through Proxx…")
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
                                              (let [result (js->clj (aget resp "body") :keywordize-keys true)]
                                                (tool-text-result (websearch-result-text result) result))
                                              (throw (js/Error. (str "websearch failed: "
                                                                     (pr-str (js->clj (aget resp "body") :keywordize-keys true)))))))))))
         create-new-file-execute (fn [_tool-call-id params a b c]
                                   (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                                         title (or (aget params "title") "Untitled Canvas")
                                         requested-path (or (aget params "path") (str "notes/canvas/" (slugify title) ".md"))
                                         content (or (aget params "content") (str "# " title "\n\n"))
                                         profile (active-agent-profile runtime config auth-context)
                                         docs-path (:docsPath profile)
                                         rel-path (normalize-relative-path requested-path)
                                         abs-path (.resolve node-path docs-path rel-path)
                                         rel-to-root (.relative node-path docs-path abs-path)
                                         parent (.dirname node-path abs-path)]
                                     (when (str/blank? rel-path)
                                       (throw (js/Error. "path is required for create_new_file")))
                                     (when (or (str/starts-with? rel-to-root "..") (.isAbsolute node-path rel-to-root))
                                       (throw (js/Error. "Path escapes active docs root")))
                                     (maybe-tool-update! on-update (str "Creating canvas file " rel-path "…"))
                                     (-> (.mkdir node-fs parent #js {:recursive true})
                                         (.then (fn [] (.writeFile node-fs abs-path content "utf8")))
                                         (.then (fn []
                                                  (tool-text-result (str "Created canvas file at " rel-path)
                                                                    {:path rel-path
                                                                     :title title
                                                                     :content content
                                                                     :canvas true}))))))
         ;; save_translation execute
         save-translation-execute (fn [_tool-call-id params a b c]
                                    (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                                          resource-policies (:resourcePolicies auth-context)
                                          source-text (aget params "source_text")
                                          translated-text (aget params "translated_text")
                                          source-lang (or (aget params "source_lang") (:source_lang resource-policies) (:source-lang resource-policies))
                                          target-lang (or (aget params "target_lang") (:target_lang resource-policies) (:target-lang resource-policies))
                                          document-id (or (aget params "document_id") (:document_id resource-policies) (:document-id resource-policies))
                                          garden-id (or (aget params "garden_id") (:garden_id resource-policies) (:garden-id resource-policies))
                                          project (or (aget params "project") (:project resource-policies) (:project-name config))
                                          segment-index (aget params "segment_index")
                                          normalized-source (str/trim (str (or source-text "")))
                                          normalized-translated (str/trim (str (or translated-text "")))
                                          prose-like? (or (> (count normalized-source) 24)
                                                          (str/includes? normalized-source " "))
                                          _ (when (and (not (str/blank? source-lang))
                                                       (not (str/blank? target-lang))
                                                       (not= source-lang target-lang)
                                                       prose-like?
                                                       (= normalized-source normalized-translated))
                                              (throw (js/Error. (str "translated_text matches source_text for segment " segment-index
                                                                      "; provide an actual " target-lang " translation"))))
                                          _ (when (str/blank? (str document-id))
                                              (throw (js/Error. "document_id is required for save_translation")))
                                          segment {:source_text source-text
                                                   :translated_text translated-text
                                                   :source_lang source-lang
                                                   :target_lang target-lang
                                                   :document_id document-id
                                                   :garden_id garden-id
                                                   :project project
                                                   :segment_index segment-index
                                                   :status "pending"
                                                   :mt_model "translation-agent"}
                                          url (str (:openplanner-base-url config) "/v1/translations/segments")]
                                      (maybe-tool-update! on-update (str "Saving translation segment " segment-index "…"))
                                      (-> (js/fetch url #js {:method "POST"
                                                             :headers #js {"Content-Type" "application/json"
                                                                           "Authorization" (str "Bearer " (:openplanner-api-key config))}
                                                             :body (.stringify js/JSON (clj->js segment))})
                                          (.then (fn [resp]
                                                   (if (.-ok resp)
                                                     (.json resp)
                                                     (-> (.text resp)
                                                         (.then (fn [text]
                                                                  (throw (js/Error. (str "HTTP " (.-status resp) ": " text)))))))))
                                          (.then (fn [result]
                                                   (tool-text-result (str "Saved segment " segment-index ": " (.substring translated-text 0 (min 50 (count translated-text))) "…")
                                                                     result))))))
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
         graph-query-tool (doto (js-obj)
                            (aset "name" "graph_query")
                            (aset "label" "Graph Query")
                            (aset "description" "Query the canonical OpenPlanner knowledge graph across the devel, web, bluesky, and knoxx-session lakes.")
                            (aset "promptSnippet" "Search the canonical knowledge graph when you need entities or cross-lake links rather than plain transcript memory or semantic document snippets.")
                            (aset "promptGuidelines" (clj->js ["Use graph_query when the question is about entities, paths, URLs, provenance across lakes, or graph connectivity."
                                                               "Prefer graph_query over semantic_query when node/edge structure matters."
                                                               "Use the lake filter to focus on devel, web, bluesky, or knoxx-session when the search space is obvious."]))
                            (aset "parameters" graph-params)
                            (aset "execute" graph-query-execute))
         websearch-tool (doto (js-obj)
                          (aset "name" "websearch")
                          (aset "label" "Web Search")
                          (aset "description" "Search the live web through Proxx websearch and return cited results.")
                          (aset "promptSnippet" "Search the live web when the user needs fresh external information or wants to expand the web frontier.")
                          (aset "promptGuidelines" (clj->js ["Use websearch when freshness matters or when the answer probably lives outside the current graph corpus."
                                                             "Prefer allowedDomains when you know the likely source surface."
                                                             "Use websearch to seed follow-up graph or semantic exploration, not as a substitute for graph_query when graph structure already exists."]))
                          (aset "parameters" websearch-params)
                          (aset "execute" websearch-execute))
         memory-session-tool (doto (js-obj)
                               (aset "name" "memory_session")
                               (aset "label" "Memory Session")
                               (aset "description" "Load the indexed transcript/events for a specific Knoxx session from OpenPlanner.")
                               (aset "promptSnippet" "Load a specific Knoxx OpenPlanner session when you need the exact previous transcript or action trace.")
                               (aset "promptGuidelines" (clj->js ["Use memory_session after memory_search identifies a promising session id."
                                                                  "memory_session is the exact transcript/action drill-down companion to memory_search."]))
                               (aset "parameters" session-params)
                               (aset "execute" memory-session-execute))
         save-translation-tool (doto (js-obj)
                                 (aset "name" "save_translation")
                                 (aset "label" "Save Translation")
                                 (aset "description" "Save a translated segment to the OpenPlanner translation database.")
                                 (aset "promptSnippet" "Save each translated segment after translating.")
                                 (aset "promptGuidelines" (clj->js ["Call save_translation for each segment you translate."
                                                                    "Include the source_text, translated_text, language codes, document_id, and segment_index."]))
                                 (aset "parameters" translation-params)
                                 (aset "execute" save-translation-execute))
         create-new-file-tool (doto (js-obj)
                                 (aset "name" "create_new_file")
                                 (aset "label" "Create New File")
                                 (aset "description" "Create a new file-backed artifact for the Knoxx canvas editor.")
                                 (aset "promptSnippet" "Create a new concrete artifact file when the user is ready to draft a real document instead of continuing in freeform chat.")
                                 (aset "promptGuidelines" (clj->js ["Use create_new_file when the user wants to start an actual artifact or canvas-backed document."
                                                                    "Return a file path and initial markdown content so the chat canvas can open it immediately."]))
                                 (aset "parameters" create-file-params)
                                 (aset "execute" create-new-file-execute))]
     (clj->js
     (vec
       (remove nil?
               [(when (or (nil? auth-context)
                          (ctx-tool-allowed? auth-context "graph_query")
                          (ctx-tool-allowed? auth-context "semantic_query"))
                  graph-query-tool)
                (when (or (nil? auth-context)
                          (ctx-tool-allowed? auth-context "websearch"))
                  websearch-tool)
                (when (or (nil? auth-context)
                          (ctx-tool-allowed? auth-context "memory_search"))
                  memory-search-tool)
                (when (or (nil? auth-context)
                          (ctx-tool-allowed? auth-context "memory_session"))
                  memory-session-tool)
                (when (or (nil? auth-context)
                          (ctx-tool-allowed? auth-context "save_translation"))
                  save-translation-tool)
                (when (or (nil? auth-context)
                          (ctx-tool-allowed? auth-context "create_new_file"))
                  create-new-file-tool)]))))))

(defn create-knoxx-custom-tools
  ([runtime config] (create-knoxx-custom-tools runtime config nil))
  ([runtime config auth-context]
   (.concat (.concat (create-semantic-custom-tools runtime config auth-context)
                     (create-discord-custom-tools runtime config auth-context))
            (create-openplanner-custom-tools runtime config auth-context))))
