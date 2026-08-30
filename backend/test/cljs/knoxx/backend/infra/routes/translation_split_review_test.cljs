(ns knoxx.backend.infra.routes.translation-split-review-test
  "Application tests for canonical resource-backed split review."
  (:require [cljs.test :as t]
            [clojure.string :as str]
            [knoxx.backend.domain.publication-resolver :as resolver]
            [knoxx.backend.infra.routes.translation-review :as facade]
            [knoxx.backend.infra.translation-evidence-store :as evidence-store]
            [knoxx.backend.infra.translation-split-projection :as projection]
            [knoxx.backend.infra.translation-split-store :as split-store]
            [knoxx.backend.law.translation-dispatch :as dispatch-law]
            [knoxx.backend.law.translation-split :as split]))

(defn- digest
  [value]
  (str "digest[" value "]"))

(def ^:private source-revision "sha256-source-review-v1")
(def ^:private admitted-at "2026-08-30T10:00:00.000Z")
(def ^:private reviewed-at "2026-08-30T11:00:00.000Z")

(def ^:private scope
  {:org-id "org-1"
   :project "knoxx-session"
   :principal {:principal/user-id "reviewer-1"
               :principal/user-email "reviewer@example.test"}})

(def ^:private document
  {:document/id :knoxx.docs/probe
   :document/title "Probe"
   :document/source-locale :en
   :document/source {:path "docs/probe.md"}})

(def ^:private garden
  {:garden/id :knoxx.gardens/promethean
   :garden/title "Promethean"
   :garden/status :active
   :garden/locales [:en :es]})

(def ^:private intent
  {:publication/id :knoxx.publications/probe-es
   :publication/document (:document/id document)
   :publication/garden (:garden/id garden)
   :publication/locale :es
   :publication/revision :source/current
   :publication/state :published
   :publication/path "/probe"
   :translation/review :required})

(def ^:private publication-index
  (resolver/publication-index [document garden intent]))

(defn- split-context
  ([] (split-context {}))
  ([{:keys [candidate-revision dispatch-key run-id admitted-at
            source-parts candidate-texts]
     :or {candidate-revision "sha256-candidate-review-v1"
          dispatch-key "dispatch-review-1"
          run-id "run-review-1"
          admitted-at "2026-08-30T10:00:00.000Z"
          source-parts ["First source.\n\n" "Second source.\n"]
          candidate-texts ["Primera candidata.\n\n"
                           "Segunda candidata.\n"]}}]
   (let [manifest (split/split-manifest
                  digest
                  {:org-id (:org-id scope)
                   :project (:project scope)
                   :garden (:garden/id garden)
                   :document (:document/id document)
                   :source-locale :en
                   :target-locale :es
                   :source-revision source-revision
                   :source-text (apply str source-parts)
                   :source-parts source-parts})
        claim (split/candidate-claim digest manifest candidate-revision)
        candidates (mapv split/candidate-split
                         (repeat digest)
                         (:candidate-claim/members claim)
                         candidate-texts)
        candidate-set (split/complete-candidate-set
                       digest manifest claim (reverse candidates))
        execution (split/execution-snapshot
                   digest
                   {:agent-id "publication_translator"
                    :model "test-model"
                    :thinking :medium
                    :system-prompt "Translate exact admitted splits."
                    :tool-ids ["save_translation"]})
        turn (split/translation-turn-admission
              digest
              {:dispatch-key dispatch-key
               :run-id run-id
               :admitted-at admitted-at
               :manifest manifest
               :candidate-claim claim
               :execution execution
               :memory (split/memory-snapshot {:status :empty :examples []})})]
    {:manifest manifest
     :claim claim
     :candidates candidates
      :candidate-set candidate-set
      :turn turn})))

(defn- persist-context!
  [store {:keys [turn candidates candidate-set]}]
  (split-store/admit-turn! store turn)
  (doseq [candidate candidates]
    (split-store/append-candidate-split!
     store (:translation-turn/id turn) candidate))
  (split-store/complete-candidate-set!
   store (:translation-turn/id turn) candidate-set))

(defn- completed-receipt
  ([context] (completed-receipt context reviewed-at))
  ([{:keys [manifest claim candidate-set turn]} at]
   {:receipt/type :translation/completed
    :translation/document (:document/id document)
    :translation/garden (:garden/id garden)
    :translation/source-locale :en
    :translation/locale :es
    :translation/source-revision source-revision
    :translation/revision (:candidate-set/revision candidate-set)
    :translation/content-digest "sha256-candidate-content"
    :translation/dispatch-key (:translation-turn/dispatch-key turn)
    :translation/org-id (:org-id scope)
    :translation/project (:project scope)
    :translation/split-manifest-id (:split-manifest/id manifest)
    :translation/candidate-claim-id (:candidate-claim/id claim)
    :translation/candidate-set-id (:candidate-set/id candidate-set)
    :translation/candidate-set-digest (:candidate-set/digest candidate-set)
    :translation/split-count (count (:candidate-set/members candidate-set))
    :translation/split-turn-admitted-at (:translation-turn/admitted-at turn)
    :translation/at at}))

