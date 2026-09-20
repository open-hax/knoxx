(ns knoxx.backend.domain.persistence-instant-test
  "Invalid timestamp refusal at real run and thread transition boundaries."
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.domain.run-store :as run]
            [knoxx.backend.domain.thread-store :as thread]
            [knoxx.backend.law.run-store :as run-law]))

(def at "2024-02-29T12:00:00.000Z")
(def at-ms 1709208000000)
(def invalid-at "2026-02-30T12:00:00.000Z")
(def run-stamp {:at at :at-ms at-ms :expires-ms (+ at-ms run-law/ttl-ms)
                :instance-id "calendar-regression"})
(def thread-stamp {:at at :at-ms at-ms :expires-at "2024-02-29T13:00:00.000Z"
                   :expires-ms (+ at-ms 3600000) :instance-id "calendar-regression"})
(def run-value {:run_id "run" :session_id "session" :conversation_id "conversation"
                :status "running" :created_at at :updated_at at})

(defn- refusal-data
  "Return the classified admission error, or nil if invalid input was admitted."
  [operation]
  (try (operation) nil
       (catch :default error (ex-data error))))

(deftest thread-transition-validates-both-stamp-instants
  (let [operation {:kind :put :thread-id "session" :thread {:session_id "session"}
                   :stamp thread-stamp}
        accepted (thread/transition thread/empty-state operation)]
    (is (= at (get-in accepted [:state :threads "session" :thread :createdAt])))
    (is (= (:expires-at thread-stamp)
           (get-in accepted [:state :threads "session" :thread :expiresAt])))
    (doseq [field [:at :expires-at]]
      (is (= {:status 400 :code "thread_store_invalid" :contract :thread/operation}
             (refusal-data #(thread/transition thread/empty-state
                                              (assoc-in operation [:stamp field] invalid-at))))))))

(deftest run-transition-rejects-invalid-stamp-and-event-instants
  (let [operation {:kind :put :run-id "run" :run run-value :stamp run-stamp}
        [state accepted] (run/transition run/empty-state operation)
        event {:run_id "run" :session_id "session" :conversation_id "conversation"
               :type "run_started" :at at}
        event-operation {:kind :event :run-id "run" :event-id "event"
                         :event event :stamp run-stamp}]
    (is (= at (:created_at accepted)))
    (is (= {:status 400 :code "run_store_invalid"}
           (refusal-data #(run/transition run/empty-state
                                         (assoc-in operation [:stamp :at] invalid-at)))))
    (is (= at (:at (second (run/transition state event-operation)))))
    (is (= {:status 400 :code "run_store_invalid"}
           (refusal-data #(run/transition state
                                         (assoc-in event-operation [:event :at] invalid-at)))))
    (is (= {:status 400 :code "run_store_invalid"}
           (refusal-data #(run/events-since state "run" invalid-at at-ms))))))
