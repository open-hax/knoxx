(ns knoxx.backend.extern.fastify.translation-dispatch-test
  "Decoding one dispatch request body.

  The body has a single optional field, and that combination is what makes the
  decode worth its own tests: every way of getting it wrong turns a scoped
  request into a whole-corpus sweep, which is the most expensive thing this
  route can do."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.extern.fastify.translation-dispatch :as adapter]
            [knoxx.backend.infra.routes.translation-dispatch :as facade]
            [knoxx.backend.infra.stores.translation-evidence-registry :as registry]
            [knoxx.backend.law.openplanner-translation :as openplanner-law]
            [knoxx.backend.law.translation-dispatch :as law]
            [malli.core :as m]))

(defn- request
  "A request whose body is `body`. Only `.body` is read by `decode-request`."
  [body]
  (js-obj "body" (clj->js body)))

(defn- request-with-raw-body
  "A request whose body is passed through unconverted, so a CLJS map survives.

  `fastify/request-body` returns a CLJS map unchanged, which is how a nil value
  can be distinguished from an absent key — `clj->js` would erase that."
  [body]
  (js-obj "body" body))

(deftest an-omitted-document-means-the-whole-corpus
  (testing "an empty body is the ordinary operator sweep"
    (is (= {} (adapter/decode-request (request {}))))))

(deftest a-named-document-is-decoded-to-its-qualified-identity
  (testing "a wire string becomes the keyword the resource index is keyed by"
    (is (= {:document :knoxx.docs/probe}
           (adapter/decode-request (request {:document "knoxx.docs/probe"}))))))

(deftest a-named-publication-selects-one-resource-work-item
  (testing "the inventory's stable identity survives the wire boundary"
    (is (= {:publication :knoxx.docs/probe-es}
           (adapter/decode-request
            (request {:publication "knoxx.docs/probe-es"})))))

  (testing "publication and document are alternatives, never precedence rules"
    (is (thrown? js/Error
                 (adapter/decode-request
                  (request {:document "knoxx.docs/probe"
                            :publication "knoxx.docs/probe-es"}))))))

(deftest a-present-but-empty-document-is-refused
  ;; The regression: dropping a blank document from the decoded map turned
  ;; {"document": ""} into a request to translate everything.
  (testing "an empty string is not a way of saying all documents"
    (is (thrown? js/Error (adapter/decode-request (request {:document ""})))))

  (testing "a blank string is refused too"
    (is (thrown? js/Error (adapter/decode-request (request {:document "   "})))))

  (testing "an explicit nil is refused"
    (is (thrown? js/Error
                 (adapter/decode-request
                  (request-with-raw-body {:document nil}))))))

(deftest a-present-but-empty-publication-is-refused
  (is (thrown? js/Error
               (adapter/decode-request (request {:publication ""}))))
  (is (thrown? js/Error
               (adapter/decode-request (request {:publication "   "}))))
  (is (thrown? js/Error
               (adapter/decode-request
                (request-with-raw-body {:publication nil})))))

(deftest an-unqualified-document-is-refused
  (testing "a bare name is a different document from the qualified one"
    ;; Accepted, it would sweep nothing while reporting success.
    (is (thrown? js/Error (adapter/decode-request (request {:document "probe"}))))))

(deftest an-unqualified-publication-is-refused
  (is (thrown? js/Error
               (adapter/decode-request (request {:publication "probe-es"})))))

(deftest an-unrecognized-field-is-refused
  (testing "a typo must not be reinterpreted as a whole-corpus sweep"
    (is (thrown? js/Error
                 (adapter/decode-request (request {:documnet "knoxx.docs/probe"}))))))

(deftest ^:async a-registered-publication-command-reaches-the-facade-exactly
  (let [routes (atom [])
        app (js-obj "route" (fn [options]
                              (swap! routes conj options)))
        response (atom {})
        reply (js-obj)
        checks (atom [])
        facade-call (atom nil)
        ctx {:org-id "org-1" :membership-id "member-1"}
        handlers
        {:with-request-context! (fn [_runtime _request _reply operation]
                                  (operation ctx))
         :ensure-permission! (fn [actual-ctx permission]
                               (swap! checks conj [actual-ctx permission]))}
        config {:session-project-name "review-stage"
                :openplanner-client-mode "rest"}]
    (aset reply "code" (fn [status]
                          (swap! response assoc :status status)
                          reply))
    (aset reply "type" (fn [_content-type] reply))
    (aset reply "send" (fn [body]
                          (swap! response assoc :body body)
                          reply))
    (aset reply "sent" false)
    (adapter/register-translation-dispatch-routes! app {} config handlers)
    (let [route (first @routes)]
      (with-redefs [registry/current (constantly ::evidence-store)
                    facade/dispatch-translations!
                    (fn [actual-config deps scope selection]
                      (reset! facade-call
                              {:config actual-config
                               :deps deps
                               :scope scope
                               :selection selection})
                      (js/Promise.resolve
                       {:considered 1 :admissible 1 :dispatched []}))]
        (await
         ((aget route "handler")
          (request {:publication "knoxx.publications/probe-es"})
          reply)))

      (testing "the real registered handler decodes and preserves publication identity"
        ;; Regressing to `(:document decoded)` hands nil to the facade here,
        ;; which means the corpus-wide form and makes this assertion fail.
        (is (= {:publication :knoxx.publications/probe-es}
               (:selection @facade-call)))
        (is (= {:org-id "org-1"
                :membership-id "member-1"
                :project "review-stage"}
               (:scope @facade-call))))

      (testing "authorization still precedes dispatch and the response succeeds"
        (is (= [[ctx adapter/dispatch-permission]] @checks))
        (is (= 200 (:status @response)))))))

(deftest dispatched-batches-are-filed-in-the-project-the-review-surfaces-read
  ;; With no project the OpenPlanner batch store defaults to "devel", while the
  ;; segment, document and export routes all filter by :session-project-name
  ;; (default "knoxx-session"). A dispatched translation therefore succeeded and
  ;; then vanished from the review flow, written to a project nothing reads.
  (let [work {:document :knoxx.docs/probe
              :locale :es
              :revision "sha256-aaa111bbb222"
              :replace-stale? false}
        context (fn [project]
                  (cond-> {:dispatch/garden "knoxx.docs/garden"
                           :dispatch/document-wire-id "knoxx.docs/probe"
                           :dispatch/source-locale :en
                           :dispatch/org-id "org-1"
                           :dispatch/membership-id "member-1"}
                    project (assoc :dispatch/project project)))]
    (testing "the configured project reaches the worker request"
      (is (= "knoxx-session"
             (:project (law/worker-request work (context "knoxx-session"))))))

    (testing "the request is still what the worker accepts"
      (is (m/validate openplanner-law/CreateTranslationBatchRequest
                      (law/worker-request work (context "knoxx-session")))))))
