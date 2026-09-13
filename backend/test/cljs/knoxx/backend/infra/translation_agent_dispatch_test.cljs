(ns knoxx.backend.infra.translation-agent-dispatch-test
  (:require ["node:fs/promises" :as fs]
            ["node:os" :as os]
            ["node:path" :as path]
            [cljs.test :as test]
            [knoxx.backend.extern.event-queue-fixture :as queue-fixture]
            [knoxx.backend.infra.agent.runner :as agent-runner]
            [knoxx.backend.infra.translation-agent-dispatch :as dispatch]
            [knoxx.backend.infra.translation-agent-sink :as agent-sink]
            [knoxx.backend.infra.translation-evidence-store :as store]
            [knoxx.backend.infra.translation-split-store :as split-store]
            [knoxx.backend.law.translation-agent :as agent-law]
            [knoxx.backend.law.translation-dispatch :as dispatch-law]
            [knoxx.backend.law.translation-split :as split-law]))

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

(test/deftest ^:async claimed-work-binds-its-run-before-the-event-is-emitted
  (let [evidence-store (store/memory-store)
        translation-store (split-store/memory-store digest-hex)
        emitted (atom [])
        result (await (dispatch/dispatch-work!
                       (deps evidence-store emitted
                             :translation-store translation-store)
                                               work context source))
        record (:dispatch/record result)
        event (first @emitted)]

    (test/testing "the work was accepted and exactly one event announced it"
      (test/is (= :dispatch/accepted (:dispatch/outcome result)))
      (test/is (= 1 (count @emitted))))

    (test/testing "the claim carries the run id before the event goes out"
      ;; The ordering the whole namespace exists for: a session that submits
      ;; immediately must find a claim that already names its run.
      (test/is (= (:translation/run-id result) (:dispatch/batch-id record)))
      (test/is (= (:translation/run-id result)
             (get-in event [:event/payload :resource-policies :run_id]))))

    (test/testing "the event is the contract the trigger subscribes to"
      (test/is (= agent-law/event-type (:event/type event)))
      (test/is (= (dispatch/event-id (:translation/run-id result)) (:event/id event))))

    (test/testing "the event carries the pin, so the action forwards one it never builds"
      (test/is (agent-law/contract-backed?
           (get-in event [:event/payload :resource-policies])))
      (test/is (agent-law/split-backed?
           (get-in event [:event/payload :resource-policies])))
      (test/is (= (:dispatch/key record)
             (get-in event [:event/payload :resource-policies :dispatch_key]))))

    (test/testing "the full turn exists before the event can name it"
      (let [turn (await (split-store/turn-for-run!
                         translation-store (:translation/run-id result)))]
        (test/is (= (:translation-turn/id turn)
               (get-in event [:event/payload :turn-id])))
        (test/is (= (count (get-in turn [:translation-turn/manifest
                                    :split-manifest/splits]))
               (get-in event [:event/payload :split-count])))))

    (test/testing "the output revision is derivable from the bound claim"
      ;; Without the binding this throws, which is what would make a fast
      ;; submission unjoinable.
      (test/is (string? (dispatch-law/output-revision record))))))

(test/deftest ^:async asking-twice-does-not-start-two-sessions
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

    (test/testing "the second pass re-announces but the live dispatcher owns it once"
      (test/is (= :dispatch/accepted (:dispatch/outcome first-pass)))
      (test/is (= :dispatch/duplicate (:dispatch/outcome second-pass)))
      (test/is (= 2 (count @emitted)))
      (test/is (= 1 (count @triggered)))
      (test/is (= (mapv :event/id @emitted)
             [(first @triggered) (first @triggered)])))

    (test/testing "the duplicate explains why replay is safe across a restart"
      (test/is (re-find #"already owned by this live process"
                   (:dispatch/detail second-pass))))))

(test/deftest ^:async a-restart-replays-process-lost-queued-work-with-the-same-turn
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
    (test/testing "a fresh process ledger accepts the durable attempt again"
      (test/is (= :dispatch/accepted (:dispatch/outcome replay)))
      (test/is (= run-id (:translation/run-id replay)))
      (test/is (= [(dispatch/event-id run-id)] @first-triggered))
      (test/is (= [(dispatch/event-id run-id)] @restarted-triggered)))

    (test/testing "replay reuses the immutable turn and its pinned dictionary"
      (test/is (= original-turn
             (await (split-store/turn-for-run! translation-store run-id))))
      (test/is (= (first @first-events) (first @restarted-events))))))

