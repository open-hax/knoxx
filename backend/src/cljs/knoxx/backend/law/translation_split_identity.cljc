(ns knoxx.backend.law.translation-split-identity
  "Canonical constructors and read-side integrity laws for translation splits."
  (:require [clojure.set :as set]
            [knoxx.backend.law.translation-split-schema :as schema]))

(defn- manifest-coordinates
  "Return every resource coordinate that contributes to manifest identity."
  [value]
  [(:split-manifest/org-id value (:org-id value))
   (:split-manifest/project value (:project value))
   (:split-manifest/garden value (:garden value))
   (:split-manifest/document value (:document value))
   (:split-manifest/source-locale value (:source-locale value))
   (:split-manifest/target-locale value (:target-locale value))
   (:split-manifest/source-revision value (:source-revision value))])

(defn- split-id
  "Derive one source member id from exact scope, span, and bytes."
  [digest-hex coordinates index start end source-digest]
  (str "translation-split-"
       (digest-hex (pr-str [coordinates index start end source-digest]))))

(defn- source-split
  "Describe one explicitly admitted logical source part."
  [digest-hex coordinates index start text]
  (let [end (+ start (count text))
        source-digest (digest-hex text)]
    {:split/id (split-id digest-hex coordinates index start end source-digest)
     :split/index index
     :split/source-start start
     :split/source-end end
     :split/source-text text
     :split/source-digest source-digest}))

(defn- admitted-splits
  "Attach contiguous spans and stable identities to logical source parts."
  [digest-hex coordinates source-parts]
  (loop [index 0
         start 0
         remaining source-parts
         splits []]
    (if-let [part (first remaining)]
      (let [split (source-split digest-hex coordinates index start part)]
        (recur (inc index) (:split/source-end split) (next remaining)
               (conj splits split)))
      splits)))

(defn- split-manifest-id
  "Derive manifest identity from scope, source, and ordered split identities."
  [digest-hex coordinates source-digest splits]
  (str "translation-split-manifest-"
       (digest-hex
        (pr-str [coordinates source-digest
                 (mapv (juxt :split/id :split/index :split/source-start
                             :split/source-end :split/source-digest)
                       splits)]))))

(defn- assert-source-parts!
  "Refuse absent, empty, or lossy logical source parts."
  [source-text source-parts]
  (when-not (and (vector? source-parts)
                 (seq source-parts)
                 (every? schema/nonblank-string? source-parts))
    (throw (ex-info "translation source parts must be a nonempty vector of nonblank text"
                    {:source-parts source-parts})))
  (when-not (= source-text (apply str source-parts))
    (throw (ex-info "translation source parts must reconstruct the exact source text"
                    {:source-text source-text
                     :source-parts source-parts}))))

(defn- manifest-value
  "Build the closed manifest value before validation."
  [[org-id project garden document source-locale target-locale source-revision]
   source-digest splits manifest-id]
  (cond-> {:split-manifest/id manifest-id
           :split-manifest/org-id org-id
           :split-manifest/garden garden
           :split-manifest/document document
           :split-manifest/source-locale source-locale
           :split-manifest/target-locale target-locale
           :split-manifest/source-revision source-revision
           :split-manifest/source-digest source-digest
           :split-manifest/splits splits}
    (some? project) (assoc :split-manifest/project project)))

(defn split-manifest
  "Admit explicit logical source parts as one deterministic resource manifest."
  [digest-hex {:keys [source-text source-parts] :as input}]
  (assert-source-parts! source-text source-parts)
  (let [coordinates (manifest-coordinates input)
        source-digest (digest-hex source-text)
        splits (admitted-splits digest-hex coordinates source-parts)
        manifest-id (split-manifest-id digest-hex coordinates source-digest splits)]
    (->> (manifest-value coordinates source-digest splits manifest-id)
         (schema/assert-valid! :translation-split/manifest schema/SplitManifest))))

(defn source-text
  "Reconstruct a manifest's exact source bytes in admitted order."
  [manifest]
  (->> (:split-manifest/splits
        (schema/assert-valid! :translation-split/manifest
                              schema/SplitManifest manifest))
       (sort-by :split/index)
       (map :split/source-text)
       (apply str)))

