(ns knoxx.backend.infra.auth.session
  (:refer-clojure :exclude [set])
  (:require [clojure.string :as str]
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
            (js/JSON.parse decrypted)))))
    (catch :default _ nil)))


(def ^:private redis-client (atom nil))
(def ^:private redis-connect-promise (atom nil))

;; --- Persistent session store (Postgres) ---

(def ^:private db-session-store (atom nil))

(declare recover-or-persist-session-secret!)

(defn set-db-session-store!
  [policyDb]
  (reset! db-session-store policyDb)
  ;; Recover or persist the session secret so tokens survive restarts
  (recover-or-persist-session-secret! policyDb))

(defn- recover-or-persist-session-secret!
  "If KNOXX_SESSION_SECRET env is set, use it. Otherwise, try to load from DB
   (table: knoxx_config, key: session_secret). If none exists, generate, store, and use."
  [policyDb]
  (let [env-secret (aget (.-env js/process) "KNOXX_SESSION_SECRET")]
    (if (not (str/blank? env-secret))
      (do
        (reset! session-secret-mem env-secret)
        (.log js/console "[knoxx-session] Using session secret from KNOXX_SESSION_SECRET env"))
      ;; No env secret — try DB, then generate
      (-> (.query policyDb "SELECT value FROM knoxx_config WHERE key = 'session_secret'" [])
          (.then
           (fn [result]
             (let [rows (aget result "rows")]
               (if (> (.-length rows) 0)
                 (let [stored (aget rows 0 "value")]
                   (reset! session-secret-mem stored)
                   (.log js/console "[knoxx-session] Recovered session secret from database"))
                 ;; Generate new secret and persist
                 (let [new-secret (.toString (.randomBytes crypto 32) "hex")]
                   (reset! session-secret-mem new-secret)
                   (-> (.query policyDb
                               "INSERT INTO knoxx_config (key, value) VALUES ('session_secret', $1)
                                ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value"
                               [new-secret])
                       (.then (fn [_]
                                (.log js/console "[knoxx-session] Generated and persisted session secret to database")))
                       (.catch (fn [err2]
                                 (.log js/console "[knoxx-session] ERROR persisting secret:" (.-message err2))))))))))
          (.catch (fn [err]
                    (.log js/console "[knoxx-session] ERROR loading session secret from DB:" (.-message err))))))))

(defn- db-store-session
  [token session-data]
  (if-not @db-session-store
    (js/Promise.resolve nil)
    (let [db @db-session-store
          payload (clj->js {:token       token
                            :userId      (or (aget session-data "userId") "")
                            :membershipId (or (aget session-data "membershipId") "")
                            :orgId       (or (aget session-data "orgId") "")
                            :email       (or (aget session-data "email") "")
                            :displayName (or (aget session-data "displayName") "")
                            :authProvider (or (aget session-data "authProvider") "github")
                            :externalSubject (or (aget session-data "externalId") nil)
                            :ipAddress   (or (aget session-data "ipAddress") nil)
                            :userAgent   (or (aget session-data "userAgent") nil)})]
      (.catch (.createSession db payload) (fn [_] nil)))))

