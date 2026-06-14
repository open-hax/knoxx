(ns knoxx.backend.infra.routes.auth
  (:require [clojure.string :as str]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.auth.session :as auth-session]
            [knoxx.backend.infra.db.policy :as policy-db]
            ["node:crypto" :as crypto]))

(defn- body-map
  [req]
  (js->clj (or (aget req "body") #js {}) :keywordize-keys true))

(defn- map-value
  [m & ks]
  (some #(get m %) ks))

(defn- env-truthy?
  [key]
  (contains? #{"1" "true" "yes" "on" "y"}
             (-> (or (aget js/process.env key) "") str str/trim str/lower-case)))

(defn- local-password-enabled?
  []
  (let [node-env (some-> (aget js/process.env "NODE_ENV") str str/trim str/lower-case)]
    (or (env-truthy? "KNOXX_LOCAL_PASSWORD_AUTH_ENABLED")
        (not= node-env "production"))))

(defn- local-host?
  [host]
  (boolean (re-find #"^(localhost|127\.0\.0\.1|\[::1\])(:|$)" (str host))))

(defn- request-base-url
  [req configured-base-url]
  (let [headers (.-headers req)
        host (some-> (or (aget headers "x-forwarded-host")
                         (aget headers "host"))
                     str str/trim not-empty)
        proto (some-> (or (aget headers "x-forwarded-proto")
                          (when (local-host? host) "http"))
                      str str/trim not-empty)]
    (if (and host proto)
      (str proto "://" host)
      configured-base-url)))

(defn- password-hash
  [password]
  (let [salt (.toString (.randomBytes crypto 16) "hex")
        hash (.toString (.scryptSync crypto (str password) salt 64) "hex")]
    {:algorithm "scrypt"
     :salt salt
     :hash hash}))

(defn- verify-password?
  [password secret-json]
  (try
    (let [salt (:salt secret-json)
          expected (:hash secret-json)
          expected-buf (.from js/Buffer (str expected) "hex")
          actual-buf (.scryptSync crypto (str password) (str salt) (.-length expected-buf))]
      (and (= "scrypt" (:algorithm secret-json))
           (pos? (.-length expected-buf))
           (.timingSafeEqual crypto expected-buf actual-buf)))
    (catch :default _ false)))

(defn- password-too-short?
  [password]
  (< (count (str password)) 8))

(defn- ^:async store-local-password!
  [policy-context ctx password]
  (await (policy-db/upsert-actor-credential-for-context!
          policy-context
          (get-in ctx [:user :id])
          {:org-id (get-in ctx [:org :id])
           :provider "local"
           :kind "password"
           :account-identifier (get-in ctx [:user :email])
           :secret-json (password-hash password)
           :status "active"})))

(defn- register-auth-config-route!
  [^js app public-base-url github-enabled]
  (.get app "/api/auth/config"
        (fn [_req ^js reply]
          (.send reply (clj->js {:githubEnabled github-enabled
                                 :localPasswordEnabled (local-password-enabled?)
                                 :publicBaseUrl public-base-url
                                 :loginUrl (when github-enabled "/api/auth/login")
                                 :localLoginUrl "/api/auth/local/login"})))))

(defn- ^:async signup-handler!
  [req reply policy-context public-base-url]
  (try
    (let [body (body-map req)
          email (str/lower-case (str/trim (str (or (:email body) ""))))
          password (str (or (:password body) ""))
          display-name (str/trim (str (or (map-value body :display-name :displayName :display_name)
                                          email)))]
      (cond
        (str/blank? email)
        (.send (.code reply 400) (clj->js {:error "email is required"}))

        (and (local-password-enabled?) (password-too-short? password))
        (.send (.code reply 400) (clj->js {:error "password must be at least 8 characters"}))

        :else
        (let [org (await (policy-db/ensure-self-org! (policy-db/context-pool policy-context)
                                                     email
                                                     (or display-name email)))
              _ (await (policy-db/create-user-for-context!
                        policy-context
                        {:email email
                         :display-name (or display-name email)
                         :org-id (:id org)
                         :role-slugs ["basic-user"]
                         :auth-provider (if (str/blank? password) "signup" "local")
                         :status "active"
                         :membership-status "active"
                         :is-default true}))
              ctx (await (policy-db/resolve-context!
                          policy-context
                          {"x-knoxx-user-email" email
                           "x-knoxx-org-slug" (:slug org)}))
              _ (when-not (str/blank? password)
                  (await (store-local-password! policy-context ctx password)))
              result (await (auth-session/create-session-from-context!
                             reply (request-base-url req public-base-url) ctx
                             {:email email
                              :display-name (or display-name email)
                              :auth-provider (if (str/blank? password) "signup" "local")}))]
          (.send reply (clj->js result)))))
    (catch :default err
      (.send (.code reply (or (.-statusCode err) (.-status err) 500))
             (clj->js {:error (or (.-message err) "Signup failed")})))))

(defn- register-signup-route!
  [^js app policy-context public-base-url]
  (.post app "/api/auth/signup"
         (fn [^js req ^js reply]
           (if-not policy-context
             (.send (.code reply 503) (clj->js {:error "Knoxx policy database is not configured"}))
             (signup-handler! req reply policy-context public-base-url)))))

(defn- ^:async local-login-handler!
  [req reply policy-context public-base-url]
  (try
    (let [body (body-map req)
          email (str/lower-case (str/trim (str (or (:email body) (:username body) ""))))
          password (str (or (:password body) ""))]
      (cond
        (not (local-password-enabled?))
        (.send (.code reply 503) (clj->js {:error "Local password auth is disabled"}))

        (or (str/blank? email) (str/blank? password))
        (.send (.code reply 400) (clj->js {:error "email and password are required"}))

        :else
        (let [auth-record (await (policy-db/local-password-auth-record-for-context! policy-context email))]
          (if-not (verify-password? password (:secret-json auth-record))
            (.send (.code reply 401) (clj->js {:error "Invalid username or password"}))
            (let [ctx (await (policy-db/resolve-context!
                              policy-context
                              {"x-knoxx-user-email" (:email auth-record)
                               "x-knoxx-membership-id" (:membership-id auth-record)}))
                  result (await (auth-session/create-session-from-context!
                                 reply (request-base-url req public-base-url) ctx
                                 {:email (:email auth-record)
                                  :display-name (:display-name auth-record)
                                  :auth-provider "local"}))]
              (.send reply (clj->js result)))))))
    (catch :default err
      (.send (.code reply (or (.-statusCode err) (.-status err) 500))
             (clj->js {:error (or (.-message err) "Login failed")})))))

(defn- register-local-login-route!
  [^js app policy-context public-base-url]
  (.post app "/api/auth/local/login"
         (fn [^js req ^js reply]
           (if-not policy-context
             (.send (.code reply 503) (clj->js {:error "Knoxx policy database is not configured"}))
             (local-login-handler! req reply policy-context public-base-url)))))

(defn- register-login-route!
  [^js app public-base-url github-enabled client-id]
  (.get app "/api/auth/login"
        (fn [^js req ^js reply]
          (if-not github-enabled
            (.send (.code reply 503) (clj->js {:error "GitHub OAuth not configured"}))
            (let [redirect (str (or (some-> req (aget "query") (aget "redirect")) "/"))
                  state (auth-session/create-state redirect)
                  callback-url (.toString (js/URL. "/api/auth/callback/github" public-base-url))
                  authorize-url (js/URL. "https://github.com/login/oauth/authorize")]
              (.set (.-searchParams authorize-url) "client_id" client-id)
              (.set (.-searchParams authorize-url) "redirect_uri" callback-url)
              (.set (.-searchParams authorize-url) "state" state)
              (.set (.-searchParams authorize-url) "scope" "read:user user:email")
              (.redirect reply (.toString authorize-url)))))))

(defn- register-github-callback-route!
  [^js app policy-context public-base-url github-enabled client-id client-secret]
  (.get app "/api/auth/callback/github"
        (fn [^js req ^js reply]
          (if-not github-enabled
            (.send (.code reply 503) (clj->js {:error "GitHub OAuth not configured"}))
            (let [code (str (or (some-> req (aget "query") (aget "code")) ""))
                  state-val (str (or (some-> req (aget "query") (aget "state")) ""))]
              (if (or (str/blank? code) (str/blank? state-val))
                (.send (.code reply 400) (clj->js {:error "Missing code or state"}))
                (if-let [state-entry (auth-session/consume-state state-val)]
                  (auth-session/handle-github-callback policy-context reply client-id client-secret state-entry code public-base-url)
                  (.send (.code reply 400) (clj->js {:error "Invalid or expired state parameter"})))))))))

(defn- ^:async logout-handler!
  [req reply public-base-url]
  (let [cookie-token (some-> req (aget "cookies") (aget auth-session/COOKIE-NAME))]
    (when cookie-token
      (let [payload (auth-session/verify-token cookie-token)]
        (when-let [session-id (:sid payload)]
          (try
            (await (auth-session/delete-session session-id cookie-token))
            (catch :default _ nil)))))
    (auth-session/clear-session-cookie reply public-base-url)
    (.send reply (clj->js {:ok true}))))

(defn- register-logout-route!
  [^js app public-base-url]
  (.post app "/api/auth/logout"
         (fn [^js req ^js reply]
           (logout-handler! req reply public-base-url))))

(defn- ^:async invite-redeem-handler!
  [req reply policy-context public-base-url]
  (try
    (let [body (body-map req)
          code (str/trim (str (or (:code body) "")))
          email (str/lower-case
                 (str/trim (str (or (:email body)
                                    (aget (.-headers req) "x-knoxx-user-email")
                                    ""))))]
      (cond
        (str/blank? code)
        (.send (.code reply 400) (clj->js {:error "Invite code is required"}))
        
        (str/blank? email)
        (.send (.code reply 400) (clj->js {:error "email is required"}))
        
        :else
        (let [result (await (policy-db/redeem-invite! (policy-db/context-pool policy-context) code email))
              invite (:invite result)
              ctx (await (policy-db/resolve-context!
                          policy-context
                          {"x-knoxx-user-email" (:email invite)
                           "x-knoxx-org-id" (:org-id invite)}))
              session (await (auth-session/create-session-from-context!
                              reply (request-base-url req public-base-url) ctx
                              {:email (:email invite)
                               :display-name (:email invite)
                               :auth-provider "invite"}))]
          (.send reply (clj->js (assoc session :ok true :invite invite))))))
    (catch :default err
      (.send (.code reply (or (.-status err) 500))
             (clj->js {:error (or (.-message err) "Invite redemption failed")})))))

(defn- register-invite-redeem-route!
  [^js app policy-context public-base-url]
  (.post app "/api/auth/invite/redeem"
         (fn [^js req ^js reply]
           (invite-redeem-handler! req reply policy-context public-base-url))))

(defn- ^:async invite-create-handler!
  [req reply policy-context runtime public-base-url]
  (try
    (let [body (body-map req)
          ctx (await (auth-session/resolve-auth-context req policy-context))
          org-id (or (map-value body :org-id :orgId :org_id)
                     (get-in ctx [:org :id]))
          email (:email body)
          role-slugs (vec (or (map-value body :role-slugs :roleSlugs :role_slugs)
                              ["basic-user"]))]
      (if (str/blank? email)
        (.send (.code reply 400) (clj->js {:error "email is required"}))
        (do
          (authz/ensure-org-scope! ctx org-id "org.users.invite")
          (let [result (await (policy-db/create-invite-for-context!
                               policy-context
                               {:org-id org-id
                                :email email
                                :role-slugs role-slugs
                                :inviter-membership-id (get-in ctx [:membership :id])}))]
            (when (not= (map-value body :send-email :sendEmail :send_email) false)
              (try
                (await (auth-session/send-invite-email runtime (:invite result) email public-base-url))
                (catch :default err
                  (.error js/console "[knoxx-session] Failed to send invite email:" (.-message err)))))
            (.send reply (clj->js {:ok true :invite (:invite result)}))))))
    (catch :default err
      (.send (.code reply (or (.-status err) 500))
             (clj->js {:error (or (.-message err) "Invite creation failed")})))))

(defn- register-invite-create-route!
  [^js app policy-context runtime public-base-url]
  (.post app "/api/auth/invite"
         (fn [^js req ^js reply]
           (invite-create-handler! req reply policy-context runtime public-base-url))))

(defn- ^:async invite-list-handler!
  [req reply policy-context]
  (try
    (let [ctx (await (auth-session/resolve-auth-context req policy-context))
          org-id (or (some-> req (aget "query") (aget "orgId"))
                     (get-in ctx [:org :id]))
          status (some-> req (aget "query") (aget "status"))]
      (authz/ensure-org-scope! ctx org-id "org.users.invite")
      (let [result (await (policy-db/list-invites!
                           (policy-db/context-pool policy-context)
                           (cond-> {:org-id org-id}
                             status (assoc :status status))))]
        (.send reply (clj->js result))))
    (catch :default err
      (.send (.code reply (or (.-status err) 401))
             (clj->js {:error (or (.-message err) "Unauthorized")})))))

(defn- register-invite-list-route!
  [^js app policy-context]
  (.get app "/api/auth/invites"
        (fn [^js req ^js reply]
          (invite-list-handler! req reply policy-context))))

(defn register-auth-routes
  [^js app opts]
  (let [public-base-url (or (aget (.-env js/process) "KNOXX_PUBLIC_BASE_URL") "http://localhost")
        policy-context (:policy-context opts)
        runtime (:runtime opts)
        client-id (or (aget (.-env js/process) "KNOXX_GITHUB_OAUTH_CLIENT_ID") "")
        client-secret (or (aget (.-env js/process) "KNOXX_GITHUB_OAUTH_CLIENT_SECRET") "")
        github-enabled (and (not (str/blank? client-id))
                            (not (str/blank? client-secret)))]
    (when policy-context
      (auth-session/set-db-session-store! policy-context))
    (register-auth-config-route! app public-base-url github-enabled)
    (register-signup-route! app policy-context public-base-url)
    (register-local-login-route! app policy-context public-base-url)
    (register-login-route! app public-base-url github-enabled client-id)
    (register-github-callback-route! app policy-context public-base-url github-enabled client-id client-secret)
    (register-logout-route! app public-base-url)
    (register-invite-redeem-route! app policy-context public-base-url)
    (register-invite-create-route! app policy-context runtime public-base-url)
    (register-invite-list-route! app policy-context)))
