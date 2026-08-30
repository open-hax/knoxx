(ns knoxx.backend.domain.translation-review-inventory-test
  (:require [cljs.test :as t]
            [knoxx.backend.domain.publication-resolver :as publication-resolver]
            [knoxx.backend.domain.translation-review-inventory :as inventory]
            [knoxx.backend.law.translation-dispatch :as dispatch-law]))

(def ^:private garden-id :knoxx.docs/promethean)
(def ^:private scope {:org-id "org-1" :project "knoxx-session"})
(def ^:private at "2026-08-30T10:00:00.000Z")

(defn- document-id
  [n]
  (keyword "knoxx.docs" (str "document-" n)))

(defn- publication-id
  [n]
  (keyword "knoxx.publications" (str "document-" n "-es")))

(defn- document
  [n]
  {:document/id (document-id n)
   :document/title (str "Document " n)
   :document/source-locale :en
   :document/source {:path (str "docs/document-" n ".md")}})

(defn- intent
  [n]
  {:publication/id (publication-id n)
   :publication/document (document-id n)
   :publication/garden garden-id
   :publication/locale :es
   :publication/revision :source/current
   :publication/state :published
   :publication/path (str "/document-" n)
   :translation/review :required})

(defn- publication-index
  [n]
  (publication-resolver/publication-index
   (into [{:garden/id garden-id
           :garden/title "Promethean"
           :garden/status :active
           :garden/locales [:en :es]}]
         (mapcat (juxt document intent))
         (range n))))

(defn- source-revisions
  [n]
  (into {} (map (fn [index]
                  [(document-id index) (str "source-revision-" index)]))
        (range n)))

(defn- scoped-desired-work
  [index revisions]
  (mapv #(assoc %
                :translation/org-id (:org-id scope)
                :translation/project (:project scope))
        (inventory/desired-work index revisions)))

(defn- receipt
  [work & {:keys [source-locale output-revision translated-at]
           :or {source-locale :en
                output-revision "candidate-revision-1"
                translated-at at}}]
  {:receipt/type :translation/completed
   :translation/document (:translation/document work)
   :translation/garden (:translation/garden work)
   :translation/source-locale source-locale
   :translation/locale (:translation/locale work)
   :translation/source-revision (:translation/source-revision work)
   :translation/revision output-revision
   :translation/content-digest (str output-revision "-content")
   :translation/dispatch-key (str "receipt:" (:publication/id work))
   :translation/org-id (:org-id scope)
   :translation/project (:project scope)
   :translation/at translated-at})

(defn- approval
  [work translation-revision]
  {:review/state :approved
   :review/document (:translation/document work)
   :review/garden (:translation/garden work)
   :review/source-locale (:translation/source-locale work)
   :review/locale (:translation/locale work)
   :review/revision (:translation/source-revision work)
   :review/translation-revision translation-revision
   :review/content-digest (str translation-revision "-content")
   :review/org-id (:org-id scope)
   :review/project (:project scope)
   :review/principal {:principal/user-email "reviewer@open-hax.local"}
   :review/at "2026-08-30T10:30:00.000Z"})

(defn- dispatch-record
  [work outcome]
  (dispatch-law/dispatch-record
   {:document (:translation/document work)
    :locale (:translation/locale work)
    :revision (:translation/source-revision work)
    :replace-stale? false}
   {:dispatch/garden "knoxx.docs/promethean"
    :dispatch/document-wire-id (str (namespace (:translation/document work))
                                    "/" (name (:translation/document work)))
    :dispatch/source-locale (:translation/source-locale work)
    :dispatch/org-id (:org-id scope)
    :dispatch/project (:project scope)
    :dispatch/membership-id "membership-1"}
   outcome
   at
   :detail (when (= outcome :dispatch/failed) "provider unavailable")))

(t/deftest desired-work-is-resource-first-and-preserves-unresolved-items
  (let [index (publication-index 18)
        revisions (source-revisions 18)
        resolved (inventory/desired-work index revisions)
        unresolved (inventory/desired-work index (dissoc revisions (document-id 7)))]
    (t/testing "one work item is derived for every desired resource relation"
      (t/is (= 18 (count resolved)))
      (t/is (= 18 (count (set (map :publication/id resolved)))))
      (t/is (= (set (vals revisions))
             (set (map :translation/source-revision resolved)))))

    (t/testing "an unreadable current source remains visible instead of disappearing"
      (t/is (= 18 (count unresolved)))
      (let [item (first (filter #(= (document-id 7)
                                    (:translation/document %))
                                unresolved))]
        (t/is (nil? (:translation/source-revision item)))
        (t/is (nil? (inventory/dispatch-key-input scope item)))
        (t/is (nil? (inventory/dispatch-lookup-key scope item)))))))

