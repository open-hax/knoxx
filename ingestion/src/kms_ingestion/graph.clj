(ns kms-ingestion.graph
  "Canonical graph helpers for lake-backed devel/web topology."
  (:require
   [clojure.string :as str])
  (:import
   [java.util UUID]))

(def doc-exts
  #{".md" ".markdown" ".txt" ".rst" ".org" ".adoc" ".tex" ".bib"})

(def code-exts
  #{".clj" ".cljs" ".cljc" ".edn" ".ts" ".tsx" ".js" ".jsx"
    ".py" ".rb" ".php" ".java" ".kt" ".go" ".rs" ".c" ".cc" ".cpp" ".h" ".hpp"
    ".sh" ".bash" ".zsh" ".fish" ".sql"})

(def config-exts
  #{".json" ".jsonc" ".yaml" ".yml" ".toml" ".ini" ".cfg" ".conf" ".env" ".properties"})

(def data-exts
  #{".jsonl" ".csv" ".tsv" ".parquet"})

(def markdown-exts
  #{".md" ".mdx" ".markdown"})

(def js-ts-exts
  #{".ts" ".tsx" ".js" ".jsx" ".mjs" ".cjs"})

(def clj-exts
  #{".clj" ".cljs" ".cljc"})

(defn file-ext
  [path]
  (let [p (str/lower-case (or path ""))
        idx (.lastIndexOf p ".")]
    (if (neg? idx) "" (subs p idx))))

(defn normalize-rel-path
  [path]
  (-> (str path)
      (str/replace "\\" "/")
      (str/replace #"^\./" "")
      (str/replace #"/+$" "")))

(defn node-type-for-path
  [path]
  (let [p (str/lower-case (normalize-rel-path path))
        ext (file-ext p)
        name (last (str/split p #"/"))]
    (cond
      (or (str/includes? p "/data/")
          (str/includes? p "/datasets/")
          (data-exts ext))
      :data

      (or (str/includes? p "/docs/")
          (str/includes? p "/specs/")
          (str/includes? p "/notes/")
          (str/includes? p "/inbox/")
          (doc-exts ext))
      :docs

      (or (= name "dockerfile")
          (str/starts-with? name ".env")
          (str/includes? p "/config/")
          (str/includes? p "/configs/")
          (config-exts ext))
      :config

      (code-exts ext)
      :code

      :else
      :docs)))

(defn- basename
  [path]
  (let [parts (->> (str/split (normalize-rel-path path) #"/") (remove str/blank?) vec)]
    (or (peek parts) (normalize-rel-path path))))

(defn- dirname
  [path]
  (let [parts (->> (str/split (normalize-rel-path path) #"/") (remove str/blank?) vec)]
    (if (<= (count parts) 1)
      ""
      (str/join "/" (pop parts)))))

(defn- split-segments
  [path]
  (->> (str/split (normalize-rel-path path) #"/")
       (remove #(or (str/blank? %) (= % ".")))))

(defn- normalize-segments
  [segments]
  (loop [out []
         xs (seq segments)]
    (if-let [seg (first xs)]
      (cond
        (= seg ".")
        (recur out (next xs))

        (= seg "..")
        (recur (if (seq out) (vec (butlast out)) []) (next xs))

        :else
        (recur (conj out seg) (next xs)))
      (str/join "/" out))))

(defn- resolve-relative-path
  [from-file target]
  (let [from-dir (split-segments (dirname from-file))
        target-path (normalize-rel-path target)
        target-segments (split-segments target-path)]
    (normalize-segments
     (if (str/starts-with? target-path "/")
       target-segments
       (concat from-dir target-segments)))))

(defn- trim-link-target
  [target]
  (-> (str target)
      (str/split #"#" 2)
      first
      (str/split #"\?" 2)
      first
      (str/trim)
      (str/replace #"^<|>$" "")))

(defn http-url?
  [value]
  (boolean (re-matches #"(?i)https?://.+" (str/trim (str value)))))

(defn- normalize-url
  [value]
  (let [raw (str/trim (str value))]
    (if (str/blank? raw)
      ""
      (try
        (let [u (java.net.URI. raw)
              scheme (some-> (.getScheme u) str/lower-case)
              host (some-> (.getHost u) str/lower-case)
              path (let [p (.getPath u)]
                     (if (str/blank? p) "/" p))
              query (.getQuery u)]
          (str scheme "://" host path (when (seq query) (str "?" query))))
        (catch Exception _ raw)))))

(defn index-context
  [existing-state file-metas]
  (let [paths (->> (concat
                    (map (comp normalize-rel-path :path val) existing-state)
                    (map (comp normalize-rel-path :path) file-metas))
                   (remove str/blank?)
                   (remove nil?)
                   set)
        md-basename->paths
        (reduce (fn [acc rel]
                  (if (markdown-exts (file-ext rel))
                    (update acc (str/lower-case (basename rel)) (fnil conj []) rel)
                    acc))
                {}
                paths)]
    {:file-set paths
     :md-basename-to-paths md-basename->paths}))

(defn extract-markdown-links
  [src]
  (let [wiki (for [[_ raw] (re-seq #"\[\[([^\]]+)\]\]" (or src ""))
                   :let [target (-> raw (str/split #"\|" 2) first trim-link-target)]
                   :when (seq target)]
               {:kind :wiki :target target})
        md (for [[_ raw] (re-seq #"\[[^\]]*\]\(([^)]+)\)" (or src ""))
                 :let [target (trim-link-target raw)]
                 :when (seq target)]
             {:kind :md :target target})
        angle (for [[_ raw] (re-seq #"<\s*(https?://[^\s>]+)\s*>" (or src ""))
                    :let [target (trim-link-target raw)]
                    :when (seq target)]
                {:kind :angle :target target})]
    (vec (concat wiki md angle))))

(defn extract-js-ts-imports
  [src]
  (let [patterns [#"\bimport\s+(?:type\s+)?[^;]*?\sfrom\s*[\"']([^\"']+)[\"']"
                  #"\bexport\s+[^;]*?\sfrom\s*[\"']([^\"']+)[\"']"
                  #"\bimport\s*\(\s*[\"']([^\"']+)[\"']\s*\)"
                  #"\brequire\s*\(\s*[\"']([^\"']+)[\"']\s*\)"]]
    (->> patterns
         (mapcat #(map second (re-seq % (or src ""))))
         (map str/trim)
         (remove str/blank?)
         distinct
         vec)))

(defn extract-clojure-requires
  [src]
  (->> (re-seq #"\[\s*([a-zA-Z0-9][a-zA-Z0-9_.\-]*)\s*(?::as|:refer|:refer-macros|:include-macros|\]|\s)" (or src ""))
       (map second)
       (map str/trim)
       (remove #(or (str/blank? %) (str/starts-with? % ":") (re-find #"^\d" %)))
       distinct
       vec))

(defn resolve-internal-link
  [{:keys [from-file target file-set md-basename-to-paths]}]
  (let [cleaned (trim-link-target target)]
    (cond
      (or (str/blank? cleaned) (str/starts-with? cleaned "#") (http-url? cleaned))
      nil

      :else
      (let [rel-candidate (if (str/starts-with? cleaned "/")
                            (normalize-rel-path (subs cleaned 1))
                            (resolve-relative-path from-file cleaned))
            direct-match (when (contains? file-set rel-candidate) rel-candidate)
            md-match (when (str/blank? (file-ext rel-candidate))
                       (let [md-path (str rel-candidate ".md")]
                         (when (contains? file-set md-path) md-path)))
            basename-match (let [key (let [base (str/lower-case (basename rel-candidate))]
                                       (if (str/ends-with? base ".md") base (str base ".md")))
                                 hits (get md-basename-to-paths key)]
                             (when (= 1 (count hits))
                               (first hits)))]
        (or direct-match md-match basename-match)))))

(defn resolve-js-ts-import
  [{:keys [from-file spec file-set]}]
  (let [raw (str/trim (str spec))]
    (when (or (str/starts-with? raw ".") (str/starts-with? raw "/"))
      (let [base (if (str/starts-with? raw "/")
                   (normalize-rel-path (subs raw 1))
                   (resolve-relative-path from-file raw))
            candidates (if (seq (file-ext base))
                         [base]
                         (concat
                          (map #(str base %) [".ts" ".tsx" ".js" ".jsx" ".mjs" ".cjs"])
                          (map #(str base "/index" %) [".ts" ".tsx" ".js" ".jsx" ".mjs" ".cjs"])))
            match (first (filter #(contains? file-set %) candidates))]
        match))))

(defn resolve-clojure-require
  [{:keys [ns file-set]}]
  (let [base (-> ns str (str/replace "." "/") (str/replace "-" "_"))
        candidates (map #(str base %) [".clj" ".cljs" ".cljc"])]
    (first (filter #(contains? file-set %) candidates))))

(defn devel-node-id
  [tenant-id rel-path]
  (str tenant-id ":file:" (normalize-rel-path rel-path)))

(defn web-node-id
  [url]
  (str "web:url:" (normalize-url url)))

(defn stable-graph-event-id
  [prefix value]
  (str prefix ":" (UUID/nameUUIDFromBytes (.getBytes (str prefix "|" value) "UTF-8"))))

(defn- graph-node-kind
  [project node-type extra]
  (cond
    (= project "devel") "file"
    (= project "web") "url"
    (= node-type "user") "user"
    (= node-type "post") "post"
    :else (or (:node_kind extra) "node")))

(defn graph-node-event
  [{:keys [ts source project session node-id node-type label extra text]}]
  {:schema "openplanner.event.v1"
   :id (stable-graph-event-id "graph.node" node-id)
   :ts ts
   :source source
   :kind "graph.node"
   :source_ref {:project project
                :session session
                :message node-id}
   :text text
   :meta {:author source
          :tags (->> ["graph" project node-type] (remove str/blank?) vec)}
   :extra (merge {:lake project
                  :node_id node-id
                  :node_type node-type
                  :node_kind (graph-node-kind project node-type extra)
                  :label label
                  :entity_key node-id}
                 extra)})

(defn graph-edge-event
  [{:keys [ts source project session edge-type source-node-id target-node-id source-lake target-lake extra text]}]
  (let [edge-id (or (:edge_id extra)
                    (stable-graph-event-id "graph.edge" (str edge-type "|" source-node-id "|" target-node-id)))]
    {:schema "openplanner.event.v1"
     :id edge-id
     :ts ts
     :source source
     :kind "graph.edge"
     :source_ref {:project project
                  :session session
                  :message edge-id}
     :text text
     :meta {:author source
            :tags (->> ["graph" project edge-type] (remove str/blank?) vec)}
     :extra (merge {:lake project
                    :edge_id edge-id
                    :edge_type edge-type
                    :source_node_id source-node-id
                    :target_node_id target-node-id
                    :source_lake source-lake
                    :target_lake target-lake}
                   extra)}))

(defn- preview-text
  [content]
  (let [s (-> (str content)
              (str/replace #"\s+" " ")
              str/trim)]
    (if (> (count s) 400)
      (subs s 0 400)
      s)))

(defn devel-node-event
  [{:keys [tenant-id source-id ts path content content-hash]}]
  (let [node-type (name (node-type-for-path path))
        node-id (devel-node-id tenant-id path)]
    (graph-node-event
     {:ts ts
      :source "kms-ingestion"
      :project tenant-id
      :session source-id
      :node-id node-id
      :node-type node-type
      :label (basename path)
      :extra {:path (normalize-rel-path path)
              :content_hash content-hash
              :preview (preview-text content)}
      :text nil})))

(defn devel-stub-node-event
  [{:keys [tenant-id source-id ts path]}]
  (let [node-type (name (node-type-for-path path))]
    (graph-node-event
     {:ts ts
      :source "kms-ingestion"
      :project tenant-id
      :session source-id
      :node-id (devel-node-id tenant-id path)
      :node-type node-type
      :label (basename path)
      :extra {:path (normalize-rel-path path)}
      :text nil})))

(defn web-unvisited-node-event
  [{:keys [source-id ts url discovered-from]}]
  (let [normalized (normalize-url url)
        node-id (web-node-id normalized)]
    (graph-node-event
     {:ts ts
      :source "kms-ingestion"
      :project "web"
      :session source-id
      :node-id node-id
      :node-type "unvisited"
      :label normalized
      :extra {:url normalized
              :visit_status "unvisited"
              :discovered_from discovered-from}
      :text nil})))

(defn collect-devel-graph-events
  [{:keys [tenant-id source-id file context ts]}]
  (let [path (normalize-rel-path (:path file))
        node-id (devel-node-id tenant-id path)
        node-event (devel-node-event {:tenant-id tenant-id
                                      :source-id source-id
                                      :ts ts
                                      :path path
                                      :content (:content file)
                                      :content-hash (:content-hash file)})
        node-type (node-type-for-path path)
        markdown-events
        (when (= node-type :docs)
          (mapcat
           (fn [{:keys [target]}]
             (let [resolved (resolve-internal-link {:from-file path
                                                   :target target
                                                   :file-set (:file-set context)
                                                   :md-basename-to-paths (:md-basename-to-paths context)})]
               (cond
                 (http-url? target)
                 (let [normalized (normalize-url target)
                       web-node (web-unvisited-node-event {:source-id source-id
                                                           :ts ts
                                                           :url normalized
                                                           :discovered-from path})]
                   [web-node
                    (graph-edge-event
                     {:ts ts
                      :source "kms-ingestion"
                      :project tenant-id
                      :session source-id
                      :edge-type "external_web_link"
                      :source-node-id node-id
                      :target-node-id (:message (:source_ref web-node))
                      :source-lake tenant-id
                      :target-lake "web"
                      :extra {:source_path path
                              :target_url normalized}
                      :text nil})])

                 (seq resolved)
                 [(devel-stub-node-event {:tenant-id tenant-id :source-id source-id :ts ts :path resolved})
                  (graph-edge-event
                   {:ts ts
                    :source "kms-ingestion"
                    :project tenant-id
                    :session source-id
                    :edge-type "local_markdown_link"
                    :source-node-id node-id
                    :target-node-id (devel-node-id tenant-id resolved)
                    :source-lake tenant-id
                    :target-lake tenant-id
                    :extra {:source_path path
                            :target_path resolved}
                    :text nil})]

                 :else
                 [])))
           (extract-markdown-links (:content file))))
        js-events
        (when (= node-type :code)
          (mapcat
           (fn [spec]
             (when-let [resolved (resolve-js-ts-import {:from-file path :spec spec :file-set (:file-set context)})]
               [(devel-stub-node-event {:tenant-id tenant-id :source-id source-id :ts ts :path resolved})
                (graph-edge-event
                 {:ts ts
                  :source "kms-ingestion"
                  :project tenant-id
                  :session source-id
                  :edge-type "code_dependency"
                  :source-node-id node-id
                  :target-node-id (devel-node-id tenant-id resolved)
                  :source-lake tenant-id
                  :target-lake tenant-id
                  :extra {:source_path path
                          :target_path resolved
                          :spec spec}
                  :text nil})]))
           (extract-js-ts-imports (:content file))))
        clj-events
        (when (= node-type :code)
          (mapcat
           (fn [ns]
             (when-let [resolved (resolve-clojure-require {:ns ns :file-set (:file-set context)})]
               [(devel-stub-node-event {:tenant-id tenant-id :source-id source-id :ts ts :path resolved})
                (graph-edge-event
                 {:ts ts
                  :source "kms-ingestion"
                  :project tenant-id
                  :session source-id
                  :edge-type "code_dependency"
                  :source-node-id node-id
                  :target-node-id (devel-node-id tenant-id resolved)
                  :source-lake tenant-id
                  :target-lake tenant-id
                  :extra {:source_path path
                          :target_path resolved
                          :spec ns}
                  :text nil})]))
           (extract-clojure-requires (:content file))))
        events (concat [node-event] markdown-events js-events clj-events)]
    (->> events
         (remove nil?)
         (reduce (fn [acc ev] (assoc acc (:id ev) ev)) {})
         vals
         vec)))
