(ns knoxx.backend.shape.event-turn-queue
  "Pure FIFO reservation and release transitions over caller-owned queue entries.")

(defn initial-state
  "Construct empty limiter bookkeeping for explicit concurrency and queue limits."
  [concurrency queue-limit]
  {:active [] :pending [] :concurrency concurrency :queue-limit queue-limit})

(defn snapshot
  "Expose queue coordinates without opaque execution or admission handles."
  [{:keys [active pending concurrency queue-limit]}]
  {:active (count active) :queued (count pending)
   :concurrency concurrency :queue-limit queue-limit
   :active-run-ids (mapv #(get-in % [:body :run-id]) active)
   :queued-run-ids (mapv #(get-in % [:body :run-id]) pending)
   :restart-aware false})

(defn reservation
  "Reserve FIFO position synchronously, before the caller begins durable admission."
  [state entry concurrency queue-limit]
  (let [configured (assoc state :concurrency concurrency :queue-limit queue-limit)
        start-now? (and (empty? (:pending configured)) (< (count (:active configured)) concurrency))
        queue-full? (and (not start-now?) (>= (count (:pending configured)) queue-limit))
        after (cond start-now? (update configured :active conj entry)
                    queue-full? configured
                    :else (update configured :pending conj entry))
        status (cond start-now? :running queue-full? :full :else :queued)]
    {:after after :queue-full? queue-full?
     :result {:status status :position (if (= :queued status) (count (:pending after)) 0)
              :snapshot (snapshot after)}}))

(defn release-entry
  "Remove one exact active or pending owner and promote at most its FIFO successor."
  [state queue-id]
  (let [owned? #(= queue-id (:queue-id %))
        active? (some owned? (:active state))
        pending? (some owned? (:pending state))]
    (when (or active? pending?)
      (let [active (filterv (complement owned?) (:active state))
            pending (filterv (complement owned?) (:pending state))
            next-entry (when active? (first pending))
            after (cond-> (assoc state :active active :pending (if next-entry (subvec pending 1) pending))
                    next-entry (update :active conj next-entry))]
        {:after after :next-entry next-entry}))))


(defn response-queue-metadata
  "Render the existing queue response field spellings."
  [{:keys [status position] queue-snapshot :snapshot}]
  {:status (name status)
   :position position
   :active (:active queue-snapshot)
   :queued (:queued queue-snapshot)
   :concurrency (:concurrency queue-snapshot)
   :queue_limit (:queue-limit queue-snapshot)
   :restart_aware (:restart-aware queue-snapshot)})

(defn queued-agent-spec-summary
  "Project the existing agent audit fields into queue settings."
  [agent-spec]
  (when agent-spec
    (cond-> {}
      (:contract-id agent-spec) (assoc :contractId (:contract-id agent-spec))
      (:actor-id agent-spec) (assoc :actorId (:actor-id agent-spec))
      (:model agent-spec) (assoc :model (:model agent-spec))
      (:thinking-level agent-spec) (assoc :thinkingLevel (:thinking-level agent-spec))
      (:tools-choice agent-spec) (assoc :toolsChoice (:tools-choice agent-spec))
      (:trigger-id agent-spec) (assoc :triggerId (:trigger-id agent-spec))
      (:event-type agent-spec) (assoc :eventType (:event-type agent-spec))
      (seq (:event-types agent-spec)) (assoc :eventTypes (vec (:event-types agent-spec)))
      (:event-id agent-spec) (assoc :eventId (:event-id agent-spec))
      (:event-scope-id agent-spec) (assoc :eventScopeId (:event-scope-id agent-spec))
      (:schedule-id agent-spec) (assoc :scheduleId (:schedule-id agent-spec)))))