(t/deftest a-reserved-explicit-revision-remains-visible-but-not-dispatchable
  (let [index (publication-resolver/publication-index
               [{:garden/id garden-id
                 :garden/title "Promethean"
                :garden/status :active
                 :garden/locales [:en :es]}
                (document 0)
                (assoc (intent 0) :publication/revision "source/v1")])
        item (first (inventory/desired-work index {}))
        row (first (inventory/project-inventory [item] [] [] []))]
    (t/testing "a reserved source/* value is a selector, never durable evidence"
      (t/is (= "source/v1" (:translation/revision-selector item)))
      (t/is (nil? (:translation/source-revision item)))
      (t/is (nil? (inventory/dispatch-key-input scope item)))
      (t/is (nil? (inventory/dispatch-lookup-key scope item))))

    (t/testing "the malformed pin cannot erase its resource-owned inventory row"
      (t/is (= (publication-id 0) (:publication row)))
      (t/is (= :revision_unresolved (:work_state row)))
      (t/is (false? (:reviewable row)))
      (t/is (= [] (:allowed_actions row)))
      (t/is (nil? (:revision row))))))

(t/deftest eighteen-intents-and-one-receipt-project-to-eighteen-rows
  (let [work (scoped-desired-work (publication-index 18)
                                  (source-revisions 18))
        completed (receipt (first work))
        orphan (assoc (receipt (second work))
                      :translation/document :knoxx.docs/not-a-resource
                      :translation/dispatch-key "orphan-key")
        rows (inventory/project-inventory work [completed orphan] [] [])]
    (t/testing "observed evidence never decides inventory cardinality"
      (t/is (= 18 (count rows)))
      (t/is (= 18 (count (set (map (juxt :publication :revision) rows)))))
      (t/is (= 1 (count (filter #(= :ready (:work_state %)) rows))))
      (t/is (= 17 (count (filter #(= :missing (:work_state %)) rows)))))

    (t/testing "only the completed item exposes immutable review coordinates"
      (let [ready (first (filter :candidate_present rows))]
        (t/is (= (:publication/id (first work)) (:publication ready)))
        (t/is (= "candidate-revision-1" (:translation_revision ready)))
        (t/is (= at (:translated_at ready)))
        (t/is (false? (:reviewable ready))
            "the pure projector cannot claim bytes were hydrated")
        (t/is (false? (:approved ready)))
        (t/is (= [] (:allowed_actions ready)))))

    (t/testing "missing work is actionable but cannot be approved"
      (doseq [row (filter #(= :missing (:work_state %)) rows)]
        (t/is (false? (:reviewable row)))
        (t/is (false? (:approved row)))
        (t/is (= [:dispatch] (:allowed_actions row)))
        (t/is (not (contains? row :translation_revision)))))))

(t/deftest receipt-join-includes-source-locale-and-is-order-independent
  (let [work (first (scoped-desired-work (publication-index 1)
                                         (source-revisions 1)))
        matching (receipt work
                          :output-revision "candidate-matching"
                          :translated-at "2026-08-30T10:00:00.000Z")
        wrong-source (receipt work
                              :source-locale :de
                              :output-revision "candidate-wrong-source"
                              :translated-at "2026-08-30T11:00:00.000Z")
        project #(first (inventory/project-inventory [work] % [] []))]
    (t/testing "a newer receipt translated from another source language cannot attach"
      (t/is (= "candidate-matching"
             (:translation_revision (project [matching wrong-source]))))
      (t/is (= "candidate-matching"
             (:translation_revision (project [wrong-source matching])))))

    (t/testing "a wrong-source receipt by itself leaves the desired work missing"
      (let [row (project [wrong-source])]
        (t/is (= :missing (:work_state row)))
        (t/is (false? (:reviewable row)))))))

(t/deftest only-an-approval-of-the-current-output-makes-work-approved
  (let [work (first (scoped-desired-work (publication-index 1)
                                         (source-revisions 1)))
        older (receipt work
                       :output-revision "candidate-old"
                       :translated-at "2026-08-30T09:00:00.000Z")
        current (receipt work
                         :output-revision "candidate-current"
                         :translated-at "2026-08-30T10:00:00.000Z")
        stale (approval work "candidate-old")
        accepted (approval work "candidate-current")]
    (t/testing "a stale approval is retained as history but does not travel"
      (let [row (first (inventory/project-inventory [work]
                                                    [older current]
                                                    [stale]
                                                    []))]
        (t/is (= :ready (:work_state row)))
        (t/is (false? (:approved row)))))

    (t/testing "the exact current approval exposes its timestamp"
      (let [row (first (inventory/project-inventory [work]
                                                    [current older]
                                                    [stale accepted]
                                                    []))]
        (t/is (= :approved (:work_state row)))
        (t/is (true? (:approved row)))
        (t/is (= "2026-08-30T10:30:00.000Z" (:approved_at row)))))))

(t/deftest approval-join-is-exact-across-project-and-source-locale
  (let [work (first (scoped-desired-work (publication-index 1)
                                         (source-revisions 1)))
        completed (receipt work)
        accepted (approval work "candidate-revision-1")
        project (fn [candidate]
                  (first (inventory/project-inventory [work]
                                                      [completed]
                                                      [candidate]
                                                      [])))]
    (doseq [foreign [(assoc accepted :review/project "another-project")
                     (assoc accepted :review/source-locale :de)]]
      (let [row (project foreign)]
        (t/is (= :ready (:work_state row)))
        (t/is (false? (:approved row)))))

    (t/is (= :approved (:work_state (project accepted))))))

(t/deftest dispatch-evidence-projects-honest-states-and-actions
  (let [work (scoped-desired-work (publication-index 6)
                                  (source-revisions 6))
        [in-flight failed rejected stale completed missing] work
        records [(dispatch-record in-flight :dispatch/accepted)
                 (dispatch-record failed :dispatch/failed)
                 (dispatch-record rejected :dispatch/rejected)
                 (dispatch-record stale :dispatch/unreachable)
                 (dispatch-record completed :dispatch/completed)]
        rows (inventory/project-inventory work [] [] records)
        by-publication (into {} (map (juxt :publication identity)) rows)]
    (t/testing "point-lookup keys are exactly the keys used when claims were written"
      (doseq [record records]
        (let [item (first (filter #(= (inventory/work-key %)
                                      (inventory/work-key record))
                                  work))]
          (t/is (= (:dispatch/key record)
                 (inventory/dispatch-lookup-key scope item))))))

    (t/testing "the current evidence supports only the states it can prove"
      (t/is (= [:in_flight []]
             ((juxt :work_state :allowed_actions)
              (get by-publication (:publication/id in-flight)))))
      (t/is (= [:failed [:retry]]
             ((juxt :work_state :allowed_actions)
              (get by-publication (:publication/id failed)))))
      (t/is (= [:rejected [:retry]]
             ((juxt :work_state :allowed_actions)
              (get by-publication (:publication/id rejected)))))
      (t/is (= [:stale []]
             ((juxt :work_state :allowed_actions)
              (get by-publication (:publication/id stale)))))
      (t/is (= [:evidence_missing [:retry]]
             ((juxt :work_state :allowed_actions)
              (get by-publication (:publication/id completed)))))
      (t/is (= [:missing [:dispatch]]
             ((juxt :work_state :allowed_actions)
              (get by-publication (:publication/id missing))))))))

(t/deftest unbound-history-is-visible-but-actionable-not-reviewable
  (let [work (first (scoped-desired-work (publication-index 1)
                                         (source-revisions 1)))
        historical (dissoc (receipt work) :translation/content-digest)
        row (first (inventory/project-inventory [work] [historical] [] []))]
    (t/is (= :evidence_unbound (:work_state row)))
    (t/is (= [:retry] (:allowed_actions row)))
    (t/is (false? (:candidate_present row)))
    (t/is (false? (:reviewable row)))
    (t/is (= "candidate-revision-1" (:translation_revision row))
          "the unusable historical output remains diagnosable")))

(t/deftest dispatch-record-body-must-match-its-durable-key
  (let [work (first (scoped-desired-work (publication-index 1)
                                         (source-revisions 1)))
        honest (dispatch-record work :dispatch/accepted)
        forged (assoc honest :dispatch/document :knoxx.docs/other)]
    (t/is (thrown-with-msg?
           js/Error
           #"translation dispatch body does not match its key"
           (inventory/project-inventory [work] [] [] [forged])))))
