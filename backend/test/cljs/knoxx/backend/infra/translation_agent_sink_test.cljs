(ns knoxx.backend.infra.translation-agent-sink-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.translation-agent-content :as content]
            [knoxx.backend.infra.translation-agent-sink :as sink]
            [knoxx.backend.infra.translation-evidence-store :as evidence-store]
            [knoxx.backend.infra.translation-split-store :as split-store]
            [knoxx.backend.law.translation-agent :as agent-law]
            [knoxx.backend.law.translation-dispatch :as dispatch-law]
            [knoxx.backend.law.translation-source-split :as source-split]
            [knoxx.backend.law.translation-split :as split-law]))

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
   :dispatch/project "promethean"
   :dispatch/membership-id "member-1"
   :dispatch/source-digest "sha256-abc123"})

(def ^:private at "2026-08-26T16:00:00.000Z")
(def ^:private run-id "translation-run-abc")
(def ^:private source "# Open Hax\n\nA garden for tools.\n")
(def ^:private translations ["# Offenes Hax\n\n" "Ein Garten für Werkzeuge.\n"])

(defn- digest-hex [value] (str "h" (hash value)))

(defn- turn
  [record]
  (let [manifest (split-law/split-manifest
                  digest-hex
                  {:org-id "open-hax"
                   :project "promethean"
                   :garden :open-hax.gardens/promethean
                   :document :open-hax.documents/promethean
                   :source-locale :en
                   :target-locale :de
                   :source-revision (:revision work)
                   :source-text source
                   :source-parts (source-split/source-parts source)})
        claim (split-law/candidate-claim digest-hex manifest
                                         (dispatch-law/output-revision record))]
    (split-law/translation-turn-admission
     digest-hex
     {:dispatch-key (:dispatch/key record)
      :run-id run-id
      :admitted-at at
      :manifest manifest
      :candidate-claim claim
      :execution (split-law/execution-snapshot
                  digest-hex
                  {:agent-id "publication_translator"
                   :model "model"
                   :thinking :medium
                   :system-prompt "Translate every admitted split."
                   :tool-ids ["save_translation"]})
      :memory (split-law/memory-snapshot {:status :empty :examples []})})))

(defn- ^:async admitted!
  []
  (let [evidence (evidence-store/memory-store)
        splits (split-store/memory-store digest-hex)
        record (dispatch-law/dispatch-record work context :dispatch/accepted at
                                             :attempt-id "dispatch-attempt-1")]
    (await (evidence-store/reserve-dispatch! evidence record))
    (let [bound (await (evidence-store/bind-dispatch-batch!
                        evidence record run-id))
          admitted-turn (turn bound)]
      (await (split-store/admit-turn! splits admitted-turn))
      {:evidence evidence :splits splits :record bound :turn admitted-turn})))

(defn- deps
  ([root state]
   (deps root state (fn [_] (js/Promise.resolve nil))))
  ([root {:keys [evidence splits]} emit-candidate-events!]
   {:content-root root
    :evidence-store evidence
    :split-store splits
    :digest-hex digest-hex
    :clock (constantly "2026-08-26T16:05:00.000Z")
    :emit-candidate-events! emit-candidate-events!
    :observe-source-revision (fn [_] (js/Promise.resolve "sha256-abc123"))}))

(defn- pair
  [admitted-turn index & {:as overrides}]
  (let [source-member (get-in admitted-turn
                              [:translation-turn/manifest
                               :split-manifest/splits index])
        claim-member (get-in admitted-turn
                             [:translation-turn/candidate-claim
                              :candidate-claim/members index])]
    (merge {:source_text (:split/source-text source-member)
            :translated_text (nth translations index)
            :segment_index index
            :split_id (:split/id source-member)
            :attempt_id (:candidate-claim-member/attempt-id claim-member)}
           overrides)))

