(ns knoxx.backend.infra.translation-agent-sink-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.translation-agent-content :as content]
            [knoxx.backend.infra.translation-agent-sink :as sink]
            [knoxx.backend.infra.translation-evidence-store :as store]
            [knoxx.backend.law.translation-agent :as agent-law]
            [knoxx.backend.law.translation-dispatch :as dispatch-law]))

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
   :dispatch/membership-id "member-1"
   ;; Supplied by `infra.translation-dispatch/dispatch-context` in production.
   ;; Without it every completion is refused as `:source-unverifiable` — the
   ;; drift guard compares digest to digest, never digest to pinned revision.
   :dispatch/source-digest "sha256-abc123"})

(def ^:private at "2026-08-26T16:00:00.000Z")
(def ^:private run-id "translation-run-abc")

(def ^:private source "Open Hax is a garden for tools, research, art, and systems.")
(def ^:private translated "Open Hax ist ein Garten für Werkzeuge, Forschung, Kunst und Systeme.")

(defn- ^:async claim!
  "A reserved, run-bound claim — the state a translation session starts in.

   Both steps, in this order, because that order is the composition's whole
   point: `infra.translation-agent-dispatch` binds the run id before the event
   is emitted so a fast submission can still be joined."
  [evidence-store]
  (let [record (dispatch-law/dispatch-record work context :dispatch/accepted at)]
    (await (store/reserve-dispatch! evidence-store record))
    (await (store/bind-dispatch-batch! evidence-store (:dispatch/key record) run-id))))

(defn- deps
  "Sink dependencies whose source-revision observer agrees with the claim."
  [root evidence-store]
  {:content-root root
   :evidence-store evidence-store
   :clock (constantly "2026-08-26T16:05:00.000Z")
   :observe-source-revision (fn [_record] (js/Promise.resolve (:revision work)))})

(defn- pair
  [& {:as overrides}]
  (merge {:source_text source
          :translated_text translated
          :segment_index 0}
         overrides))

(deftest ^:async a-submitted-pair-becomes-a-receipt-and-readable-content
  (let [root "/tmp/knoxx-translation-agent-sink-test/accepted"
        evidence-store (store/memory-store)
        bound (await (claim! evidence-store))
        policies (agent-law/session-policies bound run-id)
        result (await (sink/submit-pair! (deps root evidence-store) policies (pair)))
        receipt (:translation/receipt result)]

    (testing "the submission produced translation evidence, not a refusal"
      (is (nil? (:translation/refusal result)))
      (is (some? receipt)))

    (testing "the receipt's identity comes from the claim, never from the submission"
      (is (= :open-hax.documents/promethean (:translation/document receipt)))
      (is (= :open-hax.gardens/promethean (:translation/garden receipt)))
      (is (= :de (:translation/locale receipt)))
      (is (= :en (:translation/source-locale receipt)))
      (is (= "sha256-abc123" (:translation/source-revision receipt)))
      (is (= "open-hax" (:translation/org-id receipt))))

    (testing "the output revision names the producing run, so a re-run supersedes it"
      (is (= (dispatch-law/output-revision bound) (:translation/revision receipt)))
      (is (not= (:translation/source-revision receipt)
                (:translation/revision receipt))))

    (testing "the bytes are readable back through the receipt that attests to them"
      (is (= translated (await (content/content-for-receipt! root receipt)))))

    (testing "another run's receipt cannot read these bytes"
      ;; The guard that stops a second run's output being served under the first
      ;; run's approval.
      (is (nil? (await (content/content-for-receipt!
                        root
                        (assoc receipt :translation/revision
                               "sha256-abc123+de@another-run"))))))

    (testing "a receipt disagreeing about the document cannot read them either"
      (is (nil? (await (content/content-for-receipt!
                        root
                        (assoc receipt :translation/document :other/doc))))))

    (testing "the claim is settled, so a later pass reports duplicate rather than re-running"
      (is (= :dispatch/completed
             (:dispatch/outcome (await (store/dispatch-for-key!
                                        evidence-store (:dispatch/key bound)))))))))

