(ns knoxx.backend.infra.identity-membership-bindings-test
  "Owned membership selection and append-only compatibility over real Clio files."
  (:require [clio.extern.js.fs :as fs]
            [cljs.test :refer [deftest is]]
            [knoxx.backend.extern.clio-store-fixture :as fixture]
            [knoxx.backend.extern.promise :as promise]
            [knoxx.backend.infra.clio-application-store :as application]
            [knoxx.backend.infra.identity-bindings :as bindings]))

(def ^:private canonical
  {:principal-id "principal-a" :entity-id "entity-a" :kind :human :user-id "user-a"
   :membership-id "member-a" :org-id "org-a" :actor-id "actor-a"})

(def ^:private invited
  (assoc canonical :membership-id "member-b" :org-id "org-b" :actor-id "actor-b"))

(defn- ^:async caught [operation]
  (try {:value (await (operation))} (catch :default cause {:error (ex-data cause)})))

(defn- add! [store binding]
  (bindings/bind-membership! store binding))

(defn- select! [store principal-id selector]
  (bindings/read-membership! store principal-id selector))

(defn- ^:async with-store! [run!]
  (let [directory (fixture/temp-directory!)]
    (try (await (run! (bindings/open! {:directory directory}) directory))
         (finally (fs/remove-tree! directory)))))

(deftest ^:async legacy-default-facts-replay-unchanged-beside-new-memberships
  (await (with-store!
          (^:async fn [store directory]
            ;; This historical operation remains the sole canonical/default write.
            (is (= canonical (await (bindings/bind! store canonical))))
            (let [original (application/history store)]
              (is (= [:identity/bind]
                     (mapv #(get-in % [:event/data :operation/method]) original)))
              (is (= invited (await (add! store invited))))
              (let [reopened (bindings/open! {:directory directory})
                    facts (application/history reopened)]
                (is (= original (vec (take (count original) facts))))
                (is (= [:identity/bind :identity/bind-membership]
                       (mapv #(get-in % [:event/data :operation/method]) facts)))
                (is (= canonical (await (bindings/read! reopened "principal-a"))))
                (is (= invited (await (select! reopened "principal-a" {:org-id "org-b"}))))
                (is (= canonical (await (select! reopened "principal-a" {:membership-id "member-a"}))))
                (is (= invited (await (add! reopened invited))))
                (is (= canonical (await (add! reopened canonical))))
                (is (= canonical (await (bindings/bind! reopened canonical))))
                (is (= facts (application/history reopened)) "exact retries append no additional facts")
                (is (= 409 (get-in (await (caught #(bindings/bind! reopened invited))) [:error :status])))
                (is (= facts (application/history reopened)))))))))

(deftest ^:async supplementary-admission-requires-the-canonical-owner
  (await (with-store!
          (^:async fn [store _directory]
            (is (= 404 (get-in (await (caught #(add! store invited))) [:error :status])))
            (is (empty? (application/history store)))
            (await (bindings/bind! store canonical))
            (let [before (application/history store)]
              (doseq [foreign [(assoc invited :user-id "user-other")
                               (assoc invited :entity-id "entity-other")
                               (assoc invited :kind :service)]]
                (is (= 403 (get-in (await (caught #(add! store foreign))) [:error :status]))))
              (is (= 404 (get-in (await (caught #(add! store (assoc invited :principal-id "principal-other"))))
                                  [:error :status])))
              (is (= 400 (get-in (await (caught #(add! store (dissoc invited :user-id)))) [:error :status])))
              (is (= before (application/history store))))))))

(deftest ^:async accepted-membership-coordinates-cannot-be-rebound
  (await (with-store!
          (^:async fn [store directory]
            (await (bindings/bind! store canonical))
            (await (add! store invited))
            (let [before (application/history store)]
              (doseq [conflict [(assoc invited :org-id "org-other")
                                (assoc invited :actor-id "actor-other")
                                (assoc canonical :actor-id "actor-other")]]
                (is (= 409 (get-in (await (caught #(add! store conflict))) [:error :status]))))
              (is (= before (application/history store)))
              (is (= invited (await (select! (bindings/open! {:directory directory}) "principal-a"
                                             {:membership-id "member-b" :org-id "org-b"})))))))))

(deftest ^:async concurrent-writers-cannot-accept-conflicting-membership-coordinates
  (await (with-store!
          (^:async fn [store directory]
            (await (bindings/bind! store canonical))
            (let [other-store (bindings/open! {:directory directory})
                  answers (await (promise/all-vec
                                  [(caught #(add! store invited))
                                   (caught #(add! other-store (assoc invited :actor-id "actor-other")))]))
                  accepted (keep :value answers)
                  refused (keep :error answers)]
              (is (= 1 (count accepted)))
              (is (= [409] (mapv :status refused)))
              (is (= 2 (count (application/history store))))
              (is (= (first accepted)
                     (await (select! (bindings/open! {:directory directory}) "principal-a"
                                     {:membership-id "member-b"})))))))))

(deftest ^:async explicit-selectors-never-fall-back-or-cross-principal-ownership
  (await (with-store!
          (^:async fn [store _directory]
            (await (bindings/bind! store canonical))
            (await (add! store invited))
            (await (bindings/bind! store (assoc canonical :principal-id "principal-other"
                                                :entity-id "entity-other" :user-id "user-other"
                                                :membership-id "member-other" :org-id "org-other")))
            (doseq [selector [{:membership-id "missing"} {:org-id "org-other"}
                              {:membership-id "member-other"}
                              {:membership-id "member-b" :org-id "org-a"}]]
              (is (= 404 (get-in (await (caught #(select! store "principal-a" selector))) [:error :status]))))
            (is (= 404 (get-in (await (caught #(select! store "missing" {:org-id "org-a"}))) [:error :status])))
            (is (= canonical (await (bindings/read! store "principal-a"))))))))

(deftest ^:async organization-ambiguity-requires-an-exact-membership-selector
  (await (with-store!
          (^:async fn [store _directory]
            (await (bindings/bind! store canonical))
            (let [second-member (assoc invited :org-id "org-a")]
              (await (add! store second-member))
              (is (= 409 (get-in (await (caught #(select! store "principal-a" {:org-id "org-a"}))) [:error :status])))
              (is (= second-member (await (select! store "principal-a" {:org-id "org-a" :membership-id "member-b"}))))
              (is (= canonical (await (bindings/read! store "principal-a")))))))))

(deftest ^:async invalid-selectors-refuse-before-any-default-read
  (await (with-store!
          (^:async fn [store _directory]
            (await (bindings/bind! store canonical))
            (doseq [selector [nil {} [] "org-a" {:org-id nil} {:org-id ""} {:org-id "  "}
                              {:membership-id 42} {:email "a@example.test"}
                              {:org-id "org-a" :ignored true}]]
              (is (= 400 (get-in (await (caught #(select! store "principal-a" selector))) [:error :status]))))
            (doseq [principal-id [nil "" "  " 42]]
              (is (= 400 (get-in (await (caught #(select! store principal-id {:org-id "org-a"}))) [:error :status]))))
            (is (= 1 (count (application/history store))))))))
