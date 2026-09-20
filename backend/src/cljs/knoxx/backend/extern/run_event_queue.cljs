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

(defn- forget!
  "Drop every entry this run owns, once its tail is known to have settled."
  [{:keys [generations tails failures]} id]
  (swap! generations dissoc id)
  (swap! tails dissoc id)
  (swap! failures dissoc id)
  nil)

(defn- ^:async release-when-settled!
  "Release a retired incarnation's tail once its in-flight write lands.
   The tail never rejects, so this cannot produce an unobserved rejection."
  [{:keys [tails]} id tail]
  (await tail)
  ;; A successor may already own this run ID; release only the retired tail.
  (when (identical? tail (get @tails id))
    (swap! tails dissoc id))
  nil)

(defn- retire!
  "Abandon this run: its generation and failure go now, its tail once settled.

   A write issued before retirement cannot be recalled, so dropping the tail
   immediately would let a successor reusing this run ID interleave with the
   abandoned incarnation. Holding the tail until it settles keeps the successor
   ordered behind that write, and releases the entry as soon as it lands."
  [{:keys [generations failures tails] :as queue} id]
  (swap! generations dissoc id)
  (swap! failures dissoc id)
  (if-let [tail (get @tails id)]
    (release-when-settled! queue id tail)
    (forget! queue id))
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
        (do (forget! queue id) true)
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
     :retire! (fn [id] (retire! queue id))}))
