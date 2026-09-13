(ns knoxx.backend.infra.stores.clio-mailbox-store-test
  (:require [clio.extern.js.fs :as fs]
            [cljs.test :refer [deftest is]]
            [knoxx.backend.extern.clio-store-fixture :as disk]
            [knoxx.backend.extern.promise :as promise]
            [knoxx.backend.infra.clio-application-store :as ledger]
            [knoxx.backend.infra.stores.clio-mailbox-store :as clio]
            [knoxx.backend.shape.mailbox-store :as store]))

(def sender {:org-id "org-a" :actor-id "sender" :admin? false})
(def receiver (assoc sender :actor-id "receiver"))
(def admin (assoc sender :admin? true))
(def message {:mailbox/id "message-one" :mailbox/kind "actor-message"
              :mailbox/source {:actor-id "sender"} :mailbox/target {:kind "actor" :actor-id "receiver"}
              :mailbox/delivery {:mode "inbox-only"} :mailbox/content "The canonical full message"
              :mailbox/preview "The canonical" :mailbox/content-ref {:mailbox-id "message-one"} :mailbox/metadata {}})
(defn- ^:async refused [operation]
  (try (await (operation)) nil (catch :default error (ex-data error))))
(defn- ^:async fixture! [operation]
  (let [directory (disk/temp-directory!) clock (atom "2026-09-12T12:00:00.000Z")]
    (try
      (let [options {:directory directory :clock! #(deref clock)}]
        (await (operation (clio/open! options) options clock)))
      (catch :default error (is false (str "Unexpected durable mailbox failure: " error)))
      (finally (fs/remove-tree! directory)))))
(defn- claim-one! [provider operation-id]
  (store/claim-deliveries! provider sender {:mailbox-id "message-one" :operation-id operation-id}))

(deftest ^:async tenant-message-facts-survive-restart-and-list-never-includes-full-content
  (await (fixture!
    (^:async fn [provider options _clock]
      (is (true? (:mailbox/durable? (await (store/create-entry! provider sender message)))))
      (is (true? (:durable? (await (store/list-entries provider receiver {})))))
      (let [restarted (clio/open! options)]
        (is (= (:mailbox/content message) (:mailbox/content (await (store/read-message restarted receiver "message-one")))))
        (is (nil? (:mailbox/content (first (:entries (await (store/list-entries restarted receiver {})))))))
        (is (= 404 (:status (await (refused #(store/read-message restarted (assoc receiver :org-id "org-b") "message-one"))))))
        (is (= 404 (:status (await (refused #(store/read-message restarted (assoc receiver :actor-id "outsider") "message-one"))))))
        (is (= "mailbox_identity_conflict" (:code (await (refused #(store/create-entry! restarted sender (assoc message :mailbox/content "changed")))))))
        (is (= "pending" (:mailbox/status (await (store/create-entry! restarted sender message)))))
        (is (= 1 (count (ledger/history (:ledger restarted))))))))))

(deftest ^:async competing-claims-and-retries-retain-one-fenced-attempt
  (await (fixture!
    (^:async fn [provider options _clock]
      (await (store/create-entry! provider sender message))
      (let [results (await (promise/all-vec [(claim-one! provider "claim-a") (claim-one! (clio/open! options) "claim-b")]))
            winner (first (mapcat :entries results))
            claim-id (get-in winner [:mailbox/delivery :claim-id])]
        (is (= 1 (count (mapcat :entries results))))
        (is (= 1 (get-in winner [:mailbox/delivery :attempts])))
        (is (= "delivered" (:mailbox/status (await (store/mark-delivery! provider sender "message-one" "delivered" {:claim-id claim-id})))))
        (is (= "delivered" (:mailbox/status (await (store/mark-delivery! provider sender "message-one" "delivered" {:claim-id claim-id})))))
        (is (= 409 (:status (await (refused #(store/mark-delivery! provider sender "message-one" "failed" {:claim-id claim-id :error "late"}))))))
        (is (= 3 (count (ledger/history (:ledger provider))))))))))

(deftest ^:async expired-lease-does-not-authorize-an-old-worker-after-replacement
  (await (fixture!
    (^:async fn [provider _options clock]
      (await (store/create-entry! provider sender message))
      (let [first-claim (first (:entries (await (claim-one! provider "first"))))]
        (reset! clock "2026-09-12T12:00:31.000Z")
        (let [second-claim (first (:entries (await (claim-one! provider "second"))))]
          (is (= 2 (get-in second-claim [:mailbox/delivery :attempts])))
          (is (= "mailbox_claim_stale"
                 (:code (await (refused #(store/mark-delivery! provider sender "message-one" "delivered"
                                                          {:claim-id (get-in first-claim [:mailbox/delivery :claim-id])}))))))
          (is (= "delivered" (:mailbox/status (await (store/mark-delivery! provider sender "message-one" "delivered"
                                                                                          {:claim-id (get-in second-claim [:mailbox/delivery :claim-id])})))))))))))

(deftest ^:async recipient-acknowledgement-fences-late-delivery-and-remains-idempotent
  (await (fixture!
    (^:async fn [provider _options _clock]
      (await (store/create-entry! provider sender message))
      (let [claimed (first (:entries (await (claim-one! provider "ack-race"))))]
        (is (= 404 (:status (await (refused #(store/acknowledge-entry! provider sender "message-one"))))))
        (let [acknowledged (await (store/acknowledge-entry! provider receiver "message-one"))]
          (is (= "acknowledged" (:mailbox/status acknowledged)))
          (is (= acknowledged (await (store/acknowledge-entry! provider receiver "message-one")))))
        (is (= "mailbox_claim_stale"
               (:code (await (refused #(store/mark-delivery! provider sender "message-one" "delivered"
                                                          {:claim-id (get-in claimed [:mailbox/delivery :claim-id])})))))))))))

(deftest ^:async scoped-route-expiry-and-old-session-unregister-cannot-erase-a-new-binding
  (await (fixture!
    (^:async fn [provider options clock]
      (await (store/register-route! provider sender {:actor-id "sender" :conversation-id "old" :ttl-seconds 2}))
      (await (store/register-route! provider sender {:actor-id "sender" :conversation-id "new" :ttl-seconds 2}))
      (is (= 0 (:retired (await (store/unregister-route! provider sender "old")))))
      (is (= "new" (:conversation-id (await (store/resolve-route (clio/open! options) receiver "sender")))))
      (is (nil? (await (store/resolve-route provider (assoc receiver :org-id "org-b") "sender"))))
      (is (= 403 (:status (await (refused #(store/register-route! provider receiver {:actor-id "sender" :conversation-id "stolen"}))))))
      (reset! clock "2026-09-12T12:00:02.000Z")
      (is (nil? (await (store/resolve-route provider receiver "sender"))))))))

(deftest ^:async expiry-and-empty-scope-validation-fail-closed-without-deleting-history
  (await (fixture!
    (^:async fn [provider options clock]
      (is (= 400 (:status (await (refused #(store/list-entries provider {} {}))))))
      (await (store/create-entry! provider sender (assoc message :mailbox/expires-at "2026-09-12T12:00:01.000Z")))
      (reset! clock "2026-09-12T12:00:01.000Z")
      (is (= "expired" (:mailbox/status (await (store/read-message provider receiver "message-one")))))
      (is (empty? (:entries (await (claim-one! provider "too-late")))))
      (is (= 409 (:status (await (refused #(store/acknowledge-entry! provider receiver "message-one"))))))
      (is (= 1 (count (ledger/history (:ledger provider)))))
      (is (= "mailbox_clock_invalid" (:code (await (refused #(clio/open! (assoc options :clock! (constantly "bad"))))))))))))

(deftest ^:async stable-claim-operation-retries-do-not-create-another-attempt
  (await (fixture!
    (^:async fn [provider options clock]
      (await (store/create-entry! provider sender message))
      (let [first-result (await (claim-one! provider "operation-one"))]
        (is (true? (:durable? first-result)))
        (reset! clock "2026-09-12T12:01:00.000Z")
        (is (= first-result (await (claim-one! (clio/open! options) "operation-one"))))
        (is (= 2 (count (ledger/history (:ledger provider)))))
        (is (= "mailbox_operation_conflict"
               (:code (await (refused #(store/claim-deliveries! provider sender
                                                               {:mailbox-id "message-one" :operation-id "operation-one" :limit 2})))))))))))
