(ns knoxx.backend.law.translation-split-review
  "Server-attributed review receipts and deterministic effective selection."
  (:require [knoxx.backend.law.translation-evidence :as evidence]
            [knoxx.backend.law.translation-split-identity :as identity]
            [knoxx.backend.law.translation-split-schema :as schema]))

(def ^:private request-keys
  "The complete closed set of facts a review client may submit."
  [:review/operation-id
   :review/adequacy
   :review/fluency
   :review/terminology
   :review/risk
   :review/overall
   :review/corrected-text
   :review/editor-notes])

(defn- canonical-request
  "Validate the client boundary before normalizing absent optional values."
  [request]
  (let [checked (schema/assert-valid! :translation-split/review-request
                                      schema/SplitReviewRequest request)]
    (cond-> checked
      (nil? (:review/corrected-text checked)) (dissoc :review/corrected-text)
      (nil? (:review/editor-notes checked)) (dissoc :review/editor-notes))))

(defn- canonical-principal
  "Validate server-owned reviewer attribution and normalize optional values."
  [principal]
  (let [checked (schema/assert-valid! :translation-split/review-principal
                                      schema/ReviewPrincipal principal)]
    (cond-> checked
      (nil? (:principal/user-email checked)) (dissoc :principal/user-email)
      (nil? (:principal/membership-id checked)) (dissoc :principal/membership-id))))

(defn- candidate-member
  "Find the authenticated candidate member named by a server-resolved split id."
  [candidate-set split-id]
  (or (some #(when (= split-id (:candidate/split-id %)) %)
            (:candidate-set/members candidate-set))
      (throw (ex-info "review split is absent from the authenticated candidate set"
                      {:split/id split-id
                       :candidate-set/id (:candidate-set/id candidate-set)}))))

(defn- review-status
  "Derive effective state from the historical overall judgment vocabulary."
  [overall]
  (case overall
    "approve" :approved
    "needs_edit" :in-review
    "reject" :rejected))

(defn- correction-id
  "Derive correction identity from exact server-bound candidate bytes."
  [digest-hex request candidate-set candidate]
  (when-let [corrected-text (:review/corrected-text request)]
    (str "translation-correction-"
         (digest-hex
          (pr-str [(:review/operation-id request)
                   (:candidate-set/id candidate-set)
                   (:candidate/attempt-id candidate)
                   (:candidate/digest candidate)
                   corrected-text])))))

(defn- receipt-facts
  "Combine client judgment with server-derived scope, candidate, actor, and time."
  [digest-hex manifest candidate-set split-id candidate principal recorded-at request]
  (cond-> (merge request
                 {:review/status (review-status (:review/overall request))
                  :review/source-revision (:split-manifest/source-revision manifest)
                  :review/manifest-id (:split-manifest/id manifest)
                  :review/candidate-set-id (:candidate-set/id candidate-set)
                  :review/candidate-set-digest (:candidate-set/digest candidate-set)
                  :review/candidate-revision (:candidate-set/revision candidate-set)
                  :review/split-id split-id
                  :review/candidate-attempt-id (:candidate/attempt-id candidate)
                  :review/candidate-digest (:candidate/digest candidate)
                  :review/principal principal
                  :review/recorded-at recorded-at})
    (some? (:review/corrected-text request))
    (assoc :review/correction-id
           (correction-id digest-hex request candidate-set candidate))))

(defn- receipt-id
  "Derive one idempotent review-event identity from server-bound coordinates."
  [digest-hex facts]
  (str "translation-review-"
       (digest-hex
        (pr-str [(:review/operation-id facts)
                 (:review/manifest-id facts)
                 (:review/candidate-set-id facts)
                 (:review/split-id facts)
                 (:review/candidate-attempt-id facts)]))))

(defn- receipt-digest-input
  "Return an explicit portable ordering for every receipt fact."
  [review-id facts]
  [review-id
   (:review/operation-id facts)
   (:review/status facts)
   (:review/source-revision facts)
   (:review/manifest-id facts)
   (:review/candidate-set-id facts)
   (:review/candidate-set-digest facts)
   (:review/candidate-revision facts)
   (:review/split-id facts)
   (:review/candidate-attempt-id facts)
   (:review/candidate-digest facts)
   [(:principal/user-id (:review/principal facts))
    (:principal/user-email (:review/principal facts))
    (:principal/membership-id (:review/principal facts))]
   (:review/recorded-at facts)
   (:review/adequacy facts)
   (:review/fluency facts)
   (:review/terminology facts)
   (:review/risk facts)
   (:review/overall facts)
   (:review/corrected-text facts)
   (:review/correction-id facts)
   (:review/editor-notes facts)])

