(ns knoxx.backend.text
  (:require [clojure.string :as str]
            [knoxx.backend.http :as backend-http]))

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

(defn count-occurrences
  [text needle]
  (loop [idx 0
         total 0]
    (let [hit (.indexOf text needle idx)]
      (if (= hit -1)
        total
        (recur (+ hit (max 1 (count needle))) (inc total))))))

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

(defn clip-text
  ([text] (clip-text text 20000))
  ([text limit]
   (if (<= (count text) limit)
     [text false]
     [(subs text 0 limit) true])))

(defn replace-first
  [text old new]
  (let [idx (.indexOf text old)]
    (if (= idx -1)
      text
      (str (.slice text 0 idx)
           new
           (.slice text (+ idx (count old)))))))

(defn content-part-text
  [part]
  (cond
    (string? part) part
    (= (aget part "type") "text") (or (aget part "text") "")
    (= (aget part "type") "output_text") (or (aget part "text") "")
    :else ""))

(defn reasoning-part-text
  [part]
  (cond
    (string? part) ""
    (= (aget part "type") "reasoning") (or (aget part "text") "")
    (= (aget part "type") "reasoning_text") (or (aget part "text") "")
    (= (aget part "type") "thinking") (or (aget part "thinking")
                                             (aget part "text")
                                             "")
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

(defn assistant-message-reasoning-text
  [message]
  (let [content (aget message "content")
        merged (cond
                 (array? content) (apply str (map reasoning-part-text (array-seq content)))
                 :else "")]
    (cond
      (not (str/blank? merged)) merged
      (string? (aget message "reasoning_content")) (aget message "reasoning_content")
      (string? (aget message "reasoningContent")) (aget message "reasoningContent")
      (string? (aget message "reasoning_text")) (aget message "reasoning_text")
      (string? (aget message "reasoning")) (aget message "reasoning")
      (string? (aget message "thinking")) (aget message "thinking")
      :else "")))

(defn semantic-search-result-text
  [{:keys [database query results]}]
  (if (seq results)
    (str "Active corpus: " (:name database) "\n"
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
    (str "Active corpus: " (:name database) "\n"
         "Query: " query "\n\nNo strong semantic matches found.")))

(defn semantic-read-result-text
  [{:keys [database path content truncated]}]
  (str "Active corpus: " (:name database) "\n"
       "Path: " path
       (when truncated "\nNote: content truncated for tool safety.")
       "\n\n"
       content))

(defn value->preview-text
  ([value] (value->preview-text value 800))
  ([value max-chars]
   (let [raw (cond
               (backend-http/no-content? value) ""
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

(defn graph-query-result-text
  [result]
  (let [nodes (vec (or (:nodes result) []))
        edges (vec (or (:edges result) []))]
    (if (seq nodes)
      (let [node-text (str/join
                       "\n\n"
                       (map-indexed
                        (fn [idx node]
                          (let [data (or (:data node) {})]
                            (str (inc idx) ". [" (:lake node) "/" (:nodeType node) "] " (:label node)
                                 "\n   id=" (:id node)
                                 (when-let [path (:path data)]
                                   (str "\n   path=" path))
                                 (when-let [url (:url data)]
                                   (str "\n   url=" url))
                                 (when-let [preview (:preview data)]
                                   (str "\n   preview=" (or (value->preview-text preview 220) ""))))))
                        nodes))
            edge-text (when (seq edges)
                        (str/join
                         "\n"
                         (map (fn [edge]
                                (str "- [" (:edgeType edge) "] " (:source edge) " -> " (:target edge)))
                              edges)))]
        (str "Knowledge graph query"
             (when-let [q (:query result)]
               (str "\nQuery: " q))
             (when-let [projects (seq (:projects result))]
               (str "\nProjects: " (str/join ", " projects)))
             "\n\nNodes:\n"
             node-text
             (when edge-text
               (str "\n\nEdges:\n" edge-text))))
      "Knowledge graph query returned no matching nodes.")))