(deftest ^:async split-submissions-stay-partial-until-exact-coverage
  (let [{:keys [turn record evidence] :as state} (await (admitted!))
        root "/tmp/knoxx-translation-agent-sink-test/coverage"
        policies (agent-law/session-policies record turn)
        first-result (await (sink/submit-pair! (deps root state) policies
                                               (pair turn 0)))]
    (testing "one durable split is progress, never a whole-document receipt"
      (is (= {:completed 1 :total 2}
             (select-keys (:translation/progress first-result)
                          [:completed :total])))
      (is (nil? (:translation/receipt first-result)))
      (is (empty? (await (evidence-store/completed-translations!
                          evidence {:org-id "open-hax" :project "promethean"})))))

    (let [result (await (sink/submit-pair! (deps root state) policies
                                           (pair turn 1)))
          receipt (:translation/receipt result)
          candidate-set (await (split-store/candidate-set-for-turn!
                                (:splits state) (:translation-turn/id turn)))]
      (testing "exact coverage creates one lineage-bound raw candidate receipt"
        (is (some? receipt))
        (is (= (:candidate-set/id candidate-set)
               (:translation/candidate-set-id receipt)))
        (is (= (:candidate-set/digest candidate-set)
               (:translation/candidate-set-digest receipt)))
        (is (= 2 (:translation/split-count receipt)))
        (is (= :dispatch/completed
               (:dispatch/outcome
                (await (evidence-store/dispatch-for-key!
                        evidence (:dispatch/key record)))))))

      (testing "content is composed in manifest order, not arrival order"
        (is (= (apply str translations)
               (await (content/content-for-receipt! root receipt))))))))

(deftest ^:async partial-crash-replay-preserves-first-bytes-and-finishes-missing-splits
  (let [{:keys [turn record splits] :as state} (await (admitted!))
        root "/tmp/knoxx-translation-agent-sink-test/partial-crash-replay"
        policies (agent-law/session-policies record turn)
        first (await (sink/submit-pair! (deps root state) policies
                                        (pair turn 0)))
        replayed (await
                  (sink/submit-pair!
                   (deps root state) policies
                   (pair turn 0 :translated_text
                         "# Später abweichender Modelltext\n\n")))
        stored (await (split-store/candidate-splits-for-turn!
                       splits (:translation-turn/id turn)))]
    (testing "a nondeterministic replay adopts the first durable partial member"
      (is (= {:completed 1 :total 2}
             (select-keys (:translation/progress first) [:completed :total])))
      (is (= {:completed 1 :total 2}
             (select-keys (:translation/progress replayed)
                          [:completed :total])))
      (is (nil? (:translation/refusal replayed)))
      (is (= 1 (count stored)))
      (is (= (first translations) (:candidate/text (first stored)))))
    (let [completed (await (sink/submit-pair! (deps root state) policies
                                               (pair turn 1)))
          receipt (:translation/receipt completed)]
      (testing "the replay can supply its missing member and settle normally"
        (is (some? receipt))
        (is (= (apply str translations)
               (await (content/content-for-receipt! root receipt))))))))

(deftest ^:async admitted-source-bytes-fill-an-omitted-model-echo
  (let [{:keys [turn record splits] :as state} (await (admitted!))
        policies (agent-law/session-policies record turn)
        without-source (dissoc (pair turn 0) :source_text)
        result (await
                (sink/submit-pair!
                 (deps "/tmp/knoxx-translation-agent-sink-test/server-source" state)
                 policies
                 without-source))]
    (is (= {:completed 1 :total 2}
           (select-keys (:translation/progress result) [:completed :total])))
    (is (= 1 (count (await (split-store/candidate-splits-for-turn!
                            splits (:translation-turn/id turn))))))))

(deftest ^:async server-filled-source-still-refuses-an-untranslated-pair
  (let [{:keys [turn record splits] :as state} (await (admitted!))
        policies (agent-law/session-policies record turn)
        source-text (get-in turn [:translation-turn/manifest
                                  :split-manifest/splits 0
                                  :split/source-text])
        pair (-> (pair turn 0 :translated_text source-text)
                 (dissoc :source_text))
        result (await
                (sink/submit-pair!
                 (deps "/tmp/knoxx-translation-agent-sink-test/server-source-untranslated" state)
                 policies
                 pair))]
    (is (= :pair-translation-untranslated
           (get-in result [:translation/refusal :refusal/type])))
    (is (empty? (await (split-store/candidate-splits-for-turn!
                        splits (:translation-turn/id turn)))))))

