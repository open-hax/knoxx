(ns kms-ingestion.api.workspace-support
  "Workspace browsing and preview handlers for the ingestion API."
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [kms-ingestion.api.common :as common]
   [kms-ingestion.api.query-support :as query]
   [kms-ingestion.config :as config]
   [kms-ingestion.db :as db])
  (:import
   [java.io File]))

(def text-preview-extensions
  #{".md" ".markdown" ".txt" ".rst" ".org" ".adoc"
    ".clj" ".cljs" ".cljc" ".edn" ".ts" ".tsx" ".js" ".jsx" ".py" ".sh" ".sql"
    ".json" ".jsonl" ".yaml" ".yml" ".toml" ".ini" ".cfg" ".conf" ".env" ".properties"
    ".html" ".css" ".xml" ".csv" ".tsv"})

(defn workspace-root-file []
  (let [base (.getCanonicalFile (io/file (config/workspace-path)))
        base-packages (.getCanonicalFile (io/file base "packages"))
        devel-root (.getCanonicalFile (io/file base "devel"))
        devel-packages (.getCanonicalFile (io/file devel-root "packages"))]
    (cond
      (.exists ^File base-packages)
      base

      (.exists ^File devel-packages)
      devel-root

      :else
      base)))

(defn resolve-workspace-file
  [path]
  (let [root (workspace-root-file)
        candidate (if (or (nil? path) (str/blank? path))
                    root
                    (let [p (str path)]
                      (if (.isAbsolute (File. p))
                        (io/file p)
                        (io/file root p))))
        canonical (.getCanonicalFile ^File candidate)
        root-path (.getPath root)
        candidate-path (.getPath canonical)]
    (when (or (= candidate-path root-path)
              (str/starts-with? candidate-path (str root-path File/separator)))
      canonical)))

(defn rel-workspace-path
  [^File file]
  (let [root (workspace-root-file)
        root-path (.toPath root)
        file-path (.toPath (.getCanonicalFile file))]
    (if (= (.toString root-path) (.toString file-path))
      ""
      (str (.relativize root-path file-path)))))

