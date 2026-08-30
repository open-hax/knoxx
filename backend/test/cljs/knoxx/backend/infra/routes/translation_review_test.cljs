(ns knoxx.backend.infra.routes.translation-review-test
  "Recording one approval.

  What is worth testing here is not that a row gets written. It is that an
  approval cannot be recorded against a translation that does not exist, does not
  match, belongs to another tenant, or has since been replaced — and that
  recording one publishes nothing."
  (:require [cljs.test :as t]
            [knoxx.backend.domain.publication-resolver :as resolver]
            [knoxx.backend.domain.translation-review-inventory :as inventory]
            [knoxx.backend.infra.routes.translation-review :as facade]
            [knoxx.backend.infra.translation-evidence-store :as store]
            [knoxx.backend.law.translation-dispatch :as dispatch-law]
            [knoxx.backend.shape.resource-identity :as resource-identity]))

(def ^:private at "2026-08-22T10:00:00.000Z")

(def ^:private query-scope
  "The scope every read in this suite narrows by — the same one the facade uses."
  {:org-id "org-1" :project "knoxx-session"})

(def ^:private scope
  {:org-id "org-1"
   :project "knoxx-session"
   :principal {:principal/user-email "reviewer@open-hax.local"}})

(def ^:private default-document
  {:document/id :knoxx.docs/probe
   :document/title "Probe"
   :document/source-locale :en
   :document/source {:path "docs/probe.md"}})

(def ^:private default-garden
  {:garden/id :knoxx.docs/promethean
   :garden/title "Promethean"
   :garden/status :active
   :garden/locales [:en :es]})

(def ^:private default-intent
  {:publication/id :knoxx.publications/probe-es
   :publication/document :knoxx.docs/probe
   :publication/garden :knoxx.docs/promethean
   :publication/locale :es
   :publication/revision :source/current
   :publication/state :published
   :publication/path "/probe"
   :translation/review :required})

(def ^:private default-source-revision "sha256-aaa111bbb222")

(def ^:private default-publication-index
  (resolver/publication-index [default-document default-garden default-intent]))

(def ^:private default-source-revisions
  {(:document/id default-document) default-source-revision})

(defn- receipt
  [& {:keys [org project document garden source-locale locale revision
             translation-revision dispatch-key]
      :or {org "org-1"
           project "knoxx-session"
           document :knoxx.docs/probe
           garden :knoxx.docs/promethean
           source-locale :en
           locale :es
           revision default-source-revision
           translation-revision "sha256-aaa111bbb222+es@batch-1"
           dispatch-key "key-1"}}]
  {:receipt/type :translation/completed
   :translation/document document
   :translation/garden garden
   :translation/source-locale source-locale
   :translation/locale locale
   :translation/source-revision revision
   :translation/revision translation-revision
   :translation/content-digest (str translation-revision "-content")
   :translation/dispatch-key dispatch-key
   :translation/org-id org
   :translation/project project
   :translation/at at})

(defn- request
  [& {:keys [locale revision translation-revision document garden]
      :or {document :knoxx.docs/probe
           garden :knoxx.docs/promethean
           locale :es
           revision "sha256-aaa111bbb222"
           translation-revision "sha256-aaa111bbb222+es@batch-1"}}]
  {:review/document document
   :review/garden garden
   :review/locale locale
   :review/revision revision
   :review/translation-revision translation-revision})

(defn- dispatch-record
  [outcome]
  (dispatch-law/dispatch-record
   {:document (:document/id default-document)
    :locale (:publication/locale default-intent)
    :revision default-source-revision
    :replace-stale? false}
   {:dispatch/garden "knoxx.docs/promethean"
    :dispatch/document-wire-id "knoxx.docs/probe"
    :dispatch/source-locale (:document/source-locale default-document)
    :dispatch/org-id (:org-id scope)
    :dispatch/project (:project scope)
    :dispatch/membership-id "member-1"}
   outcome
   at
   :attempt-id "dispatch-attempt-review"))

(defn- ^:async store-with!
  ([receipts]
   (store-with! receipts {}))
  ([receipts overrides]
   (let [evidence-store (store/memory-store)]
     (doseq [r receipts]
       (await (store/record-translation! evidence-store r)))
     (merge {:evidence-store evidence-store
             :clock (constantly at)
             :authorize-approval! (fn [_ _] (js/Promise.resolve true))
             :publication-index default-publication-index
             :source-revisions default-source-revisions}
            overrides))))

