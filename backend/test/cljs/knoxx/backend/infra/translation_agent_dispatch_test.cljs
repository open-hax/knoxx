(ns knoxx.backend.infra.translation-agent-dispatch-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.translation-agent-dispatch :as dispatch]
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
   :dispatch/source-digest "sha256-abc123"})

(def ^:private source "Open Hax is a garden for tools, research, art, and systems.")

(defn- digest-hex [value] (str "h" (hash value)))

(defn- deps
  "Dispatch dependencies over a recording emitter."
  [evidence-store emitted & {:keys [emit-result throw-on-emit]}]
  {:evidence-store evidence-store
   :clock (constantly "2026-08-26T16:00:00.000Z")
   :digest-hex digest-hex
   :emit! (fn [event]
            (swap! emitted conj event)
            (if throw-on-emit
              (js/Promise.reject (js/Error. "the dispatcher is not running"))
              (js/Promise.resolve (or emit-result
                                      {:matchedTriggers [:publication/translation-needed]}))))})

(deftest ^:async claimed-work-binds-its-run-before-the-event-is-emitted
  (let [evidence-store (store/memory-store)
        emitted (atom [])
        result (await (dispatch/dispatch-work! (deps evidence-store emitted)
                                               work context source))
        record (:dispatch/record result)
        event (first @emitted)]

    (testing "the work was accepted and exactly one event announced it"
      (is (= :dispatch/accepted (:dispatch/outcome result)))
      (is (= 1 (count @emitted))))

    (testing "the claim carries the run id before the event goes out"
      ;; The ordering the whole namespace exists for: a session that submits
      ;; immediately must find a claim that already names its run.
      (is (= (:translation/run-id result) (:dispatch/batch-id record)))
      (is (= (:translation/run-id result)
             (get-in event [:event/payload :resource-policies :run_id]))))

    (testing "the event is the contract the trigger subscribes to"
      (is (= agent-law/event-type (:event/type event)))
      (is (= (dispatch/event-id (:translation/run-id result)) (:event/id event))))

    (testing "the event carries the pin, so the action forwards one it never builds"
      (is (agent-law/contract-backed?
           (get-in event [:event/payload :resource-policies])))
      (is (= (:dispatch/key record)
             (get-in event [:event/payload :resource-policies :dispatch_key]))))

    (testing "the output revision is derivable from the bound claim"
      ;; Without the binding this throws, which is what would make a fast
      ;; submission unjoinable.
      (is (string? (dispatch-law/output-revision record))))))

(deftest ^:async asking-twice-does-not-start-two-sessions
  (let [evidence-store (store/memory-store)
        emitted (atom [])
        d (deps evidence-store emitted)
        first-pass (await (dispatch/dispatch-work! d work context source))
        second-pass (await (dispatch/dispatch-work! d work context source))]

    (testing "the second pass is a duplicate, not a second announcement"
      (is (= :dispatch/accepted (:dispatch/outcome first-pass)))
      (is (= :dispatch/duplicate (:dispatch/outcome second-pass)))
      (is (= 1 (count @emitted))))

    (testing "the duplicate names the recovery this path does not have"
      ;; An in-flight agent claim cannot be settled by re-reading anything, so
      ;; the gap is reported every time rather than left to be rediscovered.
      (is (= (:gap/summary dispatch/known-gap) (:dispatch/detail second-pass))))))

(deftest ^:async an-event-nobody-subscribes-to-is-reported-rather-than-assumed-handled
  (let [evidence-store (store/memory-store)
        emitted (atom [])
        result (await (dispatch/dispatch-work!
                       (deps evidence-store emitted :emit-result {:matchedTriggers []})
                       work context source))]
    (testing "the claim is accepted but the report says nothing will translate it"
      (is (= :dispatch/accepted (:dispatch/outcome result)))
      (is (re-find #"no enabled trigger subscribes" (:dispatch/detail result))))))

(deftest ^:async a-dispatcher-that-throws-leaves-retriable-work
  (let [evidence-store (store/memory-store)
        emitted (atom [])
        result (await (dispatch/dispatch-work!
                       (deps evidence-store emitted :throw-on-emit true)
                       work context source))]

    (testing "the failure is conclusive, because an in-process throw ran no action"
      (is (= :dispatch/failed (:dispatch/outcome result)))
      (is (re-find #"dispatcher is not running" (:dispatch/detail result))))

    (testing "a failed claim is retriable, so the next pass re-announces the work"
      (is (dispatch-law/retriable?
           (:dispatch/outcome (await (store/dispatch-for-key!
                                      evidence-store
                                      (:dispatch/key (:dispatch/record result)))))))
      (let [retry (await (dispatch/dispatch-work! (deps evidence-store emitted)
                                                   work context source))]
        (is (= :dispatch/accepted (:dispatch/outcome retry)))))))

(deftest ^:async a-retry-produces-a-new-run-and-therefore-a-new-output-revision
  (let [evidence-store (store/memory-store)
        emitted (atom [])
        failed (await (dispatch/dispatch-work!
                       (deps evidence-store emitted :throw-on-emit true)
                       work context source))
        retried (await (dispatch/dispatch-work!
                        (assoc (deps evidence-store emitted)
                               :clock (constantly "2026-08-26T17:00:00.000Z"))
                        work context source))]

    (testing "the retry reuses the dispatch key but not the run"
      (is (= (:dispatch/key (:dispatch/record failed))
             (:dispatch/key (:dispatch/record retried))))
      (is (not= (:dispatch/batch-id (:dispatch/record failed))
                (:dispatch/batch-id (:dispatch/record retried)))))

    (testing "so an approval of the first translation cannot authorize the second"
      (is (not= (dispatch-law/output-revision (:dispatch/record retried))
                (str (:dispatch/revision (:dispatch/record retried))
                     "+de@" (:dispatch/batch-id (:dispatch/record failed))))))))

(deftest ^:async a-pin-that-names-bytes-nobody-has-is-refused-and-not-persisted
  (let [evidence-store (store/memory-store)
        emitted (atom [])
        pinned {:document :open-hax.documents/promethean
                :locale :de
                :revision "sha256-a-revision-that-was-never-on-disk"
                :replace-stale? false}
        result (await (dispatch/dispatch-work! (deps evidence-store emitted)
                                               pinned context source))]

    (testing "the refusal is terminal, because no retry makes a pin resolvable"
      (is (= dispatch-law/unreachable-outcome (:dispatch/outcome result)))
      (is (some? (:translation/refusal result)))
      (is (empty? @emitted)))

    (testing "nothing was written, so restoring the pinned bytes unblocks it"
      ;; A pin refusal is a statement about current state, not an observed fact.
      (is (nil? (await (store/dispatch-for-key!
                        evidence-store
                        (:dispatch/key (:dispatch/record result)))))))))
