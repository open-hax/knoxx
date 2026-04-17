(ns knoxx.backend.auth-session
  (:refer-clojure :exclude [set])
  (:require [clojure.string :as str]
            ["node:crypto" :as crypto]
            ["redis" :as redis]))


(def ^:private session-secret-mem (atom nil))

(defn- session-secret
  []
  (or @session-secret-mem
      (let [env-secret (aget (.-env js/process) "KNOXX_SESSION_SECRET")
            secret (or (when (not (str/blank? env-secret)) env-secret)
                       (let [buf (.randomBytes crypto 32)]
                         (.toString buf "hex")))]
        (when (str/blank? env-secret)
          (.warn js/console "[knoxx-session] KNOXX_SESSION_SECRET not set; using ephemeral key. Sessions will not survive restarts."))
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


(defn- verify-token
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
  (-> (get-redis)
      (.then
       (fn [redis]
         (let [ttl (js/parseInt (or (aget (.-env js/process) "KNOXX_SESSION_TTL_SECONDS") "86400") 10)]
           (.set redis (str "knoxx:session:" session-id) (js/JSON.stringify (clj->js data))
                 (clj->js {:EX ttl})))))))

(defn- load-session
  [session-id]
  (-> (get-redis)
      (.then
       (fn [redis]
         (.get redis (str "knoxx:session:" session-id))))
      (.then
       (fn [raw]
         (when raw
           (try (js/JSON.parse raw)
                (catch :default _ nil)))))))

(defn- delete-session
  [session-id]
  (-> (get-redis)
      (.then (fn [redis] (.del redis (str "knoxx:session:" session-id))))))


(defn- exchange-github-code
  [client-id client-secret code]
  (-> (js/fetch "https://github.com/login/oauth/access_token"
                (clj->js {:method "POST"
                          :headers {:Content-Type "application/json"
                                    :Accept "application/json"}
                          :body (js/JSON.stringify
                                  #js {:client_id client-id
                                       :client_secret client-secret
                                       :code code})}))
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


(def ^:private COOKIE-NAME "knoxx_session")

(defn- secure-origin?
  [base-url]
  (try
    (= (.-protocol (js/URL. base-url)) "https:")
    (catch :default _ false)))

(defn- set-session-cookie
  [reply token base-url]
  (let [secure (secure-origin? base-url)
        ttl (js/parseInt (or (aget (.-env js/process) "KNOXX_SESSION_TTL_SECONDS") "86400") 10)]
    (.setCookie reply COOKIE-NAME token
                (clj->js {:path "/"
                          :httpOnly true
                          :secure secure
                          :sameSite (if secure "Strict" "Lax")
                          :maxAge ttl}))))

(defn- clear-session-cookie
  [reply base-url]
  (let [secure (secure-origin? base-url)]
    (.clearCookie reply COOKIE-NAME
                  (clj->js {:path "/"
                            :httpOnly true
                            :secure secure
                            :sameSite (if secure "Strict" "Lax")}))))


(def ^:private STATE-TTL 600)
(def ^:private pending-states (atom {}))

(defn- create-state
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

(defn- consume-state
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


(defn- handle-github-callback
  [policyDb reply client-id client-secret state-entry code state-val public-base-url]
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
                           ;; Check whitelist
                           (let [headers-like #js {"x-knoxx-user-email" email}]
                             (-> (.resolveRequestContext policyDb headers-like)
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
                                      (-> (.resolveRequestContext policyDb headers-like)
                                          (.then
                                           (fn [ctx]
                                             (let [mid (some-> ctx (aget "membership") (aget "id"))]
                                               (if mid
                                                 (js/Promise.resolve ctx)
                                                 (-> (.getBootstrapContext policyDb)
                                                     (.then
                                                      (fn [bc]
                                                        (.createUser policyDb
                                                                     #js {:email email
                                                                          :displayName (or (aget gh-user "name") (aget gh-user "login") email)
                                                                          :orgId (some-> bc (aget "primaryOrg") (aget "id"))
                                                                          :authProvider "github"
                                                                          :externalSubject (str "github:" (aget gh-user "id"))
                                                                          :roleSlugs #js ["knowledge_worker"]})
                                                     (.then (fn [_] (.resolveRequestContext policyDb headers-like)))))))))
                                          (.then
                                           (fn [fresh-ctx]
                                             (let [session-id (.randomUUID crypto)
                                                   session-data #js {:membershipId (some-> fresh-ctx (aget "membership") (aget "id"))
                                                                     :userId (some-> fresh-ctx (aget "user") (aget "id"))
                                                                     :email email
                                                                     :orgSlug (some-> fresh-ctx (aget "org") (aget "slug"))
                                                                     :orgId (some-> fresh-ctx (aget "org") (aget "id"))
                                                                     :displayName (or (aget gh-user "name") (aget gh-user "login") email)
                                                                     :githubLogin (aget gh-user "login")
                                                                     :githubId (aget gh-user "id")
                                                                     :authProvider "github"
                                                                     :createdAt (.toISOString (js/Date.))}]
                                               (-> (store-session session-id session-data)
                                                   (.then (fn [_] (sign-token #js {:sid session-id})))
                                                   (.then
                                                    (fn [token]
                                                      (set-session-cookie reply token public-base-url)
                                                      (.log js/console (str "[knoxx-session] GitHub login: " email))
                                                      (.redirect reply (.toString (js/URL. (:redirect state-entry) public-base-url)))))))))))))))))))))))))))
      (.catch
       (fn [err]
         (.error js/console "[knoxx-session] GitHub OAuth callback error:" (.-message err))
         (let [error-url (js/URL. "/login" public-base-url)]
           (.set (.-searchParams error-url) "error" "oauth_failed")
           (.set (.-searchParams error-url) "message" (.-message err))
           (.redirect reply (.toString error-url))))))