(defn- manifest-input
  "Recover constructor input from a persisted manifest for integrity checking."
  [manifest]
  (cond-> {:org-id (:split-manifest/org-id manifest)
           :garden (:split-manifest/garden manifest)
           :document (:split-manifest/document manifest)
           :source-locale (:split-manifest/source-locale manifest)
           :target-locale (:split-manifest/target-locale manifest)
           :source-revision (:split-manifest/source-revision manifest)
           :source-text (source-text manifest)
           :source-parts (mapv :split/source-text (:split-manifest/splits manifest))}
    (some? (:split-manifest/project manifest))
    (assoc :project (:split-manifest/project manifest))))

(defn assert-manifest-integrity!
  "Validate shape and recompute every persisted manifest identity field."
  [digest-hex manifest]
  (let [checked (schema/assert-valid! :translation-split/manifest
                                      schema/SplitManifest manifest)
        expected (split-manifest digest-hex (manifest-input checked))]
    (when-not (= expected checked)
      (throw (ex-info "translation split manifest identity or coordinates are invalid"
                      {:expected expected
                       :actual checked})))
    checked))

(defn- claim-member
  "Assign one workflow-stable attempt before provider execution."
  [digest-hex manifest-id candidate-revision split]
  {:candidate-claim-member/attempt-id
   (str "translation-attempt-"
        (digest-hex (pr-str [manifest-id candidate-revision (:split/id split)])))
   :candidate-claim-member/split-id (:split/id split)
   :candidate-claim-member/split-index (:split/index split)
   :candidate-claim-member/source-digest (:split/source-digest split)})

(defn- candidate-claim-id
  "Derive claim identity from its manifest, revision, and attempt assignments."
  [digest-hex manifest-id candidate-revision members]
  (str "translation-candidate-claim-"
       (digest-hex
        (pr-str [manifest-id candidate-revision
                 (mapv (juxt :candidate-claim-member/attempt-id
                             :candidate-claim-member/split-id
                             :candidate-claim-member/source-digest)
                       members)]))))

(defn candidate-claim
  "Bind every admitted split to an attempt before provider execution."
  [digest-hex manifest candidate-revision]
  (let [checked (assert-manifest-integrity! digest-hex manifest)
        manifest-id (:split-manifest/id checked)
        members (mapv #(claim-member digest-hex manifest-id candidate-revision %)
                      (:split-manifest/splits checked))
        claim-id (candidate-claim-id digest-hex manifest-id candidate-revision members)]
    (schema/assert-valid!
     :translation-split/candidate-claim
     schema/CandidateClaim
     {:candidate-claim/id claim-id
      :candidate-claim/manifest-id manifest-id
      :candidate-claim/revision candidate-revision
      :candidate-claim/members members})))

(defn assert-claim-integrity!
  "Validate a persisted claim against its exact manifest and revision."
  [digest-hex manifest candidate-claim-value]
  (let [checked (schema/assert-valid! :translation-split/candidate-claim
                                      schema/CandidateClaim candidate-claim-value)
        expected (candidate-claim digest-hex manifest (:candidate-claim/revision checked))]
    (when-not (= expected checked)
      (throw (ex-info "translation candidate claim identity is invalid"
                      {:expected expected
                       :actual checked})))
    checked))

(defn candidate-split
  "Build one candidate from a pre-admitted claim member."
  [digest-hex claim-member-value candidate-text]
  (let [member (schema/assert-valid! :translation-split/candidate-claim-member
                                     schema/CandidateClaimMember claim-member-value)]
    (schema/assert-valid!
     :translation-split/candidate
     schema/CandidateSplit
     {:candidate/attempt-id (:candidate-claim-member/attempt-id member)
      :candidate/split-id (:candidate-claim-member/split-id member)
      :candidate/split-index (:candidate-claim-member/split-index member)
      :candidate/source-digest (:candidate-claim-member/source-digest member)
      :candidate/text candidate-text
      :candidate/digest (digest-hex candidate-text)})))

(defn assert-candidate-integrity!
  "Validate candidate shape and authenticate its digest against its bytes."
  [digest-hex candidate]
  (let [checked (schema/assert-valid! :translation-split/candidate
                                      schema/CandidateSplit candidate)]
    (when-not (= (:candidate/digest checked)
                 (digest-hex (:candidate/text checked)))
      (throw (ex-info "translation candidate digest does not match its bytes"
                      {:candidate/attempt-id (:candidate/attempt-id checked)})))
    checked))

