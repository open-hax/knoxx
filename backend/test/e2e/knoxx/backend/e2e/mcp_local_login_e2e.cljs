(ns knoxx.backend.e2e.mcp-local-login-e2e
  "Verify that MCP authorization can resume after a local password sign-in."
  (:require [clojure.string :as str]
            [cljs.test :refer [deftest is testing]]
            [knoxx.backend.e2e.harness :as harness]
            [knoxx.backend.infra.auth.password :as password]
            [knoxx.backend.infra.db.policy :as policy]
            [knoxx.backend.infra.http-server :as server]
            [knoxx.backend.infra.routes.auth :as auth]
            [knoxx.backend.infra.routes.mcp :as mcp]
            [knoxx.backend.infra.stores.mongo-mcp-oauth :as oauth]
            [knoxx.backend.runtime.state :as runtime-state]
            ["node:crypto" :as crypto]
            ["puppeteer-core" :as puppeteer]))

(def ^:private tour-port (some-> (aget js/process.env "KNOXX_MCP_LOGIN_TOUR_PORT") parse-long))
(def ^:private base-url (str "http://127.0.0.1:" (or tour-port 18882)))
(def ^:private redirect-uri "http://127.0.0.1:39187/callback")
(def ^:private email "local-mcp@knoxx.test")

(defn- header [^js response name]
  (aget (.-headers response) name))

(defn- login!
  [^js app origin path password]
  (.inject app
           #js {:method "POST"
                :url "/api/auth/local/login"
                :headers #js {:content-type "application/x-www-form-urlencoded"
                              :host (.-host (js/URL. base-url))
                              :origin origin}
                :payload (.toString (js/URLSearchParams. #js {:email email
                                                            :password password
                                                            :mcpReturnTo path}))}))

