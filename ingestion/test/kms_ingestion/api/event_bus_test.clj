(ns kms-ingestion.api.event-bus-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [kms-ingestion.api.event-bus :as bus]))

(deftest subscribe-broadcast-poll-roundtrip
  (testing "A subscribed client receives broadcast events in order"
    (let [job-id (str "test-job-" (random-uuid))
          queue (bus/subscribe-to-job! job-id)]
      (try
        (is (= 1 (bus/subscriber-count job-id)))
        (bus/broadcast-event! job-id {:type "job_start" :job_id job-id})
        (bus/broadcast-event! job-id {:type "file_complete" :job_id job-id :file_path "/a.md"})
        (let [first-event (bus/poll-event! queue 1000)
              second-event (bus/poll-event! queue 1000)]
          (is (= "job_start" (:type first-event)))
          (is (= "file_complete" (:type second-event)))
          (is (= "/a.md" (:file_path second-event))))
        (finally
          (bus/unsubscribe-from-job! job-id queue))))))

(deftest poll-times-out-when-no-events
  (testing "poll-event! returns nil after the timeout when nothing is broadcast"
    (let [job-id (str "test-job-" (random-uuid))
          queue (bus/subscribe-to-job! job-id)]
      (try
        (is (nil? (bus/poll-event! queue 10)))
        (finally
          (bus/unsubscribe-from-job! job-id queue))))))

(deftest fan-out-to-multiple-subscribers
  (testing "Every subscriber for a job gets its own copy of each event"
    (let [job-id (str "test-job-" (random-uuid))
          q1 (bus/subscribe-to-job! job-id)
          q2 (bus/subscribe-to-job! job-id)]
      (try
        (is (= 2 (bus/subscriber-count job-id)))
        (bus/broadcast-event! job-id {:type "job_complete" :job_id job-id})
        (is (= "job_complete" (:type (bus/poll-event! q1 1000))))
        (is (= "job_complete" (:type (bus/poll-event! q2 1000))))
        (finally
          (bus/unsubscribe-from-job! job-id q1)
          (bus/unsubscribe-from-job! job-id q2))))))

(deftest unsubscribe-cleans-up-job-entry
  (testing "The job entry is removed once its last subscriber unsubscribes"
    (let [job-id (str "test-job-" (random-uuid))
          queue (bus/subscribe-to-job! job-id)]
      (bus/unsubscribe-from-job! job-id queue)
      (is (zero? (bus/subscriber-count job-id)))
      (testing "broadcasting to a job with no subscribers is a no-op"
        (is (nil? (bus/broadcast-event! job-id {:type "job_start"})))))))