(test/deftest ^:async recovery-settles-a-complete-durable-set-without-provider-rerun
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
          (test/testing "recovery settles the first durable candidate set directly"
            (test/is (= :dispatch/completed (:dispatch/outcome recovered)))
            (test/is (some? (:translation/receipt recovered)))
            (test/is (= 1 (count receipts)))
            (test/is (= :dispatch/completed
                   (:dispatch/outcome
                    (await (store/dispatch-for-key!
                            evidence-store
                            (get-in first-pass [:dispatch/record
                                                :dispatch/key])))))))
          (test/testing "no second provider event is emitted"
            (test/is (empty? @provider-events))
            (test/is (= 1 (count @candidate-events)))
            (test/is (re-find #"without rerunning"
                         (:dispatch/detail recovered))))))
      (finally
        (await (.rm fs temp-root #js {:recursive true :force true}))))))

(test/deftest ^:async replay-repairs-a-crash-between-claim-and-run-binding
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
    (test/is (= :reserved (:reservation/status reservation)))
    (test/testing "the same attempt receives its deterministic run binding"
      (test/is (= :dispatch/accepted (:dispatch/outcome replay)))
      (test/is (= (:dispatch/attempt-id record)
             (:dispatch/attempt-id recovered)))
      (test/is (= (agent-law/run-id record digest-hex) run-id))
      (test/is (= run-id (:dispatch/batch-id recovered))))

    (test/testing "the missing turn is admitted before its recovered event"
      (test/is (some? (await (split-store/turn-for-run!
                         translation-store run-id))))
      (test/is (= run-id
             (get-in (first @emitted)
                     [:event/payload :resource-policies :run_id]))))))

(test/deftest ^:async replay-repairs-a-crash-between-run-binding-and-turn-admission
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
    (test/is (nil? turn-before))
    (test/testing "replay completes the same bound attempt instead of minting another"
      (test/is (= :dispatch/accepted (:dispatch/outcome replay)))
      (test/is (= (:dispatch/attempt-id bound)
             (get-in replay [:dispatch/record :dispatch/attempt-id])))
      (test/is (= run-id (:translation/run-id replay))))

    (test/testing "the recovered event can only be built after the turn is durable"
      (test/is (some? (await (split-store/turn-for-run!
                         translation-store run-id))))
      (test/is (= (dispatch/event-id run-id)
             (:event/id (first @emitted)))))))

(test/deftest ^:async an-agent-runner-fails-a-claim-bound-to-a-legacy-worker
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
    (test/testing "the incompatible owner becomes an explicit retriable failure"
      (test/is (= :dispatch/failed (:dispatch/outcome first-agent-pass)))
      (test/is (= legacy-batch-id
             (get-in first-agent-pass [:dispatch/record :dispatch/batch-id])))
      (test/is (re-find #"non-agent producer run"
                   (:dispatch/detail first-agent-pass)))
      (test/is (= :dispatch/failed
             (:dispatch/outcome
              (await (store/dispatch-for-key!
                      evidence-store (:dispatch/key bound))))))
      (test/is (empty? @emitted)))

    (test/testing "the next admission replaces the failed worker attempt"
      (let [retry (await (dispatch/dispatch-work!
                          (deps evidence-store emitted
                                :translation-store translation-store)
                          work context source))]
        (test/is (= :dispatch/accepted (:dispatch/outcome retry)))
        (test/is (not= legacy-batch-id (:translation/run-id retry)))
        (test/is (= 1 (count @emitted)))))))