(defn- candidates-by-split-id
  "Index candidates while refusing duplicate split identities."
  [candidates]
  (reduce (fn [indexed candidate]
            (let [candidate-split-id (:candidate/split-id candidate)]
              (when (contains? indexed candidate-split-id)
                (throw (ex-info "candidate set contains a duplicate split"
                                {:split/id candidate-split-id})))
              (assoc indexed candidate-split-id candidate)))
          {}
          candidates))

(defn- assert-exact-coverage!
  "Refuse candidate membership that differs from the admitted claim."
  [expected-ids actual-ids]
  (when-not (= expected-ids actual-ids)
    (throw (ex-info "candidate set does not cover the split claim exactly"
                    {:missing (vec (sort (set/difference expected-ids actual-ids)))
                     :extra (vec (sort (set/difference actual-ids expected-ids)))}))))

(defn- candidate-for-claim-member
  "Return the candidate bound to one exact attempt assignment."
  [indexed member]
  (let [candidate (get indexed (:candidate-claim-member/split-id member))
        expected [(:candidate-claim-member/attempt-id member)
                  (:candidate-claim-member/split-id member)
                  (:candidate-claim-member/split-index member)
                  (:candidate-claim-member/source-digest member)]
        actual [(:candidate/attempt-id candidate) (:candidate/split-id candidate)
                (:candidate/split-index candidate) (:candidate/source-digest candidate)]]
    (when-not (= expected actual)
      (throw (ex-info "candidate attempt does not match its pre-admitted claim"
                      {:expected expected
                       :actual actual})))
    candidate))

(defn- candidate-set-id
  "Derive complete-set identity from its claim and authenticated candidates."
  [digest-hex manifest-id claim-id candidate-revision ordered]
  (str "translation-candidate-set-"
       (digest-hex
        (pr-str [manifest-id claim-id candidate-revision
                 (mapv (juxt :candidate/attempt-id :candidate/split-id
                             :candidate/digest)
                       ordered)]))))

(defn complete-candidate-set
  "Compose only an exact complete claim, in its pre-admitted order."
  [digest-hex manifest candidate-claim-value candidates]
  (let [checked-manifest (assert-manifest-integrity! digest-hex manifest)
        checked-claim (assert-claim-integrity! digest-hex checked-manifest
                                               candidate-claim-value)
        checked-candidates (mapv #(assert-candidate-integrity! digest-hex %) candidates)
        indexed (candidates-by-split-id checked-candidates)
        claim-members (:candidate-claim/members checked-claim)]
    (assert-exact-coverage!
     (set (map :candidate-claim-member/split-id claim-members))
     (set (keys indexed)))
    (let [ordered (mapv #(candidate-for-claim-member indexed %) claim-members)
          text (apply str (map :candidate/text ordered))
          set-id (candidate-set-id digest-hex (:split-manifest/id checked-manifest)
                                   (:candidate-claim/id checked-claim)
                                   (:candidate-claim/revision checked-claim) ordered)]
      (schema/assert-valid!
       :translation-split/candidate-set
       schema/CandidateSet
       {:candidate-set/id set-id
        :candidate-set/manifest-id (:split-manifest/id checked-manifest)
        :candidate-set/claim-id (:candidate-claim/id checked-claim)
        :candidate-set/revision (:candidate-claim/revision checked-claim)
        :candidate-set/digest (digest-hex text)
        :candidate-set/text text
        :candidate-set/members ordered}))))

(defn assert-candidate-set-integrity!
  "Rebuild a persisted candidate set from authenticated bytes and coordinates."
  [digest-hex manifest candidate-set]
  (let [checked (schema/assert-valid! :translation-split/candidate-set
                                      schema/CandidateSet candidate-set)
        claim (candidate-claim digest-hex manifest (:candidate-set/revision checked))
        expected (complete-candidate-set digest-hex manifest claim
                                         (:candidate-set/members checked))]
    (when-not (= expected checked)
      (throw (ex-info "translation candidate-set identity or digest is invalid"
                      {:expected expected
                       :actual checked})))
    checked))
