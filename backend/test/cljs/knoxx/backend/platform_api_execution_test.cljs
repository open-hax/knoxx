(ns knoxx.backend.platform-api-execution-test
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.domain.action.run-state :as runs]
            [knoxx.backend.domain.agent.agent-context :as context]
            [knoxx.backend.domain.social.api-actions :as actions]
            [knoxx.backend.domain.social.api-json-request :as request]
            [knoxx.backend.domain.social.api-receipts :as receipts]
            [knoxx.backend.domain.voice.turn-control :as turns]
            [knoxx.backend.extern.platform-api-catalog :as catalog]
            [knoxx.backend.extern.social-api-codec :as codec]
            [knoxx.backend.extern.social-api-http :as http]
            [knoxx.backend.infra.actor.acting :as acting]
            [knoxx.backend.infra.actor.credentials :as credentials]
            [knoxx.backend.infra.agent.session :as session]
            [knoxx.backend.infra.social.api-tools :as tools]
            [knoxx.backend.law.social-delivery :as delivery]
            [knoxx.backend.platform-json-request-test :as fixture]
            [knoxx.backend.social-delivery-test :as evidence]))

(def input (fixture/discord-input "create_message" {"content" "hello"}))
(def response {:ok true :status 200 :body {"id" "message" "channel_id" "123"}
               :empty-body? false :rate-limit {} :redacted-fields 0})

(defn- http-spy
  [f]
  (fn ([plan auth] (f plan auth)) ([plan auth options] (f plan auth options))))

(defn- suite
  [extra]
  (let [generic (first (tools/create-tools nil {} {:toolPolicies [{:toolId "social.request" :effect "allow"}]}))
        native #js {:name "discord_send" :execute (fn [& _] #js {:content #js [] :details #js {:sent true :messageId "native"}})}
        all #js [generic native]]
    (session/wrap-custom-tools-with-agent-context! all (merge {:conversation-id "c" :agent-spec {:actor-id "actor"}} extra))
    [generic native]))

(defn- ^:async call!
  [tool input]
  (await ((aget tool "execute") "call" (.parse js/JSON (codec/encode input)) nil nil nil)))

(defn- ^:async failure!
  [tool input]
  (try (await (call! tool input)) nil (catch :default error error)))

(defn- ^:async with-runtime!
  [f]
  (let [asset (await (fixture/pinned-catalog))]
    (with-redefs [runs/runs* (atom {"r" {:run_id "r" :conversation_id "c" :actor_id "actor" :status "running" :tool_receipts []}})
                  turns/active-turn (constantly {:run_id "r"})
                  catalog/read! (fn [_] {:catalog asset :sha256 "pinned-test"})
                  credentials/get-credential! (fn [& _] {:secretJson {:botToken "fake-owned"}})]
      (try (await (f asset))
           (catch :default error
             (is false (str "Unexpected API test failure: " (ex-message error) " " (pr-str (ex-data error)))))))))

(deftest ^:async actual-generic-and-native-tools-share-one-action-key
  (await (with-runtime!
          (^:async fn [_]
            (doseq [generic-first? [true false]]
              (let [[generic native] (suite {}) calls (atom 0)]
                (with-redefs [http/execute-json! (http-spy (fn [& _] (swap! calls inc) response))]
                  (if generic-first? (await (call! generic input)) (await (call! native {:channel_id "123" :text "hello"})))
                  (let [refused (if generic-first? (await (failure! native {:channel_id "123" :text "hello"}))
                                     (await (failure! generic input)))]
                    (is (= :duplicate-action (:reason (ex-data refused))))
                    (is (= (if generic-first? 1 0) @calls))))))))))

(deftest ^:async failed-generic-request-pauses-native-aliases-too
  (await (with-runtime!
          (^:async fn [_]
            (let [[generic native] (suite {}) calls (atom 0)]
              (with-redefs [http/execute-json! (http-spy (fn [& _] (swap! calls inc) (assoc response :ok false :status 403 :body {"code" 50013})))]
                (let [result (await (call! generic input))
                      refused (await (failure! native {:channel_id "123" :text "changed"}))]
                  (is (true? (.-isError result)))
                  (is (= :unresolved-audience (:reason (ex-data refused))))
                  (is (= 1 @calls)))))))))

(deftest ^:async definite-local-credential-failure-releases-policy-and-action-reservations
  (await (with-runtime!
          (^:async fn [_]
            (reset! runs/runs* {"r" {:run_id "r" :conversation_id "c" :actor_id "actor" :status "running"
                                     :tool_receipts (mapv #(if (= "home" (get-in % [:input :channel_id]))
                                                            (assoc-in % [:input :channel_id] "123") %) evidence/complete-evidence)}})
            (let [[generic _] (suite {:delivery-policy (assoc evidence/policy :channel-id "123")}) calls (atom 0)
                  input (fixture/discord-input "create_message" {"content" (:text evidence/params)})
                  input (assoc input :parameters [{:name "channel_id" :value "123"}])]
              (with-redefs [credentials/get-credential! (fn [& _] (throw (js/Error. "not configured")))]
                (let [failed (await (failure! generic input))]
                  (is (= :not-attempted (get-in (ex-data failed) [:api-action-outcomes 0 :status])))
                  (is (.includes (ex-message failed) "No platform write was attempted"))))
              (with-redefs [http/execute-json! (http-spy (fn [& _] (swap! calls inc) response))]
                (is (not (true? (.-isError (await (call! generic input)))))))
              (is (= 1 @calls)))))))

