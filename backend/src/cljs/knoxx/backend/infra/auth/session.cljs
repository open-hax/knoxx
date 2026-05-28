(ns knoxx.backend.infra.auth.session
  (:refer-clojure :exclude [set])
  (:require [clojure.string :as str]
            [knoxx.backend.infra.clients.github :as github-client]
            [knoxx.backend.infra.db.policy :as policy-db]
            ["node:crypto" :as crypto]
            ["nodemailer" :default nodemailer]
            ["redis" :as redis]))


(def ^:private session-secret-mem (atom nil))

(defn- session-secret
  []
  (or @session-secret-mem
      (let [env-secret (aget (.-env js/process) "KNOXX_SESSION_SECRET")
            secret (or (when (not (str/blank? env-secret)) env-secret)
                       (.toString (.randomBytes crypto 32) "hex"))]
        (reset! session-secret-mem secret)
        secret)))


(defn- sign-token
  [payload]
  (let [key (session-secret)
        iv (.randomBytes crypto 12)
        data (js/JSON.stringify (clj->js payload))
        key-buf (.subarray (.from js/Buffer key "hex") 0 32)
        cipher (.createCipheriv crypto "aes-256-gcm" key-buf iv)]
    (let [encrypted (str (.update cipher data "utf8" "base64url")
                         (.final cipher "base64url"))
          tag (.getAuthTag cipher)]
      (str (.toString iv "base64url") ":" encrypted ":" (.toString tag "base64url")))))


(defn verify-token
  [token]
  (try
    (let [key (session-secret)
          parts (.split token ":")]
      (when (>= (.-length parts) 3)
        (let [iv-b64 (aget parts 0)
              encrypted (aget parts 1)
              tag-b64 (aget parts 2)
              iv (.from js/Buffer iv-b64 "base64url")
              tag (.from js/Buffer tag-b64 "base64url")
              key-buf (.subarray (.from js/Buffer key "hex") 0 32)
              decipher (.createDecipheriv crypto "aes-256-gcm" key-buf iv)]
          (.setAuthTag decipher tag)
          (let [decrypted (str (.update decipher encrypted "base64url" "utf8")
                               (.final decipher "utf8"))]
            (js->clj (js/JSON.parse decrypted) :keywordize-keys true)))))
    (catch :default _ nil)))


(def ^:private redis-client (atom nil))
(def ^:private redis-connect-promise (atom nil))

;; --- Persistent session store (Postgres) ---

(def ^:private db-session-store (atom nil))

(declare recover-or-persist-session-secret!)

(defn set-db-session-store!
  [policy-context]
  (reset! db-session-store policy-context)
  ;; Recover or persist the session secret so tokens survive restarts.
  (recover-or-persist-session-secret! policy-context))

(defn- recover-or-persist-session-secret!
  "If KNOXX_SESSION_SECRET env is set, use it. Otherwise, try to load from DB
   (table: knoxx_config, key: session_secret). If none exists, generate, store, and use."
  [policy-context]
  (let [env-secret (aget (.-env js/process) "KNOXX_SESSION_SECRET")]
    (if (not (str/blank? env-secret))
      (do
        (reset! session-secret-mem env-secret)
        (.log js/console "[knoxx-session] Using session secret from KNOXX_SESSION_SECRET env"))
      (-> (policy-db/recover-session-secret! (policy-db/context-pool policy-context))
          (.then (fn [secret]
                   (reset! session-secret-mem secret)))
          (.catch (fn [err]
                    (.log js/console "[knoxx-session] ERROR loading session secret from DB:" (.-message err))))))))

(defn- session-field
  [session-data & ks]
  (some (fn [k] (get session-data k)) ks))

(defn- normalize-session-data
  [session-data]
  (let [m (if (map? session-data)
            session-data
            (js->clj session-data :keywordize-keys true))]
    {:id               (session-field m :id)
     :user-id          (session-field m :user-id :userId)
     :membership-id    (session-field m :membership-id :membershipId)
     :actor-id         (session-field m :actor-id :actorId)
     :email            (session-field m :email)
     :org-slug         (session-field m :org-slug :orgSlug)
     :org-id           (session-field m :org-id :orgId)
     :display-name     (session-field m :display-name :displayName)
     :github-login     (session-field m :github-login :githubLogin)
     :github-id        (session-field m :github-id :githubId)
     :auth-provider    (session-field m :auth-provider :authProvider)
     :external-subject (session-field m :external-subject :externalSubject :externalId)
     :ip-address       (session-field m :ip-address :ipAddress)
     :user-agent       (session-field m :user-agent :userAgent)
     :raw-token        (session-field m :raw-token :_rawToken)
     :created-at       (session-field m :created-at :createdAt)}))

