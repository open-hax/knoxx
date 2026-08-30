(ns knoxx.backend.law.translation-agent-test
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [knoxx.backend.law.translation-agent :as law]
            [knoxx.backend.law.translation-dispatch :as dispatch-law]
            [knoxx.backend.law.translation-source-split :as source-split]
            [knoxx.backend.law.translation-split :as split]
            [malli.core :as m]))

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
   :dispatch/membership-id "member-1"})

(defn- record
  [& {:keys [at attempt-id batch-id project]
      :or {at "2026-08-26T16:00:00.000Z"
           attempt-id "dispatch-attempt-1"}}]
  (dispatch-law/dispatch-record work
                                (cond-> context
                                  (some? project) (assoc :dispatch/project project))
                                :dispatch/accepted
                                at
                                :attempt-id attempt-id
                                :batch-id batch-id))

(defn- digest-hex
  "A stand-in hash: injective over the inputs these tests use, which is the only
   property `run-id` relies on."
  [value]
  (str "h" (hash value)))

(defn- translation-turn
  "Build the atomic split authority one bound claim emits."
  [claim source]
  (let [manifest (split/split-manifest
                  digest-hex
                  (cond-> {:org-id (:dispatch/org-id claim)
                           :garden (:dispatch/garden claim)
                           :document (:dispatch/document claim)
                           :source-locale (:dispatch/source-locale claim)
                           :target-locale (:dispatch/locale claim)
                           :source-revision (:dispatch/revision claim)
                           :source-text source
                           :source-parts (source-split/source-parts source)}
                    (some? (:dispatch/project claim))
                    (assoc :project (:dispatch/project claim))))
        candidate-claim (split/candidate-claim
                         digest-hex manifest
                         (dispatch-law/output-revision claim))]
    (split/translation-turn-admission
     digest-hex
     {:dispatch-key (:dispatch/key claim)
      :run-id (:dispatch/batch-id claim)
      :admitted-at (:dispatch/at claim)
      :manifest manifest
      :candidate-claim candidate-claim
      :execution
      (split/execution-snapshot
       digest-hex
       {:agent-id "publication_translator"
        :model "gemma4:31b"
        :thinking :medium
        :system-prompt "Translate the admitted source splits."
        :tool-ids ["save_translation"]})
      :memory (split/memory-snapshot {:status :empty :examples []})})))

;; ── The run identity ────────────────────────────────────────────────────────

(deftest run-id-is-stable-per-attempt-and-changes-between-attempts
  (testing "the same claim at the same instant mints the same run id"
    (is (= (law/run-id (record) digest-hex)
           (law/run-id (record) digest-hex))))

  (testing "a replaced claim differs even when both are admitted in one millisecond"
    ;; A run id derived from the stable key or millisecond alone would be
    ;; identical here, and an approval of the first translation could authorize
    ;; the second.
    (let [first-attempt (record :attempt-id "dispatch-attempt-a")
          second-attempt (record :attempt-id "dispatch-attempt-b")]
      (is (= (:dispatch/key first-attempt) (:dispatch/key second-attempt))
          "the key must be stable, or this test is not about what it claims")
      (is (not= (law/run-id first-attempt digest-hex)
                (law/run-id second-attempt digest-hex)))))

  (testing "the run id names what it is"
    (is (str/starts-with? (law/run-id (record) digest-hex) law/run-id-prefix)))

  (testing "a claim missing its key or every attempt discriminator cannot mint one"
    (is (thrown? js/Error (law/run-id (dissoc (record) :dispatch/key) digest-hex)))
    (is (thrown? js/Error
                 (law/run-id (dissoc (record)
                                     :dispatch/attempt-id :dispatch/at)
                             digest-hex)))))

;; ── Pinning ─────────────────────────────────────────────────────────────────

(deftest session-policies-pin-every-coordinate-from-the-claim
  (let [claim (record :batch-id "translation-run-x")
        turn (translation-turn claim "One paragraph.")
        policies (law/session-policies claim turn)]
    (testing "the overlay validates and carries the claim's own coordinates"
      (is (m/validate law/SessionPolicies policies))
      (is (= "open-hax.documents/promethean" (:document_id policies)))
      (is (= "open-hax.gardens/promethean" (:garden_id policies)))
      (is (= "en" (:source_lang policies)))
      (is (= "de" (:target_lang policies)))
      (is (= "open-hax" (:org_id policies)))
      (is (= (:dispatch/key claim) (:dispatch_key policies)))
      (is (= "translation-run-x" (:run_id policies)))
      (is (= (:translation-turn/id turn) (:translation_turn_id policies))))

    (testing "a claim naming no project produces an overlay naming none"
      (is (not (contains? policies :project)))))

  (testing "a project on the claim travels onto the overlay"
    (let [claim (record :project "open-hax-site"
                        :batch-id "translation-run-x")]
      (is (= "open-hax-site"
             (:project (law/session-policies
                        claim (translation-turn claim "One paragraph.")))))))

  (testing "a blank run id is refused rather than pinning a session to nothing"
    (let [claim (record :batch-id "translation-run-x")
          turn (translation-turn claim "One paragraph.")]
      (is (thrown? js/Error
                   (law/session-policies
                    claim (assoc turn :translation-turn/run-id ""))))
      (is (thrown? js/Error
                   (law/session-policies
                    claim (assoc turn :translation-turn/run-id nil)))))))

