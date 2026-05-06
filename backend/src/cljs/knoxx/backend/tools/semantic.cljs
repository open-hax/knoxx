(ns knoxx.backend.tools.semantic
  "Semantic search and document-reading tools for the active Knoxx corpus."
  (:require [clojure.string :as str]
            [knoxx.backend.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.document-state :refer [active-agent-profile ensure-dir! list-files-recursive! normalize-relative-path indexed-meta]]
            [knoxx.backend.http :refer [openplanner-enabled? js-array-seq]]
            [knoxx.backend.openplanner-memory :refer [openplanner-semantic-search!]]
            [knoxx.backend.text :refer [search-tokens text-like-path? clip-text semantic-score snippet-around tool-text-result semantic-search-result-text semantic-read-result-text openplanner-semantic-search-text]]
            [knoxx.backend.tools.media :refer [path-relative path-basename path-resolve path-is-absolute? fs-read-file!]]
            [knoxx.backend.tools.shared :refer [maybe-tool-update! create-tool-obj]]))

(def query-params
  [:map
   [:query {:description "Natural-language semantic search query for the active Knoxx corpus."} :string]
   [:topK {:optional true :description "Maximum number of matches to return."} [:int {:min 1 :max 10}]]
   [:maxSnippetChars {:optional true :description "Maximum snippet length per hit."} [:int {:min 160 :max 1200}]]])

(def read-params
  [:map
   [:path {:description "Relative document path returned by semantic_query or visible in the active corpus."} :string]
   [:maxChars {:optional true :description "Maximum characters of document content to return."} [:int {:min 500 :max 20000}]]])

(defn- sanitize-svg-content [content]
  (-> content
      (str/replace #"(?is)<script[^>]*>.*?</script>" "")
      (str/replace #"(?i)on[a-z]+\s*=\s*['\"].*?['\"]" "")))
(defn hydrate-files [paths node-path docs-path runtime config db-id node-fs query tokens max-snippet-chars]
  (clj->js
   (for [abs-path paths]
     (let [rel-path (normalize-relative-path (path-relative node-path docs-path abs-path))
           name (path-basename node-path abs-path)
           indexed-meta (indexed-meta runtime config db-id rel-path)]
       (if (text-like-path? rel-path)
         (-> (fs-read-file! node-fs abs-path "utf8")
             (.then (fn [content]
                      (let [cleaned (if (re-find #"(?i)\.svg$" rel-path)
                                      (sanitize-svg-content content)
                                      content)
                            [clipped _] (clip-text cleaned 20000)
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
(defn rank-results [results top-k profile docs-path query tokens]
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
     :results ranked})
  )
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
                  (js/Promise.all
                   (hydrate-files
                    paths
                    node-path
                    docs-path
                    runtime
                    config
                    db-id
                    node-fs
                    query
                    tokens
                    max-snippet-chars))))
         (.then (fn [results]
                  (rank-results
                   results
                   top-k
                   profile
                   docs-path
                   query
                   tokens)))))))

(defn semantic-read-document!
  ([runtime config opts] (semantic-read-document! runtime config opts nil))
  ([runtime config {:keys [path max-chars]} auth-context]
   (let [profile (active-agent-profile runtime config auth-context)
         node-fs (aget runtime "fs")
         node-path (aget runtime "path")
         rel-path (normalize-relative-path path)
         abs-path (path-resolve node-path (:docsPath profile) rel-path)
         rel-to-root (path-relative node-path (:docsPath profile) abs-path)
         max-chars (max 500 (min 20000 (or max-chars 6000)))]
     (if (or (str/starts-with? rel-to-root "..") (path-is-absolute? node-path rel-to-root))
       (js/Promise.reject (js/Error. "Path escapes active docs root"))
       (-> (fs-read-file! node-fs abs-path "utf8")
           (.then (fn [content]
                    (let [cleaned (if (re-find #"(?i)\.svg$" rel-path)
                                    (sanitize-svg-content content)
                                    content)
                          [clipped truncated?] (clip-text cleaned max-chars)]
                      {:database {:id (:id profile)
                                  :name (:name profile)}
                       :path rel-path
                       :truncated truncated?
                       :content clipped}))))))))

(defn semantic-query-execute [runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        query (or (aget params "query") "")
        top-k (or (aget params "topK") (aget params "top_k"))
        max-snippet-chars (or (aget params "maxSnippetChars") (aget params "max_snippet_chars"))]
    (maybe-tool-update! on-update "Searching corpus via OpenPlanner…")
    (if (openplanner-enabled? config)
      (-> (openplanner-semantic-search! config {:query query :k (or top-k 5)})
          (.then (fn [result]
                   (tool-text-result (openplanner-semantic-search-text result) result))))
      (-> (semantic-search-documents! runtime config {:query query
                                                      :top-k top-k
                                                      :max-snippet-chars max-snippet-chars})
          (.then (fn [result]
                   (tool-text-result (semantic-search-result-text result) result)))))))

(defn semantic-read-execute [runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        path (or (aget params "path") "")
        max-chars (or (aget params "maxChars") (aget params "max_chars"))]
    (maybe-tool-update! on-update "Reading corpus document…")
    (-> (semantic-read-document! runtime config {:path path :max-chars max-chars})
        (.then (fn [result]
                 (tool-text-result (semantic-read-result-text result) result))))))

(def semantic-query-tool
  (partial create-tool-obj
           "semantic_query"
           "Semantic Query"
           "Search the active Knoxx knowledge corpus for semantically relevant documents and snippets."
           "Search the active Knoxx corpus by meaning and retrieve the most relevant documents/snippets."
           ["Use semantic_query when you need grounded workspace knowledge beyond what passive hydration already exposed."
            "Prefer semantic_query over guessing when the answer may live in notes, uploaded documents, or indexed corpus files."
            "Follow semantic_query with semantic_read when one result looks promising and you need exact source text."]
           query-params
           semantic-query-execute))

(def semantic-read-tool
  (partial create-tool-obj
           "semantic_read"
           "Read Document"
           "Read a document by relative path from the active Knoxx corpus."
           "Read a specific Knoxx corpus document by relative path after semantic_query identifies a likely hit."
           ["Use semantic_read after semantic_query when you need exact source text instead of summaries or snippets."
            "Pass a relative document path from semantic_query results."]
           read-params
           semantic-read-execute))

(defn create-semantic-custom-tools
  ([runtime config] (create-semantic-custom-tools runtime config nil))
  ([runtime config auth-context]
   (clj->js
    (vec
     (remove nil?
             [(when (or (nil? auth-context)
                        (ctx-tool-allowed? auth-context "semantic_query"))
                (semantic-query-tool runtime config))
              (when (or (nil? auth-context)
                        (ctx-tool-allowed? auth-context "read")
                        (ctx-tool-allowed? auth-context "semantic_query"))
                (semantic-read-tool runtime config))])))))
