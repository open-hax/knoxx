(ns knoxx.backend.infra.translation-split-projection-test
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [knoxx.backend.domain.translation-evidence :as evidence-domain]
            [knoxx.backend.infra.translation-agent-content :as content]
            [knoxx.backend.infra.translation-evidence-store :as evidence-store]
            [knoxx.backend.infra.translation-split-projection :as projection]
            [knoxx.backend.infra.translation-split-store :as split-store]
            [knoxx.backend.law.translation-dispatch :as dispatch-law]
            [knoxx.backend.law.translation-evidence :as evidence-law]
            [knoxx.backend.law.translation-split :as split-law]
            [knoxx.backend.law.translation-split-fixture :as fixture]))

(def ^:private run-id "review-projection-run")
(def ^:private admitted-at "2026-08-30T12:00:00.000Z")
(def ^:private projected-at "2026-08-30T13:00:00.000Z")

(defn- record
  []
  (assoc
   (dispatch-law/dispatch-record
    {:document (:document fixture/coordinates)
     :locale (:target-locale fixture/coordinates)
     :revision (:source-revision fixture/coordinates)
     :replace-stale? false}
    {:dispatch/garden "open-hax.gardens/promethean"
     :dispatch/document-wire-id "open-hax.documents/start-here"
     :dispatch/source-locale (:source-locale fixture/coordinates)
     :dispatch/org-id (:org-id fixture/coordinates)
     :dispatch/project (:project fixture/coordinates)
     :dispatch/membership-id "membership-1"
     :dispatch/source-digest "sha256-source-v1"}
    :dispatch/completed admitted-at
    :attempt-id "dispatch-attempt-projection")
   :dispatch/batch-id run-id))

(defn- turn
  ([dispatch manifest claim]
   (turn dispatch manifest claim run-id admitted-at))
  ([dispatch manifest claim turn-run-id turn-admitted-at]
   (split-law/translation-turn-admission
    fixture/digest
    {:dispatch-key (:dispatch/key dispatch)
     :run-id turn-run-id
     :admitted-at turn-admitted-at
     :manifest manifest
     :candidate-claim claim
     :execution (split-law/execution-snapshot
                 fixture/digest
                 {:agent-id "publication_translator"
                  :model "model"
                  :thinking :medium
                  :system-prompt "translate"
                  :tool-ids ["save_translation"]})
     :memory (split-law/memory-snapshot {:status :empty :examples []})})))

(defn- ^:async state!
  []
  (let [evidence (evidence-store/memory-store)
        splits (split-store/memory-store fixture/digest)
        manifest (fixture/manifest)
        dispatch (record)
        claim (split-law/candidate-claim fixture/digest manifest
                                         (dispatch-law/output-revision dispatch))
        candidates (fixture/candidates claim)
        candidate-set (split-law/complete-candidate-set
                       fixture/digest manifest claim candidates)
        admitted-turn (turn dispatch manifest claim)]
    ;; Reserve/bind/resolve produces the exact completed record the facade reads.
    (let [in-flight (assoc dispatch :dispatch/outcome :dispatch/accepted)]
      (await (evidence-store/reserve-dispatch! evidence in-flight))
      (let [bound (await (evidence-store/bind-dispatch-batch! evidence in-flight
                                                              run-id))]
        (await (evidence-store/claim-dispatch-completion! evidence bound))
        (await (evidence-store/finish-dispatch-completion! evidence bound nil))))
    (await (split-store/admit-turn! splits admitted-turn))
    (doseq [candidate candidates]
      (await (split-store/append-candidate-split!
              splits (:translation-turn/id admitted-turn) candidate)))
    (await (split-store/complete-candidate-set!
            splits (:translation-turn/id admitted-turn) candidate-set))
    {:evidence evidence :splits splits :manifest manifest
     :candidate-set candidate-set :turn admitted-turn}))

(defn- deps
  [root {:keys [evidence splits]}]
  {:content-root root
   :evidence-store evidence
   :split-store splits
   :digest-hex fixture/digest
   :clock (constantly projected-at)})

(defn- approve!
  [splits manifest candidate-set source-split index]
  (split-store/append-review-receipt!
   splits
   (fixture/review-receipt
    manifest candidate-set (:split/id source-split) fixture/principal
    (str "2026-08-30T12:0" index ":00.000Z")
    (fixture/review-request
     {:review/operation-id (str "projection-approve-" index)
      :review/corrected-text nil}))))

(defn- opposing-review-id-digest
  "Make receipt-id order oppose bulk operation order on the first two splits."
  [value]
  (let [text (str value)]
    (cond
      (str/includes? text "bulk-group-a/split-000") "zz-receipt-a-000"
      (str/includes? text "bulk-group-b/split-000") "aa-receipt-b-000"
      (str/includes? text "bulk-group-a/split-001") "aa-receipt-a-001"
      (str/includes? text "bulk-group-b/split-001") "zz-receipt-b-001"
      :else (fixture/digest value))))