(defn- accepted-dispatch
  [attempt-id at & {:keys [recovery-reason]}]
  (dispatch-law/dispatch-record
   {:document (:document/id document)
    :locale :es
    :revision source-revision
    :replace-stale? false}
   {:dispatch/garden "knoxx.gardens/promethean"
    :dispatch/document-wire-id "knoxx.docs/probe"
    :dispatch/source-locale :en
    :dispatch/org-id (:org-id scope)
    :dispatch/project (:project scope)
    :dispatch/membership-id "membership-1"}
   :dispatch/accepted at
   :attempt-id attempt-id
   :recovery-reason recovery-reason))

(defn- ^:async settle-dispatch!
  "Expose one attempt-bound split receipt only after exact dispatch completion."
  [evidence dispatch context at]
  (let [reservation (await (evidence-store/reserve-dispatch!
                            evidence dispatch))
        reserved (:record reservation)
        bound (await (evidence-store/bind-dispatch-batch!
                      evidence reserved
                      (get-in context [:turn :translation-turn/run-id])))
        _ (await (evidence-store/claim-dispatch-completion! evidence bound))
        receipt (assoc (completed-receipt context at)
                       :translation/dispatch-attempt-id
                       (:dispatch/attempt-id bound))]
    (await (evidence-store/record-translation! evidence receipt))
    (await (evidence-store/finish-dispatch-completion!
            evidence bound "candidate set completed"))
    receipt))

(defn- before-review-append-store
  "Delegate split persistence after running one injectable pre-append hook."
  [delegate before-append!]
  (reify split-store/ITranslationSplitStore
    (admit-turn! [_ turn]
      (split-store/admit-turn! delegate turn))
    (turn-for-run! [_ run-id]
      (split-store/turn-for-run! delegate run-id))
    (turn-by-id! [_ turn-id]
      (split-store/turn-by-id! delegate turn-id))
    (append-candidate-split! [_ turn-id candidate]
      (split-store/append-candidate-split! delegate turn-id candidate))
    (candidate-splits-for-turn! [_ turn-id]
      (split-store/candidate-splits-for-turn! delegate turn-id))
    (complete-candidate-set! [_ turn-id candidate-set]
      (split-store/complete-candidate-set! delegate turn-id candidate-set))
    (candidate-set-for-turn! [_ turn-id]
      (split-store/candidate-set-for-turn! delegate turn-id))
    (candidate-set-by-id! [_ candidate-set-id]
      (split-store/candidate-set-by-id! delegate candidate-set-id))
    (turn-for-candidate-set! [_ candidate-set-id]
      (split-store/turn-for-candidate-set! delegate candidate-set-id))
    (append-review-receipt! [_ receipt]
      (.then (js/Promise.resolve (before-append! receipt))
             (fn [_]
               (split-store/append-review-receipt! delegate receipt))))
    (review-history-for-split! [_ candidate-set-id split-id]
      (split-store/review-history-for-split!
       delegate candidate-set-id split-id))
    (applicable-memory! [_ memory-scope]
      (split-store/applicable-memory! delegate memory-scope))))

(defn- unfiltered-completed-evidence-store
  "Delegate writes/point reads while returning an intentionally stale list view."
  [delegate completed]
  (reify evidence-store/ITranslationEvidenceStore
    (reserve-dispatch! [_ record]
      (evidence-store/reserve-dispatch! delegate record))
    (resolve-dispatch! [_ expected outcome detail]
      (evidence-store/resolve-dispatch! delegate expected outcome detail))
    (bind-dispatch-batch! [_ expected batch-id]
      (evidence-store/bind-dispatch-batch! delegate expected batch-id))
    (claim-dispatch-completion! [_ expected]
      (evidence-store/claim-dispatch-completion! delegate expected))
    (finish-dispatch-completion! [_ expected detail]
      (evidence-store/finish-dispatch-completion! delegate expected detail))
    (dispatch-for-key! [_ dispatch-key]
      (evidence-store/dispatch-for-key! delegate dispatch-key))
    (dispatch-for-batch-document! [_ batch-id document-wire-id]
      (evidence-store/dispatch-for-batch-document!
       delegate batch-id document-wire-id))
    (dispatch-for-batch! [_ batch-id]
      (evidence-store/dispatch-for-batch! delegate batch-id))
    (record-translation! [_ receipt]
      (evidence-store/record-translation! delegate receipt))
    (completed-translations! [_ _scope]
      completed)
    (record-approval! [_ approval]
      (evidence-store/record-approval! delegate approval))
    (approvals! [_ query]
      (evidence-store/approvals! delegate query))))

