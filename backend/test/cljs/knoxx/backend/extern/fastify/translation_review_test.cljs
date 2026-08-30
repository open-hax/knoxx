(ns knoxx.backend.extern.fastify.translation-review-test
  "The approval boundary: what a caller may and may not put in the request.

  Every property here is one an untrusted caller would otherwise control. The
  principal, the timestamp and the tenant are all attributed by the server, and
  the only way to be sure of that is to try to send them."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.publication-resolver :as resolver]
            [knoxx.backend.domain.translation-review-inventory :as inventory]
            [knoxx.backend.extern.fastify.translation-review :as adapter]
            [knoxx.backend.infra.publication-source-revision :as source-revision]
            [knoxx.backend.infra.translation-evidence-store :as evidence-store-api]
            [knoxx.backend.law.translation-dispatch :as dispatch-law]
            [knoxx.backend.law.translation-evidence :as law]))

(defn- request
  [body]
  (js-obj "body" (clj->js body)))

(def ^:private valid-body
  {:document "knoxx.docs/probe"
   :garden "knoxx.docs/promethean"
   :locale "es"
   :revision "sha256-aaa111bbb222"
   :translation_revision "sha256-aaa111bbb222+es@batch-1"})

(deftest a-well-formed-body-decodes-to-the-identities-receipts-are-keyed-by
  (let [decoded (adapter/decode-request (request valid-body))]
    (testing "the document, garden and locale become keywords"
      (is (= :knoxx.docs/probe (:review/document decoded)))
      (is (= :knoxx.docs/promethean (:review/garden decoded)))
      (is (= :es (:review/locale decoded))))

    (testing "revisions stay opaque strings"
      ;; A revision is opaque text. Decoding it to a keyword would invent
      ;; structure that is not there.
      (is (= "sha256-aaa111bbb222" (:review/revision decoded)))
      (is (= "sha256-aaa111bbb222+es@batch-1" (:review/translation-revision decoded))))))

(deftest a-caller-cannot-supply-its-own-principal-or-timestamp
  ;; The request contract is closed, so the fields attribution owns cannot be
  ;; smuggled through the body.
  (doseq [field [:principal :review/principal :at :review/at :org_id :project]]
    (is (thrown? js/Error
                 (adapter/decode-request (request (assoc valid-body field "forged"))))
        (str field " was accepted"))))

(deftest every-field-is-required-and-must-not-be-blank
  (doseq [field [:document :garden :locale :revision :translation_revision]]
    (testing (str field " is required")
      (is (thrown? js/Error (adapter/decode-request (request (dissoc valid-body field))))))

    (testing (str field " may not be blank")
      (is (thrown? js/Error (adapter/decode-request (request (assoc valid-body field "")))))
      (is (thrown? js/Error (adapter/decode-request (request (assoc valid-body field "  "))))))))

(deftest an-unqualified-document-or-garden-is-refused
  (testing "a bare name is a different document from the qualified one"
    (is (thrown? js/Error
                 (adapter/decode-request (request (assoc valid-body :document "probe"))))))

  (testing "the same holds for the garden the approval is scoped to"
    ;; An unqualified garden is a different garden, and an approval filed
    ;; against one the receipts are not keyed by can never match — so it would
    ;; read as 'nothing to approve' rather than as a malformed request.
    (is (thrown? js/Error
                 (adapter/decode-request (request (assoc valid-body :garden "promethean")))))))

(deftest a-selector-revision-is-refused-in-either-position
  ;; This is the boundary where a revision arrives as decoded wire input, so the
  ;; string spelling of the selector is the one that has to be caught.
  (is (thrown? js/Error
               (adapter/decode-request (request (assoc valid-body
                                                       :revision "source/current")))))
  (is (thrown? js/Error
               (adapter/decode-request (request (assoc valid-body
                                                       :translation_revision "source/current"))))))

(deftest the-principal-comes-from-the-auth-context
  (testing "any one durable identity is enough"
    (is (= {:principal/user-email "a@b.c"}
           (adapter/principal-of {:user-email "a@b.c"})))
    (is (= {:principal/user-id "u1"} (adapter/principal-of {:user-id "u1"})))
    (is (= {:principal/membership-id "m1"} (adapter/principal-of {:membership-id "m1"}))))

  (testing "a context identifying nobody cannot produce review evidence"
    ;; Evidence attributable to nobody is indistinguishable from evidence nobody
    ;; produced.
    (is (thrown? js/Error (adapter/principal-of {})))
    (is (thrown? js/Error (adapter/principal-of nil)))))