(deftest contract-backed-requires-both-halves-of-the-pin
  (let [claim (record :batch-id "translation-run-x")
        policies (law/session-policies
                  claim (translation-turn claim "One paragraph."))]
    (is (law/contract-backed? policies))
    (is (law/split-backed? policies))

    (testing "neither half alone selects the contract-backed sink"
      ;; Half a pin is a misconfigured trigger. Treating it as a CMS translation
      ;; would send publication content to OpenPlanner; treating it as
      ;; publication content would record evidence against a claim it cannot
      ;; find.
      (is (not (law/contract-backed? (dissoc policies :run_id))))
      (is (not (law/contract-backed? (dissoc policies :dispatch_key))))
      (is (not (law/contract-backed? (assoc policies :run_id "  "))))
      (is (not (law/contract-backed? nil)))
      (is (not (law/split-backed? (dissoc policies :translation_turn_id)))))

    (testing "an ordinary CMS session's policies do not select it"
      (is (not (law/contract-backed? {:document_id "docs/thing"
                                      :target_lang "es"}))))))

;; ── The event ───────────────────────────────────────────────────────────────

(deftest translation-needed-event-carries-the-pin-and-the-dispatched-bytes
  (let [claim (record :batch-id "translation-run-x")
        source "# Title\n\nA paragraph.\n"
        turn (translation-turn claim source)
        event (law/translation-needed-event claim turn)
        payload (:event/payload event)]
    (testing "the event validates and names one document, locale pair and revision"
      (is (m/validate law/TranslationNeededEvent event))
      (is (= law/event-type (:event/type event)))
      (is (= :open-hax.documents/promethean (:document payload)))
      (is (= :en (:source-locale payload)))
      (is (= :de (:locale payload)))
      (is (= "sha256-abc123" (:revision payload))))

    (testing "the overlay rides along, so the action forwards a pin it never builds"
      (is (= (law/session-policies claim turn)
             (:resource-policies payload))))

    (testing "the brief embeds every dispatched split's bytes verbatim"
      ;; What makes the receipt honest: the agent translates each admitted byte
      ;; range rather than re-reading a file that may have moved. Metadata lives
      ;; between splits, so the unsplit document is intentionally not contiguous.
      (doseq [source-split (get-in turn [:translation-turn/manifest
                                         :split-manifest/splits])]
        (is (str/includes? (:content payload) (:split/source-text source-split))))
      (is (str/includes? (:content payload) "from en"))
      (is (str/includes? (:content payload) "into de")))

    (testing "the brief names every boundary and immutable attempt explicitly"
      (is (str/includes? (:content payload) "split_id:"))
      (is (str/includes? (:content payload) "attempt_id:"))
      (is (str/includes? (:content payload) "segment_index:"))
      (is (= 2 (:split-count payload))))))

(deftest translation-needed-event-refuses-what-it-cannot-honestly-carry
  (testing "an oversize document is refused, never truncated"
    ;; A truncated source would be translated in full by an agent with no way to
    ;; know it read a fragment, and the receipt would attest that the whole
    ;; revision was translated.
    (is (thrown? js/Error
                 (let [claim (record :batch-id "translation-run-x")]
                   (law/translation-needed-event
                    claim
                    (translation-turn
                     claim (apply str (repeat (inc law/max-brief-chars) "x"))))))))

  (testing "a blank source cannot become event authority"
    (let [claim (record :batch-id "translation-run-x")]
      (is (thrown? js/Error (translation-turn claim "")))
      (is (thrown? js/Error (translation-turn claim "   ")))))

  (testing "an extra payload key cannot be smuggled onto a closed event"
    (let [claim (record :batch-id "translation-run-x")]
      (is (not (m/validate law/TranslationNeededEvent
                           (update (law/translation-needed-event
                                    claim (translation-turn claim "text"))
                                   :event/payload assoc :approved true)))))))