(defn path-segments
  [path]
  (->> (str/split (or path "") #"/")
       (remove str/blank?)))

(defn summarize-entry-states
  [tenant-id current-path entries]
  (let [rows (db/list-file-states-under-path tenant-id current-path)
        current-depth (count (path-segments current-path))
        row-maps (map (fn [row]
                        (let [row-path (str (:path row))
                              segments (path-segments row-path)]
                          {:path row-path
                           :segments segments
                           :status (:status row)
                           :chunks (:chunks row)
                           :metadata (common/json->clj (:metadata row))
                           :last_ingested_at (:last_ingested_at row)}))
                      rows)]
    (into {}
          (map (fn [entry]
                 (let [entry-path (:path entry)
                       prefix-segments (path-segments entry-path)
                       exact (some #(when (= (:path %) entry-path) %) row-maps)
                       subtree (filter (fn [row]
                                         (let [row-segments (:segments row)]
                                           (and (> (count row-segments) current-depth)
                                                (= prefix-segments (take (count prefix-segments) row-segments)))))
                                       row-maps)
                       relevant (concat (when exact [exact]) subtree)
                       ingested-count (count (filter #(= "ingested" (:status %)) relevant))
                       failed-count (count (filter #(= "failed" (:status %)) relevant))
                       latest-ingested (last (sort (remove nil? (map :last_ingested_at relevant))))
                       last-error (->> relevant
                                       (filter #(= "failed" (:status %)))
                                       (map #(get-in % [:metadata :error]))
                                       (remove str/blank?)
                                       last)
                       status (cond
                                (= "failed" (:status exact)) "failed"
                                (= "ingested" (:status exact)) "ingested"
                                (and (seq subtree) (zero? ingested-count) (pos? failed-count)) "failed"
                                (seq subtree) "partial"
                                :else "not_ingested")]
                   [entry-path {:ingested_count ingested-count
                                :failed_count failed-count
                                :ingestion_status status
                                :last_ingested_at (some-> latest-ingested str)
                                :last_error last-error}]))
               entries))))

(defn file-ext
  [name]
  (let [n (str/lower-case (or name ""))
        idx (.lastIndexOf n ".")]
    (if (neg? idx) "" (subs n idx))))

(defn text-previewable?
  [^File file]
  (let [ext (file-ext (.getName file))]
    (contains? text-preview-extensions ext)))

(defn browse-path-handler
  [request]
  (let [requested (or (-> request :query-params :path)
                      (get (:query-params request) "path"))
        tenant-id (common/get-tenant-id request)
        target (resolve-workspace-file requested)]
    (println "[BROWSE] requested=" requested "query-params=" (:query-params request) "target=" (some-> target .getPath))
    (cond
      (nil? target)
      {:status 400 :body {:error "path must stay within workspace root"}}

      (not (.exists ^File target))
      {:status 404 :body {:error "path not found"}}

      (not (.isDirectory ^File target))
      {:status 400 :body {:error "path is not a directory"}}

      :else
      (let [entries (->> (or (.listFiles ^File target) (into-array File []))
                         (sort-by (fn [^File f] [(if (.isDirectory f) 0 1) (.toLowerCase (.getName f))]))
                         (map (fn [^File f]
                                {:name (.getName f)
                                 :path (rel-workspace-path f)
                                 :type (if (.isDirectory f) "dir" "file")
                                 :size (when (.isFile f) (.length f))
                                 :previewable (and (.isFile f) (text-previewable? f))}))
                         vec)
            state-by-path (summarize-entry-states tenant-id (rel-workspace-path target) entries)
            entries (mapv (fn [entry]
                            (merge entry (get state-by-path (:path entry)
                                              {:ingestion_status "not_ingested"
                                               :ingested_count 0
                                               :failed_count 0
                                               :last_ingested_at nil
                                               :last_error nil})))
                          entries)]
        {:status 200
         :body {:workspace_root (str (workspace-root-file))
                :current_path (rel-workspace-path target)
                :entries entries}}))))

(defn semantic-file-search-handler
  [request]
  (let [tenant-id (common/get-tenant-id request)
        body (common/request-body->map request)
        q (:q body)
        role (:role body)
        projects (:projects body)
        kinds (:kinds body)
        path-prefix (or (:path body) (:path_prefix body) (:path-prefix body))
        limit (or (:limit body) 20)]
    (if (str/blank? q)
      {:status 400 :body {:error "q is required"}}
      {:status 200
       :body (query/semantic-file-search {:tenant-id tenant-id
                                          :q q
                                          :role role
                                          :projects projects
                                          :kinds kinds
                                          :path-prefix path-prefix
                                          :limit limit})})))

(defn preview-file-handler
  [request]
  (let [requested (or (-> request :query-params :path)
                      (get (:query-params request) "path"))
        target (resolve-workspace-file requested)]
    (cond
      (nil? target)
      {:status 400 :body {:error "path must stay within workspace root"}}

      (not (.exists ^File target))
      {:status 404 :body {:error "file not found"}}

      (not (.isFile ^File target))
      {:status 400 :body {:error "path is not a file"}}

      (not (text-previewable? target))
      {:status 400 :body {:error "file is not previewable as text"}}

      :else
      (let [content (slurp target)
            limit 12000
            truncated (> (count content) limit)]
        {:status 200
         :body {:path (rel-workspace-path target)
                :size (.length ^File target)
                :truncated truncated
                :content (if truncated (subs content 0 limit) content)}}))))

(defn write-file-handler
  [request]
  (let [body (common/request-body->map request)
        requested (:path body)
        old-requested (:old_path body)
        content (str (or (:content body) ""))
        target (resolve-workspace-file requested)
        old-target (when (some? old-requested)
                     (resolve-workspace-file old-requested))]
    (cond
      (str/blank? (str requested))
      {:status 400 :body {:error "path is required"}}

      (nil? target)
      {:status 400 :body {:error "path must stay within workspace root"}}

      (and (some? old-requested) (nil? old-target))
      {:status 400 :body {:error "old_path must stay within workspace root"}}

      (and (.exists ^File target) (.isDirectory ^File target))
      {:status 400 :body {:error "path is a directory"}}

      :else
      (do
        (some-> ^File (.getParentFile ^File target) .mkdirs)
        (spit target content)
        (when (and old-target
                   (.exists ^File old-target)
                   (.isFile ^File old-target)
                   (not= (.getCanonicalPath ^File old-target)
                         (.getCanonicalPath ^File target)))
          (.delete ^File old-target))
        {:status 200
         :body {:ok true
                :path (rel-workspace-path target)
                :size (.length ^File target)}}))))
