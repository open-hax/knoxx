(ns knoxx.frontend.auth.api
  "Auth REST calls. CLJS port of the fetches in src/pages/AuthContext.tsx,
   LoginPage.tsx and SignupPage.tsx. Responses stay RAW JS objects — the
   auth context value is consumed by both TS and CLJS through interop."
  (:require [clojure.string :as str]))

(defn- json-or-throw [^js res fallback]
  (if (.-ok res)
    (.json res)
    (-> (.json res)
        (.catch (fn [_] #js {:error (or (.-statusText res) fallback)}))
        (.then (fn [^js body]
                 (throw (js/Error. (or (.-error body) (.-code body)
                                       (str (.-status res))))))))))

(defn- request-js
  ([path] (request-js path nil))
  ([path body]
   (let [init #js {:credentials "include"
                   :headers #js {"Content-Type" "application/json"}}]
     (when body
       (set! (.-method init) "POST")
       (set! (.-body init) (js/JSON.stringify (clj->js body))))
     (-> (js/fetch path init)
         (.then #(json-or-throw % "Request failed"))))))

(defn fetch-auth-context []
  (request-js "/api/auth/context"))

(defn fetch-auth-config []
  (-> (js/fetch "/api/auth/config")
      (.then (fn [^js res] (.json res)))))

(defn local-login [email password]
  (request-js "/api/auth/local/login" {:email (str/trim email) :password password}))

(defn redeem-invite [code email]
  (request-js "/api/auth/invite/redeem" {:code (str/trim code) :email (str/trim email)}))

(defn signup [email display-name password]
  (request-js "/api/auth/signup" {:email (str/trim email)
                                  :displayName (or (not-empty (str/trim display-name))
                                                   (str/trim email))
                                  :password password}))

(defn logout []
  (-> (request-js "/api/auth/logout" {})
      (.catch (fn [_] nil))))
