(ns knoxx.backend.infra.translation-agent-structured-output
  "A fail-closed provider completion boundary for admitted translation turns.

  Exact admitted model contracts select Ollama, isolated OpenCode, or offline
  Transformers.js transports. Each yields one untrusted `translated_text`
  value, while this orchestration layer keeps every
  authority coordinate on the Knoxx side of the boundary. The existing
  translation-agent sink remains the only component that can persist candidate
  bytes, settle a dispatch claim, mint a receipt, or project translation
  events.

  Durable candidate prefixes are read before any provider request. A retry
  calls its selected provider only for missing admitted attempts. Sink submission itself is
  replayed once with the exact same pair after an exception; this repairs the
  crash window in which the receipt became durable but its candidate-event
  projection failed, without translating the split again."
  (:require [knoxx.backend.infra.translation-agent-completion :as completion]
            [knoxx.backend.extern.promise :as xpromise]
            [knoxx.backend.extern.translation-agent-structured-output :as xstructured]
            [knoxx.backend.infra.translation-agent-sink :as sink]
            [knoxx.backend.infra.translation-split-store :as split-store]
            [knoxx.backend.law.translation-agent :as agent-law]
            [knoxx.backend.law.translation-dispatch :as dispatch-law]
            [knoxx.backend.law.translation-split :as split-law]))

(def ^:private default-timeout-ms
  ;; Local models may need substantially longer than the generic HTTP
  ;; boundary's 30 seconds for a full Markdown split.
  600000)

(defn- fail!
  [type message data]
  (throw (ex-info message
                  (assoc data :translation-agent-structured-output/error type))))

(defn- dispatch-binding
  [record]
  [(:dispatch/key record)
   (:dispatch/batch-id record)
   (:dispatch/at record)
   (:dispatch/org-id record)
   (:dispatch/project record)
   (:dispatch/garden record)
   (:dispatch/document record)
   (:dispatch/source-locale record)
   (:dispatch/locale record)
   (:dispatch/revision record)
   (dispatch-law/output-revision record)])

(defn- turn-binding
  [turn]
  (let [manifest (:translation-turn/manifest turn)
        claim (:translation-turn/candidate-claim turn)]
    [(:translation-turn/dispatch-key turn)
     (:translation-turn/run-id turn)
     (:translation-turn/admitted-at turn)
     (:split-manifest/org-id manifest)
     (:split-manifest/project manifest)
     (:split-manifest/garden manifest)
     (:split-manifest/document manifest)
     (:split-manifest/source-locale manifest)
     (:split-manifest/target-locale manifest)
     (:split-manifest/source-revision manifest)
     (:candidate-claim/revision claim)]))

(defn- assert-record-turn-binding!
  [record turn]
  (when-not (= (dispatch-binding record) (turn-binding turn))
    (fail! :turn-dispatch-mismatch
           "translation turn does not belong to the supplied dispatch"
           {:expected (dispatch-binding record)
            :actual (turn-binding turn)}))
  turn)

(defn- positive-number
  [value]
  (when (and (number? value) (pos? value)) value))

(defn- completion-timeout-ms
  [config]
  (let [event-timeout (positive-number (:event-agent-turn-timeout-ms config))
        structured-timeout
        (positive-number (:translation-agent-structured-output-timeout-ms config))]
    (if event-timeout
      ;; Recovery still owns the event FIFO slot. Its total budget may be made
      ;; stricter explicitly, but never wider than the deployment's event turn.
      (min event-timeout (or structured-timeout event-timeout))
      (or structured-timeout
          (positive-number (:agent-turn-timeout-ms config))
          default-timeout-ms))))

(defn- timeout-error
  [timeout-ms]
  (ex-info (str "Structured translation completion timed out after " timeout-ms "ms")
           {:translation-agent-structured-output/error :completion-timeout
            :timeout-ms timeout-ms}))

(defn- now-ms
  [deps]
  ((or (:now-ms deps) xstructured/now-ms)))

(defn- remaining-timeout-ms!
  [deps deadline-ms timeout-ms]
  (let [remaining (- deadline-ms (now-ms deps))]
    (when-not (pos? remaining)
      (throw (timeout-error timeout-ms)))
    remaining))

(defn- work-items
  [turn]
  (let [splits (get-in turn [:translation-turn/manifest
                             :split-manifest/splits])
        members (get-in turn [:translation-turn/candidate-claim
                              :candidate-claim/members])]
    (when-not (= (count splits) (count members))
      (fail! :turn-coverage-invalid
             "translation turn split and attempt counts differ"
             {:split-count (count splits) :attempt-count (count members)}))
    (mapv (fn [source-split member]
            (when-not (= [(:split/id source-split) (:split/index source-split)]
                         [(:candidate-claim-member/split-id member)
                          (:candidate-claim-member/split-index member)])
              (fail! :turn-member-mismatch
                     "translation split does not match its admitted attempt"
                     {:split-id (:split/id source-split)
                      :attempt-id
                      (:candidate-claim-member/attempt-id member)}))
            {:source-split source-split :claim-member member})
          splits members)))

