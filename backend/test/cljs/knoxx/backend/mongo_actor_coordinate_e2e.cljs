(ns knoxx.backend.mongo-actor-coordinate-e2e
  "Actual Mongo CAS and complete contract projection cannot replace an assigned actor."
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.extern.identity-fixture :as files]
            [knoxx.backend.extern.mongo-actor-coordinate-fixture :as fixture]
            [knoxx.backend.extern.mongo-remote-identity-fixture :as data]
            [knoxx.backend.extern.mongo-run-native-fixture :as native]
            [knoxx.backend.extern.promise :as promise]
            [knoxx.backend.infra.db.policy.connection :as connection]
            [knoxx.backend.infra.db.policy.projection :as projection]
            [knoxx.backend.infra.stores.mongo-policy-directory :as directory]))

(defn- ^:async attempt [operation]
  (try {:value (await (operation))} (catch :default cause {:error (ex-data cause)})))

(defn- ^:async with-mongo! [run!]
  (let [mongo (await (native/open!))]
    (try
      (await (data/seed! (:db mongo) {:issuer "https://fixture.example.test"}))
      (await (run! (:db mongo)))
      (finally (await (native/close! mongo))))))

(deftest ^:async concurrent-initial-actor-assignments-have-one-winner
  (await (with-mongo!
          (^:async fn [db]
            (await (fixture/initial-actor! db nil))
            (let [answers (await (promise/all-vec
                                  [(attempt #(directory/set-membership-actor-id! db "existing-member" "actor-a"))
                                   (attempt #(directory/set-membership-actor-id! db "existing-member" "actor-b"))]))
                  winners (keep :value answers)
                  failures (keep :error answers)
                  stored (:actor_id (await (directory/get-membership! db "existing-member")))]
              (is (= 1 (count winners)))
              (is (= [409] (mapv :status failures)))
              (is (= (first winners) stored))
              (is (= stored (await (directory/set-membership-actor-id! db "existing-member" (str " " stored " ")))))
              (is (= stored (:actor_id (await (directory/get-membership! db "existing-member"))))))))))

(deftest ^:async native-conflicting-assignment-or-missing-member-never-reports-success
  (await (with-mongo!
          (^:async fn [db]
            (let [before (await (data/directory-state db))]
              (is (= 409 (get-in (await (attempt #(directory/set-membership-actor-id! db "existing-member" "other")))
                                  [:error :status])))
              (is (= 404 (get-in (await (attempt #(directory/set-membership-actor-id! db "missing" "other")))
                                  [:error :status])))
              (is (= before (await (data/directory-state db)))))))))

(defn- ^:async assert-full-projection-refusal! [db]
  (await (fixture/seed-projection-role! db))
  (with-redefs [connection/db! (fn [] db)]
    (let [before (await (data/directory-state db))
          outcome (await (attempt #(projection/sync-actor-contracts! nil {:id "existing-org"})))]
      (is (= 409 (get-in outcome [:error :status])))
      (is (= before (await (data/directory-state db))))
      (is (= "existing-local-actor" (:actor_id (await (directory/get-membership! db "existing-member"))))))))

(deftest ^:async actual-full-contract-reprojection-refuses-stale-actor-without-policy-mutation
  (await (with-mongo!
          (^:async fn [db]
            (let [directory (files/directory!)]
              (try
                (await (fixture/with-conflicting-contracts! directory #(assert-full-projection-refusal! db)))
                (finally (files/remove! directory))))))))
