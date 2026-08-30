(ns knoxx.backend.law.translation-split
  "Portable façade and memory laws for revision-bound translation splits.

  Resource contracts own documents and publication intent. Split manifests,
  candidate claims, and candidate sets are immutable translation authority.
  Review memory is a derived projection of canonical evaluation evidence."
  (:require [knoxx.backend.law.translation-split-effective :as effective]
            [knoxx.backend.law.translation-split-identity :as identity]
            [knoxx.backend.law.translation-split-review :as review-law]
            [knoxx.backend.law.translation-split-schema :as schema]
            [knoxx.backend.law.translation-split-turn :as turn]))

;; One public namespace keeps callers stable while the implementations remain
;; independently reusable and reviewable.
(def split-manifest
  "Admit explicit logical source parts as one deterministic manifest."
  identity/split-manifest)

(def source-text
  "Reconstruct exact source bytes from an admitted manifest."
  identity/source-text)

(def assert-manifest-integrity!
  "Recompute and validate every persisted manifest identity field."
  identity/assert-manifest-integrity!)

(def candidate-claim
  "Bind every split attempt before provider execution."
  identity/candidate-claim)

(def assert-claim-integrity!
  "Authenticate a persisted pre-provider candidate claim."
  identity/assert-claim-integrity!)

(def candidate-split
  "Construct one immutable candidate from an admitted claim member."
  identity/candidate-split)

(def assert-candidate-integrity!
  "Authenticate candidate text against its stored digest."
  identity/assert-candidate-integrity!)

(def complete-candidate-set
  "Compose only an exact complete candidate claim in admitted order."
  identity/complete-candidate-set)

(def assert-candidate-set-integrity!
  "Rebuild and authenticate a persisted candidate set."
  identity/assert-candidate-set-integrity!)

(def review-receipt
  "Attribute a closed review request to server-owned evidence and identity."
  review-law/review-receipt)

(def assert-review-receipt-integrity!
  "Authenticate review scope, status, correction, identity, and digest."
  review-law/assert-review-receipt-integrity!)

(def effective-review-receipt
  "Select the deterministic latest receipt from complete durable history."
  review-law/effective-review-receipt)

(def execution-snapshot
  "Snapshot and digest the exact agent policy one turn will execute."
  turn/execution-snapshot)

(def assert-execution-integrity!
  "Authenticate a persisted agent execution snapshot."
  turn/assert-execution-integrity!)

(def memory-snapshot
  "Close one truthful prior-review memory lookup outcome."
  turn/memory-snapshot)

(def translation-turn-admission
  "Bind dispatch, split authority, execution, and memory before provider work."
  turn/translation-turn-admission)

(def assert-turn-integrity!
  "Authenticate one persisted pre-provider turn aggregate."
  turn/assert-turn-integrity!)

(def effective-candidate-set
  "Compose a reviewed target only when every manifest split is approved."
  effective/effective-candidate-set)

(def assert-effective-candidate-set-integrity!
  "Authenticate one persisted complete reviewed target against current history."
  effective/assert-effective-candidate-set-integrity!)

(def reviewed-output
  "Select the current ready or revoking output after any split review change."
  effective/reviewed-output)