(defn- pair-for
  [policies {:keys [source-split claim-member]} translated-text]
  (merge
   (select-keys policies
                [:document_id :garden_id :source_lang :target_lang :org_id])
   {:source_text (:split/source-text source-split)
    :translated_text translated-text
    :segment_index (:split/index source-split)
    :split_id (:split/id source-split)
    :attempt_id (:candidate-claim-member/attempt-id claim-member)}))

(defn- throw-refusal!
  [result]
  (when-let [refusal (:translation/refusal result)]
    (throw (sink/refusal-error refusal)))
  result)

(defn- ^:async submit-pair-with-replay!
  "Submit once, replaying the byte-identical pair once after an exception.

  The replay is intentionally exception-only. A typed refusal is authoritative
  and must fail the turn instead of being retried as though it were transport
  failure."
  [deps policies pair]
  (let [result
        (try
          (await (sink/submit-pair! deps policies pair))
          (catch :default _first-error
            (await (sink/submit-pair! deps policies pair))))]
    (throw-refusal! result)))

(defn- completion-result!
  [result]
  (throw-refusal! result)
  (when-not (:translation/receipt result)
    (fail! :completion-incomplete
           "translation splits did not settle to a durable receipt"
           {:result (dissoc result :translation/receipt)}))
  result)

(defn- stored-pair
  [policies item candidate]
  (pair-for policies item (:candidate/text candidate)))

(defn- ^:async finish-durable-prefix!
  [deps policies turn items candidates-by-attempt]
  (let [last-item (last items)
        attempt-id (get-in last-item
                           [:claim-member
                            :candidate-claim-member/attempt-id])
        candidate (get candidates-by-attempt attempt-id)]
    (when-not (and last-item candidate)
      (fail! :durable-prefix-invalid
             "complete durable prefix has no final candidate"
             {:turn-id (:translation-turn/id turn)}))
    (completion-result!
     (await (submit-pair-with-replay!
             deps policies (stored-pair policies last-item candidate))))))

(defn- ^:async complete-missing!
  [config deps model-id turn policies missing deadline-ms timeout-ms]
  (loop [remaining (seq missing)]
    (let [item (first remaining)
          request-timeout (remaining-timeout-ms!
                           deps deadline-ms timeout-ms)
          translated-text (await (completion/translate!
                                  config deps model-id turn
                                  (:source-split item) request-timeout))
          ;; An injected provider may ignore its request timeout. Refuse its
          ;; late bytes before they can cross the durable sink boundary.
          _ (remaining-timeout-ms! deps deadline-ms timeout-ms)
          result (await (submit-pair-with-replay!
                         deps policies
                         (pair-for policies item translated-text)))]
      (cond
        (:translation/receipt result) result
        (next remaining) (recur (next remaining))
        :else (completion-result! result)))))

(defn- ^:async complete-turn-before-deadline!
  [config {:keys [split-store digest-hex] :as deps} record turn
   deadline-ms timeout-ms]
  (let [checked-record (dispatch-law/assert-record! record)
        checked-turn (split-law/assert-turn-integrity! digest-hex turn)
        _ (assert-record-turn-binding! checked-record checked-turn)
        model-id (completion/model! config checked-turn)
        policies (agent-law/session-policies checked-record checked-turn)
        items (work-items checked-turn)
        candidates (await (split-store/candidate-splits-for-turn!
                           split-store (:translation-turn/id checked-turn)))
        candidates-by-attempt (into {} (map (juxt :candidate/attempt-id identity))
                                    candidates)
        missing (filterv
                 (fn [{:keys [claim-member]}]
                   (not (contains?
                         candidates-by-attempt
                         (:candidate-claim-member/attempt-id claim-member))))
                 items)]
    (if (empty? missing)
      ;; Replaying the final durable pair both settles a full prefix whose
      ;; aggregate set was not written and repairs a completed receipt whose
      ;; event projection previously failed. `:dispatch/completed` by itself is
      ;; never accepted as evidence that projection succeeded.
      (await (finish-durable-prefix!
              deps policies checked-turn items candidates-by-attempt))
      (await (complete-missing!
              config deps model-id checked-turn policies missing
              deadline-ms timeout-ms)))))

(defn complete-turn!
  "Complete an admitted turn through its exact provider; return a receipt or throw.

   Every missing split shares one deadline. Event-triggered recovery is capped by
   the original event turn deadline; an explicit structured timeout can only
   tighten the remaining window."
  [config deps record turn]
  (let [timeout-ms (completion-timeout-ms config)
        started-ms (now-ms deps)
        configured-deadline-ms (+ started-ms timeout-ms)
        inherited-deadline-ms
        (positive-number (:event-turn/deadline-ms deps))
        deadline-ms (if inherited-deadline-ms
                      (min configured-deadline-ms inherited-deadline-ms)
                      configured-deadline-ms)
        remaining-ms (- deadline-ms started-ms)]
    (when-not (pos? remaining-ms)
      (throw (timeout-error 0)))
    ;; Keep the operation expression outside an async `await` form so the timer
    ;; is installed while provider work is still pending.
    (xpromise/with-timeout-error
     (complete-turn-before-deadline!
      config deps record turn deadline-ms remaining-ms)
     remaining-ms
     (timeout-error remaining-ms))))
