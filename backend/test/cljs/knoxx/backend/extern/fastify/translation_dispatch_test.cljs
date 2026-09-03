(ns knoxx.backend.extern.fastify.translation-dispatch-test
  "Decoding one dispatch request body.

  The body has a single optional field, and that combination is what makes the
  decode worth its own tests: every way of getting it wrong turns a scoped
  request into a whole-corpus sweep, which is the most expensive thing this
  route can do."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.extern.fastify.translation-dispatch :as adapter]
            [knoxx.backend.infra.translation-evidence-store :as evidence-store]
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

(deftest ^:async default-rest-event-projection-is-rejected-before-dispatch
  (let [dispatched? (atom false)
        config {:openplanner-base-url "http://openplanner.test"
                :openplanner-api-key "test-key"
                :openplanner-client-mode "rest"}]
    (try
      (await
       (adapter/dispatch-selection-for-scope!
        config
        {:org-id "org-1" :membership-id "member-1"}
        {}
        {:evidence-store ::evidence-store
         :split-store ::split-store
         :dispatch-translations!
         (fn [& _args]
           (reset! dispatched? true)
           (js/Promise.resolve {:ok true}))}))
      (is false "REST translation dispatch must fail without projection repair")
      (catch :default err
        (is (= 503 (:status (ex-data err))))
        (is (= "openplanner_event_projection_repair_unsupported"
               (:code (ex-data err))))))
    (is (false? @dispatched?))))

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
        candidate-event-emitter (fn [_] (js/Promise.resolve {:ok true}))
        config {:session-project-name "review-stage"
                :publication-content-root "/translation-content"
                :openplanner-client-mode "rest"}
        dependencies
        {:evidence-store ::evidence-store
         :split-store ::split-store
         :client ::client
         :observe-source-revision (constantly (js/Promise.resolve nil))
         :emit! (constantly (js/Promise.resolve nil))
         :emit-candidate-events! candidate-event-emitter
         :resolve-agent-contract
         (fn [_config _agent-id]
           {:model "gemma4:31b"
            :thinking-level :medium
            :system-prompt "Translate admitted splits."
            :tool-ids ["save_translation"]})
         :dispatch-translations!
         (fn [actual-config deps scope selection]
           (reset! facade-call
                   {:config actual-config
                    :deps deps
                    :scope scope
                    :selection selection})
           (js/Promise.resolve
            {:considered 1 :admissible 1 :dispatched []}))}]
    (aset reply "code" (fn [status]
                          (swap! response assoc :status status)
                          reply))
    (aset reply "type" (fn [_content-type] reply))
    (aset reply "send" (fn [body]
                          (swap! response assoc :body body)
                          reply))
    (aset reply "sent" false)
    (adapter/register-translation-dispatch-routes!
     app {} config handlers dependencies)
    (let [route (first @routes)]
      (await
       ((aget route "handler")
        (request {:publication "knoxx.publications/probe-es"})
        reply))

      (testing "the real registered handler decodes and preserves publication identity"
        ;; Regressing to `(:document decoded)` hands nil to the facade here,
        ;; which means the corpus-wide form and makes this assertion fail.
        (is (= {:publication :knoxx.publications/probe-es}
               (:selection @facade-call)))
        (is (= {:org-id "org-1"
                :membership-id "member-1"
                :project "review-stage"}
               (:scope @facade-call)))
        (is (= ::split-store (get-in @facade-call [:deps :split-store])))
        (is (= "/translation-content"
               (get-in @facade-call [:deps :content-root])))
        (is (identical? candidate-event-emitter
                        (get-in @facade-call [:deps :emit-candidate-events!]))))

      (testing "authorization still precedes dispatch and the response succeeds"
        (is (= [[ctx adapter/dispatch-permission]] @checks))
        (is (= 200 (:status @response)))))))

