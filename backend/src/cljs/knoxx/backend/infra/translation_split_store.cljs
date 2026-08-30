(ns knoxx.backend.infra.translation-split-store
  "Persistence boundary for atomic translation turns and their child evidence.

  A provider may run only after `admit-turn!` has atomically persisted the
  dispatch/run binding, canonical manifest, candidate claim, execution policy,
  and pinned memory snapshot as one immutable aggregate. Candidate members,
  complete sets, and review receipts are monotonic child facts. Equal retries
  return the first fact; changed reuse of an immutable identity conflicts."
  (:require [clojure.string :as str]
            [knoxx.backend.law.translation-split :as split]
            [knoxx.backend.law.translation-split-schema :as schema]))

(defprotocol ITranslationSplitStore
  (admit-turn! [store turn]
    "Atomically admit one complete pre-provider turn aggregate.")
  (turn-for-run! [store run-id]
    "Read one authenticated turn by its exact provider run id, or nil.")
  (turn-by-id! [store turn-id]
    "Read one authenticated turn by its exact turn id, or nil.")
  (append-candidate-split! [store turn-id candidate]
    "Append one immutable candidate bound to the turn's admitted claim.")
  (candidate-splits-for-turn! [store turn-id]
    "Read authenticated candidate members in the turn's admitted order.")
  (complete-candidate-set! [store turn-id candidate-set]
    "Admit a complete set only after exact durable candidate coverage exists.")
  (candidate-set-for-turn! [store turn-id]
    "Read the authenticated complete candidate set for one turn, or nil.")
  (candidate-set-by-id! [store candidate-set-id]
    "Read one authenticated complete candidate set by exact id, or nil.")
  (turn-for-candidate-set! [store candidate-set-id]
    "Read the authenticated owning turn for one exact candidate set, or nil.")
  (append-review-receipt! [store receipt]
    "Append immutable review evidence; first timestamp wins an equal retry.")
  (review-history-for-split! [store candidate-set-id split-id]
    "Read complete authenticated history for one exact candidate-set split.")
  (applicable-memory! [store scope]
    "Derive bounded current approved examples for a future translation scope.

     `scope` must carry the exact candidate-set ids selected from current,
     dispatch-visible completion evidence. Requiring that server-owned allowset
     keeps an old but still durable approval from becoming positive memory after
     its dispatch generation has been replaced."))

(def ^:private empty-state
  {:turns {}
   :turn-by-run {}
   :candidate-splits {}
   :candidate-sets {}
   :candidate-set-turns {}
   :review-receipts {}})

(defn immutable-conflict!
  "Refuse changed evidence behind one already-claimed immutable identity."
  [kind identity existing attempted]
  (throw (ex-info (str "immutable " kind " identity conflicts with stored evidence")
                  {:translation-split-store/conflict :immutable-identity
                   :translation-split-store/kind kind
                   :translation-split-store/id identity
                   :translation-split-store/existing existing
                   :translation-split-store/attempted attempted})))

(defn inconsistent-index!
  "Refuse an internal point index that no longer names its aggregate fact."
  [kind identity]
  (throw (ex-info (str "translation split store " kind " index is inconsistent")
                  {:translation-split-store/conflict :inconsistent-index
                   :translation-split-store/kind kind
                   :translation-split-store/id identity})))

(defn- put-immutable-in-state
  "Atomically admit `value` at `path`, return equal evidence, or conflict."
  [state path kind identity value]
  (if-some [existing (get-in state path)]
    (if (= existing value)
      (assoc state :answer existing)
      (immutable-conflict! kind identity existing value))
    (-> state
        (assoc-in path value)
        (assoc :answer value))))

(defn- swap-answer!
  "Run one indivisible memory transition and return its recorded answer."
  [state transition & args]
  (:answer (apply swap! state transition args)))

(defn- checked-turn-in-state
  "Read one turn and recompute its full nested identity chain."
  [digest-hex state turn-id]
  (when-let [turn (get-in state [:turns turn-id])]
    (split/assert-turn-integrity! digest-hex turn)))

(defn- required-turn-in-state
  "Read an authenticated turn or refuse an orphan child fact."
  [digest-hex state turn-id]
  (or (checked-turn-in-state digest-hex state turn-id)
      (throw (ex-info "translation turn is not persisted"
                      {:translation-turn/id turn-id}))))

(defn- admit-turn-in-state
  "Atomically claim both aggregate identity and provider run identity."
  [state turn]
  (let [turn-id (:translation-turn/id turn)
        run-id (:translation-turn/run-id turn)
        by-id (get-in state [:turns turn-id])
        indexed-turn-id (get-in state [:turn-by-run run-id])
        by-run (get-in state [:turns indexed-turn-id])]
    (cond
      (and (= by-id turn) (= indexed-turn-id turn-id) (= by-run turn))
      (assoc state :answer by-id)

      (or by-id indexed-turn-id)
      (immutable-conflict! "translation turn" [turn-id run-id]
                           (or by-run by-id) turn)

      :else
      (-> state
          (assoc-in [:turns turn-id] turn)
          (assoc-in [:turn-by-run run-id] turn-id)
          (assoc :answer turn)))))

(defn- turn-for-run-in-state
  "Resolve a run index only when it agrees with the authenticated aggregate."
  [digest-hex state run-id]
  (when-let [turn-id (get-in state [:turn-by-run run-id])]
    (let [turn (checked-turn-in-state digest-hex state turn-id)]
      (when-not (= run-id (:translation-turn/run-id turn))
        (inconsistent-index! "run" run-id))
      turn)))

(defn- candidate-binding
  "Return all immutable attempt coordinates shared by claims and candidates."
  [value]
  [(or (:candidate-claim-member/attempt-id value)
       (:candidate/attempt-id value))
   (or (:candidate-claim-member/split-id value)
       (:candidate/split-id value))
   (or (:candidate-claim-member/split-index value)
       (:candidate/split-index value))
   (or (:candidate-claim-member/source-digest value)
       (:candidate/source-digest value))])

(defn- claim-member-for-attempt
  "Find the exact pre-admitted member named by one candidate attempt."
  [claim attempt-id]
  (some #(when (= attempt-id (:candidate-claim-member/attempt-id %)) %)
        (:candidate-claim/members claim)))

(defn checked-candidate-for-turn
  "Authenticate candidate bytes and every coordinate inherited from its turn."
  [digest-hex turn candidate]
  (let [checked (split/assert-candidate-integrity! digest-hex candidate)
        claim (:translation-turn/candidate-claim turn)
        member (claim-member-for-attempt claim (:candidate/attempt-id checked))]
    (when-not (and member (= (candidate-binding member)
                             (candidate-binding checked)))
      (throw (ex-info "translation candidate does not match its admitted turn"
                      {:translation-turn/id (:translation-turn/id turn)
                       :candidate/attempt-id (:candidate/attempt-id checked)})))
    checked))

(defn- checked-candidates-for-turn
  "Read present candidates in stable claim order and authenticate every member."
  [digest-hex state turn-id]
  (let [turn (required-turn-in-state digest-hex state turn-id)
        claim (:translation-turn/candidate-claim turn)
        stored (:candidate-splits state)]
    (into []
          (keep (fn [member]
                  (when-let [candidate
                             (get stored
                                  (:candidate-claim-member/attempt-id member))]
                    (checked-candidate-for-turn digest-hex turn candidate))))
          (:candidate-claim/members claim))))

(defn assert-persisted-candidate-coverage!
  "Require set members to equal every separately persisted candidate, in order."
  [candidate-set persisted-candidates]
  (when-not (= (:candidate-set/members candidate-set) persisted-candidates)
    (throw (ex-info
            "persisted candidate members do not equal the complete candidate set"
            {:candidate-set/id (:candidate-set/id candidate-set)
             :persisted-candidates persisted-candidates
             :candidate-set/members (:candidate-set/members candidate-set)}))))

(defn- checked-candidate-set-for-turn
  "Authenticate a set through its atomic turn and durable member evidence."
  [digest-hex state turn-id candidate-set]
  (let [turn (required-turn-in-state digest-hex state turn-id)
        manifest (:translation-turn/manifest turn)
        claim (:translation-turn/candidate-claim turn)
        persisted (checked-candidates-for-turn digest-hex state turn-id)]
    (when-not (= [(:split-manifest/id manifest)
                  (:candidate-claim/id claim)
                  (:candidate-claim/revision claim)]
                 [(:candidate-set/manifest-id candidate-set)
                  (:candidate-set/claim-id candidate-set)
                  (:candidate-set/revision candidate-set)])
      (throw (ex-info "complete candidate set does not match its admitted turn"
                      {:translation-turn/id turn-id
                       :candidate-set/id (:candidate-set/id candidate-set)})))
    (assert-persisted-candidate-coverage! candidate-set persisted)
    (split/assert-candidate-set-integrity! digest-hex manifest candidate-set)))

(defn- complete-candidate-set-in-state
  "Atomically claim one turn-to-set relationship and global set identity."
  [state turn-id candidate-set]
  (let [set-id (:candidate-set/id candidate-set)
        turn-existing (get-in state [:candidate-sets turn-id])
        indexed-turn (get-in state [:candidate-set-turns set-id])]
    (cond
      (and (= turn-existing candidate-set) (= indexed-turn turn-id))
      (assoc state :answer turn-existing)

      (or turn-existing indexed-turn)
      (immutable-conflict! "candidate set" [turn-id set-id]
                           (or turn-existing
                               (get-in state [:candidate-sets indexed-turn]))
                           candidate-set)

      :else
      (-> state
          (assoc-in [:candidate-sets turn-id] candidate-set)
          (assoc-in [:candidate-set-turns set-id] turn-id)
          (assoc :answer candidate-set)))))

(defn- checked-candidate-set-for-turn-in-state
  "Read one turn's set and authenticate its complete dependency chain."
  [digest-hex state turn-id]
  (when-let [candidate-set (get-in state [:candidate-sets turn-id])]
    (checked-candidate-set-for-turn digest-hex state turn-id candidate-set)))

(defn- checked-candidate-set-by-id-in-state
  "Resolve an exact set index only when it agrees with authenticated evidence."
  [digest-hex state candidate-set-id]
  (when-let [turn-id (get-in state [:candidate-set-turns candidate-set-id])]
    (let [candidate-set (checked-candidate-set-for-turn-in-state
                         digest-hex state turn-id)]
      (when-not (= candidate-set-id (:candidate-set/id candidate-set))
        (inconsistent-index! "candidate set" candidate-set-id))
      candidate-set)))

(defn- required-candidate-set-in-state
  "Read an authenticated complete set or refuse orphan review evidence."
  [digest-hex state candidate-set-id]
  (or (checked-candidate-set-by-id-in-state digest-hex state candidate-set-id)
      (throw (ex-info "complete translation candidate set is not persisted"
                      {:candidate-set/id candidate-set-id}))))

(defn- turn-for-candidate-set-in-state
  "Read the authenticated turn that owns one exact candidate set."
  [digest-hex state candidate-set-id]
  (let [turn-id (or (get-in state [:candidate-set-turns candidate-set-id])
                    (throw (ex-info "complete translation candidate set is not persisted"
                                    {:candidate-set/id candidate-set-id})))]
    (required-turn-in-state digest-hex state turn-id)))

(defn- candidate-member-for-split
  "Find the candidate member named by one exact split selector."
  [candidate-set split-id]
  (some #(when (= split-id (:candidate/split-id %)) %)
        (:candidate-set/members candidate-set)))

(defn assert-candidate-split-selector!
  "Refuse histories for a split outside the selected complete candidate set."
  [candidate-set split-id]
  (when-not (candidate-member-for-split candidate-set split-id)
    (throw (ex-info "split is absent from the complete candidate set"
                    {:candidate-set/id (:candidate-set/id candidate-set)
                     :split/id split-id}))))

(defn- checked-review-history-in-state
  "Read, authenticate, and deterministically order one complete split history."
  [digest-hex state candidate-set-id split-id]
  (let [candidate-set (required-candidate-set-in-state
                       digest-hex state candidate-set-id)
        turn (turn-for-candidate-set-in-state digest-hex state candidate-set-id)
        manifest (:translation-turn/manifest turn)]
    (assert-candidate-split-selector! candidate-set split-id)
    (->> (vals (:review-receipts state))
         (filter #(and (= candidate-set-id (:review/candidate-set-id %))
                       (= split-id (:review/split-id %))))
         (map #(split/assert-review-receipt-integrity!
                digest-hex manifest candidate-set %))
         (sort-by (juxt :review/recorded-at :review/operation-id :review/id))
         vec)))

