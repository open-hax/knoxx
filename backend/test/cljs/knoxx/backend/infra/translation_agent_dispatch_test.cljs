(ns knoxx.backend.infra.translation-agent-dispatch-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.agent.runner :as agent-runner]
            [knoxx.backend.infra.translation-agent-dispatch :as dispatch]
            [knoxx.backend.infra.translation-agent-sink :as agent-sink]
            [knoxx.backend.infra.translation-evidence-store :as store]
            [knoxx.backend.infra.translation-split-store :as split-store]
            [knoxx.backend.law.translation-agent :as agent-law]
            [knoxx.backend.law.translation-dispatch :as dispatch-law]
            [knoxx.backend.law.translation-split :as split-law]
            ["node:fs/promises" :as fs]
            ["node:os" :as os]
            ["node:path" :as path]))

(def ^:private work
  {:document :open-hax.documents/promethean
   :locale :de
   :revision "sha256-abc123"
   :replace-stale? false})

(def ^:private context
  {:dispatch/garden "open-hax.gardens/promethean"
   :dispatch/document-wire-id "open-hax.documents/promethean"
   :dispatch/source-locale :en
   :dispatch/org-id "open-hax"
   :dispatch/membership-id "member-1"
   :dispatch/source-digest "sha256-abc123"})

(def ^:private source "Open Hax is a garden for tools, research, art, and systems.")

(defn- digest-hex [value] (str "h" (hash value)))

(defn- flush-promises!
  []
  (js/Promise. (fn [resolve _reject]
                 (js/setTimeout resolve 0))))

(defn- deps
  "Dispatch dependencies over a recording emitter."
  [evidence-store emitted & {:keys [emit-fn emit-result register-turn-settler!
                                    complete-turn! throw-on-emit translation-store
                                    unregister-turn-settler!]
                             :or {translation-store
                                  (split-store/memory-store digest-hex)}}]
  {:evidence-store evidence-store
   :split-store translation-store
   :translation-execution
   (split-law/execution-snapshot
    digest-hex
    {:agent-id "publication_translator"
     :model "gemma4:31b"
     :thinking :medium
     :system-prompt "Translate the admitted source splits."
     :tool-ids ["save_translation"]})
   :clock (constantly "2026-08-26T16:00:00.000Z")
   :digest-hex digest-hex
   :complete-turn! complete-turn!
   :register-turn-settler! register-turn-settler!
   :unregister-turn-settler! unregister-turn-settler!
   :emit! (or emit-fn
              (fn [event]
                (swap! emitted conj event)
                (if throw-on-emit
                  (js/Promise.reject (js/Error. "the dispatcher is not running"))
                  (js/Promise.resolve
                   (or emit-result
                       {:matchedTriggers [:publication/translation-needed]})))))})

(defn- deduplicating-emitter
  "Model one event dispatcher's process-local ownership ledger."
  [emitted triggered seen]
  (fn [event]
    (swap! emitted conj event)
    (let [id (:event/id event)]
      (if (contains? @seen id)
        (js/Promise.resolve {:matchedTriggers [] :skipped true})
        (do
          (swap! seen conj id)
          (swap! triggered conj id)
          (js/Promise.resolve
           {:matchedTriggers [:publication/translation-needed]}))))))

(deftest ^:async claimed-work-binds-its-run-before-the-event-is-emitted
  (let [evidence-store (store/memory-store)
        translation-store (split-store/memory-store digest-hex)
        emitted (atom [])
        result (await (dispatch/dispatch-work!
                       (deps evidence-store emitted
                             :translation-store translation-store)
                                               work context source))
        record (:dispatch/record result)
        event (first @emitted)]

    (testing "the work was accepted and exactly one event announced it"
      (is (= :dispatch/accepted (:dispatch/outcome result)))
      (is (= 1 (count @emitted))))

    (testing "the claim carries the run id before the event goes out"
      ;; The ordering the whole namespace exists for: a session that submits
      ;; immediately must find a claim that already names its run.
      (is (= (:translation/run-id result) (:dispatch/batch-id record)))
      (is (= (:translation/run-id result)
             (get-in event [:event/payload :resource-policies :run_id]))))

    (testing "the event is the contract the trigger subscribes to"
      (is (= agent-law/event-type (:event/type event)))
      (is (= (dispatch/event-id (:translation/run-id result)) (:event/id event))))

    (testing "the event carries the pin, so the action forwards one it never builds"
      (is (agent-law/contract-backed?
           (get-in event [:event/payload :resource-policies])))
      (is (agent-law/split-backed?
           (get-in event [:event/payload :resource-policies])))
      (is (= (:dispatch/key record)
             (get-in event [:event/payload :resource-policies :dispatch_key]))))

    (testing "the full turn exists before the event can name it"
      (let [turn (await (split-store/turn-for-run!
                         translation-store (:translation/run-id result)))]
        (is (= (:translation-turn/id turn)
               (get-in event [:event/payload :turn-id])))
        (is (= (count (get-in turn [:translation-turn/manifest
                                    :split-manifest/splits]))
               (get-in event [:event/payload :split-count])))))

    (testing "the output revision is derivable from the bound claim"
      ;; Without the binding this throws, which is what would make a fast
      ;; submission unjoinable.
      (is (string? (dispatch-law/output-revision record))))))

