(ns knoxx.backend.extern.fastify-translation-config-test
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [knoxx.backend.domain.translation-config :as domain-config]
            [knoxx.backend.extern.fastify.translation-config :as adapter]
            [knoxx.backend.infra.routes.translation-config :as facade]
            ["node:fs" :as node-fs]
            ["node:path" :as path]))

(defn- source-text
  [relative-path]
  (.readFileSync node-fs (.join path (.cwd js/process) relative-path) "utf8"))

;; ── Harness ────────────────────────────────────────────────────────────────

(defn- harness
  "Capture registered routes plus every authorization check, with a
   `ensure-permission!` that throws for permissions not in `granted`."
  [granted]
  (let [routes (atom [])
        checks (atom [])
        responses (atom [])
        app (js-obj)]
    {:routes routes
     :checks checks
     :responses responses
     :app app
     :handlers
     {:route! (fn [_app method url handler]
                (swap! routes conj {:method method :url url :handler handler}))
      :json-response! (fn [_reply status body]
                        (swap! responses conj {:status status :body body}))
      :with-request-context! (fn [_runtime _request _reply f] (f {:org-id "acme"}))
      :ensure-permission! (fn [_ctx permission]
                            (swap! checks conj permission)
                            (when-not (contains? granted permission)
                              (throw (ex-info "forbidden" {:status 403
                                                           :permission permission}))))}}))

(defn- register! [h]
  (adapter/register-translation-config-routes! (:app h) {} {} (:handlers h))
  @(:routes h))