(defn- db-store-session
  [token session-data]
  (if-not @db-session-store
    (js/Promise.resolve nil)
    (let [payload (assoc (normalize-session-data session-data) :token token)]
      (.catch (policy-db/create-session! (policy-db/context-pool @db-session-store) payload)
              (fn [_] nil)))))

(defn- db-load-session
  [token]
  (if-not @db-session-store
    (js/Promise.resolve nil)
    (-> (policy-db/get-session-by-token! (policy-db/context-pool @db-session-store) token)
        (.then (fn [result]
                 (when-let [s (:session result)]
                   (normalize-session-data s))))
        (.catch (fn [_] nil)))))

(defn- get-redis
  []
  (if (and @redis-client (.-isOpen @redis-client))
    (js/Promise.resolve @redis-client)
    (if @redis-connect-promise
      @redis-connect-promise
      (let [promise
            (-> (let [url (or (aget (.-env js/process) "REDIS_URL") "redis://127.0.0.1:6379")
                      client (.createClient redis (clj->js {:url url}))]
                  (.on client "error"
                       (fn [err] (.error js/console "[knoxx-session] Redis error:" (.-message err))))
                  (-> (.connect client)
                      (.then (fn [_]
                               (.log js/console "[knoxx-session] Redis connected for session store")
                               (reset! redis-client client)
                               (reset! redis-connect-promise nil)
                               client))))
                (.catch (fn [err]
                          (reset! redis-connect-promise nil)
                          (js/Promise.reject err))))]
        (reset! redis-connect-promise promise)
        promise))))


(defn- store-session
  [session-id data]
  (let [normalized (normalize-session-data data)
        token (or (:raw-token normalized) "")]
    (-> (db-store-session token normalized)
        (.catch (fn [err]
                  (.log js/console "[knoxx-session] WARN: DB store failed:" (.-message err))))
        (.then (fn [_]
                 (-> (get-redis)
                     (.then
                      (fn [redis]
                        (let [ttl (js/parseInt (or (aget (.-env js/process) "KNOXX_SESSION_TTL_SECONDS") "86400") 10)]
                          (.set redis (str "knoxx:session:" session-id)
                                (js/JSON.stringify (clj->js normalized))
                                (clj->js {:EX ttl})))))))))))

(defn- load-session
  [session-id token]
  ;; Try Redis first (fast cache), fall back to Postgres (persistent).
  (-> (get-redis)
      (.then (fn [redis] (.get redis (str "knoxx:session:" session-id))))
      (.then
       (fn [raw]
         (if raw
           (try
             (normalize-session-data (js/JSON.parse raw))
             (catch :default _err1 nil))
           (db-load-session (or token "")))))
      (.catch (fn [_err2] (db-load-session (or token ""))))))

(defn delete-session
  [session-id token]
  ;; Delete from Postgres first (authoritative), then Redis (cache).
  (-> (if (and @db-session-store (not (str/blank? token)))
        (.catch (policy-db/delete-session-by-token! (policy-db/context-pool @db-session-store) token)
                (fn [_] nil))
        (js/Promise.resolve nil))
      (.then (fn [_]
               (-> (get-redis)
                   (.then (fn [redis] (.del redis (str "knoxx:session:" session-id))))
                   (.catch (fn [_] nil)))))
      (.then (fn [_] nil))))


(defn- github-auth-client
  [client-id client-secret]
  (github-client/client {:client-id client-id
                         :client-secret client-secret}))

(defn- exchange-github-code
  [client-id client-secret code]
  (github-client/oauth-access-token! (github-auth-client client-id client-secret) code nil))

(defn- get-github-user
  [access-token]
  (github-client/authenticated-user! (github-client/client {}) access-token))

(defn- get-github-user-emails
  [access-token]
  (-> (github-client/authenticated-emails! (github-client/client {}) access-token)
      (.then
       (fn [emails]
         (let [primary (some (fn [e] (when (:primary e) e)) emails)]
           (or (:email primary)
               (:email (first emails))))))
      (.catch (fn [_] nil))))


