(ns knoxx.backend.law.mcp-oauth-store
  "Closed, secret-free OAuth facts and finite operation contracts."
  (:require [clojure.string :as str]
            [malli.core :as m]))

(def Text [:and :string [:fn #(not (str/blank? %))]])
(def Digest [:re #"[0-9a-f]{64}"])
(def Client
  [:map {:closed true}
   [:client_id Text] [:client_name {:optional true} Text]
   [:redirect_uris [:vector {:min 1} Text]]
   [:token_endpoint_auth_method [:= "none"]]
   [:grant_types [:= ["authorization_code"]]] [:response_types [:= ["code"]]]
   [:created_at {:optional true} Text]])
(def Grant
  [:map {:closed true}
   [:clientId Text] [:membershipId Text] [:axxiumPrincipalId Text] [:axxiumEntityId Text]
   [:orgSlug Text] [:tools [:vector {:min 1} Text]] [:createdAt Text]
   [:userEmail {:optional true} Text] [:actorId {:optional true} Text]])
(def Code
  (into [:map {:closed true}] (concat (drop 2 Grant)
    [[:redirectUri Text] [:codeChallenge Text] [:codeChallengeMethod [:= "S256"]]])))
(def Token (conj Grant [:expiresAt Text]))

(defn require!
  "Fail closed with a stable public error code."
  [condition code]
  (when-not condition
    (throw (ex-info (name code) {:status 409 :code (name code)}))))

(defn validate!
  "Validate closed provider data without printing credential-bearing input."
  [schema value]
  (when-not (m/validate schema value)
    (throw (ex-info "Invalid MCP OAuth storage input" {:status 400 :code "mcp_oauth_invalid_input"})))
  value)

(defn live?
  "Expired, consumed and revoked credentials never authorize a read."
  [record now]
  (and (= :active (:status record)) (> (:expires-at record) now)))

(defn credential!
  "Validate the secret-free envelope presented for durable admission."
  [kind {:keys [id payload now ttl] :as input}]
  (validate! [:map {:closed true} [:id Digest] [:payload (if (= kind :codes) Code Token)]
              [:now :int] [:ttl [:int {:min 1 :max 31536000}]]] input)
  (require! (and (pos? now) (pos? ttl) (seq id) (seq payload)) :mcp-oauth-invalid-clock)
  input)
