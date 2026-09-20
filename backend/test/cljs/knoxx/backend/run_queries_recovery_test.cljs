(ns knoxx.backend.run-queries-recovery-test
  "Selected-provider run reads preserve caller scope, restart data and expiry."
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.domain.action.run-state :as state]
            [knoxx.backend.extern.provider-recovery-fixture :as fixture]
            [knoxx.backend.infra.run-events :as events]
            [knoxx.backend.infra.run-queries :as queries]
            [knoxx.backend.infra.stores.clio-run-store :as store]
            [knoxx.backend.infra.stores.session-store-registry :as registry]
            [knoxx.backend.shape.session-persistence :as persistence]))

(def ^:private at "2026-09-12T12:00:00.000Z")
(def ^:private run {:run_id "run-1" :session_id "session-1" :conversation_id "conversation-1"
                   :org_id "org-1" :user_id "user-1" :membership_id "member-1"
                   :status "running" :created_at at :updated_at at :answer "Durable answer"})
(def ^:private ctx {:org-id "org-1" :user-id "user-1" :membership-id "member-1"
                   :permissions ["agent.chat.use" "agent.runs.read_own"]})
(def ^:private event {:event_id "event-1" :run_id "run-1" :session_id "session-1"
                     :conversation_id "conversation-1" :at at :type "run_started"})

(defn- ^:async status [f]
  (try (await (f)) nil (catch :default error (:status (ex-data error)))))