(defn- ^:async stores-with-context!
  []
  (let [dispatch (accepted-dispatch "dispatch-attempt-default" admitted-at)
        context (split-context {:dispatch-key (:dispatch/key dispatch)})
        splits (split-store/memory-store digest)
        evidence (evidence-store/memory-store)]
    (persist-context! splits context)
    (await (settle-dispatch! evidence dispatch context reviewed-at))
    {:context context :split-store splits :evidence-store evidence}))

(defn- review-deps
  [{:keys [split-store evidence-store]}]
  {:evidence-store evidence-store
   :split-store split-store
   :digest-hex digest
   :publication-index publication-index
   :source-revisions {(:document/id document) source-revision}})

(defn- mutation-deps
  ([stores clock]
   (mutation-deps stores clock nil))
  ([{:keys [split-store evidence-store]} clock projector]
   (cond-> {:split-store split-store
            :evidence-store evidence-store
            :content-root "/translated"
            :digest-hex digest
            :clock clock}
     projector (assoc :project-reviewed-output! projector))))

(defn- ^:async histories-for!
  [store candidate-set-id split-ids]
  (loop [remaining split-ids
         histories []]
    (if-let [split-id (first remaining)]
      (recur (next remaining)
             (conj histories
                   (await (split-store/review-history-for-split!
                           store candidate-set-id split-id))))
      histories)))

(t/deftest ^:async inventory-joins-current-canonical-splits-and-history
  (let [{:keys [context split-store] :as stores}
        (await (stores-with-context!))
        {:keys [manifest candidate-set]} context
        first-split-id (:split/id (first (:split-manifest/splits manifest)))
        approved (split/review-receipt
                  digest manifest candidate-set first-split-id
                  (:principal scope) reviewed-at
                  {:review/operation-id "seed-review-1"
                   :review/adequacy "good"
                   :review/fluency "good"
                   :review/terminology "correct"
                   :review/risk "safe"
                   :review/overall "approve"
                   :review/corrected-text "Primera corregida.\n\n"})
        revised (split/review-receipt
                 digest manifest candidate-set first-split-id
                 {:principal/user-id "reviewer-2"
                  :principal/user-email "reviewer-2@example.test"}
                 reviewed-at
                 {:review/operation-id "seed-review-2"
                  :review/adequacy "excellent"
                  :review/fluency "excellent"
                  :review/terminology "minor_errors"
                  :review/risk "sensitive"
                  :review/overall "needs_edit"
                  :review/corrected-text "Primera revision.\n\n"
                  :review/editor-notes "Terminology needs another pass."})]
    (split-store/append-review-receipt! split-store approved)
    (split-store/append-review-receipt! split-store revised)
    (let [row (first (:reviews
                      (await (facade/reviewable-translations!
                              (review-deps stores) scope))))
          review (:split_review row)
          splits (:splits review)]
      (t/is (= (:candidate-set/id candidate-set)
               (:candidate_set_id review)))
      (t/is (= (:split-manifest/id manifest) (:manifest_id review)))
      (t/is (= :partial-review (:status review)))
      (t/is (= [0 1] (mapv :split_index splits)))
      (t/is (= (mapv :split/id (:split-manifest/splits manifest))
               (mapv :split_id splits)))
      (t/is (= (mapv :split/source-text (:split-manifest/splits manifest))
               (mapv :source_text splits)))
      (t/is (= (mapv :candidate/text (:candidate-set/members candidate-set))
               (mapv :candidate_text splits)))
      (t/is (= (mapv :candidate/digest (:candidate-set/members candidate-set))
               (mapv :candidate_digest splits)))
      (t/is (= :in-review (:review_status (first splits))))
      (t/is (= {:adequacy "excellent"
                :fluency "excellent"
                :terminology "minor_errors"
                :risk "sensitive"
                :overall "needs_edit"}
               (select-keys (first splits)
                            [:adequacy :fluency :terminology :risk :overall])))
      (t/is (= (:review/id revised) (:review_id (first splits))))
      (t/is (= reviewed-at (:reviewed_at (first splits))))
      (t/is (= "Primera revision.\n\n"
               (:corrected_text (first splits))))
      (t/is (= 2 (:label_count (first splits))))
      (t/is (= [(:review/id revised) (:review/id approved)]
               (mapv :id (:labels (first splits)))))
      (t/is (= ["reviewer-2@example.test" "reviewer@example.test"]
               (mapv :labeler_email (:labels (first splits)))))
      (t/is (= [reviewed-at reviewed-at]
               (mapv :ts (:labels (first splits)))))
      (t/is (= "Terminology needs another pass."
               (get-in splits [0 :labels 0 :editor_notes])))
      (t/is (= "Primera corregida.\n\n"
               (get-in splits [0 :labels 1 :corrected_text])))
      (t/is (nil? (:review_status (second splits))))
      (t/is (not (contains? (second splits) :corrected_text)))
      (t/is (= [] (:labels (second splits)))))))