(defn- point-read-store
  "Delegate evidence persistence while making dispatch point reads observable.

   This is a protocol-level test double, not a global Var redefinition, so an
   async inventory projection cannot leak its lookup behavior into another test."
  [delegate point-read!]
  (reify store/ITranslationEvidenceStore
    (reserve-dispatch! [_ record]
      (store/reserve-dispatch! delegate record))
    (resolve-dispatch! [_ expected-record outcome detail]
      (store/resolve-dispatch! delegate expected-record outcome detail))
    (bind-dispatch-batch! [_ expected-record batch-id]
      (store/bind-dispatch-batch! delegate expected-record batch-id))
    (claim-dispatch-completion! [_ expected-record]
      (store/claim-dispatch-completion! delegate expected-record))
    (finish-dispatch-completion! [_ expected-record detail]
      (store/finish-dispatch-completion! delegate expected-record detail))
    (dispatch-for-key! [_ dispatch-key]
      (point-read! delegate dispatch-key))
    (dispatch-for-batch-document! [_ batch-id document-wire-id]
      (store/dispatch-for-batch-document! delegate batch-id document-wire-id))
    (dispatch-for-batch! [_ batch-id]
      (store/dispatch-for-batch! delegate batch-id))
    (record-translation! [_ completed]
      (store/record-translation! delegate completed))
    (completed-translations! [_ query]
      (store/completed-translations! delegate query))
    (record-approval! [_ approval]
      (store/record-approval! delegate approval))
    (approvals! [_ query]
      (store/approvals! delegate query))))

(def ^:private inventory-size 18)
(def ^:private inventory-garden :knoxx.gardens/inventory)

(defn- inventory-document-id
  [index]
  (keyword "knoxx.docs" (str "inventory-" index)))

(defn- inventory-publication-id
  [index]
  (keyword "knoxx.publications" (str "inventory-" index "-es")))

(defn- inventory-revision
  [index]
  (str "sha256-inventory-source-" index))

(defn- inventory-document
  [index]
  {:document/id (inventory-document-id index)
   :document/title (str "Inventory document " index)
   :document/source-locale :en
   :document/source {:path (str "docs/inventory-" index ".md")}})

(defn- inventory-intent
  [index]
  {:publication/id (inventory-publication-id index)
   :publication/document (inventory-document-id index)
   :publication/garden inventory-garden
   :publication/locale :es
   :publication/revision :source/current
   :publication/state :published
   :publication/path (str "/inventory/" index)
   :translation/review :required})

(defn- inventory-deps
  []
  (let [indices (range inventory-size)
        documents (mapv inventory-document indices)
        intents (mapv inventory-intent indices)
        garden {:garden/id inventory-garden
                :garden/title "Inventory"
                :garden/status :active
                :garden/locales [:en :es]}]
    {:publication-index (resolver/publication-index
                         (into [garden] (concat documents intents)))
     :source-revisions (into {}
                             (map (fn [index]
                                    [(inventory-document-id index)
                                     (inventory-revision index)]))
                             indices)}))

(defn- inventory-dispatch-record
  [work outcome & {:keys [detail]}]
  (dispatch-law/dispatch-record
   {:document (:translation/document work)
    :locale (:translation/locale work)
    :revision (:translation/source-revision work)
    :replace-stale? false}
   {:dispatch/garden
    (resource-identity/encode-keyword (:translation/garden work))
    :dispatch/document-wire-id
    (resource-identity/encode-keyword (:translation/document work))
    :dispatch/source-locale (:translation/source-locale work)
    :dispatch/org-id (:org-id scope)
    :dispatch/project (:project scope)
    :dispatch/membership-id "member-1"}
   outcome
   at
   :attempt-id (str "dispatch-attempt-inventory-"
                    (:translation/document work))
   :detail detail))

