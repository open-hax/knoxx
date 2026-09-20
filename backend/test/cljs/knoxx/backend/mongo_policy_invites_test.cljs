(ns knoxx.backend.mongo-policy-invites-test
  "Failure classification around invite ownership; native CAS is proved separately."
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.extern.mongo-policy-invites :as admission]
            [knoxx.backend.infra.db.policy.connection :as connection]
            [knoxx.backend.infra.db.policy.invites :as invites]
            [knoxx.backend.infra.db.policy.users :as users]))

(def ^:private claim
  {:id "invite-a" :redemption_claim_id "claim-a" :org_id "org-a"
   :email "alice@example.test" :role_slugs ["basic-user"] :status "provisioning"})

(defn- ^:async caught [operation]
  (try (await (operation)) nil (catch :default error error)))

(defn- ^:async with-reservation! [run!]
  (with-redefs [connection/db! (fn [] :fixture-db)
                admission/reserve! (fn [_ code email]
                                     (is (= ["code-a" "alice@example.test"] [code email])) claim)]
    (await (run!))))

(defn- redeem! [] (invites/redeem-invite! nil "code-a" "alice@example.test"))

(deftest ^:async known-provisioning-failure-releases-only-its-owner-and-preserves-the-cause
  (await (with-reservation!
          (^:async fn []
            (let [cause (ex-info "create failed" {:status 503 :code "create_failed"}) released (atom [])]
              (with-redefs [users/create-user! (fn [_ _ _ _] (throw cause))
                            admission/release! (fn [db id owner] (swap! released conj [db id owner]) true)
                            admission/finalize! (fn [_ _ _] (is false "failed provisioning must not finalize"))]
                (is (identical? cause (await (caught redeem!))))
                (is (= [[:fixture-db "invite-a" "claim-a"]] @released))))))))

(deftest ^:async failed-reservation-release-is-an-unconfirmed-outcome
  (await (with-reservation!
          (^:async fn []
            (doseq [release [(fn [_ _ _] false) (fn [_ _ _] (throw (ex-info "Mongo unavailable" {})))]]
              (with-redefs [users/create-user! (fn [_ _ _ _] (throw (ex-info "create failed" {})))
                            admission/release! release
                            admission/finalize! (fn [_ _ _] (is false "release failure must not finalize"))]
                (let [result (ex-data (await (caught redeem!)))]
                  (is (= 503 (:status result)))
                  (is (= "invite_redemption_unconfirmed" (:code result)))
                  (is (= "invite-a" (:invite-id result))))))))))

(deftest ^:async uncertain-finalization-never-releases-or-repeats-provisioning
  (await (with-reservation!
          (^:async fn []
            (let [calls (atom 0)]
              (with-redefs [users/create-user! (fn [_ _ _ _] (swap! calls inc))
                            admission/finalize! (fn [_ _ _] (throw (ex-info "reply lost" {})))
                            admission/release! (fn [_ _ _] (is false "completed provisioning must retain uncertain ownership"))]
                (let [result (ex-data (await (caught redeem!)))]
                  (is (= 1 @calls))
                  (is (= 503 (:status result)))
                  (is (= "invite_redemption_unconfirmed" (:code result))))))))))
