(ns knoxx.backend.infra.translation-agent-dispatch
  "Dispatching derived translation work to an agent actor instead of a worker.

  The sibling of `infra.translation-dispatch`'s send path, and deliberately a
  sibling rather than a branch inside it. The two share everything that decides
  *whether* work happens — the gate derives it, `law.translation-dispatch` keys
  and claims it, and the completion path mints the receipt — and differ only in
  who is asked to do it. Expressed as a flag inside one function, the worker's
  ambiguous-send recovery would have had to grow a second meaning it does not
  have here.

  ## Why sending is unambiguous on this path

  `infra.translation-dispatch` carries a large amount of machinery for one
  problem: an HTTP POST that throws may still have landed, so a failed create is
  not evidence that no batch exists, and adopting or retrying it wrongly
  translates twice. Emitting an event has no such window. The dispatcher is
  in-process, the event id is derived from the run id so a re-emit is deduped
  rather than doubled, and a throw means the dispatcher did not confirm a
  successful trigger action. So a failure here is retriable, and there is no
  remote batch to observe or adopt.

  Durable replay closes the process-local queue's restart gap. An accepted
  claim is re-announced with the same deterministic event id: the live process'
  dispatcher deduplicates it, while a restarted process has no in-memory event
  owner and enqueues it again. The immutable split turn is reused when present
  and reconstructed only when the process died before admitting it.

  The runner's full-turn settlement callback also moves an exact claim that did
  not complete to retriable failure. That covers provider rejection, tool
  rejection, partial submission, and a successful-looking turn that simply did
  not produce the claimed complete candidate set. What remains is scheduling:
  unlike the worker path there is no internal retry timer, so the next admission
  or reconciliation performs the retry. That narrower gap is recorded rather
  than papered over — see `known-gap` below."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.node.crypto :as crypto]
            [knoxx.backend.domain.translation-evidence :as evidence-domain]
            [knoxx.backend.infra.translation-agent-sink :as agent-sink]
            [knoxx.backend.infra.translation-evidence-store :as store]
            [knoxx.backend.infra.translation-split-store :as split-store]
            [knoxx.backend.infra.translation-dictionary :as translation-dictionary]
            [knoxx.backend.law.translation-agent :as agent-law]
            [knoxx.backend.law.translation-dispatch :as law]
            [knoxx.backend.law.translation-source-split :as source-split]
            [knoxx.backend.law.translation-split :as split-law]))

(def known-gap
  "The recovery an agent-dispatched claim does not have.

   Stated as data so `scripts/verify-publication-epic.sh` can print it every run
   rather than leaving it to be rediscovered. A permanently visible known gap is
   the house rule; a silently narrowed one is how a reviewer approves a
   guarantee that was never made."
  {:gap/id :translation-agent-dispatch/no-autonomous-retry-timer
   :gap/summary
   (str "a failed agent turn is durably made retriable, but Knoxx does not run"
        " an internal translation retry timer; the next document admission,"
        " deployment admission, or explicit reconciliation starts the retry")
   :gap/consequence
   (str "failed work is visible and no longer stranded in flight, but it waits"
        " for the next reconciliation trigger rather than retrying on a timer")})

(defn event-id
  "The dispatcher's dedup key for one attempt's translation-needed event.

   Derived from the run id, which is derived from the claim and its instant. So
   re-emitting the same attempt is deduplicated by
   `domain.event.dispatch/mark-event-dispatched!` instead of starting a second
   session, while a genuinely new attempt — a replaced retriable claim — has a
   new instant, a new run id and therefore a new event."
  [run-id]
  (str "translation-needed-" run-id))

(defn- manifest-input
  "Map one authenticated dispatch and its exact bytes onto split authority."
  [record source-content]
  (cond-> {:org-id (:dispatch/org-id record)
           :garden (:dispatch/garden record)
           :document (:dispatch/document record)
           :source-locale (:dispatch/source-locale record)
           :target-locale (:dispatch/locale record)
           :source-revision (:dispatch/revision record)
           :source-text source-content
           :source-parts (source-split/source-parts source-content)}
    (some? (:dispatch/project record))
    (assoc :project (:dispatch/project record))))