(defn- ^:async operation-ordered-state!
  "Persist one dispatch/turn whose review ids deliberately misorder bulk groups."
  []
  (let [evidence (evidence-store/memory-store)
        splits (split-store/memory-store opposing-review-id-digest)
        dispatch (record)
        manifest (split-law/split-manifest
                  opposing-review-id-digest
                  (assoc fixture/coordinates
                         :source-text fixture/source
                         :source-parts fixture/source-parts))
        claim (split-law/candidate-claim
               opposing-review-id-digest manifest
               (dispatch-law/output-revision dispatch))
        candidates (mapv split-law/candidate-split
                         (repeat opposing-review-id-digest)
                         (:candidate-claim/members claim)
                         ["# Empieza aqui\n\n"
                          "Primer parrafo.\n\n"
                          "  Segundo parrafo.\n"])
        candidate-set (split-law/complete-candidate-set
                       opposing-review-id-digest manifest claim candidates)
        admitted-turn
        (split-law/translation-turn-admission
         opposing-review-id-digest
         {:dispatch-key (:dispatch/key dispatch)
          :run-id run-id
          :admitted-at admitted-at
          :manifest manifest
          :candidate-claim claim
          :execution (split-law/execution-snapshot
                      opposing-review-id-digest
                      {:agent-id "publication_translator"
                       :model "model"
                       :thinking :medium
                       :system-prompt "translate"
                       :tool-ids ["save_translation"]})
          :memory (split-law/memory-snapshot {:status :empty :examples []})})]
    (let [in-flight (assoc dispatch :dispatch/outcome :dispatch/accepted)]
      (await (evidence-store/reserve-dispatch! evidence in-flight))
      (let [bound (await (evidence-store/bind-dispatch-batch!
                          evidence in-flight run-id))]
        (await (evidence-store/claim-dispatch-completion! evidence bound))
        (await (evidence-store/finish-dispatch-completion! evidence bound nil))))
    (await (split-store/admit-turn! splits admitted-turn))
    (doseq [candidate candidates]
      (await (split-store/append-candidate-split!
              splits (:translation-turn/id admitted-turn) candidate)))
    (await (split-store/complete-candidate-set!
            splits (:translation-turn/id admitted-turn) candidate-set))
    {:evidence evidence :splits splits :manifest manifest
     :candidate-set candidate-set :turn admitted-turn}))

(defn- append-operation-group!
  [state group overall]
  (let [{:keys [splits manifest candidate-set]} state]
    (doseq [[index source-split]
            (map-indexed vector (:split-manifest/splits manifest))]
      (let [operation-id (str "bulk-group-" group "/split-"
                              (.padStart (str index) 3 "0"))
            request (cond-> (fixture/review-request
                             {:review/operation-id operation-id
                              :review/overall overall})
                      (not= "approve" overall)
                      (dissoc :review/corrected-text))]
        (split-store/append-review-receipt!
         splits
         (split-law/review-receipt
          opposing-review-id-digest manifest candidate-set
          (:split/id source-split) fixture/principal projected-at request))))))

