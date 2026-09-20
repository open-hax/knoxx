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

(defn- guarded-write! [directory]
  (let [gate (fixture/deferred)
        entered (fixture/deferred)
        calls (atom 0)
        observed (atom [])
        answer (atom :pending)
        store (assoc (open! directory)
                     :before-append (fn [_] (swap! calls inc)
                                      ((:resolve! entered) nil)
                                      (:promise gate))
                     :after-append (fn [_] (swap! observed conj :after)))
        unsubscribe (clio/subscribe! #(swap! observed conj :change))
        ;; Observe rejection independently so the pre-fix ignored promise can
        ;; be asserted without an unrelated unhandled-rejection fatal exit.
        guard-result (attempt #(:promise gate))
        pending ((^:async fn []
                   (let [result (await (attempt #(clio/write! store "guarded" :test/put [:key :accepted])))]
                     (reset! answer result)
                     result)))]
    {:store store :gate gate :entered (:promise entered) :calls calls
     :observed observed :answer answer :pending pending
     :guard-result guard-result :unsubscribe unsubscribe}))

(deftest ^:async pending-admission-stays-private-and-success-appends-once
  (let [directory (fixture/temp-directory!)
        {:keys [store gate entered observed answer calls pending unsubscribe]} (guarded-write! directory)]
    (try
      (await entered)
      (await (fixture/drain!))
      (is (empty? (clio/history store)))
      (is (= :pending @answer))
      (is (empty? @observed))
      ((:resolve! gate) :allowed)
      (is (= {:value :accepted} (await pending)))
      (await (fixture/drain!))
      (is (= 1 (count (clio/history store))))
      (is (= [:change :after] @observed))
      (is (= :accepted (await (clio/write! store "guarded" :test/put [:key :accepted]))))
      (is (= 1 @calls))
      (is (= 1 (count (clio/history store))))
      (finally (unsubscribe) (fs/remove-tree! directory)))))

(deftest ^:async rejected-admission-never-becomes-a-fact-or-success
  (let [directory (fixture/temp-directory!)
        {:keys [store gate entered observed pending guard-result unsubscribe]} (guarded-write! directory)
        refusal {:status 403 :code "admission_revoked"}]
    (try
      (await entered)
      ((:reject! gate) (ex-info "Admission revoked" refusal))
      (is (= {:error refusal} (await guard-result)))
      (is (= {:error refusal} (await pending)))
      (await (fixture/drain!))
      (is (empty? (clio/history store)))
      (is (nil? (await (clio/read! store :test/get [:key]))))
      (is (empty? @observed))
      (finally (unsubscribe) (fs/remove-tree! directory)))))

(deftest ^:async delayed-admission-retains-stale-slot-refusal
  (let [directory (fixture/temp-directory!)
        {:keys [store gate entered observed pending unsubscribe]} (guarded-write! directory)]
    (try
      (await entered)
      (is (= :winner (await (clio/write! (open! directory) "winner" :test/put [:key :winner]))))
      ((:resolve! gate) :allowed)
      (is (= "clio_application_stale_head" (get-in (await pending) [:error :code])))
      (is (= 409 (get-in (await pending) [:error :status])))
      (is (= 1 (count (clio/history store))))
      (is (= :winner (await (clio/read! store :test/get [:key]))))
      (is (= [:change] @observed) "only the winning append notifies observers")
      (finally (unsubscribe) (fs/remove-tree! directory)))))

(deftest ^:async synchronous-admission-refusal-still-prevents-append
  (let [directory (fixture/temp-directory!)]
    (try
      (let [store (assoc (open! directory) :before-append
                         (fn [_] (throw (ex-info "Refused" {:status 403}))))]
        (is (= {:error {:status 403}} (await (attempt #(clio/write! store :test/put [:key :refused])))))
        (is (empty? (clio/history store))))
      (finally (fs/remove-tree! directory)))))

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
