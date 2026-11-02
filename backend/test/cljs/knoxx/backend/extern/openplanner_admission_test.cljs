(ns knoxx.backend.extern.openplanner-admission-test
  "Concurrent native Clio OpenPlanner writes must retain every admitted fact."
  (:require [clio.extern.js.fs :as fs]
            [cljs.test :as test]
            [knoxx.backend.extern.clio-store-fixture :as disk]
            [knoxx.backend.infra.clients.openplanner :as client]
            [knoxx.backend.infra.clients.openplanner-clio :as local]
            [knoxx.backend.infra.clio-application-store :as engine]
            [knoxx.backend.infra.openplanner-event-sink :as sink]))

(def ^:private scope {:org_id "organization" :project "wiki"})

(defn- options [directory]
  {:directory directory
   :embedding-config {:embed-provider-base-url "http://127.0.0.1:9999/v1"
                      :embed-provider-model "finite" :embed-provider-dimensions 2}
   :embed! (fn [texts] {:model "finite" :dimensions 2 :vectors (mapv (constantly [1 0]) texts)})})

(defn- event [id text]
  {:id id :text text :source "knoxx" :kind "knoxx.diagnostic"
   :ts "2026-09-12T12:00:00.000Z" :extra {:org_id "organization"}
   :source_ref {:project "wiki" :session "conversation"} :meta {:role "system"}})

(defn- ^:async outcome! [operation]
  (try {:value (await (operation))} (catch :default error {:error (ex-data error)})))

(defn- ^:async together! [operations]
  (let [timer* (atom nil)
        limit (js/Promise. (fn [_complete reject]
                             (reset! timer* (js/setTimeout #(reject (ex-info "Concurrent admission timed out" {})) 10000))))]
    (try
      (vec (array-seq (await (js/Promise.race
                             #js [(js/Promise.all (to-array (mapv outcome! operations))) limit]))))
      (finally (js/clearTimeout @timer*)))))

(defn- ^:async with-directory! [verify!]
  (let [directory (disk/temp-directory!)]
    (try (await (verify! directory)) (finally (fs/remove-tree! directory)))))

(test/deftest ^:async independent-providers-retain-concurrent-events-and-vectors-after-reopen
  (await
   (with-directory!
    (^:async fn [directory]
      (let [left (local/open! (options directory)) right (local/open! (options directory))
            values (mapv #(event (str "distinct-" %) (str "Value " %)) (range 6))
            results (await (together! (mapv (fn [index value]
                                              #(client/events! (if (even? index) left right) [value]))
                                            (range) values)))
            reopened (local/open! (options directory))
            rows (:rows (await (client/session! reopened "conversation" scope)))
            vector-result (await (client/ensure-event-vectors! reopened (mapv :id rows)))]
        (test/is (every? :value results) (pr-str results))
        (test/is (= (set (map :id values)) (set (map :id rows))))
        (test/is (= [] (:repaired-event-ids vector-result)) "successful writes already admitted their vectors")
        (test/is (= 6 (:vector-count vector-result)))
        (test/is (= 12 (count (engine/history (:ledger reopened))))))))))

(test/deftest ^:async concurrent-equal-events-return-first-fact-without-duplicate-history
  (await
   (with-directory!
    (^:async fn [directory]
      (let [left (local/open! (options directory)) right (local/open! (options directory))
            value (event "equal" "Same")
            results (await (together! [#(client/events! left [value]) #(client/events! right [value])]))]
        (test/is (every? :value results) (pr-str results))
        (test/is (= [0 1] (sort (keep #(get-in % [:value :count]) results))))
        (test/is (= 2 (count (engine/history (:ledger (local/open! (options directory))))))))))))

(test/deftest ^:async refused-command-releases-admission-for-the-next-valid-writer
  (await
   (with-directory!
    (^:async fn [directory]
      (let [provider (local/open! (options directory)) value (event "existing" "Original")]
        (await (client/events! provider [value]))
        (let [results (await (together! [#(client/events! provider [(assoc value :text "Conflict")])
                                        #(client/events! provider [(event "following" "Next")])]))]
          (test/is (= "openplanner_event_conflict" (get-in results [0 :error :code])))
          (test/is (some? (:value (second results))))
          (test/is (= #{"existing" "following"}
                      (set (map :id (:rows (await (client/session! provider "conversation" scope)))))))))))))

(test/deftest ^:async same-run-queued-and-started-projections-can-arrive-together
  (await
   (with-directory!
    (^:async fn [directory]
      (let [provider (local/open! (options directory))
            config {:session-project-name "wiki"}
            run {:run_id "run" :org_id "organization" :conversation_id "conversation"}]
        (with-redefs [client/client (fn ([_] provider) ([_ _] provider))]
          (let [results (await (together! [#(sink/project! config run {:run_id "run" :type "event_turn_queued" :at 0})
                                          #(sink/project! config run {:run_id "run" :type "event_turn_started" :at 1})]))
                rows (:rows (await (client/session! (local/open! (options directory)) "conversation" scope)))]
            (test/is (every? :value results) (pr-str results))
            (test/is (= #{"event_turn_queued" "event_turn_started"} (set (map :text rows)))))))))))
