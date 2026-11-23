(ns knoxx.backend.platform-action-admission-test
  (:require [cljs.test :refer [deftest is]]
            [cljs.reader :as reader]
            [knoxx.backend.domain.action.run-state :as run-state]
            [knoxx.backend.domain.voice.turn-control :as turn-control]
            [knoxx.backend.domain.discord.rest-client :as discord-rest]
            [knoxx.backend.domain.discord.tools :as discord-tools]
            [knoxx.backend.extern.fetch :as xfetch]
            [knoxx.backend.infra.agent.session :as session]
            [knoxx.backend.law.platform-action :as law]
            [knoxx.backend.social-delivery-test :as evidence]))

(defn- ^:async in-run!
  [f]
  (with-redefs [run-state/runs* (atom {"r" {:run_id "r" :actor_id "a" :conversation_id "c"
                                          :status "running" :tool_receipts []}})
                turn-control/active-turn (constantly {:run_id "r"})]
    (await (f))))

(defn- wrapped-tools
  ([execute] (wrapped-tools execute {}))
  ([execute extra-context]
   (let [tools #js [#js {:name "discord_send" :execute execute}
                   #js {:name "discord_publish" :execute execute}]]
     (session/wrap-custom-tools-with-agent-context!
      tools (merge {:conversation-id "c" :agent-spec {:actor-id "a"}} extra-context))
     (vec (array-seq tools)))))

(defn- ^:async call!
  [tool params]
  (await ((aget tool "execute") "call" (clj->js params))))

(defn- ^:async failure!
  [tool params]
  (try (await (call! tool params)) nil (catch :default error error)))

(deftest registry-data-and-executable-boundaries
  (is (= law/registry (reader/read-string (pr-str law/registry))))
  (is (thrown? js/Error (law/assert-valid! law/Intent {:platform "discord" :effect :publish :target "" :arguments {}})))
  (is (thrown? js/Error (law/assert-valid! law/Outcome {:status :confirmed :invented true}))))

(deftest ^:async actual-wrapper-shares-default-admission-across-aliases
  (await
   (in-run!
    (^:async fn []
      (let [calls (atom 0)
            value #js {:content #js [] :details #js {:sent true :messageId "sent"}}
            [send publish] (wrapped-tools (fn [& _] (swap! calls inc) value))
            result (await (call! send {:channel_id "home" :text " hello "}))
            duplicate (await (failure! publish {:channel_id "home" :content "hello"}))]
        (is (= 1 @calls))
        (is (= :duplicate-action (:reason (ex-data duplicate))))
        (is (= "confirmed" (aget (aget (aget result "details") "actionOutcome") "status")))
        (is (undefined? (aget (aget value "details") "actionOutcome")) "original result was not mutated"))))))

(deftest ^:async explicit-reference-refusal-can-be-repaired-before-permission-denial
  (await
   (in-run!
    (^:async fn []
      (let [calls (atom 0)
            [send publish] (wrapped-tools
                            (fn [& _]
                              (let [n (swap! calls inc)]
                                (throw (ex-info "Provider refused"
                                                {:phase :platform :http-status (if (= n 1) 400 403)
                                                 :platform-code (if (= n 1) 50035 50013)})))))
            first (await (failure! send {:channel_id "home" :text "hello" :reply_to "invented"}))
            second (await (failure! send {:channel_id "home" :text "hello"}))
            third (await (failure! publish {:channel_id "home" :content "changed"}))]
        (is (= 400 (:http-status (ex-data first))))
        (is (= :refused (get-in (ex-data first) [:action-outcome :status])))
        (is (= 403 (:http-status (ex-data second))))
        (is (= :unresolved-audience (:reason (ex-data third))))
        (is (= 2 @calls) "neither a new body nor an alias repeats the denied channel write"))))))

(deftest ^:async actual-wrapper-does-not-enter-parallel-audience-writes
  (await
   (in-run!
    (^:async fn []
      (let [calls (atom 0) release* (atom nil)
            [send publish] (wrapped-tools (fn [& _] (swap! calls inc)
                                            (js/Promise. (fn [resolve _] (reset! release* resolve)))))
            pending (call! send {:channel_id "home" :text "first"})]
        (try
          (let [refused (await (failure! publish {:channel_id "home" :content "second"}))]
            (is (= :action-in-flight (:reason (ex-data refused))))
            (is (= 1 @calls)))
          (finally (@release* #js {:content #js [] :details #js {:sent true :messageId "first"}})))
        (await pending))))))

(deftest ^:async optional-source-obligations-are-shared-and-precede-reservation
  (await
   (in-run!
    (^:async fn []
      (let [calls (atom 0)
            [send publish] (wrapped-tools
                            (fn [& _] (swap! calls inc)
                              #js {:content #js [] :details #js {:sent true :messageId "one"}})
                            {:delivery-policy evidence/policy})
            refused (await (failure! publish {:channel_id "home" :content (:text evidence/params)}))]
        (is (= "delivery_evidence_required" (:code (ex-data refused))))
        (is (zero? @calls))
        (swap! run-state/runs* assoc-in ["r" :tool_receipts] evidence/complete-evidence)
        (await (call! publish {:channel_id "home" :content (:text evidence/params)}))
        (is (= 1 @calls))
        (let [blocked (await (failure! send evidence/params))]
          (is (= "delivery_evidence_required" (:code (ex-data blocked))))
          (is (= 1 @calls) "native publish and send share the trusted one-attempt evidence guard")))))))

(deftest ^:async rest-client-preserves-typed-platform-refusal
  (with-redefs [xfetch/json! (fn [& _] (js/Promise.resolve {:ok false :status 400 :body {:code 50035 :message "Unknown reference"}}))]
    (try
      (await (discord-rest/create-channel-message! (discord-rest/client "fixture-only") "home" {:content "hello"}))
      (is false "the provider response was a refusal")
      (catch :default error
        (is (= {:code "discord_api_error" :phase :platform :platform "discord" :http-status 400 :platform-code 50035}
               (ex-data error)))))))

(deftest ^:async partial-chunks-retain-confirmed-message-identifiers
  (let [calls (atom 0)]
    (with-redefs [discord-rest/create-channel-message-form!
                  (fn [& _]
                    (if (= 1 (swap! calls inc))
                      (js/Promise.resolve {:id "delivered-first"})
                      (js/Promise.reject (ex-info "Second refused" {:http-status 400 :platform-code 50035}))))]
      (try
        (await (discord-tools/post-discord-message-chunks! nil "home" nil [] ["one" "two" "three"]))
        (is false "a later refusal cannot erase the first delivery")
        (catch :default error
          (is (= ["delivered-first"] (:delivered-message-ids (ex-data error))))
          (is (= 400 (:http-status (ex-data error))))
          (is (= 2 @calls) "third chunk was never attempted"))))))

(deftest ^:async attachment-preflight-failure-is-not-a-platform-attempt
  (let [platform-calls (atom 0)]
    (with-redefs [discord-tools/resolve-discord-upload-attachment!
                  (fn [& _] (js/Promise.reject (js/Error. "Missing local fixture")))
                  discord-rest/create-channel-message-form!
                  (fn [& _] (swap! platform-calls inc) (js/Promise.resolve {:id "should-not-exist"}))]
      (try
        (await (discord-tools/discord-send-message! nil {} "home" "hello" nil ["missing-local-fixture"]))
        (is false "missing attachment must fail before the Discord write")
        (catch :default error
          (is (= :preflight (:phase (ex-data error))))
          (is (= [] (:delivered-message-ids (ex-data error))))
          (is (zero? @platform-calls)))))))
