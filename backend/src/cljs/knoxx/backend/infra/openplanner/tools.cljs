(ns knoxx.backend.infra.openplanner.tools
  "OpenPlanner memory, graph, websearch, and translation tools."
  (:require [clojure.string :as str]
            [knoxx.backend.infra.auth.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.infra.core-memory :refer [fetch-openplanner-session-rows! filter-authorized-memory-hits! session-visible?]]
            [knoxx.backend.infra.document-state :refer [active-agent-profile normalize-relative-path]]
            [knoxx.backend.infra.clients.proxx :as proxx-client]
            [knoxx.backend.infra.clients.openplanner :as openplanner-client]
            [knoxx.backend.infra.http :refer [http-error]]
            [knoxx.backend.infra.openplanner.memory :refer [openplanner-memory-search! openplanner-graph-query! openplanner-event]]
            [knoxx.backend.domain.text :refer [tool-text-result openplanner-memory-search-text openplanner-session-text graph-query-result-text websearch-result-text]]
            [knoxx.backend.domain.media :as media]
            [knoxx.backend.domain.tools :refer [maybe-tool-update! create-tool-obj]]
            ["node:fs/promises" :as fs]
            ["node:path" :as path]))

(def memory-search-params
  [:map
   [:query {:description "Semantic memory search across prior Knoxx sessions and actions indexed in OpenPlanner."} :string]
   [:k {:optional true :description "Maximum number of memory hits to return (default 7, max 12). Reasoning traces are excluded by default."} [:int {:min 1 :max 12}]]
   [:sessionId {:optional true :description "Optional conversation/session id to scope the search."} :string]])

(def graph-query-params
  [:map
   [:query {:description "Search text for canonical graph nodes across OpenPlanner lakes."} :string]
   [:lake {:optional true :description "Optional lake/project filter such as devel, web, bluesky, or knoxx-session."} :string]
   [:nodeType {:optional true :description "Optional node_type filter such as docs, code, visited, assistant_message, tool_result, or reasoning."} :string]
   [:limit {:optional true :description "Maximum number of graph nodes to return."} [:int {:min 1 :max 20}]]
   [:edgeLimit {:optional true :description "Maximum number of incident edges to include."} [:int {:min 0 :max 60}]]])

(def websearch-params
  [:map
   [:query {:description "Live web search query routed through Proxx websearch."} :string]
   [:numResults {:optional true :description "Maximum number of results to return."} [:int {:min 1 :max 20}]]
   [:searchContextSize {:optional true :description "Search context size: low, medium, or high."} :string]
   [:allowedDomains {:optional true :description "Optional domain allowlist."} [:vector :string]]
   [:model {:optional true :description "Optional Proxx/OpenAI model override for search."} :string]])

(def web-read-params
  [:map
   [:url {:description "Web link or attachment URL to fetch and read."} :string]
   [:maxChars {:optional true :description "Maximum number of characters to return."} [:int {:min 200 :max 20000}]]])

(def memory-session-params
  [:map
   [:sessionId {:description "Knoxx conversation/session id stored in OpenPlanner."} :string]])

(def translation-params
  [:map
   [:source_text {:description "Original source text"} :string]
   [:translated_text {:description "Translated text"} :string]
   [:source_lang {:description "Source language code (e.g. 'en')"} :string]
   [:target_lang {:description "Target language code (e.g. 'es')"} :string]
   [:document_id {:description "Document ID being translated"} :string]
   [:garden_id {:optional true :description "Garden ID"} :string]
   [:project {:optional true :description "Project name"} :string]
   [:segment_index {:description "0-based segment index"} :int]])

(def create-file-params
  [:map
   [:title {:optional true :description "Human-readable title for the new artifact."} :string]
   [:path {:optional true :description
           "Relative path for the new file inside the active docs root."} :string]
   [:content {:optional true :description "Initial markdown content to write into the new file."} :string]])

(def push-claim-params
  [:map
   [:claim {:description "The proposition or claim to add to the knowledge graph."} :string]
   [:evidence {:optional true :description "Supporting evidence or a chain of claims that lead to this conclusion."} [:vector :string]]
   [:probability {:optional true :description "Confidence score from 0.0 to 1.0."} [:double {:min 0.0 :max 1.0}]]
   [:source {:optional true :description "The source of the claim (e.g. \"web-research\", \"llm-inference\")."} :string]])