(defn review-receipt
  "Build review evidence from a closed request and server-owned authority.

  The boundary supplies the authenticated manifest/candidate set, resolved split,
  authenticated principal, and clock reading separately. The request cannot name
  or override any of them."
  [digest-hex manifest candidate-set split-id principal recorded-at request]
  (let [checked-manifest (identity/assert-manifest-integrity! digest-hex manifest)
        checked-set (identity/assert-candidate-set-integrity!
                     digest-hex checked-manifest candidate-set)
        candidate (candidate-member checked-set split-id)
        checked-principal (canonical-principal principal)
        checked-at (schema/assert-valid! :translation-split/review-recorded-at
                                         evidence/Instant recorded-at)
        checked-request (canonical-request request)
        facts (receipt-facts digest-hex checked-manifest checked-set split-id candidate
                             checked-principal checked-at checked-request)
        review-id (receipt-id digest-hex facts)
        receipt-digest (digest-hex (pr-str (receipt-digest-input review-id facts)))]
    (schema/assert-valid!
     :translation-split/review-receipt
     schema/SplitReviewReceipt
     (assoc facts :review/id review-id :review/digest receipt-digest))))

(defn assert-review-receipt-integrity!
  "Rebuild a receipt from authenticated evidence and its closed request facts."
  [digest-hex manifest candidate-set receipt]
  (let [checked (schema/assert-valid! :translation-split/review-receipt
                                      schema/SplitReviewReceipt receipt)
        expected (review-receipt digest-hex manifest candidate-set
                                 (:review/split-id checked)
                                 (:review/principal checked)
                                 (:review/recorded-at checked)
                                 (select-keys checked request-keys))]
    (when-not (= expected checked)
      (throw (ex-info "translation review receipt identity or digest is invalid"
                      {:expected expected
                       :actual checked})))
    checked))

(defn- review-supersedes?
  "Define a total order: instant, operation id, then receipt id.

  Operation identity precedes per-receipt identity so one document-level review
  group wins consistently across every split when the server clock has only
  millisecond precision. Receipt id remains the final deterministic tie-breaker
  for otherwise equal coordinates."
  [candidate incumbent]
  (let [candidate-at (:review/recorded-at candidate)
        incumbent-at (:review/recorded-at incumbent)
        operation-order (when incumbent
                          (compare (:review/operation-id candidate)
                                   (:review/operation-id incumbent)))]
    (or (nil? incumbent)
        (evidence/later-instant? candidate-at incumbent-at)
        (and (= candidate-at incumbent-at)
             (or (pos? operation-order)
                 (and (zero? operation-order)
                      (pos? (compare (:review/id candidate)
                                     (:review/id incumbent)))))))))

(defn- unique-history
  "Dedupe equal retries and refuse changed evidence behind one receipt id."
  [receipts]
  (vals
   (reduce (fn [by-id receipt]
             (if-let [incumbent (get by-id (:review/id receipt))]
               (if (= incumbent receipt)
                 by-id
                 (throw (ex-info "review history conflicts behind one receipt id"
                                 {:review/id (:review/id receipt)})))
               (assoc by-id (:review/id receipt) receipt)))
           {}
           receipts)))

(defn effective-review-receipt
  "Select the deterministic latest receipt from a complete split history.

  The caller must supply the complete durable history for this candidate split;
  passing one arbitrary receipt is deliberately not this function's API."
  [digest-hex manifest candidate-set split-id receipts]
  (let [history (schema/assert-valid! :translation-split/review-history
                                      schema/SplitReviewHistory receipts)
        checked (mapv #(assert-review-receipt-integrity!
                        digest-hex manifest candidate-set %)
                      history)]
    (doseq [receipt checked]
      (when-not (= split-id (:review/split-id receipt))
        (throw (ex-info "review history contains another candidate split"
                        {:expected-split/id split-id
                         :actual-split/id (:review/split-id receipt)}))))
    (reduce (fn [incumbent candidate]
              (if (review-supersedes? candidate incumbent) candidate incumbent))
            nil
            (unique-history checked))))
