(ns knoxx.backend.platform-json-request-test
  (:require [cljs.reader :as reader]
            [cljs.test :refer [deftest is]]
            [knoxx.backend.domain.social.api-json-request :as request]
            [knoxx.backend.extern.platform-api-catalog :as catalog]
            [knoxx.backend.extern.social-api-http :as http]
            [knoxx.backend.law.platform-api-request :as law]
            [knoxx.backend.law.platform-payload :as payload]))

(defn ^:async pinned-catalog
  "Read the same checksum-verified catalog used by the service."
  []
  (:catalog (await (catalog/read! ["../contracts" "contracts"]))))

(def poll
  {"content" "Which rhythm should our next sketch use?" "tts" false
   "poll" {"question" {"text" "Choose a rhythm"}
           "answers" [{"poll_media" {"text" "A slow pulse"}}
                       {"poll_media" {"text" "Broken percussion"}}]
           "allow_multiselect" false "duration" 24}
   "allowed_mentions" {"parse" []}
   "future_data" {"0" [false nil {"$type" "test#shape" "arbitrary-field" 1.5}]
                  "__proto__" {"retained" true} "constructor" {"prototype" "also retained"}}})

(defn discord-input
  "Pinned Discord operation with a channel binding."
  [operation body]
  {:platform "discord" :operation operation
   :parameters [{:name "channel_id" :value "123"}] :body body})