(t/deftest ^:async corrected-approval-and-equal-retry-append-one-review
  (let [{:keys [context split-store] :as stores}
        (await (stores-with-context!))
        split-id (get-in context [:manifest :split-manifest/splits 0 :split/id])
        times (atom ["2026-08-30T12:00:00.000Z"
                     "2026-08-30T12:01:00.000Z"])
        clock (fn [] (let [value (first @times)]
                       (swap! times #(if (next %) (vec (next %)) %))
                       value))
        request {:split-review/candidate-set-id
                 (get-in context [:candidate-set :candidate-set/id])
                 :split-review/split-id split-id
                 :split-review/status :approved
                 :split-review/corrected-text "Texto corregido."}
        projection-calls (atom [])
        projected-receipt {:translation/revision "effective-v1"}
        projector
        (fn [deps candidate-set-id]
          (swap! projection-calls conj [deps candidate-set-id])
          (js/Promise.resolve
           {:translation/review-status :not-ready
            :translation/receipt projected-receipt}))
        operation
        (fn [] (facade/record-split-review!
                (mutation-deps stores clock projector) scope request))
        first-result (await (operation))
        retry-result (await (operation))
        history (await (split-store/review-history-for-split!
                        split-store
                        (get-in context [:candidate-set :candidate-set/id])
                        split-id))
        receipt (first history)]
    (t/is (= :recorded (:split-review/status first-result)))
    (t/is (= :existing (:split-review/status retry-result)))
    (t/is (= (:split-review/receipt first-result)
             (:split-review/receipt retry-result)))
    (t/is (= 1 (count history)))
    (t/is (= :approved (:review/status receipt)))
    (t/is (= "approve" (:review/overall receipt)))
    (t/is (= "good" (:review/adequacy receipt)))
    (t/is (= "Texto corregido." (:review/corrected-text receipt)))
    (t/is (= (:principal scope) (:review/principal receipt)))
    (t/is (= "2026-08-30T12:00:00.000Z" (:review/recorded-at receipt)))
    (t/is (= :not-ready (:split-review/current-status retry-result)))
    (t/is (= projected-receipt
             (:split-review/current-translation-receipt retry-result)))
    (t/is (= 2 (count @projection-calls)))))

(t/deftest ^:async rejection-is-durable-and-a-later-reapproval-is-not-a-retry
  (let [{:keys [context split-store] :as stores}
        (await (stores-with-context!))
        set-id (get-in context [:candidate-set :candidate-set/id])
        split-id (get-in context [:manifest :split-manifest/splits 1 :split/id])
        instants (atom ["2026-08-30T12:00:00.000Z"
                        "2026-08-30T12:01:00.000Z"
                        "2026-08-30T12:02:00.000Z"])
        clock (fn [] (let [value (first @instants)]
                       (swap! instants #(vec (rest %)))
                       value))
        projector (fn [_ _]
                    (js/Promise.resolve
                     {:translation/review-status :not-ready
                      :translation/receipt {:translation/revision "current"}}))
        submit (fn [status]
                 (facade/record-split-review!
                  (mutation-deps stores clock projector) scope
                  {:split-review/candidate-set-id set-id
                   :split-review/split-id split-id
                   :split-review/status status}))]
    (await (submit :approved))
    (let [rejected (await (submit :rejected))
            reapproved (await (submit :approved))
            history (await (split-store/review-history-for-split!
                            split-store set-id split-id))]
        (t/is (= :rejected
                 (get-in rejected [:split-review/receipt :review/status])))
        (t/is (= "reject"
                 (get-in rejected [:split-review/receipt :review/overall])))
        (t/is (= "adequate"
                 (get-in rejected [:split-review/receipt :review/adequacy])))
        (t/is (= :recorded (:split-review/status reapproved)))
        (t/is (= 3 (count history)))
        (t/is (= [:approved :rejected :approved]
                 (mapv :review/status history))))))

(t/deftest ^:async document-review-enumerates-the-persisted-set-and-projects-once
  (let [{:keys [context split-store] :as stores}
        (await (stores-with-context!))
        set-id (get-in context [:candidate-set :candidate-set/id])
        split-ids (mapv :split/id
                        (get-in context [:manifest :split-manifest/splits]))
        instants (atom ["2026-08-30T12:00:00.000Z"
                        "2026-08-30T12:00:01.000Z"
                        "2026-08-30T12:01:00.000Z"
                        "2026-08-30T12:01:01.000Z"])
        clock (fn [] (let [value (first @instants)]
                       (swap! instants #(vec (rest %)))
                       value))
        projection-calls (atom [])
        projected-receipt {:translation/revision "effective-document-v1"}
        projector (fn [_ candidate-set-id]
                    (swap! projection-calls conj candidate-set-id)
                    (js/Promise.resolve
                     {:translation/review-status :ready
                      :translation/receipt projected-receipt}))
        request {:split-review/candidate-set-id set-id
                 :split-review/status :approved
                 :review/adequacy "excellent"
                 :review/fluency "good"
                 :review/terminology "correct"
                 :review/risk "safe"
                 :review/editor-notes "Document terminology is consistent."}
        operation #(facade/record-candidate-set-review!
                    (mutation-deps stores clock projector) scope request)
        first-result (await (operation))
        retry-result (await (operation))
        histories (await (histories-for! split-store set-id split-ids))]
    (t/testing "the server reviews every canonical manifest member"
      (t/is (= :recorded (:split-review/status first-result)))
      (t/is (= :existing (:split-review/status retry-result)))
      (t/is (= 2 (count (:split-review/receipts first-result))))
      (t/is (= (:split-review/receipts first-result)
               (:split-review/receipts retry-result)))
      (t/is (= [1 1] (mapv count histories)))
      (t/is (= (set split-ids)
               (set (map (comp :review/split-id first) histories))))
      (t/is (every? #(= :approved (:review/status (first %))) histories))
      (t/is (every? #(= "excellent" (:review/adequacy (first %))) histories))
      (t/is (every? #(= "Document terminology is consistent."
                         (:review/editor-notes (first %)))
                    histories)))

    (t/testing "one bulk call composes once, including an idempotent retry"
      (t/is (= [set-id set-id] @projection-calls))
      (t/is (= :ready (:split-review/current-status retry-result)))
      (t/is (= projected-receipt
               (:split-review/current-translation-receipt retry-result))))))

(t/deftest ^:async stale-candidate-review-is-refused-before-any-append
  (let [evidence (evidence-store/memory-store)
        dispatch-a (accepted-dispatch
                    "dispatch-attempt-a" "2026-08-30T09:00:00.000Z")
        dispatch-b (accepted-dispatch
                    "dispatch-attempt-b" "2026-08-30T10:00:00.000Z"
                    :recovery-reason :candidate-unavailable)
        shared-key (:dispatch/key dispatch-a)
        context-a (split-context
                   {:candidate-revision "sha256-candidate-review-a"
                    :dispatch-key shared-key
                    :run-id "run-review-a"
                    :admitted-at "2026-08-30T09:00:00.000Z"})
        context-b (split-context
                   {:candidate-revision "sha256-candidate-review-b"
                    :dispatch-key shared-key
                    :run-id "run-review-b"
                    :admitted-at "2026-08-30T10:00:00.000Z"})
        splits (split-store/memory-store digest)
        set-id-a (get-in context-a [:candidate-set :candidate-set/id])
        split-id-a (get-in context-a [:manifest :split-manifest/splits 0
                                      :split/id])
        projected? (atom false)
        projector (fn [_ _]
                    (reset! projected? true)
                    (js/Promise.resolve {}))]
    (persist-context! splits context-a)
    (persist-context! splits context-b)
    (await (settle-dispatch! evidence dispatch-a context-a
                             "2026-08-30T09:30:00.000Z"))
    (await (settle-dispatch! evidence dispatch-b context-b
                             "2026-08-30T10:30:00.000Z"))

    (let [error (try
                  (await
                   (facade/record-split-review!
                    (mutation-deps {:split-store splits
                                    :evidence-store evidence}
                                   (constantly reviewed-at) projector)
                    scope
                    {:split-review/candidate-set-id set-id-a
                     :split-review/split-id split-id-a
                     :split-review/status :approved
                     :split-review/corrected-text "No debe persistir."}))
                  nil
                  (catch :default err err))]
      (t/is (= 409 (:status (ex-data error))))
      (t/is (empty? (await (split-store/review-history-for-split!
                            splits set-id-a split-id-a))))
      (t/is (false? @projected?)))))

(t/deftest ^:async current-attempt-wins-a-same-admission-time-stale-list-view
  (let [evidence (evidence-store/memory-store)
        dispatch-a (accepted-dispatch
                    "dispatch-attempt-same-time-a" admitted-at)
        dispatch-b (accepted-dispatch
                    "dispatch-attempt-same-time-b" admitted-at
                    :recovery-reason :candidate-unavailable)
        shared-key (:dispatch/key dispatch-a)
        contexts [(split-context
                   {:candidate-revision "sha256-same-time-a"
                    :dispatch-key shared-key
                    :run-id "run-same-time-a"
                    :admitted-at admitted-at})
                  (split-context
                   {:candidate-revision "sha256-same-time-b"
                    :dispatch-key shared-key
                    :run-id "run-same-time-b"
                    :admitted-at admitted-at})]
        ;; Make the stale set lexically greater, which is exactly the arbitrary
        ;; candidate-set tiebreak an unfiltered current-receipt selection used.
        [context-b context-a]
        (sort-by #(get-in % [:candidate-set :candidate-set/id]) contexts)
        splits (split-store/memory-store digest)
        set-id-a (get-in context-a [:candidate-set :candidate-set/id])
        set-id-b (get-in context-b [:candidate-set :candidate-set/id])
        split-id-b (get-in context-b [:manifest :split-manifest/splits 0
                                      :split/id])
        projector (fn [_ _]
                    (js/Promise.resolve
                     {:translation/review-status :not-ready
                      :translation/receipt nil}))]
    (persist-context! splits context-a)
    (persist-context! splits context-b)
    (let [receipt-a (await (settle-dispatch!
                            evidence dispatch-a context-a
                            "2026-08-30T09:30:00.000Z"))
          receipt-b (await (settle-dispatch!
                            evidence dispatch-b context-b
                            "2026-08-30T10:30:00.000Z"))
          stale-list-store
          (unfiltered-completed-evidence-store evidence [receipt-a receipt-b])
          result (await
                  (facade/record-split-review!
                   (mutation-deps {:split-store splits
                                   :evidence-store stale-list-store}
                                  (constantly reviewed-at) projector)
                   scope
                   {:split-review/candidate-set-id set-id-b
                    :split-review/split-id split-id-b
                    :split-review/status :approved}))]
      (t/is (pos? (compare set-id-a set-id-b)))
      (t/is (= :recorded (:split-review/status result)))
      (t/is (= set-id-b
               (get-in result
                       [:split-review/receipt :review/candidate-set-id])))
      (t/is (empty? (await (split-store/review-history-for-split!
                            splits set-id-a
                            (get-in context-a
                                    [:manifest :split-manifest/splits 0
                                     :split/id]))))))))

(t/deftest ^:async replaced-after-review-guard-cannot-enter-future-memory
  (let [evidence (evidence-store/memory-store)
        dispatch-a (accepted-dispatch
                    "dispatch-attempt-a" "2026-08-30T09:00:00.000Z")
        dispatch-b (accepted-dispatch
                    "dispatch-attempt-b" "2026-08-30T10:00:00.000Z"
                    :recovery-reason :candidate-unavailable)
        shared-key (:dispatch/key dispatch-a)
        context-a (split-context
                   {:candidate-revision "sha256-candidate-review-a"
                    :dispatch-key shared-key
                    :run-id "run-review-a"
                    :admitted-at "2026-08-30T09:00:00.000Z"})
        context-b (split-context
                   {:candidate-revision "sha256-candidate-review-b"
                    :dispatch-key shared-key
                    :run-id "run-review-b"
                    :admitted-at "2026-08-30T10:00:00.000Z"})
        splits (split-store/memory-store digest)
        set-id-a (get-in context-a [:candidate-set :candidate-set/id])
        set-id-b (get-in context-b [:candidate-set :candidate-set/id])
        split-id-a (get-in context-a [:manifest :split-manifest/splits 0
                                      :split/id])
        replaced? (atom false)
        replace! (fn [_receipt]
                   (if (compare-and-set! replaced? false true)
                     (settle-dispatch! evidence dispatch-b context-b
                                       "2026-08-30T10:30:00.000Z")
                     (js/Promise.resolve nil)))
        racing-store (before-review-append-store splits replace!)
        projector (fn [_ _]
                    (js/Promise.resolve
                     {:translation/review-status :not-ready
                      :translation/receipt nil}))
        memory-scope {:org-id (:org-id scope)
                      :project (:project scope)
                      :garden (:garden/id garden)
                      :source-locale :en
                      :target-locale :es
                      :current-candidate-set-ids #{set-id-b}}]
    (persist-context! splits context-a)
    (persist-context! splits context-b)
    (await (settle-dispatch! evidence dispatch-a context-a
                             "2026-08-30T09:30:00.000Z"))

    (let [result (await
                  (facade/record-split-review!
                   (mutation-deps {:split-store racing-store
                                   :evidence-store evidence}
                                  (constantly reviewed-at) projector)
                   scope
                   {:split-review/candidate-set-id set-id-a
                    :split-review/split-id split-id-a
                    :split-review/status :approved
                    :split-review/corrected-text "Correccion de A."}))]
      (t/is (= :recorded (:split-review/status result)))
      (t/is (true? @replaced?))
      (t/is (= 1 (count (await
                         (split-store/review-history-for-split!
                          splits set-id-a split-id-a)))))
      (t/is (empty? (split-store/applicable-memory! splits memory-scope))))))

(defn- bulk-operation-group
  "Return the shared prefix which precedes a bulk receipt's split suffix."
  [receipt]
  (first (str/split (:review/operation-id receipt) #":split:" 2)))

(t/deftest ^:async same-millisecond-conflicting-bulk-actions-have-one-effective-group
  (let [{:keys [context split-store] :as stores}
        (await (stores-with-context!))
        set-id (get-in context [:candidate-set :candidate-set/id])
        split-ids (mapv :split/id
                        (get-in context [:manifest :split-manifest/splits]))
        projector (fn [_ _]
                    (js/Promise.resolve
                     {:translation/review-status :not-ready
                      :translation/receipt nil}))
        deps (mutation-deps stores (constantly "2026-08-30T12:00:00.000Z")
                            projector)
        request {:split-review/candidate-set-id set-id}
        _ (await
           (js/Promise.all
            (clj->js
             [(facade/record-candidate-set-review!
               deps scope (assoc request :split-review/status :approved))
              (facade/record-candidate-set-review!
               deps scope (assoc request :split-review/status :rejected))])))
        histories (await (histories-for! split-store set-id split-ids))
        effective (mapv #(split/effective-review-receipt
                          digest (:manifest context) (:candidate-set context)
                          %1 %2)
                        split-ids histories)]
    (t/testing "both complete actions are retained as immutable evidence"
      (t/is (= [2 2] (mapv count histories))))
    (t/testing "operation-group rank, not per-split append/id order, wins"
      (t/is (= 1 (count (set (map :review/status effective)))))
      (t/is (= 1 (count (set (map bulk-operation-group effective))))))))

(t/deftest ^:async partial-bulk-retry-restamps-all-splits-and-replay-appends-nothing
  (let [{:keys [context split-store evidence-store]}
        (await (stores-with-context!))
        set-id (get-in context [:candidate-set :candidate-set/id])
        split-ids (mapv :split/id
                        (get-in context [:manifest :split-manifest/splits]))
        first-split (first split-ids)
        correction "Correccion retenida durante reintento."
        calls (atom 0)
        fail-once? (atom true)
        hooked-store
        (before-review-append-store
         split-store
         (fn [_receipt]
           (let [call (swap! calls inc)]
             (when (and (= 3 call) (compare-and-set! fail-once? true false))
               (throw (ex-info "simulated bulk append interruption" {}))))))
        instants (atom ["2026-08-30T11:00:00.000Z"
                        "2026-08-30T11:01:00.000Z"
                        "2026-08-30T11:02:00.000Z"
                        "2026-08-30T11:03:00.000Z"])
        clock (fn []
                (let [instant (first @instants)]
                  (swap! instants #(vec (rest %)))
                  instant))
        projector (fn [_ _]
                    (js/Promise.resolve
                     {:translation/review-status :not-ready
                      :translation/receipt nil}))
        deps (mutation-deps {:split-store hooked-store
                             :evidence-store evidence-store}
                            clock projector)
        base-request {:split-review/candidate-set-id set-id}
        _ (await
           (facade/record-split-review!
            deps scope
            (assoc base-request
                   :split-review/split-id first-split
                   :split-review/status :approved
                   :split-review/corrected-text correction)))
        interrupted (try
                      (await
                       (facade/record-candidate-set-review!
                        deps scope
                        (assoc base-request :split-review/status :in-review)))
                      nil
                      (catch :default err err))
        retried (await
                 (facade/record-candidate-set-review!
                  deps scope
                  (assoc base-request :split-review/status :in-review)))
        replayed (await
                  (facade/record-candidate-set-review!
                   deps scope
                   (assoc base-request :split-review/status :in-review)))
        histories (await (histories-for! split-store set-id split-ids))
        effective (mapv #(split/effective-review-receipt
                          digest (:manifest context) (:candidate-set context)
                          %1 %2)
                        split-ids histories)]
    (t/is (= "simulated bulk append interruption" (ex-message interrupted)))
    (t/is (= :recorded (:split-review/status retried)))
    (t/is (= :existing (:split-review/status replayed)))
    (t/is (= [3 1] (mapv count histories))
          "the retry rewrites every split once; the complete replay writes none")
    (t/is (every? #(= :in-review (:review/status %)) effective))
    (t/is (= 1 (count (set (map bulk-operation-group effective)))))
    (t/is (= correction (:review/corrected-text (first effective))))
    (t/is (= (:split-review/receipts retried)
             (:split-review/receipts replayed)))))

(t/deftest ^:async one-split-document-review-keeps-the-bulk-envelope
  (let [dispatch (accepted-dispatch "dispatch-attempt-one-split" admitted-at)
        context (split-context
                 {:source-parts ["Only source.\n"]
                  :candidate-texts ["Unica candidata.\n"]
                  :dispatch-key (:dispatch/key dispatch)})
        splits (split-store/memory-store digest)
        evidence (evidence-store/memory-store)
        set-id (get-in context [:candidate-set :candidate-set/id])
        split-id (get-in context [:manifest :split-manifest/splits 0 :split/id])
        projector (fn [_ _]
                    (js/Promise.resolve
                     {:translation/review-status :not-ready
                      :translation/receipt nil}))
        deps (mutation-deps {:split-store splits :evidence-store evidence}
                            (constantly reviewed-at) projector)]
    (persist-context! splits context)
    (await (settle-dispatch! evidence dispatch context reviewed-at))
    (let [single (await
                  (facade/record-split-review!
                   deps scope
                   {:split-review/candidate-set-id set-id
                    :split-review/split-id split-id
                    :split-review/status :approved}))
          bulk (await
                (facade/record-candidate-set-review!
                 deps scope
                 {:split-review/candidate-set-id set-id
                  :split-review/status :rejected}))]
      (t/testing "the per-split mutation remains singular"
        (t/is (map? (:split-review/receipt single)))
        (t/is (not (contains? single :split-review/receipts))))
      (t/testing "the document mutation remains plural at cardinality one"
        (t/is (= 1 (count (:split-review/receipts bulk))))
        (t/is (not (contains? bulk :split-review/receipt)))))))

(t/deftest ^:async bulk-verdict-cycles-preserve-an-existing-split-correction
  (let [{:keys [context split-store] :as stores}
        (await (stores-with-context!))
        set-id (get-in context [:candidate-set :candidate-set/id])
        split-id (get-in context [:manifest :split-manifest/splits 0 :split/id])
        corrected "Primera correccion durable.\n\n"
        projector (fn [_ _]
                    (js/Promise.resolve
                     {:translation/review-status :not-ready
                      :translation/receipt nil}))
        instants (atom ["2026-08-30T11:00:00.000Z"
                        "2026-08-30T11:01:00.000Z"
                        "2026-08-30T11:02:00.000Z"
                        "2026-08-30T11:03:00.000Z"
                        "2026-08-30T11:04:00.000Z"
                        "2026-08-30T11:05:00.000Z"
                        "2026-08-30T11:06:00.000Z"])
        clock (fn []
                (let [instant (first @instants)]
                  (swap! instants #(vec (rest %)))
                  instant))
        deps (mutation-deps stores clock projector)
        base-request {:split-review/candidate-set-id set-id}
        memory-scope {:org-id (:org-id scope)
                      :project (:project scope)
                      :garden (:garden/id garden)
                      :source-locale :en
                      :target-locale :es
                      :current-candidate-set-ids #{set-id}}]
    (await
     (facade/record-split-review!
      deps scope
      (assoc base-request
             :split-review/split-id split-id
             :split-review/status :approved
             :split-review/corrected-text corrected)))
    (doseq [status [:in-review :rejected :approved]]
      (await
       (facade/record-candidate-set-review!
        deps scope (assoc base-request :split-review/status status))))

    (let [snapshot (await
                    (projection/current-reviewed-output!
                     {:split-store split-store :digest-hex digest} set-id))
          reviewed-output (:reviewed-output snapshot)
          examples (split-store/applicable-memory! split-store memory-scope)
          corrected-example (first
                             (filter #(= split-id
                                         (:translation-memory/split-id %))
                                     examples))]
      (t/is (= :ready (:translation-reviewed-output/status reviewed-output)))
      (t/is (= corrected
               (subs (:translation-reviewed-output/text reviewed-output)
                     0 (count corrected))))
      (t/is (= corrected
               (:translation-memory/target-text corrected-example))))))

(t/deftest ^:async document-review-refuses-another-tenant-before-any-append
  (let [{:keys [context split-store] :as stores}
        (await (stores-with-context!))
        set-id (get-in context [:candidate-set :candidate-set/id])
        split-ids (mapv :split/id
                        (get-in context [:manifest :split-manifest/splits]))
        projected? (atom false)
        projector (fn [_ _]
                    (reset! projected? true)
                    (js/Promise.resolve {}))
        error (try
                (await
                 (facade/record-candidate-set-review!
                  (mutation-deps stores (constantly reviewed-at) projector)
                  (assoc scope :org-id "org-other")
                  {:split-review/candidate-set-id set-id
                   :split-review/status :rejected}))
                nil
                (catch :default err err))]
    (t/is (= 403 (:status (ex-data error))))
    (t/is (every? empty?
                  (await (histories-for! split-store set-id split-ids))))
    (t/is (false? @projected?))))

(t/deftest ^:async another-tenant-or-project-cannot-append-review-evidence
  (doseq [forged-scope [(assoc scope :org-id "org-other")
                        (assoc scope :project "project-other")]]
    (let [{:keys [context split-store] :as stores}
          (await (stores-with-context!))
          set-id (get-in context [:candidate-set :candidate-set/id])
          split-id (get-in context [:manifest :split-manifest/splits 0 :split/id])
          projected? (atom false)
          projector (fn [_ _]
                      (reset! projected? true)
                      (js/Promise.resolve {}))
          error
          (try
            (await
             (facade/record-split-review!
              (mutation-deps stores (constantly reviewed-at) projector)
              forged-scope
              {:split-review/candidate-set-id set-id
               :split-review/split-id split-id
               :split-review/status :rejected}))
            nil
            (catch :default err err))]
      (t/is (= 403 (:status (ex-data error))))
      (t/is (= "translation_split_review_scope_mismatch"
               (:code (ex-data error))))
      (t/is (empty? (await (split-store/review-history-for-split!
                            split-store set-id split-id))))
      (t/is (false? @projected?)))))
