(ns knoxx.backend.law.translation-split-fixture
  "Shared exact-evidence fixtures for translation split law tests."
  (:require [knoxx.backend.law.translation-split :as split]))

(defn digest
  "Deterministic test digest that exposes which bytes influenced identity."
  [value]
  (str "digest[" value "]"))

(def coordinates
  "Resource coordinates shared by the split fixtures."
  {:org-id "open-hax"
   :project "promethean"
   :garden :open-hax.gardens/promethean
   :document :open-hax.documents/start-here
   :source-locale :en
   :target-locale :es
   :source-revision "sha256-source-v1"})

(def source-parts
  "Explicit logical parts that reconstruct the exact source bytes."
  ["# Start Here\n\n"
   "First paragraph.\n\n"
   "  Second paragraph.\n"])

(def source
  "The exact fixture source reconstructed by `source-parts`."
  (apply str source-parts))

(def principal
  "Server-authenticated reviewer identity used by receipt fixtures."
  {:principal/user-id "reviewer-1"
   :principal/user-email "reviewer@open-hax.local"
   :principal/membership-id "membership-1"})

(def recorded-at
  "A valid fixed-width comparable server clock reading."
  "2026-08-30T12:00:00.000Z")

(defn manifest
  "Build the three-member fixture manifest."
  []
  (split/split-manifest
   digest
   (assoc coordinates :source-text source :source-parts source-parts)))

(defn claim
  "Build the pre-provider candidate claim for a fixture manifest."
  [value]
  (split/candidate-claim digest value "candidate-revision-1"))

(defn candidates
  "Return candidate members intentionally in reverse provider order."
  [candidate-claim]
  (let [[first-member second-member third-member]
        (:candidate-claim/members candidate-claim)]
    [(split/candidate-split digest third-member "  Segundo párrafo.\n")
     (split/candidate-split digest second-member "Primer párrafo.\n\n")
     (split/candidate-split digest first-member "# Empieza aquí\n\n")]))

(defn candidate-set
  "Complete a fixture candidate set in the claim's admitted order."
  [value candidate-claim]
  (split/complete-candidate-set
   digest value candidate-claim (candidates candidate-claim)))

(defn review-request
  "Build one closed client review request with optional overrides."
  ([] (review-request {}))
  ([overrides]
   (merge {:review/operation-id "review-operation-1"
           :review/adequacy "good"
           :review/fluency "good"
           :review/terminology "correct"
           :review/risk "safe"
           :review/overall "approve"
           :review/corrected-text "Párrafo inicial corregido.\n\n"
           :review/editor-notes "Terminology normalized."}
          overrides)))

(defn review-receipt
  "Attribute one request to authenticated fixture evidence."
  ([value complete-set split-id request]
   (review-receipt value complete-set split-id principal recorded-at request))
  ([value complete-set split-id reviewer at request]
   (split/review-receipt digest value complete-set split-id reviewer at request)))