(defn same-review-operation?
  "Whether a replay differs only in its newly observed server timestamp."
  [existing attempted]
  (= (dissoc existing :review/recorded-at :review/digest)
     (dissoc attempted :review/recorded-at :review/digest)))

(defn- append-review-in-state
  "Claim one review operation; an equal replay keeps the first timestamp."
  [state receipt]
  (let [review-id (:review/id receipt)]
    (if-some [existing (get-in state [:review-receipts review-id])]
      (if (or (= existing receipt) (same-review-operation? existing receipt))
        (assoc state :answer existing)
        (immutable-conflict! "review receipt" review-id existing receipt))
      (-> state
          (assoc-in [:review-receipts review-id] receipt)
          (assoc :answer receipt)))))

(defn- memory-examples-for-set
  "Project every currently approved split example in one complete set."
  [digest-hex state turn-id candidate-set]
  (let [turn (required-turn-in-state digest-hex state turn-id)
        manifest (:translation-turn/manifest turn)
        set-id (:candidate-set/id candidate-set)]
    (into []
          (keep (fn [candidate]
                  (let [split-id (:candidate/split-id candidate)
                        history (checked-review-history-in-state
                                 digest-hex state set-id split-id)]
                    (split/approved-memory-example
                     digest-hex manifest candidate-set split-id history))))
          (:candidate-set/members candidate-set))))

