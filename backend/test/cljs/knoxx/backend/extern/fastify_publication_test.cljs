(ns knoxx.backend.extern.fastify-publication-test
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [malli.core :as m]
            [knoxx.backend.extern.fastify.publications :as adapter]
            [knoxx.backend.law.error-body :as error-body]
            [knoxx.backend.law.publication :as law]
            ["node:fs" :as node-fs]
            ["node:path" :as path]))

(defn- source-text
  [relative-path]
  (.readFileSync node-fs (.join path (.cwd js/process) relative-path) "utf8"))

(defn- code-only
  "Source with string literals and comments blanked out, so a boundary
   assertion reads the code rather than the prose that documents it. A
   docstring explaining that this layer must not touch `reply` should not
   itself trip a check for `reply`."
  [source]
  (-> source
      (str/replace #"(?s)\"(\\.|[^\"\\])*\"" "\"\"")
      (str/replace #";[^\n]*" "")))

(def adapter-source
  (source-text "src/cljs/knoxx/backend/extern/fastify/publications.cljs"))

(def facade-source
  (source-text "src/cljs/knoxx/backend/infra/routes/publications.cljs"))

;; ── Fakes ──────────────────────────────────────────────────────────────────
;;
;; A reply that records what it was sent, shaped like the subset of Fastify's
;; reply the extern adapter actually calls.

(defn- fake-reply []
  (let [captured (atom {})]
    {:captured captured
     :reply (js-obj "code" (fn [status]
                             (swap! captured assoc :status status)
                             (this-as this this))
                    "type" (fn [_content-type] (this-as this this))
                    "send" (fn [body]
                             (swap! captured assoc :body body)
                             (this-as this this))
                    "sent" false)}))

(defn- fake-request [params]
  (js-obj "params" (clj->js params) "method" "GET"))

(defn- null-prototype-request
  "A request whose `params` has NO prototype, exactly as Fastify's router builds
   it. `(clj->js {...})` produces a normal Object and converts fine, which is why
   every other test here passed against a surface that answered 500 to every
   real request."
  [params]
  (let [obj (js/Object.create nil)]
    (doseq [[k v] params]
      (aset obj (name k) v))
    (js-obj "params" obj "method" "GET")))

(defn- harness
  "Capture registered routes plus every authorization check. `ensure-permission!`
   throws unless the permission is in `granted`."
  [granted]
  (let [routes (atom [])
        checks (atom [])
        app (js-obj "route" (fn [opts]
                              (swap! routes conj (js->clj opts :keywordize-keys true))
                              nil))]
    {:routes routes
     :checks checks
     :app app
     :handlers {:with-request-context! (fn [_runtime _request _reply f] (f {:org-id "acme"}))
                :ensure-permission! (fn [_ctx permission]
                                      (swap! checks conj permission)
                                      (when-not (contains? granted permission)
                                        (throw (ex-info "forbidden"
                                                        {:status 403
                                                         :permission permission}))))}}))

(defn- registered-routes
  "Capture what the adapter registers without a real Fastify instance."
  [config]
  (let [h (harness #{})]
    (adapter/register-publication-routes! (:app h) {} config (:handlers h))
    @(:routes h)))

(defn- route-for [routes url]
  (some #(when (= url (:url %)) %) routes))

;; ── Decoding ───────────────────────────────────────────────────────────────

(deftest decode-request-projects-native-handle-onto-cljs
  (let [decoded (adapter/decode-request (fake-request {:documentId "knoxx.docs/translation-pipeline"}))]
    (is (= "knoxx.docs/translation-pipeline" (get-in decoded [:params :documentId])))
    (is (= "GET" (:method decoded)))
    (testing "the decoded value is CLJS data, not a handle"
      (is (map? decoded))
      (is (map? (:params decoded))))))

(deftest decode-request-handles-fastify-null-prototype-params
  (testing "a params object with no prototype still decodes to a CLJS map"
    (let [decoded (adapter/decode-request
                   (null-prototype-request {:documentId "knoxx.docs/probe"}))]
      (is (map? (:params decoded)))
      (is (= "knoxx.docs/probe" (get-in decoded [:params :documentId])))))
  (testing "and so does an empty one, as a collection route receives"
    (let [decoded (adapter/decode-request (null-prototype-request {}))]
      (is (map? (:params decoded)))
      (is (= {} (:params decoded))))))

;; ── 13 async route behaviour ───────────────────────────────────────────────

(deftest ^:async publication-route-returns-projection
  (let [{:keys [captured reply]} (fake-reply)
        view {:documents [] :gardens []}]
    (await (adapter/send-projection! reply (fn [] view)))
    (is (= 200 (:status @captured)))
    (testing "the body is the projection, serialized"
      (let [body (js->clj (:body @captured) :keywordize-keys true)]
        (is (= {:documents [] :gardens []} body))
        (is (true? (m/validate law/PublicationListView view)))))))

(deftest ^:async qualified-identity-survives-the-json-boundary
  (let [{:keys [captured reply]} (fake-reply)
        view {:documents [{:document {:document/id :knoxx.docs/translation-pipeline
                                      :document/source-locale :en}
                           :publications [{:publication/id :knoxx.docs/tp-es
                                           :publication/garden :gardens/promethean
                                           :publication/state :published
                                           :publication/revision :source/current}]}]
              :gardens [{:garden/id :knoxx.docs/promethean :garden/status :active}]}]
    (await (adapter/send-projection! reply (fn [] view)))
    (let [sent (js->clj (:body @captured) :keywordize-keys true)
          document (get-in sent [:documents 0 :document])
          publication (get-in sent [:documents 0 :publications 0])
          garden (get-in sent [:gardens 0])]
      (testing "JSON keys are unqualified — clj->js renders :document/id as \"id\",
                which is this codebase's documented wire-key convention"
        (is (= #{:id :source-locale} (set (keys document))))
        (is (= #{:id :garden :state :revision} (set (keys publication))))
        (is (= #{:id :status} (set (keys garden)))))
      (testing "but VALUES keep their namespace instead of being flattened"
        (is (= "knoxx.docs/translation-pipeline" (:id document)))
        (is (= "knoxx.docs/tp-es" (:id publication)))
        (is (= "gardens/promethean" (:garden publication)))
        (is (= "knoxx.docs/promethean" (:id garden))))
      (testing "enum values cross as strings, and the revision selector keeps its namespace"
        (is (= "published" (:state publication)))
        (is (= "source/current" (:revision publication)))
        (is (= "active" (:status garden))))
      (testing "and no value carries an EDN leading colon"
        (let [string-values (filter string? (tree-seq coll? seq sent))]
          (is (seq string-values))
          (is (empty? (filter #(str/starts-with? % ":") string-values))
              "a value rendered with (str keyword) would begin with a colon")))
      (testing "distinct namespaces would have collided without the encoder"
        (is (not= (:id document) (:id garden)))))))

(deftest ^:async blocked-references-are-a-409
  (let [{:keys [captured reply]} (fake-reply)]
    (await (adapter/send-projection!
            reply
            (fn [] (throw (ex-info "unresolved publication references"
                                   {:blockers [{:publication/id :knoxx.docs/orphan
                                                :blocker :unresolved-garden}]})))))
    (is (= 409 (:status @captured)))
    (testing "the blocker reaches the client with identity intact"
      (let [sent (js->clj (:body @captured) :keywordize-keys true)
            blocker (get-in sent [:error :blockers 0])]
        (is (= "knoxx.docs/orphan" (:id blocker)))
        (is (= "unresolved-garden" (:blocker blocker)))))))

(deftest ^:async publication-route-awaits-async-handlers
  (let [{:keys [captured reply]} (fake-reply)]
    (await (adapter/send-projection!
            reply
            (fn [] (js/Promise.resolve {:documents [] :gardens []}))))
    (is (= 200 (:status @captured)))
    (is (some? (:body @captured))
        "an unresolved promise would have been serialized instead of its value")))

(deftest ^:async projection-conflict-is-a-409-not-a-500
  (let [{:keys [captured reply]} (fake-reply)]
    (await (adapter/send-projection!
            reply
            (fn [] (throw (ex-info "conflicting publication intents"
                                   {:conflicts [{:publication/key [:a :b :en :source/current]}]})))))
    (is (= 409 (:status @captured)))))

(deftest ^:async unknown-document-is-a-404
  (let [{:keys [captured reply]} (fake-reply)]
    (await (adapter/send-projection!
            reply
            (fn [] (throw (ex-info "unknown document" {:document/id :knoxx.docs/nope})))))
    (is (= 404 (:status @captured)))))

;; ── Registration ───────────────────────────────────────────────────────────

(deftest registers-list-and-document-routes
  (let [routes (registered-routes {})]
    (is (= 2 (count routes)))
    (is (some? (route-for routes "/api/publications/documents")))
    (is (some? (route-for routes "/api/publications/documents/:documentId")))
    (testing "no legacy publishing-backend path is registered"
      (let [legacy-marker (str "open" "planner")]
        (is (not (str/includes? (str/lower-case (pr-str routes)) legacy-marker)))))))

;; ── Source-level boundary laws ─────────────────────────────────────────────

(deftest adapter-uses-native-async-not-promise-chains
  (doseq [[label source] [["adapter" adapter-source] ["facade" facade-source]]]
    (testing label
      (is (not (str/includes? (code-only source) ".then"))
          "native ^:async/await is the prescribed shape, not Promise chaining")
      (is (str/includes? source "await")
          "and the await is real, not merely an absence of .then"))))

(deftest native-request-handles-stay-in-extern
  (let [facade-code (code-only facade-source)]
    (testing "the facade performs no raw Fastify interop"
      (doseq [interop ["aget" "js->clj" "clj->js" "#js" "js-obj"]]
        (is (not (str/includes? facade-code interop))
            (str "facade must not contain " interop))))
    (testing "and never binds a native request or reply handle"
      (is (not (str/includes? facade-code "reply")))
      (is (not (str/includes? facade-code "request"))))
    (testing "while the adapter is the layer that does own them"
      (is (str/includes? (code-only adapter-source) "reply")))))

(deftest facade-contract-has-no-legacy-backend
  (let [legacy-marker (str "open" "planner")]
    (is (not (str/includes? (str/lower-case facade-source) legacy-marker)))))

;; ── Authorization (Codex P1 on #230) ──────────────────────────────────────

(deftest ^:async unauthorized-projection-read-is-refused
  (testing "the projection exposes document titles, garden membership and
            publication paths off the filesystem — an anonymous caller must not
            be able to enumerate it"
    (let [h (harness #{})
          _ (adapter/register-publication-routes! (:app h) {} {} (:handlers h))
          {:keys [captured reply]} (fake-reply)
          route (first @(:routes h))]
      (await ((:handler route) (fake-request {}) reply))
      (is (= ["org.publications.read"] @(:checks h)))
      (let [body (js->clj (:body @captured) :keywordize-keys true)]
        (is (= 403 (get-in body [:error :status])))
        (is (not= 200 (:status @captured)))))))

(deftest ^:async authorized-read-checks-the-permission-once
  (let [h (harness #{"org.publications.read"})
        _ (adapter/register-publication-routes! (:app h) {} {} (:handlers h))
        {:keys [reply]} (fake-reply)
        route (first @(:routes h))]
    (await ((:handler route) (fake-request {}) reply))
    (is (= ["org.publications.read"] @(:checks h)))))

(deftest read-permission-is-publication-scoped
  (is (= "org.publications.read" adapter/read-permission))
  (testing "not borrowed from an unrelated surface"
    (is (not (str/includes? adapter/read-permission "translations")))))

;; ── Request boundary contract ─────────────────────────────────────────────

(deftest decoded-request-is-validated-against-a-named-contract
  (testing "a well-formed request decodes"
    (is (map? (adapter/decode-request (fake-request {:documentId "docs/probe"})))))
  (testing "a changed parameter shape fails at the boundary, not downstream"
    (is (thrown? js/Error
                 (adapter/decode-request (js-obj "params" #js {"documentId" 42}
                                                 "method" "GET"))))))

;; ── authorization status and fail-closed context (#230 review) ─────────────

(deftest ^:async denied-read-is-a-403-not-a-500
  (testing "a denied request must not be reported as an internal failure — that
            tells the caller to retry something that will never succeed"
    (let [h (harness #{})
          _ (adapter/register-publication-routes! (:app h) {} {} (:handlers h))
          {:keys [captured reply]} (fake-reply)
          route (first @(:routes h))]
      (await ((:handler route) (fake-request {}) reply))
      (is (= 403 (:status @captured)))
      (testing "and the status the error carried is the status that was sent"
        (let [body (js->clj (:body @captured) :keywordize-keys true)]
          (is (= 403 (get-in body [:error :status]))))))))

(deftest ^:async a-nil-request-context-fails-closed
  (testing "with-request-context! hands down a nil context when the policy
            database is disabled; that must refuse rather than read as
            permission, or the projection is world-readable"
    (let [checks (atom [])
          routes (atom [])
          app (js-obj "route" (fn [opts]
                                (swap! routes conj (js->clj opts :keywordize-keys true))
                                nil))
          ;; Mirrors the real ensure-permission!: a nil context carries no
          ;; permissions and no system-admin role, so it is denied.
          handlers {:with-request-context! (fn [_runtime _request _reply f] (f nil))
                    :ensure-permission! (fn [ctx permission]
                                          (swap! checks conj [ctx permission])
                                          (when-not (map? ctx)
                                            (throw (ex-info "forbidden" {:status 403}))))}
          {:keys [captured reply]} (fake-reply)]
      (adapter/register-publication-routes! app {} {} handlers)
      (await ((:handler (first @routes)) (fake-request {}) reply))
      (testing "the check ran despite the absent context"
        (is (= [[nil "org.publications.read"]] @checks)))
      (is (= 403 (:status @captured)))
      (is (not= 200 (:status @captured))))))

;; ── one error body for the whole surface (author decision on #230) ─────────

(deftest ^:async the-error-body-is-the-shared-law-shape
  (testing "three adapters hand-rolled this map and two disagreed on which key
            held which value; it is now built through one law"
    (let [{:keys [captured reply]} (fake-reply)]
      (await (adapter/send-projection!
              reply
              (fn [] (throw (ex-info "unresolved publication references"
                                     {:blockers [{:blocker :unresolved-garden}]})))))
      (let [body (js->clj (:body @captured) :keywordize-keys true)]
        (testing ":detail is the message and :error is the evidence"
          (is (string? (:detail body)))
          (is (str/includes? (:detail body) "unresolved publication references"))
          (is (map? (:error body))))
        (testing "and nothing else is sent"
          (is (= #{:detail :error} (set (keys body)))))))))

(deftest the-error-body-law-refuses-a-hand-rolled-shape
  (testing "a swapped or extra key fails its own contract rather than shipping"
    (doseq [bad [{:error "boom" :detail {:a 1}}
                 {:detail "boom" :error {:a 1} :extra true}
                 {:error {:a 1}}]]
      (is (false? (m/validate error-body/ErrorBody bad))
          (str (pr-str bad) " must not validate"))))
  (testing "while the built body does"
    (is (true? (m/validate error-body/ErrorBody
                           (error-body/error-body (ex-info "boom" {:a 1}) 409))))
    (testing "including an error carrying no data at all"
      (is (true? (m/validate error-body/ErrorBody
                             (error-body/error-body (js/Error. "plain") 404)))))
    (testing "and the opaque body it builds for an unclassified status"
      (is (true? (m/validate error-body/ErrorBody
                             (error-body/error-body (ex-info "boom" {:a 1}) 500)))))))

(deftest the-status-decides-whether-a-body-may-describe-itself
  (testing "4xx is the boundary saying it understood the failure, so the
            evidence is the point and travels"
    (doseq [status [400 403 404 409 422 499]]
      (is (true? (error-body/classified? status)) (str status " must be describable"))))
  (testing "anything else fails to the safe side — 500, a nil status from an
            adapter that could not classify at all, and a 5xx a future adapter
            invents"
    (doseq [status [500 502 503 nil "409" :409 399]]
      (is (false? (error-body/classified? status))
          (str (pr-str status) " must not be describable")))))

(deftest an-unclassified-failure-publishes-neither-half
  (let [err (ex-info "ENOENT: /srv/knoxx/contracts/secret.edn"
                     {:path "/srv/knoxx/contracts/secret.edn"
                      :mongo-uri "mongodb://user:pw@host"})]
    (testing "the ex-data is withheld — at a 500 the boundary does not know what
              it is holding, and it routinely holds a path or a connection string"
      (is (nil? (:error (error-body/error-body err 500)))))
    (testing "and so is the message, which names the same path the data did"
      (let [detail (:detail (error-body/error-body err 500))]
        (is (= error-body/opaque-detail detail))
        (is (not (str/includes? detail "secret.edn")))))
    (testing "the same error at a status the boundary chose deliberately keeps both"
      (let [body (error-body/error-body err 409)]
        (is (str/includes? (:detail body) "secret.edn"))
        (is (map? (:error body)))))))

(deftest ^:async an-unexpected-throw-reaches-the-caller-as-an-opaque-500
  (testing "send-projection! is the path an unrecognized error actually takes;
            the redaction has to hold there, not only in the law"
    (let [{:keys [captured reply]} (fake-reply)]
      (await (adapter/send-projection!
              reply
              (fn [] (throw (ex-info "ENOENT: /srv/knoxx/contracts/secret.edn"
                                     {:path "/srv/knoxx/contracts/secret.edn"})))))
      (is (= 500 (:status @captured)))
      (let [body (js->clj (:body @captured) :keywordize-keys true)]
        (is (= {:detail error-body/opaque-detail} body))
        (is (not (str/includes? (pr-str body) "secret.edn")))))))