(deftest ^:async asking-twice-does-not-start-two-sessions
  (let [evidence-store (store/memory-store)
        translation-store (split-store/memory-store digest-hex)
        emitted (atom [])
        triggered (atom [])
        seen (atom #{})
        d (deps evidence-store emitted
                :translation-store translation-store
                :emit-fn (deduplicating-emitter emitted triggered seen))
        first-pass (await (dispatch/dispatch-work! d work context source))
        second-pass (await (dispatch/dispatch-work! d work context source))]

    (testing "the second pass re-announces but the live dispatcher owns it once"
      (is (= :dispatch/accepted (:dispatch/outcome first-pass)))
      (is (= :dispatch/duplicate (:dispatch/outcome second-pass)))
      (is (= 2 (count @emitted)))
      (is (= 1 (count @triggered)))
      (is (= (mapv :event/id @emitted)
             [(first @triggered) (first @triggered)])))

    (testing "the duplicate explains why replay is safe across a restart"
      (is (re-find #"already owned by this live process"
                   (:dispatch/detail second-pass))))))

(deftest ^:async a-restart-replays-process-lost-queued-work-with-the-same-turn
  (let [evidence-store (store/memory-store)
        translation-store (split-store/memory-store digest-hex)
        first-events (atom [])
        first-triggered (atom [])
        first-process-seen (atom #{})
        first-pass (await (dispatch/dispatch-work!
                           (deps evidence-store first-events
                                 :translation-store translation-store
                                 :emit-fn
                                 (deduplicating-emitter first-events
                                                       first-triggered
                                                       first-process-seen))
                           work context source))
        run-id (:translation/run-id first-pass)
        original-turn (await (split-store/turn-for-run!
                              translation-store run-id))
        restarted-events (atom [])
        restarted-triggered (atom [])
        restarted-process-seen (atom #{})
        replay (await (dispatch/dispatch-work!
                       (deps evidence-store restarted-events
                             :translation-store translation-store
                             :emit-fn
                             (deduplicating-emitter restarted-events
                                                   restarted-triggered
                                                   restarted-process-seen))
                       work context source))]
    (testing "a fresh process ledger accepts the durable attempt again"
      (is (= :dispatch/accepted (:dispatch/outcome replay)))
      (is (= run-id (:translation/run-id replay)))
      (is (= [(dispatch/event-id run-id)] @first-triggered))
      (is (= [(dispatch/event-id run-id)] @restarted-triggered)))

    (testing "replay reuses the immutable turn and its pinned dictionary"
      (is (= original-turn
             (await (split-store/turn-for-run! translation-store run-id))))
      (is (= (first @first-events) (first @restarted-events))))))

(deftest ^:async recovery-settles-a-complete-durable-set-without-provider-rerun
  (let [temp-root (await (.mkdtemp fs (.join path (.tmpdir os)
                                             "knoxx-translation-prefix-")))
        evidence-store (store/memory-store)
        translation-store (split-store/memory-store digest-hex)
        first-events (atom [])
        candidate-events (atom [])]
    (try
      (let [first-pass (await (dispatch/dispatch-work!
                               (deps evidence-store first-events
                                     :translation-store translation-store)
                               work context source))
            run-id (:translation/run-id first-pass)
            turn (await (split-store/turn-for-run! translation-store run-id))
            turn-id (:translation-turn/id turn)
            claim (:translation-turn/candidate-claim turn)
            candidates
            (mapv (fn [member]
                    (split-law/candidate-split
                     digest-hex member
                     (str "Dauerhafte Übersetzung "
                          (:candidate-claim-member/split-index member)
                          ".")))
                  (:candidate-claim/members claim))]
        (doseq [candidate candidates]
          (await (split-store/append-candidate-split!
                  translation-store turn-id candidate)))
        (await (split-store/complete-candidate-set!
                translation-store turn-id
                (split-law/complete-candidate-set
                 digest-hex (:translation-turn/manifest turn) claim candidates)))

        ;; Model a fresh process: its provider-event recorder starts empty while
        ;; all candidate evidence from the first process remains durable.
        (let [provider-events (atom [])
              recovery-deps
              (assoc (deps evidence-store provider-events
                           :translation-store translation-store)
                     :content-root temp-root
                     :emit-candidate-events!
                     (fn [projection]
                       (swap! candidate-events conj projection)
                       (js/Promise.resolve {:ok true}))
                     :observe-source-revision
                     (fn [_record] (js/Promise.resolve "sha256-abc123")))
              recovered (await (dispatch/dispatch-work!
                                recovery-deps work context source))
              receipts (await (store/completed-translations!
                               evidence-store {:org-id "open-hax"
                                               :project nil}))]
          (testing "recovery settles the first durable candidate set directly"
            (is (= :dispatch/completed (:dispatch/outcome recovered)))
            (is (some? (:translation/receipt recovered)))
            (is (= 1 (count receipts)))
            (is (= :dispatch/completed
                   (:dispatch/outcome
                    (await (store/dispatch-for-key!
                            evidence-store
                            (get-in first-pass [:dispatch/record
                                                :dispatch/key])))))))
          (testing "no second provider event is emitted"
            (is (empty? @provider-events))
            (is (= 1 (count @candidate-events)))
            (is (re-find #"without rerunning"
                         (:dispatch/detail recovered))))))
      (finally
        (await (.rm fs temp-root #js {:recursive true :force true}))))))

(deftest ^:async replay-repairs-a-crash-between-claim-and-run-binding
  (let [evidence-store (store/memory-store)
        translation-store (split-store/memory-store digest-hex)
        emitted (atom [])
        record (dispatch-law/dispatch-record
                work context :dispatch/accepted
                "2026-08-26T16:00:00.000Z"
                :attempt-id "crashed-before-run-binding")
        reservation (await (store/reserve-dispatch! evidence-store record))
        replay (await (dispatch/dispatch-work!
                       (deps evidence-store emitted
                             :translation-store translation-store)
                       work context source))
        recovered (:dispatch/record replay)
        run-id (:translation/run-id replay)]
    (is (= :reserved (:reservation/status reservation)))
    (testing "the same attempt receives its deterministic run binding"
      (is (= :dispatch/accepted (:dispatch/outcome replay)))
      (is (= (:dispatch/attempt-id record)
             (:dispatch/attempt-id recovered)))
      (is (= (agent-law/run-id record digest-hex) run-id))
      (is (= run-id (:dispatch/batch-id recovered))))

    (testing "the missing turn is admitted before its recovered event"
      (is (some? (await (split-store/turn-for-run!
                         translation-store run-id))))
      (is (= run-id
             (get-in (first @emitted)
                     [:event/payload :resource-policies :run_id]))))))

(deftest ^:async replay-repairs-a-crash-between-run-binding-and-turn-admission
  (let [evidence-store (store/memory-store)
        translation-store (split-store/memory-store digest-hex)
        emitted (atom [])
        record (dispatch-law/dispatch-record
                work context :dispatch/accepted
                "2026-08-26T16:00:00.000Z"
                :attempt-id "crashed-before-turn-admission")
        _ (await (store/reserve-dispatch! evidence-store record))
        run-id (agent-law/run-id record digest-hex)
        bound (await (store/bind-dispatch-batch!
                      evidence-store record run-id))
        turn-before (await (split-store/turn-for-run!
                            translation-store run-id))
        replay (await (dispatch/dispatch-work!
                       (deps evidence-store emitted
                             :translation-store translation-store)
                       work context source))]
    (is (nil? turn-before))
    (testing "replay completes the same bound attempt instead of minting another"
      (is (= :dispatch/accepted (:dispatch/outcome replay)))
      (is (= (:dispatch/attempt-id bound)
             (get-in replay [:dispatch/record :dispatch/attempt-id])))
      (is (= run-id (:translation/run-id replay))))

    (testing "the recovered event can only be built after the turn is durable"
      (is (some? (await (split-store/turn-for-run!
                         translation-store run-id))))
      (is (= (dispatch/event-id run-id)
             (:event/id (first @emitted)))))))

(deftest ^:async an-agent-runner-fails-a-claim-bound-to-a-legacy-worker
  (let [evidence-store (store/memory-store)
        translation-store (split-store/memory-store digest-hex)
        emitted (atom [])
        record (dispatch-law/dispatch-record
                work context :dispatch/accepted
                "2026-08-26T16:00:00.000Z"
                :attempt-id "legacy-worker-attempt")
        _ (await (store/reserve-dispatch! evidence-store record))
        legacy-batch-id "legacy-worker-batch"
        bound (await (store/bind-dispatch-batch!
                      evidence-store record legacy-batch-id))
        first-agent-pass (await (dispatch/dispatch-work!
                                 (deps evidence-store emitted
                                       :translation-store translation-store)
                                 work context source))]
    (testing "the incompatible owner becomes an explicit retriable failure"
      (is (= :dispatch/failed (:dispatch/outcome first-agent-pass)))
      (is (= legacy-batch-id
             (get-in first-agent-pass [:dispatch/record :dispatch/batch-id])))
      (is (re-find #"non-agent producer run"
                   (:dispatch/detail first-agent-pass)))
      (is (= :dispatch/failed
             (:dispatch/outcome
              (await (store/dispatch-for-key!
                      evidence-store (:dispatch/key bound))))))
      (is (empty? @emitted)))

    (testing "the next admission replaces the failed worker attempt"
      (let [retry (await (dispatch/dispatch-work!
                          (deps evidence-store emitted
                                :translation-store translation-store)
                          work context source))]
        (is (= :dispatch/accepted (:dispatch/outcome retry)))
        (is (not= legacy-batch-id (:translation/run-id retry)))
        (is (= 1 (count @emitted)))))))

(deftest ^:async a-rejected-provider-turn-makes-the-exact-claim-retriable
  (let [evidence-store (store/memory-store)
        translation-store (split-store/memory-store digest-hex)
        emitted (atom [])
        settlers (atom {})
        register! (fn [event-id settle!]
                    (swap! settlers assoc event-id settle!)
                    true)
        unregister! (fn [event-id]
                      (swap! settlers dissoc event-id)
                      true)
        d (deps evidence-store emitted
                :translation-store translation-store
                :register-turn-settler! register!
                :unregister-turn-settler! unregister!)
        first-pass (await (dispatch/dispatch-work! d work context source))
        first-run-id (:translation/run-id first-pass)
        first-event-id (dispatch/event-id first-run-id)
        settle! (get @settlers first-event-id)]
    (is (fn? settle!))
    (await (settle! {:event-turn/status :failed
                     :event-turn/detail "provider unavailable"}))

    (testing "the full-turn callback releases only its exact accepted attempt"
      (let [failed (await (store/dispatch-for-key!
                           evidence-store
                           (get-in first-pass [:dispatch/record :dispatch/key])))]
        (is (= :dispatch/failed (:dispatch/outcome failed)))
        (is (re-find #"provider unavailable" (:dispatch/detail failed)))))

    (testing "the next reconciliation creates a fresh attempt and event"
      (let [retry (await (dispatch/dispatch-work! d work context source))]
        (is (= :dispatch/accepted (:dispatch/outcome retry)))
        (is (not= first-run-id (:translation/run-id retry)))
        (is (not= first-event-id
                  (:event/id (last @emitted))))))))

(deftest ^:async a-no-tool-settlement-can-complete-only-through-the-durable-sink
  (let [temp-root (await (.mkdtemp fs (.join path (.tmpdir os)
                                             "knoxx-structured-settlement-")))
        evidence-store (store/memory-store)
        translation-store (split-store/memory-store digest-hex)
        emitted (atom [])
        candidate-events (atom [])
        settlers (atom {})
        recovery-deadlines (atom [])
        register! (fn [event-id settle!]
                    (swap! settlers assoc event-id settle!)
                    true)
        complete!
        (fn [runtime-deps bound turn]
          (swap! recovery-deadlines conj
                 (:event-turn/deadline-ms runtime-deps))
          (let [split (first (get-in turn [:translation-turn/manifest
                                           :split-manifest/splits]))
                member (first (get-in turn [:translation-turn/candidate-claim
                                            :candidate-claim/members]))]
            (agent-sink/submit-pair!
             runtime-deps
             (agent-law/session-policies bound turn)
             {:source_text (:split/source-text split)
              :translated_text
              "Open Hax ist ein Garten fuer Werkzeuge, Forschung, Kunst und Systeme."
              :split_id (:split/id split)
              :attempt_id (:candidate-claim-member/attempt-id member)
              :segment_index (:split/index split)})))
        d (assoc (deps evidence-store emitted
                       :translation-store translation-store
                       :register-turn-settler! register!
                       :complete-turn! complete!)
                 :content-root temp-root
                 :emit-candidate-events!
                 (fn [projection]
                   (swap! candidate-events conj projection)
                   (js/Promise.resolve {:ok true}))
                 :observe-source-revision
                 (fn [_record] (js/Promise.resolve "sha256-abc123")))]
    (try
      (let [started (await (dispatch/dispatch-work! d work context source))
            run-id (:translation/run-id started)
            settle! (get @settlers (dispatch/event-id run-id))]
        (is (fn? settle!))
        ;; Model the exact Ollama failure this regression exposed: the generic
        ;; turn returned prose but no structured tool call.
        (await (settle! {:event-turn/status :failed
                         :event-turn/deadline-ms 301000
                         :event-turn/detail "required_tool_call_missing"}))
        (let [record (await (store/dispatch-for-batch! evidence-store run-id))
              receipts (await (store/completed-translations!
                               evidence-store {:org-id "open-hax"
                                               :project nil}))]
          (testing "only canonical sink completion satisfies the obligation"
            (is (= :dispatch/completed (:dispatch/outcome record)))
            (is (= 1 (count receipts)))
            (is (= [301000] @recovery-deadlines))
            (is (= 1 (count @candidate-events))))))
      (finally
        (await (.rm fs temp-root #js {:recursive true :force true}))))))

(deftest ^:async settlement-replays-a-completed-receipt-after-event-projection-fails
  (let [temp-root (await (.mkdtemp fs (.join path (.tmpdir os)
                                             "knoxx-settlement-event-repair-")))
        evidence-store (store/memory-store)
        translation-store (split-store/memory-store digest-hex)
        emitted (atom [])
        projections (atom [])
        structured-recoveries (atom 0)
        settlers (atom {})
        register! (fn [event-id settle!]
                    (swap! settlers assoc event-id settle!)
                    true)
        emit-candidate-events!
        (fn [projection]
          (swap! projections conj projection)
          (if (< (count @projections) 3)
            (js/Promise.reject (js/Error. "candidate event store unavailable"))
            (js/Promise.resolve {:ok true})))
        submit-final-pair!
        (fn [runtime-deps bound turn]
          (let [split (first (get-in turn [:translation-turn/manifest
                                           :split-manifest/splits]))
                member (first (get-in turn [:translation-turn/candidate-claim
                                            :candidate-claim/members]))]
            (agent-sink/submit-pair!
             runtime-deps
             (agent-law/session-policies bound turn)
             {:source_text (:split/source-text split)
              :translated_text
              "Open Hax ist ein Garten fuer Werkzeuge, Forschung, Kunst und Systeme."
              :split_id (:split/id split)
              :attempt_id (:candidate-claim-member/attempt-id member)
              :segment_index (:split/index split)})))
        complete! (fn [runtime-deps bound turn]
                    (swap! structured-recoveries inc)
                    (submit-final-pair! runtime-deps bound turn))
        d (assoc (deps evidence-store emitted
                       :translation-store translation-store
                       :register-turn-settler! register!
                       :complete-turn! complete!)
                 :content-root temp-root
                 :emit-candidate-events! emit-candidate-events!
                 :observe-source-revision
                 (fn [_record] (js/Promise.resolve "sha256-abc123")))]
    (try
      (let [started (await (dispatch/dispatch-work! d work context source))
            run-id (:translation/run-id started)
            bound (:dispatch/record started)
            turn (await (split-store/turn-for-run! translation-store run-id))
            first-error (try
                          (await (submit-final-pair! d bound turn))
                          nil
                          (catch :default err err))
            settle! (get @settlers (dispatch/event-id run-id))]
        (is (= "candidate event store unavailable" (ex-message first-error)))
        (is (fn? settle!))
        (await (settle! {:event-turn/status :failed
                         :event-turn/detail "ordinary tool turn failed"}))
        (let [record (await (store/dispatch-for-batch! evidence-store run-id))
              receipts (await (store/completed-translations!
                               evidence-store {:org-id "open-hax"
                                               :project nil}))]
          (testing "a projection exception still reaches exact terminal replay"
            (is (= :dispatch/completed (:dispatch/outcome record)))
            (is (= 1 @structured-recoveries))
            (is (= 3 (count @projections)))
            (is (apply = @projections)))
          (testing "repair preserves the one immutable receipt"
            (is (= 1 (count receipts))))))
      (finally
        (await (.rm fs temp-root #js {:recursive true :force true}))))))

(deftest ^:async a-rejected-settlement-callback-is-redelivered-on-reconciliation
  (agent-runner/reset-event-turn-queue!)
  (agent-runner/reset-event-turn-settlers!)
  (let [evidence-store (store/memory-store)
        translation-store (split-store/memory-store digest-hex)
        emitted (atom [])
        triggered (atom [])
        seen (atom #{})
        reject-next? (atom true)
        settlement-deliveries (atom 0)
        register!
        (fn [event-id settle!]
          (agent-runner/register-event-turn-settler!
           event-id
           (fn [settlement]
             (swap! settlement-deliveries inc)
             (if (compare-and-set! reject-next? true false)
               (js/Promise.reject
                (js/Error. "transient evidence-store failure"))
               (settle! settlement)))))
        d (deps evidence-store emitted
                :translation-store translation-store
                :emit-fn (deduplicating-emitter emitted triggered seen)
                :register-turn-settler! register!
                :unregister-turn-settler!
                agent-runner/unregister-event-turn-settler!)
        first-pass (await (dispatch/dispatch-work! d work context source))
        first-event-id (dispatch/event-id (:translation/run-id first-pass))]
    (agent-runner/enqueue-event-turn!
     {:llmModel "test-model" :collection-name "test"}
     {:run-id (:translation/run-id first-pass)
      :conversation-id (:translation/run-id first-pass)
      :session-id (:translation/run-id first-pass)
      :message "translate"
      :agent-spec {:trigger-id "publication-translation"
                   :event-id first-event-id}}
     (fn [] (js/Promise.reject (js/Error. "provider unavailable"))))
    (await (flush-promises!))

    (testing "the rejected callback leaves the original claim accepted"
      (is (= :dispatch/accepted
             (:dispatch/outcome
              (await (store/dispatch-for-key!
                      evidence-store
                      (get-in first-pass [:dispatch/record :dispatch/key])))))))

    (let [reconciliation (await (dispatch/dispatch-work!
                                 d work context source))]
      (testing "reconciliation redelivers the cached provider failure"
        (is (= :dispatch/failed (:dispatch/outcome reconciliation)))
        (is (re-find #"provider unavailable"
                     (:dispatch/detail reconciliation)))
        (is (= :dispatch/failed
               (:dispatch/outcome
                (await (store/dispatch-for-key!
                        evidence-store
                        (get-in first-pass [:dispatch/record :dispatch/key]))))))
        (is (= 2 @settlement-deliveries))))

    (testing "a skipped old event does not retain its re-armed callback"
      (agent-runner/enqueue-event-turn!
       {:llmModel "test-model" :collection-name "test"}
       {:run-id (:translation/run-id first-pass)
        :conversation-id (:translation/run-id first-pass)
        :session-id (:translation/run-id first-pass)
        :message "stale event"
        :agent-spec {:trigger-id "publication-translation"
                     :event-id first-event-id}}
       (fn [] (js/Promise.resolve {:ok true})))
      (await (flush-promises!))
      (is (= 2 @settlement-deliveries)))

    (let [retry (await (dispatch/dispatch-work! d work context source))]
      (testing "the following claim is a fresh provider attempt"
        (is (= :dispatch/accepted (:dispatch/outcome retry)))
        (is (not= (:translation/run-id first-pass)
                  (:translation/run-id retry)))
        (is (= 2 (count @triggered)))))
    (agent-runner/reset-event-turn-queue!)
    (agent-runner/reset-event-turn-settlers!)))

(deftest ^:async an-event-nobody-subscribes-to-fails-the-retriable-claim
  (let [evidence-store (store/memory-store)
        emitted (atom [])
        result (await (dispatch/dispatch-work!
                       (deps evidence-store emitted :emit-result {:matchedTriggers []})
                       work context source))]
    (testing "an emission without a matching trigger is not reported as accepted"
      (is (= :dispatch/failed (:dispatch/outcome result)))
      (is (re-find #"no enabled trigger subscribes" (:dispatch/detail result))))

    (testing "enabling the trigger lets the same work be claimed again"
      (is (dispatch-law/retriable?
           (:dispatch/outcome (await (store/dispatch-for-key!
                                      evidence-store
                                      (:dispatch/key (:dispatch/record result)))))))
      (let [retry (await (dispatch/dispatch-work! (deps evidence-store emitted)
                                                   work context source))]
        (is (= :dispatch/accepted (:dispatch/outcome retry)))
        (is (= 2 (count @emitted)))))))

(deftest ^:async a-dispatcher-that-throws-leaves-retriable-work
  (let [evidence-store (store/memory-store)
        emitted (atom [])
        result (await (dispatch/dispatch-work!
                       (deps evidence-store emitted :throw-on-emit true)
                       work context source))]

    (testing "the failure is conclusive, because an in-process throw ran no action"
      (is (= :dispatch/failed (:dispatch/outcome result)))
      (is (re-find #"dispatcher is not running" (:dispatch/detail result))))

    (testing "a failed claim is retriable, so the next pass re-announces the work"
      (is (dispatch-law/retriable?
           (:dispatch/outcome (await (store/dispatch-for-key!
                                      evidence-store
                                      (:dispatch/key (:dispatch/record result)))))))
      (let [retry (await (dispatch/dispatch-work! (deps evidence-store emitted)
                                                   work context source))]
        (is (= :dispatch/accepted (:dispatch/outcome retry)))))))

(deftest ^:async a-retry-produces-a-new-run-and-therefore-a-new-output-revision
  (let [evidence-store (store/memory-store)
        emitted (atom [])
        failed (await (dispatch/dispatch-work!
                       (deps evidence-store emitted :throw-on-emit true)
                       work context source))
        retried (await (dispatch/dispatch-work!
                        (assoc (deps evidence-store emitted)
                               :clock (constantly "2026-08-26T17:00:00.000Z"))
                        work context source))]

    (testing "the retry reuses the dispatch key but not the run"
      (is (= (:dispatch/key (:dispatch/record failed))
             (:dispatch/key (:dispatch/record retried))))
      (is (not= (:dispatch/batch-id (:dispatch/record failed))
                (:dispatch/batch-id (:dispatch/record retried)))))

    (testing "so an approval of the first translation cannot authorize the second"
      (is (not= (dispatch-law/output-revision (:dispatch/record retried))
                (str (:dispatch/revision (:dispatch/record retried))
                     "+de@" (:dispatch/batch-id (:dispatch/record failed))))))))

(deftest ^:async a-pin-that-names-bytes-nobody-has-is-refused-and-not-persisted
  (let [evidence-store (store/memory-store)
        emitted (atom [])
        pinned {:document :open-hax.documents/promethean
                :locale :de
                :revision "sha256-a-revision-that-was-never-on-disk"
                :replace-stale? false}
        result (await (dispatch/dispatch-work! (deps evidence-store emitted)
                                               pinned context source))]

    (testing "the refusal is terminal, because no retry makes a pin resolvable"
      (is (= dispatch-law/unreachable-outcome (:dispatch/outcome result)))
      (is (some? (:translation/refusal result)))
      (is (empty? @emitted)))

    (testing "nothing was written, so restoring the pinned bytes unblocks it"
      ;; A pin refusal is a statement about current state, not an observed fact.
      (is (nil? (await (store/dispatch-for-key!
                        evidence-store
                        (:dispatch/key (:dispatch/record result)))))))))
