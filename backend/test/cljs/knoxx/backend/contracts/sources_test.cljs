(ns knoxx.backend.contracts.sources-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.contracts.loader :as loader]
            [knoxx.backend.domain.contracts.sources :as sut]))

(def openplanner-source-contract
  {:contract/id "openplanner-memory"
   :contract/kind :source
   :source/id :source/openplanner-memory
   :source/provider :openplanner
   :source/hydration {:strategy :memory-search
                      :mode :triggered
                      :k 6}})

(def disabled-github-source-contract
  {:contract/id "app-events"
   :contract/kind :source
   :source/id :github/app-events
   :source/provider :github
   :source/driver :driver/github-app
   :source/listens [:github.delivery-received]
   :enabled false})

(defn- source-contract-sync
  [_ contract-class contract-id]
  (when (= "sources" contract-class)
    (case contract-id
      "openplanner-memory" openplanner-source-contract
      "app-events" disabled-github-source-contract
      nil)))

(deftest normalize-source-id-test
  (testing "legacy unqualified IDs gain the source namespace"
    (is (= :source/openplanner-memory
           (sut/normalize-source-id :openplanner_memory)))
    (is (= :source/openplanner-memory
           (sut/normalize-source-id "openplanner_memory"))))
  (testing "qualified identities remain qualified"
    (is (= :source/openplanner-memory
           (sut/normalize-source-id :source/openplanner-memory)))
    (is (= :source/openplanner-memory
           (sut/normalize-source-id "source/openplanner-memory")))
    (is (= :github/app-events
           (sut/normalize-source-id :github/app_events)))
    (is (= :github/app-events
           (sut/normalize-source-id "github/app_events")))))

(deftest compose-source-refs-dedupes-and-deep-merges-by-source-id
  (with-redefs [loader/contract-sync source-contract-sync
                loader/load-all-contracts-sync (fn [_] [])]
    (let [resolved (sut/compose-source-refs
                    {}
                    [:source/openplanner-memory]
                    [{:source/ref :source/openplanner-memory
                      :hydration {:k 10}
                      :filters {:session "session-a"}}])
          source (first resolved)]
      (testing "one source survives duplicate refs"
        (is (= 1 (count resolved)))
        (is (= :source/openplanner-memory (:source/id source))))
      (testing "contract defaults and later overrides merge"
        (is (= {:strategy :memory-search
                :mode :triggered
                :k 10}
               (:source/hydration source)))
        (is (= {:session "session-a"}
               (:source/filters source)))))))

(deftest disabled-source-contract-cannot-leak-into-an-agent
  (with-redefs [loader/contract-sync source-contract-sync
                loader/load-all-contracts-sync (fn [_] [])]
    (testing "a disabled qualified resource is omitted"
      (is (= []
             (sut/source-specs-for-agent
              {}
              {:sources [:github/app-events]}))))
    (testing "a run or actor ref cannot silently re-enable the resource"
      (is (= []
             (sut/source-specs-for-agent
              {}
              {:sources [{:source/ref :github/app-events
                          :enabled true}]}))))))

(deftest later-source-ref-may-disable-an-enabled-source
  (with-redefs [loader/contract-sync source-contract-sync
                loader/load-all-contracts-sync (fn [_] [])]
    (is (= []
           (sut/compose-source-refs
            {}
            [:source/openplanner-memory]
            [{:source/ref :source/openplanner-memory
              :enabled false}])))))

(deftest qualified-source-lookup-rejects-a-colliding-legacy-contract
  (let [legacy (assoc openplanner-source-contract
                      :contract/id "app-events"
                      :source/id :source/app-events)
        records [{:contractClass "sources" :id "app-events" :contract legacy}
                 {:contractClass "sources" :id "github/app-events"
                  :contract disabled-github-source-contract}]]
    (doseq [ordered-records [records (vec (reverse records))]]
      (with-redefs [loader/load-all-contracts-sync (fn [_] ordered-records)]
        (testing "qualified refs resolve the matching contract in either catalog order"
          (doseq [source-ref [:github/app-events "github/app_events"
                              {:source/ref :github/app-events}]]
            (is (= disabled-github-source-contract
                   (sut/source-contract {} source-ref)))))
        (testing "the legacy ref still resolves its own contract"
          (is (= legacy (sut/source-contract {} :source/app-events))))
        (testing "a colliding enabled contract cannot bypass GitHub disablement"
          (is (= [] (sut/compose-source-refs
                     {} [:github/app-events]
                     [{:source/ref :github/app-events :enabled true}]))))))))

(deftest missing-qualified-source-does-not-inherit-a-legacy-contract
  (doseq [enabled [true false]]
    (let [legacy (assoc openplanner-source-contract
                        :contract/id "app-events"
                        :source/id :source/app-events
                        :enabled enabled)]
      (with-redefs [loader/load-all-contracts-sync
                    (fn [_] [{:contractClass "sources" :id "app-events"
                              :contract legacy}])]
        (is (nil? (sut/source-contract {} :github/app-events)))
        (testing "a missing contract remains a run-local ref without inherited settings"
          (is (= {:source/id :github/app-events :source/ref :github/app-events}
                 (sut/resolve-source-spec {} :github/app-events))))))))

(deftest legacy-contract-id-still-supplies-a-missing-source-id
  (let [legacy (dissoc openplanner-source-contract :source/id)]
    (with-redefs [loader/load-all-contracts-sync
                  (fn [_] [{:contractClass "sources" :id "openplanner-memory"
                            :contract legacy}])]
      (is (= legacy (sut/source-contract {} "openplanner_memory"))))))
