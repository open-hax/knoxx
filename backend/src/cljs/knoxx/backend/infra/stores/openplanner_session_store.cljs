(ns knoxx.backend.infra.stores.openplanner-session-store
  "OpenPlanner driver for ISessionStore.

   Writes runs as structured events. Reads are best-effort via graph query.
   This store is authoritative for COMPLETED runs only.
   In-flight runs are owned by Redis."
  (:require [shadow.cljs.modern :refer [js-await]]
            [clojure.string :as str]
            [knoxx.backend.shape.session-persistence :refer [ISessionStore assert-run!]]
            [knoxx.backend.infra.openplanner.memory :as op-mem]
            [knoxx.backend.infra.clients.openplanner :as openplanner-client]
            [knoxx.backend.domain.time :as time]))

(defn- run->events
  "Translate a KnoxxRun into the openplanner.event.v1 wire format."
  [config run]
  (let [{:keys [run_id session_id conversation_id status model
                answer reasoning error messages
                tool_receipts trace_blocks content_parts
                created_at updated_at
                org_id user_id]} run
        session-project (:session-project-name config)
        scope {:run_id run_id :session_id session_id
               :conversation_id conversation_id
               :status status
               :org_id org_id :user_id user_id}
        mk (fn [id kind role text extra]
             (op-mem/openplanner-event config
               {:id id
                :ts (or updated_at created_at (time/now-iso))
                :kind kind
                :project session-project
                :session conversation_id
                :message id
                :role role
                :model model
                :text text
                :extra (merge scope extra)}))
        request-text (or (some-> messages first :content) "")
        user-extra (cond-> {:run_id run_id :session_id session_id
                            :conversation_id conversation_id}
                     (seq content_parts)
                     (assoc :content_parts
                            (mapv (fn [p]
                                    (if (and (= "image" (:type p))
                                             (str/blank? (:url p))
                                             (not (str/blank? (:data p))))
                                      {:type "image" :mimeType (:mimeType p)
                                       :data (subs (str (:data p)) 0 (min 2048 (count (str (:data p)))))
                                       :truncated true}
                                      (select-keys p [:type :url :mimeType :filename :text])))
                                  content_parts)))]
    (cond-> []
      (not (str/blank? request-text))
      (conj (mk (str run_id ":user") "knoxx.message" "user"
                request-text user-extra))
      true
      (conj (mk (str run_id ":run") "knoxx.run" "system"
                 (str "Run " run_id " · " status)
                 {:trace_blocks trace_blocks
                  :message_count (count messages)}))
      (not (str/blank? answer))
      (conj (mk (str run_id ":assistant") "knoxx.message" "assistant"
                 answer (merge {:status status}
                               (op-mem/output-quality-extra answer))))
      (not (str/blank? reasoning))
      (conj (mk (str run_id ":reasoning") "knoxx.reasoning" "system"
                 reasoning {:status status}))
      (not (str/blank? error))
      (conj (mk (str run_id ":error") "knoxx.error" "system"
                 error (merge {:status status}
                              (op-mem/output-quality-extra error))))
      (seq tool_receipts)
      (into (mapv (fn [r]
                    (mk (str run_id ":tool:" (:id r))
                        "knoxx.tool_receipt" "system"
                        (op-mem/tool-receipt-summary-text r)
                        {:receipt r}))
                  tool_receipts)))))

(defrecord OpenPlannerSessionStore [config]
  ISessionStore

  (put-run! [_ run]
    (assert-run! run "OpenPlannerSessionStore/put-run!")
    (let [events (run->events config run)]
      (js-await [_ (openplanner-client/events! (openplanner-client/client config) events)]
        run)))

  (get-run [_ run-id]
    (js-await [result (openplanner-client/vector-search! (openplanner-client/client config)
                                                          {:q run-id
                                                           :k 1
                                                           :project (:session-project-name config)
                                                           :kind "knoxx.run"})]
      (when-let [hit (first (:hits result))]
        (some-> hit :metadata :run_payload))))

  (patch-run! [store run-id patch]
    (js-await [current (.get-run store run-id)]
      (let [updated (merge (or current {:run_id run-id}) patch
                           {:updated_at (time/now-iso)})]
        (.put-run! store updated))))

  (list-active-runs [_ _session-id]
    (js/Promise.resolve []))

  (complete-run! [store run-id opts]
    (.patch-run! store run-id
                 (merge {:status "completed"
                         :has_active_stream false}
                        (select-keys opts [:status :answer :error
                                           :trace_blocks :messages]))))

  (delete-run! [_ _run-id]
    (js/Promise.resolve true)))