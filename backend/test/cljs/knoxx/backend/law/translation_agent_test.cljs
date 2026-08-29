(ns knoxx.backend.law.translation-agent-test
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [knoxx.backend.law.translation-agent :as law]
            [knoxx.backend.law.translation-dispatch :as dispatch-law]
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
  [& {:keys [at batch-id project]
      :or {at "2026-08-26T16:00:00.000Z"}}]
  (dispatch-law/dispatch-record work
                                (cond-> context
                                  (some? project) (assoc :dispatch/project project))
                                :dispatch/accepted
                                at
                                :batch-id batch-id))

(defn- digest-hex
  "A stand-in hash: injective over the inputs these tests use, which is the only
   property `run-id` relies on."
  [value]
  (str "h" (hash value)))

;; ── The run identity ────────────────────────────────────────────────────────

(deftest run-id-is-stable-per-attempt-and-changes-between-attempts
  (testing "the same claim at the same instant mints the same run id"
    (is (= (law/run-id (record) digest-hex)
           (law/run-id (record) digest-hex))))

  (testing "a replaced claim — same key, new instant — mints a different run id"
    ;; The whole reason the instant is in the derivation. A run id derived from
    ;; the dispatch key alone would be identical here, the output revision would
    ;; not change, and an approval of the first translation would authorize the
    ;; second.
    (let [first-attempt (record :at "2026-08-26T16:00:00.000Z")
          second-attempt (record :at "2026-08-26T17:00:00.000Z")]
      (is (= (:dispatch/key first-attempt) (:dispatch/key second-attempt))
          "the key must be stable, or this test is not about what it claims")
      (is (not= (law/run-id first-attempt digest-hex)
                (law/run-id second-attempt digest-hex)))))

  (testing "the run id names what it is"
    (is (str/starts-with? (law/run-id (record) digest-hex) law/run-id-prefix)))

  (testing "a claim missing its key or instant cannot mint one"
    (is (thrown? js/Error (law/run-id (dissoc (record) :dispatch/key) digest-hex)))
    (is (thrown? js/Error (law/run-id (dissoc (record) :dispatch/at) digest-hex)))))

;; ── Pinning ─────────────────────────────────────────────────────────────────

(deftest session-policies-pin-every-coordinate-from-the-claim
  (let [claim (record)
        policies (law/session-policies claim "translation-run-x")]
    (testing "the overlay validates and carries the claim's own coordinates"
      (is (m/validate law/SessionPolicies policies))
      (is (= "open-hax.documents/promethean" (:document_id policies)))
      (is (= "open-hax.gardens/promethean" (:garden_id policies)))
      (is (= "en" (:source_lang policies)))
      (is (= "de" (:target_lang policies)))
      (is (= "open-hax" (:org_id policies)))
      (is (= (:dispatch/key claim) (:dispatch_key policies)))
      (is (= "translation-run-x" (:run_id policies))))

    (testing "a claim naming no project produces an overlay naming none"
      (is (not (contains? policies :project)))))

  (testing "a project on the claim travels onto the overlay"
    (is (= "open-hax-site"
           (:project (law/session-policies (record :project "open-hax-site")
                                           "translation-run-x")))))

  (testing "a blank run id is refused rather than pinning a session to nothing"
    (is (thrown? js/Error (law/session-policies (record) "")))
    (is (thrown? js/Error (law/session-policies (record) nil)))))

(deftest contract-backed-requires-both-halves-of-the-pin
  (let [policies (law/session-policies (record) "translation-run-x")]
    (is (law/contract-backed? policies))

    (testing "neither half alone selects the contract-backed sink"
      ;; Half a pin is a misconfigured trigger. Treating it as a CMS translation
      ;; would send publication content to OpenPlanner; treating it as
      ;; publication content would record evidence against a claim it cannot
      ;; find.
      (is (not (law/contract-backed? (dissoc policies :run_id))))
      (is (not (law/contract-backed? (dissoc policies :dispatch_key))))
      (is (not (law/contract-backed? (assoc policies :run_id "  "))))
      (is (not (law/contract-backed? nil))))

    (testing "an ordinary CMS session's policies do not select it"
      (is (not (law/contract-backed? {:document_id "docs/thing"
                                      :target_lang "es"}))))))

;; ── The event ───────────────────────────────────────────────────────────────

(deftest translation-needed-event-carries-the-pin-and-the-dispatched-bytes
  (let [claim (record)
        source "# Title\n\nA paragraph.\n"
        event (law/translation-needed-event claim "translation-run-x" source)
        payload (:event/payload event)]
    (testing "the event validates and names one document, locale pair and revision"
      (is (m/validate law/TranslationNeededEvent event))
      (is (= law/event-type (:event/type event)))
      (is (= :open-hax.documents/promethean (:document payload)))
      (is (= :en (:source-locale payload)))
      (is (= :de (:locale payload)))
      (is (= "sha256-abc123" (:revision payload))))

    (testing "the overlay rides along, so the action forwards a pin it never builds"
      (is (= (law/session-policies claim "translation-run-x")
             (:resource-policies payload))))

    (testing "the brief embeds the dispatched bytes verbatim"
      ;; What makes the receipt honest: the agent translates the bytes the claim
      ;; was taken for, rather than re-reading a file that may have moved.
      (is (str/includes? (:content payload) source))
      (is (str/includes? (:content payload) "from en"))
      (is (str/includes? (:content payload) "into de")))

    (testing "the brief is delimited by something a Markdown code fence cannot close"
      (let [fenced (law/translation-brief :en :de "text\n```\ncode\n```\nmore")]
        (is (str/includes? fenced "<<<SOURCE-DOCUMENT"))
        (is (str/includes? fenced "SOURCE-DOCUMENT>>>"))
        (is (str/ends-with? fenced "SOURCE-DOCUMENT>>>"))))))

(deftest translation-needed-event-refuses-what-it-cannot-honestly-carry
  (testing "an oversize document is refused, never truncated"
    ;; A truncated source would be translated in full by an agent with no way to
    ;; know it read a fragment, and the receipt would attest that the whole
    ;; revision was translated.
    (is (thrown? js/Error
                 (law/translation-needed-event
                  (record)
                  "translation-run-x"
                  (apply str (repeat (inc law/max-brief-chars) "x"))))))

  (testing "an empty source is refused"
    (is (thrown? js/Error (law/translation-needed-event (record) "translation-run-x" "")))
    (is (thrown? js/Error (law/translation-needed-event (record) "translation-run-x" "   "))))

  (testing "an extra payload key cannot be smuggled onto a closed event"
    (is (not (m/validate law/TranslationNeededEvent
                         (update (law/translation-needed-event (record)
                                                               "translation-run-x"
                                                               "text")
                                 :event/payload assoc :approved true))))))
