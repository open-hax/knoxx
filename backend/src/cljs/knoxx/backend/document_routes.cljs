(ns knoxx.backend.document-routes
  (:require [clojure.string :as str]
            [knoxx.backend.runtime-config :as rc]
            [knoxx.backend.http :as http]
            [knoxx.backend.text :as text]
            [knoxx.backend.authz :as authz]))

;; Database state atom
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
  [runtime config profile {:keys [full selected-files]}]
  (let [node-fs (aget runtime "fs")
        node-path (aget runtime "path")
        node-crypto (aget runtime "crypto")
        db-id (:id profile)
        docs-path (:docsPath profile)]
    (-> (list-files-recursive! runtime docs-path)
        (.then (fn [all-abs]
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
                   (.then (js/Promise.all
                           (clj->js
                            (map (fn [{:keys [abs rel]}]
                                   (-> (.readFile node-fs abs "utf8")
                                       (.then (fn [content]
                                                {:rel rel
                                                 :error false
                                                 :chunkCount (file-chunk-count content)}))
                                       (.catch (fn [err]
                                                 {:rel rel
                                                  :error true
                                                  :detail (str err)
                                                  :chunkCount 0}))))
                                 queue)))
                          (fn [results]
                            (let [items (vec (js-array-seq results))
                                  files-updated (count (remove :error items))
                                  errors (count (filter :error items))
                                  total-indexed (reduce + 0 (map :chunkCount items))
                                  started-ms (.getTime (js/Date. started-at))
                                  duration-seconds (max 0 (js/Math.round (/ (- (.now js/Date) started-ms) 1000)))
                                  history-item {:id (.randomUUID node-crypto)
                                                :completedAt (rc/now-iso)
                                                :mode mode
                                                :chunksUpserted total-indexed
                                                :processedChunks total
                                                :filesUpdated files-updated
                                                :durationSeconds duration-seconds
                                                :errors errors}]
                              (swap! database-state*
                                     (fn [state]
                                       (let [state-with-index (reduce (fn [acc {:keys [rel error chunkCount]}]
                                                                        (if error
                                                                          acc
                                                                          (assoc-in acc [:records db-id :indexed rel]
                                                                                    {:chunkCount chunkCount
                                                                                     :indexedAt (rc/now-iso)})))
                                                                      state
                                                                      items)]
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
                                                        :filesUpdated files-updated
                                                        :errors errors
                                                        :stale false})
                                             (update-in [:records db-id :history]
                                                        (fn [history]
                                                          (->> (conj (vec history) history-item)
                                                               (take-last 50)
                                                               vec)))))))
                              {:ok true
                               :started true
                               :mode mode
                               :selectedFiles (vec (map :rel queue))})))))))))

;; Route registration

