(ns knoxx.backend.tools.semantic
  "Semantic search and document-reading tools for the active Knoxx corpus."
  (:require [clojure.string :as str]
            [knoxx.backend.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.document-state :refer [active-agent-profile ensure-dir! list-files-recursive! normalize-relative-path indexed-meta]]
            [knoxx.backend.http :as backend-http :refer [openplanner-enabled? js-array-seq]]
            [knoxx.backend.openplanner-memory :refer [openplanner-semantic-search!]]
            [knoxx.backend.text :refer [search-tokens text-like-path? clip-text semantic-score snippet-around value->preview-text tool-text-result semantic-search-result-text semantic-read-result-text openplanner-semantic-search-text]]
            [knoxx.backend.tools.media :refer [path-relative path-basename path-resolve path-is-absolute? fs-read-file!]]
            [knoxx.backend.tools.shared :refer [maybe-tool-update! type-optional]]))

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
                            (let [rel-path (normalize-relative-path (path-relative node-path docs-path abs-path))
                                  name (path-basename node-path abs-path)
                                  indexed-meta (indexed-meta runtime config db-id rel-path)]
                              (if (text-like-path? rel-path)
                                (-> (fs-read-file! node-fs abs-path "utf8")
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
        abs-path (path-resolve node-path (:docsPath profile) rel-path)
        rel-to-root (path-relative node-path (:docsPath profile) abs-path)
        max-chars (max 500 (min 20000 (or max-chars 6000)))]
    (if (or (str/starts-with? rel-to-root "..") (path-is-absolute? node-path rel-to-root))
      (js/Promise.reject (js/Error. "Path escapes active docs root"))
      (-> (fs-read-file! node-fs abs-path "utf8")
          (.then (fn [content]
                   (let [[clipped truncated?] (clip-text content max-chars)]
                     {:database {:id (:id profile)
                                 :name (:name profile)}
                     :path rel-path
                     :truncated truncated?
                     :content clipped}))))))))


(defn create-semantic-custom-tools
  ([runtime config] (create-semantic-custom-tools runtime config nil))
  ([runtime config auth-context]
   (let [Type (aget runtime "Type")
         query-params (.Object Type
                               #js {:query (.String Type #js {:description "Natural-language semantic search query for the active Knoxx corpus."})
                                    :topK (type-optional Type (.Number Type #js {:description "Maximum number of matches to return." :minimum 1 :maximum 10}))
                                    :maxSnippetChars (type-optional Type (.Number Type #js {:description "Maximum snippet length per hit." :minimum 160 :maximum 1200}))})
         read-params (.Object Type
                              #js {:path (.String Type #js {:description "Relative document path returned by semantic_query or visible in the active corpus."})
                                   :maxChars (type-optional Type (.Number Type #js {:description "Maximum characters of document content to return." :minimum 500 :maximum 20000}))})
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
                                 :label "Read Document"
                                 :description "Read a document by relative path from the active Knoxx corpus."
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

