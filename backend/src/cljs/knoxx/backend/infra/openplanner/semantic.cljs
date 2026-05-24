(ns knoxx.backend.infra.openplanner.semantic
  "Semantic search and document-reading tools for the active Knoxx corpus."
  (:require [clojure.string :as str]
            [knoxx.backend.infra.auth.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.infra.openplanner.memory :refer [openplanner-semantic-search!]]
            [knoxx.backend.domain.text :refer [clip-text semantic-search-result-text tool-text-result]]
            [knoxx.backend.domain.tools :refer [maybe-tool-update! create-tool-obj]]))

(def query-params
  [:map
   [:query {:description "Natural-language semantic search query for the active Knoxx corpus."} :string]
   [:topK {:optional true :description "Maximum number of matches to return."} [:int {:min 1 :max 10}]]
   [:maxSnippetChars {:optional true :description "Maximum snippet length per hit."} [:int {:min 160 :max 1200}]]])





(defn- bounded-score
  [distance]
  (if (number? distance)
    (max 0 (- 1 (min 1 distance)))
    0))

(defn- metadata-value
  [metadata & keys]
  (some (fn [k] (get metadata k)) keys))

(defn- semantic-hit-path
  [hit]
  (let [metadata (or (:metadata hit) {})]
    (str (or (metadata-value metadata
                             :sourcePath :source-path :source_path
                             :relativePath :relative-path :relative_path
                             :filePath :file-path :file_path
                             :path :file :title)
             (:id hit)
             "unknown"))))

(defn- semantic-hit-chunk-count
  [metadata]
  (or (metadata-value metadata :chunkCount :chunk-count :chunk_count :chunks)
      1))

(defn- semantic-hit-result
  [max-snippet-chars hit]
  (let [metadata (or (:metadata hit) {})]
    {:id (:id hit)
     :path (semantic-hit-path hit)
     :score (bounded-score (:distance hit))
     :distance (:distance hit)
     :indexed true
     :chunkCount (semantic-hit-chunk-count metadata)
     :snippet (first (clip-text (or (:document hit) "") max-snippet-chars))
     :metadata metadata}))

(defn semantic-search-documents!
  "Search OpenPlanner for passive semantic hydration and return the legacy
   document-result shape expected by agent hydration. Returns a Promise."
  [_runtime config {:keys [query top-k max-snippet-chars]} _auth-context]
  (let [k (max 1 (min 10 (or top-k 5)))
        max-snippet-chars (max 160 (min 1200 (or max-snippet-chars 240)))]
    (-> (openplanner-semantic-search! config {:query query :k k})
        (.then (fn [result]
                 {:query query
                  :database {:name "OpenPlanner"}
                  :results (mapv (partial semantic-hit-result max-snippet-chars)
                                 (take k (:hits result)))})))))

(defn semantic-query-execute [runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        query (or (aget params "query") "")
        top-k (or (aget params "topK") (aget params "top_k"))
        max-snippet-chars (or (aget params "maxSnippetChars") (aget params "max_snippet_chars"))]
    (maybe-tool-update! on-update "Searching corpus via OpenPlanner…")
    (-> (semantic-search-documents! runtime config {:query query
                                                    :top-k (or top-k 5)
                                                    :max-snippet-chars (or max-snippet-chars 600)} nil)
        (.then (fn [result]
                 (tool-text-result (semantic-search-result-text result) result))))))


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



(defn create-semantic-custom-tools
  ([runtime config] (create-semantic-custom-tools runtime config nil))
  ([runtime config auth-context]
   (clj->js
    (vec
     (remove nil?
             [(when (or (nil? auth-context)
                        (ctx-tool-allowed? auth-context "semantic_query"))
                (semantic-query-tool runtime config))])))))
