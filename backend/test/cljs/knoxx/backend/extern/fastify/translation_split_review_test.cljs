(ns knoxx.backend.extern.fastify.translation-split-review-test
  "Wire-boundary tests for resource-backed split review mutations."
  (:require [cljs.test :as t]
            [knoxx.backend.extern.fastify.translation-review :as adapter]))

(defn- request
  [body]
  (js-obj "body" (clj->js body)))

(def ^:private valid-body
  {:candidate_set_id "candidate-set-1"
   :split_id "split-1"
   :status "approved"
   :corrected_text "Texto corregido."})

(def ^:private valid-bulk-body
  {:candidate_set_id "candidate-set-1"
   :status "in-review"
   :adequacy "adequate"
   :fluency "good"
   :terminology "minor_errors"
   :risk "safe"
   :editor_notes "Review every persisted split again."})

(t/deftest minimal-verdict-and-optional-evaluations-decode-to-client-facts
  (t/is (= {:split-review/candidate-set-id "candidate-set-1"
            :split-review/split-id "split-1"
            :split-review/status :approved
            :split-review/corrected-text "Texto corregido."}
           (adapter/decode-split-review-request (request valid-body))))
  (t/is (= {:split-review/candidate-set-id "candidate-set-1"
            :split-review/split-id "split-1"
            :split-review/status :in-review
            :review/adequacy "poor"
            :review/fluency "adequate"
            :review/terminology "minor_errors"
            :review/risk "sensitive"
            :review/editor-notes "Needs another pass."}
           (adapter/decode-split-review-request
            (request {:candidate_set_id "candidate-set-1"
                      :split_id "split-1"
                      :status "in-review"
                      :adequacy "poor"
                      :fluency "adequate"
                      :terminology "minor_errors"
                      :risk "sensitive"
                      :editor_notes "Needs another pass."}))))
  (t/is (= :rejected
           (:split-review/status
            (adapter/decode-split-review-request
             (request {:candidate_set_id "candidate-set-1"
                       :split_id "split-1"
                       :status "rejected"}))))))

(t/deftest split-review-body-is-closed-and-cannot-forge-server-authority
  (doseq [field [:principal :review/principal :recorded_at :timestamp
                 :org_id :project :manifest_id :candidate_digest
                 :operation_id]]
    (t/is (thrown? js/Error
                   (adapter/decode-split-review-request
                    (request (assoc valid-body field "forged"))))
          (str field " was accepted")))
  (doseq [body [(dissoc valid-body :candidate_set_id)
                (dissoc valid-body :split_id)
                (dissoc valid-body :status)
                (assoc valid-body :candidate_set_id " ")
                (assoc valid-body :split_id "")
                (assoc valid-body :status "approve")
                (assoc valid-body :corrected_text "  ")]]
    (t/is (thrown? js/Error
                   (adapter/decode-split-review-request (request body))))))

(t/deftest bulk-review-body-delegates-membership-and-corrections-to-the-server
  (t/is (= {:split-review/candidate-set-id "candidate-set-1"
            :split-review/status :in-review
            :review/adequacy "adequate"
            :review/fluency "good"
            :review/terminology "minor_errors"
            :review/risk "safe"
            :review/editor-notes "Review every persisted split again."}
           (adapter/decode-bulk-split-review-request
            (request valid-bulk-body))))
  (doseq [body [(assoc valid-bulk-body :split_id "split-1")
                (assoc valid-bulk-body :corrected_text "wrong fan-out")
                (assoc valid-bulk-body :principal "forged")
                (dissoc valid-bulk-body :candidate_set_id)
                (dissoc valid-bulk-body :status)]]
    (t/is (thrown? js/Error
                   (adapter/decode-bulk-split-review-request (request body))))))

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
    (aset reply "type" (fn [_] reply))
    (aset reply "send" (fn [body]
                         (swap! response assoc
                                :body (js->clj body :keywordize-keys true))
                         reply))
    (aset reply "sent" false)
    reply))

(def ^:private config
  {:session-project-name "knoxx-session"
   :publication-content-root "/translated"})

(def ^:private context
  {:org-id "org-authenticated"
   :user-id "user-authenticated"
   :user-email "reviewer@example.test"
   :membership-id "membership-authenticated"})

(defn- handlers
  [permissions]
  {:with-request-context! (fn [_ _ _ operation]
                            (operation context))
   :ensure-permission! (fn [ctx permission]
                         (swap! permissions conj [ctx permission]))})

(defn- review-route
  [routes url]
  (some #(when (and (= "POST" (aget % "method"))
                    (= url (aget % "url")))
           %)
        routes))

(defn- ^:async invoke-review!
  [url operation-key body facade-result]
  (let [routes (atom [])
        response (atom {})
        permissions (atom [])
        calls (atom [])
        app (route-capture routes)]
    (adapter/register-translation-review-routes!
     app {} config (handlers permissions)
     (assoc {:evidence-store ::evidence-store
             :split-store ::split-store}
            operation-key
            (fn [deps scope decoded]
              (swap! calls conj {:deps deps
                                 :scope scope
                                 :request decoded})
              (js/Promise.resolve facade-result))))
    (await ((aget (review-route @routes url) "handler")
            (request body)
            (response-reply response)))
    {:response @response :permissions @permissions :calls @calls}))

(defn- ^:async invoke-split-review!
  [body facade-result]
  (await (invoke-review! "/api/publications/translations/reviews"
                         :record-split-review! body facade-result)))

(defn- ^:async invoke-bulk-review!
  [body facade-result]
  (await (invoke-review! "/api/publications/translations/reviews/bulk"
                         :record-candidate-set-review! body facade-result)))

