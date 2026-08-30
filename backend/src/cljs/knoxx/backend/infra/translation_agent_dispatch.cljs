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
  rather than doubled, and a throw means no trigger ran. So a failure here is
  conclusively retriable, and there is nothing to observe or adopt.

  What this path does *not* have is the worker path's recovery from a settled
  batch. `recover-settled-batch!` can re-read a batch and learn that a document
  finished even when the completion bookkeeping was lost; an agent session has
  no equivalent read here, so a claim whose session died mid-run stays in flight
  and needs an operator. That is a real gap and it is recorded rather than
  papered over — see `known-gap` below."
  (:require [knoxx.backend.domain.node.crypto :as crypto]
            [knoxx.backend.domain.translation-evidence :as evidence-domain]
            [knoxx.backend.infra.translation-evidence-store :as store]
            [knoxx.backend.infra.translation-split-store :as split-store]
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
  {:gap/id :translation-agent-dispatch/no-session-recovery
   :gap/summary
   (str "an agent-dispatched translation claim whose session dies mid-run stays"
        " in flight: there is no session read that can settle it, so that"
        " revision needs an operator")
   :gap/consequence
   (str "the publication gate keeps reporting the translation missing while"
        " every later pass answers duplicate")})

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
          examples (await (split-store/applicable-memory!
                           translation-store
                           (memory-scope manifest current-candidate-set-ids)))]
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

(defn- ^:async start-run!
  "Bind the run id to a freshly reserved claim, then announce the work.

   Bound *before* the event is emitted, and that order is the whole reason this
   function exists separately. The trigger's action may start a session that
   submits a pair immediately; `law.translation-dispatch/output-revision`
   requires the run id to already be on the claim, so an event emitted first
   could be answered before the claim could accept the answer — and the
   submission would be refused as `:dispatch-record-missing`, losing work that
   really happened.

   A binding that cannot be recorded refuses to emit at all. That is the
   opposite of the worker path's choice, and correctly so: there, the batch
   existed and was recoverable by its persisted dispatch key, so proceeding kept
   real work. Here nothing has happened yet, so declining costs nothing and
   emitting anyway would start an agent whose output has nowhere to land."
  [{:keys [evidence-store emit! digest-hex] :as deps} record source-content]
  (let [run-id (agent-law/run-id record digest-hex)
        bound (await (store/bind-dispatch-batch! evidence-store record run-id))]
    (if-not bound
      {:dispatch/outcome :dispatch/failed
       :dispatch/record (or (await (store/resolve-dispatch!
                                    evidence-store record
                                    :dispatch/failed
                                    "the run id could not be bound to the claim"))
                            record)
       :dispatch/detail "the run id could not be bound to the claim"}
      (try
        (let [turn (await (admit-translation-turn! deps bound source-content))
              event (assoc (agent-law/translation-needed-event bound turn)
                           :event/id (event-id run-id))
              dispatched (await (emit! event))]
          (cond-> {:dispatch/outcome :dispatch/accepted
                   :dispatch/record bound
                   :translation/run-id run-id}
            (empty? (:matchedTriggers dispatched))
            (assoc :dispatch/detail
                   (str "no enabled trigger subscribes to "
                        agent-law/event-type
                        ", so nothing will translate this claim"))))
        (catch :default err
          ;; Conclusively retriable: a throw from the in-process dispatcher means
          ;; no action ran. See the ns docstring for why this needs none of the
          ;; worker path's observation.
          (let [detail (or (not-empty (str (ex-message err)))
                           "the translation event could not be dispatched")]
            {:dispatch/outcome :dispatch/failed
             :dispatch/record (or (await (store/resolve-dispatch!
                                          evidence-store bound
                                          :dispatch/failed detail))
                                  bound)
             :dispatch/detail detail}))))))

(defn ^:async dispatch-work!
  "Dispatch one derived translation work item to an agent actor, exactly once.

   The claim is taken before the event is emitted, for the reason
   `infra.translation-dispatch/dispatch-work!` gives: emitting first would leave
   a window in which a second pass sees no claim and starts a second session for
   the same revision.

   A pin refusal is decided and deliberately not persisted, matching the worker
   path exactly. A pin refusal is a statement about *current* state — the pinned
   bytes are not the bytes on disk — so restoring them makes it stop applying.
   Recorded as a terminal claim it would outlive its own reason and block a pin
   that had become valid.

   `deps` needs `:evidence-store`, `:clock`, `:emit!` and `:digest-hex`.
   `emit!` receives one already-enveloped event and returns the dispatcher's
   answer. The envelope — the dedup `:event/id` — is added here rather than by
   `law.translation-agent`, whose payload contract is closed on purpose: a law
   that also owned the envelope would have to admit runtime identity fields into
   a shape whose reviewability comes from being closed.

   `digest-hex` is the hash `law.translation-agent/run-id` mints with, injected
   so the law stays free of a runtime dependency.
   `source-content` is the document's bytes at the dispatched revision, read by
   the caller. Passed in rather than read here because the caller has already
   read them to compute the digest the claim is keyed by — reading again would be
   a second read of a file that could have changed in between, which is exactly
   the drift this design removes."
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

          ;; In flight, and unlike the worker path there is nothing to re-read.
          ;; Reported as a duplicate with the gap named, so an operator reading a
          ;; reconcile report can tell "still translating" from "stuck" by the
          ;; claim's age rather than by guessing.
          :in-flight
          {:dispatch/outcome :dispatch/duplicate
           :dispatch/record (:record reservation)
           :dispatch/detail (:gap/summary known-gap)}

          :done
          {:dispatch/outcome :dispatch/duplicate
           :dispatch/record (:record reservation)})))))
