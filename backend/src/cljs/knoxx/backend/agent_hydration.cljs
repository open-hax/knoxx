(ns knoxx.backend.agent-hydration
  (:require [clojure.string :as str]
            [knoxx.backend.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.core-memory :refer [fetch-openplanner-session-rows! filter-authorized-memory-hits! session-visible?]]
            [knoxx.backend.document-state :refer [active-agent-profile ensure-dir! list-files-recursive! normalize-relative-path indexed-meta]]
            [knoxx.backend.http :as backend-http :refer [openplanner-enabled? http-error js-array-seq]]
            [knoxx.backend.openplanner-memory :refer [openplanner-memory-search! openplanner-graph-memory! openplanner-graph-query!]]
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

(defn parse-csv-list
  [value]
  (->> (str/split (str (or value "")) #",")
       (map str/trim)
       (remove str/blank?)
       vec))

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
    (str "Passive conversational memory hydration from OpenPlanner follows. This is prior Knoxx session memory and action history; verify with graph_query, the knoxx-session scoped memory_search alias, or memory_session if precision matters.\n\n"
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
         unified-params (.Object Type
                                 #js {:query (.String Type #js {:description "Semantic seed query for unified graph/memory traversal."})
                                      :lakes (.Optional Type (.String Type #js {:description "Optional comma-separated lakes such as devel,web,bluesky,knoxx-session."}))
                                      :nodeTypes (.Optional Type (.String Type #js {:description "Optional comma-separated node types such as file,url,run,tool_call."}))
                                      :k (.Optional Type (.Number Type #js {:description "Maximum semantic seed count." :minimum 1 :maximum 20}))
                                      :maxCost (.Optional Type (.Number Type #js {:description "Maximum traversal cost from seed nodes." :minimum 0.1 :maximum 20}))
                                      :maxNodes (.Optional Type (.Number Type #js {:description "Maximum traversed nodes to return." :minimum 1 :maximum 120}))
                                      :minSimilarity (.Optional Type (.Number Type #js {:description "Minimum semantic edge similarity to traverse." :minimum 0 :maximum 1}))
                                      :minVectorSimilarity (.Optional Type (.Number Type #js {:description "Minimum vector similarity for semantic seeds." :minimum 0 :maximum 1}))})
         session-params (.Object Type
                                 #js {:sessionId (.String Type #js {:description "Knoxx conversation/session id stored in OpenPlanner."})})
         run-unified-graph-memory! (fn [params forced-lakes]
                                     (let [query (or (aget params "query") "")
                                           lakes (vec (distinct (concat forced-lakes (parse-csv-list (aget params "lakes")))))
                                           node-types (parse-csv-list (aget params "nodeTypes"))
                                           k (aget params "k")
                                           max-cost (aget params "maxCost")
                                           max-nodes (aget params "maxNodes")
                                           min-similarity (aget params "minSimilarity")
                                           min-vector-similarity (aget params "minVectorSimilarity")]
                                       (openplanner-graph-memory! config {:query query
                                                                          :lakes lakes
                                                                          :node-types node-types
                                                                          :k k
                                                                          :max-cost max-cost
                                                                          :max-nodes max-nodes
                                                                          :min-similarity min-similarity
                                                                          :min-vector-similarity min-vector-similarity})))
         memory-search-execute (fn [_tool-call-id params a b c]
                                 (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                                       query (or (aget params "query") "")
                                       k (aget params "k")]
                                   (maybe-tool-update! on-update "Walking Knoxx session memory as a semantic cost graph…")
                                   (-> (run-unified-graph-memory! params ["knoxx-session"])
                                       (.then (fn [result]
                                                (if (seq (:nodes result))
                                                  (tool-text-result (graph-query-result-text result) result)
                                                  (do
                                                    (maybe-tool-update! on-update "Unified graph walk found no seeds; falling back to transcript memory search…")
                                                    (-> (openplanner-memory-search! config {:query query :k k :session-id ""})
                                                        (.then (fn [legacy-result]
                                                                 (-> (filter-authorized-memory-hits! config auth-context (:hits legacy-result))
                                                                     (.then (fn [hits]
                                                                              (let [filtered (assoc legacy-result :hits hits)]
                                                                                (tool-text-result (openplanner-memory-search-text filtered) filtered)))))))))))))))
         memory-session-execute (fn [_tool-call-id params a b c]
                                  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                                        session-id (or (aget params "sessionId") "")]
                                    (maybe-tool-update! on-update "Loading Knoxx session from OpenPlanner…")
                                    (-> (fetch-openplanner-session-rows! config session-id {:mode :resume :limit 400})
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
                                     lakes (parse-csv-list (aget params "lakes"))
                                     node-types (parse-csv-list (aget params "nodeTypes"))
                                     max-nodes (aget params "maxNodes")]
                                 (maybe-tool-update! on-update "Walking unified graph/memory field from semantic seeds…")
                                 (-> (run-unified-graph-memory! params [])
                                     (.then (fn [result]
                                              (if (seq (:nodes result))
                                                (tool-text-result (graph-query-result-text result) result)
                                                (do
                                                  (maybe-tool-update! on-update "Unified graph walk found no seeds; falling back to legacy graph query…")
                                                  (-> (openplanner-graph-query! config {:query query
                                                                                        :lake (first lakes)
                                                                                        :node-type (first node-types)
                                                                                        :limit max-nodes
                                                                                        :edge-limit 40})
                                                      (.then (fn [legacy-result]
                                                               (tool-text-result (graph-query-result-text legacy-result) legacy-result)))))))))))
         memory-search-tool (doto (js-obj)
                              (aset "name" "memory_search")
                              (aset "label" "Memory Search")
                              (aset "description" "Unified graph-memory traversal scoped to the Knoxx session lake.")
                              (aset "promptSnippet" "Walk prior Knoxx session memory as a semantic-seed cost graph when the user asks about earlier sessions, prior decisions, or past actions.")
                              (aset "promptGuidelines" (clj->js ["memory_search is now a knoxx-session scoped alias of the unified graph/memory tool."
                                                                 "Use it when the user asks what happened before, what you already tried, or which prior session matters."
                                                                 "If one session looks relevant, follow with memory_session to inspect the exact transcript slice."]))
                              (aset "parameters" unified-params)
                              (aset "execute" memory-search-execute))
         graph-query-tool (doto (js-obj)
                            (aset "name" "graph_query")
                            (aset "label" "Graph Memory Query")
                            (aset "description" "Unified semantic-seed graph/memory traversal across OpenPlanner lakes using bounded cost expansion.")
                            (aset "promptSnippet" "Use semantic seeds plus bounded graph traversal when you need entities, provenance, paths, prior sessions, or cross-lake memory in one query.")
                            (aset "promptGuidelines" (clj->js ["graph_query is the primary unified graph/memory tool."
                                                               "It replaces the old split between graph and memory search by seeding semantically, then traversing the graph with bounded cost."
                                                               "Use lakes and nodeTypes to focus the walk when the search space is obvious."]))
                            (aset "parameters" unified-params)
                            (aset "execute" graph-query-execute))
         memory-session-tool (doto (js-obj)
                               (aset "name" "memory_session")
                               (aset "label" "Memory Session")
                               (aset "description" "Load the indexed transcript/events for a specific Knoxx session from OpenPlanner.")
                               (aset "promptSnippet" "Load a specific Knoxx OpenPlanner session when you need the exact previous transcript or action trace.")
                               (aset "promptGuidelines" (clj->js ["Use memory_session after graph_query or memory_search identifies a promising Knoxx session."
                                                                  "memory_session is the exact transcript/action drill-down companion to the unified graph/memory query surface."]))
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
