(ns knoxx.frontend.auth.api
  "Axxium authentication HTTP boundary; compatibility context remains native JS."
  (:require [clojure.string :as str]))

(defn- ^:async request-js
  ([path] (await (request-js path nil)))
  ([path body]
   (let [init #js {:credentials "include" :headers #js {"Content-Type" "application/json"}}]
     (when (some? body)
       (set! (.-method init) "POST")
       (set! (.-body init) (js/JSON.stringify (clj->js body))))
     (let [^js response (await (js/fetch path init))
           ^js result (try (await (.json response))
                       (catch :default _ (throw (js/Error. "Authentication response was not JSON"))))]
       (when-not (.-ok response)
         (throw (doto (js/Error. (or (.-error result) (.-code result) (str (.-status response))))
                  (aset "code" (.-code result)) (aset "status" (.-status response)))))
       result))))

(defn fetch-auth-context "Read the verified compatibility authorization context." [] (request-js "/api/auth/context"))
(defn fetch-auth-config "Read the live authentication method registry." [] (request-js "/api/auth/config"))
(defn local-login "Authenticate an exact username or email." [identifier password]
  (request-js "/api/auth/local/login" {:identifier (str/trim identifier) :password password}))
(defn redeem-invite "Preserve the advertised legacy invitation flow." [code email]
  (request-js "/api/auth/invite/redeem" {:code (str/trim code) :email (str/trim email)}))
(defn signup "Create distinct username and email aliases under Axxium." [username email display-name password]
  (request-js "/api/auth/signup" {:username (str/trim username) :email (str/trim email)
                                  :display-name (or (not-empty (str/trim display-name)) (str/trim username))
                                  :password password}))
(defn logout "End the browser session; refusals are not swallowed." [] (request-js "/api/auth/logout" {}))
(defn ^:async config-data "Decode advertised methods." [] (js->clj (await (fetch-auth-config)) :keywordize-keys true))
(defn ^:async command "Submit a named Axxium credential ceremony." [method payload]
  (when-not (contains? #{"pgp/challenge" "pgp/enroll" "pgp/verify" "passkey/registration-options"
                         "passkey/registration-verify" "passkey/authentication-options" "passkey/authentication-verify"} method)
    (throw (js/Error. "Unsupported credential ceremony")))
  (js->clj (await (request-js (str "/api/auth/" method) payload)) :keywordize-keys true))

(defn- credential-endpoint! [config field expected]
  (when-not (= expected (get config field))
    (throw (js/Error. "Credential management is not advertised by this instance.")))
  expected)
(defn- public-credential? [credential]
  (and (map? credential)
       (every? #(and (string? (get credential %)) (not (str/blank? (get credential %)))) [:id :method :label])
       (boolean? (:revocable credential))
       (or (nil? (:blockedReason credential)) (string? (:blockedReason credential)))))
(defn ^:async credential-inventory "Reject malformed metadata instead of inventing an empty inventory." [config]
  (let [result (js->clj (await (request-js (credential-endpoint! config :credentialListUrl "/api/auth/credentials"))) :keywordize-keys true)]
    (when-not (and (vector? (:credentials result)) (every? public-credential? (:credentials result))
                   (boolean? (:reauthenticationRequired result)))
      (throw (js/Error. "Axxium returned an invalid credential inventory.")))
    result))
(defn ^:async revoke-credential "Revoke exactly one binding through the advertised local endpoint." [config id]
  (when-not (and (string? id) (not (str/blank? id))) (throw (js/Error. "Select a sign-in credential to revoke.")))
  (let [result (js->clj (await (request-js (credential-endpoint! config :credentialRevokeUrl "/api/auth/credentials/revoke")
                                          {:credentialId id})) :keywordize-keys true)]
    (when-not (and (true? (:ok result)) (true? (:reauthenticate result)))
      (throw (js/Error. "Credential revocation was not confirmed by Axxium.")))
    result))
(defn link-initiation-url "Admit only the selected provider's same-origin POST endpoint." [method origin]
  (when-not (and (:available method) (= "POST" (:linkMethod method))
                 (contains? #{"github" "discord" "google" "atproto"} (:id method))
                 (string? (:linkUrl method)) (not (str/blank? (:linkUrl method))))
    (throw (js/Error. "Account linking is not available for this provider.")))
  (let [url (js/URL. (:linkUrl method) origin)]
    (when-not (and (= origin (.-origin url)) (= (str "/api/auth/providers/" (:id method) "/link") (.-pathname url))
                   (empty? (.-username url)) (empty? (.-password url)) (empty? (.-search url)) (empty? (.-hash url)))
      (throw (js/Error. "Account linking must use this site's provider endpoint.")))
    (.-href url)))
(defn authorization-destination "Validate an HTTPS issuer or explicit HTTP loopback development issuer." [value]
  (when-not (and (string? value) (not (str/blank? value))) (throw (js/Error. "The identity provider returned no authorization URL.")))
  (let [url (js/URL. value)]
    (when-not (and (empty? (.-username url)) (empty? (.-password url))
                   (or (= "https:" (.-protocol url)) (and (= "http:" (.-protocol url))
                    (contains? #{"localhost" "127.0.0.1" "[::1]"} (.-hostname url)))))
      (throw (js/Error. "The identity provider returned an unsafe authorization URL.")))
    (.-href url)))
(defn ^:async link-provider "POST authenticated linking before navigating to its validated authorization URL." [method handle]
  (let [url (link-initiation-url method (.-origin js/window.location)) handle (str/trim (or handle ""))]
    (when (and (= "atproto" (:id method)) (str/blank? handle)) (throw (js/Error. "Enter a Bluesky handle or DID to link that identity.")))
    (let [^js response (await (request-js url (cond-> {:redirect "/account"} (= "atproto" (:id method)) (assoc :handle handle))))]
      (authorization-destination (.-authorizationUrl response)))))