(defn- nonblank?
  [value]
  (and (string? value) (not (str/blank? value))))

(defn checked-memory-scope
  "Validate the exact tenant/language selector for future translation memory."
  [scope]
  (let [allowed #{:org-id :project :garden :source-locale :target-locale
                  :exclude-manifest-id :current-candidate-set-ids :limit}
        limit (or (:limit scope) 12)]
    (when-not (and (map? scope)
                   (every? allowed (keys scope))
                   (nonblank? (:org-id scope))
                   (or (nil? (:project scope)) (nonblank? (:project scope)))
                   (or (nil? (:garden scope))
                       (qualified-keyword? (:garden scope)))
                   (keyword? (:source-locale scope))
                   (keyword? (:target-locale scope))
                   (or (nil? (:exclude-manifest-id scope))
                       (nonblank? (:exclude-manifest-id scope)))
                   (set? (:current-candidate-set-ids scope))
                   (every? nonblank? (:current-candidate-set-ids scope))
                   (integer? limit)
                   (pos? limit))
      (throw (ex-info "invalid translation memory scope" {:scope scope})))
    (assoc scope :limit limit)))

(defn- example-in-scope?
  "Whether one authenticated example is relevant to a future translation."
  [{:keys [org-id project source-locale target-locale exclude-manifest-id]}
   example]
  (and (= org-id (:translation-memory/org-id example))
       (= project (:translation-memory/project example))
       (= source-locale (:translation-memory/source-locale example))
       (= target-locale (:translation-memory/target-locale example))
       (not= exclude-manifest-id (:translation-memory/manifest-id example))))