(defn- db-load-session
  [token]
  (if-not @db-session-store
    (do (js/Promise.resolve nil))
    (-> (.getSessionByToken @db-session-store token)
        (.then (fn [result]
                 (when (and result (aget result "session"))
                   (let [s (aget result "session")]
                     (clj->js {:id            (aget s "id")
                               :userId        (aget s "userId")
                               :membershipId  (aget s "membershipId")
                               :email         (aget s "email")
                               :orgSlug       nil
                               :orgId         (aget s "orgId")
                               :displayName   (aget s "displayName")
                               :githubLogin   nil
                               :githubId      nil
                               :authProvider  (aget s "authProvider")
                               :createdAt     (aget s "createdAt")})))))
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
  (let [token (or (aget data "_rawToken") "")]
    (-> (db-store-session token data)
        (.catch (fn [err]
                  (.log js/console "[knoxx-session] WARN: DB store failed:" (.-message err))))
        (.then (fn [_]
                 (-> (get-redis)
                     (.then
                       (fn [redis]
                         (let [ttl (js/parseInt (or (aget (.-env js/process) "KNOXX_SESSION_TTL_SECONDS") "86400") 10)]
                           (.set redis (str "knoxx:session:" session-id)
                                 (js/JSON.stringify (clj->js data))
                                 (clj->js {:EX ttl})))))))))))

(defn- load-session
  [session-id token]
  ;; Try Redis first (fast cache), fall back to Postgres (persistent)
  (-> (get-redis)
      (.then (fn [redis] (.get redis (str "knoxx:session:" session-id))))
      (.then
        (fn [raw]
          (if raw
            (try (js/JSON.parse raw) (catch :default _err1 nil))
            (db-load-session (or token "")))))
      (.catch (fn [_err2] (db-load-session (or token ""))))))

(defn delete-session
  [session-id token]
  ;; Delete from Postgres first (authoritative), then Redis (cache)
  (-> (if (and @db-session-store (not (str/blank? token)))
        (.catch (.deleteSessionByToken @db-session-store token) (fn [_] nil))
        (js/Promise.resolve nil))
      (.then (fn [_]
               (-> (get-redis)
                   (.then (fn [redis] (.del redis (str "knoxx:session:" session-id))))
                   (.catch (fn [_] nil)))))
      (.then (fn [_] nil))))


(defn- exchange-github-code
  [client-id client-secret code]
  (-> (js/fetch "https://github.com/login/oauth/access_token"
                (clj->js {:method "POST"
                          :headers {:Content-Type "application/json"
                                    :Accept "application/json"}
                          :body (js/JSON.stringify
                                  (clj->js {:client_id client-id
                                            :client_secret client-secret
                                            :code code}))}))
      (.then
       (fn [resp]
         (if (not (.-ok resp))
           (throw (js/Error. (str "GitHub token exchange failed: " (.-status resp))))
           (.json resp))))
      (.then
       (fn [data]
         (if (aget data "error")
           (throw (js/Error. (str "GitHub OAuth error: "
                                  (or (aget data "error_description") (aget data "error")))))
           (aget data "access_token"))))))

(defn- gh-json
  [url access-token]
  (-> (js/fetch url (clj->js {:headers {:Authorization (str "Bearer " access-token)
                                         :Accept "application/json"}}))
      (.then
       (fn [resp]
         (if (not (.-ok resp))
           (throw (js/Error. (str "GitHub API " url " returned " (.-status resp))))
           (.json resp))))))

(defn- get-github-user-emails
  [access-token]
  (-> (gh-json "https://api.github.com/user/emails" access-token)
      (.then
       (fn [emails]
         (let [primary (some (fn [e] (when (aget e "primary") e)) emails)]
           (or (some-> primary (aget "email"))
               (some-> (first emails) (aget "email"))))))
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
      (clj->js ["system_admin"])

      (contains? allowlisted-emails normalized-email)
      (clj->js (if (seq allowlist-role-slugs)
                 allowlist-role-slugs
                 ["knowledge_worker"]))

      :else
      (clj->js ["knowledge_worker"]))))

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
   (some #(= (str %) "system_admin")
         (or (some-> ctx (aget "roleSlugs") array-seq)
             []))))

(defn- ensure-bootstrap-admin-role!
  "Repair an existing bootstrap admin account that was created before the
   bootstrap email config was set correctly. If the authenticating GitHub email
   now matches the configured bootstrap admin email, guarantee the membership
   carries the global system_admin role before building the session context."
  [policyDb ctx email]
  (let [membership-id (some-> ctx (aget "membership") (aget "id"))
        org-id (some-> ctx (aget "org") (aget "id"))]
    (if (and membership-id
             (bootstrap-admin-email? email)
             (not (has-system-admin-role? ctx)))
      (-> (.setMembershipRoles policyDb membership-id
                               (clj->js {:orgId org-id
                                         :roleSlugs ["system_admin"]}))
          (.then (fn [_]
                   (.resolveRequestContext policyDb (clj->js {"x-knoxx-membership-id" membership-id})))))
      (js/Promise.resolve ctx))))

