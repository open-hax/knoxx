(ns knoxx.backend.infra.clio-application-store-test
  "Real-filesystem replay, identity, corruption and concurrent-admission laws."
  (:require [clio.extern.js.fs :as fs]
            [cljs.test :refer [deftest is testing]]
            [knoxx.backend.extern.clio-store-fixture :as fixture]
            [knoxx.backend.extern.promise :as promise]
            [knoxx.backend.infra.clio-application-store :as clio]))

(defn- projection
  "Small deterministic reference exposing observable state and command answers."
  []
  (let [state (atom {})]
    {:store state :snapshot #(deref state)}))

(defn- open!
  "Open a fixture provider whose operations intentionally include arbitrary EDN."
  [directory]
  (clio/open!
   {:directory directory :stream "test/application" :projection projection
    :reads {:test/get (fn [state key] (get @state key))}
    :writes {:test/put (fn [state key value] (swap! state assoc key value) value)}}))

(defn- ^:async attempt
  "Capture success or classified failure without concealing rejected writes."
  [operation]
  (try {:value (await (operation))}
       (catch :default cause {:error (ex-data cause)})))

(deftest ^:async durable-restart-and-lost-response-idempotence
  (let [directory (fixture/temp-directory!)]
    (try
      (let [store (open! directory)
            data {:identity/id #uuid "b4dd8aeb-996b-46b1-9c38-2e1ce579f37a"
                  :observed/at #inst "2026-09-12T00:00:00.000-00:00"
                  :roles #{:roles/author :roles/reviewer}
                  :wiki/page {:page/title "EDN preserves namespaced fields"}}]
        (is (fixture/resolved-directory? directory))
        (is (= data (await (clio/write! store "operation-1" :test/put [:page/home data]))))
        (let [restarted (open! directory)]
          (is (= data (await (clio/read! restarted :test/get [:page/home])))))
        (is (= data (await (clio/write! store "operation-1" :test/put [:page/home data]))))
        (is (= data (await (clio/write! store :test/put [:page/home data]))))
        (is (= 1 (count (clio/history store))) "both retry forms append no second fact")
        (is (= :clio-application/operation-conflict
               (get-in (await (attempt #(clio/write! store "operation-1" :test/put
                                                     [:page/home :changed])))
                       [:error :cause])))
        (is (= data (await (clio/read! store :test/get [:page/home])))))
      (finally (fs/remove-tree! directory)))))

(deftest ^:async separate-writers-refuse-a-stale-admission
  (let [directory (fixture/temp-directory!)]
    (try
      (let [left (open! directory)
            right (open! directory)
            answers (await (promise/all-vec
                            [(attempt #(clio/write! left :test/put [:writer :left]))
                             (attempt #(clio/write! right :test/put [:writer :right]))]))]
        (is (= 1 (count (filter #(contains? % :value) answers))))
        (is (= [:clio.ledger/concurrent-stream-write]
               (keep #(get-in % [:error :clio/error]) answers)))
        (is (= 1 (count (clio/history left))))
        (is (contains? #{:left :right} (await (clio/read! left :test/get [:writer])))))
      (finally (fs/remove-tree! directory)))))

(deftest ^:async malformed-and-semantically-corrupt-ledgers-fail-closed
  (let [directory (fixture/temp-directory!)]
    (try
      (let [store (open! directory)
            file (:file store)]
        (await (clio/write! store :test/put [:saved :original]))
        (let [fact (first (clio/history store))]
          (fs/write-text! file (str (pr-str (assoc-in fact [:event/data :operation/result]
                                                      :forged-answer)) "\n"))
          (is (= :clio-application/replay-conflict
                 (get-in (await (attempt #(clio/read! store :test/get [:saved])))
                         [:error :cause]))))
        (fs/write-text! file "{:truncated\n")
        (is (thrown? js/Error (open! directory)))
        (fs/delete-if-exists! file)
        (is (thrown? js/Error (open! directory)))
        (is (not (fs/exists? file)) "known schema history never resurrects a lost ledger"))
      (finally (fs/remove-tree! directory)))))

(deftest ^:async undeclared-methods-are-never-interpreted-as-provider-queries
  (let [directory (fixture/temp-directory!)]
    (try
      (let [store (open! directory)]
        (is (= :clio-application/unsupported-operation
               (get-in (await (attempt #(clio/write! store :mongo/find [{:$where "code"}])))
                       [:error :cause])))
        (is (= :clio.schema/non-canonical-event
               (get-in (await (attempt #(clio/write! store :test/put [:unsafe (fn [] :code)])))
                       [:error :clio/error])))
        (is (nil? (await (clio/read! store :test/get [:unsafe]))))
        (is (empty? (clio/history store))))
      (finally (fs/remove-tree! directory)))))

(deftest directory-is-an-explicit-boundary
  (testing "an omitted or blank data path cannot accidentally create local state"
    (is (thrown? js/Error (open! nil)))
    (is (thrown? js/Error (open! "  ")))))