(def COOKIE-NAME "knoxx_session")

(defn- secure-origin?
  [base-url]
  (try
    (= (.-protocol (js/URL. base-url)) "https:")
    (catch :default _ false)))

(defn- set-session-cookie
  [reply token base-url]
  (let [secure (secure-origin? base-url)
        ttl (js/parseInt (or (aget (.-env js/process) "KNOXX_SESSION_TTL_SECONDS") "86400") 10)]
    ;; OAuth callback -> redirected protected route is a cross-site navigation
    ;; chain from github.com. SameSite=Strict drops the fresh session cookie on
    ;; that first redirected request, which loops MCP OAuth back into login.
    ;; Lax preserves CSRF protection for subrequests while allowing top-level
    ;; navigations like OAuth callback completion.
    (.setCookie reply COOKIE-NAME token
                (clj->js {:path "/"
                          :httpOnly true
                          :secure secure
                          :sameSite "Lax"
                          :maxAge ttl}))))

(defn clear-session-cookie
  [reply base-url]
  (let [secure (secure-origin? base-url)]
    (.clearCookie reply COOKIE-NAME
                  (clj->js {:path "/"
                            :httpOnly true
                            :secure secure
                            :sameSite "Lax"}))))


(def ^:private STATE-TTL 600)
(def ^:private pending-states (atom {}))

(defn create-state
  [redirect]
  (let [state (.toString (.randomBytes crypto 16) "hex")]
    (swap! pending-states assoc state {:redirect (or redirect "/")
                                       :createdAt (js/Date.now)})
    (swap! pending-states
           (fn [states]
             (into {}
                   (filter (fn [[_ v]]
                             (< (- (js/Date.now) (:createdAt v))
                                (* STATE-TTL 1000))))
                   states)))
    state))

(defn consume-state
  [state]
  (when-let [entry (get @pending-states state)]
    (swap! pending-states dissoc state)
    (when (< (- (js/Date.now) (:createdAt entry)) (* STATE-TTL 1000))
      entry)))


(defn- http-error
  [status message code]
  (let [err (js/Error. message)]
    (set! (.-status err) status)
    (set! (.-code err) code)
    err))


;; --- Extracted helpers for handle-github-callback (paren hygiene) ----------

