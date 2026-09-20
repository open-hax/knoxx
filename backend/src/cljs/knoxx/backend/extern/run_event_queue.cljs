(ns knoxx.backend.extern.run-event-queue
  "Ordered asynchronous admission at the synchronous runtime event boundary.
   A failed write remains observable by flush; later writes never erase it.
   Retirement is the only release for an observed failure. It fences the retired
   generation so a late callback can neither resurrect it nor reach a successor.")

(defn- generation!
  "Reserve this run's fencing token. Submission is synchronous, so the
   read-then-write below cannot interleave with another admission."
  [{:keys [generations next-generation*]} id]
  (or (get @generations id)
      (let [fresh (swap! next-generation* inc)]
        (swap! generations assoc id fresh)
        fresh)))

(defn- live?
  "Only the run's current generation may record a failure against it."
  [{:keys [generations]} id generation]
  (= generation (get @generations id)))

(defn- release!
  "Drop every entry this run owns and retire the generation that held them."
  [{:keys [generations tails failures]} id]
  (swap! generations dissoc id)
  (swap! tails dissoc id)
  (swap! failures dissoc id)
  nil)

(defn- submit!
  "Chain one event behind its run's tail without exposing the native promise."
  [{:keys [tails failures] :as queue} write! event]
  (let [id (:run_id event)
        generation (generation! queue id)
        previous (get @tails id)
        next-work ((^:async fn []
                     (when previous (await previous))
                     (when-let [failure (get @failures id)] (throw failure))
                     (await (write! event))))
        observed (.catch next-work
                         (fn [error]
                           (when (live? queue id generation)
                             (swap! failures assoc id error))
                           nil))]
    (swap! tails assoc id observed)
    nil))

(defn- ^:async flush!
  "Wait for this run's admitted events, surfacing the first persistence failure."
  [{:keys [tails failures] :as queue} id]
  (loop []
    (let [tail (get @tails id)]
      (when tail (await tail))
      (when-let [failure (get @failures id)] (throw failure))
      (if (identical? tail (get @tails id))
        (do (release! queue id) true)
        (recur)))))

(defn create
  "Return per-run submit/flush/retire functions without exposing native Promise state."
  [write!]
  (let [queue {:tails (atom {}) :failures (atom {})
               :generations (atom {}) :next-generation* (atom 0)}]
    {:submit! (fn [event] (submit! queue write! event))
     :flush! (^:async fn [id] (await (flush! queue id)))
     ;; Terminal abandonment only: the owner has already observed the failure
     ;; through flush, and no further event for this run will be admitted.
     :retire! (fn [id] (release! queue id))}))