(deftest the-review-scope-is-taken-from-context-and-config
  (testing "the organization comes from the context"
    (is (= "org-1" (:org-id (adapter/review-scope {} {:org-id "org-1"
                                                      :user-email "a@b.c"})))))

  (testing "the project comes from configuration, matching where batches are filed"
    (is (= "knoxx-session"
           (:project (adapter/review-scope {:session-project-name "knoxx-session"}
                                           {:org-id "org-1" :user-email "a@b.c"})))))

  (testing "an approval cannot be filed under an invented tenant"
    (is (thrown? js/Error (adapter/review-scope {} {:user-email "a@b.c"})))))

(deftest responses-distinguish-recorded-existing-and-refused
  (testing "a first approval is 201"
    (is (= 201 (:status (adapter/response-for {:approval/status :recorded
                                               :approval {:review/state :approved}})))))

  (testing "an already-recorded approval is 200, not a conflict"
    ;; An honest double-click is not something a reviewer has to resolve.
    (is (= 200 (:status (adapter/response-for {:approval/status :existing
                                               :approval {:review/state :approved}})))))

  (testing "a refusal is 409 and carries its typed evidence to the caller"
    (let [{:keys [status body]}
          (adapter/response-for {:approval/refusal
                                 {:refusal/type :translation-revision-mismatch
                                  :refusal/requested "a"
                                  :refusal/recorded "b"}})]
      (is (= 409 status))
      (is (true? (:refused body)))
      (is (= :translation-revision-mismatch (:refusal/type (:refusal body))))))

  (testing "every refusal type the law can produce has a status chosen for it"
    ;; Compared against `law/approval-refusal-types`, not against a second copy
    ;; of the adapter's own keys. A restated list validates the table against
    ;; itself: adding a refusal type to the law and forgetting the adapter left
    ;; both this assertion and `refusal-status` unchanged and still agreeing,
    ;; while the new type fell through `get`'s default to a status nobody chose.
    (is (= law/approval-refusal-types (set (keys adapter/refusal-status))))))

(def ^:private hydration-document
  {:document/id :knoxx.docs/probe
   :document/title "Probe"
   :document/source-locale :en
   :document/source {:path "docs/probe.md"}})

(def ^:private hydration-source "Source text")
(def ^:private hydration-source-revision
  (source-revision/content-revision hydration-source))

(def ^:private hydration-review
  {:publication :knoxx.publications/probe-es
   :document :knoxx.docs/probe
   :garden :knoxx.docs/promethean
   :project "review-stage"
   :source_locale :en
   :locale :es
   :revision hydration-source-revision
   :translation_revision "sha256-target"
   :candidate_present true
   :reviewable true})

(def ^:private hydration-receipt
  {:receipt/type :translation/completed
   :translation/document :knoxx.docs/probe
   :translation/garden :knoxx.docs/promethean
   :translation/source-locale :en
   :translation/locale :es
   :translation/source-revision hydration-source-revision
   :translation/revision "sha256-target"
   :translation/content-digest
   (source-revision/content-revision "Texto traducido")
   :translation/dispatch-key "dispatch-key"
   :translation/org-id "org-1"
   :translation/project "review-stage"
   :translation/at "2026-08-30T10:00:00.000Z"})

(defn- hydration-dependencies
  ([source translated]
   (hydration-dependencies source translated nil))
  ([source translated observed-receipts]
   {:source-content! (fn [_ _] (js/Promise.resolve source))
    :agent-content!
    (fn [_ receipt]
      (when observed-receipts
        (swap! observed-receipts conj receipt))
      (js/Promise.resolve translated))}))

(def ^:private approval-garden
  {:garden/id :knoxx.docs/promethean
   :garden/title "Promethean"
   :garden/status :active
   :garden/locales [:en :es]})

(def ^:private approval-intent
  {:publication/id :knoxx.publications/probe-es
   :publication/document :knoxx.docs/probe
   :publication/garden :knoxx.docs/promethean
   :publication/locale :es
   :publication/revision :source/current
   :publication/state :published
   :publication/path "/probe"
   :translation/review :required})

(def ^:private approval-index
  (resolver/publication-index
   [hydration-document approval-garden approval-intent]))

(def ^:private undeclared-approval-index
  (resolver/publication-index [hydration-document approval-garden]))