(deftest ^:async a-registered-publication-command-narrows-the-real-facade
  (let [document {:document/id :knoxx.docs/probe
                  :document/title "Probe"
                  :document/source-locale :en
                  :document/source {:path "docs/probe.md"}}
        garden (fn [id]
                 {:garden/id id
                  :garden/title (name id)
                  :garden/status :active
                  :garden/locales [:en :es]})
        intent (fn [publication garden-id]
                 {:publication/id publication
                  :publication/document :knoxx.docs/probe
                  :publication/garden garden-id
                  :publication/locale :es
                  :publication/revision :source/current
                  :publication/state :published
                  :publication/path (str "/" (name publication))
                  :translation/review :required})
        publication-a :knoxx.publications/probe-es-a
        publication-b :knoxx.publications/probe-es-b
        garden-a :knoxx.gardens/a
        garden-b :knoxx.gardens/b
        resource-record (fn [kind definition]
                          {:ok? true
                           :resource/kind kind
                           :resource/file-path
                           (str "/contracts/" (name kind) ".edn")
                           :resource/definition definition})
        records [(resource-record :document document)
                 (resource-record :garden (garden garden-a))
                 (resource-record :garden (garden garden-b))
                 (resource-record :publication (intent publication-a garden-a))
                 (resource-record :publication (intent publication-b garden-b))]
        routes (atom [])
        app (js-obj "route" (fn [options] (swap! routes conj options)))
        response (atom {})
        reply (js-obj)
        revision-input (atom nil)
        dispatched-intents (atom nil)
        ctx {:org-id "org-1" :membership-id "member-1"}
        evidence (evidence-store/memory-store)
        handlers
        {:with-request-context! (fn [_runtime _request _reply operation]
                                  (operation ctx))
         :ensure-permission! (fn [_ctx _permission] nil)}
        config {:session-project-name "review-stage"
                :translation-runner "agent"
                :openplanner-client-mode "rest"}
        dependencies
        {:evidence-store evidence
         :split-store ::split-store
         :client ::client
         :emit-candidate-events! (fn [_completion]
                                   (js/Promise.resolve {:ok true}))
         :observe-source-revision (constantly (js/Promise.resolve nil))
         :emit! (constantly (js/Promise.resolve nil))
         :resolve-agent-contract
         (fn [_config _agent-id]
           {:model "gemma4:31b"
            :thinking-level :medium
            :system-prompt "Translate admitted splits."
            :tool-ids ["save_translation"]})
         :resource-records! (fn [_config] (js/Promise.resolve records))
         :source-revisions!
         (fn [_config documents _roots]
           (reset! revision-input documents)
           (js/Promise.resolve
            {:knoxx.docs/probe "sha256-aaa111bbb222"}))
         :ensure-contract-receipts! (fn [& _] (js/Promise.resolve []))
         :dispatch-agent-intents!
         (fn [_deps _index intents _facts _scope _roots]
           (reset! dispatched-intents intents)
           (js/Promise.resolve
            (mapv (fn [selected]
                    {:publication/id (:publication/id selected)
                     :dispatch/outcome :dispatch/accepted})
                  intents)))}]
    (aset reply "code" (fn [status]
                          (swap! response assoc :status status)
                          reply))
    (aset reply "type" (fn [_content-type] reply))
    (aset reply "send" (fn [body]
                          (swap! response assoc :body
                                 (js->clj body :keywordize-keys true))
                          reply))
    (aset reply "sent" false)
    (adapter/register-translation-dispatch-routes!
     app {} config handlers dependencies)
    (let [route (first @routes)]
      (await
       ((aget route "handler")
        (request {:publication "knoxx.publications/probe-es-b"})
        reply))

      (testing "the transport selector narrows before revision derivation"
        (is (= [document] @revision-input))
        (is (= [publication-b]
               (mapv :publication/id @dispatched-intents)))
        (is (not-any? #(= publication-a (:publication/id %))
                      @dispatched-intents)))

      (testing "the real facade reports one considered and dispatched relation"
        (is (= 200 (:status @response)))
        (is (= 1 (get-in @response [:body :considered])))
        (is (= 1 (get-in @response [:body :admissible])))
        (is (= ["knoxx.publications/probe-es-b"]
               (mapv :id
                     (get-in @response [:body :dispatched]))))))))

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