(t/deftest ^:async an-authorized-approval-is-recorded-with-attribution
  (let [deps (await (store-with! [(receipt)]))
        result (await (facade/approve-translation! deps scope (request)))
        approval (:approval result)]
    (t/testing "the approval is recorded"
      (t/is (= :recorded (:approval/status result))))

    (t/testing "identity comes from the receipt, not the request"
      (t/is (= :knoxx.docs/probe (:review/document approval)))
      (t/is (= :knoxx.docs/promethean (:review/garden approval)))
      (t/is (= :es (:review/locale approval)))
      (t/is (= "sha256-aaa111bbb222" (:review/revision approval)))
      (t/is (= "sha256-aaa111bbb222+es@batch-1" (:review/translation-revision approval))))

    (t/testing "the tenant and project are inherited from the evidence"
      ;; The request never carries them, so they cannot be forged.
      (t/is (= "org-1" (:review/org-id approval)))
      (t/is (= "knoxx-session" (:review/project approval))))

    (t/testing "the acting principal and the clock are attributed by the server"
      (t/is (= "reviewer@open-hax.local"
             (:principal/user-email (:review/principal approval))))
      (t/is (= at (:review/at approval))))))

(t/deftest ^:async approving-twice-is-the-same-fact-not-two
  (let [deps (await (store-with! [(receipt)]))
        first-result (await (facade/approve-translation! deps scope (request)))
        second-result (await (facade/approve-translation! deps scope (request)))]
    (t/testing "the second approval is recognized as the one already recorded"
      (t/is (= :recorded (:approval/status first-result)))
      (t/is (= :existing (:approval/status second-result)))
      (t/is (= (:approval first-result) (:approval second-result))))

    (t/testing "only one approval exists"
      ;; Appending would make `approved?` depend on how many times a button was
      ;; clicked.
      (t/is (= 1 (count (await (store/approvals! (:evidence-store deps) query-scope))))))))

(t/deftest ^:async content-admission-is-required-before-an-approval-write
  (let [base (await (store-with! [(receipt)]))]
    (t/testing "a caller cannot silently omit the byte-verification boundary"
      (let [error (try
                    (await (facade/approve-translation!
                            (dissoc base :authorize-approval!) scope (request)))
                    nil
                    (catch :default err err))]
        (t/is (= "translation approval requires content admission"
                 (.-message error)))
        (t/is (empty? (await (store/approvals! (:evidence-store base)
                                               query-scope))))))

    (t/testing "a failed byte check leaves no approval evidence"
      (let [deps (assoc base :authorize-approval!
                        (fn [_ _]
                          (js/Promise.reject
                           (ex-info "candidate bytes missing"
                                    {:code "translation_candidate_content_unavailable"}))))
            error (try
                    (await (facade/approve-translation! deps scope (request)))
                    nil
                    (catch :default err err))]
        (t/is (= "candidate bytes missing" (.-message error)))
        (t/is (empty? (await (store/approvals! (:evidence-store deps)
                                               query-scope))))))))

(t/deftest ^:async server-derived-source-locale-selects-the-exact-receipt
  (let [english (receipt :source-locale :en
                         :translation-revision "target-from-en")
        former-german (assoc (receipt :source-locale :de
                                      :translation-revision "target-from-de")
                             :translation/at "2026-08-22T11:00:00.000Z")
        observed (atom nil)
        deps (await
              (store-with!
               [english former-german]
               {:receipt-admissible? #(= :en (:translation/source-locale %))
                :authorize-approval!
                (fn [_ receipt]
                  (reset! observed receipt)
                  (js/Promise.resolve true))}))
        result (await (facade/approve-translation!
                       deps scope
                       (request :translation-revision "target-from-en")))]
    (t/is (= english @observed)
        "a newer former-locale receipt cannot win the approval lookup")
    (t/is (= :recorded (:approval/status result)))
    (t/is (= :en (get-in result [:approval :review/source-locale])))))

(t/deftest ^:async an-approval-with-no-translation-behind-it-is-refused
  (let [deps (await (store-with! []))
        result (await (facade/approve-translation! deps scope (request)))]
    (t/testing "there is nothing to approve, and the refusal says so"
      (t/is (= :translation-receipt-missing (:refusal/type (:approval/refusal result)))))

    (t/testing "nothing was persisted"
      ;; A rejected approval must leave no trace a later read could mistake for
      ;; evidence.
      (t/is (empty? (await (store/approvals! (:evidence-store deps) query-scope)))))))

(t/deftest ^:async historical-receipt-without-content-digest-is-not-approvable
  (let [deps (await (store-with! [(dissoc (receipt)
                                          :translation/content-digest)]))
        result (await (facade/approve-translation! deps scope (request)))]
    (t/is (= :translation-content-unbound
             (get-in result [:approval/refusal :refusal/type])))
    (t/is (empty? (await (store/approvals! (:evidence-store deps)
                                           query-scope))))))

