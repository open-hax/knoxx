(ns knoxx.backend.extern.fastify-translation-config-test
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [knoxx.backend.extern.fastify.translation-config :as adapter]
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
  (is (= "org.translations.manage" adapter/write-permission))
  (is (not= adapter/read-permission adapter/write-permission)
      "reading the pipeline config must not imply authority to change it"))

(deftest ^:async unauthorized-patch-is-refused-before-any-work
  (let [h (harness #{"org.translations.read"})
        routes (register! h)
        patch (route-for routes "PATCH")]
    (await ((:handler patch) (js-obj "body" (clj->js {:model "glm-5"}) "method" "PATCH")
                             (js-obj)))
    (testing "the write permission was checked"
      (is (= ["org.translations.manage"] @(:checks h))))
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