(deftest ^:async projection-revokes-and-then-materializes-reviewed-corrections
  (let [{:keys [manifest candidate-set splits] :as state} (await (state!))
        root "/tmp/knoxx-translation-split-projection-test"
        candidate-set-id (:candidate-set/id candidate-set)
        initial (await (projection/project-reviewed-output!
                        (deps root state) candidate-set-id))]
    (testing "missing review produces a non-ready current revision"
      (is (= :not-ready (:translation/review-status initial)))
      (is (seq (get-in initial [:translation/review-refusal :refusal/splits]))))

    (doseq [[index source-split] (map-indexed vector
                                              (:split-manifest/splits manifest))]
      (await (approve! splits manifest candidate-set source-split index)))

    (let [ready (await (projection/project-reviewed-output!
                        (deps root state) candidate-set-id))
          receipt (:translation/receipt ready)
          approval (evidence-law/approve
                    nil receipt fixture/principal projected-at)]
      (testing "all approvals produce the effective reviewed bytes"
        (is (= :ready (:translation/review-status ready)))
        (is (re-find #"^translation-effective-revision-"
                     (:translation/revision receipt)))
        (is (= (:translation/split-count receipt)
               (count (:translation/split-review-order receipt))))
        (is (= (get-in ready [:translation/receipt :translation/content-digest])
               (:translation/content-digest receipt)))
        (is (string? (await (content/content-for-receipt! root receipt))))
        (is (projection/current-ready-receipt?
             receipt
             (await (projection/current-reviewed-output!
                     {:split-store splits :digest-hex fixture/digest}
                     candidate-set-id)))))

      (testing "an older ready generation cannot authorize a newer unready one"
        ;; A newer set can legitimately produce the same bytes. Even if a
        ;; malformed or migrated ledger row also reuses the older output
        ;; revision, the current generation's durable review history remains
        ;; the only readiness authority.
        (let [later-at "2026-08-30T14:00:00.000Z"
              later-claim (split-law/candidate-claim
                           fixture/digest manifest "candidate-revision-2")
              later-candidates (fixture/candidates later-claim)
              later-set (split-law/complete-candidate-set
                         fixture/digest manifest later-claim later-candidates)
              later-turn (turn (record) manifest later-claim
                               "review-projection-run-2" later-at)]
          (await (split-store/admit-turn! splits later-turn))
          (doseq [candidate later-candidates]
            (await (split-store/append-candidate-split!
                    splits (:translation-turn/id later-turn) candidate)))
          (await (split-store/complete-candidate-set!
                  splits (:translation-turn/id later-turn) later-set))
          (let [first-split (first (:split-manifest/splits manifest))
                rejection (fixture/review-receipt
                           manifest later-set (:split/id first-split)
                           fixture/principal later-at
                           (fixture/review-request
                            {:review/operation-id "projection-reject-new-set"
                             :review/overall "reject"
                             :review/corrected-text nil}))]
            (await (split-store/append-review-receipt! splits rejection)))
          (let [newer-receipt
                (-> receipt
                    (assoc :translation/candidate-claim-id
                           (:candidate-claim/id later-claim)
                           :translation/candidate-set-id
                           (:candidate-set/id later-set)
                           :translation/candidate-set-digest
                           (:candidate-set/digest later-set)
                           :translation/split-turn-admitted-at later-at
                           :translation/at later-at)
                    (dissoc :translation/split-review-order))
                admissible (await (projection/current-review-approvals!
                                   {:split-store splits
                                    :digest-hex fixture/digest}
                                   [receipt newer-receipt]
                                   [approval]))]
            (is (evidence-law/supersedes? newer-receipt receipt))
            (is (evidence-law/approval-current? approval newer-receipt))
            (is (empty? admissible)))))

      (testing "durable rejection revokes approval before projection catches up"
        ;; Append the review but deliberately do not run the materializing
        ;; projector. This is the process-crash window that previously left the
        ;; old ready receipt and its whole-output approval publishable.
        (let [first-split (first (:split-manifest/splits manifest))
              rejection (fixture/review-receipt
                         manifest candidate-set (:split/id first-split)
                         fixture/principal "2026-08-30T14:00:00.000Z"
                         (fixture/review-request
                          {:review/operation-id "projection-reject-after-ready"
                           :review/overall "reject"
                           :review/corrected-text nil}))]
          (await (split-store/append-review-receipt! splits rejection))
          (let [snapshot (await (projection/current-reviewed-output!
                                 {:split-store splits
                                  :digest-hex fixture/digest}
                                 candidate-set-id))
                admissible (await (projection/current-review-approvals!
                                   {:split-store splits
                                    :digest-hex fixture/digest}
                                   [receipt]
                                   [approval]))]
            (is (= :not-ready
                   (get-in snapshot
                           [:reviewed-output
                            :translation-reviewed-output/status])))
            (is (not (projection/current-ready-receipt? receipt snapshot)))
            (is (empty? admissible))))))))

(deftest ^:async completed-receipt-order-matches-same-millisecond-operation-order
  (let [{:keys [evidence splits candidate-set] :as state}
        (await (operation-ordered-state!))
        candidate-set-id (:candidate-set/id candidate-set)
        root "/tmp/knoxx-translation-operation-order-test"
        projection-deps {:content-root root
                         :evidence-store evidence
                         :split-store splits
                         :digest-hex opposing-review-id-digest
                         :clock (constantly projected-at)}]
    (append-operation-group! state "a" "approve")
    (let [group-a (await (projection/project-reviewed-output!
                          projection-deps candidate-set-id))]
      (is (= :ready (:translation/review-status group-a)))

      (append-operation-group! state "b" "reject")
      (let [snapshot (await (projection/current-reviewed-output!
                             {:split-store splits
                              :digest-hex opposing-review-id-digest}
                             candidate-set-id))
            group-b (await (projection/project-reviewed-output!
                            projection-deps candidate-set-id))
            receipts (await (evidence-store/completed-translations!
                             evidence {:org-id (:org-id fixture/coordinates)
                                       :project (:project fixture/coordinates)}))
            current (first (evidence-domain/current-receipts receipts))
            [first-split] (get-in state [:manifest :split-manifest/splits])
            history (await (split-store/review-history-for-split!
                            splits candidate-set-id (:split/id first-split)))
            by-operation (into {} (map (juxt :review/operation-id identity)) history)
            group-a-review (get by-operation "bulk-group-a/split-000")
            group-b-review (get by-operation "bulk-group-b/split-000")]
        (testing "the fixture really makes hash receipt order oppose operation order"
          (is (pos? (compare (:review/id group-a-review)
                             (:review/id group-b-review))))
          (is (pos? (compare (:review/operation-id group-b-review)
                             (:review/operation-id group-a-review)))))

        (testing "composition and completed evidence select the same whole group"
          (is (= :not-ready (:translation/review-status group-b)))
          (is (= (:translation/receipt group-b) current))
          (is (not= (:translation/receipt group-a) current))
          (is (= (get-in snapshot
                         [:reviewed-output
                          :translation-reviewed-output/review-order])
                 (:translation/split-review-order current))))

        (testing "new projections persist the full effective-review order"
          (is (every? #(= 3 (count %))
                      (:translation/split-review-order current)))
          (is (= "bulk-group-b/split-000"
                 (second (first (:translation/split-review-order current))))))))))
