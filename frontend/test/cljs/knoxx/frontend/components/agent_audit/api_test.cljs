(ns knoxx.frontend.components.agent-audit.api-test
  "Real loopback HTTP → existing JSON helper → audit adapter → session merge."
  (:require ["node:http" :as http]
            [cljs.test :as t]
            [knoxx.frontend.components.agent-audit.api :as api]
            [knoxx.frontend.components.agent-audit.logic :as logic]))

(def ^:private active-runs
  [{:run_id "fork-active-run-id" :session_id "fork-active-session-id"
    :conversation_id "fork-run-active" :status "running" :model "gemma4:31b"
    :created_at "2026-05-14T23:10:00.000Z" :updated_at "2026-05-14T23:12:00.000Z"
    :has_active_stream true :active_turn_registered true
    :latest_user_message "Advance Fork Tales lore and compose the next song."
    :agent_spec {:contractId "fork_tales_creative_director"
                 :subAgentId "fork_tales_creative_director" :role "knowledge_worker"}}])

(defn- respond! [mode calls ^js request ^js response]
  (let [url (.-url request)
        denied? (or (= mode :all)
                    (and (= mode :admin) (.startsWith url "/api/admin/")))]
    (swap! calls conj url)
    (set! (.-statusCode response) (if denied? 403 200))
    (.setHeader response "Content-Type" "application/json")
    (.end response (js/JSON.stringify
                    (clj->js (if denied? {:error "fixture denied"}
                                 {:runs active-runs :count (count active-runs)}))))))

(defn- listen! [^js server]
  (js/Promise.
   (fn [complete reject]
     (.once server "error" reject)
     (.listen server 0 "127.0.0.1" complete))))

(defn- close! [^js server]
  (js/Promise.
   (fn [complete reject]
     (.close server (fn [error] (if error (reject error) (complete nil))))
     (.closeAllConnections server))))

(defn- ^:async with-active-http [mode run]
  (let [calls (atom [])
        ^js server (.createServer http #(respond! mode calls %1 %2))
        original-fetch js/fetch]
    (await (listen! server))
    (let [origin (str "http://127.0.0.1:" (.-port (.address server)))]
      (set! (.-fetch js/globalThis)
            (fn [path options]
              (let [url (js/URL. path origin)]
                (if (= origin (.-origin url))
                  (original-fetch url options)
                  (js/Promise.reject (js/Error. "Unexpected fixture destination"))))))
      (try
        (await (run calls))
        (finally
          (set! (.-fetch js/globalThis) original-fetch)
          (await (close! server)))))))

(defn- ^:async check-run-vector [read-runs path]
  (await
   (with-active-http
    :none
    (^:async fn [calls]
      (let [runs (await (read-runs 25))]
        (t/is (vector? runs) "the actual HTTP response page must become the run vector")
        (t/is (= active-runs runs) "preserve every returned row and field")
        (t/is (= ["fork-run-active"]
                 (mapv :session (logic/merge-sessions [] runs "fork_tales_creative_director")))
              "the adapter output must reach the unchanged actual merge")
        (t/is (= [path] @calls)))))))

(t/deftest ^:async caller-http-response-exposes-active-runs
  (await (check-run-vector api/list-active-agents "/api/knoxx/agents/active?limit=25")))

(t/deftest ^:async admin-http-response-exposes-active-runs
  (await (check-run-vector api/list-admin-active-agents "/api/admin/agents/active?limit=25")))

(t/deftest ^:async operator-fallback-keeps-the-caller-run-vector
  (await
   (with-active-http
    :admin
    (^:async fn [calls]
      (let [runs (await (api/list-operator-active-agents))]
        (t/is (= active-runs runs))
        (t/is (= ["fork-run-active"]
                 (mapv :session (logic/merge-sessions [] runs "fork_tales_creative_director"))))
        (t/is (= ["/api/admin/agents/active?limit=25" "/api/knoxx/agents/active?limit=25"] @calls)))))))

(t/deftest ^:async operator-fallback-preserves-http-error-propagation
  (await
   (with-active-http
    :all
    (^:async fn [calls]
      (let [message (try (await (api/list-operator-active-agents)) nil
                         (catch :default error (.-message error)))]
        (t/is (boolean (and message (re-find #"fixture denied" message))))
        (t/is (= ["/api/admin/agents/active?limit=25" "/api/knoxx/agents/active?limit=25"] @calls)))))))