(t/deftest ^:async a-mismatched-approval-is-refused-with-both-sides-named
  (let [deps (await (store-with! [(receipt)]))]
    (t/testing "a different target locale has no receipt of its own"
      (t/is (= :translation-receipt-missing
             (:refusal/type (:approval/refusal
                             (await (facade/approve-translation!
                                     deps scope (request :locale :fr))))))))

    (t/testing "naming a translation output that is not the current one is refused"
      (let [refusal (:approval/refusal
                     (await (facade/approve-translation!
                             deps scope
                             (request :translation-revision "sha256-aaa111bbb222+es@batch-9"))))]
        (t/is (= :translation-revision-mismatch (:refusal/type refusal)))
        (t/is (= "sha256-aaa111bbb222+es@batch-9" (:refusal/requested refusal)))
        (t/is (= "sha256-aaa111bbb222+es@batch-1" (:refusal/recorded refusal))
            "both sides travel, so a reviewer can see which was stale")))

    (t/testing "nothing was persisted by any refusal"
      (t/is (empty? (await (store/approvals! (:evidence-store deps) query-scope)))))))

(t/deftest ^:async a-reviewer-cannot-approve-another-tenants-translation
  (let [deps (await (store-with! [(receipt :org "org-other")]))
        result (await (facade/approve-translation! deps scope (request)))]
    (t/testing "the other tenant's receipt is invisible, so there is nothing to approve"
      (t/is (= :translation-receipt-missing (:refusal/type (:approval/refusal result)))))

    (t/testing "nothing was persisted"
      (t/is (empty? (await (store/approvals! (:evidence-store deps) query-scope)))))))

(t/deftest ^:async a-reviewer-cannot-approve-another-projects-translation
  (let [deps (await (store-with! [(receipt :project "other-project")]))
        result (await (facade/approve-translation! deps scope (request)))]
    (t/testing "project scope is enforced like tenant scope"
      (t/is (= :translation-receipt-missing (:refusal/type (:approval/refusal result)))))))

(t/deftest ^:async a-selector-revision-cannot-be-approved
  (let [deps (await (store-with! [(receipt)]))]
    (t/testing "the wire's dangerous string shape is refused by contract"
      ;; A revision arrives as decoded wire input here, so the string form of the
      ;; selector is the one that has to be caught.
      (t/is (thrown? js/Error
                   (await (facade/approve-translation!
                           deps scope (request :revision "source/current")))))
      (t/is (thrown? js/Error
                   (await (facade/approve-translation!
                           deps scope
                           (request :translation-revision "source/current"))))))))

(t/deftest ^:async approval-materializes-nothing
  (let [deps (await (store-with! [(receipt)]))
        _ (await (facade/approve-translation! deps scope (request)))]
    (t/testing "an approval may make a plan admissible but must not publish"
      ;; The card is explicit: recording review evidence and materializing
      ;; content are separate acts, so a reviewer can accept a translation
      ;; without also deciding when the bytes go live.
      (t/is (= 1 (count (await (store/approvals! (:evidence-store deps) query-scope)))))
      (t/is (= 1 (count (await (store/completed-translations!
                              (:evidence-store deps) query-scope)))))
      (t/is (empty? (filter #(= :publication/materialized (:receipt/type %))
                          (await (store/completed-translations!
                                  (:evidence-store deps) query-scope))))))))

(t/deftest ^:async review-list-exposes-the-exact-revisions-the-approval-needs
  (let [deps (await (store-with! [(receipt)]))
        before (first (:reviews (await (facade/reviewable-translations! deps scope))))]
    (t/is (= :knoxx.docs/probe (:document before)))
    (t/is (= :knoxx.publications/probe-es (:publication before)))
    (t/is (= :knoxx.docs/promethean (:garden before)))
    (t/is (= :es (:locale before)))
    (t/is (= "sha256-aaa111bbb222" (:revision before)))
    (t/is (= "sha256-aaa111bbb222+es@batch-1" (:translation_revision before)))
    (t/is (false? (:approved before)))
    (await (facade/approve-translation! deps scope (request)))
    (let [after (first (:reviews (await (facade/reviewable-translations! deps scope))))]
      (t/is (true? (:approved after)))
      (t/is (= at (:approved_at after))))))