(defn- memory-scope
  "Select prior approved examples without admitting another tenant or locale."
  [manifest current-candidate-set-ids]
  (cond-> {:org-id (:split-manifest/org-id manifest)
           :garden (:split-manifest/garden manifest)
           :source-locale (:split-manifest/source-locale manifest)
           :target-locale (:split-manifest/target-locale manifest)
           :exclude-manifest-id (:split-manifest/id manifest)
           :current-candidate-set-ids current-candidate-set-ids
           :limit 12}
    (some? (:split-manifest/project manifest))
    (assoc :project (:split-manifest/project manifest))))

(defn- ^:async current-candidate-set-ids!
  "Resolve the exact split generations that may supply positive memory.

   Completed receipt selection is the same total order used by publication and
   review inventory. Each split receipt is then point-joined to its current
   completed dispatch attempt a second time. The store already performs this
   visibility join, but repeating it here makes the memory boundary fail closed
   when a replaceable store returns a stale snapshot or ignores its contract.
   Historical receipts without attempt identity can remain publication history;
   they cannot authorize the newer split-memory feature."
  [evidence-store manifest]
  (let [scope {:org-id (:split-manifest/org-id manifest)
               :project (:split-manifest/project manifest)}
        receipts (await (store/completed-translations! evidence-store scope))
        current (evidence-domain/current-receipts receipts)]
    (loop [remaining (seq current)
           candidate-set-ids #{}]
      (if-let [receipt (first remaining)]
        (let [candidate-set-id (:translation/candidate-set-id receipt)
              attempt-id (:translation/dispatch-attempt-id receipt)]
          (if (and candidate-set-id attempt-id)
            (let [record (await (store/dispatch-for-key!
                                 evidence-store
                                 (:translation/dispatch-key receipt)))]
              (recur (next remaining)
                     (cond-> candidate-set-ids
                       (store/receipt-visible-for-dispatch? receipt record)
                       (conj candidate-set-id))))
            (recur (next remaining) candidate-set-ids)))
        candidate-set-ids))))

(defn- ^:async pinned-memory!
  "Read future-translation memory once and preserve failure as turn evidence."
  [evidence-store translation-store manifest]
  (try
    (let [current-candidate-set-ids
          (await (current-candidate-set-ids! evidence-store manifest))
          dictionary (await (translation-dictionary/current!
                             translation-store
                             (memory-scope manifest current-candidate-set-ids)))
          examples (mapv :translation-dictionary/evidence
                         (:translation-dictionary/entries dictionary))]
      (split-law/memory-snapshot {:status (if (seq examples) :found :empty)
                                  :examples examples}))
    (catch :default err
      (split-law/memory-snapshot
       {:status :failed
        :examples []
        :error (or (some-> (ex-message err) str not-empty)
                   "translation memory retrieval failed")}))))

(defn- ^:async admit-translation-turn!
  "Persist the complete turn aggregate before any provider event is emitted."
  [{:keys [evidence-store split-store digest-hex translation-execution]}
   record source-content]
  (when-not split-store
    (throw (ex-info "translation split persistence is not configured"
                    {:dispatch/key (:dispatch/key record)})))
  (let [manifest (split-law/split-manifest
                  digest-hex (manifest-input record source-content))
        claim (split-law/candidate-claim digest-hex manifest
                                         (law/output-revision record))
        memory (await (pinned-memory! evidence-store split-store manifest))
        execution (split-law/assert-execution-integrity!
                   digest-hex translation-execution)
        turn (split-law/translation-turn-admission
              digest-hex
              {:dispatch-key (:dispatch/key record)
               :run-id (:dispatch/batch-id record)
               :admitted-at (:dispatch/at record)
               :manifest manifest
               :candidate-claim claim
               :execution execution
               :memory memory})]
    (await (split-store/admit-turn! split-store turn))))

(defn- ^:async fail-run!
  [evidence-store record detail]
  {:dispatch/outcome :dispatch/failed
   :dispatch/record (or (await (store/resolve-dispatch!
                                evidence-store record :dispatch/failed detail))
                        record)
   :dispatch/detail detail})

(defn- accepted-run
  [bound run-id]
  {:dispatch/outcome :dispatch/accepted
   :dispatch/record bound
   :translation/run-id run-id})

(defn- duplicate-run
  [bound run-id]
  {:dispatch/outcome :dispatch/duplicate
   :dispatch/record bound
   :translation/run-id run-id
   :dispatch/detail
   (str "the translation event is already owned by this live process; durable"
        " replay will enqueue it after a restart")})

(defn- settlement-redelivery-failure
  [record run-id detail]
  {:dispatch/outcome :dispatch/failed
   :dispatch/record record
   :translation/run-id run-id
   :dispatch/detail detail})

(defn- settled-redelivery-run
  [bound run-id current]
  (case (:dispatch/outcome current)
    :dispatch/completed
    {:dispatch/outcome :dispatch/completed
     :dispatch/record current
     :translation/run-id run-id}

    :dispatch/failed
    (settlement-redelivery-failure
     current run-id
     (or (:dispatch/detail current)
         "the cached translation settlement failed"))

    (throw
     (ex-info "cached translation settlement redelivery left the claim unresolved"
              {:dispatch/batch-id (:dispatch/batch-id bound)
               :dispatch/outcome (:dispatch/outcome current)}))))

(defn- turn-settlement-detail
  [settlement]
  (if (= :failed (:event-turn/status settlement))
    (str "translation agent turn failed before completing its claim: "
         (or (:event-turn/detail settlement) "unknown agent turn failure"))
    (str "translation agent turn completed without producing the claimed"
         " complete translation")))

(defn- recovery-error-detail
  [err]
  (or (some-> (ex-message err) str str/trim not-empty)
      "structured translation completion failed"))

(defn- ^:async recovery-attempt!
  [recover!]
  (try
    {:recovery/result (await (recover!))}
    (catch :default err
      {:recovery/error err})))

(defn- ^:async recover-turn-obligation!
  "Try durable settlement first, then the structured provider boundary.

   The second step also runs when durable settlement threw. That exception can
   mean the receipt became immutable before candidate-event projection failed;
   replaying through the structured adapter authenticates the stored final pair
   and repairs that projection without asking the model for new bytes."
  [{:keys [complete-turn!] :as deps} bound turn settlement]
  (let [recovery-deps
        (cond-> deps
          (number? (:event-turn/deadline-ms settlement))
          (assoc :event-turn/deadline-ms
                 (:event-turn/deadline-ms settlement)))
        durable (await (recovery-attempt!
                        #(agent-sink/settle-durable-turn!
                          recovery-deps bound turn)))]
    (if (and (nil? (:recovery/error durable))
             (:recovery/result durable))
      nil
      (if complete-turn!
        (:recovery/error
         (await (recovery-attempt!
                 #(complete-turn! recovery-deps bound turn))))
        (:recovery/error durable)))))

(defn- ^:async settle-bound-turn!
  "Complete a durable translation obligation or make its exact claim retriable.

   A successful `save_translation` completion has already moved the claim to
   `:dispatch/completed`. Before failing any still-accepted turn, recover a
   complete durable prefix and, when production supplied one, run the provider's
   validated structured-output adapter for only the missing admitted splits.

   The adapter cannot declare success: after it returns, the stored dispatch is
   re-read and only the canonical `:dispatch/completed` state satisfies the
   obligation. A rejected provider/schema result, partial submission, or prose
   imitation therefore leaves the exact attempt accepted until this callback
   moves it to retriable failure. A delayed callback cannot touch a replacement
   attempt because `resolve-dispatch!` compares immutable attempt identity, not
   only the shared dispatch key."
  [{:keys [evidence-store] :as deps} bound turn settlement]
  (let [recovery-error (await (recover-turn-obligation!
                               deps bound turn settlement))
        current (await (store/dispatch-for-batch!
                        evidence-store (:dispatch/batch-id bound)))]
    (if (= :dispatch/completed (:dispatch/outcome current))
      ;; A durable receipt is irreversible. Keep callback delivery visibly
      ;; pending if its idempotent event repair still failed; the next admission
      ;; also performs tenant-scoped completed-event repair before new writes.
      (when recovery-error
        (throw recovery-error))
      (await (store/resolve-dispatch!
              evidence-store bound :dispatch/failed
              (str (turn-settlement-detail settlement)
                   (when recovery-error
                     (str "; structured completion failed: "
                          (recovery-error-detail recovery-error))))))))
  true)

(defn- ^:async register-bound-turn-settler!
  [{:keys [register-turn-settler!] :as deps} event-id bound turn]
  (when register-turn-settler!
    (await (register-turn-settler!
            event-id
            (fn [settlement]
              (settle-bound-turn! deps bound turn settlement))))))

(defn- ^:async unregister-bound-turn-settler!
  [{:keys [unregister-turn-settler!]} event-id registered?]
  (when (and registered? unregister-turn-settler!)
    (await (unregister-turn-settler! event-id))))

(defn- ^:async redelivered-settlement-run!
  [{:keys [evidence-store] :as deps} bound run-id event-id registered?]
  (if (:event-turn/redelivery-accepted? registered?)
    (do
      ;; Successful delivery clears the cached result, but registration re-arms
      ;; the callback for a replay that the sealed event will never enqueue.
      (await (unregister-bound-turn-settler! deps event-id registered?))
      (settled-redelivery-run
       bound run-id
       (await (store/dispatch-for-batch!
               evidence-store (:dispatch/batch-id bound)))))
    (settlement-redelivery-failure
     bound run-id
     "cached translation settlement could not be redelivered")))

(defn- ^:async handle-emission-result!
  [{:keys [evidence-store] :as deps} bound run-id event-id' registered? dispatched]
  (cond
    (seq (:matchedTriggers dispatched))
    (accepted-run bound run-id)

    (:skipped dispatched)
    (if (:event-turn/redelivered? registered?)
      (await (redelivered-settlement-run!
              deps bound run-id event-id' registered?))
      (duplicate-run bound run-id))

    :else
    (do
      (await (unregister-bound-turn-settler!
              deps event-id' registered?))
      (await (fail-run!
              evidence-store bound
              (str "no enabled trigger subscribes to " agent-law/event-type
                   ", so nothing will translate this claim"))))))

(defn- ^:async emit-bound-turn!
  [{:keys [emit!] :as deps} bound turn run-id]
  (let [event-id' (event-id run-id)
        event (assoc (agent-law/translation-needed-event bound turn)
                     :event/id event-id')
        registered? (await (register-bound-turn-settler!
                            deps event-id' bound turn))]
    (try
      (let [dispatched (await (emit! event))]
        (await (handle-emission-result!
                deps bound run-id event-id' registered? dispatched)))
      (catch :default err
        (await (unregister-bound-turn-settler! deps event-id' registered?))
        (throw err)))))

(defn- ^:async emit-bound-run!
  [deps bound source-content run-id]
  (let [turn (await (admit-translation-turn! deps bound source-content))]
    (await (emit-bound-turn! deps bound turn run-id))))

(defn- recovered-durable-run
  [bound run-id settled]
  (let [settled-record (or (:dispatch/record settled) bound)]
    (cond-> (assoc settled
                   :dispatch/outcome
                   (cond
                     (:translation/receipt settled) :dispatch/completed
                     (:dispatch/record settled) (:dispatch/outcome settled-record)
                     :else :dispatch/duplicate)
                   :dispatch/record settled-record
                   :translation/run-id run-id)
      (:translation/receipt settled)
      (assoc :dispatch/detail
             (str "settled the complete durable candidate set without"
                  " rerunning the translation provider")))))

(defn- ^:async recover-bound-run!
  "Settle a complete durable prefix, otherwise replay the exact provider event.

   Reusing the persisted turn preserves its original dictionary snapshot. A
   missing turn is the safe bind-before-admission crash window and is completed
   before either recovery path continues."
  [{:keys [split-store] :as deps} bound source-content run-id]
  (let [turn (or (await (split-store/turn-for-run! split-store run-id))
                 (await (admit-translation-turn! deps bound source-content)))
        settled (await (agent-sink/settle-durable-turn! deps bound turn))]
    (if settled
      (recovered-durable-run bound run-id settled)
      (await (emit-bound-turn! deps bound turn run-id)))))

(declare start-run!)

(defn- ^:async recover-run!
  "Repair any accepted-claim crash window, then replay its deterministic event."
  [{:keys [evidence-store digest-hex] :as deps} record source-content]
  (let [expected-run-id (agent-law/run-id record digest-hex)
        bound-run-id (:dispatch/batch-id record)]
    (cond
      (and bound-run-id (not= expected-run-id bound-run-id))
      (await
       (fail-run!
        evidence-store record
        (str "the accepted claim is bound to a non-agent producer run and cannot"
             " be replayed through the translation agent")))

      bound-run-id
      (try
        (await (recover-bound-run! deps record source-content bound-run-id))
        (catch :default err
          (await (fail-run!
                  evidence-store record
                  (or (not-empty (str (ex-message err)))
                      "the accepted translation event could not be replayed")))))

      :else
      ;; The first process can die after the atomic claim but before binding the
      ;; deterministic run. `start-run!` binds exactly that same attempt before
      ;; it admits or emits anything, so an immediate answer remains joinable.
      (await (start-run! deps record source-content)))))

(defn- ^:async start-run!
  "Persist the run binding before emission so an immediate agent answer can
   join its claim. A missing binding or unconfirmed trigger remains retriable."
  [{:keys [evidence-store digest-hex] :as deps} record source-content]
  (let [run-id (agent-law/run-id record digest-hex)
        bound (await (store/bind-dispatch-batch! evidence-store record run-id))]
    (if-not bound
      (await (fail-run! evidence-store record
                        "the run id could not be bound to the claim"))
      (try
        (await (emit-bound-run! deps bound source-content run-id))
        (catch :default err
          ;; There is no remote worker batch to observe or adopt on this path.
          (await (fail-run!
                  evidence-store bound
                  (or (not-empty (str (ex-message err)))
                      "the translation event could not be dispatched"))))))))

(defn ^:async dispatch-work!
  "Claim then dispatch one turn from exact already-digested source bytes."
  [{:keys [clock] :as deps} work context source-content]
  (let [checked-work (law/assert-valid! :translation-dispatch/work law/DerivedWork work)
        pin-refusal (law/pin-refusal checked-work context)
        record (law/dispatch-record checked-work
                                    context
                                    (if pin-refusal
                                      law/unreachable-outcome
                                      :dispatch/accepted)
                                    (clock)
                                    :attempt-id (crypto/random-uuid)
                                    :recovery-reason
                                    (:dispatch/recovery-reason context))]
    (if pin-refusal
      {:dispatch/outcome law/unreachable-outcome
       :dispatch/record record
       :translation/refusal pin-refusal}
      (let [reservation (await (store/reserve-dispatch! (:evidence-store deps) record))]
        (case (:reservation/status reservation)
          :reserved (await (start-run! deps (:record reservation) source-content))

          :in-flight
          (await (recover-run! deps (:record reservation) source-content))

          :done
          {:dispatch/outcome :dispatch/duplicate
           :dispatch/record (:record reservation)})))))
