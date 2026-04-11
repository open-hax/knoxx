(ns knoxx.backend.document-state
  (:require [clojure.string :as str]
            [knoxx.backend.authz :as authz]
            [knoxx.backend.runtime-config :as rc]
            [knoxx.backend.openplanner-memory :as op-memory]))

(defonce database-state* (atom nil))

;; Utility functions

(defn js-array-seq
  [arr]
  (when (some? arr)
    (for [i (range (.-length arr))]
      (aget arr i))))

;; Helper functions

(defn request-session-id
  [request]
  (str (or (aget request "headers" "x-knoxx-session-id") "")))

(defn database-root-dir
  [runtime config]
  (.resolve (aget runtime "path") (:workspace-root config) ".knoxx" "databases"))

(defn database-docs-dir
  [runtime config db-id]
  (.join (aget runtime "path") (database-root-dir runtime config) db-id "docs"))

(defn database-owner-key
  [auth-context]
  (or (some-> (authz/ctx-org-id auth-context) str not-empty) "__global__"))

(defn default-database-id
  [auth-context]
  (if-let [org-id (some-> (authz/ctx-org-id auth-context) str not-empty)]
    (str "default:" org-id)
    "default"))

(defn default-database-profile
  ([runtime config] (default-database-profile runtime config nil))
  ([runtime config auth-context]
   (let [db-id (default-database-id auth-context)]
     {:id db-id
      :name "Workspace Docs"
      :orgId (authz/ctx-org-id auth-context)
      :orgSlug (authz/ctx-org-slug auth-context)
      :docsPath (database-docs-dir runtime config db-id)
      :qdrantCollection (:collection-name config)
      :publicDocsBaseUrl ""
      :useLocalDocsBaseUrl true
      :forumMode false
      :privateToSession false
      :ownerSessionId nil
      :ownerUserId (authz/ctx-user-id auth-context)
      :ownerMembershipId (authz/ctx-membership-id auth-context)
      :createdAt (rc/now-iso)})))

(defn default-database-record
  []
  {:indexed {}
   :history []
   :progress nil
   :lastRequest nil})

