(ns knoxx.backend.openplanner-memory
  (:require [clojure.string :as str]
            [knoxx.backend.http :as backend-http]
            [knoxx.backend.runtime-config :as runtime-config]))

(defn js-array-seq
  [arr]
  (when (some? arr)
    (for [i (range (.-length arr))]
      (aget arr i))))

;; ---------------------------------------------------------------------------
;; Document Ingestion
;; Sends documents to OpenPlanner's /v1/documents endpoint for embedding
;; and vector storage. This replaces direct Qdrant calls.
;; ---------------------------------------------------------------------------

(defn guess-document-kind
  "Infer document kind from file extension for OpenPlanner indexing."
  [rel-path]
  (let [ext (some-> (str/lower-case rel-path)
                    (str/split #"\.")
                    last)]
    (cond
      (contains? #{"ts" "tsx" "js" "jsx" "mjs" "cjs" "py" "clj" "cljs" "cljc" "rs" "go" "java" "rb" "php"} ext) "code"
      (contains? #{"md" "mdx" "txt" "rst" "adoc"} ext) "docs"
      (contains? #{"json" "yaml" "yml" "toml" "ini" "env" "conf"} ext) "config"
      (contains? #{"csv" "tsv" "sql" "xml"} ext) "data"
      :else "docs")))

(defn upsert-openplanner-document!
  "Send a document to OpenPlanner's /v1/documents endpoint for indexing.
   Returns {:ok true, :document ...} on success, or {:ok false ...} on failure."
  [config {:keys [id rel-path content project kind title source-path domain visibility extra]}]
  (when-not (backend-http/openplanner-enabled? config)
    (throw (js/Error. "OpenPlanner is not configured")))
  (let [doc-id (or id (str "knoxx-doc:" (or rel-path (.randomUUID js/crypto))))
        doc-kind (or kind (guess-document-kind rel-path))
        doc-title (or title (some-> rel-path (str/split #"/") last) doc-id)
        doc-content (str (or content ""))
        doc-project (or project (:project-name config) "devel")
        payload {:document {:id doc-id
                            :title doc-title
                            :content doc-content
                            :project doc-project
                            :kind doc-kind
                            :visibility (or visibility "internal")
                            :source "knoxx-ingestion"
                            :sourcePath (or source-path rel-path)
                            :domain (or domain "general")
                            :language "en"
                            :createdBy "knoxx-ingestion"
                            :metadata (merge {:indexed_from "knoxx"}
                                             extra)}}]
    (-> (backend-http/openplanner-request! config "POST" "/v1/documents" payload)
        (.then (fn [resp]
                 {:ok true
                  :document (:document resp)
                  :indexed (:indexed resp)
                  :rel-path rel-path}))
        (.catch (fn [err]
                  (.warn js/console "[knoxx] failed to index document into OpenPlanner:" rel-path err)
                  {:ok false
                   :error (str err)
                   :rel-path rel-path})))))

(defn batch-upsert-openplanner-documents!
  "Ingest multiple documents into OpenPlanner with concurrency control.
   Returns {:ok true, :indexed [...], :failed [...]} summary."
  [config documents {:keys [concurrency project visibility extra]
                     :or {concurrency 3
                          visibility "internal"}}]
  (cond
    (empty? documents)
    (js/Promise.resolve {:ok true
                         :indexed []
                         :failed []
                         :total 0
                         :indexed-count 0
                         :failed-count 0})

    (not (backend-http/openplanner-enabled? config))
    (js/Promise.resolve {:ok false
                         :indexed []
                         :failed []
                         :total (count documents)
                         :indexed-count 0
                         :failed-count (count documents)
                         :error "OpenPlanner is not configured"})

    :else
    (let [chunks (vec (partition-all concurrency documents))
          results (atom {:indexed []
                         :failed []})
          process-chunk! (fn [chunk]
                           (-> (.all js/Promise
                                     (clj->js
                                      (map (fn [doc]
                                             (upsert-openplanner-document! config
                                                                           (merge {:project project
                                                                                   :visibility visibility
                                                                                   :extra extra}
                                                                                  doc)))
                                           chunk)))
                               (.then (fn [chunk-results]
                                        (doseq [result (js-array-seq chunk-results)]
                                          (if (:ok result)
                                            (swap! results update :indexed conj result)
                                            (swap! results update :failed conj result)))
                                        nil))))]
      (-> (reduce (fn [promise chunk]
                    (.then promise (fn [] (process-chunk! chunk))))
                  (js/Promise.resolve nil)
                  chunks)
          (.then (fn []
                   {:ok true
                    :indexed (:indexed @results)
                    :failed (:failed @results)
                    :total (count documents)
                    :indexed-count (count (:indexed @results))
                    :failed-count (count (:failed @results))}))))))

(defn planner-row-timestamp-ms
  [row]
  (let [ts (:ts row)]
    (cond
      (number? ts) ts
      (string? ts) (let [parsed (js/Date.parse ts)]
                     (if (js/isNaN parsed) (.now js/Date) parsed))
      :else (.now js/Date))))

(defn planner-row->agent-message
  [row]
  (let [role (some-> (:role row) str)
        text (some-> (:text row) str)]
    (when (and (contains? #{"user" "assistant" "system"} role)
               (not (str/blank? text)))
      #js {:role role
           :content #js [#js {:type "text" :text text}]
           :timestamp (planner-row-timestamp-ms row)})))

(defn rehydrate-session-manager!
  [config session-manager conversation-id _model-id]
  (if (or (str/blank? conversation-id)
          (not (backend-http/openplanner-enabled? config)))
    (js/Promise.resolve session-manager)
    (-> (backend-http/openplanner-request! config "GET" (str "/v1/sessions/" conversation-id))
        (.then (fn [body]
                 (doseq [row (or (:rows body) [])]
                   (when-let [message (planner-row->agent-message row)]
                     (.appendMessage session-manager message)))
                 session-manager))
        (.catch (fn [err]
                  (.warn js/console "[knoxx] failed to rehydrate session from OpenPlanner" err)
                  session-manager)))))

(defn first-result-array
  [value]
  (let [items (or value [])
        first-item (first items)]
    (if (sequential? first-item)
      (vec first-item)
      [])))

(defn vector-result-hits
  [result]
  (let [ids (first-result-array (:ids result))
        docs (first-result-array (:documents result))
        metas (first-result-array (:metadatas result))
        distances (first-result-array (:distances result))]
    (mapv (fn [idx id]
            {:id id
             :document (nth docs idx "")
             :metadata (nth metas idx {})
             :distance (nth distances idx nil)})
          (range (count ids))
          ids)))

(defn openplanner-recent-session-summaries!
  [config]
  (-> (backend-http/openplanner-request! config "GET" (str "/v1/sessions?project=" (js/encodeURIComponent (:session-project-name config))))
      (.then (fn [body]
               (let [session-ids (->> (or (:rows body) [])
                                      (map :session)
                                      (remove str/blank?)
                                      distinct
                                      (take 4)
                                      vec)]
                 (if (seq session-ids)
                   (-> (.all js/Promise
                             (clj->js
                              (map (fn [session-id]
                                     (-> (backend-http/openplanner-request! config "GET" (str "/v1/sessions/" session-id))
                                         (.then (fn [session-body]
                                                  (let [rows (or (:rows session-body) [])
                                                        row (or (last (filter #(contains? #{"assistant" "system"} (:role %)) rows))
                                                                (last rows))]
                                                    (when row
                                                      {:session session-id
                                                       :role (:role row)
                                                       :text (:text row)}))))
                                         (.catch (fn [_] nil))))
                                   session-ids)))
                       (.then (fn [results]
                                (->> (js->clj results :keywordize-keys true)
                                     (remove nil?)
                                     vec))))
                   (js/Promise.resolve [])))))
      (.catch (fn [_] (js/Promise.resolve [])))))

(defn openplanner-memory-search!
  [config {:keys [query k session-id]}]
  (let [query (str/trim (or query ""))
        k (max 1 (min 8 (or k 5)))]
    (if (str/blank? query)
      (js/Promise.resolve {:query "" :hits [] :mode :none})
      (-> (backend-http/openplanner-request! config "POST" "/v1/search/vector"
                                             (cond-> {:q query
                                                      :k k
                                                      :source "knoxx"
                                                      :project (:session-project-name config)}
                                               (not (str/blank? session-id)) (assoc :session session-id)))
          (.then (fn [body]
                   {:query query
                    :mode :vector
                    :hits (vector-result-hits (:result body))}))
          (.catch (fn [_]
                    (-> (backend-http/openplanner-request! config "POST" "/v1/search/fts"
                                                           (cond-> {:q query
                                                                    :limit k
                                                                    :source "knoxx"
                                                                    :project (:session-project-name config)}
                                                             (not (str/blank? session-id)) (assoc :session session-id)))
                        (.then (fn [body]
                                 (let [hits (vec (or (:rows body) []))]
                                   (if (seq hits)
                                     {:query query
                                      :mode :fts
                                      :hits hits}
                                     (-> (openplanner-recent-session-summaries! config)
                                         (.then (fn [recent]
                                                  {:query query
                                                   :mode :recent
                                                   :hits recent}))))))))))))))

(defn openplanner-graph-query!
  [config {:keys [query lake node-type limit edge-limit]}]
  (let [params (js/URLSearchParams.)]
    (when-not (str/blank? (or query ""))
      (.set params "q" query))
    (when-not (str/blank? (or lake ""))
      (.set params "projects" lake))
    (when-not (str/blank? (or node-type ""))
      (.set params "nodeTypes" node-type))
    (.set params "limit" (str (max 1 (min 20 (or limit 8)))))
    (.set params "edgeLimit" (str (max 0 (min 60 (or edge-limit 16)))))
    (backend-http/openplanner-request! config "GET" (str "/v1/graph/query?" (.toString params)))))

(defn openplanner-graph-export!
  [config request]
  (backend-http/openplanner-request! config "GET" (str "/v1/graph/export" (backend-http/request-query-string request))))

(defn openplanner-event
  [config {:keys [id ts kind project session message role model text extra]}]
  {:schema "openplanner.event.v1"
   :id id
   :ts (or ts (runtime-config/now-iso))
   :source "knoxx"
   :kind kind
   :source_ref {:project (or project (:project-name config))
                :session session
                :message message}
   :text text
   :meta {:role role
          :author (if (= role "user") "user" "knoxx")
          :model model
          :tags ["knoxx" kind role]}
   :extra extra})

(defn tool-receipt-summary-text
  [receipt]
  (str "Tool: " (or (:tool_name receipt) (:id receipt) "tool")
       "\nStatus: " (or (:status receipt) "unknown")
       (when-let [input (:input_preview receipt)]
         (str "\nInput:\n" input))
       (when-let [result (:result_preview receipt)]
         (str "\nOutput:\n" result))))

(defn run-summary-text
  [run]
  (str "Run " (:run_id run)
       "\nMode: " (get-in run [:settings :mode])
       "\nModel: " (:model run)
       "\nStatus: " (:status run)
       (when-let [answer (:answer run)]
         (str "\nAnswer:\n" answer))
       (when-let [error (:error run)]
         (str "\nError:\n" error))))

(defn run-scope-extra
  [run]
  (select-keys run [:org_id :org_slug :user_id :user_email :membership_id]))

(defn session-node-kind
  [node-type]
  (case node-type
    "tool_call" "tool_call"
    "tool_result" "tool_result"
    "reasoning" "reasoning"
    "message"))

(defn session-graph-node-event
  [config {:keys [event-id node-id ts session message node-type text label model extra]}]
  (openplanner-event config {:id event-id
                             :ts ts
                             :kind "graph.node"
                             :project (:session-project-name config)
                             :session session
                             :message message
                             :role "system"
                             :model model
                             :text text
                             :extra (merge {:lake (:session-project-name config)
                                            :node_id node-id
                                            :node_type node-type
                                            :node_kind (session-node-kind node-type)
                                            :label label
                                            :entity_key node-id}
                                           extra)}))

(defn session-graph-edge-event
  [config {:keys [event-id ts session message edge-type source-node-id target-node-id source-lake target-lake extra]}]
  (openplanner-event config {:id event-id
                             :ts ts
                             :kind "graph.edge"
                             :project (:session-project-name config)
                             :session session
                             :message message
                             :role "system"
                             :text (str source-node-id " -> " target-node-id)
                             :extra (merge {:lake (:session-project-name config)
                                            :edge_id event-id
                                            :edge_type edge-type
                                            :source_node_id source-node-id
                                            :target_node_id target-node-id
                                            :source_lake source-lake
                                            :target_lake target-lake}
                                           extra)}))

(defn session-text-graph-events
  [config extract-mentioned-devel-paths extract-mentioned-urls {:keys [run-id conversation-id session-id ts node-id node-type text label model scope-extra]}]
  (let [safe-text (or text "")
        node-event (session-graph-node-event config {:event-id (str node-id ":node")
                                                     :node-id node-id
                                                     :ts ts
                                                     :session conversation-id
                                                     :message node-id
                                                     :node-type node-type
                                                     :text safe-text
                                                     :label label
                                                     :model model
                                                     :extra (merge {:run_id run-id
                                                                    :conversation_id conversation-id
                                                                    :session_id session-id}
                                                                   scope-extra)})
        devel-edges (for [path (extract-mentioned-devel-paths safe-text)]
                      (session-graph-edge-event config {:event-id (str node-id ":mentions_devel:" path)
                                                        :ts ts
                                                        :session conversation-id
                                                        :message node-id
                                                        :edge-type "mentions_devel_path"
                                                        :source-node-id node-id
                                                        :target-node-id (str "devel:file:" path)
                                                        :source-lake (:session-project-name config)
                                                        :target-lake "devel"
                                                        :extra (merge {:run_id run-id
                                                                       :conversation_id conversation-id
                                                                       :session_id session-id
                                                                       :path path}
                                                                      scope-extra)}))
        web-edges (for [url (extract-mentioned-urls safe-text)]
                    (session-graph-edge-event config {:event-id (str node-id ":mentions_web:" url)
                                                      :ts ts
                                                      :session conversation-id
                                                      :message node-id
                                                      :edge-type "mentions_web_url"
                                                      :source-node-id node-id
                                                      :target-node-id (str "web:url:" url)
                                                      :source-lake (:session-project-name config)
                                                      :target-lake "web"
                                                      :extra (merge {:run_id run-id
                                                                     :conversation_id conversation-id
                                                                     :session_id session-id
                                                                     :url url}
                                                                    scope-extra)}))]
    (into [node-event] (concat devel-edges web-edges))))

(defn index-run-memory!
  [config run extract-mentioned-devel-paths extract-mentioned-urls]
  (if-not (backend-http/openplanner-enabled? config)
    (js/Promise.resolve nil)
    (let [conversation-id (:conversation_id run)
          session-id (:session_id run)
          scope-extra (run-scope-extra run)
          session-project (:session-project-name config)
          request-text (or (some-> (:request_messages run) first :content) "")
          answer (:answer run)
          reasoning-text (or (:reasoning run) "")
          trace-blocks (vec (or (:trace_blocks run) []))
          run-id (:run_id run)
          common-extra (merge {:run_id run-id
                               :conversation_id conversation-id
                               :session_id session-id
                               :mode (get-in run [:settings :mode])}
                              scope-extra)
          base-events (cond-> [(openplanner-event config {:id (str run-id ":user")
                                                          :ts (:created_at run)
                                                          :kind "knoxx.message"
                                                          :project session-project
                                                          :session conversation-id
                                                          :message (str run-id ":user")
                                                          :role "user"
                                                          :model (:model run)
                                                          :text request-text
                                                          :extra common-extra})
                               (openplanner-event config {:id (str run-id ":summary")
                                                          :ts (:updated_at run)
                                                          :kind "knoxx.run"
                                                          :project session-project
                                                          :session conversation-id
                                                          :message (str run-id ":summary")
                                                          :role "system"
                                                          :model (:model run)
                                                          :text (run-summary-text run)
                                                          :extra (merge common-extra
                                                                        {:status (:status run)
                                                                         :ttft_ms (:ttft_ms run)
                                                                         :total_time_ms (:total_time_ms run)})})]
                        (not (str/blank? (or answer "")))
                        (conj (openplanner-event config {:id (str run-id ":assistant")
                                                         :ts (:updated_at run)
                                                         :kind "knoxx.message"
                                                         :project session-project
                                                         :session conversation-id
                                                         :message (str run-id ":assistant")
                                                         :role "assistant"
                                                         :model (:model run)
                                                         :text answer
                                                         :extra (merge common-extra
                                                                       {:status (:status run)
                                                                        :trace_blocks trace-blocks})}))
                        (not (str/blank? reasoning-text))
                        (conj (openplanner-event config {:id (str run-id ":reasoning")
                                                         :ts (:updated_at run)
                                                         :kind "knoxx.reasoning"
                                                         :project session-project
                                                         :session conversation-id
                                                         :message (str run-id ":reasoning")
                                                         :role "system"
                                                         :model (:model run)
                                                         :text reasoning-text
                                                         :extra (merge common-extra {:status (:status run)})})))
          tool-events (mapcat (fn [receipt]
                                (let [tool-id (or (:id receipt) "tool")
                                      tool-ts (or (:ended_at receipt) (:started_at receipt) (:updated_at run))
                                      summary-text (tool-receipt-summary-text receipt)
                                      call-text (or (:input_preview receipt) summary-text)
                                      result-text (or (:result_preview receipt) summary-text)]
                                  (cond-> [(openplanner-event config {:id (str run-id ":tool:" tool-id)
                                                                      :ts tool-ts
                                                                      :kind "knoxx.tool_receipt"
                                                                      :project session-project
                                                                      :session conversation-id
                                                                      :message (str run-id ":tool:" tool-id)
                                                                      :role "system"
                                                                      :model (:model run)
                                                                      :text summary-text
                                                                      :extra (merge common-extra {:receipt receipt})})]
                                    true (into (session-text-graph-events config extract-mentioned-devel-paths extract-mentioned-urls
                                                                          {:run-id run-id
                                                                           :conversation-id conversation-id
                                                                           :session-id session-id
                                                                           :ts tool-ts
                                                                           :node-id (str session-project ":run:" run-id ":tool-call:" tool-id)
                                                                           :node-type "tool_call"
                                                                           :text call-text
                                                                           :label (str "Tool call · " (or (:tool_name receipt) tool-id))
                                                                           :model (:model run)
                                                                           :scope-extra scope-extra}))
                                    true (into (session-text-graph-events config extract-mentioned-devel-paths extract-mentioned-urls
                                                                          {:run-id run-id
                                                                           :conversation-id conversation-id
                                                                           :session-id session-id
                                                                           :ts tool-ts
                                                                           :node-id (str session-project ":run:" run-id ":tool-result:" tool-id)
                                                                           :node-type "tool_result"
                                                                           :text result-text
                                                                           :label (str "Tool result · " (or (:tool_name receipt) tool-id))
                                                                           :model (:model run)
                                                                           :scope-extra scope-extra})))))
                              (:tool_receipts run))
          graph-events (concat
                        (session-text-graph-events config extract-mentioned-devel-paths extract-mentioned-urls
                                                   {:run-id run-id
                                                    :conversation-id conversation-id
                                                    :session-id session-id
                                                    :ts (:created_at run)
                                                    :node-id (str session-project ":run:" run-id ":user")
                                                    :node-type "user_message"
                                                    :text request-text
                                                    :label "User message"
                                                    :model (:model run)
                                                    :scope-extra scope-extra})
                        (when-not (str/blank? (or answer ""))
                          (session-text-graph-events config extract-mentioned-devel-paths extract-mentioned-urls
                                                     {:run-id run-id
                                                      :conversation-id conversation-id
                                                      :session-id session-id
                                                      :ts (:updated_at run)
                                                      :node-id (str session-project ":run:" run-id ":assistant")
                                                      :node-type "assistant_message"
                                                      :text answer
                                                      :label "Assistant message"
                                                      :model (:model run)
                                                      :scope-extra scope-extra}))
                        (when-not (str/blank? reasoning-text)
                          (session-text-graph-events config extract-mentioned-devel-paths extract-mentioned-urls
                                                     {:run-id run-id
                                                      :conversation-id conversation-id
                                                      :session-id session-id
                                                      :ts (:updated_at run)
                                                      :node-id (str session-project ":run:" run-id ":reasoning")
                                                      :node-type "reasoning"
                                                      :text reasoning-text
                                                      :label "Reasoning"
                                                      :model (:model run)
                                                      :scope-extra scope-extra})))
          all-events (vec (concat base-events graph-events tool-events))]
      (-> (backend-http/openplanner-request! config "POST" "/v1/events" {:events all-events})
          (.catch (fn [err]
                    (.warn js/console "[knoxx] failed to index run memory into OpenPlanner" err)
                    nil))))))