(defn- bootstrap-role-slugs-for-email
  [email]
  (let [normalized-email (str/lower-case (str/trim (str (or email ""))))
        bootstrap-admin-email (some-> (or (aget (.-env js/process) "KNOXX_BOOTSTRAP_SYSTEM_ADMIN_EMAIL")
                                          "system-admin@open-hax.local")
                                      str
                                      str/trim
                                      str/lower-case)
        allowlisted-emails (->> (str/split (str (or (aget (.-env js/process) "KNOXX_BOOTSTRAP_ALLOWLIST_EMAILS") "")) #"[\s,]+")
                                (map str/trim)
                                (remove str/blank?)
                                (map str/lower-case)
                                clojure.core/set)
        allowlist-role-slugs (->> (str/split (str (or (aget (.-env js/process) "KNOXX_BOOTSTRAP_ALLOWLIST_ROLE_SLUGS") "")) #"[\s,]+")
                                  (map str/trim)
                                  (remove str/blank?)
                                  vec)]
    (cond
      (= normalized-email bootstrap-admin-email)
      ["system_admin"]

      (contains? allowlisted-emails normalized-email)
      (if (seq allowlist-role-slugs)
        allowlist-role-slugs
        ["knowledge_worker"])

      :else
      ["knowledge_worker"])))

(defn- bootstrap-admin-email?
  [email]
  (let [normalized-email (str/lower-case (str/trim (str (or email ""))))
        bootstrap-admin-email (some-> (or (aget (.-env js/process) "KNOXX_BOOTSTRAP_SYSTEM_ADMIN_EMAIL")
                                          "system-admin@open-hax.local")
                                      str
                                      str/trim
                                      str/lower-case)]
    (= normalized-email bootstrap-admin-email)))

(defn- has-system-admin-role?
  [ctx]
  (boolean
   (some #{"system_admin" "system-admin"}
         (map str (or (:role-slugs ctx) [])))))

(defn- ensure-bootstrap-admin-role!
  "Repair an existing bootstrap admin account that was created before the
   bootstrap email config was set correctly. If the authenticating GitHub email
   now matches the configured bootstrap admin email, guarantee the membership
   carries the global system_admin role before building the session context."
  [policy-context ctx email]
  (let [membership-id (get-in ctx [:membership :id])
        org-id (get-in ctx [:org :id])]
    (if (and membership-id
             (bootstrap-admin-email? email)
             (not (has-system-admin-role? ctx)))
      (-> (policy-db/set-membership-roles-for-context!
           policy-context
           membership-id
           {:org-id org-id
            :role-slugs ["system_admin"]})
          (.then (fn [_]
                   (policy-db/resolve-context! policy-context {"x-knoxx-membership-id" membership-id}))))
      (js/Promise.resolve ctx))))

(defn- ensure-email-membership!
  [policy-context {:keys [email display-name auth-provider external-subject]}]
  (let [normalized-email (some-> email str str/trim not-empty)
        headers-like (when normalized-email
                       {"x-knoxx-user-email" normalized-email})]
    (if-not normalized-email
      (js/Promise.reject (http-error 401 "Not authenticated" "no_email"))
      (-> (policy-db/sync-user-from-actor-contract-for-context!
           policy-context
           {:email normalized-email
            :display-name (or display-name normalized-email)
            :auth-provider (or auth-provider "github")
            :external-subject external-subject})
          (.then (fn [_]
                   (policy-db/resolve-context! policy-context headers-like)))))))

(defn- gh-value
  [gh-user k]
  (if (map? gh-user)
    (get gh-user k)
    (aget gh-user (name k))))

(defn ensure-user-membership!
  "Resolve the canonical Knoxx user context by GitHub email.

   Email is the canonical username. Actor and role assignment now come from the
   persisted Knoxx user/membership records rather than being inferred from the
   OAuth callback environment."
  [policy-context gh-user email]
  (ensure-email-membership! policy-context {:email email
                                            :display-name (or (gh-value gh-user :name)
                                                              (gh-value gh-user :login)
                                                              email)
                                            :auth-provider "github"
                                            :external-subject (when (gh-value gh-user :id)
                                                                (str "github:" (gh-value gh-user :id)))}))

(defn- configured-api-key
  []
  (some-> (aget (.-env js/process) "KNOXX_API_KEY") str str/trim not-empty))

(defn- request-api-key
  [req]
  (some-> (or (aget (.-headers req) "x-api-key")
              (aget (.-headers req) "X-API-Key"))
          str
          str/trim
          not-empty))

(defn- api-key-auth-email
  []
  (let [node-env (some-> (aget (.-env js/process) "NODE_ENV") str str/trim str/lower-case)]
    (some-> (or (aget (.-env js/process) "KNOXX_API_KEY_USER_EMAIL")
                (when (not= node-env "production")
                  "pi@open-hax.local"))
            str
            str/trim
            not-empty)))

(defn- valid-api-key-request?
  [req]
  (let [expected (configured-api-key)
        provided (request-api-key req)]
    (and expected provided (= expected provided))))

(defn- ensure-api-key-membership!
  [policy-context]
  (if-let [email (api-key-auth-email)]
    (ensure-email-membership! policy-context {:email email
                                              :display-name "Pi"
                                              :auth-provider "api-key"
                                              :external-subject (str "api-key:" email)})
    (js/Promise.reject (http-error 401 "Knoxx API key user email is not configured" "api_key_identity_missing"))))

(defn- resolve-cookie-auth-context
  [req policy-context]
  (let [cookie-token (some-> req (aget "cookies") (aget COOKIE-NAME))]
    (if-not cookie-token
      (js/Promise.reject (http-error 401 "Not authenticated" "no_session"))
      (let [payload (verify-token cookie-token)
            session-id (:sid payload)]
        (if-not session-id
          (js/Promise.reject (http-error 401 "Invalid session token" "invalid_token"))
          (-> (load-session session-id cookie-token)
              (.then
               (fn [session-data]
                 (if-not session-data
                   (js/Promise.reject (http-error 401 "Session expired" "session_expired"))
                   (let [headers (cond-> {"x-knoxx-user-email" (:email session-data)
                                          "x-knoxx-org-slug" (:org-slug session-data)}
                                   (:membership-id session-data)
                                   (assoc "x-knoxx-membership-id" (:membership-id session-data)))]
                     (policy-db/resolve-context! policy-context headers)))))))))))

(defn- create-session-and-redirect!
  "Create session from resolved context, set cookie, and redirect."
  [policy-context reply gh-user email state-entry public-base-url]
  (-> (ensure-user-membership! policy-context gh-user email)
      (.then
       (fn [fresh-ctx]
         (let [session-id (.randomUUID crypto)
               raw-token (sign-token {:sid session-id})
               session-data {:membership-id (get-in fresh-ctx [:membership :id])
                             :actor-id (or (get-in fresh-ctx [:membership :actor-id])
                                           (get-in fresh-ctx [:actor :id]))
                             :user-id (get-in fresh-ctx [:user :id])
                             :email email
                             :org-slug (get-in fresh-ctx [:org :slug])
                             :org-id (get-in fresh-ctx [:org :id])
                             :display-name (or (gh-value gh-user :name) (gh-value gh-user :login) email)
                             :github-login (gh-value gh-user :login)
                             :github-id (gh-value gh-user :id)
                             :auth-provider "github"
                             :raw-token raw-token
                             :created-at (.toISOString (js/Date.))}]
           (-> (store-session session-id session-data)
               (.then (fn [_] raw-token))
               (.then
                (fn [token]
                  (set-session-cookie reply token public-base-url)
                  (.log js/console (str "[knoxx-session] GitHub login: " email))
                  (.redirect reply
                             (.toString (js/URL. (:redirect state-entry) public-base-url)))))))))))

(defn create-session-from-context!
  [reply public-base-url ctx session-options]
  (let [session-id (.randomUUID crypto)
        raw-token (sign-token {:sid session-id})
        session-data {:membership-id (get-in ctx [:membership :id])
                      :actor-id (or (get-in ctx [:membership :actor-id])
                                    (get-in ctx [:actor :id]))
                      :user-id (get-in ctx [:user :id])
                      :email (or (:email session-options)
                                 (get-in ctx [:user :email])
                                 "")
                      :org-slug (get-in ctx [:org :slug])
                      :org-id (get-in ctx [:org :id])
                      :display-name (or (:display-name session-options)
                                        (get-in ctx [:user :display-name])
                                        (get-in ctx [:user :email])
                                        "")
                      :auth-provider (or (:auth-provider session-options) "local")
                      :raw-token raw-token
                      :created-at (.toISOString (js/Date.))}]
    (-> (store-session session-id session-data)
        (.then (fn [_]
                 (set-session-cookie reply raw-token public-base-url)
                 {:ok true
                  :session-id session-id
                  :user (:user ctx)
                  :actor (:actor ctx)
                  :org (:org ctx)
                  :membership (:membership ctx)})))))

(defn- check-whitelist-and-session!
  "Check if email is whitelisted; if so, create session and redirect, otherwise redirect to invite page."
  [policy-context reply gh-user email state-entry public-base-url]
  (-> (ensure-user-membership! policy-context gh-user email)
      (.then (fn [_] true))
      (.catch (fn [_] false))
      (.then
       (fn [whitelisted]
         (if (not whitelisted)
           ;; Not whitelisted — redirect to invite page
           (let [invite-url (js/URL. "/login" public-base-url)]
             (.set (.-searchParams invite-url) "error" "not_whitelisted")
             (.set (.-searchParams invite-url) "email" email)
             (.set (.-searchParams invite-url) "github_login" (or (gh-value gh-user :login) ""))
             (.redirect reply (.toString invite-url)))
           ;; Whitelisted — upsert and create session
           (create-session-and-redirect!
            policy-context reply gh-user email state-entry public-base-url))))))

;; --- Main callback handler -------------------------------------------------

(defn handle-github-callback
  [policy-context reply client-id client-secret state-entry code public-base-url]
  (-> (exchange-github-code client-id client-secret code)
      (.then
        (fn [access-token]
          (-> (get-github-user access-token)
              (.then
                (fn [gh-user]
                  (if-not (gh-value gh-user :id)
                    (throw (js/Error. "GitHub user lookup failed"))
                    (-> (get-github-user-emails access-token)
                        (.then
                          (fn [email]
                            (if-not email
                              (throw (js/Error. "Could not retrieve GitHub email"))
                              (check-whitelist-and-session!
                                policy-context reply gh-user email
                                state-entry public-base-url)))))))))))
      (.catch
        (fn [err]
          (.error js/console
            "[knoxx-session] GitHub OAuth callback error:" (.-message err))
          (let [error-url (js/URL. "/login" public-base-url)]
            (.set (.-searchParams error-url) "error" "oauth_failed")
            (.set (.-searchParams error-url) "message" (.-message err))
            (.redirect reply (.toString error-url)))))))


;; ---------------------------------------------------------------------------
;; Invite email (optional)
;; ---------------------------------------------------------------------------

(defn send-invite-email
  "Best-effort invite email sender.

   IMPORTANT: This function MUST always return a Promise, so callers can safely
   attach .catch even when email sending is disabled/unconfigured."
  [_runtime invite email public-base-url]
  (try
    (let [smtp-host (str (or (aget (.-env js/process) "KNOXX_SMTP_HOST") ""))
          smtp-port (js/parseInt (or (aget (.-env js/process) "KNOXX_SMTP_PORT") "587") 10)
          smtp-user (str (or (aget (.-env js/process) "KNOXX_SMTP_USER") ""))
          smtp-pass (str (or (aget (.-env js/process) "KNOXX_SMTP_PASS") ""))
          from (str (or (aget (.-env js/process) "KNOXX_EMAIL_FROM") smtp-user ""))
          invite-code (str (or (aget invite "code") ""))
          invite-url (try
                       (let [u (js/URL. "/login" public-base-url)]
                         (.set (.-searchParams u) "invite" invite-code)
                         (.set (.-searchParams u) "email" (str email))
                         (.toString u))
                       (catch :default _ ""))]
      (if (or (str/blank? smtp-host)
              (str/blank? from)
              (str/blank? smtp-user)
              (str/blank? smtp-pass)
              (str/blank? (str email))
              (str/blank? invite-code)
              (str/blank? invite-url))
        (js/Promise.resolve nil)
        (let [transporter (.createTransport nodemailer
                                            #js {:host smtp-host
                                                 :port smtp-port
                                                 :secure false
                                                 :auth #js {:user smtp-user
                                                            :pass smtp-pass}})
              subject "Knoxx invite"
              text (str "You have been invited to Knoxx.\n\n"
                        "Invite link: " invite-url "\n")]
          (.sendMail transporter
                     #js {:from from
                          :to (str email)
                          :subject subject
                          :text text}))))
    (catch :default err
      ;; Never block invite creation on email failure.
      (.warn js/console "[knoxx-session] send-invite-email error:" (.-message err))
      (js/Promise.resolve nil))))


