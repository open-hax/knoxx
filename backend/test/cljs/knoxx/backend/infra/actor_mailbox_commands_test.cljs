(ns knoxx.backend.infra.actor-mailbox-commands-test
  (:require [clio.extern.js.fs :as fs] [cljs.test :refer [deftest is]]
            [knoxx.backend.extern.clio-store-fixture :as disk] [knoxx.backend.extern.promise :as promise]
            [knoxx.backend.infra.actor-mailbox :as mailbox] [knoxx.backend.infra.actor-mailbox-commands :as commands]
            [knoxx.backend.infra.auth.authz :as authz] [knoxx.backend.infra.clio-application-store :as ledger]
            [knoxx.backend.infra.mailbox-delivery :as delivery] [knoxx.backend.infra.mailbox-delivery-registry :as deliveries]
            [knoxx.backend.infra.stores.clio-mailbox-store :as clio] [knoxx.backend.infra.stores.mailbox-store :as registry]
            [knoxx.backend.shape.mailbox-delivery :as effect] [knoxx.backend.shape.mailbox-store :as store]))
(def sender {:org-id "org-a" :user-id "user-a" :membership-id "member-a" :actor-binding "sender"
             :permissions ["agent.chat.use"] :tool-policies [{:tool-id "actors.send-message" :effect "allow"}]})
(def receiver (assoc sender :user-id "user-b" :membership-id "member-b" :actor-binding "receiver"))
(def request {:operation-id "message-one" :target "actor:receiver" :mode "inbox-only"
              :content (str (apply str (repeat 300 "x")) " canonical ending")})
(def instant "2026-09-12T12:00:00.000Z")
(defn- ^:async attempt [f] (try {:result (await (f))} (catch :default error {:error (ex-data error)})))
(defn- ^:async fixture! [f]
  (let [directory (disk/temp-directory!) previous @registry/provider* old-delivery @deliveries/provider*
        options {:directory directory :clock! (constantly instant)} provider (clio/open! options)]
    (try (registry/install! provider) (deliveries/install! (delivery/provider))
         (with-redefs [authz/current-context! (fn [_ context] context)] (await (f provider options)))
         (catch :default error (is false (str "Unexpected mailbox fixture failure: " error)))
         (finally (registry/install! previous) (deliveries/install! old-delivery) (fs/remove-tree! directory)))))

