(ns knoxx.backend.text
  (:require [clojure.string :as str]
            [knoxx.backend.http :as backend-http]))

(declare value->preview-text)

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

(def preview-preferred-keys
  [:content
   :text
   :answer
   :message
   :result
   :output
   :preview
   :summary
   :translated_text
   :corrected_text
   :snippet])

(def preview-collection-keys
  [:rows :hits :results :sources :items :documents])

(defn- safe-get
  [m k]
  (when (map? m)
    (or (get m k)
        (when (keyword? k)
          (get m (name k)))
        (when (string? k)
          (get m (keyword k))))))

(defn- format-preview-scalar
  [value]
  (cond
    (nil? value) "null"
    (string? value) (let [trimmed (str/trim value)]
                      (if (str/blank? trimmed) "" trimmed))
    (number? value) (str value)
    (boolean? value) (str value)
    :else (str value)))

(defn- summarize-structured
  ([value] (summarize-structured value 0))
  ([value depth]
   (when (< depth 4)
     (cond
       (or (string? value) (number? value) (boolean? value) (nil? value))
       (let [s (format-preview-scalar value)]
         (when-not (str/blank? s) s))

       (map? value)
       (or (some (fn [k]
                   (when-let [hit (summarize-structured (safe-get value k) (inc depth))]
                     hit))
                 preview-preferred-keys)
           (some (fn [k]
                   (when-let [hit (summarize-structured (safe-get value k) (inc depth))]
                     hit))
                 preview-collection-keys)
           nil)

       (sequential? value)
       (let [items (vec (take 8 value))
             looks-like-content-parts? (and (seq items)
                                           (every? map? items)
                                           (every? (fn [m]
                                                     (let [t (or (safe-get m :text) (safe-get m "text"))
                                                           ty (or (safe-get m :type) (safe-get m "type"))]
                                                       (and (string? t)
                                                            (or (nil? ty) (= ty "text") (= ty "output_text")))))
                                                   items))]
         (if looks-like-content-parts?
           (let [joined (->> items
                             (map (fn [m] (or (safe-get m :text) (safe-get m "text"))))
                             (map #(str/trim (str %)))
                             (remove str/blank?)
                             (str/join "\n\n"))]
             (when-not (str/blank? joined) joined))
           (let [summaries (->> items
                                (map #(summarize-structured % (inc depth)))
                                (remove #(or (nil? %) (str/blank? %))))]
             (when (seq summaries)
               (str/join "\n" (map (fn [item] (str "- " item)) summaries))))))

       :else nil))))

(defn- markdown-bullets
  ([value] (markdown-bullets value 0))
  ([value depth]
   (when (< depth 4)
     (cond
       (or (string? value) (number? value) (boolean? value) (nil? value))
       (let [s (format-preview-scalar value)]
         (when-not (str/blank? s) (str "- " s)))

       (map? value)
       (let [keys (take 20 (keys value))]
         (when (seq keys)
           (str/join
            "\n"
            (map (fn [k]
                   (let [child (get value k)
                         label (cond
                                 (keyword? k) (name k)
                                 (string? k) k
                                 :else (str k))
                         nested (markdown-bullets child (inc depth))]
                     (if (and nested (or (map? child) (sequential? child)))
                       (str "- " label ":\n" (str/replace nested #"(?m)^-" "  -"))
                       (str "- " label ": " (or (summarize-structured child (inc depth)) (format-preview-scalar child))))))
                 keys))))

       (sequential? value)
       (let [items (take 20 value)
             rendered (->> items
                           (map #(markdown-bullets % (inc depth)))
                           (remove #(or (nil? %) (str/blank? %))))]
         (when (seq rendered)
           (str/join "\n" rendered)))

       :else nil))))

(defn- extract-json-keys
  [text]
  (->> (re-seq #"\"([A-Za-z0-9_\-]+)\"\s*:" (str text))
       (map second)
       distinct
       (take 16)
       vec))

(defn- unescape-json-fragment
  [text]
  (-> (str text)
      (str/replace #"\\\\n" "\n")
      (str/replace #"\\\\t" "\t")
      (str/replace #"\\\\r" "\r")
      (str/replace #"\\\\\"" "\"")
      (str/replace #"\\\\\\\\" "\\")))

(defn- extract-json-string-field
  "Best-effort extraction for truncated/invalid JSON: pulls the string value of a field like \"text\"."
  [text field]
  (let [s (str text)
        needle (str "\"" field "\"")
        idx (.indexOf s needle)]
    (when (>= idx 0)
      (let [colon (.indexOf s ":" (+ idx (count needle)))
            quote (.indexOf s "\"" (if (>= colon 0) (inc colon) (+ idx (count needle))))]
        (when (>= quote 0)
          (loop [i (inc quote)
                 escaped? false
                 acc (transient [])]
            (if (>= i (count s))
              (unescape-json-fragment (apply str (persistent! acc)))
              (let [ch (.charAt s i)]
                (cond
                  escaped? (recur (inc i) false (conj! acc (str "\\" ch)))
                  (= ch "\\") (recur (inc i) true acc)
                  (= ch "\"") (unescape-json-fragment (apply str (persistent! acc)))
                  :else (recur (inc i) false (conj! acc ch)))))))))))

(defn- summarize-json-string
  [text]
  (let [trimmed (str/trim (str text))]
    (when (and (not (str/blank? trimmed))
               (or (str/starts-with? trimmed "{")
                   (str/starts-with? trimmed "[")))
      (try
        (let [parsed (js->clj (.parse js/JSON trimmed) :keywordize-keys true)
              summarized (or (summarize-structured parsed)
                             (markdown-bullets parsed))]
          (when-not (str/blank? (str summarized))
            summarized))
        (catch :default _
          (or (some-> (extract-json-string-field trimmed "text") str/trim not-empty)
              (when-let [keys (seq (extract-json-keys trimmed))]
                (str "- JSON (truncated/invalid preview) keys: " (str/join ", " keys)))))))))

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

(defn openplanner-semantic-search-text
  [result]
  (let [query (:query result)
        mode (:mode result)
        hits (seq (:hits result))]
    (if hits
      (str "OpenPlanner semantic search for: " query
           "\nMode: " (name mode)
           "\n\n"
           (str/join
            "\n\n"
            (map-indexed
             (fn [idx hit]
               (let [metadata (or (:metadata hit) {})
                     doc (or (:document hit) "")
                     source-path (or (:sourcePath metadata) (:path metadata) "")
                     distance (:distance hit)
                     kind (or (:kind metadata) (:kind hit) "")]
                 (str (inc idx) ". [" (or kind "doc") "]"
                      (when (number? distance)
                        (str " distance=" (.toFixed (js/Number. distance) 3)))
                      (when-not (str/blank? source-path)
                        (str "\n   path: " source-path))
                      (when-let [title (:title metadata)]
                        (str "\n   title: " title))
                      "\n   " (or (value->preview-text doc 320) ""))))
             hits)))
      (str "OpenPlanner semantic search for: " query "\nNo indexed documents matched."))))

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
               (string? value)
               (let [trimmed (str/trim (str value))
                     lowered (str/lower-case trimmed)]
                 (cond
                   (or (= lowered "null") (= lowered "undefined")) ""
                   :else (or (summarize-json-string value)
                             value)))

               (or (map? value) (vector? value) (seq? value))
               (or (summarize-structured value)
                   (markdown-bullets value)
                   (pr-str value))

               :else
               (try
                 (let [clj-value (js->clj value :keywordize-keys true)]
                   (or (summarize-structured clj-value)
                       (markdown-bullets clj-value)
                       (let [json (.stringify js/JSON value nil 2)]
                         (if (string? json) json (str value)))))
                 (catch :default _
                   (str value))))
         [text truncated] (clip-text raw max-chars)]
     (if (str/blank? text)
       ;; Guaranteed fallback: for non-nil, non-scalar objects, always produce
       ;; a JSON preview so callers (e.g. tool input_preview) never get nil
       ;; for a real value that simply didn't match preferred keys.
       (when (and (not (nil? value))
                  (not= value js/undefined)
                  (not (string? value))
                  (not (number? value))
                  (not (boolean? value)))
         (try
           (let [json (.stringify js/JSON value)]
             (when (and (string? json) (not (str/blank? json)))
               (let [[jt jtrunc] (clip-text json max-chars)]
                 (if jtrunc (str jt "…") jt))))
           (catch :default _ nil)))
       (if truncated
         (str text "…")
         text)))))

(defn float-format
  [x]
  (when (number? x)
    (.toFixed (js/Number x) 3)))

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

(defn websearch-result-text
  [{:keys [output sources model]}]
  (str "Web search"
       (when model (str " via " model))
       "\n\n"
       (or output "")
       (when (seq sources)
         (str "\n\nSources:\n"
              (str/join "\n"
                        (map (fn [source]
                               (str "- " (or (:title source) (:url source))
                                    " — " (:url source)))
                             sources))))))

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
        edges (vec (or (:edges result) []))
        stats (:stats result)]
    (if (seq nodes)
      (let [node-text (str/join
                       "\n\n"
                       (map-indexed
                        (fn [idx node]
                          (let [data (or (:data node) {})
                                label (or (:label node) (:id node))
                                text (or (:text node) (:preview (:data node)))]
                            (str (inc idx) ". [" (:lake node) "/" (:nodeType node) "] " label
                                 (when-let [s (:score node)]
                                   (str " (score=" (float-format s) ")"))
                                 "\n   id=" (:id node)
                                 (when-let [path (or (:path data) (some-> (:id node) (str/replace #"^[^:]+:" "")))]
                                   (str "\n   path=" path))
                                 (when-let [url (:url data)]
                                   (str "\n   url=" url))
                                 (when text
                                   (str "\n   text=" (or (value->preview-text text 280) ""))))))
                        nodes))
            edge-text (when (seq edges)
                        (str/join
                         "\n"
                         (map (fn [edge]
                                (let [etype (or (:edgeType edge) (when-let [sim (:similarity edge)]
                                                                    (str "sim=" (float-format sim))))]
                                  (str "- [" (or etype "link") "] " (:source edge) " -> " (:target edge))))
                              edges)))
            clusters-text (when-let [clusters (seq (:clusters result))]
                            (str "\n\nClusters:\n"
                                 (str/join "\n"
                                           (map (fn [c]
                                                  (str "  " (:lake c) ": " (:count c) " nodes"
                                                       (when-let [top (seq (:topNodes c))]
                                                         (str "\n    top: " (str/join ", " (map :id top))))))
                                                clusters))))]
        (str "Knowledge graph query"
             (when-let [q (:query result)]
               (str "\nQuery: " q))
             (when-let [projects (seq (:projects result))]
               (str "\nProjects: " (str/join ", " projects)))
             (when stats
               (str "\nStats: " (pr-str stats)))
             "\n\nNodes:\n"
             node-text
             edge-text
             clusters-text))
      (str "Knowledge graph query returned no matching nodes."
           (when stats (str " " (pr-str stats)))))))
