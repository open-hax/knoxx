(ns knoxx.backend.e2e.invite-expiry-e2e
  "Exercise the native invite endpoints over HTTP against an owned Mongo process."
  (:require [clojure.string :as str]
            [cljs.test :refer [deftest is testing]]
            [knoxx.backend.extern.fetch :as fetch]
            [knoxx.backend.extern.mongo-invite-native-fixture :as fixture]))

(def ^:private member-email "invite-member@example.invalid")

(defn- request!
  [origin endpoint payload cookie]
  (fetch/json! fetch/default-client
               {:url (str origin "/api/auth/" endpoint)
                :method "POST"
                :headers (when cookie {"cookie" cookie})
                :json payload}))

(defn- ^:async prove-member-session!
  [origin success]
  (let [cookie (first (str/split (get-in success [:headers "set-cookie"] "") #";" 2))
        url (str origin "/api/auth/context")
        anon (await (fetch/json! fetch/default-client {:url url}))
        {:keys [status body]} (await (fetch/json! fetch/default-client
                                                 {:url url :headers {"cookie" cookie}}))]
    (is (= 401 (:status anon)))
    (is (= 200 status) "The issued cookie authenticates a subsequent request")
    (is (= member-email (get-in body [:user :email])))
    (is (= (get-in success [:body :membership :id]) (get-in body [:membership :id])))
    (is (= (get-in success [:body :org :id]) (get-in body [:org :id])))
    (is (= "active" (get-in body [:membership :status])))
    (is (= ["basic-user"] (:roleSlugs body)))
    (is (false? (:isSystemAdmin body)))))

(defn- ^:async prove-current-invite!
  [{:keys [origin admin-cookie] :as owned}]
  (let [create-payload {:email member-email :send-email false}
        refused (await (request! origin "invite" create-payload nil))
        created (await (request! origin "invite" create-payload admin-cookie))
        code (get-in created [:body :invite :code])
        payload {:code code :email member-email}]
    (is (= 401 (:status refused)) "Invite creation requires a session")
    (is (= 200 (:status created))
        (str "The normal invite endpoint issued an invite: " (get-in created [:body :error])))
    (is (await (fixture/stored-iso-expiry? owned code))
        "The current writer's ISO-string expiry is usable")
    (is (= 403 (:status (await (request! origin "invite/redeem"
                                        (assoc payload :email "other@example.invalid") nil))))
        "An email mismatch cannot consume the invite")
    (let [first-redeem (request! origin "invite/redeem" payload nil)
          second-redeem (request! origin "invite/redeem" payload nil)
          attempts [(await first-redeem) (await second-redeem)]
          success (first (filter #(= 200 (:status %)) attempts))
          refused (first (filter #(= 400 (:status %)) attempts))
          replay (await (request! origin "invite/redeem" payload nil))]
      (is (= [200 400] (sort (map :status attempts))) "One concurrent redemption wins")
      (is (= "redeemed" (get-in success [:body :invite :status])))
      (is (= member-email (get-in success [:body :user :email])))
      (is (str/starts-with? (get-in success [:headers "set-cookie"] "") "knoxx_session=")
          "Knoxx issued the normal session cookie")
      (is (nil? (get-in refused [:headers "set-cookie"])))
      (is (= 400 (:status replay)) "A replay cannot mint another session")
      (is (nil? (get-in replay [:headers "set-cookie"])))
      (await (prove-member-session! origin success)))))

(defn- ^:async prove-stored-expiries!
  [{:keys [origin] :as owned}]
  (doseq [[expiry status] [[:future-date 200] [:expired-date 400] [:expired-string 400]
                           [:database-now 400] [:invalid 400] [:null 400] [:missing 400] [:number 400]]]
    (let [code (name expiry)
          email (str code "@example.invalid")]
      (await (fixture/seed-invite! owned {:code code :email email :expiry expiry}))
      (is (= status (:status (await (request! origin "invite/redeem" {:code code :email email} nil))))
          (str "Stored expiry case: " code)))))

(deftest ^:async native-invite-expiry-test
  (when (fixture/enabled?)
    (testing "Native invite authorization, expiry, session creation and replay"
      (await (fixture/with-fixture!
              (^:async fn [owned]
                (await (prove-current-invite! owned))
                (await (prove-stored-expiries! owned))))))))
