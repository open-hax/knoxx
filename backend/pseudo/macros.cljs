;; macros.clj
(ns my.app.routes.macros)

(defmacro defroute-fn
  [fname {:keys [method path]} & body]
  (let [m (symbol (str "." method))]
    `(defn ~fname [app runtime config]
       (defroute app ~m ~path
         ~@body))))
```

```cljs
;; routes.cljs
(ns my.app.routes
  (:require-macros [my.app.routes.macros :refer [defroute-fn]]))

(defroute-fn register-mcp-http-routes!
  {:method "get"
   :path "/.well-known/oauth-authorization-server"}
  (let [issuer (js/URL. (.toString base))]
    (.send reply
           #js {:issuer (-> (.toString issuer)
                            (.replace (js/RegExp. "/$") ""))
                :authorization_endpoint (.toString (js/URL. "/api/mcp/oauth/authorize" issuer))
                :token_endpoint (.toString (js/URL. "/api/mcp/oauth/token" issuer))
                :registration_endpoint (.toString (js/URL. "/api/mcp/oauth/register" issuer))
                :response_types_supported #js ["code"]
                :grant_types_supported #js ["authorization_code"]
                :code_challenge_methods_supported #js ["S256"]
                :token_endpoint_auth_methods_supported #js ["none"]})))
