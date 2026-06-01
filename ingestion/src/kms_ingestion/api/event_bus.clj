(ns kms-ingestion.api.event-bus
  "In-memory progress event distribution for ingestion jobs.

   Each connected SSE client registers a blocking queue against a job id.
   The worker calls `broadcast-event!` as it makes progress; every queue
   registered for that job receives a copy of the event. Clients drain their
   queue via `poll-event!` and clean up with `unsubscribe-from-job!` when the
   connection closes.

   No external dependency (core.async / manifold) is required — a plain atom
   of `LinkedBlockingQueue`s keeps the fan-out simple and side-effect free."
  (:import
   [java.util.concurrent LinkedBlockingQueue TimeUnit]))

;; job-id (string) -> set of subscriber queues
(defonce ^:private subscribers (atom {}))

(defn subscribe-to-job!
  "Register a listener for `job-id`'s events. Returns a fresh
   `LinkedBlockingQueue` that will receive every event broadcast for the job
   from this point forward. Pass the returned queue to `poll-event!` and
   `unsubscribe-from-job!`."
  ^LinkedBlockingQueue [job-id]
  (let [queue (LinkedBlockingQueue.)]
    (swap! subscribers update job-id (fnil conj #{}) queue)
    queue))

(defn unsubscribe-from-job!
  "Remove a previously-subscribed `queue` for `job-id`. Cleans up the job entry
   entirely once it has no remaining listeners."
  [job-id ^LinkedBlockingQueue queue]
  (swap! subscribers
         (fn [m]
           (let [remaining (disj (get m job-id #{}) queue)]
             (if (empty? remaining)
               (dissoc m job-id)
               (assoc m job-id remaining))))))

(defn subscriber-count
  "Number of clients currently listening to `job-id`."
  [job-id]
  (count (get @subscribers job-id)))

(defn broadcast-event!
  "Emit `event` (a Clojure map) to every client subscribed to `job-id`.
   No-op when there are no listeners, so the worker pays nothing on the common
   path. Never throws — a full queue simply drops the slowest client's event."
  [job-id event]
  (doseq [^LinkedBlockingQueue queue (get @subscribers job-id)]
    (try
      (.offer queue event)
      (catch Exception _ nil))))

(defn poll-event!
  "Block up to `timeout-ms` for the next event on `queue`. Returns the event
   map, or nil when the timeout elapses (the caller should send a heartbeat)."
  [^LinkedBlockingQueue queue timeout-ms]
  (.poll queue (long timeout-ms) TimeUnit/MILLISECONDS))
