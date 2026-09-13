(ns knoxx.backend.extern.cms-store
  "CMS filesystem boundary. JSON is read only for migration; new history is EDN."
  (:require ["node:fs" :as fs]
            ["node:path" :as path]
            [clio.extern.js.fs :as clio-fs]
            [knoxx.backend.extern.node-env :as env]))

(defn configured? []
  (boolean (and (env/variable "KNOXX_PUBLICATION_CONTENT_ROOT")
                (env/variable "KNOXX_GENERATED_CONTRACTS_DIR"))))

(defn roots []
  {:content (or (env/variable "KNOXX_PUBLICATION_CONTENT_ROOT")
                (throw (ex-info "CMS content root is not configured" {:status 503})))
   :resources (or (env/variable "KNOXX_GENERATED_CONTRACTS_DIR")
                  (throw (ex-info "Generated resource root is not configured" {:status 503})))})

(defn paths [org id]
  (let [{:keys [content resources]} (roots)
        root (.join path content "cms" org)]
    {:root root :metadata (.join path root (str id ".json"))
     :source (.join path root (str id ".md"))
     :history (.join path content ".ημ" "cms" org)
     :manifest (.join path resources (str "cms-" org "-" id ".edn"))
     :garden (.join path resources (str "cms-garden-" org ".edn"))
     :resource-root resources}))

(defn legacy-record [org id]
  (try
    (js->clj (.parse js/JSON (.readFileSync fs (:metadata (paths org id)) "utf8"))
              :keywordize-keys true)
    (catch :default e (if (= "ENOENT" (.-code e)) nil (throw e)))))

(defn legacy-ids [org]
  (try
    (->> (.readdirSync fs (:root (paths org "index"))) array-seq
         (filter #(.endsWith % ".json")) (mapv #(.slice % 0 -5)))
    (catch :default e (if (= "ENOENT" (.-code e)) [] (throw e)))))

(defn create-resource! [target value]
  (.mkdirSync fs (.dirname path target) #js {:recursive true :mode 448})
  ;; Publish a whole inode. Later document revisions are projected in memory,
  ;; so they cannot overwrite publication-state edits to this manifest.
  (let [pending (str target "." (random-uuid) ".tmp")]
    (try
      (.writeFileSync fs pending (str (pr-str value) "\n") #js {:flag "wx" :mode 384})
      (try (.linkSync fs pending target)
           (catch :default e (when-not (= "EEXIST" (.-code e)) (throw e))))
      (finally (.rmSync fs pending #js {:force true})))))

(defn with-document-operation!
  "Run a synchronous CMS operation under Clio's kernel lock. Never await here."
  [org id operation]
  (let [target (.join path (:history (paths org id)) "operations" (str id ".lock"))]
    (.mkdirSync fs (.dirname path target) #js {:recursive true :mode 448})
    (try (clio-fs/create-exclusive! target)
         (catch :default e (when-not (= "EEXIST" (.-code e)) (throw e))))
    (let [lock (clio-fs/acquire-lock! target)]
      (try (operation) (finally (clio-fs/release-lock! lock))))))

(defn read-resource-text [target] (.readFileSync fs target "utf8"))

(defn replace-resource!
  "Atomically replace a complete manifest while its caller holds the operation lock."
  [target value]
  (let [pending (str target "." (random-uuid) ".tmp")]
    (try
      (.writeFileSync fs pending (str (pr-str value) "\n") #js {:flag "wx" :mode 384})
      (.renameSync fs pending target)
      (finally (.rmSync fs pending #js {:force true})))))
