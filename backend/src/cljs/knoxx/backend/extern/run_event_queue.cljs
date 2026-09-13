(ns knoxx.backend.extern.run-event-queue
  "Ordered asynchronous admission at the synchronous runtime event boundary.
   A failed write remains observable by flush; later writes never erase it.")

(defn create
  "Return per-run submit/flush functions without exposing native Promise state."
  [write!]
  (let [tails (atom {})
        failures (atom {})]
    {:submit! (fn [event]
                (let [id (:run_id event)
                      previous (get @tails id)
                      next-work ((^:async fn []
                                   (when previous (await previous))
                                   (when-let [failure (get @failures id)] (throw failure))
                                   (await (write! event))))
                      observed (.catch next-work
                                       (fn [error] (swap! failures assoc id error) nil))]
                  (swap! tails assoc id observed)
                  nil))
     :flush! (^:async fn [id]
               (loop []
                 (let [tail (get @tails id)]
                   (when tail (await tail))
                   (when-let [failure (get @failures id)] (throw failure))
                   (if (identical? tail (get @tails id))
                     (do (swap! tails dissoc id) true)
                     (recur)))))}))