(defn- split-member
  "Find one source split in an authenticated manifest."
  [manifest split-id]
  (some #(when (= split-id (:split/id %)) %) (:split-manifest/splits manifest)))

(defn- candidate-member
  "Find one candidate split in an authenticated candidate set."
  [candidate-set split-id]
  (some #(when (= split-id (:candidate/split-id %)) %)
        (:candidate-set/members candidate-set)))

(defn- review-binding
  "Return comparable evidence coordinates for one candidate member."
  [manifest candidate-set split-id candidate]
  [(:split-manifest/source-revision manifest)
   (:split-manifest/id manifest)
   (:candidate-set/id candidate-set)
   (:candidate-set/digest candidate-set)
   (:candidate-set/revision candidate-set)
   split-id
   (:candidate/attempt-id candidate)
   (:candidate/digest candidate)])

(defn- claimed-review-binding
  "Return the candidate coordinates claimed by a review projection."
  [review]
  [(:review/source-revision review)
   (:review/manifest-id review)
   (:review/candidate-set-id review)
   (:review/candidate-set-digest review)
   (:review/candidate-revision review)
   (:review/split-id review)
   (:review/candidate-attempt-id review)
   (:review/candidate-digest review)])

(defn- assert-review-binding!
  "Refuse positive memory whose review does not name exact candidate bytes."
  [manifest candidate-set split-id source-split candidate review]
  (when-not (and source-split
                 candidate
                 (= (review-binding manifest candidate-set split-id candidate)
                    (claimed-review-binding review)))
    (throw (ex-info "review does not bind the requested candidate split"
                    {:split/id split-id
                     :review review}))))

(defn- assert-correction-shape!
  "Require corrected bytes and correction identity to appear together."
  [review]
  (let [corrected? (some? (:review/corrected-text review))
        correction-id? (some? (:review/correction-id review))]
    (when-not (= corrected? correction-id?)
      (throw (ex-info "review correction text and identity must appear together"
                      {:review/id (:review/id review)})))))

(defn- manifest-memory-coordinates
  "Project authenticated resource and locale scope into translation memory."
  [manifest]
  (cond-> {:translation-memory/org-id (:split-manifest/org-id manifest)
           :translation-memory/garden (:split-manifest/garden manifest)
           :translation-memory/document (:split-manifest/document manifest)
           :translation-memory/source-locale (:split-manifest/source-locale manifest)
           :translation-memory/target-locale (:split-manifest/target-locale manifest)
           :translation-memory/manifest-id (:split-manifest/id manifest)
           :translation-memory/source-revision (:split-manifest/source-revision manifest)
           :translation-memory/source-digest (:split-manifest/source-digest manifest)}
    (some? (:split-manifest/project manifest))
    (assoc :translation-memory/project (:split-manifest/project manifest))))

(defn- candidate-memory-coordinates
  "Project authenticated candidate-set and member identity into memory."
  [candidate-set candidate]
  {:translation-memory/candidate-set-id (:candidate-set/id candidate-set)
   :translation-memory/candidate-set-digest (:candidate-set/digest candidate-set)
   :translation-memory/candidate-revision (:candidate-set/revision candidate-set)
   :translation-memory/candidate-attempt-id (:candidate/attempt-id candidate)
   :translation-memory/candidate-digest (:candidate/digest candidate)})

(defn- memory-example-id
  "Derive memory identity from authenticated candidate and review evidence."
  [digest-hex manifest candidate-set split-id review target-text]
  (str "translation-memory-"
       (digest-hex
        (pr-str [(:split-manifest/id manifest)
                 (:candidate-set/id candidate-set)
                 split-id
                 (:review/id review)
                 (:review/correction-id review)
                 target-text]))))

(defn- memory-example-value
  "Build one approved source/target example from authenticated inputs."
  [memory-id manifest candidate-set source-split candidate review target-text]
  (cond-> (merge (manifest-memory-coordinates manifest)
                 (candidate-memory-coordinates candidate-set candidate)
                 {:translation-memory/id memory-id
                  :translation-memory/split-id (:split/id source-split)
                  :translation-memory/split-source-digest
                  (:split/source-digest source-split)
                  :translation-memory/source-text (:split/source-text source-split)
                  :translation-memory/target-text target-text
                  :translation-memory/review-receipt-id (:review/id review)})
    (some? (:review/correction-id review))
    (assoc :translation-memory/correction-id (:review/correction-id review))))

(defn approved-memory-example
  "Project approved canonical review evidence into an exact positive example.

  The caller supplies complete durable history for this exact candidate split.
  Its deterministic effective receipt wins; a later in-review or rejected
  receipt suppresses an older approval. All persisted identities and digests
  are recomputed before the projection may inherit tenant scope."
  [digest-hex manifest candidate-set split-id review-history]
  (let [checked-manifest (identity/assert-manifest-integrity! digest-hex manifest)
        checked-set (identity/assert-candidate-set-integrity!
                     digest-hex checked-manifest candidate-set)
        effective-review (review-law/effective-review-receipt
                          digest-hex checked-manifest checked-set split-id
                          review-history)]
    (when (= :approved (:review/status effective-review))
      (let [source-split (split-member checked-manifest split-id)
            candidate (candidate-member checked-set split-id)]
        (assert-review-binding! checked-manifest checked-set split-id source-split
                                candidate effective-review)
        (assert-correction-shape! effective-review)
        (let [target-text (or (:review/corrected-text effective-review)
                              (:candidate/text candidate))
              memory-id (memory-example-id digest-hex checked-manifest checked-set
                                           split-id effective-review target-text)
              value (memory-example-value memory-id checked-manifest checked-set
                                          source-split candidate effective-review
                                          target-text)]
          (schema/assert-valid! :translation-split/memory-example
                                schema/TranslationMemoryExample value))))))