(t/deftest ^:async registered-route-attributes-scope-and-supports-approve-reject
  (let [approved (await
                  (invoke-split-review!
                   valid-body
                   {:split-review/status :recorded
                    :split-review/current-status :ready
                    :split-review/receipt {:review/status :approved}
                    :split-review/current-translation-receipt
                    {:translation/revision "effective-v1"}}))
        rejected (await
                  (invoke-split-review!
                   {:candidate_set_id "candidate-set-1"
                    :split_id "split-1"
                    :status "rejected"}
                   {:split-review/status :existing
                    :split-review/current-status :not-ready
                    :split-review/receipt {:review/status :rejected}
                    :split-review/current-translation-receipt
                    {:translation/revision "review-state-v2"}}))
        approved-call (first (:calls approved))
        rejected-call (first (:calls rejected))]
    (t/is (= 201 (get-in approved [:response :status])))
    (t/is (= 200 (get-in rejected [:response :status])))
    (t/is (= "ready"
             (get-in approved [:response :body :review_status])))
    (t/is (= "not-ready"
             (get-in rejected [:response :body :review_status])))
    (t/is (= :approved
             (get-in approved-call [:request :split-review/status])))
    (t/is (= "Texto corregido."
             (get-in approved-call
                     [:request :split-review/corrected-text])))
    (t/is (= :rejected
             (get-in rejected-call [:request :split-review/status])))
    (t/is (= {:org-id "org-authenticated"
              :project "knoxx-session"
              :principal {:principal/user-id "user-authenticated"
                          :principal/user-email "reviewer@example.test"
                          :principal/membership-id
                          "membership-authenticated"}}
             (:scope approved-call)))
    (t/is (= [[context adapter/approve-permission]]
             (:permissions approved)))
    (t/is (= ::evidence-store
             (get-in approved-call [:deps :evidence-store])))
    (t/is (= ::split-store
             (get-in approved-call [:deps :split-store])))
    (t/is (fn? (get-in approved-call [:deps :clock])))))

(t/deftest ^:async registered-bulk-route-preserves-evaluation-and-plural-response
  (let [receipts [{:review/split-id "split-1" :review/status :in-review}
                  {:review/split-id "split-2" :review/status :in-review}]
        result (await
                (invoke-bulk-review!
                 valid-bulk-body
                 {:split-review/status :recorded
                  :split-review/current-status :not-ready
                  :split-review/receipts receipts
                  :split-review/current-translation-receipt
                  {:translation/revision "review-state-v2"}}))
        call (first (:calls result))]
    (t/is (= 201 (get-in result [:response :status])))
    (t/is (= [{:split-id "split-1" :status "in-review"}
              {:split-id "split-2" :status "in-review"}]
             (get-in result [:response :body :reviews])))
    (t/is (nil? (get-in result [:response :body :review])))
    (t/is (= :in-review
             (get-in call [:request :split-review/status])))
    (t/is (= "adequate" (get-in call [:request :review/adequacy])))
    (t/is (= "Review every persisted split again."
             (get-in call [:request :review/editor-notes])))
    (t/is (not (contains? (:request call) :split-review/split-id)))
    (t/is (not (contains? (:request call)
                          :split-review/corrected-text)))
    (t/is (= [[context adapter/approve-permission]]
             (:permissions result)))
    (t/is (= ::evidence-store (get-in call [:deps :evidence-store])))
    (t/is (= ::split-store (get-in call [:deps :split-store])))))

(t/deftest response-distinguishes-first-write-from-idempotent-retry
  (t/is (= 201
           (:status
            (adapter/split-review-response-for
             {:split-review/status :recorded}))))
  (t/is (= 200
           (:status
            (adapter/split-review-response-for
             {:split-review/status :existing}))))
  (t/is (= 200
           (:status
            (adapter/bulk-split-review-response-for
             {:split-review/status :existing})))))

(t/deftest ^:async whole-output-approval-requires-the-current-ready-projection
  (let [receipt {:translation/candidate-set-id "candidate-set-1"
                 :translation/revision "effective-v1"}]
    (t/testing "an incomplete split set cannot pass whole-output admission"
      (let [error (try
                    (await
                     (#'adapter/require-current-ready-split!
                      config ::evidence-store receipt
                      {:split-store ::split-store
                       :project-reviewed-output!
                       (fn [_ _]
                         (js/Promise.resolve
                          {:translation/review-status :not-ready
                           :translation/receipt receipt}))}))
                    nil
                    (catch :default err err))]
        (t/is (= 409 (:status (ex-data error))))
        (t/is (= "translation_split_review_incomplete"
                 (:code (ex-data error))))))

    (t/testing "a ready projection must still be the exact selected receipt"
      (let [error (try
                    (await
                     (#'adapter/require-current-ready-split!
                      config ::evidence-store receipt
                      {:split-store ::split-store
                       :project-reviewed-output!
                       (fn [_ _]
                         (js/Promise.resolve
                          {:translation/review-status :ready
                           :translation/receipt
                           (assoc receipt :translation/revision
                                  "effective-v2")}))}))
                    nil
                    (catch :default err err))]
        (t/is (= "translation_candidate_content_moved"
                 (:code (ex-data error))))))

    (t/testing "the exact current ready receipt is admitted"
      (t/is (= receipt
               (await
                (#'adapter/require-current-ready-split!
                 config ::evidence-store receipt
                 {:split-store ::split-store
                  :project-reviewed-output!
                  (fn [_ _]
                    (js/Promise.resolve
                     {:translation/review-status :ready
                      :translation/receipt receipt}))})))))))
