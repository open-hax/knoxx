(ns knoxx.backend.agent-hydration
  (:require [clojure.string :as str]
            [knoxx.backend.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.core-memory :refer [fetch-openplanner-session-rows! filter-authorized-memory-hits! session-visible?]]
            [knoxx.backend.document-state :refer [active-agent-profile ensure-dir! list-files-recursive! normalize-relative-path indexed-meta]]
            [knoxx.backend.http :as backend-http :refer [openplanner-enabled? http-error js-array-seq]]
            [knoxx.backend.openplanner-memory :refer [openplanner-memory-search! openplanner-graph-query!]]
            [knoxx.backend.runtime-config :refer [default-settings]]
            [knoxx.backend.text :refer [search-tokens text-like-path? clip-text semantic-score snippet-around value->preview-text tool-text-result semantic-search-result-text semantic-read-result-text openplanner-memory-search-text openplanner-session-text graph-query-result-text]]))

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
         graph-params (.Object Type
                               #js {:query (.String Type #js {:description "Search text for canonical graph nodes across OpenPlanner lakes."})
                                    :lake (.Optional Type (.String Type #js {:description "Optional lake/project filter such as devel, web, bluesky, or knoxx-session."}))
                                    :nodeType (.Optional Type (.String Type #js {:description "Optional node_type filter such as docs, code, visited, assistant_message, tool_result, or reasoning."}))
                                    :limit (.Optional Type (.Number Type #js {:description "Maximum number of graph nodes to return." :minimum 1 :maximum 20}))
                                    :edgeLimit (.Optional Type (.Number Type #js {:description "Maximum number of incident edges to include." :minimum 0 :maximum 60}))})
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
                          (ctx-tool-allowed? auth-context "graph_query")
                          (ctx-tool-allowed? auth-context "semantic_query"))
                  graph-query-tool)
                (when (or (nil? auth-context)
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