(deftest ^:async administrator-mail-capabilities-match-the-authorized-command
  (await
   (fixture!
    (^:async fn [_provider _options]
      (let [administrator (-> sender (assoc :role-slugs ["system_admin"])
                              (dissoc :permissions :tool-policies))
            capabilities (commands/interface-capabilities administrator)
            result (await (commands/send! {} {} administrator request))]
        (is (= "delivered" (get-in result [:entry :mailbox/status])))
        (is (true? (:send capabilities)))
        (is (= #{"inbox-only" "follow-up" "steer" "event"} (set (:modes capabilities))))
        (is (true? (:acknowledge capabilities)))
        (let [denied (assoc administrator :tool-policies [{:tool-id "actors.send-message" :effect "deny"}])]
          (is (false? (:send (commands/interface-capabilities denied))))
          (is (empty? (:modes (commands/interface-capabilities denied))))
          (is (= "mailbox_tool_denied"
                 (get-in (await (attempt #(commands/send! {} {} denied
                                                         (assoc request :operation-id "denied-admin"))))
                         [:error :code])))))))))

(deftest ordinary-mail-capabilities-still-require-both-permission-and-tool-policy
  (is (= ["inbox-only"] (:modes (commands/interface-capabilities sender))))
  (is (false? (:send (commands/interface-capabilities (dissoc sender :permissions)))))
  (is (false? (:send (commands/interface-capabilities (dissoc sender :tool-policies))))))
(deftest ^:async canonical-body-survives-restart-without-leaking-through-list
  (await (fixture! (^:async fn [provider options]
    (let [sent (await (commands/send! {} {} sender request)) inbox (mailbox/context {} receiver)]
      (is (= "delivered" (get-in sent [:entry :mailbox/status])))
      (is (= (:content request) (:mailbox/content (await (mailbox/read-message! inbox "message-one")))))
      (is (nil? (:mailbox/content (first (:entries (await (mailbox/list-entries! inbox {})))))))
      (is (= (:content request) (:mailbox/content (await (store/read-message (clio/open! options) (:scope inbox) "message-one")))))
      (is (= "mailbox_identity_conflict" (get-in (await (attempt #(commands/send! {} {} sender (update request :content str " changed tail")))) [:error :code])))
      (is (= "mailbox_not_found" (get-in (await (attempt #(mailbox/read-message! (mailbox/context {} (assoc receiver :actor-binding "outsider")) "message-one"))) [:error :code])))
      (is (= "mailbox_not_found" (get-in (await (attempt #(mailbox/read-message! (mailbox/context {} (assoc receiver :org-id "org-b")) "message-one"))) [:error :code])))
      (is (= 3 (count (ledger/history (:ledger provider)))))
      (is (true? (:existing (await (commands/send! {} {} sender request)))))
      (is (= 3 (count (ledger/history (:ledger provider))))))))))
(deftest ^:async denial-and-revocation-precede-all-mailbox-writes
  (await (fixture! (^:async fn [provider _options]
    (let [denied (assoc sender :role-slugs ["system_admin"] :tool-policies [{:tool-id "actors.send-message" :effect "deny"}])]
      (is (false? (commands/allowed? denied)))
      (is (= "mailbox_tool_denied" (get-in (await (attempt #(commands/send! {} {} denied request))) [:error :code])))
      (with-redefs [authz/current-context! (fn [_ _] (throw (ex-info "revoked" {:status 401 :code "session_revoked"})))]
        (is (= "session_revoked" (get-in (await (attempt #(commands/send! {} {} sender request))) [:error :code]))))
      (is (empty? (ledger/history (:ledger provider)))))))))
(deftest ^:async concurrent-same-message-does-not-reuse-a-claim-as-an-effect-permit
  (await (fixture! (^:async fn [_provider _options]
    (let [effects (atom 0)]
      (deliveries/install! (reify effect/IMailboxDelivery (deliver! [_ _runtime _config _context _request] (swap! effects inc) {:delivered true})))
      (let [results (await (promise/all-vec [(attempt #(commands/send! {} {} sender request)) (attempt #(commands/send! {} {} sender request))]))]
        (is (= 1 @effects)) (is (some :result results))
        (is (every? #(or (:result %) (= "mailbox_delivery_pending" (get-in % [:error :code]))) results))))))))
(deftest ^:async successful-effect-with-failed-receipt-is-unconfirmed-and-repairable
  (await (fixture! (^:async fn [provider options]
    (deliveries/install! (reify effect/IMailboxDelivery
      (deliver! [_ _runtime _config _context _request]
        (registry/install! (assoc-in provider [:ledger :before-append] (fn [_] (throw (ex-info "disk full" {:code "test_disk_full"})))))
        {:delivered true})))
    (with-redefs [mailbox/mark-delivered! (fn [_ _ _] (throw (ex-info "disk full" {:code "test_disk_full"})))]
      (is (= "mailbox_delivery_unconfirmed" (get-in (await (attempt #(commands/send! {} {} sender request))) [:error :code]))))
    (let [message (await (store/read-message (clio/open! options) {:org-id "org-a" :actor-id "sender" :admin? false} "message-one"))]
      (is (= "pending" (:mailbox/status message))) (is (= (:content request) (:mailbox/content message)))
      (is (nil? (:mailbox/delivered-at message))))))))
(deftest ^:async missing-provider-refuses-instead-of-fabricating-a-send-or-ack
  (await (fixture! (^:async fn [_provider _options]
    (registry/install! nil)
    (is (= "mailbox_provider_unavailable" (get-in (await (attempt #(commands/send! {} {} sender request))) [:error :code])))
    (is (= "mailbox_provider_unavailable" (get-in (await (attempt #(mailbox/context {} receiver))) [:error :code])))))))
(deftest ^:async delivery-permission-is-required-before-admission
  (await (fixture! (^:async fn [provider _options]
    (is (= 403 (get-in (await (attempt #(commands/send! {} {} sender (assoc request :mode "steer")))) [:error :status])))
    (is (empty? (ledger/history (:ledger provider))))))))
(deftest ^:async retry-delivers-the-original-body-after-a-recorded-failure
  (await (fixture! (^:async fn [provider _options]
    (deliveries/install! (reify effect/IMailboxDelivery (deliver! [_ _runtime _config _context _request] (throw (ex-info "offline" {:code "test_offline"})))))
    (is (= "test_offline" (get-in (await (attempt #(commands/send! {} {} sender request))) [:error :code])))
    (let [admin (update sender :permissions conj "org.events.control") received (atom nil)]
      (deliveries/install! (reify effect/IMailboxDelivery (deliver! [_ _runtime _config _context message] (reset! received (:content message)) {:ok true})))
      (let [result (await (commands/retry! {} {} admin {:mailbox-id "message-one"}))]
        (is (= (:content request) @received))
        (is (= "delivered" (get-in result [:deliveries 0 :entry :mailbox/status])))
        (is (= 2 (get-in result [:deliveries 0 :entry :mailbox/delivery :attempts])))))
    (is (= 5 (count (ledger/history (:ledger provider)))))))))

(deftest ^:async revocation-after-claim-blocks-the-effect-and-records-failure
  (await
   (fixture!
    (^:async fn [provider _options]
      (let [refreshes (atom 0) effects (atom 0)]
        (deliveries/install!
         (reify effect/IMailboxDelivery
           (deliver! [_ _runtime _config _context _request] (swap! effects inc))))
        (with-redefs [authz/current-context!
                      (fn [_ context]
                        (if (= 1 (swap! refreshes inc)) context
                            (throw (ex-info "revoked" {:status 401 :code "session_revoked"}))))]
          (is (= "session_revoked"
                 (get-in (await (attempt #(commands/send! {} {} sender request))) [:error :code]))))
        (is (zero? @effects))
        (let [entry (await (store/read-message provider
                                              {:org-id "org-a" :actor-id "sender" :admin? false}
                                              "message-one"))]
          (is (= "failed" (:mailbox/status entry)))
          (is (nil? (:mailbox/delivered-at entry)))))))))

(deftest ^:async acknowledgement-between-claim-and-effect-fences-the-effect
  (await
   (fixture!
    (^:async fn [provider _options]
      (let [refreshes (atom 0) effects (atom 0)]
        (deliveries/install!
         (reify effect/IMailboxDelivery
           (deliver! [_ _runtime _config _context _request] (swap! effects inc))))
        (with-redefs [authz/current-context!
                      (^:async fn [_ context]
                        (when (= 2 (swap! refreshes inc))
                          (await (store/acknowledge-entry!
                                  provider {:org-id "org-a" :actor-id "receiver" :admin? false}
                                  "message-one")))
                        context)]
          (is (= "mailbox_claim_stale"
                 (get-in (await (attempt #(commands/send! {} {} sender request))) [:error :code]))))
        (is (zero? @effects))
        (is (= "acknowledged"
               (:mailbox/status
                (await (store/read-message provider
                                           {:org-id "org-a" :actor-id "receiver" :admin? false}
                                           "message-one"))))))))))

(deftest ^:async exact-request-retry-retains-original-delivery-after-actor-route-changes
  (await
   (fixture!
    (^:async fn [provider options]
      (let [receiver-scope {:org-id "org-a" :actor-id "receiver" :admin? false}
            effects (atom [])]
        (await (store/register-route! provider receiver-scope
                                      {:actor-id "receiver" :conversation-id "original" :session-id "original-session"}))
        (deliveries/install!
         (reify effect/IMailboxDelivery
           (deliver! [_ _runtime _config _context message]
             (swap! effects conj (:target message)) {:delivered true})))
        (let [first-result (await (commands/send! {} {} sender request))]
          (is (= "original" (get-in first-result [:entry :mailbox/target :conversation-id]))))
        (await (store/register-route! provider receiver-scope
                                      {:actor-id "receiver" :conversation-id "new" :session-id "new-session"}))
        (registry/install! (clio/open! options))
        (let [retried (await (commands/send! {} {} sender request))]
          (is (true? (:existing retried)))
          (is (= "original" (get-in retried [:entry :mailbox/target :conversation-id]))))
        (is (= 1 (count @effects)))
        (is (= "mailbox_identity_conflict"
               (get-in (await (attempt #(commands/send! {} {} sender (assoc request :session-id "new-session"))))
                       [:error :code])))
        (let [entries (:entries (await (store/list-entries provider receiver-scope {})))]
          (is (nil? (:mailbox/intent (first entries))))
          (is (nil? (:mailbox/content (first entries))))))))))