(deftest ^:async a-turn-ending-during-catalog-load-never-enters-a-new-run
  (await (with-runtime!
          (^:async fn [asset]
            (let [[generic _] (suite {}) calls (atom 0)]
              (with-redefs [catalog/read! (fn [_]
                                           (reset! runs/runs* {"new" {:run_id "new" :conversation_id "c" :actor_id "actor" :status "running"}})
                                           {:catalog asset :sha256 "pinned-test"})
                            turns/active-turn (fn [_] {:run_id (if (contains? @runs/runs* "new") "new" "r")})
                            http/execute-json! (http-spy (fn [& _] (swap! calls inc) response))]
                (let [failed (await (failure! generic input))]
                  (is (= "platform_action_run_changed" (:code (ex-data failed))))
                  (is (zero? @calls)))))))))

(deftest ^:async direct-scoped-write-refuses-before-credentials-or-transport
  (let [asset (await (fixture/pinned-catalog)) observed (atom [])
        old (context/get-context)]
    (try
      (context/set-context! {:agent-spec {:actor-id "foreign"}
                             :platform-admission {:reserve-many! #(swap! observed conj :borrowed)}})
      (with-redefs [catalog/read! (fn [_] {:catalog asset :sha256 "test"})
                    credentials/get-credential! (fn [& _] (swap! observed conj :credentials))
                    http/execute-json! (fn [& _] (swap! observed conj :transport))]
        (let [error (try
                      (await (acting/run-as! {:actor-id "owner" :org-id "org" :membership-id "member"}
                                            #(tools/request-execute nil {} "call" (codec/json-value input) nil nil nil)))
                      nil
                      (catch :default error error))]
          (is (= "platform_action_context_required" (:code (ex-data error))))
          (is (empty? @observed))))
      (finally (if old (context/set-context! old) (context/clear-context!))))))

(deftest ^:async destructive-api-write-is-refused-before-credentials
  (await
   (with-runtime!
    (^:async fn [_]
      (let [[generic _] (suite {}) calls (atom [])
            input {:platform "discord" :operation "delete_message"
                   :parameters [{:name "channel_id" :value "123"}
                                {:name "message_id" :value "456"}]}]
        (with-redefs [credentials/get-credential! (fn [& _] (swap! calls conj :credentials))
                      http/execute-json! (fn [& _] (swap! calls conj :transport))]
          (let [failed (await (failure! generic input))]
            (is (= "platform_effect_not_allowed" (:code (ex-data failed))))
            (is (empty? @calls)))))))))

(deftest ^:async direct-scoped-read-keeps-actor-across-awaits
  (let [asset (await (fixture/pinned-catalog)) observed (atom nil)
        query {:platform "bluesky" :operation "tools.ozone.safelink.queryRules"
               :body {"limit" 3 "urls" ["example.test"]}}]
    (with-redefs [catalog/read! (fn [_] {:catalog asset :sha256 "test"})
                  credentials/get-credential! (fn [& _] (reset! observed (credentials/current-actor-id)) {:secretJson {}})
                  http/execute-json! (http-spy (fn [& _] response))]
      (await (acting/run-as! {:actor-id "owner" :org-id "org" :membership-id "member"}
                            #(tools/request-execute nil {} "call" (codec/json-value query) nil nil nil)))
      (is (= "owner" @observed)))))

(deftest ^:async follow-handle-and-api-subject-use-one-action-key
  (await
   (with-runtime!
    (^:async fn [_]
      (let [did "did:plc:followed" observed (atom [])
            generic (first (tools/create-tools nil {} {:toolPolicies [{:toolId "social.request" :effect "allow"}]}))
            native #js {:name "bluesky_follow"
                        :execute (fn [_call-id args & _]
                                   (swap! observed conj (aget args "actor"))
                                   #js {:content #js [] :details #js {:uri "at://did:plc:self/app.bsky.graph.follow/record"
                                                                    :cid "test-cid"}})}
            request {:platform "bluesky" :operation "com.atproto.repo.createRecord"
                     :body {"repo" "did:plc:self" "collection" "app.bsky.graph.follow"
                            "record" {"subject" did "createdAt" "2026-10-27T00:00:00Z"}}}]
        (session/wrap-custom-tools-with-agent-context!
         #js [generic native] {:conversation-id "c" :agent-spec {:actor-id "actor"}
                               :resolve-bluesky-actor! (fn [handle]
                                                         (is (= "followed.test" handle))
                                                         {:did did})})
        (with-redefs [http/execute-json! (http-spy (fn [& _] (swap! observed conj :generic-transport) response))]
          (await (call! native {:actor "followed.test"}))
          (let [error (await (failure! generic request))]
            (is (= :duplicate-action (:reason (ex-data error))))
            (is (= [did] @observed)))))))))

(deftest ^:async a-follow-never-borrows-the-next-run-during-handle-resolution
  (await
   (with-runtime!
    (^:async fn [_]
      (let [resolved-run (atom "r") calls (atom 0)
            native #js {:name "bluesky_follow" :execute (fn [& _] (swap! calls inc))}]
        (session/wrap-custom-tools-with-agent-context!
         #js [native]
         {:conversation-id "c" :agent-spec {:actor-id "actor"}
          :resolve-bluesky-actor! (fn [_]
                                   (swap! runs/runs* assoc "new"
                                          {:run_id "new" :conversation_id "c" :actor_id "actor" :status "running"})
                                   (reset! resolved-run "new")
                                   {:did "did:plc:followed"})})
        (with-redefs [turns/active-turn (fn [_] {:run_id @resolved-run})]
          (let [error (await (failure! native {:actor "followed.test"}))]
            (is (= "platform_action_run_changed" (:code (ex-data error))))
            (is (zero? @calls)))))))))

(deftest ^:async partial-batch-results-keep-known-evidence-without-fabricating-missing-results
  (let [asset (await (fixture/pinned-catalog))
        prepared (request/json-plan asset {:platform "bluesky" :operation "com.atproto.repo.applyWrites"
                                            :body {"repo" "did:plc:self" "writes"
                                                   [{"$type" "com.atproto.repo.applyWrites#create" "collection" "app.bsky.feed.like"
                                                     "value" {"subject" {"uri" "at://did:plc:other/app.bsky.feed.post/post" "cid" "source"} "createdAt" "2026-10-27T00:00:00Z"}}
                                                    {"$type" "com.atproto.repo.applyWrites#create" "collection" "app.bsky.feed.post"
                                                     "value" {"text" "authored" "createdAt" "2026-10-27T00:00:00Z"}}]}})
        actions (actions/planned-actions prepared "actor")
        response (assoc response :authenticated-did "did:plc:self" :body {"results" [{"$type" "com.atproto.repo.applyWrites#createResult"
                                                                                  "uri" "at://did:plc:self/app.bsky.feed.like/like" "cid" "like-cid"}]})
        outcomes (receipts/outcomes actions response)]
    (is (= [:confirmed :unknown] (mapv :status outcomes)))
    (is (empty? (receipts/publications actions response outcomes)))))

(deftest canonical-publication-projection-retains-original-order-and-only-confirmed-posts
  (let [post {:uri "at://did:plc:self/app.bsky.feed.post/first" :cid "cid" :text "one" :reply_to "" :actorDid "did:plc:self"}
        receipt (evidence/receipt "social_request" {:body {"record" {"text" "one"}}} {:publications [post]})
        native (evidence/receipt "bluesky_publish" {:text "two"} {:uri "at://did:plc:self/app.bsky.feed.post/second" :cid "cid2"})
        projected (vec (delivery/confirmed-publications [receipt native]))]
    (is (= ["one" "two"] (mapv #(get-in % [:input :text]) projected)))
    (is (= [(:uri post) "at://did:plc:self/app.bsky.feed.post/second"] (mapv #(get-in % [:result :details :uri]) projected)))
    (is (empty? (delivery/confirmed-publications [(assoc receipt :is_error true)])))))
