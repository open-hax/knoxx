(ns knoxx.backend.extern.startup-publication-test
  "Startup broadcasts acknowledge their own real Clio facts, including rendered tasks."
  (:require [cljs.test :as test]
            [knoxx.backend.domain.realtime :as realtime]
            [knoxx.backend.extern.agent-turn-fixture :as fixture]
            [knoxx.backend.extern.event-queue-fixture :as waiting]
            [knoxx.backend.infra.agent.run-admission :as admission]
            [knoxx.backend.infra.run-events :as events]
            [knoxx.backend.infra.stores.session-store-registry :as registry]
            [knoxx.backend.shape.session-persistence :as runs]))

(defn- gate []
  (let [resolve* (atom nil) reject* (atom nil)
        promise (js/Promise. (fn [resolve reject] (reset! resolve* resolve) (reset! reject* reject)))]
    {:promise promise :resolve! #(@resolve* true) :reject! #(@reject* %)}))

(defn- ^:async start! [id settled*]
  (try
    (await (admission/create-initial-run!
            id (str id "-session") (str id "-conversation") "2026-09-20T12:00:00.000Z"
            "fixture-model" "direct" "off" {:rendered-task-prompt "Owned task text"}
            {:org_id "owned-org" :membership_id "owned-member"} [] {}))
    (reset! settled* {:ok true})
    (catch :default error (reset! settled* {:error error}))))

(defn- ^:async verify! [phase fails?]
  (let [id (str "rendered-" (random-uuid)) gate (gate)
        entered* (atom false) published* (atom []) settled* (atom nil)
        append! runs/append-event! provider @registry/session-store*
        prior (if (= phase "run_started") [] ["run_started"])]
    (with-redefs [runs/append-event!
                  (^:async fn [store event]
                    (when (= phase (:type event))
                      (reset! entered* true) (await (:promise gate)))
                    (await (append! store event)))
                  realtime/broadcast-ws-session! (fn [_ _ event] (swap! published* conj (:type event)))]
      (let [work (start! id settled*)]
        (try
          (await (waiting/wait-until! #(or @entered* @settled*)))
          (test/is @entered*)
          (test/is (nil? @settled*))
          (test/is (= prior @published*) "An accepted prior event can be announced")
          (test/is (= prior (mapv :type (await (runs/events-since provider id nil)))))
          (if fails? ((:reject! gate) (ex-info "Owned rendered-task refusal" {:code "rendered_refused"}))
              ((:resolve! gate)))
          (await work)
          (test/is (= (not fails?) (:ok @settled* false)))
          (test/is (= (if fails? prior ["run_started" "action_task_rendered"]) @published*))
          (test/is (= @published* (mapv :type (await (runs/events-since provider id nil)))))
          (finally
            ((:resolve! gate)) (await work)
            (try (await (events/flush! id)) (catch :default _error nil))
            (events/install! provider)))))))

(test/deftest ^:async every-startup-publication-waits-for-its-own-durable-fact
  (doseq [phase ["run_started" "action_task_rendered"] fails? [false true]]
    (await (fixture/with-run!
            {:run_id "startup-fixture" :session_id "startup-fixture" :conversation_id "startup-fixture"}
            #(verify! phase fails?)))))