(defn create-session-hook
  [_policy-context]
  (fn session-hook [req reply]
    (when (not (and (.startsWith (.-url req) "/api/auth/")
                    (not (.startsWith (.-url req) "/api/auth/context"))))
      (let [headers (.-headers req)
            header-email (str/trim (or (aget headers "x-knoxx-user-email") ""))
            header-mid (str/trim (or (aget headers "x-knoxx-membership-id") ""))
            cookie-token (some-> req (aget "cookies") (aget COOKIE-NAME))]
        (when (and (str/blank? header-email)
                   (str/blank? header-mid)
                   cookie-token)
          (let [payload (verify-token cookie-token)
                session-id (:sid payload)]
            (when session-id
              (-> (load-session session-id cookie-token)
                  (.then
                   (fn [session-data]
                     (if-not session-data
                       (clear-session-cookie reply (or (aget (.-env js/process) "KNOXX_PUBLIC_BASE_URL") "http://localhost"))
                       (do
                         (aset headers "x-knoxx-user-email" (:email session-data))
                         (when (:org-slug session-data)
                           (aset headers "x-knoxx-org-slug" (:org-slug session-data)))
                         (when (:membership-id session-data)
                           (aset headers "x-knoxx-membership-id" (:membership-id session-data))))))
                  (.catch (fn [_] nil)))))))))))


(defn resolve-auth-context
  [req policy-context]
  (let [header-email (str/trim (or (aget (.-headers req) "x-knoxx-user-email") ""))
        header-mid (str/trim (or (aget (.-headers req) "x-knoxx-membership-id") ""))]
    (cond
      (or (not (str/blank? header-email)) (not (str/blank? header-mid)))
      (policy-db/resolve-context! policy-context (.-headers req))

      (valid-api-key-request? req)
      (ensure-api-key-membership! policy-context)

      :else
      (resolve-cookie-auth-context req policy-context))))