(deftest ^:async a-pair-with-nothing-to-join-is-refused-and-the-claim-survives
  (let [root "/tmp/knoxx-translation-agent-sink-test/unjoinable"
        evidence-store (store/memory-store)
        bound (await (claim! evidence-store))
        policies (agent-law/session-policies bound run-id)]

    (testing "a run id no claim is bound to is refused"
      (is (= :dispatch-record-missing
             (:refusal/type
              (:translation/refusal
               (await (sink/submit-pair! (deps root evidence-store)
                                         (assoc policies :run_id "translation-run-nobody")
                                         (pair))))))))

    (testing "a dispatch key disagreeing with the claim the run id found is refused"
      ;; Defence in depth: the run id is derived from the key, so disagreement
      ;; means a store returned a record it was not asked for, or a trigger built
      ;; its overlay from one claim and its run id from another.
      (is (= :worker-batch-mismatch
             (:refusal/type
              (:translation/refusal
               (await (sink/submit-pair! (deps root evidence-store)
                                         (assoc policies :dispatch_key "some|other|key")
                                         (pair))))))))

    (testing "no refusal settled the claim, so a corrected submission still lands"
      (is (= :dispatch/accepted
             (:dispatch/outcome (await (store/dispatch-for-key!
                                        evidence-store (:dispatch/key bound))))))
      (is (some? (:translation/receipt
                  (await (sink/submit-pair! (deps root evidence-store)
                                            policies
                                            (pair)))))))))

(deftest ^:async an-unusable-pair-is-refused-before-the-claim-is-read
  (let [root "/tmp/knoxx-translation-agent-sink-test/unusable"
        evidence-store (store/memory-store)
        bound (await (claim! evidence-store))
        policies (agent-law/session-policies bound run-id)]

    (testing "a blank translation is refused"
      (is (= :pair-translation-missing
             (:refusal/type
              (:translation/refusal
               (await (sink/submit-pair! (deps root evidence-store) policies
                                         (pair :translated_text ""))))))))

    (testing "prose echoed back untranslated is refused"
      (is (= :pair-translation-untranslated
             (:refusal/type
              (:translation/refusal
               (await (sink/submit-pair! (deps root evidence-store) policies
                                         (pair :translated_text source))))))))

    (testing "a numbered segment is refused for a whole-file document"
      (is (= :pair-segmented-document
             (:refusal/type
              (:translation/refusal
               (await (sink/submit-pair! (deps root evidence-store) policies
                                         (pair :segment_index 2))))))))

    (testing "a submission about another document is refused"
      (is (= :pair-document-mismatch
             (:refusal/type
              (:translation/refusal
               (await (sink/submit-pair! (deps root evidence-store) policies
                                         (pair :document_id "other/doc"))))))))

    (testing "the claim is untouched by all of them"
      (is (= :dispatch/accepted
             (:dispatch/outcome (await (store/dispatch-for-key!
                                        evidence-store (:dispatch/key bound)))))))))

(deftest ^:async a-completion-whose-source-moved-mints-no-receipt
  (let [root "/tmp/knoxx-translation-agent-sink-test/drifted"
        evidence-store (store/memory-store)
        bound (await (claim! evidence-store))
        policies (agent-law/session-policies bound run-id)
        moved (assoc (deps root evidence-store)
                     :observe-source-revision
                     (fn [_record] (js/Promise.resolve "sha256-something-else")))
        result (await (sink/submit-pair! moved policies (pair)))]

    (testing "the drift guard refuses rather than attesting to bytes nobody translated"
      (is (nil? (:translation/receipt result)))
      (is (= :source-moved-since-dispatch
             (:refusal/type (:translation/refusal result)))))

    (testing "the claim is terminal, because retrying this exact revision cannot succeed"
      (is (= dispatch-law/unreachable-outcome
             (:dispatch/outcome (await (store/dispatch-for-key!
                                        evidence-store (:dispatch/key bound)))))))))

(deftest ^:async a-refusal-becomes-an-error-an-agent-can-act-on
  (testing "a pair refusal becomes the sentence the law wrote for it"
    (let [error (sink/refusal-error {:refusal/type :pair-segmented-document})]
      (is (= (agent-law/nonblank-string? (ex-message error)) true))
      (is (re-find #"one save_translation call" (ex-message error)))))

  (testing "a dispatch-layer refusal says it is a misconfiguration, not the agent's fault"
    (let [error (sink/refusal-error {:refusal/type :dispatch-record-missing})]
      (is (re-find #"not bound to a live publication" (ex-message error)))
      (is (= :dispatch-record-missing (:refusal/type (ex-data error)))))))