(defn- ^:async browser-tour!
  [^js app authorization-path password]
  (await (.listen app #js {:host "127.0.0.1" :port tour-port}))
  (let [directory (aget js/process.env "KNOXX_SHOT_DIR")
        browser (await (.launch puppeteer #js {:headless true
                                              :executablePath (aget js/process.env "CHROME_BIN")}))]
    (try
      (let [page (await (.newPage browser))]
        (await (.goto page (str base-url authorization-path)))
        (await (.waitForSelector page "input[name=mcpReturnTo]"))
        (await (.screenshot page #js {:path (str directory "/mcp-local-password.png")}))
        (await (.type page "input[name=email]" email))
        (await (.type page "input[name=password]" password))
        (await (js/Promise.all #js [(.waitForNavigation page) (.click page "button[type=submit]")]))
        (is (= (str base-url authorization-path) (.url page)))
        (await (.waitForSelector page "input[name=actor_id][value=e2e_actor]"))
        (is (= "Authorize MCP Client" (await (.title page))))
        (await (.screenshot page #js {:path (str directory "/mcp-actor-consent.png")})))
      (finally (await (.close browser))))))

(defn- ^:async exercise!
  [^js app password]
  (let [register (await (.inject app #js {:method "POST" :url "/api/mcp/oauth/register"
                                          :payload #js {:client_name "local-login regression"
                                                        :redirect_uris #js [redirect-uri]
                                                        :token_endpoint_auth_method "none"}}))
        client-id (aget (.json register) "client_id")
        verifier (.toString (.randomBytes crypto 32) "base64url")
        challenge (.digest (.update (.createHash crypto "sha256") verifier) "base64url")
        parameters (js/URLSearchParams. #js {:client_id client-id
                                             :redirect_uri redirect-uri
                                             :state "same-state"
                                             :code_challenge challenge
                                             :code_challenge_method "S256"})
        authorize-path (str "/api/mcp/oauth/authorize?" parameters)
        challenge-response (await (.inject app authorize-path))
        login-path (js/URL. (header challenge-response "location"))
        form (await (.inject app (str (.-pathname login-path) (.-search login-path))))]
    (is (= 201 (.-statusCode register)))
    (is (= 302 (.-statusCode challenge-response)))
    (is (= base-url (.-origin login-path)))
    (is (= 200 (.-statusCode form)))
    (is (str/includes? (header form "content-type") "text/html"))
    (is (= "no-store" (header form "cache-control")))
    (is (str/includes? (.-body form) "Sign in with your Knoxx account"))
    (is (str/includes? (.-body form) "mcpReturnTo"))
    (is (not (str/includes? (.-body form) "GitHub OAuth not configured")))

    (testing "Failed credentials leave the authorization flow unauthenticated"
      (let [wrong-password (await (login! app base-url authorize-path "wrong password"))]
        (is (= 401 (.-statusCode wrong-password)))
        (is (nil? (header wrong-password "set-cookie")))
        (is (str/includes? (.-body wrong-password) "mcpReturnTo"))))

    (testing "The form can resume only the same-origin MCP authorization endpoint"
      (let [missing (await (.inject app #js {:method "POST" :url "/api/auth/local/login"
                                            :headers #js {:content-type "application/x-www-form-urlencoded"
                                                          :origin "http://elsewhere.test"}
                                            :payload "email=local-mcp%40knoxx.test&password=fixture"}))]
        (is (= 400 (.-statusCode missing)))
        (is (nil? (header missing "set-cookie"))))
      (doseq [path ["//elsewhere.test/path" "https://elsewhere.test/path" "/api/auth/logout"
                    "/api/mcp/oauth/authorize/../logout" "/api/mcp/oauth/authorize#fragment"
                    "/api/mcp/oauth/authorize/confirm"]]
        (let [invalid (await (login! app base-url path password))]
          (is (= 400 (.-statusCode invalid)))
          (is (nil? (header invalid "location")))
          (is (nil? (header invalid "set-cookie")))))
      (let [foreign-origin (await (login! app "http://elsewhere.test" authorize-path password))]
        (is (= 403 (.-statusCode foreign-origin)))
        (is (nil? (header foreign-origin "set-cookie")))))

    (testing "A valid local password creates the actor session before resuming consent"
      (let [success (await (login! app base-url authorize-path password))
            cookie (first (str/split (header success "set-cookie") #";"))
            consent (await (.inject app #js {:url (header success "location")
                                             :headers #js {:cookie cookie}}))]
        (is (= 303 (.-statusCode success)))
        (is (= authorize-path (header success "location")))
        (is (= 200 (.-statusCode consent)))
        (is (str/includes? (.-body consent) "Authorize MCP Client"))
        (is (str/includes? (.-body consent) "e2e_actor"))))
    (when tour-port (await (browser-tour! app authorize-path password)))))

(deftest ^:async mcp-authorize-reuses-local-password-and-returns-to-actor-consent
  (let [environment js/process.env
        settings {"KNOXX_PUBLIC_BASE_URL" base-url
                  "KNOXX_LOCAL_PASSWORD_AUTH_ENABLED" "true"
                  "KNOXX_SESSION_SECRET" (.toString (.randomBytes crypto 32) "hex")
                  "KNOXX_GITHUB_OAUTH_CLIENT_ID" ""
                  "KNOXX_GITHUB_OAUTH_CLIENT_SECRET" ""}
        originals (mapv (fn [[key _]] [key (aget environment key)]) settings)
        password (.randomUUID crypto)
        password-hash (password/hash-password password)
        context (assoc harness/system-admin-context
                       :user-email email
                       :user {:id "e2e-user" :email email}
                       :org {:id "e2e-org" :slug "open-hax"}
                       :membership {:id "e2e-membership" :actor-id "e2e_actor"})
        policy-context (harness/policy-context context)
        sessions (atom {})
        clients (atom {})
        config (harness/e2e-config)
        runtime #js {}
        app (server/create-app!)]
    (try
      (doseq [[key value] settings] (aset environment key value))
      (runtime-state/remember-context! runtime config policy-context)
      (await (server/register-default-plugins! app))
      (auth/register-auth-routes app {:policy-context policy-context :runtime runtime})
      (mcp/register-mcp-http-routes! app runtime config)
      (with-redefs [policy/local-password-auth-record-for-context!
                    (fn [_ queried-email]
                      (js/Promise.resolve
                       (when (= email queried-email)
                         {:email email :membership-id "e2e-membership" :secret-json password-hash})))
                    policy/create-session!
                    (fn [_ session] (swap! sessions assoc (:token session) session) (js/Promise.resolve nil))
                    policy/get-session-by-token!
                    (fn [_ token] (js/Promise.resolve {:session (get @sessions token)}))
                    oauth/set-client!
                    (fn save-client!
                      ([id client] (save-client! nil id client))
                      ([_ id client] (swap! clients assoc id client) (js/Promise.resolve nil)))
                    oauth/get-client!
                    (fn read-client!
                      ([id] (read-client! nil id))
                      ([_ id] (js/Promise.resolve (get @clients id))))]
        (await (exercise! app password)))
      (finally
        (await (server/close! app))
        (doseq [[key value] originals]
          (if (nil? value) (js-delete environment key) (aset environment key value)))))))
