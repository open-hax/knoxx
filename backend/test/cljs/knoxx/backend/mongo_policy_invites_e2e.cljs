(ns knoxx.backend.mongo-policy-invites-e2e
  "Opt-in native Mongo redemption proof, outside ordinary -test discovery."
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.extern.clio-store-fixture :as disk]
            [knoxx.backend.extern.mongo-invite-fixture :as fixture]
            [knoxx.backend.extern.mongo-policy-invites :as admission]
            [knoxx.backend.extern.mongo-run-native-fixture :as native]
            [knoxx.backend.infra.db.policy.connection :as connection]
            [knoxx.backend.infra.db.policy.invites :as invites]
            [knoxx.backend.infra.db.policy.users :as users]))

(defn- ^:async refusal [operation]
  (try (await (operation)) nil (catch :default error (ex-data error))))

(deftest ^:async failed-provisioning-leaves-the-invite-retryable
  (let [server (await (native/open!)) db (:db server) calls (atom 0)]
    (try
      (await (fixture/seed! db "known-failure" (fixture/native-expiry)))
      (with-redefs [connection/db! (fn [] db)
                    users/create-user! (fn [_ _ _ _]
                                         (swap! calls inc)
                                         (throw (ex-info "Provisioning failed" {:status 503 :code "provisioning_failed"})))]
        (is (= "provisioning_failed"
               (:code (await (refusal #(invites/redeem-invite! nil "known-failure" "alice@example.test"))))))
        (is (= 1 @calls))
        (is (= "pending" (:status (await (fixture/row db "known-failure"))))))
      (with-redefs [connection/db! (fn [] db)
                    users/create-user! (fn [_ _ _ _] (swap! calls inc) {:ok true})]
        (is (= "redeemed" (get-in (await (invites/redeem-invite! nil "known-failure" "alice@example.test")) [:invite :status])))
        (is (= 2 @calls)))
      (finally (await (native/close! server))))))

(deftest ^:async successful-portable-expiry-redemption-is-single-use
  (let [server (await (native/open!)) db (:db server) calls (atom 0)]
    (try
      (await (fixture/seed! db "portable" "2099-01-01T00:00:00.000Z"))
      (with-redefs [connection/db! (fn [] db)
                    users/create-user! (fn [_ _ _ _] (swap! calls inc) {:ok true})]
        (is (= "redeemed" (get-in (await (invites/redeem-invite! nil "portable" "alice@example.test")) [:invite :status])))
        (is (= 400 (:status (await (refusal #(invites/redeem-invite! nil "portable" "alice@example.test"))))))
        (is (= 1 @calls))
        (is (nil? (:redemption_claim_id (await (fixture/row db "portable"))))))
      (finally (await (native/close! server))))))

(deftest ^:async concurrent-redemption-cannot-provision-the-same-invite-twice
  (let [server (await (native/open!)) db (:db server)
        calls (atom 0) entered (disk/deferred) finish (disk/deferred)]
    (try
      (await (fixture/seed! db "concurrent" (fixture/native-expiry)))
      (with-redefs [connection/db! (fn [] db)
                    users/create-user! (^:async fn [_ _ _ _]
                                         (swap! calls inc)
                                         ((:resolve! entered) true)
                                         (await (:promise finish)))]
        (let [first-attempt (invites/redeem-invite! nil "concurrent" "alice@example.test")]
          (try
            (await (:promise entered))
            (is (= "invite_redemption_unconfirmed"
                   (:code (await (refusal #(invites/redeem-invite! nil "concurrent" "alice@example.test"))))))
            (is (= 1 @calls))
            (finally ((:resolve! finish) {:ok true})))
          (is (= "redeemed" (get-in (await first-attempt) [:invite :status])))))
      (finally (await (native/close! server))))))

(deftest ^:async abandoned-claim-survives-process-restart-and-refuses-replay
  (let [server (atom (await (native/open!))) calls (atom 0)]
    (try
      (await (fixture/seed! (:db @server) "abandoned" (fixture/native-expiry)))
      (let [claim (await (admission/reserve! (:db @server) "abandoned" "alice@example.test"))]
        (reset! server (await (native/restart! @server)))
        (with-redefs [connection/db! (fn [] (:db @server))
                      users/create-user! (fn [_ _ _ _] (swap! calls inc))]
          (is (= "invite_redemption_unconfirmed"
                 (:code (await (refusal #(invites/redeem-invite! nil "abandoned" "alice@example.test")))))))
        (is (zero? @calls))
        (is (= (:redemption_claim_id claim) (:redemption_claim_id (await (fixture/row (:db @server) "abandoned")))))
        (is (false? (await (admission/release! (:db @server) "abandoned" "wrong-owner"))))
        (is (= 409 (:status (await (refusal #(admission/finalize! (:db @server) "abandoned" "wrong-owner"))))))
        (is (true? (await (admission/release! (:db @server) "abandoned" (:redemption_claim_id claim)))))
        (is (= "pending" (:status (await (fixture/row (:db @server) "abandoned"))))))
      (finally (await (native/close! @server))))))

(deftest ^:async invalid-email-and-expiry-admit-no-provisioning
  (let [server (await (native/open!)) db (:db server) calls (atom 0)]
    (try
      (await (fixture/seed! db "foreign" (fixture/native-expiry)))
      (await (fixture/seed! db "expired" "2000-01-01T00:00:00.000Z"))
      (await (fixture/seed! db "malformed" "2099-02-31T00:00:00.000Z"))
      (with-redefs [connection/db! (fn [] db)
                    users/create-user! (fn [_ _ _ _] (swap! calls inc))]
        (is (= 403 (:status (await (refusal #(invites/redeem-invite! nil "foreign" "outsider@example.test"))))))
        (is (= "invite_expired" (:code (await (refusal #(invites/redeem-invite! nil "expired" "alice@example.test"))))))
        (is (= "invite_expiry_invalid" (:code (await (refusal #(invites/redeem-invite! nil "malformed" "alice@example.test"))))))
        (is (zero? @calls))
        (doseq [code ["foreign" "expired" "malformed"]]
          (is (= "pending" (:status (await (fixture/row db code)))))))
      (finally (await (native/close! server))))))