(test/deftest ^:async a-rejected-provider-turn-makes-the-exact-claim-retriable
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
    (test/is (fn? settle!))
    (await (settle! {:event-turn/status :failed
                     :event-turn/detail "provider unavailable"}))

    (test/testing "the full-turn callback releases only its exact accepted attempt"
      (let [failed (await (store/dispatch-for-key!
                           evidence-store
                           (get-in first-pass [:dispatch/record :dispatch/key])))]
        (test/is (= :dispatch/failed (:dispatch/outcome failed)))
        (test/is (re-find #"provider unavailable" (:dispatch/detail failed)))))

    (test/testing "the next reconciliation creates a fresh attempt and event"
      (let [retry (await (dispatch/dispatch-work! d work context source))]
        (test/is (= :dispatch/accepted (:dispatch/outcome retry)))
        (test/is (not= first-run-id (:translation/run-id retry)))
        (test/is (not= first-event-id
                  (:event/id (last @emitted))))))))

(test/deftest ^:async a-no-tool-settlement-can-complete-only-through-the-durable-sink
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
        (test/is (fn? settle!))
        ;; Model the exact Ollama failure this regression exposed: the generic
        ;; turn returned prose but no structured tool call.
        (await (settle! {:event-turn/status :failed
                         :event-turn/deadline-ms 301000
                         :event-turn/detail "required_tool_call_missing"}))
        (let [record (await (store/dispatch-for-batch! evidence-store run-id))
              receipts (await (store/completed-translations!
                               evidence-store {:org-id "open-hax"
                                               :project nil}))]
          (test/testing "only canonical sink completion satisfies the obligation"
            (test/is (= :dispatch/completed (:dispatch/outcome record)))
            (test/is (= 1 (count receipts)))
            (test/is (= [301000] @recovery-deadlines))
            (test/is (= 1 (count @candidate-events))))))
      (finally
        (await (.rm fs temp-root #js {:recursive true :force true}))))))

(test/deftest ^:async settlement-replays-a-completed-receipt-after-event-projection-fails
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
        (test/is (= "candidate event store unavailable" (ex-message first-error)))
        (test/is (fn? settle!))
        (await (settle! {:event-turn/status :failed
                         :event-turn/detail "ordinary tool turn failed"}))
        (let [record (await (store/dispatch-for-batch! evidence-store run-id))
              receipts (await (store/completed-translations!
                               evidence-store {:org-id "open-hax"
                                               :project nil}))]
          (test/testing "a projection exception still reaches exact terminal replay"
            (test/is (= :dispatch/completed (:dispatch/outcome record)))
            (test/is (= 1 @structured-recoveries))
            (test/is (= 3 (count @projections)))
            (test/is (apply = @projections)))
          (test/testing "repair preserves the one immutable receipt"
            (test/is (= 1 (count receipts))))))
      (finally
        (await (.rm fs temp-root #js {:recursive true :force true}))))))

(test/deftest ^:async a-rejected-settlement-callback-is-redelivered-on-reconciliation
  (await (queue-fixture/with-queue!
          (^:async fn []
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
    (await (agent-runner/enqueue-event-turn!
     {:llmModel "test-model" :collection-name "test"}
     {:run-id (:translation/run-id first-pass)
      :conversation-id (:translation/run-id first-pass)
      :session-id (:translation/run-id first-pass)
      :message "translate"
      :agent-spec {:trigger-id "publication-translation"
                   :event-id first-event-id}}
     (fn [] (js/Promise.reject (js/Error. "provider unavailable")))))
    (await (queue-fixture/wait-idle!))

    (test/testing "the rejected callback leaves the original claim accepted"
      (test/is (= :dispatch/accepted
             (:dispatch/outcome
              (await (store/dispatch-for-key!
                      evidence-store
                      (get-in first-pass [:dispatch/record :dispatch/key])))))))

    (let [reconciliation (await (dispatch/dispatch-work!
                                 d work context source))]
      (test/testing "reconciliation redelivers the cached provider failure"
        (test/is (= :dispatch/failed (:dispatch/outcome reconciliation)))
        (test/is (re-find #"provider unavailable"
                     (:dispatch/detail reconciliation)))
        (test/is (= :dispatch/failed
               (:dispatch/outcome
                (await (store/dispatch-for-key!
                        evidence-store
                        (get-in first-pass [:dispatch/record :dispatch/key]))))))
        (test/is (= 2 @settlement-deliveries))))

    (test/testing "a skipped old event does not retain its re-armed callback"
      (await (agent-runner/enqueue-event-turn!
       {:llmModel "test-model" :collection-name "test"}
       {:run-id (:translation/run-id first-pass)
        :conversation-id (:translation/run-id first-pass)
        :session-id (:translation/run-id first-pass)
        :message "stale event"
        :agent-spec {:trigger-id "publication-translation"
                     :event-id first-event-id}}
       (fn [] (js/Promise.resolve {:ok true}))))
      (await (queue-fixture/wait-idle!))
      (test/is (= 2 @settlement-deliveries)))

    (let [retry (await (dispatch/dispatch-work! d work context source))]
      (test/testing "the following claim is a fresh provider attempt"
        (test/is (= :dispatch/accepted (:dispatch/outcome retry)))
        (test/is (not= (:translation/run-id first-pass)
                  (:translation/run-id retry)))
        (test/is (= 2 (count @triggered)))))
    (agent-runner/reset-event-turn-queue!)
    (agent-runner/reset-event-turn-settlers!))))))

(test/deftest ^:async an-event-nobody-subscribes-to-fails-the-retriable-claim
  (let [evidence-store (store/memory-store)
        emitted (atom [])
        result (await (dispatch/dispatch-work!
                       (deps evidence-store emitted :emit-result {:matchedTriggers []})
                       work context source))]
    (test/testing "an emission without a matching trigger is not reported as accepted"
      (test/is (= :dispatch/failed (:dispatch/outcome result)))
      (test/is (re-find #"no enabled trigger subscribes" (:dispatch/detail result))))

    (test/testing "enabling the trigger lets the same work be claimed again"
      (test/is (dispatch-law/retriable?
           (:dispatch/outcome (await (store/dispatch-for-key!
                                      evidence-store
                                      (:dispatch/key (:dispatch/record result)))))))
      (let [retry (await (dispatch/dispatch-work! (deps evidence-store emitted)
                                                   work context source))]
        (test/is (= :dispatch/accepted (:dispatch/outcome retry)))
        (test/is (= 2 (count @emitted)))))))

(test/deftest ^:async a-dispatcher-that-throws-leaves-retriable-work
  (let [evidence-store (store/memory-store)
        emitted (atom [])
        result (await (dispatch/dispatch-work!
                       (deps evidence-store emitted :throw-on-emit true)
                       work context source))]

    (test/testing "the failure is conclusive, because an in-process throw ran no action"
      (test/is (= :dispatch/failed (:dispatch/outcome result)))
      (test/is (re-find #"dispatcher is not running" (:dispatch/detail result))))

    (test/testing "a failed claim is retriable, so the next pass re-announces the work"
      (test/is (dispatch-law/retriable?
           (:dispatch/outcome (await (store/dispatch-for-key!
                                      evidence-store
                                      (:dispatch/key (:dispatch/record result)))))))
      (let [retry (await (dispatch/dispatch-work! (deps evidence-store emitted)
                                                   work context source))]
        (test/is (= :dispatch/accepted (:dispatch/outcome retry)))))))

(test/deftest ^:async a-retry-produces-a-new-run-and-therefore-a-new-output-revision
  (let [evidence-store (store/memory-store)
        emitted (atom [])
        failed (await (dispatch/dispatch-work!
                       (deps evidence-store emitted :throw-on-emit true)
                       work context source))
        retried (await (dispatch/dispatch-work!
                        (assoc (deps evidence-store emitted)
                               :clock (constantly "2026-08-26T17:00:00.000Z"))
                        work context source))]

    (test/testing "the retry reuses the dispatch key but not the run"
      (test/is (= (:dispatch/key (:dispatch/record failed))
             (:dispatch/key (:dispatch/record retried))))
      (test/is (not= (:dispatch/batch-id (:dispatch/record failed))
                (:dispatch/batch-id (:dispatch/record retried)))))

    (test/testing "so an approval of the first translation cannot authorize the second"
      (test/is (not= (dispatch-law/output-revision (:dispatch/record retried))
                (str (:dispatch/revision (:dispatch/record retried))
                     "+de@" (:dispatch/batch-id (:dispatch/record failed))))))))

(test/deftest ^:async a-pin-that-names-bytes-nobody-has-is-refused-and-not-persisted
  (let [evidence-store (store/memory-store)
        emitted (atom [])
        pinned {:document :open-hax.documents/promethean
                :locale :de
                :revision "sha256-a-revision-that-was-never-on-disk"
                :replace-stale? false}
        result (await (dispatch/dispatch-work! (deps evidence-store emitted)
                                               pinned context source))]

    (test/testing "the refusal is terminal, because no retry makes a pin resolvable"
      (test/is (= dispatch-law/unreachable-outcome (:dispatch/outcome result)))
      (test/is (some? (:translation/refusal result)))
      (test/is (empty? @emitted)))

    (test/testing "nothing was written, so restoring the pinned bytes unblocks it"
      ;; A pin refusal is a statement about current state, not an observed fact.
      (test/is (nil? (await (store/dispatch-for-key!
                        evidence-store
                        (:dispatch/key (:dispatch/record result)))))))))
