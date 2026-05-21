(ns knoxx.backend.infra.openplanner.semantic
  "Semantic search and document-reading tools for the active Knoxx corpus."
  (:require [clojure.string :as str]
            [knoxx.backend.infra.auth.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.infra.document-state :refer [active-agent-profile ensure-dir! list-files-recursive! normalize-relative-path indexed-meta]]
            [knoxx.backend.infra.openplanner.memory :refer [openplanner-semantic-search!]]
            [knoxx.backend.domain.text :refer [search-tokens text-like-path? clip-text snippet-around tool-text-result semantic-read-result-text openplanner-semantic-search-text]]
            [knoxx.backend.domain.media :refer [path-relative path-basename path-resolve path-is-absolute? fs-read-file!]]
            [knoxx.backend.domain.tools :refer [maybe-tool-update! create-tool-obj]]
            ["node:fs/promises" :as fs]
            ["node:path" :as node-path]))

(def query-params
  [:map
   [:query {:description "Natural-language semantic search query for the active Knoxx corpus."} :string]
   [:topK {:optional true :description "Maximum number of matches to return."} [:int {:min 1 :max 10}]]
   [:maxSnippetChars {:optional true :description "Maximum snippet length per hit."} [:int {:min 160 :max 1200}]]])





(defn semantic-query-execute [runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        query (or (aget params "query") "")
        top-k (or (aget params "topK") (aget params "top_k"))
        max-snippet-chars (or (aget params "maxSnippetChars") (aget params "max_snippet_chars"))]
    (maybe-tool-update! on-update "Searching corpus via OpenPlanner…")
    (-> (openplanner-semantic-search! config {:query query :k (or top-k 5)})
        (.then (fn [result]
                 (tool-text-result (openplanner-semantic-search-text result) result))))))


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