(def ^:private approval-config
  {:session-project-name "review-stage"
   :publication-content-root "/translated"})

(def ^:private approval-context
  {:org-id "org-1" :user-email "reviewer@example.test"})

(defn- approval-handlers
  [permission-checks]
  {:with-request-context! (fn [_runtime _request _reply operation]
                            (operation approval-context))
   :ensure-permission! (fn [actual-ctx permission]
                         (swap! permission-checks conj
                                [actual-ctx permission]))})

(defn- route-capture
  [routes]
  (js-obj "route" (fn [options]
                    (swap! routes conj options))))

(defn- response-reply
  [response]
  (let [reply (js-obj)]
    (aset reply "code" (fn [status]
                          (swap! response assoc :status status)
                          reply))
    (aset reply "type" (fn [_content-type] reply))
    (aset reply "send" (fn [body]
                          (swap! response assoc :body body)
                          reply))
    (aset reply "sent" false)
    reply))

(defn- point-read-evidence-store
  "Delegate persistence while exposing the dispatch key used by a list route."
  [delegate point-read!]
  (reify evidence-store-api/ITranslationEvidenceStore
    (reserve-dispatch! [_ record]
      (evidence-store-api/reserve-dispatch! delegate record))
    (resolve-dispatch! [_ expected-record outcome detail]
      (evidence-store-api/resolve-dispatch! delegate expected-record outcome detail))
    (bind-dispatch-batch! [_ expected-record batch-id]
      (evidence-store-api/bind-dispatch-batch! delegate expected-record batch-id))
    (claim-dispatch-completion! [_ expected-record]
      (evidence-store-api/claim-dispatch-completion! delegate expected-record))
    (finish-dispatch-completion! [_ expected-record detail]
      (evidence-store-api/finish-dispatch-completion! delegate expected-record detail))
    (dispatch-for-key! [_ dispatch-key]
      (point-read! dispatch-key))
    (dispatch-for-batch-document! [_ batch-id document-wire-id]
      (evidence-store-api/dispatch-for-batch-document!
       delegate batch-id document-wire-id))
    (dispatch-for-batch! [_ batch-id]
      (evidence-store-api/dispatch-for-batch! delegate batch-id))
    (record-translation! [_ completed]
      (evidence-store-api/record-translation! delegate completed))
    (completed-translations! [_ query]
      (evidence-store-api/completed-translations! delegate query))
    (record-approval! [_ approval]
      (evidence-store-api/record-approval! delegate approval))
    (approvals! [_ query]
      (evidence-store-api/approvals! delegate query))))

(defn- ^:async invoke-registered-approval!
  "Drive the registered Fastify handler through the real approval facade/store."
  [{:keys [index source translated revision]
    :or {revision hydration-source-revision}}]
  (let [routes (atom [])
        app (route-capture routes)
        response (atom {})
        reply (response-reply response)
        permission-checks (atom [])
        content-reads (atom [])
        evidence-store (evidence-store-api/memory-store)
        wire-body (assoc valid-body
                         :revision revision
                         :translation_revision "sha256-target")
        receipt (assoc hydration-receipt
                       :translation/source-revision revision)]
    (await (evidence-store-api/record-translation!
            evidence-store receipt))
    (adapter/register-translation-review-routes!
     app {} approval-config (approval-handlers permission-checks)
     {:evidence-store evidence-store
      :resource-records! (fn [_] (js/Promise.resolve ::records))
      :publication-index (constantly index)
      :document-source-roots
      (fn [_ _] {:knoxx.docs/probe "/contracts"})
      :source-revisions!
      (fn [_ _ _]
        (js/Promise.resolve {:knoxx.docs/probe revision}))
      :ensure-contract-receipts!
      (fn [_ _ _ _ _] (js/Promise.resolve []))
      :authenticate-receipts!
      (fn [_ _ _ _ receipts]
        (doseq [candidate receipts]
          (swap! content-reads conj
                 [:target (:translation/revision candidate)]))
        (js/Promise.resolve
         (filterv #(= (:translation/content-digest %)
                      (source-revision/content-revision translated))
                  receipts)))
      :source-content!
      (fn [_ document]
        (swap! content-reads conj [:source (:document/id document)])
        (js/Promise.resolve source))
      :agent-content!
      (fn [_ candidate]
        (swap! content-reads conj
               [:target (:translation/revision candidate)])
        (js/Promise.resolve translated))})
    (let [route (second @routes)]
      (await
       ((aget route "handler")
        (request wire-body)
        reply)))
    {:response @response
     :approvals (await (evidence-store-api/approvals!
                        evidence-store
                        {:org-id "org-1" :project "review-stage"}))
     :permission-checks @permission-checks
     :content-reads @content-reads}))