(defn- web-read-url! [url max-chars]
  (-> (js/fetch url #js {:headers #js {"User-Agent" "Knoxx-Agent/1.0"}})
      (.then
       (fn [resp]
         (let [content-type (or (.get (.-headers resp) "content-type") "application/octet-stream")]
           (if-not (.-ok resp)
             (-> (.text resp)
                 (.then (fn [text]
                          (throw (js/Error. (str "web.read failed " (.-status resp) ": " text))))))
             (if (or (str/starts-with? content-type "text/")
                     (str/includes? content-type "json")
                     (str/includes? content-type "xml")
                     (str/includes? content-type "html"))
               (-> (.text resp)
                   (.then (fn [text]
                            (let [collapsed (-> text
                                                (str/replace #"<[^>]+>" " ")
                                                (str/replace #"\s+" " ")
                                                str/trim)
                                  clipped (subs collapsed 0 (min max-chars (count collapsed)))]
                              (tool-text-result (str "Read URL " url " (" content-type "):\n\n" clipped)
                                                {:url url :contentType content-type :text clipped})))))
               (js/Promise.resolve
                (tool-text-result (str "Fetched URL " url " with content-type " content-type ". Binary/image content is available at the URL for follow-up use.")
                                  {:url url :contentType content-type :binary true})))))))))

(defn- slugify [value]
  (let [raw (-> (str (or value "untitled-canvas"))
                str/lower-case
                (str/replace #"[^a-z0-9]+" "-")
                (str/replace #"^-+|-+$" ""))]
    (if (str/blank? raw) "untitled-canvas" raw)))

(defn make-memory-search-execute [auth-context]
  (fn [_runtime config _tool-call-id params a b c]
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
                                  (tool-text-result (openplanner-memory-search-text filtered) filtered)))))))))))

(defn make-memory-session-execute [auth-context]
  (fn [_runtime config _tool-call-id params a b c]
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
                     (tool-text-result (openplanner-session-text session-id rows) payload))))))))

(defn graph-query-execute [_runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        query (or (aget params "query") "")
        lake (or (aget params "lake") "")
        node-type (or (aget params "nodeType") "")
        limit (aget params "limit")
        edge-limit (aget params "edgeLimit")]
    (maybe-tool-update! on-update "Querying canonical knowledge graph…")
    (-> (openplanner-graph-query! config {:query query :lake lake :node-type node-type :limit limit :edge-limit edge-limit})
        (.then (fn [result]
                 (tool-text-result (graph-query-result-text result) result))))))

(defn websearch-execute [_runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        query (or (aget params "query") "")
        num-results (or (aget params "numResults") 8)
        search-context-size (aget params "searchContextSize")
        allowed-domains (or (aget params "allowedDomains") [])
        model (aget params "model")]
    (maybe-tool-update! on-update "Searching the live web through Proxx…")
    (-> (proxx-client/websearch! (proxx-client/client config)
                                 {:query query
                                  :numResults num-results
                                  :searchContextSize search-context-size
                                  :allowedDomains allowed-domains
                                  :model model})
        (.then (fn [resp]
                 (if (:ok resp)
                   (tool-text-result (websearch-result-text (:body resp)) (:body resp))
                   (throw (js/Error. (str "websearch failed: " (pr-str (:body resp)))))))))))

(defn web-read-execute [_runtime _config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        url (or (aget params "url") "")
        max-chars (max 200 (min 20000 (or (aget params "maxChars") 6000)))]
    (when (str/blank? (str/trim url))
      (throw (js/Error. "url is required")))
    (maybe-tool-update! on-update (str "Fetching " url "…"))
    (web-read-url! url max-chars)))

