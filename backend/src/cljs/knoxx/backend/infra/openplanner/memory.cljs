(ns knoxx.backend.infra.openplanner.memory
  (:require [clojure.string :as str]
            [knoxx.backend.infra.stores.session-store-registry :as store-registry]
            [knoxx.backend.infra.clients.openplanner :as openplanner-client]
            [knoxx.backend.shape.session-persistence :refer [complete-run!]]
            [knoxx.backend.extern.fastify :as fastify]
            [knoxx.backend.extern.promise :as promise]
            [knoxx.backend.domain.label.quality :as quality-labels]
            [knoxx.backend.domain.actor.scope :as actor-scope]
            [knoxx.backend.domain.time :as time]))

(defn- openplanner-configured?
  [config]
  (openplanner-client/enabled? (openplanner-client/client config)))

(defn- request-query-map
  [request]
  (fastify/request-query-string-map request))

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
  (when-not (openplanner-configured? config)
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
    (-> (openplanner-client/upsert-document! (openplanner-client/client config) (:document payload))
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

    (not (openplanner-configured? config))
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
                           (-> (promise/all-vec
                                (map (fn [doc]
                                       (upsert-openplanner-document! config
                                                                     (merge {:project project
                                                                             :visibility visibility
                                                                             :extra extra}
                                                                            doc)))
                                     chunk))
                               (.then (fn [chunk-results]
                                        (doseq [result chunk-results]
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
  (let [role  (some-> (:role row) str)
        text  (some-> (:text row) str)
        parts (or (get-in row [:extra :content_parts])
                  (get-in row [:metadata :content_parts]))]
    (when (and (contains? #{"user" "assistant" "system"} role)
               (not (str/blank? text)))
      (let [text-block #js {:type "text" :text text}
            content-arr (if (seq parts)
                          (clj->js (into [text-block]
                                         (mapv (fn [p]
                                                 (if (= "image" (:type p))
                                                   #js {:type "image_url"
                                                        :image_url #js {:url (or (:url p)
                                                                                 (str "data:" (:mimeType p) ";base64," (:data p)))}}
                                                   #js {:type "text" :text (or (:text p) "")}))
                                               parts)))
                          #js [text-block])]
        #js {:role role
             :content content-arr
             :timestamp (planner-row-timestamp-ms row)}))))

(defn rehydrate-session-manager!
  [config session-manager conversation-id _model-id]
  (if (or (str/blank? conversation-id)
          (not (openplanner-configured? config)))
    (js/Promise.resolve session-manager)
    (-> (openplanner-client/session! (openplanner-client/client config) conversation-id nil)
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

(defn- hit-metadata
  [hit]
  (or (:metadata hit) hit {}))

(defn- hit-text
  [hit]
  (str (or (:snippet hit) (:document hit) (:text hit) "")))

(defn- reasoning-memory-hit?
  [hit]
  (let [metadata (hit-metadata hit)
        kind (str (or (:kind hit) (:kind metadata) ""))
        role (str (or (:role hit) (:role metadata) ""))
        id (str (or (:id hit) (:parent_id metadata) (:parent-id metadata) ""))]
    (or (= kind "knoxx.reasoning")
        (= kind "reasoning")
        (= (:node_type metadata) "reasoning")
        (= (:node-type metadata) "reasoning")
        (= role "reasoning")
        (str/includes? id ":reasoning"))))

(defn- operational-failure-memory-hit?
  [hit]
  (let [text (hit-text hit)]
    (boolean
     (or (re-find #"(?i)\b403\s+No upstream providers are allowed\b" text)
         (re-find #"(?i)\bNo upstream providers are allowed for this tenant and request\b" text)
         (re-find #"(?i)\bprovider_not_allowed\b" text)))))

(defn- default-memory-hit?
  [hit]
  (and (not (reasoning-memory-hit? hit))
       (not (operational-failure-memory-hit? hit))
       (quality-labels/not-bad? hit)))

(defn- default-memory-hits
  [hits limit]
  (->> (or hits [])
       (filter default-memory-hit?)
       quality-labels/good-first-then-not-bad
       (take limit)
       vec))

(defn- ^:async fetch-session-summary!
  [config session-id]
  (let [rows (or (:rows (await (openplanner-client/session! (openplanner-client/client config) session-id nil))) [])
        row (or (last (filter #(and (contains? #{"assistant" "system"} (:role %))
                                   (default-memory-hit? %))
                               rows))
                (last (filter default-memory-hit? rows)))]
    (when row
      {:session session-id
       :role (:role row)
       :text (:text row)})))

(defn ^:async openplanner-recent-session-summaries!
  [config]
  (let [session-ids (->> (or (:rows (await (openplanner-client/sessions!
                                            (openplanner-client/client config)
                                            {:project (:session-project-name config)}))) [])
                         (map :session)
                         (remove str/blank?)
                         distinct
                         (take 4)
                         vec)]
    (if (seq session-ids)
      (let [results (await (promise/all-vec (map #(fetch-session-summary! config %) session-ids)))]
        (->> results
             (remove nil?)
             vec))
      [])))

(defn ^:async openplanner-memory-search!
  [config {:keys [query k session-id]}]
  "Search OpenPlanner's indexed document corpus via vector similarity.
   Returns {:query, :mode, :hits} where each hit has :id, :document, :metadata, :distance. "
  (let [query (str/trim (or query ""))
        k (max 1 (min 12 (or k 7)))
        fetch-k (max k (min 36 (* k 3)))]
    (if (str/blank? query)
      (throw (js/Error "Must provide query string for memory search"))
      {:query query
       :mode :vector
       :hits (default-memory-hits (vector-result-hits (:result (await (openplanner-client/vector-search!
                                                                        (openplanner-client/client config)
                                                                        (cond-> {:q query
                                                                                 :k fetch-k
                                                                                 :source "knoxx"
                                                                                 :project (:session-project-name config)}
                                                                          (not (str/blank? session-id)) (assoc :session session-id)))))) k)})))

(defn openplanner-graph-query!
  [config {:keys [query lake node-type limit edge-limit]}]
  (let [k (max 1 (min 60 (or limit 15)))
        node-types (when-not (str/blank? (or node-type ""))
                     (vec (str/split node-type #",")))
        lakes (when-not (str/blank? (or lake ""))
                (vec (str/split lake #",")))]
    (openplanner-client/graph-memory!
     (openplanner-client/client config)
     (cond-> {:q (or query "")
              :k k
              :includeText true}
       node-types (assoc :nodeTypes node-types)
       lakes (assoc :lakes lakes)
       edge-limit (assoc :maxCost (/ 1.0 (max 0.01 (or edge-limit 16))))))))

(defn openplanner-semantic-search!
  [config {:keys [query k project source kind visibility]}]
  (let [query (str/trim (or query ""))
        k (max 1 (min 20 (or k 10)))]
    (if (str/blank? query)
      (js/Promise.resolve {:query "" :hits [] :mode :none})
      (-> (openplanner-client/vector-search!
           (openplanner-client/client config)
           (cond-> {:q query :k k :project (or project (:project-name config) "devel")}
             source (assoc :source source)
             kind (assoc :kind kind)
             visibility (assoc :visibility visibility)))
          (.then (fn [body]
                   {:query query
                    :mode :vector
                    :hits (vector-result-hits (:result body))}))))))

(defn openplanner-graph-export!
  [config request]
  (openplanner-client/graph-export! (openplanner-client/client config) (request-query-map request)))

(defn operational-failure-text?
  [text]
  (let [text (str text)]
    (boolean
     (or (re-find #"(?i)\b403\s+No upstream providers are allowed\b" text)
         (re-find #"(?i)\bNo upstream providers are allowed for this tenant and request\b" text)
         (re-find #"(?i)\bprovider_not_allowed\b" text)))))

(defn quality-label-extra
  [quality explicit-meaning]
  {:openplanner_labels {:claim_system "knoxx-auto-quality-v1"
                        :quality quality
                        :explicit_meaning explicit-meaning
                        :labels [(str "quality:" quality)]
                        :updated_at (time/now-iso)}})

(defn output-quality-extra
  [text]
  (when (operational-failure-text? text)
    (quality-label-extra "bad" "operational provider error, not useful assistant output")))

(declare index-run-memory-legacy!)

(defn openplanner-event
  [config {:keys [id ts kind project session message role model text extra]}]
  {:schema "openplanner.event.v1"
   :id id
   :ts (or ts (time/now-iso))
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

(defn- sanitize-tool-receipt-for-indexing
  [receipt]
  (-> receipt
      (dissoc :content_parts)
      (dissoc :contentParts)
      (dissoc :attachments)))

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
  (let [base (select-keys run [:org_id :org_slug :user_id :user_email :membership_id])
        agent-spec (or (get-in run [:settings :agentSpec])
                       (:agent_spec run))
        contract-id (some-> (or (:contractId agent-spec)
                                (:contract-id agent-spec))
                            str
                            str/trim
                            not-empty)
        actor-id (some-> (or (:actorId agent-spec)
                             (:actor-id agent-spec))
                         str
                         str/trim
                         not-empty)
        contract-actors (actor-scope/actor-claims->wire
                         (or (:contractActors agent-spec)
                             (:contract-actors agent-spec)))
        sub-agent-id (some-> (or (:subAgentId agent-spec)
                                 (:sub-agent-id agent-spec))
                             str str/trim not-empty)
        parent-agent-id (some-> (or (:parentAgentId agent-spec)
                                    (:parent-agent-id agent-spec))
                                str str/trim not-empty)
        parent-run-id (some-> (or (:parentRunId agent-spec)
                                  (:parent-run-id agent-spec))
                              str str/trim not-empty)
        spawn-kind (some-> (or (:spawnKind agent-spec)
                               (:spawn-kind agent-spec))
                           str str/trim not-empty)]
    (cond-> base
      contract-id (assoc :contract_id contract-id)
      actor-id (assoc :actor_id actor-id)
      (seq contract-actors) (assoc :contract_actors contract-actors)
      sub-agent-id (assoc :sub_agent_id sub-agent-id)
      parent-agent-id (assoc :parent_agent_id parent-agent-id)
      parent-run-id (assoc :parent_run_id parent-run-id)
      spawn-kind (assoc :spawn_kind spawn-kind))))

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
                                                                   scope-extra
                                                                   (output-quality-extra safe-text))})
        devel-edges (for [{:keys [path target_node_id target_kind]} (extract-mentioned-devel-paths safe-text)]
                      (session-graph-edge-event config {:event-id (str node-id ":mentions_devel:" target_node_id)
                                                        :ts ts
                                                        :session conversation-id
                                                        :message node-id
                                                        :edge-type "mentions_devel_path"
                                                        :source-node-id node-id
                                                        :target-node-id target_node_id
                                                        :source-lake (:session-project-name config)
                                                        :target-lake "devel"
                                                        :extra (merge {:run_id run-id
                                                                       :conversation_id conversation-id
                                                                       :session_id session-id
                                                                       :path path
                                                                       :target_kind target_kind}
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

(defn- fail-open-indexing!
  [indexing-promise]
  (-> indexing-promise
      (.catch (fn [err]
                (.warn js/console "[openplanner-memory] run indexing failed; continuing without OpenPlanner persistence" err)
                nil))))

(defn index-run-memory!
  [config run extract-mentioned-devel-paths extract-mentioned-urls]
  (if-not (openplanner-configured? config)
    (js/Promise.resolve nil)
    (fail-open-indexing!
     (if-let [store @store-registry/session-store*]
       (complete-run! store (:run_id run)
                      {:status        (:status run)
                       :answer        (:answer run)
                       :error         (:error run)
                       :messages      (:request_messages run)
                       :trace_blocks  (:trace_blocks run)
                       :content_parts (:content_parts run)})
       (index-run-memory-legacy! config run extract-mentioned-devel-paths extract-mentioned-urls)))))

(defn- index-run-memory-legacy!
  [config run extract-mentioned-devel-paths extract-mentioned-urls]
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
          content-parts (->> (or (some-> (:request_messages run)
                                         first
                                         :content-parts)
                                [])
                              (mapv (fn [p]
                                    (if (and (= "image" (:type p))
                                             (str/blank? (:url p))
                                             (not (str/blank? (:data p))))
                                      {:type "image" :mimeType (:mimeType p)
                                       :data (subs (:data p) 0 (min 2048 (count (:data p))))
                                       :truncated true}
                                      (select-keys p [:type :url :mimeType :filename :text])))))
          base-events (cond-> [(openplanner-event config {:id (str run-id ":user")
                                                          :ts (:created_at run)
                                                          :kind "knoxx.message"
                                                          :project session-project
                                                          :session conversation-id
                                                          :message (str run-id ":user")
                                                          :role "user"
                                                          :model (:model run)
                                                          :text request-text
                                                          :extra (merge common-extra
                                                                        {:content_parts content-parts})})
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
                                                                         :total_time_ms (:total_time_ms run)}
                                                                        (output-quality-extra (run-summary-text run)))})]
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
                                                                        :trace_blocks trace-blocks}
                                                                       (output-quality-extra answer))}))
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
                                                                      :extra (merge common-extra {:receipt (sanitize-tool-receipt-for-indexing receipt)})})]
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
          content-parts (vec (or (:content_parts run) []))
          media-events  (when (seq content-parts)
                          [(openplanner-event config
                             {:id      (str run-id ":media")
                              :ts      (:created_at run)
                              :kind    "knoxx.run.media"
                              :project session-project
                              :session conversation-id
                              :message (str run-id ":media")
                              :role    "system"
                              :model   (:model run)
                              :text    (str "Media context: "
                                            (count content-parts) " part(s)"
                                            " \u2014 "
                                            (str/join ", "
                                              (mapv (fn [p]
                                                      (str (or (:type p) "?")
                                                           "/" (or (:mimeType p) "?")
                                                           " " (or (:filename p) (:url p) "(inline)")))
                                                    content-parts)))
                              :extra   (merge common-extra
                                             {:content_parts_count   (count content-parts)
                                              :content_parts_summary (mapv #(select-keys % [:type :mimeType :filename :url])
                                                                           content-parts)})})])
          all-events (vec (concat base-events graph-events tool-events media-events))]
      (-> (openplanner-client/events! (openplanner-client/client config) all-events)
          (.catch (fn [err]
                    (.warn js/console "[knoxx] failed to index run memory into OpenPlanner" err)
                    nil)))))