(defn json-response
  "A native streaming response for transport observation."
  [body status]
  (js/Response. (.stringify js/JSON (clj->js body))
                #js {:status status :headers #js {"content-type" "application/json"}}))

(deftest named-json-contracts-survive-edn-and-preserve-dynamic-keys
  (is (= law/registry (reader/read-string (pr-str law/registry))))
  (is (= poll (:body (law/assert-valid! law/Input (discord-input "create_message" poll)))))
  (doseq [body [{:keyword "not JSON"} ##Inf ##NaN (fn [] nil)]]
    (is (thrown? js/Error (law/assert-valid! law/Input (discord-input "create_message" body)))))
  (doseq [extra [{:headers {"Authorization" "forged"}} {:url "https://other.test"} {:actor "foreign"}]]
    (is (thrown? js/Error (law/assert-valid! law/Input (merge (discord-input "create_message" {}) extra)))))
  (is (thrown? js/Error (law/assert-valid! law/Input
                                         (discord-input "create_message" (nth (iterate vector "too deep") 34))))))

(deftest ^:async rich-json-body-retains-all-fields-and-real-effects
  (let [{:keys [plan effects]} (request/json-plan (await (pinned-catalog)) (discord-input "create_message" poll))]
    (is (= poll (:body plan)))
    (is (= "POST" (:method plan)))
    (is (= {"channel_id" "123"} (:path plan)))
    (is (= :publish (get-in effects [:actions 0 :effect])))
    (is (= [] (:empty-success-statuses plan)))))

(deftest ^:async basic-payload-shape-fails-before-any-credential-or-platform-access
  (let [asset (await (pinned-catalog))]
    (doseq [body [(assoc-in poll ["poll" "duration"] "24")
                  (assoc-in poll ["poll" "answers"] [])
                  (assoc-in poll ["poll" "duration"] 769)
                  (assoc-in poll ["poll" "question"] {})]]
      ;; PollMedia has no required text field, so the last shape is legal to
      ;; the basic validator; full Discord semantics remain authoritative.
      (if (= {} (get-in body ["poll" "question"]))
        (is (= body (get-in (request/json-plan asset (discord-input "create_message" body)) [:plan :body])))
        (is (thrown? js/Error (request/json-plan asset (discord-input "create_message" body))))))
    (doseq [body [false nil []]]
      (try (request/json-plan asset (discord-input "create_thread" body))
           (is false "Malformed root bodies must fail as typed preflight errors")
           (catch :default error (is (= "platform_payload_shape_invalid" (:code (ex-data error)))))))
    (is (thrown? js/Error (request/json-plan asset (dissoc (discord-input "create_message" {}) :body))))
    (is (thrown? js/Error (request/json-plan asset (discord-input "create_thread_from_message" {"name" "x"})))))
  (is (payload/basic-shape? {} "test" {"type" "boolean" "enum" [false]} false 0))
  (is (not (payload/basic-shape? {} "test" {"type" "object" "additionalProperties" false} {"extra" true} 0))))

(deftest ^:async real-repository-batches-and-forum-starters-are-expanded
  (let [asset (await (pinned-catalog))
        input {:platform "bluesky" :operation "com.atproto.repo.applyWrites"
               :body {"repo" "did:plc:own" "validate" false
                      "writes" [{"$type" "com.atproto.repo.applyWrites#create" "collection" "app.bsky.feed.post"
                                 "value" {"$type" "app.bsky.feed.post" "text" "hello" "createdAt" "2026-10-27T00:00:00Z"}}
                                {"$type" "com.atproto.repo.applyWrites#delete" "collection" "app.bsky.graph.list" "rkey" "own-list"}]}}
        planned (request/json-plan asset input)
        forum (request/json-plan asset (discord-input "create_thread" {"name" "A room for sketches" "message" {"content" "First sketch"}}))]
    (is (= [:publish :delete] (mapv :effect (get-in planned [:effects :actions]))))
    (is (= "did:plc:own" (get-in planned [:plan :account-repository])))
    (is (= (:body input) (get-in planned [:plan :body])))
    (is (= [:thread :publish] (mapv :effect (get-in forum [:effects :actions]))))
    (is (thrown? js/Error (request/json-plan asset (assoc-in input [:body "writes" 0 "$type"] "example.other#create"))))
    (is (thrown? js/Error (request/json-plan asset (assoc-in input [:body "writes" 0 "collection"] "unknown.record"))))))

(deftest ^:async credential-binary-and-unknown-operations-cannot-become-json-requests
  (let [asset (await (pinned-catalog))]
    (doseq [input [{:platform "bluesky" :operation "com.atproto.server.getServiceAuth"}
                   {:platform "bluesky" :operation "com.atproto.repo.importRepo" :body {}}
                   {:platform "bluesky" :operation "com.atproto.repo.uploadBlob" :body {}}
                   {:platform "bluesky" :operation "app.bsky.feed.post" :body {}}
                   {:platform "discord" :operation "future_unknown" :body {}}
                   (discord-input "create_message" {"nested" {"accessToken" "private"}})]]
      (is (thrown? js/Error (request/json-plan asset input))))
    (is (thrown? js/Error (request/json-plan asset {:platform "discord" :operation "add_guild_member"
                                                   :parameters [{:name "guild_id" :value "1"} {:name "user_id" :value "2"}]
                                                   :body {"access_token" "delegated"}})))))

(deftest ^:async post-read-and-empty-write-plans-follow-pinned-semantics
  (let [asset (await (pinned-catalog))
        read (request/json-plan asset {:platform "bluesky" :operation "tools.ozone.safelink.queryRules"
                                       :body {"limit" 3 "urls" ["example.test"]}})
        reaction (request/json-plan asset {:platform "discord" :operation "add_my_message_reaction"
                                           :parameters [{:name "channel_id" :value "123"} {:name "message_id" :value "456"}
                                                        {:name "emoji_name" :value "🎵"}]})]
    (is (= :read (get-in read [:effects :actions 0 :effect])))
    (is (= "POST" (get-in read [:plan :method])))
    (is (= [204] (get-in reaction [:plan :empty-success-statuses])))
    (is (not (contains? (:plan reaction) :body)))))

(deftest ^:async json-transport-preserves-method-body-and-never-retries
  (let [plan (:plan (request/json-plan (await (pinned-catalog)) (discord-input "create_message" poll)))
        calls (atom [])
        fetcher (fn [url options]
                  (swap! calls conj {:url url :method (aget options "method")
                                     :redirect (aget options "redirect") :body (aget options "body")
                                     :headers (js->clj (aget options "headers"))})
                  (js/Promise.resolve
                   (js/Response. (str "{\"id\":\"789\",\"token\":\"withheld\",\"echo\":" (aget options "body") "}")
                                 #js {:status 200 :headers #js {"content-type" "application/json"}})))
        result (await (http/execute-json! plan {:token "test-owned-token"} {:fetch-fn fetcher}))]
    (is (= 1 (count @calls)))
    (is (= "https://discord.com/api/v10/channels/123/messages" (:url (first @calls))))
    (is (= "POST" (:method (first @calls))))
    (is (= "error" (:redirect (first @calls))))
    (is (= poll (get-in result [:body "echo"])) "Actual wire body survives both HTTP encoding and response decoding")
    (let [wire (.parse js/JSON (:body (first @calls)))]
      (is (true? (aget wire "future_data" "__proto__" "retained")))
      (is (= "also retained" (aget wire "future_data" "constructor" "prototype"))))
    (is (= "application/json" (get-in (first @calls) [:headers "Content-Type"])))
    (is (= "[credential withheld]" (get-in result [:body "token"])))
    (is (false? (:empty-body? result)))))

(deftest ^:async empty-204-is-a-response-not-an-invented-message-confirmation
  (let [plan (:plan (request/json-plan (await (pinned-catalog))
                                     {:platform "discord" :operation "delete_message"
                                      :parameters [{:name "channel_id" :value "123"} {:name "message_id" :value "456"}]}))
        calls (atom [])
        result (await (http/execute-json! plan {:token "fake"}
                                          {:fetch-fn (fn [_ options]
                                                       (swap! calls conj options)
                                                       (js/Promise.resolve (js/Response. nil #js {:status 204})))}))]
    (is (= "DELETE" (aget (first @calls) "method")))
    (is (not (.hasOwn js/Object (first @calls) "body")))
    (is (:ok result))
    (is (= 204 (:status result)))
    (is (:empty-body? result))
    (is (nil? (:body result)))
    (is (nil? (:messageId result)))))

(deftest ^:async ambiguous-and-definite-http-failures-are-never-replayed
  (let [plan (:plan (request/json-plan (await (pinned-catalog)) (discord-input "create_message" poll)))]
    (doseq [mode [:rate-limit :permission :malformed :empty :timeout]]
      (let [calls (atom 0)
            fetcher (fn [& _]
                      (swap! calls inc)
                      (case mode
                        :rate-limit (js/Promise.resolve (json-response {"code" 429} 429))
                        :permission (js/Promise.resolve (json-response {"code" 50013} 403))
                        :malformed (js/Promise.resolve (js/Response. "not JSON"))
                        :empty (js/Promise.resolve (js/Response. nil))
                        :timeout (js/Promise.reject (js/Error. "private raw error"))))]
        (try
          (let [result (await (http/execute-json! plan {:token "fake"} {:fetch-fn fetcher}))]
            (is (contains? #{403 429} (:status result)))
            (is (not (:ok result))))
          (catch :default error
            (is (contains? #{"platform_request_unavailable" "platform_response_not_json"} (:code (ex-data error))))
            (is (not= "private raw error" (ex-message error)))))
        (is (= 1 @calls))))))

(deftest ^:async authenticated-repository-owner-and-pds-are-resolved-before-mutation
  (let [plan (:plan (request/json-plan (await (pinned-catalog))
                                     {:platform "bluesky" :operation "com.atproto.repo.createRecord"
                                      :body {"repo" "did:plc:foreign" "collection" "app.bsky.graph.list"
                                             "record" {"$type" "app.bsky.graph.list" "name" "A list" "purpose" "app.bsky.graph.defs#curatelist"
                                                       "createdAt" "2026-10-27T00:00:00Z"}}}))
        calls (atom [])
        fetcher (fn [url options]
                  (swap! calls conj [url (aget options "method")])
                  (js/Promise.resolve
                   (json-response (if (= url "https://bsky.social/xrpc/com.atproto.server.createSession")
                                    {"accessJwt" "fake-jwt" "did" "did:plc:own" "handle" "own.test"
                                     "didDoc" {"id" "did:plc:own" "service" [{"id" "#atproto_pds" "serviceEndpoint" "https://own-pds.test"}]}}
                                    {"uri" "at://did:plc:own/app.bsky.graph.list/test" "cid" "real-response-cid"}) 200)))
        auth {:identifier "own.test" :password "fake-password" :pds "https://bsky.social" :scope-key "json-request-owner-test"}]
    (try (await (http/execute-json! plan auth {:fetch-fn fetcher :cache? false}))
         (is false "Foreign repo must not reach the operation")
         (catch :default error (is (= "platform_repository_not_owned" (:code (ex-data error))))))
    (is (= 1 (count @calls)) "Only account authentication occurred; no foreign repository write")
    (try (await (http/execute-json! (assoc plan :account-repository "did:plc:own") auth {:fetch-fn fetcher :cache? false}))
         (is false "A forged derived ownership field cannot hide the actual foreign body.repo")
         (catch :default error (is (= "platform_repository_not_owned" (:code (ex-data error))))))
    (is (= 2 (count @calls)) "Neither ownership refusal attempted a repository mutation")
    (doseq [repo ["did:plc:own" "OWN.TEST"]]
      (let [owned (-> plan (assoc :account-repository repo) (assoc-in [:body "repo"] repo))
            result (await (http/execute-json! owned auth {:fetch-fn fetcher :cache? false}))]
        (is (:ok result))
        (is (= "https://own-pds.test/xrpc/com.atproto.repo.createRecord" (ffirst (reverse @calls))))))))

(deftest ^:async invalid-and-oversized-json-never-reach-authentication
  (let [plan (:plan (request/json-plan (await (pinned-catalog)) (discord-input "create_message" poll)))
        calls (atom 0) fetcher (fn [& _] (swap! calls inc))]
    (doseq [input [(assoc plan :url "https://elsewhere.test") (assoc plan :method "CONNECT")]]
      (try (await (http/execute-json! input {:token "fake"} {:fetch-fn fetcher}))
           (is false "Invalid plan should fail")
           (catch :default error (is (= "platform_json_request_invalid" (:code (ex-data error)))))))
    (try (await (http/execute-json! plan {:token "fake"} {:fetch-fn fetcher :max-request-bytes 10}))
         (is false "Oversized payload should fail")
         (catch :default error (is (= "platform_request_too_large" (:code (ex-data error))))))
    (is (zero? @calls))))