(defn make-create-new-file-execute [auth-context]
  (fn [runtime config _tool-call-id params a b c]
    (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
          title (or (aget params "title") "Untitled Canvas")
          requested-path (or (aget params "path") (str "notes/canvas/" (slugify title) ".md"))
          content (or (aget params "content") (str "# " title "\n\n"))
          profile (active-agent-profile runtime config auth-context)
          docs-path (:docsPath profile)
          rel-path (normalize-relative-path requested-path)
          abs-path (media/path-resolve path docs-path rel-path)
          rel-to-root (media/path-relative path docs-path abs-path)
          parent (.dirname path abs-path)]
      (when (str/blank? rel-path)
        (throw (js/Error. "path is required for create_new_file")))
      (when (or (str/starts-with? rel-to-root "..") (media/path-is-absolute? path rel-to-root))
        (throw (js/Error. "Path escapes active docs root")))
      (maybe-tool-update! on-update (str "Creating canvas file " rel-path "…"))
      (-> (media/fs-mkdir! fs parent #js {:recursive true})
          (.then (fn [] (media/fs-write-file! fs abs-path content "utf8")))
          (.then (fn []
                   (tool-text-result (str "Created canvas file at " rel-path)
                                     {:path rel-path :title title :content content :canvas true})))))))

(defn make-push-claim-execute [auth-context]
  (fn [runtime config _tool-call-id params a b c]
    (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
          claim (aget params "claim")
          evidence (or (aget params "evidence") #js [])
          p (or (aget params "probability") 0.5)
          source (or (aget params "source") "llm-investigation")
          profile (active-agent-profile runtime config auth-context)
          event (openplanner-event config {:id (str "claim:" (.randomUUID js/crypto))
                                           :kind "knoxx.claim"
                                           :session (:sessionId auth-context)
                                           :role "assistant"
                                           :model (:model profile)
                                           :text claim
                                           :extra {:claim claim :evidence (clj->js evidence) :p p :src source}})]
      (maybe-tool-update! on-update (str "Pushing claim to graph: " claim "…"))
      (-> (openplanner-client/events! (openplanner-client/client config) [event])
          (.then (fn [resp]
                   (tool-text-result (str "Successfully pushed claim to graph: " claim) resp)))))))

(defn make-save-translation-execute [auth-context]
  (fn [_runtime config _tool-call-id params a b c]
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
          prose-like? (or (> (count normalized-source) 24) (str/includes? normalized-source " "))
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
                   :mt_model "translation-agent"}]
      (maybe-tool-update! on-update (str "Saving translation segment " segment-index "…"))
      (-> (openplanner-client/create-translation-segment! (openplanner-client/client config) segment)
          (.then (fn [result]
                   (tool-text-result (str "Saved segment " segment-index ": " (.substring translated-text 0 (min 50 (count translated-text))) "…")
                                     result)))))))

(def graph-query-tool
  (partial create-tool-obj
           "graph_query" "Graph Query"
           "Query the canonical OpenPlanner knowledge graph across the devel, web, bluesky, and knoxx-session lakes."
           "Search the canonical knowledge graph when you need entities or cross-lake links rather than plain transcript memory or semantic document snippets."
           ["Use graph_query when the question is about entities, paths, URLs, provenance across lakes, or graph connectivity."
            "Prefer graph_query over semantic_query when node/edge structure matters."
            "Use the lake filter to focus on devel, web, bluesky, or knoxx-session when the search space is obvious."]
           graph-query-params
           graph-query-execute))

(def websearch-tool
  (partial create-tool-obj
           "websearch" "Web Search"
           "Search the live web through Proxx websearch and return cited results."
           "Search the live web when the user needs fresh external information or wants to expand the web frontier."
           ["Use websearch when freshness matters or when the answer probably lives outside the current graph corpus."
            "Prefer allowedDomains when you know the likely source surface."
            "Use websearch to seed follow-up graph or semantic exploration, not as a substitute for graph_query when graph structure already exists."]
           websearch-params
           websearch-execute))

(def web-read-tool
  (partial create-tool-obj
           "web.read" "Web Read"
           "Fetch a web link or attachment URL and extract readable text or metadata."
           "Read a web page, text attachment, or direct file URL when you already have a concrete link."
           ["Use web.read for direct URLs from Discord messages, embeds, or attachments."
            "For image/binary URLs, web.read returns metadata so you can decide whether to forward the attachment or inspect it another way."]
           web-read-params
           web-read-execute))

