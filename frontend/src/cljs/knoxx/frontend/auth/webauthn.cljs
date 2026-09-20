(ns knoxx.frontend.auth.webauthn
  "Convert WebAuthn JSON at the browser credential boundary; private keys stay in the authenticator."
  (:require [clojure.string :as str]
            [knoxx.frontend.auth.api :as api]))
(defn decode-bytes "Decode base64url challenge and credential identifiers." [value]
  (let [text (str/replace (str/replace value "-" "+") "_" "/")
        padded (str text (apply str (repeat (mod (- 4 (mod (count text) 4)) 4) "=")))
        raw (js/atob padded)]
    (js/Uint8Array.from raw (fn [^js character] (.charCodeAt character 0)))))
(defn encode-bytes "Encode public authenticator response bytes as base64url." [value]
  (when value
    (-> (js/btoa (apply str (map #(js/String.fromCharCode %) (array-seq (js/Uint8Array. value)))))
        (str/replace "+" "-") (str/replace "/" "_") (str/replace #"=+$" ""))))
(defn public-options "Decode only WebAuthn binary option fields." [options]
  (cond-> (update options :challenge decode-bytes)
    (:user options) (update-in [:user :id] decode-bytes)
    (:excludeCredentials options) (update :excludeCredentials #(mapv (fn [c] (update c :id decode-bytes)) %))
    (:allowCredentials options) (update :allowCredentials #(mapv (fn [c] (update c :id decode-bytes)) %))))
(defn credential-response "Serialize public attestation or assertion for server verification." [^js credential]
  (let [^js response (.-response credential)]
    (cond-> {:id (.-id credential) :rawId (encode-bytes (.-rawId credential)) :type (.-type credential)
             :clientExtensionResults (js->clj (.getClientExtensionResults credential) :keywordize-keys true)
             :response (cond-> {:clientDataJSON (encode-bytes (.-clientDataJSON response))}
                         (.-attestationObject response) (assoc :attestationObject (encode-bytes (.-attestationObject response)))
                         (.-authenticatorData response) (assoc :authenticatorData (encode-bytes (.-authenticatorData response)))
                         (.-signature response) (assoc :signature (encode-bytes (.-signature response)))
                         (.-userHandle response) (assoc :userHandle (encode-bytes (.-userHandle response)))
                         (.-getTransports response) (assoc :transports (js->clj (.getTransports response))))}
      (.-authenticatorAttachment credential) (assoc :authenticatorAttachment (.-authenticatorAttachment credential)))))
(defn ^:async perform! "Run a real authenticator ceremony, then ask Axxium to verify its result." [enroll?]
  (when-not (some-> js/navigator .-credentials) (throw (js/Error. "Passkeys are unavailable in this browser.")))
  (let [prefix (if enroll? "passkey/registration" "passkey/authentication")
        challenge (await (api/command (str prefix "-options") {}))
        options #js {:publicKey (clj->js (public-options (:options challenge)))}
        credential (await (if enroll? (.create js/navigator.credentials options) (.get js/navigator.credentials options)))]
    (when-not credential (throw (js/Error. "The passkey ceremony did not return a credential.")))
    (await (api/command (str prefix "-verify") {:challenge-id (:challenge-id challenge) :response (credential-response credential)}))))