(deftest ^:async selected-run-is-returned-after-restart-and-never-replaced-by-stale-memory
  (let [directory (fixture/temporary-directory) clock (atom at)
        options {:directory directory :clock! #(deref clock) :instance-id "original"}]
    (try
      (await (persistence/put-run! (store/open! options) run))
      (with-redefs [registry/session-store* (atom (store/open! (assoc options :instance-id "reopened")))
                    state/runs* (atom {"run-1" (assoc run :answer "Stale memory")})]
        (is (= "Durable answer" (:answer (await (queries/read! ctx "run-1")))))
        (is (= "original" (:system_instance_id (await (queries/read! ctx "run-1")))))
        (reset! clock "2026-09-12T14:00:00.000Z")
        (is (= 404 (await (status #(queries/read! ctx "run-1")))))
        (is (= 404 (await (status #(queries/events-since! ctx "run-1" nil))))))
      (finally (fixture/remove! directory)))))

(deftest ^:async cross-tenant-own-principal-and-anonymous-reads-stop-before-provider-or-flush
  (let [directory (fixture/temporary-directory) operations (atom [])]
    (try
      (let [provider (store/open! {:directory directory :clock! (constantly at) :instance-id "test"})]
        (await (persistence/put-run! provider run))
        (await (persistence/append-event! provider event))
        (with-redefs [registry/session-store* (atom provider)
                      events/flush! (fn [id] (swap! operations conj [:flush id]))]
          (is (= 401 (await (status #(queries/read! nil "run-1")))))
          (is (= 401 (await (status #(queries/events-since! nil "run-1" nil)))))
          (is (= 403 (await (status #(queries/read! (assoc ctx :org-id "foreign-org") "run-1")))))
          (is (= 403 (await (status #(queries/events-since! (assoc ctx :org-id "foreign-org") "run-1" nil)))))
          (is (= 403 (await (status #(queries/events-since! (assoc ctx :user-id "foreign" :membership-id "foreign") "run-1" nil)))))
          (is (empty? @operations) "Unauthorized reads cannot flush pending event effects")
          (is (= [1] (mapv :sequence (await (queries/events-since! ctx "run-1" "0")))))
          (is (= [[:flush "run-1"]] @operations))
          (is (= "run-1" (:run_id (await (queries/read! (assoc ctx :org-id "foreign" :permissions ["agent.chat.use" "agent.runs.read_all"]) "run-1")))))
          (is (= "run-1" (:run_id (await (queries/read! {:role-slugs ["system-admin"]} "run-1")))))))
      (finally (fixture/remove! directory)))))

(deftest ^:async provider-corruption-is-not-a-memory-fallback
  (let [directory (fixture/temporary-directory)]
    (try
      (let [provider (store/open! {:directory directory :clock! (constantly at) :instance-id "test"})]
        (await (persistence/put-run! provider run))
        (fixture/corrupt! directory)
        (with-redefs [registry/session-store* (atom provider) state/runs* (atom {"run-1" run})]
          (try (await (queries/read! ctx "run-1")) (is false "Corrupt durable authority must surface")
               (catch :default error (is (some? error))))))
      (finally (fixture/remove! directory)))))

(deftest ^:async invalid-cursors-refuse-without-returning-accepted-events
  (let [directory (fixture/temporary-directory)]
    (try
      (let [provider (store/open! {:directory directory :clock! (constantly at) :instance-id "test"})]
        (await (persistence/put-run! provider run))
        (await (persistence/append-event! provider event))
        (with-redefs [registry/session-store* (atom provider) events/flush! (constantly true)]
          (doseq [cursor [-1 {} "nonsense" "-1" "99999999999999999999999999999999999999999999999999999999999999"]]
            (is (= 400 (await (status #(queries/events-since! ctx "run-1" cursor))))))
          (is (= [] (await (queries/events-since! ctx "run-1" at))))))
      (finally (fixture/remove! directory)))))

(deftest ^:async missing-event-provider-refuses-after-ownership-before-any-heap-read-or-flush
  (let [operations (atom [])
        unsupported (reify persistence/ISessionStore
                      (get-run [_ _] run)
                      (put-run! [_ _] nil)
                      (patch-run! [_ _ _] nil)
                      (list-active-runs [_ _] [])
                      (complete-run! [_ _ _] nil)
                      (delete-run! [_ _] nil))]
    (doseq [provider [nil unsupported]]
      (with-redefs [registry/session-store* (atom provider)
                    state/runs* (atom {"run-1" run})
                    state/get-run-events-since (fn [_ _] (swap! operations conj :heap) [event])
                    events/flush! (fn [_] (swap! operations conj :flush))]
        (is (= 401 (await (status #(queries/events-since! nil "run-1" nil)))))
        (is (= (if provider 403 503) (await (status #(queries/events-since! (assoc ctx :org-id "foreign") "run-1" nil)))))
        (try (await (queries/events-since! ctx "run-1" nil))
             (is false "Unavailable durable authority must not silently become process memory")
             (catch :default error
               (is (= {:status 503 :code (if provider "run_event_provider_unsupported" "run_provider_unavailable")} (ex-data error)))))
        (is (empty? @operations) "Refusal happens before either event effect")))))

(deftest ^:async directory-reads-selected-durable-state-and-current-tenant-policy
  (let [directory (fixture/temporary-directory) clock (atom at)]
    (try
      (let [options {:directory directory :clock! #(deref clock) :instance-id "test"}
            provider (store/open! options)]
        (await (persistence/put-run! provider run))
        (await (persistence/put-run! provider (assoc run :run_id "other-org" :org_id "foreign")))
        (await (persistence/put-run! provider (assoc run :run_id "other-user" :user_id "other" :membership_id "other")))
        (with-redefs [registry/session-store* (atom (store/open! options))
                      state/runs* (atom {"heap-only" (assoc run :run_id "heap-only")})]
          (is (= 401 (await (status #(queries/list! nil 100)))))
          (is (= ["run-1"] (mapv :run_id (await (queries/list! ctx 100)))))
          (is (= #{"run-1" "other-user"}
                 (set (map :run_id (await (queries/list! (assoc ctx :permissions ["agent.chat.use" "agent.runs.read_org"]) 100))))))
          (is (= 3 (count (await (queries/list! {:role-slugs ["system-admin"]} 100)))))
          (doseq [limit [0 501 nil 1.5]] (is (= 400 (await (status #(queries/list! ctx limit))))))
          (reset! clock "2026-09-12T14:00:00.000Z")
          (is (= [] (await (queries/list! ctx 100))))))
      (finally (fixture/remove! directory)))))