(defn memory-search-tool [auth-context]
  (partial create-tool-obj
           "memory_search" "Memory Search"
           "Search prior Knoxx sessions, answers, and tool/action receipts stored in OpenPlanner."
           "Search Knoxx long-term memory in OpenPlanner when the user asks about earlier sessions, prior decisions, or the agent's own past actions."
           ["Use memory_search when the user references previous sessions, past work, or asks you to remember what happened before."
            "Prefer memory_search over guessing about prior conversations or actions."
            "Reasoning traces are filtered out of memory_search by default; use memory_session only when exact transcript drill-down is required."
            "If one session looks relevant, follow with memory_session to inspect the full transcript slice."]
           memory-search-params
           (make-memory-search-execute auth-context)))

(defn memory-session-tool [auth-context]
  (partial create-tool-obj
           "memory_session" "Memory Session"
           "Load the indexed transcript/events for a specific Knoxx session from OpenPlanner."
           "Load a specific Knoxx OpenPlanner session when you need the exact previous transcript or action trace."
           ["Use memory_session after memory_search identifies a promising session id."
            "memory_session is the exact transcript/action drill-down companion to memory_search."]
           memory-session-params
           (make-memory-session-execute auth-context)))

(defn save-translation-tool [auth-context]
  (partial create-tool-obj
           "save_translation" "Save Translation"
           "Save a translated segment to the OpenPlanner translation database."
           "Save each translated segment after translating."
           ["Call save_translation for each segment you translate."
            "Include the source_text, translated_text, language codes, document_id, and segment_index."]
           translation-params
           (make-save-translation-execute auth-context)))

(defn create-new-file-tool [auth-context]
  (partial create-tool-obj
           "create_new_file" "Create New File"
           "Create a new file-backed artifact for the Knoxx canvas editor."
           "Create a new concrete artifact file when the user is ready to draft a real document instead of continuing in freeform chat."
           ["Use create_new_file when the user wants to start an actual artifact or canvas-backed document."
            "Return a file path and initial markdown content so the chat canvas can open it immediately."]
           create-file-params
           (make-create-new-file-execute auth-context)))

(defn push-claim-tool [auth-context]
  (partial create-tool-obj
           "push_claim" "Push Claim"
           "Add a new claim or proposition to the knowledge graph. Use this when you discover a fact or derive an inference during an investigation."
           "Push a discovered claim or derived inference into the knowledge graph. Treat the graph as an evolving tapestry of hypotheses; the act of recording a discovery is as valuable as the discovery itself."
           ["Use push_claim when you uncover a new fact or derive a logical inference during investigation."
            "Don't obsess over binary truth; prioritize the capture of the epistemic trail."
            "Include all relevant evidence and a confidence probability."]
           push-claim-params
           (make-push-claim-execute auth-context)))

(defn create-openplanner-custom-tools
  ([runtime config] (create-openplanner-custom-tools runtime config nil))
  ([runtime config auth-context]
   (clj->js
    (vec
     (remove nil?
             [(when (or (nil? auth-context)
                        (ctx-tool-allowed? auth-context "graph_query")
                        (ctx-tool-allowed? auth-context "semantic_query"))
                (graph-query-tool runtime config))
              (when (or (nil? auth-context)
                        (ctx-tool-allowed? auth-context "websearch"))
                (websearch-tool runtime config))
              (when (or (nil? auth-context)
                        (ctx-tool-allowed? auth-context "web.read"))
                (web-read-tool runtime config))
              (when (or (nil? auth-context)
                        (ctx-tool-allowed? auth-context "memory_search"))
                ((memory-search-tool auth-context) runtime config))
              (when (or (nil? auth-context)
                        (ctx-tool-allowed? auth-context "memory_session"))
                ((memory-session-tool auth-context) runtime config))
              (when (or (nil? auth-context)
                        (ctx-tool-allowed? auth-context "save_translation"))
                ((save-translation-tool auth-context) runtime config))
              (when (or (nil? auth-context)
                        (ctx-tool-allowed? auth-context "create_new_file"))
                ((create-new-file-tool auth-context) runtime config))
              (when (or (nil? auth-context)
                        (ctx-tool-allowed? auth-context "push_claim"))
                ((push-claim-tool auth-context) runtime config))])))))