(defn register-auth-routes
  [app {:keys [policyDb runtime]}]
  (let [public-base-url (or (aget (.-env js/process) "KNOXX_PUBLIC_BASE_URL") "http://localhost")
        client-id (or (aget (.-env js/process) "KNOXX_GITHUB_OAUTH_CLIENT_ID") "")
        client-secret (or (aget (.-env js/process) "KNOXX_GITHUB_OAUTH_CLIENT_SECRET") "")
        github-enabled (and (not (str/blank? client-id)) (not (str/blank? client-secret)))]

    (.get app "/api/auth/config"
          (fn [_req reply]
            (.send reply (clj->js {:githubEnabled github-enabled
                                   :publicBaseUrl public-base-url
                                   :loginUrl (when github-enabled "/api/auth/login")}))))

    (.get app "/api/auth/login"
          (fn [req reply]
            (if-not github-enabled
              (.send (.code reply 503) (clj->js {:error "GitHub OAuth not configured"}))
              (let [redirect (str (or (some-> req (aget "query") (aget "redirect")) "/"))
                    state (create-state redirect)
                    callback-url (.toString (js/URL. "/api/auth/callback/github" public-base-url))
                    authorize-url (js/URL. "https://github.com/login/oauth/authorize")]
                (.set (.-searchParams authorize-url) "client_id" client-id)
                (.set (.-searchParams authorize-url) "redirect_uri" callback-url)
                (.set (.-searchParams authorize-url) "state" state)
                (.set (.-searchParams authorize-url) "scope" "read:user user:email")
                (.redirect reply (.toString authorize-url))))))

    (.get app "/api/auth/callback/github"
          (fn [req reply]
            (if-not github-enabled
              (.send (.code reply 503) (clj->js {:error "GitHub OAuth not configured"}))
              (let [code (str (or (some-> req (aget "query") (aget "code")) ""))
                    state-val (str (or (some-> req (aget "query") (aget "state")) ""))]
                (if (or (str/blank? code) (str/blank? state-val))
                  (.send (.code reply 400) (clj->js {:error "Missing code or state"}))
                  (if-let [state-entry (consume-state state-val)]
                    (handle-github-callback policyDb reply client-id client-secret state-entry code state-val public-base-url)
                    (.send (.code reply 400) (clj->js {:error "Invalid or expired state parameter"}))))))))

    (.post app "/api/auth/logout"
           (fn [req reply]
             (let [cookie-token (some-> req (aget "cookies") (aget COOKIE-NAME))]
               (when cookie-token
                 (let [payload (verify-token cookie-token)]
                   (when (and payload (aget payload "sid"))
                     (.catch (delete-session (aget payload "sid")) (fn [_])))))
               (clear-session-cookie reply public-base-url)
               (.send reply (clj->js {:ok true})))))

    (.post app "/api/auth/invite/redeem"
           (fn [req reply]
             (let [code (str/trim (str (or (some-> req (aget "body") (aget "code")) "")))]
               (if (str/blank? code)
                 (.send (.code reply 400) (clj->js {:error "Invite code is required"}))
                 (let [email (or (let [cookie-token (some-> req (aget "cookies") (aget COOKIE-NAME))]
                                   (when cookie-token
                                     (let [payload (verify-token cookie-token)]
                                       (when (and payload (aget payload "sid"))
                                         ;; sync only — can't await here in sync handler
                                         nil))))
                                 (str/trim (or (aget (.-headers req) "x-knoxx-user-email") "")))]
                   (if (str/blank? email)
                     (.send (.code reply 401) (clj->js {:error "Not authenticated"}))
                     (-> (.redeemInvite policyDb code email)
                         (.then
                          (fn [result]
                            (.send reply (clj->js {:ok true
                                                   :invite (aget result "invite")
                                                   :user (aget result "user")}))))
                         (.catch
                          (fn [err]
                            (.send (.code reply (or (.-status err) 500))
                                   (clj->js {:error (or (.-message err) "Invite redemption failed")})))))))))))

    (.post app "/api/auth/invite"
           (fn [req reply]
             (-> (resolve-auth-context req policyDb)
                 (.then
                  (fn [ctx]
                    (let [org-id (or (some-> req (aget "body") (aget "orgId"))
                                     (some-> ctx (aget "org") (aget "id")))
                          email (some-> req (aget "body") (aget "email"))
                          role-slugs (or (some-> req (aget "body") (aget "roleSlugs")) #js ["knowledge_worker"])]
                      (if (str/blank? email)
                        (.send (.code reply 400) (clj->js {:error "email is required"}))
                        (-> (.createInvite policyDb
                                           (clj->js {:orgId org-id
                                                     :email email
                                                     :roleSlugs role-slugs
                                                     :inviterMembershipId (some-> ctx (aget "membership") (aget "id"))}))
                            (.then
                             (fn [result]
                               (when (not= (some-> req (aget "body") (aget "sendEmail")) false)
                                 (.catch (send-invite-email runtime (aget result "invite") email public-base-url)
                                         (fn [err]
                                           (.error js/console "[knoxx-session] Failed to send invite email:" (.-message err)))))
                               (.send reply (clj->js {:ok true :invite (aget result "invite")}))))
                            (.catch
                             (fn [err]
                               (.send (.code reply (or (.-status err) 500))
                                      (clj->js {:error (or (.-message err) "Invite creation failed")})))))))))
                 (.catch
                  (fn [err]
                    (.send (.code reply (or (.-status err) 401))
                           (clj->js {:error (or (.-message err) "Unauthorized")})))))))

    (.get app "/api/auth/invites"
          (fn [req reply]
            (-> (resolve-auth-context req policyDb)
                (.then
                 (fn [ctx]
                   (let [org-id (or (some-> req (aget "query") (aget "orgId"))
                                    (some-> ctx (aget "org") (aget "id")))
                         status (some-> req (aget "query") (aget "status"))]
                     (-> (.listInvites policyDb (clj->js (cond-> {:orgId org-id}
                                                           status (assoc :status status))))
                         (.then (fn [result] (.send reply result)))
                         (.catch (fn [err]
                                   (.send (.code reply 500) (clj->js {:error (.-message err)}))))))))
                (.catch
                 (fn [err]
                   (.send (.code reply (or (.-status err) 401))
                          (clj->js {:error (or (.-message err) "Unauthorized")})))))))))


(defn create-session-hook
  [policyDb]
  (fn session-hook [req reply]
    (when (not (and (.startsWith (.-url req) "/api/auth/")
                    (not (.startsWith (.-url req) "/api/auth/context"))))
      (let [header-email (str/trim (or (aget (.-headers req) "x-knoxx-user-email") ""))
            header-mid (str/trim (or (aget (.-headers req) "x-knoxx-membership-id") ""))]
        (when (and (str/blank? header-email) (str/blank? header-mid))
          (let [cookie-token (some-> req (aget "cookies") (aget COOKIE-NAME))]
            (when cookie-token
              (let [payload (verify-token cookie-token)]
                (when (and payload (aget payload "sid"))
                  (-> (load-session (aget payload "sid"))
                      (.then
                       (fn [session-data]
                         (if-not session-data
                           (clear-session-cookie reply (or (aget (.-env js/process) "KNOXX_PUBLIC_BASE_URL") "http://localhost"))
                           (do
                             (aset (.-headers req) "x-knoxx-user-email" (aget session-data "email"))
                             (when (aget session-data "orgSlug")
                               (aset (.-headers req) "x-knoxx-org-slug" (aget session-data "orgSlug")))
                             (when (aget session-data "membershipId")
                               (aset (.-headers req) "x-knoxx-membership-id" (aget session-data "membershipId")))))))
                      (.catch (fn [_]))))))))))))


(defn resolve-auth-context
  [req policyDb]
  (let [header-email (str/trim (or (aget (.-headers req) "x-knoxx-user-email") ""))
        header-mid (str/trim (or (aget (.-headers req) "x-knoxx-membership-id") ""))]
    (if (or (not (str/blank? header-email)) (not (str/blank? header-mid)))
      (.resolveRequestContext policyDb (.-headers req))
      (let [cookie-token (some-> req (aget "cookies") (aget COOKIE-NAME))]
        (if-not cookie-token
          (js/Promise.reject (http-error 401 "Not authenticated" "no_session"))
          (let [payload (verify-token cookie-token)]
            (if-not (and payload (aget payload "sid"))
              (js/Promise.reject (http-error 401 "Invalid session token" "invalid_token"))
              (-> (load-session (aget payload "sid"))
                  (.then
                   (fn [session-data]
                     (if-not session-data
                       (js/Promise.reject (http-error 401 "Session expired" "session_expired"))
                       (let [headers #js {"x-knoxx-user-email" (aget session-data "email")
                                          "x-knoxx-org-slug" (aget session-data "orgSlug")}]
                         (when (aget session-data "membershipId")
                           (aset headers "x-knoxx-membership-id" (aget session-data "membershipId")))
                         (.resolveRequestContext policyDb headers)))))))))))))

