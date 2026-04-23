(ns knoxx.backend.routes.auth
  (:require [clojure.string :as str]
            [knoxx.backend.auth.session :as auth-session]))


(defn register-auth-routes
  ;; NOTE: Called from JS (server.mjs). `opts` may be a plain JS object.
  [app opts]
  (let [public-base-url (or (aget (.-env js/process) "KNOXX_PUBLIC_BASE_URL") "http://localhost")
        policyDb (or (when (map? opts) (:policyDb opts)) (aget opts "policyDb"))
        runtime (or (when (map? opts) (:runtime opts)) (aget opts "runtime"))
        client-id (or (aget (.-env js/process) "KNOXX_GITHUB_OAUTH_CLIENT_ID") "")
        client-secret (or (aget (.-env js/process) "KNOXX_GITHUB_OAUTH_CLIENT_SECRET") "")
        github-enabled (and (not (str/blank? client-id)) (not (str/blank? client-secret)))]

    ;; Wire persistent session store so auth_session can save/load sessions from Postgres
    (when policyDb (auth-session/set-db-session-store! policyDb))

    (.get app "/api/auth/config"
          (fn [_req reply]
            (.send reply (clj->js {:githubEnabled github-enabled
                                   :publicBaseUrl public-base-url
                                   :loginUrl (when github-enabled "/api/auth/login")}))))

    (.post app "/api/auth/signup"
           (fn [req reply]
             (if-not policyDb
               (.send (.code reply 503) (clj->js {:error "Knoxx policy database is not configured"}))
               (let [body (or (aget req "body") #js {})
                     email (str/lower-case (str/trim (str (or (aget body "email") ""))))
                     display-name (str/trim (str (or (aget body "displayName")
                                                     (aget body "display_name")
                                                     email)))]
                 (if (str/blank? email)
                   (.send (.code reply 400) (clj->js {:error "email is required"}))
                   (-> (.getBootstrapContext policyDb)
                       (.then (fn [bootstrap]
                                (let [primary-org (aget bootstrap "primaryOrg")
                                      org-id (aget primary-org "id")
                                      org-slug (aget primary-org "slug")]
                                  (-> (.createUser policyDb
                                                   #js {:email email
                                                        :displayName (or display-name email)
                                                        :orgId org-id
                                                        :roleSlugs #js ["basic_user"]
                                                        :authProvider "signup"
                                                        :status "active"
                                                        :membershipStatus "active"
                                                        :isDefault true})
                                      (.then (fn [_]
                                               (.resolveRequestContext policyDb
                                                                       #js {"x-knoxx-user-email" email
                                                                            "x-knoxx-org-slug" org-slug})))
                                      (.then (fn [ctx]
                                               (auth-session/create-session-from-context! reply public-base-url ctx
                                                                             #js {:email email
                                                                                  :displayName (or display-name email)
                                                                                  :authProvider "signup"}))))))
                       (.then (fn [result]
                                (.send reply result)))
                       (.catch (fn [err]
                                 (.send (.code reply (or (.-statusCode err) (.-status err) 500))
                                        (clj->js {:error (or (.-message err) "Signup failed")})))))))))

    (.get app "/api/auth/login"
          (fn [req reply]
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
                (.redirect reply (.toString authorize-url))))))

    (.get app "/api/auth/callback/github"
          (fn [req reply]
            (if-not github-enabled
              (.send (.code reply 503) (clj->js {:error "GitHub OAuth not configured"}))
              (let [code (str (or (some-> req (aget "query") (aget "code")) ""))
                    state-val (str (or (some-> req (aget "query") (aget "state")) ""))]
                (if (or (str/blank? code) (str/blank? state-val))
                  (.send (.code reply 400) (clj->js {:error "Missing code or state"}))
                  (if-let [state-entry (auth-session/consume-state state-val)]
                    (auth-session/handle-github-callback policyDb reply client-id client-secret state-entry code public-base-url)
                    (.send (.code reply 400) (clj->js {:error "Invalid or expired state parameter"}))))))))

    (.post app "/api/auth/logout"
           (fn [req reply]
             (let [cookie-token (some-> req (aget "cookies") (aget auth-session/COOKIE-NAME))]
               (when cookie-token
                 (let [payload (auth-session/verify-token cookie-token)]
                   (when (and payload (aget payload "sid"))
                     (.catch (auth-session/delete-session (aget payload "sid") cookie-token) (fn [_])))))
               (auth-session/clear-session-cookie reply public-base-url)
               (.send reply (clj->js {:ok true})))))

    (.post app "/api/auth/invite/redeem"
           (fn [req reply]
             (let [code (str/trim (str (or (some-> req (aget "body") (aget "code")) "")))]
               (if (str/blank? code)
                 (.send (.code reply 400) (clj->js {:error "Invite code is required"}))
                 (let [email (or (let [cookie-token (some-> req (aget "cookies") (aget auth-session/COOKIE-NAME))]
                                   (when cookie-token
                                     (let [payload (auth-session/verify-token cookie-token)]
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
             (-> (auth-session/resolve-auth-context req policyDb)
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
                                 (.catch (auth-session/send-invite-email runtime (aget result "invite") email public-base-url)
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
            (-> (auth-session/resolve-auth-context req policyDb)
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

    ))
