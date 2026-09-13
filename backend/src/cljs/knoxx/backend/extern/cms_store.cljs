(ns knoxx.backend.extern.cms-store
  "Private per-organization CMS files. All paths are constructed from validated IDs."
  (:require ["node:fs/promises" :as fs]
            ["node:path" :as path]
            [knoxx.backend.extern.node-env :as env]))
(defn roots []
  {:content (or (env/variable "KNOXX_PUBLICATION_CONTENT_ROOT") (throw (ex-info "CMS content root is not configured" {:status 503})))
   :resources (or (env/variable "KNOXX_GENERATED_CONTRACTS_DIR") (throw (ex-info "Generated resource root is not configured" {:status 503})))})
(defn paths [org id]
  (let [{:keys [content resources]} (roots)
        root (.join path content "cms" org)]
    {:root root :metadata (.join path root (str id ".json")) :source (.join path root (str id ".md"))
     :manifest (.join path resources (str "cms-" org "-" id ".edn"))
     :garden (.join path resources (str "cms-garden-" org ".edn")) :resource-root resources}))
(defn ^:async read-record! [org id]
  (try (js->clj (.parse js/JSON (await (.readFile fs (:metadata (paths org id)) "utf8"))) :keywordize-keys true)
       (catch :default e (if (= "ENOENT" (.-code e)) nil (throw e)))))
(defn ^:async list-records! [org]
  (let [root (:root (paths org "index"))]
    (try
      (let [names (await (.readdir fs root))]
        (vec (array-seq (await (js/Promise.all (.map (.filter names #(.endsWith % ".json"))
                                               #(read-record! org (.slice % 0 -5))))))))
      (catch :default e (if (= "ENOENT" (.-code e)) [] (throw e))))))
(defn ^:async atomic-write! [target text]
  (let [temporary (str target "." (random-uuid) ".tmp")]
    (await (.writeFile fs temporary text #js {:mode 384}))
    (await (.rename fs temporary target))))
(defn ^:async save! [org record manifest garden]
  (let [p (paths org (:doc_id record))]
    (await (.mkdir fs (:root p) #js {:recursive true :mode 448}))
    (await (.mkdir fs (:resource-root p) #js {:recursive true :mode 448}))
    (when garden
      (try (await (.writeFile fs (:garden p) garden #js {:flag "wx" :mode 384}))
           (catch :default e (when-not (= "EEXIST" (.-code e)) (throw e)))))
    (await (atomic-write! (:source p) (:content record)))
    (when manifest (await (atomic-write! (:manifest p) manifest)))
    (await (atomic-write! (:metadata p) (.stringify js/JSON (clj->js record))))
    record))
