(ns knoxx.backend.law.translation-split-effective
  "Deterministic reviewed composition for one complete translation candidate set."
  (:require [knoxx.backend.law.translation-split-identity :as identity]
            [knoxx.backend.law.translation-split-review :as review-law]
            [knoxx.backend.law.translation-split-schema :as schema]))

(def ^:private effective-id-prefix
  "Names a complete reviewed candidate-set identity inside the value itself."
  "translation-effective-candidate-set-")

(def ^:private effective-revision-prefix
  "Names the composed reviewed revision inside the value itself."
  "translation-effective-revision-")

(def ^:private review-state-revision-prefix
  "Names a non-ready review projection that supersedes an earlier approval."
  "translation-review-state-")

(defn- candidate-by-split-id
  "Index authenticated raw candidates by their admitted split identity."
  [candidate-set]
  (into {} (map (juxt :candidate/split-id (fn [candidate] candidate)))
        (:candidate-set/members candidate-set)))

(defn- not-ready-reason
  "Translate an absent or non-approved latest review into a stable reason."
  [effective-review]
  (case (:review/status effective-review)
    nil :review-missing
    :in-review :review-in-review
    :rejected :review-rejected
    nil))

(defn- not-ready-split
  "Describe one blocking split without carrying source, candidate, or target bytes."
  [source-split effective-review]
  (cond->
   {:effective-candidate-not-ready/split-id (:split/id source-split)
    :effective-candidate-not-ready/split-index (:split/index source-split)
    :effective-candidate-not-ready/reason (not-ready-reason effective-review)}
    (some? effective-review)
    (assoc :effective-candidate-not-ready/review-status
           (:review/status effective-review)
           :effective-candidate-not-ready/review-receipt-id
           (:review/id effective-review))))

(defn- effective-member
  "Select approved corrected bytes when present, otherwise the raw candidate."
  [digest-hex source-split candidate effective-review]
  (let [target-text (or (:review/corrected-text effective-review)
                        (:candidate/text candidate))]
    (cond->
     {:effective-candidate/split-id (:split/id source-split)
      :effective-candidate/split-index (:split/index source-split)
      :effective-candidate/candidate-attempt-id (:candidate/attempt-id candidate)
      :effective-candidate/candidate-digest (:candidate/digest candidate)
      :effective-candidate/review-receipt-id (:review/id effective-review)
      :effective-candidate/review-receipt-digest (:review/digest effective-review)
      :effective-candidate/target-text target-text
      :effective-candidate/target-digest (digest-hex target-text)}
      (some? (:review/correction-id effective-review))
      (assoc :effective-candidate/correction-id
             (:review/correction-id effective-review)))))

(defn- member-lineage
  "Return the ordered facts that bind one chosen target to its review authority."
  [member]
  [(:effective-candidate/split-id member)
   (:effective-candidate/split-index member)
   (:effective-candidate/candidate-attempt-id member)
   (:effective-candidate/candidate-digest member)
   (:effective-candidate/review-receipt-id member)
   (:effective-candidate/review-receipt-digest member)
   (:effective-candidate/correction-id member)
   (:effective-candidate/target-digest member)])

(defn- candidate-set-lineage
  "Return every raw candidate-set coordinate inherited by reviewed composition."
  [candidate-set]
  [(:candidate-set/id candidate-set)
   (:candidate-set/manifest-id candidate-set)
   (:candidate-set/claim-id candidate-set)
   (:candidate-set/revision candidate-set)
   (:candidate-set/digest candidate-set)])

(defn- ready-value
  "Build one content- and lineage-addressed effective candidate set."
  [digest-hex manifest candidate-set members]
  (let [text (apply str (map :effective-candidate/target-text members))
        content-digest (digest-hex text)
        lineage-digest
        (digest-hex
         (pr-str [(candidate-set-lineage candidate-set)
                  (mapv member-lineage members)
                  content-digest]))]
    (schema/assert-valid!
     :translation-split/effective-candidate-set
     schema/EffectiveCandidateSet
     {:effective-candidate-set/id (str effective-id-prefix lineage-digest)
      :effective-candidate-set/manifest-id (:split-manifest/id manifest)
      :effective-candidate-set/candidate-set-id (:candidate-set/id candidate-set)
      :effective-candidate-set/candidate-set-digest (:candidate-set/digest candidate-set)
      :effective-candidate-set/revision
      (str effective-revision-prefix lineage-digest)
      :effective-candidate-set/digest lineage-digest
      :effective-candidate-set/content-digest content-digest
      :effective-candidate-set/text text
      :effective-candidate-set/members members})))

(defn- selected-split
  "Authenticate complete history and select one split's latest review outcome."
  [digest-hex manifest candidate-set candidates-by-id review-histories source-split]
  (let [split-id (:split/id source-split)
        effective-review
        (review-law/effective-review-receipt
         digest-hex manifest candidate-set split-id
         (get review-histories split-id []))]
    (if (= :approved (:review/status effective-review))
      {:member (effective-member digest-hex source-split
                                 (get candidates-by-id split-id)
                                 effective-review)}
      {:not-ready (not-ready-split source-split effective-review)})))

(defn- composition-result
  "Return either every blocker or one complete reviewed target."
  [digest-hex manifest candidate-set selections]
  (let [not-ready (into [] (keep :not-ready) selections)]
    (if (seq not-ready)
      {:effective-candidate-set/status :not-ready
       :effective-candidate-set/refusal
       {:refusal/type :effective-candidate-set/not-ready
        :refusal/splits not-ready}}
      {:effective-candidate-set/status :ready
       :effective-candidate-set/value
       (ready-value digest-hex manifest candidate-set (mapv :member selections))})))