(defn register-document-routes!
  [app runtime config {:keys [route! json-response! error-response!
                              with-request-context! ensure-permission!
                              clip-text openplanner-graph-export!
                              send-fetch-response! bearer-headers
                              fetch-json openai-auth-error request-query-string]}]
  (route! app "GET" "/api/documents"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "datalake.read"))
                (-> (list-documents! runtime config request ctx)
                    (.then (fn [resp]
                             (json-response! reply 200 resp)))
                    (.catch (fn [err]
                              (json-response! reply 500 {:detail (str "Failed to list documents: " err)}))))))))

  (route! app "GET" "/api/documents/content/*"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "datalake.read"))
                (let [profile (active-database-profile runtime config request ctx)
                      node-fs (aget runtime "fs")
                      node-path (aget runtime "path")
                      rel-path (normalize-relative-path (aget request "params" "*"))
                      abs-path (.resolve node-path (:docsPath profile) rel-path)
                      rel-to-root (.relative node-path (:docsPath profile) abs-path)]
                  (if (or (str/starts-with? rel-to-root "..") (.isAbsolute node-path rel-to-root))
                    (json-response! reply 403 {:detail "Path escapes active docs root"})
                    (-> (.readFile node-fs abs-path "utf8")
                        (.then (fn [content]
                                 (json-response! reply 200 {:path rel-path
                                                            :content content})))
                        (.catch (fn [err]
                                  (json-response! reply 404 {:detail (str "Failed to read document: " err)}))))))))))

  (route! app "POST" "/api/documents/upload"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "datalake.write"))
                (let [profile (active-database-profile runtime config request ctx)
                      docs-path (:docsPath profile)
                      node-fs (aget runtime "fs")
                      node-path (aget runtime "path")
                      promise (.then
                               (.then (ensure-dir! runtime docs-path)
                                      (fn [] (.fromAsync js/Array (.parts request))))
                               (fn [parts]
                                 (let [part-seq (js-array-seq parts)
                                       auto-ingest? (boolean (some (fn [part]
                                                                     (and (= (aget part "type") "field")
                                                                          (= (aget part "fieldname") "autoIngest")
                                                                          (= (str/lower-case (str (aget part "value"))) "true")))
                                                                   part-seq))
                                       file-parts (filter #(= (aget % "type") "file") part-seq)
                                       write-promises (mapv (fn [part]
                                                             (let [safe-name (sanitize-upload-name (or (aget part "filename") "upload.bin"))
                                                                   abs-path (.join node-path docs-path safe-name)
                                                                   rel-path (normalize-relative-path (.relative node-path docs-path abs-path))]
                                                               (.then (.arrayBuffer (js/Response. (aget part "file")))
                                                                      (fn [buf]
                                                                        (.then (.writeFile node-fs abs-path (.from js/Buffer buf))
                                                                               (fn [] rel-path))))))
                                                           file-parts)]
                                   (.then (js/Promise.all (clj->js write-promises))
                                          (fn [written]
                                            (let [files (vec (js-array-seq written))]
                                              (if auto-ingest?
                                                (do
                                                  (start-document-ingestion! runtime config profile {:full false
                                                                                                     :selected-files files})
                                                  (json-response! reply 200 {:ok true
                                                                             :uploaded files
                                                                             :autoIngest true}))
                                                (json-response! reply 200 {:ok true
                                                                           :uploaded files
                                                                           :autoIngest false}))))))))]
                  (.catch promise
                          (fn [err]
                            (json-response! reply 500 {:detail (str "Upload failed: " err)}))))))))

  (route! app "DELETE" "/api/documents/*"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "datalake.write"))
                (let [profile (active-database-profile runtime config request ctx)
                      node-fs (aget runtime "fs")
                      node-path (aget runtime "path")
                      rel-path (normalize-relative-path (aget request "params" "*"))
                      abs-path (.resolve node-path (:docsPath profile) rel-path)
                      rel-to-root (.relative node-path (:docsPath profile) abs-path)
                      db-id (:id profile)]
                  (if (or (str/starts-with? rel-to-root "..") (.isAbsolute node-path rel-to-root))
                    (json-response! reply 403 {:detail "Path escapes active docs root"})
                    (-> (.rm node-fs abs-path #js {:force true})
                        (.then (fn []
                                 (swap! database-state* update-in [:records db-id :indexed] dissoc rel-path)
                                 (json-response! reply 200 {:ok true
                                                            :path rel-path})))
                        (.catch (fn [err]
                                  (json-response! reply 500 {:detail (str "Delete failed: " err)}))))))))))

  (route! app "POST" "/api/documents/ingest"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "datalake.ingest"))
                (let [profile (active-database-profile runtime config request ctx)
                      body (js->clj (or (aget request "body") #js {}) :keywordize-keys true)]
                  (-> (start-document-ingestion! runtime config profile body)
                      (.then (fn [resp]
                               (json-response! reply 200 resp)))
                      (.catch (fn [err]
                                (json-response! reply 500 {:detail (str "Ingestion failed to start: " err)})))))))))

  (route! app "POST" "/api/documents/ingest/restart"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "datalake.ingest"))
                (let [profile (active-database-profile runtime config request ctx)
                      db-id (:id profile)
                      last-request (get-in (ensure-database-state! runtime config ctx) [:records db-id :lastRequest])]
                  (if (nil? last-request)
                    (json-response! reply 400 {:detail "No active ingestion to restart"
                                               :resumed false})
                    (-> (start-document-ingestion! runtime config profile {:full (:full last-request)
                                                                           :selected-files (:selectedFiles last-request)})
                        (.then (fn [resp]
                                 (json-response! reply 200 (assoc resp :resumed true))))
                        (.catch (fn [err]
                                  (json-response! reply 500 {:detail (str "Restart failed: " err)
                                                             :resumed false}))))))))))

  (route! app "GET" "/api/documents/ingestion-status"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "datalake.read"))
                (let [record (active-record runtime config request ctx)
                      progress (:progress record)]
                  (json-response! reply 200 {:active (boolean (:active progress))
                                             :progress progress
                                             :canResumeForum false
                                             :stale false}))))))

  (route! app "GET" "/api/documents/ingestion-progress"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "datalake.read"))
                (let [record (active-record runtime config request ctx)
                      progress (:progress record)]
                  (json-response! reply 200 {:active (boolean (:active progress))
                                             :progress progress
                                             :canResumeForum false
                                             :stale false}))))))

  (route! app "GET" "/api/documents/ingestion-history"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "datalake.read"))
                (let [profile (active-database-profile runtime config request ctx)
                      record (active-record runtime config request ctx)]
                  (json-response! reply 200 {:collection (:qdrantCollection profile)
                                             :items (:history record)}))))))

  (route! app "POST" "/api/chat/retrieval-debug"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "datalake.query"))
                (let [body (js->clj (or (aget request "body") #js {}) :keywordize-keys true)
                      query (str/trim (str (:message body)))
                      top-k (or (:topK body) 5)]
                  (if (str/blank? query)
                    (json-response! reply 400 {:detail "message is required"})
                    (-> (list-documents! runtime config request ctx)
                        (.then (fn [resp]
                                 (let [documents (:documents resp)
                                       lowered (str/lower-case query)
                                       results (->> documents
                                                    (map (fn [doc]
                                                           (let [path (str (:relativePath doc))
                                                                 name (str (:name doc))
                                                                 hay (str/lower-case (str path " " name))
                                                                 score (cond
                                                                         (str/includes? hay lowered) 1
                                                                         (str/includes? lowered (str/lower-case name)) 0.5
                                                                         :else 0)]
                                                             (assoc doc :score score))))
                                                    (filter #(pos? (:score %)))
                                                    (sort-by :score >)
                                                    (take top-k)
                                                    vec)]
                                   (json-response! reply 200 {:query query
                                                              :topK top-k
                                                              :results results}))))
                        (.catch (fn [err]
                                  (json-response! reply 500 {:detail (str "Retrieval debug failed: " err)}))))))))))

  (route! app "GET" "/api/graph/export"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "datalake.query"))
                (-> (openplanner-graph-export! config request)
                    (.then (fn [resp]
                             (json-response! reply 200 resp)))
                    (.catch (fn [err]
                              (error-response! reply err 502))))))))

  (route! app "GET" "/api/settings/databases"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "org.datalakes.read"))
                (let [state (ensure-database-state! runtime config ctx)
                      session-id (request-session-id request)
                      active-id (effective-active-database-id runtime config request ctx)
                      active-profile (get-in state [:profiles active-id])
                      profiles (->> (:profiles state)
                                    vals
                                    (filter #(profile-can-access? % ctx session-id))
                                    (sort-by :createdAt)
                                    (mapv (fn [profile]
                                            (assoc profile :canAccess (profile-can-access? profile ctx session-id)))))]
                  (json-response! reply 200 {:activeDatabaseId active-id
                                             :databases profiles
                                             :activeRuntime {:projectName (:project-name config)
                                                             :docsPath (:docsPath active-profile)
                                                             :qdrantCollection (:qdrantCollection active-profile)}}))))))

  (route! app "POST" "/api/settings/databases"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "org.datalakes.create"))
                (let [body (js->clj (or (aget request "body") #js {}) :keywordize-keys true)
                      name (str/trim (str (:name body)))
                      session-id (request-session-id request)]
                  (if (str/blank? name)
                    (json-response! reply 400 {:detail "name is required"})
                    (let [db-id (create-db-id runtime name)
                          docs-path (or (:docsPath body) (database-docs-dir runtime config db-id))
                          profile {:id db-id
                                   :name name
                                   :orgId (authz/ctx-org-id ctx)
                                   :orgSlug (authz/ctx-org-slug ctx)
                                   :ownerUserId (authz/ctx-user-id ctx)
                                   :ownerMembershipId (authz/ctx-membership-id ctx)
                                   :docsPath docs-path
                                   :qdrantCollection (or (:qdrantCollection body) (str (:collection-name config) "_" db-id))
                                   :publicDocsBaseUrl (or (:publicDocsBaseUrl body) "")
                                   :useLocalDocsBaseUrl (not= false (:useLocalDocsBaseUrl body))
                                   :forumMode (boolean (:forumMode body))
                                   :privateToSession (boolean (:privateToSession body))
                                   :ownerSessionId (when (:privateToSession body) session-id)
                                   :createdAt (rc/now-iso)}]
                      (-> (ensure-dir! runtime docs-path)
                          (.then (fn []
                                   (swap! database-state*
                                          (fn [state]
                                            (let [owner-key (database-owner-key ctx)]
                                              (-> state
                                                  (assoc-in [:profiles db-id] profile)
                                                  (assoc-in [:records db-id] (default-database-record))
                                                  ((fn [s]
                                                     (if (:activate body)
                                                       (assoc-in s [:active-ids owner-key] db-id)
                                                       s)))))))
                                   (json-response! reply 200 profile)))
                          (.catch (fn [err]
                                    (json-response! reply 500 {:detail (str "Failed to create database profile: " err)})))))))))))

  (route! app "POST" "/api/settings/databases/activate"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "org.datalakes.read"))
                (let [body (js->clj (or (aget request "body") #js {}) :keywordize-keys true)
                      db-id (str (:id body))
                      session-id (request-session-id request)
                      profile (get-in (ensure-database-state! runtime config ctx) [:profiles db-id])]
                  (cond
                    (nil? profile) (json-response! reply 404 {:detail "Database profile not found"})
                    (not (profile-can-access? profile ctx session-id)) (json-response! reply 403 {:detail "Database profile is outside the current Knoxx scope"})
                    :else (do
                            (swap! database-state* assoc-in [:active-ids (database-owner-key ctx)] db-id)
                            (json-response! reply 200 {:ok true
                                                       :activeDatabaseId db-id}))))))))

  (route! app "PATCH" "/api/settings/databases/:id"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "org.datalakes.update"))
                (let [db-id (str (aget request "params" "id"))
                      body (js->clj (or (aget request "body") #js {}) :keywordize-keys true)
                      session-id (request-session-id request)
                      profile (get-in (ensure-database-state! runtime config ctx) [:profiles db-id])]
                  (cond
                    (nil? profile) (json-response! reply 404 {:detail "Database profile not found"})
                    (not (profile-can-access? profile ctx session-id)) (json-response! reply 403 {:detail "Database profile is outside the current Knoxx scope"})
                    :else (let [updated (merge profile
                                               (select-keys body [:name :publicDocsBaseUrl :useLocalDocsBaseUrl :forumMode]))]
                            (swap! database-state* assoc-in [:profiles db-id] updated)
                            (json-response! reply 200 updated))))))))

  (route! app "DELETE" "/api/settings/databases/:id"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "org.datalakes.delete"))
                (let [db-id (str (aget request "params" "id"))
                      session-id (request-session-id request)
                      profile (get-in (ensure-database-state! runtime config ctx) [:profiles db-id])]
                  (cond
                    (nil? profile) (json-response! reply 404 {:detail "Database profile not found"})
                    (= db-id (default-database-id ctx)) (json-response! reply 400 {:detail "Default database cannot be deleted"})
                    (not (profile-can-access? profile ctx session-id)) (json-response! reply 403 {:detail "Database profile is outside the current Knoxx scope"})
                    :else (do
                            (swap! database-state*
                                   (fn [state]
                                     (let [owner-key (database-owner-key ctx)]
                                       (-> state
                                           (update :profiles dissoc db-id)
                                           (update :records dissoc db-id)
                                           ((fn [s]
                                              (if (= (get-in s [:active-ids owner-key]) db-id)
                                                (assoc-in s [:active-ids owner-key] (default-database-id ctx))
                                                s)))))))
                            (json-response! reply 200 {:ok true
                                                       :deleted db-id}))))))))

  (route! app "POST" "/api/settings/databases/:id/make-private"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "org.datalakes.update"))
                (let [db-id (str (aget request "params" "id"))
                      session-id (request-session-id request)
                      profile (get-in (ensure-database-state! runtime config ctx) [:profiles db-id])]
                  (if (nil? profile)
                    (json-response! reply 404 {:detail "Database profile not found"})
                    (if-not (profile-can-access? profile ctx session-id)
                      (json-response! reply 403 {:detail "Database profile is outside the current Knoxx scope"})
                      (let [updated (assoc profile
                                           :privateToSession true
                                           :ownerSessionId session-id)]
                        (swap! database-state* assoc-in [:profiles db-id] updated)
                        (json-response! reply 200 updated)))))))))
  nil)