(t/deftest ^:async desired-resource-cardinality-survives-sparse-candidate-evidence
  (let [{:keys [publication-index source-revisions] :as inventory} (inventory-deps)
        ;; Keep the only ready relation away from both ends. A projection that
        ;; accidentally truncates, takes the first receipt, or zips by position
        ;; cannot make this scenario pass by coincidence.
        ready-index 11
        ready-document (inventory-document-id ready-index)
        ready-publication (inventory-publication-id ready-index)
        ready-revision (inventory-revision ready-index)
        ready-output "sha256-inventory-target-11"
        completed (receipt :document ready-document
                           :garden inventory-garden
                           :revision ready-revision
                           :translation-revision ready-output
                           :dispatch-key "inventory-ready-key")
        deps (await (store-with! [completed] inventory))
        reviews (:reviews (await (facade/reviewable-translations! deps scope)))
        desired-publications (set (map :publication/id
                                       (:publications publication-index)))
        returned-publications (set (map :publication reviews))
        work-states (frequencies (map :work_state reviews))
        ready (first (filter #(= :ready (:work_state %)) reviews))]
    (t/testing "candidate evidence annotates desired work instead of defining it"
      (t/is (= 18 (count (:publications publication-index)))
          "the scenario literally declares eighteen resource intents")
      (t/is (= 1 (count (await (store/completed-translations!
                                (:evidence-store deps) query-scope))))
          "the scenario contains exactly one translation receipt")
      (t/is (= 18 (count reviews))
          "one completed candidate must not collapse eighteen desired relations to one row")
      (t/is (= desired-publications returned-publications)
          "the inventory contains every resource-derived publication relation exactly once")
      (t/is (= {:ready 1 :missing 17} work-states)))

    (t/testing "the one completed candidate keeps its immutable approval coordinates"
      (t/is (= ready-publication (:publication ready)))
      (t/is (= ready-document (:document ready)))
      (t/is (= ready-revision (:revision ready)))
      (t/is (= ready-output (:translation_revision ready))))

    (t/testing "the planned facade dependency carries concrete source revisions explicitly"
      (t/is (= ready-revision (get source-revisions ready-document))))))

(t/deftest ^:async an-orphan-receipt-cannot-invent-a-resource-work-item
  (let [inventory (inventory-deps)
        orphan (receipt :document :knoxx.docs/orphan
                        :garden inventory-garden
                        :revision "sha256-orphan-source"
                        :translation-revision "sha256-orphan-target"
                        :dispatch-key "inventory-orphan-key")
        deps (await (store-with! [orphan] inventory))
        reviews (:reviews (await (facade/reviewable-translations! deps scope)))]
    (t/testing "observed evidence may only annotate a declared relation"
      (t/is (= 18 (count reviews)))
      (t/is (= {:missing 18} (frequencies (map :work_state reviews))))
      (t/is (not-any? #(= :knoxx.docs/orphan (:document %)) reviews)))))

(t/deftest ^:async the-facade-authors-project-on-the-envelope-and-every-row
  (let [authoritative-project "review-stage-nondefault"
        acting-scope (assoc scope :project authoritative-project)
        deps (await (store-with! [] (inventory-deps)))
        result (await (facade/reviewable-translations! deps acting-scope))]
    (t/testing "project authority is the server scope, not a resource or receipt default"
      (t/is (= authoritative-project (:project result)))
      (t/is (= inventory-size (count (:reviews result))))
      (t/is (every? #(= authoritative-project (:project %))
                    (:reviews result))))))

(t/deftest ^:async dispatch-point-reads-drive-inventory-state-and-actions
  (let [deps (await (store-with! []))
        evidence-store (:evidence-store deps)
        accepted (dispatch-record :dispatch/accepted)]
    (await (store/reserve-dispatch! evidence-store accepted))
    (let [row (first (:reviews
                      (await (facade/reviewable-translations! deps scope))))]
      (t/testing "the exact stored claim suppresses a duplicate dispatch action"
        (t/is (= :dispatch/accepted (:dispatch_outcome row)))
        (t/is (= :in_flight (:work_state row)))
        (t/is (= [] (:allowed_actions row)))))

    (await (store/resolve-dispatch! evidence-store accepted
                                    :dispatch/failed "provider unavailable"))
    (let [row (first (:reviews
                      (await (facade/reviewable-translations! deps scope))))]
      (t/testing "the same point-read exposes a terminal retriable attempt"
        (t/is (= :dispatch/failed (:dispatch_outcome row)))
        (t/is (= "provider unavailable" (:dispatch_detail row)))
        (t/is (= :failed (:work_state row)))
        (t/is (= [:retry] (:allowed_actions row)))))))

(t/deftest ^:async every-resource-row-uses-its-own-dispatch-key
  (let [unresolved-document (inventory-document-id 17)
        inventory-data (update (inventory-deps)
                               :source-revisions
                               dissoc
                               unresolved-document)
        work (mapv #(assoc % :translation/project (:project scope))
                   (inventory/desired-work (:publication-index inventory-data)
                                           (:source-revisions inventory-data)))
        [in-flight-work failed-work duplicate-work] work
        in-flight-record (inventory-dispatch-record
                          in-flight-work :dispatch/accepted)
        failed-record (inventory-dispatch-record failed-work :dispatch/accepted)
        duplicate-record (inventory-dispatch-record
                          duplicate-work :dispatch/duplicate)
        delegate (store/memory-store)
        looked-up (atom [])
        observed-store
        (point-read-store
         delegate
         (fn [actual-store dispatch-key]
           (swap! looked-up conj dispatch-key)
           (store/dispatch-for-key! actual-store dispatch-key)))
        deps (await (store-with! [] (assoc inventory-data
                                           :evidence-store observed-store)))
        evidence-store (:evidence-store deps)
        expected-keys (into #{}
                            (keep #(inventory/dispatch-lookup-key scope %))
                            work)]
    (await (store/reserve-dispatch! evidence-store in-flight-record))
    (await (store/reserve-dispatch! evidence-store failed-record))
    (await (store/resolve-dispatch! evidence-store
                                    failed-record
                                    :dispatch/failed
                                    "provider unavailable"))
    (await (store/reserve-dispatch! evidence-store duplicate-record))
    (let [result (await (facade/reviewable-translations! deps scope))
          by-publication (into {}
                               (map (juxt :publication identity))
                               (:reviews result))
          row-for (fn [item]
                    (get by-publication (:publication/id item)))
          unresolved-row (first (filter #(= unresolved-document (:document %))
                                        (:reviews result)))]
      (t/testing "every concrete desired relation performs one exact point read"
        (t/is (= expected-keys (set @looked-up)))
        (t/is (= (count expected-keys) (count @looked-up)))
        (t/is (= (zipmap expected-keys (repeat 1))
                 (frequencies @looked-up))))

      (t/testing "independent outcomes stay attached to their own rows"
        (t/is (= [:in_flight :dispatch/accepted []]
                 ((juxt :work_state :dispatch_outcome :allowed_actions)
                  (row-for in-flight-work))))
        (t/is (= [:failed :dispatch/failed "provider unavailable" [:retry]]
                 ((juxt :work_state :dispatch_outcome :dispatch_detail
                        :allowed_actions)
                  (row-for failed-work))))
        (t/is (= [:evidence_missing :dispatch/duplicate [:retry]]
                 ((juxt :work_state :dispatch_outcome :allowed_actions)
                  (row-for duplicate-work)))))

      (t/testing "an unresolved revision is visible but never dispatchable"
        (t/is (= :revision_unresolved (:work_state unresolved-row)))
        (t/is (nil? (:revision unresolved-row)))
        (t/is (= [] (:allowed_actions unresolved-row)))))))

(t/deftest ^:async a-corrupt-dispatch-point-read-fails-closed
  (let [looked-up (atom [])
        evidence-store
        (point-read-store
         (store/memory-store)
         (fn [_ dispatch-key]
           (swap! looked-up conj dispatch-key)
           (js/Promise.resolve
            {:dispatch/key (str dispatch-key ":wrong")})))
        deps (await (store-with! [] {:evidence-store evidence-store}))
        error (try
                (await (facade/reviewable-translations! deps scope))
                nil
                (catch :default err err))]
    (t/testing "a replaceable store cannot attach another claim to desired work"
      (t/is (= 1 (count @looked-up)))
      (t/is (string? (first @looked-up)))
      (t/is (some? error))
      (t/is (= "translation dispatch lookup returned the wrong claim"
             (.-message error)))
      (t/is (= (first @looked-up)
             (:expected-dispatch-key (ex-data error))))
      (t/is (= (str (first @looked-up) ":wrong")
             (:actual-dispatch-key (ex-data error)))))))
