(ns knoxx.backend.infra.routes.app-lifecycle-test
  "Native HTTP compatibility checks for application orchestration extraction."
  (:require ["fastify" :as fastify]
            ["node:fs/promises" :as fs]
            ["node:os" :as os]
            ["node:path" :as path]
            [cljs.test :as test]
            [knoxx.backend.domain.action.run-state :as run-state]
            [knoxx.backend.domain.contracts.sources :as sources]
            [knoxx.backend.domain.voice.turn-control :as turns]
            [knoxx.backend.infra.agent.policy :as policy]
            [knoxx.backend.infra.agent.service :as service]
            [knoxx.backend.infra.agent.turn :as turn]
            [knoxx.backend.infra.clients.openplanner :as openplanner]
            [knoxx.backend.infra.routes.app :as app]
            [knoxx.backend.infra.stores.mongo-session-store :as sessions]
            [knoxx.backend.infra.tooling :as tooling]
            [knoxx.backend.shape.agent :as agent]))

(defn- ^:async request! [registrar method url payload & [config]]
  (let [server (fastify #js {})
        dependencies (assoc app/deps
                            :ensure-permission! (fn [_ _] true)
                            :with-request-context! (fn [_ _ _ handler] (handler nil)))]
    (try
      (registrar server #js {} (or config {}) dependencies)
      (let [response (await (.inject server (clj->js (cond-> {:method method :url url}
                                                      payload (assoc :payload payload)))))]
        {:status (.-statusCode response) :body (js->clj (.json response) :keywordize-keys true)})
      (finally (await (.close server))))))

(defn- ^:async with-chat! [validate send run]
  (with-redefs [sources/compose-source-refs (fn [_ & _groups] [])
                tooling/default-actor-id (fn [_] "actor-a")
                tooling/default-agent-contract-id (fn ([_] "agent-a") ([_ _] "agent-a"))
                tooling/effective-agent-contract (fn ([_ _] {:id "agent-a" :model "model-a"}) ([_ _ _] {:id "agent-a" :model "model-a"}))
                policy/validate-chat-policy! validate
                service/send-agent-turn! send]
    (await (run))))

(def ^:private starts
  [[app/api-knoxx-chat-start! "/api/knoxx/chat/start" "rag"]
   [app/api-knoxx-direct-start! "/api/knoxx/direct/start" "direct"]])

(test/deftest ^:async chat-policy-denial-never-enqueues-work
  (let [queued (atom [])]
    (await (with-chat!
            (fn [_ _] (throw (ex-info "quota exhausted" {:status 429 :code "quota_exhausted"})))
            (fn [_ _ body] (swap! queued conj body))
            (^:async fn []
              (doseq [[registrar url _] starts]
                (test/is (= 429 (:status (await (request! registrar "POST" url {:message "hello"})))))))))
    (test/is (empty? @queued))))

(test/deftest ^:async accepted-chat-preserves-mode-identities-and-wire-spellings
  (let [queued (atom [])]
    (await (with-chat! (fn [_ _] true) (fn [_ _ body] (swap! queued conj body))
            (^:async fn []
              (doseq [[registrar url mode] starts]
                (let [{:keys [status body]} (await (request! registrar "POST" url
                                                   {:message "hello" :conversation_id "conversation-a"
                                                    :run_id "run-a"}))]
                  (test/is (= 202 status) (pr-str body))
                  (test/is (= mode (:mode (last @queued))))
                  (test/is (= "conversation-a" (:conversation_id body)))
                  (test/is (= "run-a" (:run_id body)))
                  (test/is (= "model-a" (:model body)))
                  (test/is (= (= "rag" mode) (contains? body :conversationId))))))))))

(test/deftest ^:async live-stream-conflicts-preserve-both-existing-response-shapes
  (with-redefs [sessions/get-session (fn ([_] {:status "completed"}) ([_ _] {:status "completed"}))
                agent/streaming? (fn [_] true)
                service/active-agent-session (fn [_] #js {:streaming true})]
    (await (with-chat! (fn [_ _] true) (fn [& _] (throw (js/Error. "must not enqueue")))
            (^:async fn []
              (doseq [[registrar url mode] starts]
                (let [{:keys [status body]} (await (request! registrar "POST" url
                                                   {:message "hello" :session_id "session-a"
                                                    :conversation_id "conversation-a"}))]
                  (test/is (= 409 status) (pr-str body))
                  (test/is (= (if (= mode "rag") "agent-already-processing" "agent_already_processing")
                              (:code body))))))))))

(test/deftest ^:async session-status-refuses-missing-id-and-reports-absent-state
  (with-redefs [sessions/get-session (fn ([_] nil) ([_ _] nil))
                service/active-agent-session (fn [_] nil)
                turns/active-turn (fn [_] nil)]
    (test/is (= 400 (:status (await (request! app/api-knoxx-session-status! "GET"
                                            "/api/knoxx/session/status" nil)))))
    (let [result (await (request! app/api-knoxx-session-status! "GET"
                                 "/api/knoxx/session/status?sessionId=s&conversationId=c" nil))]
      (test/is (= 200 (:status result)))
      (test/is (= "not_found" (get-in result [:body :status])))
      (test/is (true? (get-in result [:body :can_send]))))))

(test/deftest ^:async undo-session-refuses-running-work-before-mutating
  (let [writes (atom [])]
    (with-redefs [sessions/get-session (fn ([_] {:status "running"}) ([_ _] {:status "running"}))
                  sessions/undo-session-turns! (fn [& args] (swap! writes conj args))]
      (test/is (= 400 (:status (await (request! app/api-knoxx-session-undo! "POST"
                                              "/api/knoxx/session/undo" {})))))
      (test/is (= 409 (:status (await (request! app/api-knoxx-session-undo! "POST"
                                              "/api/knoxx/session/undo" {:sessionId "s"}))))))
    (test/is (empty? @writes))))

(test/deftest ^:async undo-session-preserves-access-check-and-native-result
  (let [calls (atom [])]
    (with-redefs [sessions/get-session (fn ([_] {:status "completed" :conversation_id "stored-c"
                                                   :messages [{:role "user" :content "first"}
                                                              {:role "assistant" :content "reply"}]})
                                           ([_ _] nil))
                  turn/ensure-conversation-access! (fn [ctx id] (swap! calls conj [:access ctx id]))
                  sessions/undo-session-turns! (fn ([id turn-count] (swap! calls conj [:undo id turn-count]))
                                                  ([_ id turn-count] (swap! calls conj [:undo id turn-count])))]
      (let [{:keys [status body]} (await (request! app/api-knoxx-session-undo! "POST"
                                          "/api/knoxx/session/undo"
                                          {:sessionId "s" :actorId "actor-a" :turns "invalid"}))]
        (test/is (= 200 status))
        (test/is (= "stored-c" (:conversation_id body)))
        (test/is (= 2 (:removed_count body)))
        (test/is (= [[:access {:actorId "actor-a"} "stored-c"] [:undo "s" 1]] @calls))))))

(test/deftest ^:async admin-abort-preserves-selected-session-and-default-reason
  (let [calls (atom [])]
    (with-redefs [sessions/get-session (fn ([_] {:conversation_id "stored-c" :session_id "s"}) ([_ _] nil))
                  turns/abort-active-turn! (fn [id reason] (swap! calls conj [:abort id reason]) {:aborted true})
                  sessions/update-session! (fn ([id value] (swap! calls conj [:persist id value]))
                                               ([_ id value] (swap! calls conj [:persist id value])))]
      (test/is (= 400 (:status (await (request! app/api-admin-agents-abort! "POST"
                                              "/api/admin/agents/abort" {})))))
      (let [{:keys [status body]} (await (request! app/api-admin-agents-abort! "POST"
                                          "/api/admin/agents/abort" {:sessionId "s"}))]
        (test/is (= 200 status))
        (test/is (true? (:marked_aborted body)))
        (test/is (= [[:abort "stored-c" "operator_abort"]
                    [:persist "s" {:status "aborted" :error "operator_abort" :has_active_stream false}]]
                    @calls))))))

(test/deftest ^:async control-abort-decodes-the-native-conversation-field
  (let [calls (atom [])]
    (with-redefs [turn/ensure-conversation-access! (fn [ctx id] (swap! calls conj [:access ctx id]))
                  turns/abort-active-turn! (fn [id reason] (swap! calls conj [:abort id reason]) {:ok true})]
      (let [result (await (request! app/api-knoxx-abort! "POST" "/api/knoxx/abort"
                                    {:conversationId "c" :actorId "actor-a" :reason "human-stop"}))]
        (test/is (= 200 (:status result)))
        (test/is (= [[:access {:actorId "actor-a"} "c"] [:abort "c" "human-stop"]] @calls))))))

(test/deftest ^:async session-only-admin-abort-also-marks-its-known-run
  (let [marked (atom [])]
    (with-redefs [sessions/get-session (fn ([_] {:conversation_id "c" :session_id "s" :run_id "run-a"})
                                           ([_ _] nil))
                  turns/abort-active-turn! (fn [_ _] {:aborted true})
                  sessions/update-session! (fn ([_ _] true) ([_ _ _] true))
                  run-state/update-run! (fn [id update-run]
                                          (swap! marked conj [id (select-keys (update-run {}) [:status :error])]))]
      (let [result (await (request! app/api-admin-agents-abort! "POST"
                                    "/api/admin/agents/abort" {:sessionId "s"}))]
        (test/is (= "run-a" (get-in result [:body :run_id])))
        (test/is (= [["run-a" {:status "aborted" :error "operator_abort"}]] @marked))))))

(test/deftest ^:async semantic-job-preserves-requested-numeric-options
  (let [calls (atom [])]
    (with-redefs [openplanner/client (fn ([_] :client) ([_ _] :client))
                  openplanner/build-semantic-edges! (fn [client options]
                                                      (swap! calls conj [client options]) {:ok true})]
      (let [result (await (request! app/api-data-jobs-build-semantic-edges! "POST"
                                    "/api/data/jobs/build-semantic-edges" {:k 3 :minSimilarity 0.8}))]
        (test/is (= 200 (:status result)))
        (test/is (= [[:client {:k 3 :minSimilarity 0.8}]] @calls))))))

(test/deftest ^:async ingestion-file-endpoint-decodes-path-and-content
  (let [directory (await (.mkdtemp fs (.join path (.tmpdir os) "knoxx-app-ingestion-")))]
    (try
      (let [result (await (request! app/api-ingestion-file-put! "PUT" "/api/ingestion/file"
                                    {:path "notes/page.md" :content "Human-readable source."}
                                    {:workspace-root directory}))]
        (test/is (= 200 (:status result)))
        (test/is (= "Human-readable source." (await (.readFile fs (.join path directory "notes/page.md") "utf8")))))
      (finally (await (.rm fs directory #js {:recursive true :force true}))))))
