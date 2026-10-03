(ns knoxx.frontend.auth.api
  "Auth REST calls. CLJS port of the fetches in src/pages/AuthContext.tsx,
   LoginPage.tsx and SignupPage.tsx. Responses stay RAW JS objects — the
   auth context value is consumed by both TS and CLJS through interop."
  (:require [clojure.string :as str]))

(defn- ^:async json-or-throw [^js res fallback]
  (if (.-ok res)
    (await (.json res))
    (let [^js body (try
                     (await (.json res))
                     (catch :default _
                       #js {:error (or (.-statusText res) fallback)}))
          error (js/Error. (or (.-error body) (.-code body)
                               (str (.-status res))))]
      (set! (.-status error) (.-status res))
      (throw error))))

(defn- ^:async request-js
  ([path] (request-js path nil))
  ([path body]
   (let [init #js {:credentials "include"
                   :headers #js {"Content-Type" "application/json"}}]
     (when body
       (set! (.-method init) "POST")
       (set! (.-body init) (js/JSON.stringify (clj->js body))))
     (await (json-or-throw (await (js/fetch path init)) "Request failed")))))

(defn ^:async fetch-auth-context
  "An unsigned visitor has no context; other failures still reach the caller."
  []
  (let [^js res (await (js/fetch "/api/auth/context"
                                #js {:credentials "include"}))]
    (when-not (= 401 (.-status res))
      (await (json-or-throw res "Could not load authentication")))))

(defn ^:async fetch-auth-config
  "Load the identity provider and sign-in methods offered by Knoxx."
  []
  (let [^js res (await (js/fetch "/api/auth/config"))]
    (await (.json res))))

(defn local-login
  "Exchange a local or Axxium password for a Knoxx session."
  [email password]
  (request-js "/api/auth/local/login" {:email (str/trim email) :password password}))

(defn redeem-invite
  "Redeem an invitation for the supplied email."
  [code email]
  (request-js "/api/auth/invite/redeem" {:code (str/trim code) :email (str/trim email)}))

(defn signup
  "Create a local Knoxx account where local signup is enabled."
  [email display-name password]
  (request-js "/api/auth/signup" {:email (str/trim email)
                                  :displayName (or (not-empty (str/trim display-name))
                                                   (str/trim email))
                                  :password password}))

(defn ^:async logout
  "Clear the current session; report transport and server failures."
  []
  (await (request-js "/api/auth/logout" {})))