(defn ensure-database-state!
  ([runtime config] (ensure-database-state! runtime config nil))
  ([runtime config auth-context]
   (when-not @database-state*
     (reset! database-state* {:active-id "default"
                              :active-ids {}
                              :profiles {}
                              :records {}}))
   (swap! database-state*
          (fn [state]
            (let [state (merge {:active-id "default"
                                :active-ids {}
                                :profiles {}
                                :records {}}
                               state)
                  global-default (default-database-profile runtime config nil)
                  state (-> state
                            (update :profiles #(if (contains? % (:id global-default)) % (assoc % (:id global-default) global-default)))
                            (update :records #(if (contains? % (:id global-default)) % (assoc % (:id global-default) (default-database-record)))))
                  owner-key (database-owner-key auth-context)
                  scoped-default (default-database-profile runtime config auth-context)]
              (cond-> state
                auth-context (-> (update :profiles #(if (contains? % (:id scoped-default)) % (assoc % (:id scoped-default) scoped-default)))
                                 (update :records #(if (contains? % (:id scoped-default)) % (assoc % (:id scoped-default) (default-database-record))))
                                 (update :active-ids #(if (contains? % owner-key) % (assoc % owner-key (:id scoped-default)))))
                (nil? auth-context) (assoc :active-id (or (:active-id state) (:id global-default)))))))
   @database-state*))

(defn ensure-dir!
  [runtime dir-path]
  (.mkdir (aget runtime "fs") dir-path #js {:recursive true}))

(defn profile-can-access?
  ([profile session-id] (profile-can-access? profile nil session-id))
  ([profile auth-context session-id]
   (let [org-id (some-> (:orgId profile) str not-empty)
         org-allowed? (if org-id
                        (or (nil? auth-context)
                            (authz/system-admin? auth-context)
                            (= org-id (str (authz/ctx-org-id auth-context))))
                        (or (nil? auth-context)
                            (authz/system-admin? auth-context)))
         session-allowed? (or (not (:privateToSession profile))
                              (str/blank? (str (:ownerSessionId profile)))
                              (= (str (:ownerSessionId profile)) (str session-id)))]
     (and org-allowed? session-allowed?))))

(defn effective-active-database-id
  ([runtime config request] (effective-active-database-id runtime config request nil))
  ([runtime config request auth-context]
   (let [state (ensure-database-state! runtime config auth-context)
         session-id (request-session-id request)
         owner-key (database-owner-key auth-context)
         default-id (default-database-id auth-context)
         active-id (if auth-context
                     (or (get-in state [:active-ids owner-key]) default-id)
                     (or (:active-id state) default-id))
         active-profile (get-in state [:profiles active-id])]
     (if (profile-can-access? active-profile auth-context session-id)
       active-id
       (or (some (fn [[db-id profile]]
                   (when (profile-can-access? profile auth-context session-id) db-id))
                 (:profiles state))
           default-id)))))

(defn active-database-profile
  ([runtime config request] (active-database-profile runtime config request nil))
  ([runtime config request auth-context]
   (let [state (ensure-database-state! runtime config auth-context)
         db-id (effective-active-database-id runtime config request auth-context)]
     (get-in state [:profiles db-id]))))

(defn normalize-relative-path
  [value]
  (-> (str value)
      (str/replace #"\\" "/")
      (str/replace #"^/+" "")))

(defn sanitize-upload-name
  [name]
  (let [trimmed (str/trim (str name))
        cleaned (-> trimmed
                    (str/replace #"[\\/]+" "-")
                    (str/replace #"[^A-Za-z0-9._ -]" "-")
                    (str/replace #"\s+" " "))]
    (if (str/blank? cleaned) "upload.bin" cleaned)))

(defn create-db-id
  [runtime name]
  (let [base (-> (str/lower-case (str name))
                 (str/replace #"[^a-z0-9]+" "-")
                 (str/replace #"^-+|-+$" ""))
        prefix (if (str/blank? base) "db" base)]
    (str prefix "-" (.slice (.randomUUID (aget runtime "crypto")) 0 8))))

(defn list-files-recursive!
  [runtime dir-path]
  (let [node-fs (aget runtime "fs")
        node-path (aget runtime "path")
        read-promise (.readdir node-fs dir-path #js {:withFileTypes true})]
    (-> read-promise
        (.then (fn [entries]
                 (.then (js/Promise.all
                         (clj->js
                          (for [entry (js-array-seq entries)]
                            (let [full-path (.join node-path dir-path (.-name entry))]
                              (if (.isDirectory entry)
                                (list-files-recursive! runtime full-path)
                                (js/Promise.resolve #js [full-path]))))))
                        (fn [nested]
                          (into [] (mapcat js-array-seq) (js-array-seq nested))))))
        (.catch (fn [err]
                  (if (= (aget err "code") "ENOENT")
                    (js/Promise.resolve [])
                    (js/Promise.reject err)))))))

(defn file-chunk-count
  [text]
  (max 1 (js/Math.ceil (/ (max 1 (count (str text))) 1800))))

(defn indexed-meta
  [runtime config db-id rel-path]
  (get-in (ensure-database-state! runtime config) [:records db-id :indexed rel-path]))

(defn document-entry!
  [runtime config profile db-id abs-path]
  (let [node-fs (aget runtime "fs")
        node-path (aget runtime "path")
        docs-path (:docsPath profile)]
    (-> (.stat node-fs abs-path)
        (.then (fn [stats]
                 (let [rel-path (normalize-relative-path (.relative node-path docs-path abs-path))
                       meta (indexed-meta runtime config db-id rel-path)]
                   {:name (.basename node-path abs-path)
                    :relativePath rel-path
                    :size (or (aget stats "size") 0)
                    :indexed (boolean meta)
                    :chunkCount (or (:chunkCount meta) 0)
                    :indexedAt (:indexedAt meta)}))))))

(defn list-documents!
  ([runtime config request] (list-documents! runtime config request nil))
  ([runtime config request auth-context]
   (let [profile (active-database-profile runtime config request auth-context)
         db-id (:id profile)]
     (-> (ensure-dir! runtime (:docsPath profile))
         (.then (fn [] (list-files-recursive! runtime (:docsPath profile))))
         (.then (fn [paths]
                  (-> (js/Promise.all
                       (clj->js (map #(document-entry! runtime config profile db-id %) paths)))
                      (.then (fn [items]
                               {:documents (->> (js-array-seq items)
                                                (sort-by :relativePath)
                                                vec)})))))))))

(defn active-record
  ([runtime config request] (active-record runtime config request nil))
  ([runtime config request auth-context]
   (let [db-id (effective-active-database-id runtime config request auth-context)]
     (get-in (ensure-database-state! runtime config auth-context) [:records db-id]))))

(defn active-agent-profile
  ([runtime config] (active-agent-profile runtime config nil))
  ([runtime config auth-context]
   (let [state (ensure-database-state! runtime config auth-context)
         owner-key (database-owner-key auth-context)
         active-id (if auth-context
                     (or (get-in state [:active-ids owner-key]) (default-database-id auth-context))
                     (or (:active-id state) "default"))]
     (or (get-in state [:profiles active-id])
         (get-in state [:profiles (default-database-id auth-context)])
         (get-in state [:profiles "default"])))))

(defn start-document-ingestion!
  "Ingest documents into OpenPlanner for embedding and vector storage.
   Replaces previous metadata-only tracking with OpenPlanner /v1/documents indexing."
  [runtime config profile {:keys [full selected-files]}]
  (let [node-fs (aget runtime "fs")
        node-path (aget runtime "path")
        node-crypto (aget runtime "crypto")
        db-id (:id profile)
        docs-path (:docsPath profile)
        project (or (:project-name config) "devel")]
    (-> (list-files-recursive! runtime docs-path)
        (.then
         (fn [all-abs]
           (let [wanted (when-not full
                          (into #{} (map normalize-relative-path) selected-files))
                 queue (->> all-abs
                            (map (fn [abs]
                                   (let [rel (normalize-relative-path (.relative node-path docs-path abs))]
                                     {:abs abs :rel rel})))
                            (filter (fn [{:keys [rel]}]
                                      (or full (contains? wanted rel))))
                            vec)
                 started-at (rc/now-iso)
                 total (count queue)
                 mode (if full "full" "selected")]
             (swap! database-state* assoc-in [:records db-id :progress]
                    {:active true
                     :startedAt started-at
                     :mode mode
                     :currentFile (some-> queue first :rel)
                     :processedChunks 0
                     :totalChunks total
                     :percent 0
                     :percentPrecise 0
                     :filesUpdated 0
                     :errors 0
                     :stale false})
             (swap! database-state* assoc-in [:records db-id :lastRequest]
                    {:full (boolean full)
                     :selectedFiles (vec (map :rel queue))})
             (if (zero? total)
               (do
                 (swap! database-state*
                        (fn [state]
                          (-> state
                              (assoc-in [:records db-id :progress]
                                        {:active false
                                         :startedAt started-at
                                         :mode mode
                                         :currentFile nil
                                         :processedChunks 0
                                         :totalChunks 0
                                         :percent 100
                                         :percentPrecise 100
                                         :filesUpdated 0
                                         :errors 0
                                         :stale false})
                              (update-in [:records db-id :history]
                                         (fn [history]
                                           (->> (conj (vec history)
                                                      {:id (.randomUUID node-crypto)
                                                       :completedAt (rc/now-iso)
                                                       :mode mode
                                                       :chunksUpserted 0
                                                       :processedChunks 0
                                                       :filesUpdated 0
                                                       :durationSeconds 0
                                                       :errors 0})
                                                (take-last 50)
                                                vec))))))
                 (js/Promise.resolve {:ok true
                                      :started true
                                      :mode mode
                                      :selectedFiles []
                                      :indexedCount 0
                                      :failedCount 0
                                      :openplanner true}))
               (-> (.all js/Promise
                         (clj->js
                          (map (fn [{:keys [abs rel]}]
                                 (-> (.readFile node-fs abs "utf8")
                                     (.then (fn [content]
                                              {:rel rel
                                               :content content
                                               :error false}))
                                     (.catch (fn [err]
                                               {:rel rel
                                                :content nil
                                                :error true
                                                :detail (str err)}))))
                               queue)))
                   (.then
                    (fn [read-results]
                      (let [items (vec (js-array-seq read-results))
                            read-failed (vec (filter :error items))
                            valid-items (vec (remove :error items))
                            documents (mapv (fn [item]
                                              {:id (str "knoxx:" db-id ":" (:rel item))
                                               :rel-path (:rel item)
                                               :content (:content item)
                                               :source-path (:rel item)
                                               :project project
                                               :extra {:database-id db-id
                                                       :org-id (:orgId profile)}})
                                            valid-items)]
                        (-> (op-memory/batch-upsert-openplanner-documents!
                             config
                             documents
                             {:concurrency 3
                              :project project
                              :visibility "internal"
                              :extra {:database-id db-id
                                      :org-id (:orgId profile)}})
                            (.then
                             (fn [index-result]
                               (let [successful-rels (set (keep :rel-path (:indexed index-result)))
                                     indexed-items (vec (filter (fn [item]
                                                                  (contains? successful-rels (:rel item)))
                                                                valid-items))
                                     indexed-count (count indexed-items)
                                     failed-count (+ (count read-failed)
                                                     (:failed-count index-result 0))
                                     chunk-count (reduce + 0 (map (comp file-chunk-count :content) indexed-items))
                                     started-ms (.getTime (js/Date. started-at))
                                     duration-seconds (max 0 (js/Math.round (/ (- (.now js/Date) started-ms) 1000)))
                                     history-item {:id (.randomUUID node-crypto)
                                                   :completedAt (rc/now-iso)
                                                   :mode mode
                                                   :chunksUpserted chunk-count
                                                   :processedChunks total
                                                   :filesUpdated indexed-count
                                                   :durationSeconds duration-seconds
                                                   :errors failed-count}]
                                 (swap! database-state*
                                        (fn [state]
                                          (let [state-with-index (reduce (fn [acc item]
                                                                           (assoc-in acc [:records db-id :indexed (:rel item)]
                                                                                     {:chunkCount (file-chunk-count (:content item))
                                                                                      :indexedAt (rc/now-iso)
                                                                                      :openplanner true}))
                                                                         state
                                                                         indexed-items)]
                                            (-> state-with-index
                                                (assoc-in [:records db-id :progress]
                                                          {:active false
                                                           :startedAt started-at
                                                           :mode mode
                                                           :currentFile nil
                                                           :processedChunks total
                                                           :totalChunks total
                                                           :percent 100
                                                           :percentPrecise 100
                                                           :filesUpdated indexed-count
                                                           :errors failed-count
                                                           :stale false})
                                                (update-in [:records db-id :history]
                                                           (fn [history]
                                                             (->> (conj (vec history) history-item)
                                                                  (take-last 50)
                                                                  vec)))))))
                                 {:ok true
                                  :started true
                                  :mode mode
                                  :selectedFiles (vec (map :rel queue))
                                  :indexedCount indexed-count
                                  :failedCount failed-count
                                  :openplanner true})))))))))))))))

;; Route registration
