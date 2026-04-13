(ns knoxx.backend.document-routes
  (:require [clojure.string :as str]
            [knoxx.backend.authz :as authz]
            [knoxx.backend.document-state :refer [database-state* js-array-seq request-session-id database-docs-dir database-owner-key default-database-id default-database-record ensure-database-state! ensure-dir! profile-can-access? effective-active-database-id active-database-profile normalize-relative-path sanitize-upload-name create-db-id list-documents! active-record start-document-ingestion! priority-ingest-workspace-files!]]
            [knoxx.backend.runtime-config :as rc]))

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
                      node-path (aget runtime "path")]
                  (-> (ensure-dir! runtime docs-path)
                      (.then (fn []
                               (.fromAsync js/Array (.parts request))))
                      (.then (fn [parts]
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
                                                             (-> (.arrayBuffer (js/Response. (aget part "file")))
                                                                 (.then (fn [buf]
                                                                          (.writeFile node-fs abs-path (.from js/Buffer buf))))
                                                                 (.then (fn [] rel-path)))))
                                                         file-parts)]
                                 (-> (js/Promise.all (clj->js write-promises))
                                     (.then (fn [written]
                                              (let [files (vec (js-array-seq written))]
                                                (when auto-ingest?
                                                  (start-document-ingestion! runtime config profile {:full false
                                                                                                     :selected-files files}))
                                                (json-response! reply 200 {:ok true
                                                                           :uploaded files
                                                                           :autoIngest auto-ingest?})))))))
                      (.catch (fn [err]
                                (json-response! reply 500 {:detail (str "Upload failed: " err)})))))))))

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

  (route! app "POST" "/api/documents/ingest/priority"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "datalake.ingest"))
                (let [body (js->clj (or (aget request "body") #js {}) :keywordize-keys true)
                      paths (vec (or (:paths body) (:files body) []))
                      project (:project body)]
                  (if (empty? paths)
                    (json-response! reply 400 {:detail "paths (array of workspace-relative file paths) is required"})
                    (-> (priority-ingest-workspace-files! runtime config {:paths paths :project project})
                        (.then (fn [resp]
                                 (json-response! reply 200 resp)))
                        (.catch (fn [err]
                                  (json-response! reply 500 {:detail (str "Priority ingestion failed: " err)}))))))))))

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
  nil))
