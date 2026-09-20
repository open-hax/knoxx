(ns knoxx.backend.extern.cms-history-tour
  "Disposable loopback browser fixture, excluded from production builds.
   CMS routes are real; identity and garden topology are fixtures."
  (:require ["node:fs" :as fs]
            ["node:path" :as path]
            [clojure.string :as str]
            [knoxx.backend.extern.fastify :as fastify]
            [knoxx.backend.extern.fastify.cms-documents :as transport]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.cms-store :as store]
            [knoxx.backend.infra.http-server :as http]))

(defn- authenticated? [request token]
  (some #{(str "cms_tour=" token)}
        (str/split (or (aget (.-headers request) "cookie") "") #";\s*")))

(defn- route! [app method url handler]
  (fastify/route! app {:method method :url url :handler handler}))

(defn- guarded! [app url token value]
  (route! app "GET" url
          (fn [request reply]
            (fastify/send-json! reply (if (authenticated? request token) 200 403)
                                (if (authenticated? request token) value {:error "Fixture session required"})))))

(defn- static! [app frontend]
  (route! app "GET" "/*"
          (fn [request reply]
            (let [url (first (str/split (.-url request) #"\?"))
                  file (path/resolve frontend (str "." (if (contains? #{"/" "/cms" "/login"} url) "/index.html" url)))
                  extension (path/extname file)]
              (if (and (str/starts-with? file (str frontend path/sep))
                       (not (str/starts-with? url "/api/"))
                       (fs/existsSync file) (.isFile (fs/statSync file))
                       (str/starts-with? (fs/realpathSync file) (str frontend path/sep)))
                (do (.type reply (get {".js" "application/javascript" ".mjs" "application/javascript"
                                      ".css" "text/css" ".html" "text/html" ".svg" "image/svg+xml"}
                                     extension "application/octet-stream"))
                    (.send reply (fs/readFileSync file)))
                (fastify/send-json! reply 404 {:error "Not part of the CMS history fixture"}))))))

(defn- browser-routes! [app token principal proof current auth]
  (transport/register! app nil
                       {:route! route!
                        :with-request-context! (fn [_ request _ operation]
                                                 (operation (when (authenticated? request token) principal)))
                        :ensure-permission! authz/ensure-permission!})
  (route! app "GET" "/_verify/start/:token"
          (fn [request reply]
            (if (= token (:token (fastify/request-params request)))
              (do (.header reply "Set-Cookie" (str "cms_tour=" token "; HttpOnly; SameSite=Strict; Path=/"))
                  (.header reply "Cache-Control" "no-store")
                  (.redirect reply "/cms"))
              (fastify/send-json! reply 403 {:error "Unknown fixture session"}))))
  (guarded! app "/api/auth/context" token auth)
  (guarded! app "/_verify/proof" token proof)
  (guarded! app "/api/cms/publications/documents" token
            {:documents [] :gardens [{:id (:garden_id current) :title "Disposable CMS history fixture" :status "active"}]}))

(defn- ^:async serve! [root frontend checkout head diff-sha]
  (let [root (fs/realpathSync root)
        frontend (fs/realpathSync frontend)
        proof {:checkout checkout :head head :diff-sha diff-sha}
        token (str (random-uuid))
        org (str "tour-" (random-uuid))
        title (str "Clio history tour " org)
        actor "cms-tour-author"
        principal {:org-id org :actor-id actor :permissions ["org.publications.read" "org.publications.manage"]}
        app (http/create-app!)]
    (when-not (empty? (vec (fs/readdirSync root)))
      (throw (ex-info "CMS tour requires its own empty temporary directory" {})))
    (aset js/process.env "KNOXX_PUBLICATION_CONTENT_ROOT" (path/join root "content"))
    (aset js/process.env "KNOXX_GENERATED_CONTRACTS_DIR" (path/join root "contracts"))
    (let [initial (store/save! org actor nil {:title title :content "First fixture revision" :parents []})
          current (store/save! org actor (:doc_id initial)
                               {:title title :content "Second fixture revision" :parents [(:revision initial)]})
          auth {:user {:id actor :email "cms-tour@example.invalid" :displayName "Disposable CMS tour"}
                :org {:id org :name "Disposable CMS history fixture"} :roleSlugs ["admin"]
                :permissions (:permissions principal) :authProvider "test-fixture"}]
      (browser-routes! app token principal proof current auth)
      (static! app frontend)
      (await (http/listen! app "127.0.0.1" 0))
      (let [origin (str "http://127.0.0.1:" (.-port (.address (.-server app))))
            receipt (merge proof {:origin origin :start-url (str origin "/_verify/start/" token)
                                  :title title :document-id (:doc_id current)
                                  :initial-revision (:revision initial) :current-revision (:revision current)})]
        (fs/mkdirSync (path/join root ".ημ"))
        (fs/writeFileSync (path/join root ".ημ" "tour.edn") (pr-str receipt) #js {:mode 384})
        (println "CMS history fixture ready at" origin)))))

(defn ^:async main
  "Serve a freshly built fixture until the owning shell stops it."
  [& args]
  (try
    (when-not (= 5 (count args)) (throw (ex-info "Expected root, frontend, checkout, head and diff digest" {})))
    (await (apply serve! args))
    (catch :default error
      (js/console.error error)
      (js/process.exit 1))))
