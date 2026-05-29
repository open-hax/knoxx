(ns knoxx.backend.infra.routes.auth
  (:require [clojure.string :as str]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.auth.session :as auth-session]
            [knoxx.backend.infra.db.policy :as policy-db]))

(defn- body-map
  [req]
  (js->clj (or (aget req "body") #js {}) :keywordize-keys true))

(defn- map-value
  [m & ks]
  (some #(get m %) ks))


(defn- register-auth-config-route!
  [^js app public-base-url github-enabled]
  (.get app "/api/auth/config"
        (fn [_req ^js reply]
          (.send reply (clj->js {:githubEnabled github-enabled
                                 :publicBaseUrl public-base-url
                                 :loginUrl (when github-enabled "/api/auth/login")})))))

(defn- register-signup-route!
  [^js app policy-context public-base-url]
  (.post app "/api/auth/signup"
         (fn [^js req ^js reply]
           (if-not policy-context
             (.send (.code reply 503) (clj->js {:error "Knoxx policy database is not configured"}))
             (let [body (body-map req)
                   email (str/lower-case (str/trim (str (or (:email body) ""))))
                   display-name (str/trim (str (or (map-value body :display-name :displayName :display_name)
                                                   email)))]
               (if (str/blank? email)
                 (.send (.code reply 400) (clj->js {:error "email is required"}))
                 (-> (policy-db/ensure-self-org! (policy-db/context-pool policy-context)
                                                 email
                                                 (or display-name email))
                     (.then (fn [org]
                              (-> (policy-db/create-user-for-context!
                                   policy-context
                                   {:email email
                                    :display-name (or display-name email)
                                    :org-id (:id org)
                                    :role-slugs ["basic-user"]
                                    :auth-provider "signup"
                                    :status "active"
                                    :membership-status "active"
                                    :is-default true})
                                  (.then (fn [_]
                                           (policy-db/resolve-context!
                                            policy-context
                                            {"x-knoxx-user-email" email
                                             "x-knoxx-org-slug" (:slug org)})))
                                  (.then (fn [ctx]
                                           (auth-session/create-session-from-context!
                                            reply public-base-url ctx
                                            {:email email
                                             :display-name (or display-name email)
                                             :auth-provider "signup"}))))))
                     (.then (fn [result]
                              (.send reply (clj->js result))))
                     (.catch (fn [err]
                               (.send (.code reply (or (.-statusCode err) (.-status err) 500))
                                      (clj->js {:error (or (.-message err) "Signup failed")})))))))))))

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

(defn- register-logout-route!
  [^js app public-base-url]
  (.post app "/api/auth/logout"
         (fn [^js req ^js reply]
           (let [cookie-token (some-> req (aget "cookies") (aget auth-session/COOKIE-NAME))]
             (when cookie-token
               (let [payload (auth-session/verify-token cookie-token)]
                 (when-let [session-id (:sid payload)]
                   (.catch (auth-session/delete-session session-id cookie-token) (fn [_])))))
             (auth-session/clear-session-cookie reply public-base-url)
             (.send reply (clj->js {:ok true}))))))

(defn- register-invite-redeem-route!
  [^js app policy-context public-base-url]
  (.post app "/api/auth/invite/redeem"
         (fn [^js req ^js reply]
           (let [body (body-map req)
                 code (str/trim (str (or (:code body) "")))
                 email (str/lower-case
                        (str/trim (str (or (:email body)
                                           (aget (.-headers req) "x-knoxx-user-email")
                                           ""))))]
             (if (str/blank? code)
               (.send (.code reply 400) (clj->js {:error "Invite code is required"}))
               (if (str/blank? email)
                 (.send (.code reply 400) (clj->js {:error "email is required"}))
                 (-> (policy-db/redeem-invite! (policy-db/context-pool policy-context) code email)
                     (.then (fn [result]
                              (let [invite (:invite result)]
                                (-> (policy-db/resolve-context!
                                     policy-context
                                     {"x-knoxx-user-email" (:email invite)
                                      "x-knoxx-org-id" (:org-id invite)})
                                    (.then (fn [ctx]
                                             (auth-session/create-session-from-context!
                                              reply public-base-url ctx
                                              {:email (:email invite)
                                               :display-name (:email invite)
                                               :auth-provider "invite"})))
                                    (.then (fn [session]
                                             (.send reply (clj->js (assoc session
                                                                          :ok true
                                                                          :invite invite))))))))
                     (.catch (fn [err]
                               (.send (.code reply (or (.-status err) 500))
                                      (clj->js {:error (or (.-message err) "Invite redemption failed")}))))))))))))

(defn- register-invite-create-route!
  [^js app policy-context runtime public-base-url]
  (.post app "/api/auth/invite"
         (fn [^js req ^js reply]
           (let [body (body-map req)]
             (-> (auth-session/resolve-auth-context req policy-context)
                 (.then (fn [ctx]
                          (let [org-id (or (map-value body :org-id :orgId :org_id)
                                           (get-in ctx [:org :id]))
                                email (:email body)
                                role-slugs (vec (or (map-value body :role-slugs :roleSlugs :role_slugs)
                                                    ["basic-user"]))]
                            (if (str/blank? email)
                              (.send (.code reply 400) (clj->js {:error "email is required"}))
                              (do
                                (authz/ensure-org-scope! ctx org-id "org.users.invite")
                                (-> (policy-db/create-invite-for-context!
                                     policy-context
                                     {:org-id org-id
                                      :email email
                                      :role-slugs role-slugs
                                      :inviter-membership-id (get-in ctx [:membership :id])})
                                    (.then (fn [result]
                                             (when (not= (map-value body :send-email :sendEmail :send_email) false)
                                               (.catch (auth-session/send-invite-email runtime (:invite result) email public-base-url)
                                                       (fn [err]
                                                         (.error js/console "[knoxx-session] Failed to send invite email:" (.-message err)))))
                                             (.send reply (clj->js {:ok true
                                                                    :invite (:invite result)}))))
                                    (.catch (fn [err]
                                              (.send (.code reply (or (.-status err) 500))
                                                     (clj->js {:error (or (.-message err) "Invite creation failed")})))))))))
                 (.catch (fn [err]
                           (.send (.code reply (or (.-status err) 401))
                                  (clj->js {:error (or (.-message err) "Unauthorized")}))))))))))

(defn- register-invite-list-route!
  [^js app policy-context]
  (.get app "/api/auth/invites"
        (fn [^js req ^js reply]
          (-> (auth-session/resolve-auth-context req policy-context)
              (.then (fn [ctx]
                       (let [org-id (or (some-> req (aget "query") (aget "orgId"))
                                        (get-in ctx [:org :id]))
                             status (some-> req (aget "query") (aget "status"))]
                         (authz/ensure-org-scope! ctx org-id "org.users.invite")
                         (-> (policy-db/list-invites!
                              (policy-db/context-pool policy-context)
                              (cond-> {:org-id org-id}
                                status (assoc :status status)))
                             (.then (fn [result]
                                      (.send reply (clj->js result))))
                             (.catch (fn [err]
                                       (.send (.code reply 500)
                                              (clj->js {:error (.-message err)}))))))))
              (.catch (fn [err]
                        (.send (.code reply (or (.-status err) 401))
                               (clj->js {:error (or (.-message err) "Unauthorized")}))))))))

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
    (register-login-route! app public-base-url github-enabled client-id)
    (register-github-callback-route! app policy-context public-base-url github-enabled client-id client-secret)
    (register-logout-route! app public-base-url)
    (register-invite-redeem-route! app policy-context public-base-url)
    (register-invite-create-route! app policy-context runtime public-base-url)
    (register-invite-list-route! app policy-context)))
