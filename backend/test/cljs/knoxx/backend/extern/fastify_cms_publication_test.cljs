(ns knoxx.backend.extern.fastify-cms-publication-test
  "Status and body classification for the CMS publication adapter.

  This adapter had no test at all, which is how a 403 could be reported as a 500
  without anything noticing. Every case goes through the public `respond!`,
  because that is where the failing operation actually lands: `guarded!` runs
  inside it, so an authorization denial is caught here rather than escaping to
  Fastify."
  (:require ["fastify" :default fastify-app]
            [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [knoxx.backend.extern.fastify :as fastify]
            [knoxx.backend.extern.fastify.cms-publication :as adapter]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.publication-reconciler :as reconciler]
            [knoxx.backend.infra.publication-target-memory :as memory]
            [knoxx.backend.infra.routes.publication-reconcile :as reconcile]
            [knoxx.backend.infra.routes.publications :as publications]
            [knoxx.backend.law.error-body :as error-body]))

(defn- capturing
  "Handlers whose `json-response!` records what the adapter decided."
  [sent]
  {:json-response! (fn [_reply status body]
                     (reset! sent {:status status :body body})
                     nil)})

(defn- ^:async status-for
  "The status this adapter assigns to an operation that throws `err`."
  [err]
  (let [sent (atom nil)]
    (await (adapter/respond! (capturing sent) #js {} (fn [] (throw err))))
    @sent))

;; ── A carried status wins ──────────────────────────────────────────────────

(deftest ^:async a-denied-request-is-a-403-not-a-500
  (testing "ensure-permission! throws http-error with :status in ex-data, and
            guarded! runs inside respond! — so the denial is classified here.
            Without reading the carried status it fell through to 500, telling
            the caller to retry a request that will never succeed"
    (let [sent (await (status-for (ex-info "forbidden" {:status 403
                                                       :permission "org.publications.manage"})))]
      (is (= 403 (:status sent))))))

(deftest ^:async a-status-on-the-js-error-object-is-read-too
  (testing "the real http-error carries statusCode as a JS property; a fake
            permission check in a test throws plain ex-info. Both must work"
    (let [err (js/Error. "forbidden")]
      (aset err "statusCode" 403)
      (is (= 403 (:status (await (status-for err))))))))

(deftest ^:async a-carried-status-beats-the-shape-guesses
  (testing "a denial whose ex-data also happens to name a publication must not
            be reported as 404 — the resource exists, the caller may not have it"
    (let [sent (await (status-for (ex-info "forbidden"
                                           {:status 403
                                            :publication/id :knoxx.docs/probe-es})))]
      (is (= 403 (:status sent))))))

;; ── Shape-based classification, when nothing is carried ────────────────────

(deftest ^:async resource-shaped-failures-keep-their-statuses
  (testing "an unknown publication is a 404"
    (is (= 404 (:status (await (status-for
                                (ex-info "unknown publication"
                                         {:publication/id :knoxx.docs/probe-es})))))))
  (testing "an unknown document is a 404"
    (is (= 404 (:status (await (status-for
                                (ex-info "unknown document"
                                         {:document/id :knoxx.docs/probe})))))))
  (testing "a conflict is a 409 — a retry will not help, the data contradicts"
    (is (= 409 (:status (await (status-for
                                (ex-info "conflicting entries"
                                         {:conflicts [{:matches 2}]})))))))
  (testing "blockers are a 409 for the same reason"
    (is (= 409 (:status (await (status-for
                                (ex-info "unresolved references"
                                         {:blockers [:unresolved-document]})))))))
  (testing "a rejected patch body is a 422"
    (is (= 422 (:status (await (status-for
                                (ex-info "invalid patch" {:errors {:state ["unknown"]}})))))))
  (testing "and anything unrecognized stays a 500 rather than guessing"
    (is (= 500 (:status (await (status-for (ex-info "boom" {}))))))))

;; ── Body shape ─────────────────────────────────────────────────────────────

(deftest ^:async the-error-body-is-the-one-contract-shape
  (testing "knoxx.backend.law.error-body puts the message in :detail and the
            ex-data in :error, on every route of this surface"
    ;; The body reaching json-response! is CLJS data — clj->js happens inside
    ;; send-json!, past this seam.
    (let [sent (await (status-for (ex-info "conflicting entries"
                                           {:conflicts [{:matches 2}]})))
          body (:body sent)]
      (is (= "conflicting entries" (:detail body)))
      (is (some? (:error body))))))

(deftest ^:async an-unclassified-failure-sends-an-opaque-body
  (testing "the redaction has to hold on THIS adapter too, not only in the law —
            a 500 is the boundary saying it does not know what it is holding, and
            here it routinely holds a resource file path"
    (let [sent (await (status-for
                       (ex-info "ENOENT: /srv/knoxx/contracts/secret.edn"
                                {:resource/file-path "/srv/knoxx/contracts/secret.edn"})))]
      (is (= 500 (:status sent)))
      (is (= {:detail error-body/opaque-detail} (:body sent)))
      (is (not (str/includes? (pr-str (:body sent)) "secret.edn"))))))

(deftest ^:async a-classified-failure-still-carries-its-evidence
  (testing "the evidence is the whole point of a 409 here, so redaction must not
            reach it"
    (let [sent (await (status-for (ex-info "conflicting entries"
                                           {:conflicts [{:matches 2}]})))]
      (is (= 409 (:status sent)))
      (is (= "conflicting entries" (:detail (:body sent))))
      (is (some? (:error (:body sent)))))))

(deftest ^:async a-successful-operation-is-a-200
  (let [sent (atom nil)]
    (await (adapter/respond! (capturing sent) #js {}
                             (fn [] {:publication/id :knoxx.docs/probe-es})))
    (is (= 200 (:status @sent)))
    (testing "and identity crosses the wire qualified, not collapsed onto its name
              — clj->js renders a keyword with `name`, which would have sent
              \"probe-es\" and merged every namespace onto one wire id"
      (is (= "knoxx.docs/probe-es" (:publication/id (:body @sent)))))))

(deftest cms-routes-project-organization-scope-only-from-the-auth-context
  (is (= {:org-id "org-1"}
         (#'adapter/request-scope {:org-id "org-1"})))
  (is (= {:org-id nil}
         (#'adapter/request-scope nil))))

;; Exercise the registered HTTP callbacks, not the facade's explicit-evidence
;; overload: supplying receipts to that overload would skip the missing wire.
;; Fastify inject owns disposable native requests without opening a listener.

(def ^:private fixture-revision "cms-route-concrete-revision")
(def ^:private fixture-target :cms-route-test/memory)
(def ^:private fixture-principal
  {:org-id "cms-route-org"
   :permissions ["org.publications.read" "org.publications.manage"]})

(defn- fixture-index
  []
  {:documents {:cms-route-test/probe
               {:document/id :cms-route-test/probe
                :document/title "CMS route probe"
                :document/source-locale :en
                :document/source {:path "docs/probe.md"}
                ;; Shared intent lets the isolation test distinguish hidden
                ;; evidence from an entirely hidden document.
                :document/visibility :public}}
   :gardens {:cms-route-test/garden
             {:garden/id :cms-route-test/garden
              :garden/title "CMS route garden"
              :garden/status :active
              :garden/locales [:en :es]}}
   :publications [{:publication/id :cms-route-test/probe-es
                   :publication/document :cms-route-test/probe
                   :publication/garden :cms-route-test/garden
                   :publication/target fixture-target
                   :publication/locale :es
                   :publication/revision :source/current
                   :publication/state :published
                   :publication/path "/cms-route-probe"
                   :translation/review :required}]})

(defn- ^:async inject!
  [app method url payload]
  (let [response (await (.inject app (clj->js (cond-> {:method method :url url}
                                              payload (assoc :payload payload)))))]
    {:status (.-statusCode response)
     :body (js->clj (.json response) :keywordize-keys true)}))

(defn- cms-get-paths
  []
  [["/api/cms/publications/documents" [:documents 0 :publications 0]]
   ["/api/cms/publications/documents/cms-route-test%2Fprobe" [:publications 0]]])

(defn- fixture-config
  [target load-index! artifact-calls*]
  {:session-project-name "cms-route-project"
   :publication/reconciliation
   {:targets [{:publication-target/id fixture-target
               :publication-target/kind :publication-target/memory
               :publication-target/config {}
               :publication-target/enabled? true}]
    :target-factories {:publication-target/memory (constantly (:target target))}
    :load-index! load-index!
    :evidence-facts (fn [_intent]
                      {:current-source-revision (constantly fixture-revision)
                       :translated-revision? (constantly true)
                       :approved? (constantly true)
                       :source-revision-superseded? (constantly false)})
    :artifact-source (fn [_intent revision]
                       (swap! artifact-calls* inc)
                       {:artifact/content "<p>CMS route artifact</p>"
                        :artifact/media-type "text/html"
                        :artifact/encoding "utf-8"
                        :artifact/locale :es
                        :artifact/revision revision})
    :locale-admissible? (constantly true)}})

(defn- fixture-app!
  [apps* context* config]
  (let [app (fastify-app #js {:logger false})
        handlers {:route! (fn [app method url handler]
                            (fastify/route! app {:method method :url url :handler handler}))
                  :json-response! fastify/send-json!
                  :with-request-context! (fn [_runtime _request _reply operation]
                                           (operation @context*))
                  :ensure-permission! authz/ensure-permission!}]
    (swap! apps* conj app)
    (adapter/register-cms-publication-routes! app nil config handlers)
    (reconcile/register-publication-reconcile-routes! app nil config handlers)
    app))

(defn- instrument-journal
  [make-journal receipt-reads* receipt-read-error*]
  (let [journal (make-journal)]
    (assoc journal :receipts
           (fn []
             (swap! receipt-reads* inc)
             (when @receipt-read-error*
               (throw (ex-info "fixture receipt reader unavailable" {})))
             ((:receipts journal))))))

(defn- ^:async with-publication-routes!
  "Run actual CMS/reconcile callbacks with fixture identity and desired input.
   Registry, planner, receipt emission, scope caches and wire projection are
   real. Every app and runtime cache is released even when an assertion fails."
  [exercise!]
  (let [apps* (atom [])
        context* (atom fixture-principal)
        index* (atom (fixture-index))
        index-loads* (atom 0)
        receipt-reads* (atom 0)
        receipt-read-error* (atom false)
        artifact-calls* (atom 0)
        target (memory/memory-target {:id fixture-target})
        load-index! (fn [] (swap! index-loads* inc) @index*)
        make-journal reconciler/make-receipt-journal
        config (fixture-config target load-index! artifact-calls*)
        new-app! (partial fixture-app! apps* context*)]
    (reconcile/reset-cached-runtimes!)
    (try
      (with-redefs [publications/publication-index! (fn [_config] (load-index!))
                    reconciler/make-receipt-journal
                    (fn [] (instrument-journal make-journal receipt-reads* receipt-read-error*))]
        (await (exercise! {:app (new-app! config) :new-app! new-app! :config config
                           :context* context* :index* index* :index-loads* index-loads*
                           :receipt-reads* receipt-reads* :receipt-read-error* receipt-read-error*
                           :artifact-calls* artifact-calls* :target target})))
      (finally
        (doseq [app @apps*] (await (.close app)))
        (reconcile/reset-cached-runtimes!)))))

(defn- ^:async materialize-through-route!
  [app]
  (let [response (await (inject! app "POST" "/api/publications/reconcile"
                                {:publicationId "cms-route-test/probe-es"
                                 :triggerId "cms-route-test/materialize"}))]
    (is (= 200 (:status response)))
    ;; This existing HTTP boundary encodes keyword values, while clj->js
    ;; produces unqualified wire keys. Domain receipts stay unchanged.
    (is (= "publication/materialized" (get-in response [:body :type])))
    (is (= "cms-route-test/probe-es" (get-in response [:body :publication])))
    (is (= fixture-revision (get-in response [:body :revision])))
    response))

(deftest ^:async registered-cms-gets-show-the-receipt-emitted-by-the-reconcile-route
  (await
   (with-publication-routes!
    (^:async fn [{:keys [app new-app! config context* index* artifact-calls* target]}]
      (let [desired-before (pr-str @index*)]
        (doseq [[url row-path] (cms-get-paths)]
          (let [response (await (inject! app "GET" url nil))]
            (is (= 200 (:status response)))
            (is (= "published" (get-in response (into [:body] (conj row-path :desired)))))
            (is (nil? (get-in response (into [:body] (conj row-path :observed)))))))
        (let [emitted (:body (await (materialize-through-route! app)))
              journal (await (inject! app "GET" "/api/publications/receipts" nil))]
          (is (= 200 (:status journal)))
          (is (= [emitted] (get-in journal [:body :receipts])))
          (doseq [[url row-path] (cms-get-paths)]
            (let [response (await (inject! app "GET" url nil))]
              (is (= 200 (:status response)))
              (is (= "published" (get-in response (into [:body] (conj row-path :desired)))))
              (is (= fixture-revision
                     (get-in response (into [:body] (conj row-path :observed))))
                  (str url " must show the revision in the real runtime journal")))))
        (testing "same public intent, separate organization and trusted project evidence"
          (doseq [[scoped-app principal]
                  [[app (assoc fixture-principal :org-id "another-org")]
                   [(new-app! (assoc config :session-project-name "another-project"))
                    fixture-principal]]]
            (reset! context* principal)
            (let [journal (await (inject! scoped-app "GET" "/api/publications/receipts" nil))]
              (is (= 200 (:status journal)))
              (is (= [] (get-in journal [:body :receipts]))))
            (doseq [[url row-path] (cms-get-paths)]
              (let [response (await (inject! scoped-app "GET" url nil))]
                (is (= 200 (:status response)))
                (is (= "published" (get-in response (into [:body] (conj row-path :desired)))))
                (is (nil? (get-in response (into [:body] (conj row-path :observed)))))))))
        (is (= desired-before (pr-str @index*)) "GET never changes desired resources")
        (is (= 1 @artifact-calls*) "GET never produces another artifact")
        (is (= 1 (memory/materialization-count target)) "GET never publishes")
        (reset! context* fixture-principal)
        (swap! index* assoc-in [:publications 0 :publication/state] :withheld)
        (let [removed (await (inject! app "POST" "/api/publications/reconcile"
                                     {:publicationId "cms-route-test/probe-es"
                                      :triggerId "cms-route-test/remove"}))]
          (is (= 200 (:status removed)))
          (is (= "publication/removed" (get-in removed [:body :type]))))
        (doseq [[url row-path] (cms-get-paths)]
          (let [response (await (inject! app "GET" url nil))]
            (is (= 200 (:status response)))
            (is (= "withheld" (get-in response (into [:body] (conj row-path :desired)))))
            (is (nil? (get-in response (into [:body] (conj row-path :observed))))))))))))

(deftest ^:async registered-cms-gets-authorize-before-resource-or-receipt-reads
  (await
   (with-publication-routes!
    (^:async fn [{:keys [app context* index-loads* receipt-reads* artifact-calls* target]}]
      (await (materialize-through-route! app))
      (reset! context* nil)
      (let [loads-before @index-loads* reads-before @receipt-reads*]
        (doseq [[url _row-path] (cms-get-paths)]
          (is (= 403 (:status (await (inject! app "GET" url nil))))))
        (is (= 403 (:status (await (inject! app "GET" "/api/publications/receipts" nil)))))
        (is (= loads-before @index-loads*))
        (is (= reads-before @receipt-reads*))
        (is (= 1 @artifact-calls*))
        (is (= 1 (memory/materialization-count target))))))))

(deftest ^:async registered-cms-gets-refuse-a-failed-runtime-receipt-reader
  (await
   (with-publication-routes!
    (^:async fn [{:keys [app receipt-read-error* artifact-calls* target]}]
      (await (materialize-through-route! app))
      (reset! receipt-read-error* true)
      (is (= 500 (:status (await (inject! app "GET" "/api/publications/receipts" nil)))))
      (doseq [[url _row-path] (cms-get-paths)]
        (let [response (await (inject! app "GET" url nil))]
          (is (= 500 (:status response))
              (str url " cannot report successful empty evidence when its reader failed"))
          (when (= 500 (:status response))
            (is (= {:detail error-body/opaque-detail} (:body response))))))
      (is (= 1 @artifact-calls*))
      (is (= 1 (memory/materialization-count target)))))))

(deftest ^:async unconfigured-cms-still-renders-desired-intent-and-receipt-get-stays-503
  (await
   (with-publication-routes!
    (^:async fn [{:keys [new-app! config artifact-calls* target]}]
      (let [app (new-app! (dissoc config :publication/reconciliation))
            receipts (await (inject! app "GET" "/api/publications/receipts" nil))]
        (is (= 503 (:status receipts)))
        (is (= {:detail "publication reconciliation is not configured"} (:body receipts)))
        (doseq [[url row-path] (cms-get-paths)]
          (let [response (await (inject! app "GET" url nil))]
            (is (= 200 (:status response)))
            (is (= "published" (get-in response (into [:body] (conj row-path :desired)))))
            (is (nil? (get-in response (into [:body] (conj row-path :observed)))))))
        (is (= 0 @artifact-calls*))
        (is (= 0 (memory/materialization-count target))))))))