(deftest ^:async out-of-order-splits-compose-in-server-order
  (let [{:keys [turn record] :as state} (await (admitted!))
        root "/tmp/knoxx-translation-agent-sink-test/order"
        policies (agent-law/session-policies record turn)]
    (is (= 1 (get-in (await (sink/submit-pair! (deps root state) policies
                                               (pair turn 1)))
                     [:translation/progress :completed])))
    (let [receipt (:translation/receipt
                   (await (sink/submit-pair! (deps root state) policies
                                             (pair turn 0))))]
      (is (= (apply str translations)
             (await (content/content-for-receipt! root receipt)))))))

(deftest ^:async persisted-turn-must-match-the-complete-dispatch-relation
  (let [evidence (evidence-store/memory-store)
        splits (split-store/memory-store digest-hex)
        mismatched-context (dissoc context :dispatch/project)
        record (dispatch-law/dispatch-record work mismatched-context
                                             :dispatch/accepted at
                                             :attempt-id "dispatch-attempt-mismatch")]
    (await (evidence-store/reserve-dispatch! evidence record))
    (let [bound (await (evidence-store/bind-dispatch-batch!
                        evidence record run-id))
          ;; `turn` deliberately keeps the project that the dispatch lacks.
          admitted-turn (turn bound)]
      (await (split-store/admit-turn! splits admitted-turn))
      (let [result (await
                    (sink/submit-pair!
                     (deps "/tmp/knoxx-translation-agent-sink-test/binding"
                           {:evidence evidence :splits splits})
                     (agent-law/session-policies bound admitted-turn)
                     (pair admitted-turn 0)))]
        (is (= :translation-turn-mismatch
               (get-in result [:translation/refusal :refusal/type])))
        (is (empty? (await (split-store/candidate-splits-for-turn!
                            splits (:translation-turn/id admitted-turn)))))))))

(deftest ^:async immutable-attempts-and-terminal-replays-are-idempotent
  (let [{:keys [turn record evidence] :as state} (await (admitted!))
        root "/tmp/knoxx-translation-agent-sink-test/replay"
        policies (agent-law/session-policies record turn)]
    (await (sink/submit-pair! (deps root state) policies (pair turn 0)))
    (let [first-result (await (sink/submit-pair! (deps root state) policies
                                                 (pair turn 1)))
          replay (await (sink/submit-pair! (deps root state) policies
                                           (pair turn 1)))
          changed (await (sink/submit-pair!
                          (deps root state) policies
                          (pair turn 1 :translated_text "Andere Bytes.\n")))
          receipts (await (evidence-store/completed-translations!
                           evidence {:org-id "open-hax" :project "promethean"}))
          stored-after-refusal
          (await (content/content-for-receipt!
                  root (:translation/receipt first-result)))]
      (is (= (:translation/receipt first-result) (:translation/receipt replay)))
      (is (= 1 (count receipts)))
      (is (= :pair-candidate-conflict
             (get-in changed [:translation/refusal :refusal/type])))
      (is (= (apply str translations) stored-after-refusal)
          "a refused changed replay leaves the first materialized bytes intact"))))

(deftest ^:async candidate-events-follow-the-durable-receipt
  (let [{:keys [turn record evidence] :as state} (await (admitted!))
        emitted (atom [])
        emitter (fn [projection]
                  ((^:async fn []
                    (let [receipts
                          (await (evidence-store/completed-translations!
                                  evidence
                                  {:org-id "open-hax" :project "promethean"}))]
                      (is (= (:receipt/type (:receipt projection))
                             (:receipt/type (first receipts)))
                          "the event boundary runs only after receipt persistence")
                      (swap! emitted conj projection)))))
        runtime-deps (deps "/tmp/knoxx-translation-agent-sink-test/events"
                           state emitter)
        policies (agent-law/session-policies record turn)]
    (await (sink/submit-pair! runtime-deps policies (pair turn 0)))
    (is (empty? @emitted) "partial split coverage emits no completion event")
    (await (sink/submit-pair! runtime-deps policies (pair turn 1)))
    (is (= 1 (count @emitted)))
    (is (= (:translation-turn/id turn)
           (get-in @emitted [0 :turn :translation-turn/id])))))

