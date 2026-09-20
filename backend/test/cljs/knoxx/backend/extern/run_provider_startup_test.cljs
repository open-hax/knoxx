(ns knoxx.backend.extern.run-provider-startup-test
  "Publication of the actual provider waits for Mongo's uniqueness barrier."
  (:require [cljs.test :as test]
            [knoxx.backend.domain.action.run-state :as state]
            [knoxx.backend.extern.provider-recovery-fixture :as fixture]
            [knoxx.backend.infra.run-event-payload :as payload]
            [knoxx.backend.infra.run-events :as events]
            [knoxx.backend.infra.stores.clio-run-store :as clio]
            [knoxx.backend.infra.stores.mongo-run-store :as mongo]
            [knoxx.backend.infra.stores.run-provider-startup :as startup]
            [knoxx.backend.infra.stores.session-store-registry :as registry]
            [knoxx.backend.shape.session-persistence :as runs]))

(defn- deferred []
  (let [complete* (atom nil)]
    {:promise (js/Promise. (fn [complete _reject] (reset! complete* complete)))
     :complete! (fn [] (@complete* nil))}))

(test/deftest ^:async unpublished-until-indexes-and-writer-are-ready
  (let [directory (fixture/temporary-directory)
        previous @registry/session-store* previous-heap @state/runs*
        provider (clio/open! {:directory directory})
        gate (deferred) constructed* (atom false)
        at (fixture/instant (fixture/now-ms))
        run {:run_id "startup" :session_id "session" :conversation_id "conversation"
             :status "running" :created_at at :updated_at at}]
    (try
      (reset! registry/session-store* nil)
      (events/install! nil)
      (with-redefs [mongo/setup-indexes! (fn [_db] (:promise gate))
                    mongo/create-mongo-run-store (fn ([_db] (reset! constructed* true) provider) ([_db _opts] provider))]
        (let [pending (startup/install-mongo! :owned-handle)]
          (test/is (nil? @registry/session-store*))
          (test/is (false? @constructed*))
          ((:complete! gate))
          (let [installed (await pending)] (test/is (identical? provider installed)))
          (await (events/persist-run! (assoc run :events [] :run_events [] :sequence 99)))
          (state/append-run-event! "startup" (payload/tool-event-payload "startup" "conversation" "session" "started" {}))
          (await (events/flush! "startup"))
          (test/is (= ["started"] (mapv :type (await (runs/events-since provider "startup" nil)))))
          (let [stored (await (runs/get-run provider "startup"))]
            (test/is (not-any? #(contains? stored %) [:events :run_events :sequence])))))
      (finally
        (reset! registry/session-store* previous)
        (events/install! previous)
        (reset! state/runs* previous-heap)
        (fixture/remove! directory)))))

(test/deftest ^:async failed-unique-index-never-publishes-a-provider
  (let [previous @registry/session-store* constructed* (atom false)]
    (try
      (reset! registry/session-store* nil)
      (with-redefs [mongo/setup-indexes! (^:async fn [_db] (throw (ex-info "unique index refused" {})))
                    mongo/create-mongo-run-store (fn ([_db] (reset! constructed* true)) ([_db _opts] nil))]
        (try (await (startup/install-mongo! :owned-handle)) (test/is false "Index failure must refuse startup")
             (catch :default error (test/is (= "unique index refused" (ex-message error))))))
      (test/is (nil? @registry/session-store*))
      (test/is (false? @constructed*))
      (finally (reset! registry/session-store* previous)))))