(defn- route-for [routes method]
  (some #(when (= method (:method %)) %) routes))

;; ── Registration ───────────────────────────────────────────────────────────

(deftest registers-knoxx-owned-config-routes
  (let [routes (register! (harness #{}))]
    (is (= 2 (count routes)))
    (doseq [route routes]
      (is (= "/api/translations/config" (:url route))))
    (is (= #{"GET" "PATCH"} (set (map :method routes))))
    (testing "and never the legacy backend path"
      (let [legacy-marker (str "open" "planner")]
        (is (not (str/includes? (str/lower-case (pr-str (map :url routes)))
                                legacy-marker)))))))

;; ── Authorization (the CodeRabbit finding) ────────────────────────────────

(deftest read-and-write-use-distinct-permissions
  (is (= "org.translations.read" adapter/read-permission))
  (is (not= adapter/read-permission adapter/write-permission)
      "reading the pipeline config must not imply authority to change it")
  (testing "the write is platform-scoped, because the resource it rewrites is the
            global default — an org-scoped permission would have let one tenant's
            administrator change the default for every other tenant"
    (is (= "platform.translations.manage" adapter/write-permission))
    (is (str/starts-with? adapter/write-permission "platform."))
    (is (not (str/starts-with? adapter/write-permission "org."))))
  (testing "while the read stays org-scoped — the resolved config is the org's own"
    (is (str/starts-with? adapter/read-permission "org."))))

(deftest ^:async unauthorized-patch-is-refused-before-any-work
  (let [h (harness #{"org.translations.read" "org.translations.manage"})
        routes (register! h)
        patch (route-for routes "PATCH")]
    (await ((:handler patch) (js-obj "body" (clj->js {:model "glm-5"}) "method" "PATCH")
                             (js-obj)))
    (testing "the write permission was checked — and org.translations.manage is
              deliberately NOT enough for it"
      (is (= ["platform.translations.manage"] @(:checks h))))
    (testing "and the request was refused rather than served"
      (let [{:keys [status body]} (first @(:responses h))]
        (is (= 403 (get-in body [:error :status])))
        (is (not= 200 status))))))

(deftest ^:async authorized-read-checks-the-read-permission
  (let [h (harness #{"org.translations.read"})
        routes (register! h)
        get-route (route-for routes "GET")]
    (await ((:handler get-route) (js-obj "method" "GET") (js-obj)))
    (is (= ["org.translations.read"] @(:checks h)))))

(deftest ^:async deployment-model-overlay-is-visible-without-rewriting-policy
  (let [authored {:namespace :knoxx.translation
                  :policy/id :pipeline-default
                  :translation/model "glm-5"
                  :translation/source-locale :en
                  :translation/default-review :required}
        index (domain-config/index-resources
               [authored
                {:model/id "glm-5"}
                {:model/id "gemma4:e2b"}])]
    (with-redefs [facade/config-index!
                  (fn [_config] (js/Promise.resolve index))]
      (let [effective (await
                       (facade/resolved-config!
                        {:agent-model-overrides
                         {"publication_translator" "gemma4:e2b"}}
                        {}))]
        (is (= "gemma4:e2b" (:translation/model effective)))
        (is (= "glm-5"
               (:translation/model (domain-config/resolve-config index {})))
            "the environment overlay must not mutate authored policy")))))

(deftest ^:async deployment-managed-model-refuses-a-misleading-patch
  (let [loaded? (atom false)
        caught (atom nil)]
    (with-redefs [facade/config-records!
                  (fn [_config]
                    (reset! loaded? true)
                    (js/Promise.resolve []))]
      (try
        (await
         (facade/patch-config!
          {:agent-model-overrides
           {"publication_translator" "gemma4:e2b"}}
          {:translation/model "glm-5"}))
        (catch :default error
          (reset! caught error))))
    (is (= 409 (:status (ex-data @caught))))
    (is (= "translation_model_deployment_managed"
           (:code (ex-data @caught))))
    (is (false? @loaded?)
        "a deployment-managed patch must fail before reading or writing policy")))

(deftest ^:async unauthorized-read-is-refused
  (let [h (harness #{})
        routes (register! h)
        get-route (route-for routes "GET")]
    (await ((:handler get-route) (js-obj "method" "GET") (js-obj)))
    (let [{:keys [body]} (first @(:responses h))]
      (is (= 403 (get-in body [:error :status]))))))

;; ── Boundary laws ──────────────────────────────────────────────────────────

(deftest adapter-and-facade-use-native-async
  (doseq [[label relative-path]
          [["adapter" "src/cljs/knoxx/backend/extern/fastify/translation_config.cljs"]
           ["facade" "src/cljs/knoxx/backend/infra/routes/translation_config.cljs"]]]
    (testing label
      (let [source (source-text relative-path)
            code (-> source
                     (str/replace #"(?s)\"(\\.|[^\"\\])*\"" "\"\"")
                     (str/replace #";[^\n]*" ""))]
        (is (not (str/includes? code ".then")))
        (is (str/includes? source "await"))))))

(deftest facade-performs-no-fastify-interop
  (let [code (-> (source-text "src/cljs/knoxx/backend/infra/routes/translation_config.cljs")
                 (str/replace #"(?s)\"(\\.|[^\"\\])*\"" "\"\"")
                 (str/replace #";[^\n]*" ""))]
    (doseq [interop ["aget" "js->clj" "clj->js" "#js" "js-obj" "reply"]]
      (is (not (str/includes? code interop))
          (str "facade must not contain " interop)))))

;; ── Status codes carry the right meaning (Codex P2s on #233) ───────────────

(deftest ^:async denied-request-is-a-403-not-a-500
  (testing "reporting access denial as a server fault tells the caller to retry
            something that can never succeed, and hides it from monitoring"
    (let [h (harness #{})
          routes (register! h)]
      (await ((:handler (route-for routes "GET")) (js-obj "method" "GET") (js-obj)))
      (is (= 403 (:status (first @(:responses h))))))))

(deftest ^:async a-malformed-patch-body-is-a-client-error
  (testing "a body missing model, or carrying a blank or non-string one, is the
            caller's mistake — 500 would blame the server for it"
    (doseq [body [{} {:model ""} {:model 42} {:model "glm-5" :surprise true}]]
      (let [h (harness #{"platform.translations.manage"})
            routes (register! h)]
        (await ((:handler (route-for routes "PATCH"))
                (js-obj "body" (clj->js body) "method" "PATCH")
                (js-obj)))
        (is (= 400 (:status (first @(:responses h))))
            (str "body " (pr-str body) " must be a client error"))))))

(deftest ^:async a-nil-request-context-fails-closed
  (testing "with-request-context! hands down nil when the policy database is
            disabled; skipping the check then would let anyone rewrite the
            authoritative translation model"
    (let [checks (atom [])
          routes (atom [])
          responses (atom [])
          handlers {:route! (fn [_app method url handler]
                              (swap! routes conj {:method method :url url :handler handler}))
                    :json-response! (fn [_reply status body]
                                      (swap! responses conj {:status status :body body}))
                    :with-request-context! (fn [_runtime _request _reply f] (f nil))
                    :ensure-permission! (fn [ctx permission]
                                          (swap! checks conj [ctx permission])
                                          (when-not (map? ctx)
                                            (throw (ex-info "forbidden" {:status 403}))))}]
      (adapter/register-translation-config-routes! (js-obj) {} {} handlers)
      (let [patch (some #(when (= "PATCH" (:method %)) %) @routes)]
        (await ((:handler patch)
                (js-obj "body" (clj->js {:model "glm-5"}) "method" "PATCH")
                (js-obj))))
      (testing "the check ran despite the absent context"
        (is (= [[nil "platform.translations.manage"]] @checks)))
      (is (= 403 (:status (first @responses)))))))
