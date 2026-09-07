(ns knoxx.backend.law.translation-split-turn
  "Atomic pre-provider admission for one revision-bound translation turn."
  (:require [knoxx.backend.law.translation-evidence :as evidence]
            [knoxx.backend.law.translation-split-identity :as identity]
            [knoxx.backend.law.translation-split-schema :as schema]))

(defn- execution-digest-input
  "Return a portable order for every executable policy coordinate."
  [execution]
  (cond-> [(:translation-execution/agent-id execution)
           (:translation-execution/model execution)
           (:translation-execution/thinking execution)
           (:translation-execution/system-prompt execution)
           (:translation-execution/tool-ids execution)]
    (contains? execution :translation-execution/tools-choice)
    (conj (:translation-execution/tools-choice execution))))

(defn execution-snapshot
  "Authenticate and digest the exact agent policy this turn will execute."
  [digest-hex {:keys [agent-id model thinking system-prompt tool-ids tools-choice]}]
  (let [facts (cond-> {:translation-execution/agent-id agent-id
                       :translation-execution/model model
                       :translation-execution/thinking (name thinking)
                       :translation-execution/system-prompt system-prompt
                       :translation-execution/tool-ids (vec tool-ids)}
                tools-choice
                (assoc :translation-execution/tools-choice tools-choice))]
    (schema/assert-valid!
     :translation-split/execution-snapshot
     schema/TranslationExecutionSnapshot
     (assoc facts :translation-execution/digest
            (digest-hex (pr-str (execution-digest-input facts)))))))

(defn assert-execution-integrity!
  "Recompute an execution snapshot digest before trusting its provenance."
  [digest-hex execution]
  (let [checked (schema/assert-valid! :translation-split/execution-snapshot
                                      schema/TranslationExecutionSnapshot
                                      execution)
        expected (digest-hex (pr-str (execution-digest-input checked)))]
    (when-not (= expected (:translation-execution/digest checked))
      (throw (ex-info "translation execution snapshot digest is invalid"
                      {:expected expected
                       :actual (:translation-execution/digest checked)})))
    checked))

(defn memory-snapshot
  "Close one truthful memory lookup outcome for durable turn admission."
  [{:keys [status examples error]}]
  (schema/assert-valid!
   :translation-split/memory-snapshot
   schema/TranslationMemorySnapshot
   (cond-> {:translation-memory-snapshot/status status
            :translation-memory-snapshot/examples (vec examples)}
     (some? error) (assoc :translation-memory-snapshot/error error))))

(defn- memory-example-order
  "Return every immutable coordinate whose bytes a future prompt inherits."
  [example]
  [(:translation-memory/id example)
   (:translation-memory/source-revision example)
   (:translation-memory/split-id example)
   (:translation-memory/split-source-digest example)
   (:translation-memory/candidate-set-id example)
   (:translation-memory/candidate-digest example)
   (:translation-memory/review-receipt-id example)
   (:translation-memory/source-text example)
   (:translation-memory/target-text example)])

(defn- turn-id
  "Derive one admission identity from every provider-visible immutable fact."
  [digest-hex dispatch-key run-id admitted-at manifest claim execution memory]
  (str "translation-turn-"
       (digest-hex
        (pr-str
         [dispatch-key run-id admitted-at
          (:split-manifest/id manifest)
          (:split-manifest/source-digest manifest)
          (:candidate-claim/id claim)
          (:candidate-claim/revision claim)
          (:translation-execution/digest execution)
          (:translation-memory-snapshot/status memory)
          (mapv memory-example-order
                (:translation-memory-snapshot/examples memory))
          (:translation-memory-snapshot/error memory)]))))

(defn translation-turn-admission
  "Build the atomic fact that must exist before provider execution begins."
  [digest-hex {:keys [dispatch-key run-id admitted-at manifest candidate-claim
                      execution memory]}]
  (let [checked-manifest (identity/assert-manifest-integrity! digest-hex manifest)
        checked-claim (identity/assert-claim-integrity!
                       digest-hex checked-manifest candidate-claim)
        checked-execution (assert-execution-integrity! digest-hex execution)
        checked-memory (schema/assert-valid! :translation-split/memory-snapshot
                                             schema/TranslationMemorySnapshot
                                             memory)
        checked-at (schema/assert-valid! :translation-split/admitted-at
                                         evidence/Instant admitted-at)
        id (turn-id digest-hex dispatch-key run-id checked-at checked-manifest
                    checked-claim checked-execution checked-memory)]
    (schema/assert-valid!
     :translation-split/turn-admission
     schema/TranslationTurnAdmission
     {:translation-turn/id id
      :translation-turn/dispatch-key dispatch-key
      :translation-turn/run-id run-id
      :translation-turn/admitted-at checked-at
      :translation-turn/manifest checked-manifest
      :translation-turn/candidate-claim checked-claim
      :translation-turn/execution checked-execution
      :translation-turn/memory checked-memory})))

(defn- admission-input
  "Recover constructor input from one persisted turn for integrity checking."
  [turn]
  {:dispatch-key (:translation-turn/dispatch-key turn)
   :run-id (:translation-turn/run-id turn)
   :admitted-at (:translation-turn/admitted-at turn)
   :manifest (:translation-turn/manifest turn)
   :candidate-claim (:translation-turn/candidate-claim turn)
   :execution (:translation-turn/execution turn)
   :memory (:translation-turn/memory turn)})

(defn assert-turn-integrity!
  "Rebuild every nested identity and the aggregate turn identity."
  [digest-hex turn]
  (let [checked (schema/assert-valid! :translation-split/turn-admission
                                      schema/TranslationTurnAdmission turn)
        expected (translation-turn-admission digest-hex (admission-input checked))]
    (when-not (= expected checked)
      (throw (ex-info "translation turn admission identity is invalid"
                      {:expected expected :actual checked})))
    checked))

(defn dispatch-review-binding
  "Project the dispatch coordinates that own a split review generation.

   Completion time and output revision are intentionally absent. Review and
   projection need to establish that the persisted turn belongs to the current
   dispatch/run and exact translation work. The candidate sink additionally
   binds admission time and claimed output revision before it accepts provider
   bytes; that stronger pre-completion contract remains at the sink."
  [record]
  [(:dispatch/key record)
   (:dispatch/batch-id record)
   (:dispatch/org-id record)
   (:dispatch/project record)
   (:dispatch/garden record)
   (:dispatch/document record)
   (:dispatch/source-locale record)
   (:dispatch/locale record)
   (:dispatch/revision record)])

(defn turn-review-binding
  "Project the same review-generation coordinates from admitted turn authority."
  [turn]
  (let [manifest (:translation-turn/manifest turn)]
    [(:translation-turn/dispatch-key turn)
     (:translation-turn/run-id turn)
     (:split-manifest/org-id manifest)
     (:split-manifest/project manifest)
     (:split-manifest/garden manifest)
     (:split-manifest/document manifest)
     (:split-manifest/source-locale manifest)
     (:split-manifest/target-locale manifest)
     (:split-manifest/source-revision manifest)]))

(defn review-binding-matches?
  "Whether one dispatch still owns this turn's review/projection authority.

   Callers remain responsible for the state they require: review currentness
   accepts only the exact point-read relation, while materialization separately
   requires `:dispatch/completed`. Keeping outcome out of this shared identity
   predicate preserves both boundaries' existing refusal behavior."
  [record turn]
  (= (dispatch-review-binding record)
     (turn-review-binding turn)))
