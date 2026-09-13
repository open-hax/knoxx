(ns knoxx.frontend.pages.mail.api-contract-test
  "Transport contracts preserve human/agent command parity without importing authority."
  (:require [cljs.test :as t]
            [knoxx.frontend.lib.api :as http]
            [knoxx.frontend.pages.mail.api :as api]
            [knoxx.frontend.pages.mail.logic :as logic]))

(t/deftest ^:async compose-sends-only-finite-tool-arguments
  (let [requests (atom [])]
    (with-redefs [http/request (fn ([_] (throw (js/Error. "Unexpected read"))) ([url options]
                                (swap! requests conj [url options])
                                (js/Promise.resolve {:ok true :entry {:id "message-1" :status "delivered" :durable true}})))]
      (await (api/send-message {:operation_id "retry-1" :target "actor:recipient" :mode "inbox-only"
                               :content "A complete message" :org_id "forged" :actorId "forged" :lineage {:actor-id "forged"}}))
      (t/is (= [["/api/actors/messages" {:method "POST" :body {:operation_id "retry-1" :target "actor:recipient"
                                                                             :mode "inbox-only" :content "A complete message"}}]] @requests)))))

(t/deftest ^:async full-message-refuses-a-preview-or-different-resource
  (doseq [entry [{:id "message-1" :preview "Truncated"} {:id "other" :content "Another actor's content"}]]
    (with-redefs [http/request (fn ([_] (js/Promise.resolve {:ok true :entry entry}))
                                  ([_ _] (throw (js/Error. "Unexpected mutation"))))]
      (try (await (api/read-entry "message-1")) (t/is false "Invalid resource must be refused")
           (catch :default error (t/is (= "The server did not return the requested full message" (.-message error))))))))

(t/deftest successful-http-without-delivery-is-not-a-confirmed-send
  (doseq [status ["pending" "failed" nil]]
    (t/is (thrown? js/Error (logic/assert-send! {:ok true :entry {:id "message-1" :status status}}))))
  (t/is (false? (:durable (logic/normalize-list-response {:durable false} "inbox"))))
  (t/is (false? (:durable (logic/normalize-entry {:id "message-1" :durable false}))))
  (t/is (nil? (:durable (logic/normalize-list-response {} "inbox")))))

(t/deftest editing-intent-rotates-operation-identity
  (let [first-command (logic/next-operation nil {:content "Original"} "first")]
    (t/is (= first-command (logic/next-operation first-command {:content "Original"} "unused")))
    (t/is (= {:id "changed" :payload {:content "Corrected"}}
             (logic/next-operation first-command {:content "Corrected"} "changed")))))

(t/deftest mailbox-stream-reconciles-reconnects-and-removes-all-listeners
  (let [original (.-EventSource js/globalThis) listeners (atom {}) status (atom []) changed (atom 0) closed (atom false)]
    (set! (.-EventSource js/globalThis)
          (fn [url options]
            (t/is (= "/api/actors/mailbox/events/stream" url)) (t/is (true? (.-withCredentials options)))
            #js {:addEventListener (fn [kind callback] (swap! listeners assoc kind callback))
                 :removeEventListener (fn [kind _] (swap! listeners dissoc kind)) :close #(reset! closed true)}))
    (try
      (let [close! (api/subscribe! #(swap! changed inc) #(swap! status conj %))]
        ((get @listeners "open") nil) ((get @listeners "mailbox-changed") #js {:data "{}"})
        ((get @listeners "error") nil)
        (t/is (= 2 @changed)) (t/is (= ["Live" "Reconnecting; refresh is available"] @status))
        (close!) (t/is @closed) (t/is (empty? @listeners)))
      (finally (set! (.-EventSource js/globalThis) original)))))
