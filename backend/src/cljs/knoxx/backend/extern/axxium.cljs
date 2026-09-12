(ns knoxx.backend.extern.axxium
  "Knoxx's named native boundary for the Axxium-owned identity plugin."
  (:require [axxium.api :as api]
            [axxium.extern.identity-http :as identity-http]
            [knoxx.backend.extern.fastify :as fastify]
            [knoxx.backend.extern.node-env :as environment]))

(defn options
  "Read explicit identity provider settings without exposing native environment data."
  [config]
  (let [env environment/variable
        directory (or (env "KNOXX_AXXIUM_DIRECTORY") (env "AXXIUM_EDN_DIRECTORY")
                      (str (:wiki-directory config ".knoxx/wiki") "/identity"))
        origin (or (env "KNOXX_PUBLIC_BASE_URL") (:public-base-url config) "http://localhost")
        oauth (fn [prefix]
                {:client-id (or (env (str "KNOXX_" prefix "_OAUTH_CLIENT_ID"))
                                (env (str prefix "_OAUTH_CLIENT_ID")))
                 :client-secret (or (env (str "KNOXX_" prefix "_OAUTH_CLIENT_SECRET"))
                                    (env (str prefix "_OAUTH_CLIENT_SECRET")))})]
    {:provider :edn :directory directory :public-base-url origin
     :providers {:github (oauth "GITHUB") :discord (oauth "DISCORD") :google (oauth "GOOGLE")
                 :atproto {:client-id (or (env "KNOXX_ATPROTO_OAUTH_CLIENT_ID")
                                         (env "ATPROTO_OAUTH_CLIENT_ID"))}}}))

(defn bootstrap-options
  "Resolve explicit server-managed administrator credentials; never infer account ownership."
  [options]
  {:username (or (environment/variable "KNOXX_BOOTSTRAP_SYSTEM_ADMIN_USERNAME") "admin")
   :email (:bootstrapSystemAdminEmail options)
   :password (:bootstrapSystemAdminPassword options)
   :display-name (:bootstrapSystemAdminName options)
   :principal-id (or (environment/variable "KNOXX_BOOTSTRAP_AXXIUM_PRINCIPAL_ID")
                     "axxium_bootstrap_system_admin")})

(defn request-data
  "Reject malformed or mixed credentials instead of falling back to a cookie."
  [request]
  (try
    (assoc (identity-http/request-data request) :method (fastify/request-method request))
    (catch :default cause
      (throw (ex-info "Invalid authentication credentials"
                      {:status 401 :code "invalid_credentials"} cause)))))

(defn require-origin!
  "Cookie-authenticated mutations must name the configured origin."
  [request origin]
  (when-not (contains? #{"GET" "HEAD" "OPTIONS"} (:method request))
    (try (identity-http/csrf! request origin)
         (catch :default cause
           (throw (ex-info "Request origin is not allowed" {:status 403 :code "forbidden_origin"} cause)))))
  request)

(defn ^:async register!
  "Register Axxium's public composition API; Knoxx owns no OAuth implementation."
  [app service]
  (await (api/register-routes app service)))

(defn register-context!
  "Adapt one fresh context query to Fastify without exposing authentication secrets."
  [app path resolve-context]
  (.get app path
        (fn ^:async context-handler [request reply]
          (try
            (let [context (await (resolve-context request))]
              (.header reply "Cache-Control" "no-store")
              (fastify/send-json! reply 200
                                  (select-keys context [:user :org :membership :actor :roles :role-slugs
                                                        :permissions :tool-policies :membership-tool-policies])))
            (catch :default cause
              (fastify/send-json! reply (fastify/error-status cause 500)
                                  {:error "Authentication required" :code (fastify/error-code cause)}))))))

(defn session-hook
  "Create a native mutation-origin hook with a defined request-data boundary."
  [origin]
  (fn ^:async enforce-origin [request _reply]
    (require-origin! (request-data request) origin)
    js/undefined))

(defn clear-cookie!
  "Expire the Axxium browser cookie through its owning adapter."
  [reply]
  (identity-http/clear-session! reply))

(defn ^:async handler-result
  "Normalize an already-sent nil handler result to native no-payload."
  [value]
  (let [result (await value)]
    (if (nil? result) js/undefined result)))