(defn- ensure-email-membership!
  [policyDb {:keys [email display-name auth-provider external-subject]}]
  (let [normalized-email (some-> email str str/trim not-empty)
        headers-like (when normalized-email
                       (clj->js {"x-knoxx-user-email" normalized-email}))
        sync-user-from-actor-contract (aget policyDb "syncUserFromActorContract")]
    (if-not normalized-email
      (js/Promise.reject (http-error 401 "Not authenticated" "no_email"))
      (-> (if sync-user-from-actor-contract
            (sync-user-from-actor-contract (clj->js {:email normalized-email
                                                     :displayName (or display-name normalized-email)
                                                     :authProvider (or auth-provider "github")
                                                     :externalSubject external-subject}))
            (js/Promise.resolve nil))
          (.then (fn [_]
                   (.resolveRequestContext policyDb headers-like)))))))

(defn ensure-user-membership!
  "Resolve the canonical Knoxx user context by GitHub email.

   Email is the canonical username. Actor and role assignment now come from the
   persisted Knoxx user/membership records rather than being inferred from the
   OAuth callback environment."
  [policyDb gh-user email]
  (ensure-email-membership! policyDb {:email email
                                      :display-name (or (aget gh-user "name")
                                                        (aget gh-user "login")
                                                        email)
                                      :auth-provider "github"
                                      :external-subject (when (aget gh-user "id")
                                                          (str "github:" (aget gh-user "id")))}))

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
  [policyDb]
  (if-let [email (api-key-auth-email)]
    (ensure-email-membership! policyDb {:email email
                                        :display-name "Pi"
                                        :auth-provider "api-key"
                                        :external-subject (str "api-key:" email)})
    (js/Promise.reject (http-error 401 "Knoxx API key user email is not configured" "api_key_identity_missing"))))

(defn- resolve-cookie-auth-context
  [req policyDb]
  (let [cookie-token (some-> req (aget "cookies") (aget COOKIE-NAME))]
    (if-not cookie-token
      (js/Promise.reject (http-error 401 "Not authenticated" "no_session"))
      (let [payload (verify-token cookie-token)]
        (if-not (and payload (aget payload "sid"))
          (js/Promise.reject (http-error 401 "Invalid session token" "invalid_token"))
          (-> (load-session (aget payload "sid") cookie-token)
              (.then
               (fn [session-data]
                 (if-not session-data
                   (js/Promise.reject (http-error 401 "Session expired" "session_expired"))
                   (let [headers #js {"x-knoxx-user-email" (aget session-data "email")
                                      "x-knoxx-org-slug" (aget session-data "orgSlug")}]
                     (when (aget session-data "membershipId")
                       (aset headers "x-knoxx-membership-id" (aget session-data "membershipId")))
                     (.resolveRequestContext policyDb headers)))))))))))

