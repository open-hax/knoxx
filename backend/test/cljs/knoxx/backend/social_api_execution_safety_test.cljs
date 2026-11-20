(ns knoxx.backend.social-api-execution-safety-test
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.domain.action.run-state :as runs]
            [knoxx.backend.domain.social.api-actions :as actions]
            [knoxx.backend.domain.social.api-json-request :as request]
            [knoxx.backend.domain.voice.turn-control :as turns]
            [knoxx.backend.extern.social-api-http :as http]
            [knoxx.backend.infra.agent.action-admission :as admission]
            [knoxx.backend.infra.agent.social-delivery :as guards]
            [knoxx.backend.infra.social.api-execution :as execution]
            [knoxx.backend.law.social-delivery :as delivery]
            [knoxx.backend.platform-json-request-test :as fixture]
            [knoxx.backend.social-delivery-test :as evidence]))

(def post-uri "at://did:plc:outside/app.bsky.feed.post/parent")
(def root-uri "at://did:plc:outside/app.bsky.feed.post/original")
(def reply {"parent" {"uri" post-uri "cid" "parent-cid"}
            "root" {"uri" root-uri "cid" "root-cid" "$type" "com.atproto.repo.strongRef"}})
(def input {:platform "bluesky" :operation "com.atproto.repo.createRecord"
            :body {"repo" "did:plc:self" "collection" "app.bsky.feed.post"
                   "record" {"$type" "app.bsky.feed.post" "text" "A direct reply" "reply" reply "createdAt" "2026-10-27T00:00:00Z"}}})

(deftest ^:async actual-http-replies-preserve-original-root-and-exact-parent-version
  (let [plan (:plan (request/json-plan (await (fixture/pinned-catalog)) input))]
    (doseq [variant [:correct :wrong-root :stale-parent :unavailable]]
      (let [calls (atom [])
            plan (case variant :wrong-root (assoc-in plan [:body "record" "reply" "root" "uri"] post-uri)
                               :stale-parent (assoc-in plan [:body "record" "reply" "parent" "cid"] "old-cid") plan)
            fetcher (fn [url options]
                      (swap! calls conj [url (aget options "method")])
                      (js/Promise.resolve
                       (fixture/json-response
                        (cond
                          (.endsWith url "createSession") {"accessJwt" "fake-owned" "did" "did:plc:self" "handle" "self.test"}
                          (.includes url "getPostThread") {"thread" {"post" {"uri" post-uri "cid" "parent-cid"
                                                                                  "record" {"reply" {"root" {"uri" root-uri "cid" "root-cid"}}}}}}
                          :else {"uri" "at://did:plc:self/app.bsky.feed.post/reply" "cid" "reply-cid"})
                        (if (and (= :unavailable variant) (.includes url "getPostThread")) 404 200))))
            auth {:identifier "self.test" :password "fake-password" :pds "https://self.test" :scope-key "reply-test"}]
        (try
          (let [response (await (http/execute-json! plan auth {:fetch-fn fetcher :cache? false}))]
            (is (= :correct variant))
            (is (:ok response)))
          (catch :default error
            (is (not= :correct variant))
            (is (= :preflight (:phase (ex-data error))))))
        (is (= (if (= :correct variant) 3 2) (count @calls)))
        (is (= "GET" (second (second @calls))))))))

(deftest late-local-rollback-cannot-release-a-newer-guard-attempt
  (with-redefs [runs/runs* (atom {"r" {:run_id "r" :actor_id "actor" :conversation_id "c" :status "running"
                                      :tool_receipts evidence/complete-evidence}})
                turns/active-turn (constantly {:run_id "r"})]
    (let [guard (guards/make-guard {:actor-id "actor" :conversation-id "c" :policy evidence/policy})
          reserve! (:reserve! (meta guard)) first (reserve! evidence/params)
          release! (:release! (meta guard))]
      (is (release! first))
      (let [second (reserve! evidence/params)]
        (is (not= (:after first) (:after second)))
        (is (false? (release! first)))
        (is (thrown? js/Error (guard evidence/params)))))))

(deftest ^:async strict-publication-batch-refusal-consumes-neither-evidence-nor-action-budget
  (let [asset (await (fixture/pinned-catalog))
        prepared (request/json-plan asset {:platform "bluesky" :operation "com.atproto.repo.applyWrites"
                                            :body {"repo" "did:plc:self" "writes"
                                                   (mapv (fn [text] {"$type" "com.atproto.repo.applyWrites#create" "collection" "app.bsky.feed.post"
                                                                     "value" {"text" text "createdAt" "2026-10-27T00:00:00Z"}}) ["one" "two"])}})
        descriptors (actions/planned-actions prepared "actor") calls (atom 0)]
    (with-redefs [runs/runs* (atom {"r" {:run_id "r" :actor_id "actor" :conversation_id "c" :status "running"}})
                  turns/active-turn (constantly {:run_id "r"})]
      (let [port (admission/create {:actor-id "actor" :conversation-id "c"})
            context {:platform-admission port :delivery-guards {:bluesky (fn [_] (swap! calls inc))}}]
        (try (await (execution/execute! context "r" descriptors (fn [] (swap! calls inc))))
             (is false "A strict policy needs sequential confirmed posts")
             (catch :default error (is (= "platform_publication_batch_not_admitted" (:code (ex-data error))))))
        (is (zero? @calls))
        (is (map? ((:reserve! port) (:intent (first descriptors)))))))))

(defn- ^:async execute-admitted-fixture!
  [actions response]
  (with-redefs [runs/runs* (atom {"r" {:run_id "r" :actor_id "actor" :conversation_id "c" :status "running"}})
                turns/active-turn (constantly {:run_id "r"})]
    (await (execution/execute! {:platform-admission (admission/create {:actor-id "actor" :conversation-id "c"})}
                               "r" actions (constantly response)))))

(deftest ^:async generic-image-only-post-keeps-the-api-text-contract
  (let [prepared (request/json-plan (await (fixture/pinned-catalog))
                                   (-> input (assoc-in [:body "record" "text"] "") (update-in [:body "record"] dissoc "reply")))
        actions (actions/planned-actions prepared "actor")
        response {:ok true :status 200 :body {"uri" "at://did:plc:self/app.bsky.feed.post/image" "cid" "image-cid"}
                  :authenticated-did "did:plc:self"}
        result (await (execute-admitted-fixture! actions response))]
    (is (= [:confirmed] (mapv :status (:outcomes result))))
    (is (= "" (get-in result [:publications 0 :text])))))

(deftest ^:async a-repository-ack-is-not-a-feed-post-with-the-wrong-record-type
  (let [prepared (request/json-plan (await (fixture/pinned-catalog))
                                   (-> input (assoc-in [:body "record" "$type"] "example.invalid.record")
                                       (update-in [:body "record"] dissoc "reply")))
        actions (actions/planned-actions prepared "actor")
        response {:ok true :status 200 :body {"uri" "at://did:plc:self/app.bsky.feed.post/object" "cid" "object-cid"}
                  :authenticated-did "did:plc:self"}
        result (await (execute-admitted-fixture! actions response))]
    (is (= [:confirmed] (mapv :status (:outcomes result))) "The controlled response acknowledges the repository write")
    (is (empty? (:publications result)) "It does not qualify as an authored feed post")))

(deftest nested-source-urls-can-only-add-obligations
  (let [params (assoc evidence/params :source-text "embedded source https://unread.test/claim")
        check (delivery/delivery-evidence evidence/policy evidence/complete-evidence params)]
    (is (seq (:missing check)))
    (is (= ["https://unread.test/claim"] (:uninspected-source-urls check)))))