(defn- applicable-memory-in-state
  "Fold current review history across prior sets with deterministic ranking."
  [digest-hex state scope]
  (let [{:keys [garden limit current-candidate-set-ids] :as checked}
        (checked-memory-scope scope)]
    (->> (:candidate-sets state)
         (filter (fn [[_ candidate-set]]
                   (contains? current-candidate-set-ids
                              (:candidate-set/id candidate-set))))
         (mapcat (fn [[turn-id candidate-set]]
                   (memory-examples-for-set digest-hex state turn-id
                                            (checked-candidate-set-for-turn
                                             digest-hex state turn-id
                                             candidate-set))))
         (filter #(example-in-scope? checked %))
         distinct
         (sort-by (juxt #(if (= garden (:translation-memory/garden %)) 0 1)
                        :translation-memory/id))
         (take limit)
         vec)))

(defrecord MemoryTranslationSplitStore [digest-hex state]
  ITranslationSplitStore
  (admit-turn! [_ turn]
    (let [checked (split/assert-turn-integrity! digest-hex turn)]
      (swap-answer! state admit-turn-in-state checked)))

  (turn-for-run! [_ run-id]
    (turn-for-run-in-state digest-hex @state run-id))

  (turn-by-id! [_ turn-id]
    (checked-turn-in-state digest-hex @state turn-id))

  (append-candidate-split! [_ turn-id candidate]
    (let [turn (required-turn-in-state digest-hex @state turn-id)
          checked (checked-candidate-for-turn digest-hex turn candidate)
          attempt-id (:candidate/attempt-id checked)]
      (swap-answer! state put-immutable-in-state
                    [:candidate-splits attempt-id]
                    "candidate split" attempt-id checked)))

  (candidate-splits-for-turn! [_ turn-id]
    (checked-candidates-for-turn digest-hex @state turn-id))

  (complete-candidate-set! [_ turn-id candidate-set]
    (let [checked (checked-candidate-set-for-turn
                   digest-hex @state turn-id candidate-set)]
      (swap-answer! state complete-candidate-set-in-state turn-id checked)))

  (candidate-set-for-turn! [_ turn-id]
    (checked-candidate-set-for-turn-in-state digest-hex @state turn-id))

  (candidate-set-by-id! [_ candidate-set-id]
    (checked-candidate-set-by-id-in-state digest-hex @state candidate-set-id))

  (turn-for-candidate-set! [_ candidate-set-id]
    (when (checked-candidate-set-by-id-in-state
           digest-hex @state candidate-set-id)
      (turn-for-candidate-set-in-state digest-hex @state candidate-set-id)))

  (append-review-receipt! [_ receipt]
    (let [shaped (schema/assert-valid! :translation-split/review-receipt
                                       schema/SplitReviewReceipt receipt)
          set-id (:review/candidate-set-id shaped)
          candidate-set (required-candidate-set-in-state digest-hex @state set-id)
          turn (turn-for-candidate-set-in-state digest-hex @state set-id)
          manifest (:translation-turn/manifest turn)
          checked (split/assert-review-receipt-integrity!
                   digest-hex manifest candidate-set shaped)]
      (swap-answer! state append-review-in-state checked)))

  (review-history-for-split! [_ candidate-set-id split-id]
    (checked-review-history-in-state digest-hex @state candidate-set-id split-id))

  (applicable-memory! [_ scope]
    (applicable-memory-in-state digest-hex @state scope)))

(defn memory-store
  "Create an atomic in-memory `ITranslationSplitStore` using `digest-hex`.

  This adapter is for tests and local verification, not production durability.
  It mirrors the unique aggregate and child indexes a Mongo adapter must own."
  [digest-hex]
  (when-not (fn? digest-hex)
    (throw (ex-info "translation split store requires a digest function" {})))
  (->MemoryTranslationSplitStore digest-hex (atom empty-state)))