(defn effective-candidate-set
  "Compose reviewed target bytes only when every manifest split is approved.

  `review-histories` is a map from split id to that split's complete immutable
  receipt history. Histories and their deterministic latest receipts are fully
  authenticated before any result is returned. Missing, rejected, or in-review
  latest evidence yields a typed refusal listing every blocking split in manifest
  order; the refusal deliberately contains no partial target bytes."
  [digest-hex manifest candidate-set review-histories]
  (let [checked-manifest (identity/assert-manifest-integrity! digest-hex manifest)
        checked-set (identity/assert-candidate-set-integrity!
                     digest-hex checked-manifest candidate-set)
        checked-histories
        (schema/assert-valid! :translation-split/effective-review-histories
                              schema/EffectiveReviewHistories
                              review-histories)
        candidates (candidate-by-split-id checked-set)
        selections (mapv #(selected-split digest-hex checked-manifest checked-set
                                          candidates checked-histories %)
                         (:split-manifest/splits checked-manifest))]
    (schema/assert-valid!
     :translation-split/effective-candidate-set-result
     schema/EffectiveCandidateSetResult
     (composition-result digest-hex checked-manifest checked-set selections))))

(defn assert-effective-candidate-set-integrity!
  "Rebuild a ready effective set from complete history and compare every field."
  [digest-hex manifest candidate-set review-histories effective-set]
  (let [checked (schema/assert-valid! :translation-split/effective-candidate-set
                                      schema/EffectiveCandidateSet effective-set)
        result (effective-candidate-set digest-hex manifest candidate-set
                                        review-histories)]
    (when-not (= :ready (:effective-candidate-set/status result))
      (throw (ex-info "translation effective candidate set is no longer ready"
                      (:effective-candidate-set/refusal result))))
    (let [expected (:effective-candidate-set/value result)]
      (when-not (= expected checked)
        (throw (ex-info "translation effective candidate set identity is invalid"
                        {:expected expected :actual checked})))
      checked)))

(defn- review-state-coordinate
  "Bind one manifest member to its deterministic latest review, including nil."
  [digest-hex manifest candidate-set review-histories source-split]
  (let [split-id (:split/id source-split)
        receipt (review-law/effective-review-receipt
                 digest-hex manifest candidate-set split-id
                 (get review-histories split-id []))]
    [split-id
     (:split/index source-split)
     (:review/id receipt)
     (:review/digest receipt)
     (:review/status receipt)]))

(defn- review-state-digest
  "Address the exact latest-review projection over one raw candidate set."
  [digest-hex manifest candidate-set review-histories]
  (digest-hex
   (pr-str
    [(candidate-set-lineage candidate-set)
     (mapv #(review-state-coordinate digest-hex manifest candidate-set
                                     review-histories %)
           (:split-manifest/splits manifest))])))

(defn- review-order
  "Return the exact effective-review total order in manifest order.

  The completed-receipt selector must use the same coordinates as
  `effective-review-receipt`: timestamp, then operation group, then receipt id.
  Omitting the operation id lets a hash-derived receipt id select a different
  same-millisecond bulk group after projection than composition selected."
  [digest-hex manifest candidate-set review-histories]
  (mapv (fn [source-split]
          (let [receipt (review-law/effective-review-receipt
                         digest-hex manifest candidate-set (:split/id source-split)
                         (get review-histories (:split/id source-split) []))]
            [(:review/recorded-at receipt)
             (:review/operation-id receipt)
             (:review/id receipt)]))
        (:split-manifest/splits manifest)))

(defn reviewed-output
  "Return the output revision that must be current after any split review.

  A fully approved set yields corrected-or-candidate bytes and the effective
  revision. Any missing, in-review, or rejected split yields the immutable raw
  candidate bytes under a review-state revision derived from the complete
  latest-review projection. Recording that non-ready revision is intentional:
  it supersedes a previously approved effective receipt, so a later rejection
  cannot leave old approval evidence authorizing content the reviewer revoked."
  [digest-hex manifest candidate-set review-histories]
  (let [result (effective-candidate-set digest-hex manifest candidate-set
                                        review-histories)
        checked-manifest (identity/assert-manifest-integrity! digest-hex manifest)
        checked-set (identity/assert-candidate-set-integrity!
                     digest-hex checked-manifest candidate-set)
        state-digest (review-state-digest digest-hex checked-manifest checked-set
                                          review-histories)
        order (review-order digest-hex checked-manifest checked-set review-histories)]
    (if (= :ready (:effective-candidate-set/status result))
      (let [effective (:effective-candidate-set/value result)]
        {:translation-reviewed-output/status :ready
         :translation-reviewed-output/revision
         (:effective-candidate-set/revision effective)
         :translation-reviewed-output/lineage-digest
         (:effective-candidate-set/digest effective)
         :translation-reviewed-output/review-order order
         :translation-reviewed-output/text (:effective-candidate-set/text effective)
         :translation-reviewed-output/effective-candidate-set effective})
      {:translation-reviewed-output/status :not-ready
       :translation-reviewed-output/revision
       (str review-state-revision-prefix state-digest)
       :translation-reviewed-output/lineage-digest state-digest
       :translation-reviewed-output/review-order order
       :translation-reviewed-output/text (:candidate-set/text checked-set)
       :translation-reviewed-output/refusal
       (:effective-candidate-set/refusal result)})))