(defn- create-session-and-redirect!
  "Create session from resolved context, set cookie, and redirect."
  [policyDb reply gh-user email state-entry public-base-url]
  (-> (ensure-user-membership! policyDb gh-user email)
      (.then
        (fn [fresh-ctx]
          (let [session-id (.randomUUID crypto)
                raw-token (sign-token #js {:sid session-id})
                session-data #js {:membershipId (some-> fresh-ctx (aget "membership") (aget "id"))
                                  :actorId (or (some-> fresh-ctx (aget "membership") (aget "actorId"))
                                               (some-> fresh-ctx (aget "actor") (aget "id")))
                                  :userId (some-> fresh-ctx (aget "user") (aget "id"))
                                  :email email
                                  :orgSlug (some-> fresh-ctx (aget "org") (aget "slug"))
                                  :orgId (some-> fresh-ctx (aget "org") (aget "id"))
                                  :displayName (or (aget gh-user "name") (aget gh-user "login") email)
                                  :githubLogin (aget gh-user "login")
                                  :githubId (aget gh-user "id")
                                  :authProvider "github"
                                  :_rawToken raw-token
                                  :createdAt (.toISOString (js/Date.))}]
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
        raw-token (sign-token #js {:sid session-id})
        session-data #js {:membershipId (some-> ctx (aget "membership") (aget "id"))
                          :actorId (or (some-> ctx (aget "membership") (aget "actorId"))
                                       (some-> ctx (aget "actor") (aget "id")))
                          :userId (some-> ctx (aget "user") (aget "id"))
                          :email (or (aget session-options "email")
                                     (some-> ctx (aget "user") (aget "email"))
                                     "")
                          :orgSlug (some-> ctx (aget "org") (aget "slug"))
                          :orgId (some-> ctx (aget "org") (aget "id"))
                          :displayName (or (aget session-options "displayName")
                                           (some-> ctx (aget "user") (aget "displayName"))
                                           (some-> ctx (aget "user") (aget "email"))
                                           "")
                          :authProvider (or (aget session-options "authProvider") "local")
                          :_rawToken raw-token
                          :createdAt (.toISOString (js/Date.))}]
    (-> (store-session session-id session-data)
        (.then (fn [_]
                 (set-session-cookie reply raw-token public-base-url)
                 #js {:ok true
                      :sessionId session-id
                      :user (aget ctx "user")
                      :actor (aget ctx "actor")
                      :org (aget ctx "org")
                      :membership (aget ctx "membership")})))))

(defn- check-whitelist-and-session!
  "Check if email is whitelisted; if so, create session and redirect, otherwise redirect to invite page."
  [policyDb reply gh-user email state-entry public-base-url]
  (-> (ensure-user-membership! policyDb gh-user email)
      (.then (fn [_] true))
      (.catch (fn [_] false))
      (.then
       (fn [whitelisted]
         (if (not whitelisted)
           ;; Not whitelisted — redirect to invite page
           (let [invite-url (js/URL. "/login" public-base-url)]
             (.set (.-searchParams invite-url) "error" "not_whitelisted")
             (.set (.-searchParams invite-url) "email" email)
             (.set (.-searchParams invite-url) "github_login" (or (aget gh-user "login") ""))
             (.redirect reply (.toString invite-url)))
           ;; Whitelisted — upsert and create session
           (create-session-and-redirect!
            policyDb reply gh-user email state-entry public-base-url))))))

;; --- Main callback handler -------------------------------------------------

(defn handle-github-callback
  [policyDb reply client-id client-secret state-entry code public-base-url]
  (-> (exchange-github-code client-id client-secret code)
      (.then
        (fn [access-token]
          (-> (gh-json "https://api.github.com/user" access-token)
              (.then
                (fn [gh-user]
                  (if-not (aget gh-user "id")
                    (throw (js/Error. "GitHub user lookup failed"))
                    (-> (get-github-user-emails access-token)
                        (.then
                          (fn [email]
                            (if-not email
                              (throw (js/Error. "Could not retrieve GitHub email"))
                              (check-whitelist-and-session!
                                policyDb reply gh-user email
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
  [_policyDb]
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
                session-id (some-> payload (aget "sid"))]
            (when session-id
              (-> (load-session session-id cookie-token)
                  (.then
                   (fn [session-data]
                     (if-not session-data
                       (clear-session-cookie reply (or (aget (.-env js/process) "KNOXX_PUBLIC_BASE_URL") "http://localhost"))
                       (do
                         (aset headers "x-knoxx-user-email" (aget session-data "email"))
                         (when (aget session-data "orgSlug")
                           (aset headers "x-knoxx-org-slug" (aget session-data "orgSlug")))
                         (when (aget session-data "membershipId")
                           (aset headers "x-knoxx-membership-id" (aget session-data "membershipId"))))))
                  (.catch (fn [_] nil)))))))))))


(defn resolve-auth-context
  [req policyDb]
  (let [header-email (str/trim (or (aget (.-headers req) "x-knoxx-user-email") ""))
        header-mid (str/trim (or (aget (.-headers req) "x-knoxx-membership-id") ""))]
    (cond
      (or (not (str/blank? header-email)) (not (str/blank? header-mid)))
      (.resolveRequestContext policyDb (.-headers req))

      (valid-api-key-request? req)
      (ensure-api-key-membership! policyDb)

      :else
      (resolve-cookie-auth-context req policyDb))))