(deftest review-columns-must-contain-visible-content
  (doseq [content [nil "" " " "\n\t"]]
    (is (false? (#'adapter/displayable-content? content))))
  (is (true? (#'adapter/displayable-content? "Visible"))))

(deftest ^:async the-registered-approval-route-admits-only-current-visible-resource-bytes
  (testing "a receipt cannot be approved when no desired resource declares it"
    (let [{:keys [response approvals content-reads]}
          (await (invoke-registered-approval!
                  {:index undeclared-approval-index
                   :source hydration-source
                   :translated "Texto traducido"}))]
      (is (= 409 (:status response)))
      (is (empty? approvals))
      (is (empty? content-reads)
          "an undeclared receipt is refused before any candidate bytes are trusted")))

  (testing "the source bytes must still hash to the declared snapshot revision"
    (let [{:keys [response approvals]}
          (await (invoke-registered-approval!
                  {:index approval-index
                   :source "Source text moved after the resource snapshot"
                   :translated "Texto traducido"}))]
      (is (= 409 (:status response)))
      (is (empty? approvals))))

  (testing "the target bytes must still hash to the completed receipt"
    (let [{:keys [response approvals]}
          (await (invoke-registered-approval!
                  {:index approval-index
                   :source hydration-source
                   :translated "Texto cambiado después de la revisión"}))]
      (is (= 409 (:status response)))
      (is (empty? approvals))))

  (testing "both review columns must contain visible nonblank content"
    (let [blank " \n\t"]
      (doseq [[source translated revision column]
              [[blank "Texto traducido"
                (source-revision/content-revision blank) :source]
               [hydration-source blank hydration-source-revision :target]]]
        (let [{:keys [response approvals]}
              (await (invoke-registered-approval!
                      {:index approval-index
                       :source source
                       :translated translated
                       :revision revision}))]
          (is (= 409 (:status response)) (str column " bytes were accepted"))
          (is (empty? approvals)
              (str column " bytes produced approval evidence"))))))

  (testing "exact current source and target bytes reach the facade write path"
    (let [{:keys [response approvals permission-checks content-reads]}
          (await (invoke-registered-approval!
                  {:index approval-index
                   :source hydration-source
                   :translated "Texto traducido"}))
          approval (first approvals)]
      (is (= 201 (:status response)))
      (is (= 1 (count approvals)))
      (is (= :knoxx.docs/probe (:review/document approval)))
      (is (= :en (:review/source-locale approval)))
      (is (= hydration-source-revision (:review/revision approval)))
      (is (= "sha256-target" (:review/translation-revision approval)))
      (is (= [[{:org-id "org-1" :user-email "reviewer@example.test"}
               adapter/approve-permission]]
             permission-checks))
      (is (= [[:target "sha256-target"]
              [:source :knoxx.docs/probe]
              [:target "sha256-target"]]
             content-reads)))))

(deftest hydration-relations-include-the-source-locale
  (let [review-relation (#'adapter/review-relation hydration-review)
        receipt-relation (#'adapter/receipt-relation hydration-receipt)
        wrong-source-relation (#'adapter/receipt-relation
                               (assoc hydration-receipt
                                      :translation/source-locale :de))]
    (testing "a matching review and receipt share all six immutable coordinates"
      (is (= receipt-relation review-relation)))

    (testing "output from a former source language cannot hydrate this review"
      (is (not= wrong-source-relation review-relation))
      (is (= :en (nth review-relation 2)))
      (is (= :de (nth wrong-source-relation 2))))))

(deftest ^:async a-resource-candidate-with-missing-output-is-not-reviewable
  (let [relation (#'adapter/review-relation hydration-review)
        observed-receipts (atom [])
        result (await
                (#'adapter/hydrate-review!
                 {:publication-content-root "/translated"}
                 {:documents {:knoxx.docs/probe hydration-document}}
                 {:knoxx.docs/probe "/contracts"}
                 #{}
                 {relation hydration-receipt}
                 hydration-review
                 (hydration-dependencies
                  hydration-source nil observed-receipts)))]
    (testing "the candidate relation is retained even when its output cannot be loaded"
      (is (= [hydration-receipt] @observed-receipts))
      (is (true? (:contract_candidate result)))
      (is (= :content_missing (:hydration_state result)))
      (is (not (contains? result :source_text))))

    (testing "candidate existence alone never enables approval"
      (is (false? (:reviewable result)))
      (is (not (contains? result :translated_text)))
      (is (not (contains? result :content_source))))))

(deftest ^:async source-movement-between-snapshot-and-hydration-fails-closed
  (let [relation (#'adapter/review-relation hydration-review)
        result (await
                (#'adapter/hydrate-review!
                 {:publication-content-root "/translated"}
                 {:documents {:knoxx.docs/probe hydration-document}}
                 {:knoxx.docs/probe "/contracts"}
                 #{}
                 {relation hydration-receipt}
                 hydration-review
                 (hydration-dependencies
                  "Moved source" "Texto traducido")))]
    (is (true? (:contract_candidate result)))
    (is (false? (:reviewable result)))
    (is (= :source_moved (:hydration_state result)))
    (is (not (contains? result :source_text)))
    (is (not (contains? result :translated_text)))
    (is (not (contains? result :content_source)))))

(deftest ^:async target-movement-between-receipt-and-hydration-fails-closed
  (let [relation (#'adapter/review-relation hydration-review)
        result (await
                (#'adapter/hydrate-review!
                 {:publication-content-root "/translated"}
                 {:documents {:knoxx.docs/probe hydration-document}}
                 {:knoxx.docs/probe "/contracts"}
                 #{}
                 {relation hydration-receipt}
                 hydration-review
                 (hydration-dependencies
                  hydration-source "Texto cambiado")))]
    (is (true? (:contract_candidate result)))
    (is (false? (:reviewable result)))
    (is (= :content_moved (:hydration_state result)))
    (is (not (contains? result :source_text)))
    (is (not (contains? result :translated_text)))))

(deftest ^:async computed-source-revisions-cross-the-fastify-review-boundary
  (let [alpha :knoxx.docs/alpha
        beta :knoxx.docs/beta
        documents [{:document/id alpha
                    :document/title "Alpha"
                    :document/source-locale :en
                    :document/source {:path "docs/alpha.md"}}
                   {:document/id beta
                    :document/title "Beta"
                    :document/source-locale :en
                    :document/source {:path "docs/beta.md"}}]
        index {:documents (into {} (map (juxt :document/id identity)) documents)}
        roots {alpha "/contracts-a" beta "/contracts-b"}
        revisions {alpha "sha256-alpha" beta "sha256-beta"}
        observed-source-call (atom nil)
        observed-receipt-call (atom nil)
        observed-facade-call (atom nil)
        config {:session-project-name "review-stage"}
        routes (atom [])
        response (atom {})
        permission-checks (atom [])
        app (route-capture routes)
        reply (response-reply response)
        evidence-store (evidence-store-api/memory-store)
        dependencies
        {:evidence-store evidence-store
         :split-store ::split-store
         :resource-records! (fn [_] (js/Promise.resolve ::records))
         :publication-index (fn [_] index)
         :document-source-roots (fn [_ _] roots)
         :source-revisions!
         (fn [actual-config actual-documents actual-roots]
           (reset! observed-source-call
                   {:config actual-config
                    :documents actual-documents
                    :roots actual-roots})
           (js/Promise.resolve revisions))
         :ensure-contract-receipts!
         (fn [store actual-index actual-roots scope actual-revisions]
           (reset! observed-receipt-call
                   {:store store
                    :index actual-index
                    :roots actual-roots
                    :scope scope
                    :source-revisions actual-revisions})
           (js/Promise.resolve []))
         :authenticate-receipts!
         (fn [_ _ _ _ receipts] (js/Promise.resolve (vec receipts)))
         :reviewable-translations!
         (fn [deps scope]
           (reset! observed-facade-call {:deps deps :scope scope})
           (js/Promise.resolve
            {:project (:project scope)
             :reviews
             (mapv (fn [{document-id :document/id}]
                     {:publication
                      (keyword "knoxx.publications"
                               (str (name document-id) "-es"))
                      :document document-id
                      :garden :knoxx.gardens/promethean
                      :project (:project scope)
                      :source_locale :en
                      :locale :es
                      :revision (get (:source-revisions deps) document-id)
                      :revision_selector :source/current
                      :work_state :missing
                      :reviewable false
                      :approved false
                      :allowed_actions [:dispatch]})
                   documents)}))}]
    (adapter/register-translation-review-routes!
     app {} config (approval-handlers permission-checks) dependencies)
    (await
     ((aget (first @routes) "handler")
      (request {})
      reply))
    (let [wire-result (js->clj (:body @response) :keywordize-keys true)]
      (testing "the registered GET computes both revisions from one resource snapshot"
        (is (= 200 (:status @response)))
        (is (= [[approval-context adapter/read-permission]]
               @permission-checks))
        (is (= config (:config @observed-source-call)))
        (is (= (set documents) (set (:documents @observed-source-call))))
        (is (= roots (:roots @observed-source-call)))
        (is (= revisions (:source-revisions @observed-receipt-call))))

      (testing "that exact map crosses the extern/facade boundary intact"
        ;; Before this handoff existed, receipt-backed rows could carry a revision
        ;; but missing work could not, making observed evidence the row authority.
        (is (= revisions (get-in @observed-facade-call
                                 [:deps :source-revisions])))
        (is (= ::split-store
               (get-in @observed-facade-call [:deps :split-store])))
        (is (= "review-stage" (:project wire-result)))
        (is (= #{["knoxx.docs/alpha" "sha256-alpha"]
                 ["knoxx.docs/beta" "sha256-beta"]}
               (set (map (juxt :document :revision)
                         (:reviews wire-result)))))))))

(deftest ^:async registered-list-uses-the-computed-revision-for-its-dispatch-point-read
  (let [scope {:org-id "org-1" :project "review-stage"}
        revisions {:knoxx.docs/probe hydration-source-revision}
        [work] (inventory/desired-work approval-index revisions)
        expected-key (inventory/dispatch-lookup-key scope work)
        expected-record
        (dispatch-law/dispatch-record
         {:document (:translation/document work)
          :locale (:translation/locale work)
          :revision (:translation/source-revision work)
          :replace-stale? false}
         {:dispatch/garden "knoxx.docs/promethean"
          :dispatch/document-wire-id "knoxx.docs/probe"
          :dispatch/source-locale (:translation/source-locale work)
          :dispatch/org-id (:org-id scope)
          :dispatch/project (:project scope)
          :dispatch/membership-id "member-1"}
         :dispatch/accepted
         "2026-08-30T10:00:00.000Z"
         :attempt-id "dispatch-attempt-list-boundary")
        observed-keys (atom [])
        delegate (evidence-store-api/memory-store)
        evidence-store
        (point-read-evidence-store
         delegate
         (fn [dispatch-key]
           (swap! observed-keys conj dispatch-key)
           (js/Promise.resolve
            (when (= expected-key dispatch-key) expected-record))))
        routes (atom [])
        response (atom {})
        app (route-capture routes)
        reply (response-reply response)
        dependencies
        {:evidence-store evidence-store
         :split-store ::split-store
         :resource-records! (fn [_] (js/Promise.resolve ::records))
         :publication-index (constantly approval-index)
         :document-source-roots
         (fn [_ _] {:knoxx.docs/probe "/contracts"})
         :source-revisions!
         (fn [_ documents roots]
           (is (= [hydration-document] documents))
           (is (= {:knoxx.docs/probe "/contracts"} roots))
           (js/Promise.resolve revisions))
         :ensure-contract-receipts!
         (fn [_ _ _ _ source-revisions]
           (is (= revisions source-revisions))
           (js/Promise.resolve []))
         :authenticate-receipts!
         (fn [_ _ _ _ receipts]
           (js/Promise.resolve (vec receipts)))}]
    (adapter/register-translation-review-routes!
     app {} approval-config (approval-handlers (atom [])) dependencies)
    (await
     ((aget (first @routes) "handler")
      (request {})
      reply))
    (let [[row] (:reviews (js->clj (:body @response) :keywordize-keys true))]
      (testing "the real facade derives one exact, non-nil lookup key"
        (is (= 200 (:status @response)))
        (is (string? expected-key))
        (is (seq expected-key))
        (is (= [expected-key] @observed-keys)))

      (testing "the non-nil point-read result reaches the wire inventory row"
        (is (= hydration-source-revision (:revision row)))
        (is (= "in_flight" (:work_state row)))
        (is (= "dispatch/accepted" (:dispatch_outcome row)))
        (is (= [] (:allowed_actions row)))))))