(deftest ^:async equal-terminal-replay-repairs-a-failed-event-write
  (let [{:keys [turn record evidence] :as state} (await (admitted!))
        root "/tmp/knoxx-translation-agent-sink-test/event-repair"
        policies (agent-law/session-policies record turn)
        attempts (atom 0)
        flaky-emitter
        (fn [_]
          (swap! attempts inc)
          (if (= 1 @attempts)
            (js/Promise.reject (js/Error. "OpenPlanner unavailable"))
            (js/Promise.resolve {:ok true})))
        runtime-deps (deps root state flaky-emitter)]
    (await (sink/submit-pair! runtime-deps policies (pair turn 0)))
    (let [first-outcome
          (try
            (await (sink/submit-pair! runtime-deps policies (pair turn 1)))
            :unexpected-success
            (catch :default err (.-message err)))]
      (is (= "OpenPlanner unavailable" first-outcome))
      (is (= 1 (count (await (evidence-store/completed-translations!
                              evidence
                              {:org-id "open-hax" :project "promethean"}))))
          "event failure cannot roll back or duplicate the receipt")
      (is (some? (:translation/receipt
                  (await (sink/submit-pair! runtime-deps policies (pair turn 1)))))
          "the same terminal split repairs the projection without retranslating")
      (is (= 2 @attempts)))))

(deftest ^:async split-authority-is-checked-before-any-candidate-is-written
  (let [{:keys [turn record splits] :as state} (await (admitted!))
        policies (agent-law/session-policies record turn)
        root "/tmp/knoxx-translation-agent-sink-test/refused"]
    (doseq [[bad expected]
            [[(pair turn 0 :split_id "translation-split-forged")
              :pair-split-id-mismatch]
             [(pair turn 0 :attempt_id "translation-attempt-forged")
              :pair-attempt-id-mismatch]
             [(pair turn 0 :source_text "similar, but not exact")
              :pair-source-text-mismatch]]]
      (is (= expected
             (get-in (await (sink/submit-pair! (deps root state) policies bad))
                     [:translation/refusal :refusal/type]))))
    (is (empty? (await (split-store/candidate-splits-for-turn!
                        splits (:translation-turn/id turn)))))))

(deftest ^:async a-completion-whose-source-moved-mints-no-receipt
  (let [{:keys [turn record evidence] :as state} (await (admitted!))
        root "/tmp/knoxx-translation-agent-sink-test/drifted"
        policies (agent-law/session-policies record turn)
        moved (assoc (deps root state) :observe-source-revision
                     (fn [_] (js/Promise.resolve "sha256-moved")))]
    (await (sink/submit-pair! moved policies (pair turn 0)))
    (let [result (await (sink/submit-pair! moved policies (pair turn 1)))]
      (is (= :source-moved-since-dispatch
             (get-in result [:translation/refusal :refusal/type])))
      (is (empty? (await (evidence-store/completed-translations!
                          evidence {:org-id "open-hax" :project "promethean"}))))
      (is (= dispatch-law/unreachable-outcome
             (:dispatch/outcome
              (await (evidence-store/dispatch-for-key!
                      evidence (:dispatch/key record)))))))))

(deftest ^:async an-unusable-pair-is-refused-before-the-turn-is-read
  (let [{:keys [turn record splits] :as state} (await (admitted!))
        policies (agent-law/session-policies record turn)
        root "/tmp/knoxx-translation-agent-sink-test/unusable"
        result (await (sink/submit-pair!
                       (deps root state) policies
                       (pair turn 0 :translated_text "")))]
    (is (= :pair-translation-missing
           (get-in result [:translation/refusal :refusal/type])))
    (is (empty? (await (split-store/candidate-splits-for-turn!
                        splits (:translation-turn/id turn)))))))

(deftest a-refusal-becomes-an-error-an-agent-can-act-on
  (let [error (sink/refusal-error {:refusal/type :pair-split-id-mismatch})]
    (is (re-find #"split_id" (ex-message error)))
    (is (= :pair-split-id-mismatch (:refusal/type (ex-data error)))))
  (let [error (sink/refusal-error {:refusal/type :translation-turn-missing})]
    (is (re-find #"not bound to a live publication" (ex-message error)))))
