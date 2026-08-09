(ns knoxx.backend.infra.auth.method-config
  "Load the authentication contract for a surface and gather the facts law needs.

   law.auth-methods owns every decision; this namespace owns the effects those
   decisions need — reading the contract off disk, reading the token out of the
   environment the contract names, asking Fastify who the peer is — and the
   shape of the principal a non-OAuth method produces.

   That principal is deliberately indistinguishable downstream from a token
   record loaded out of Mongo. Nothing after this point knows which method
   admitted the request, which is the point: an e2e test exercises the shipped
   serving path rather than a parallel one that could pass while production
   fails."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.contracts.loader :as contracts]
            [knoxx.backend.extern.fastify :as fastify]
            [knoxx.backend.law.auth-methods :as law]))

(def mcp-surface :mcp)

(def ^:private mcp-contract-id "mcp_http")

(defn- env
  [k]
  (some-> (aget js/process.env k) str str/trim not-empty))

(defn- production?
  []
  (= "production" (some-> (env "NODE_ENV") str/lower-case)))

(defn contract-for
  "The authentication contract for `surface`, or nil.

   A missing or unreadable contract is nil rather than a throw: law refuses on
   nil, so the surface falls back to OAuth-only. Losing the ability to serve
   /mcp because a contract file was mid-edit would be a worse failure than
   ignoring it."
  [config surface]
  (when (= mcp-surface surface)
    (try
      (contracts/contract-sync config "authentication" mcp-contract-id)
      (catch :default err
        (.warn js/console "[knoxx-auth] authentication contract unreadable; OAuth only" err)
        nil))))

(defn method-enabled?
  "True when `surface` accepts `method-id` under the current contract."
  [config surface method-id]
  (law/method-enabled? (contract-for config surface) surface method-id))

(defn- request-facts
  [request method-token-env]
  {:configured-token (some-> method-token-env env)
   :presented-token  nil
   :remote-address   (fastify/request-remote-address request)
   :production?      (production?)})

(defn- token-env-for
  [contract surface]
  (some->> (law/enabled-methods contract surface)
           (some #(when (= :trusted-loopback (:auth-method/id %)) %))
           :auth-method/token-env))

(defn trusted-loopback-grant
  "The grant admitting this request under :trusted-loopback, or nil.

   Returns law's decision unchanged. The token is read from whichever env var
   the contract names, so which secret opens the door is part of the reviewable
   configuration rather than a constant compiled into this file."
  [config surface request presented-token]
  (let [contract (contract-for config surface)]
    (when-let [token-env (token-env-for contract surface)]
      (law/trusted-loopback-grant
       contract surface
       (assoc (request-facts request token-env) :presented-token presented-token)))))

(defn grant->token-record
  "A token record for `grant`, shaped like one minted through OAuth.

   `available-tool-names` is the catalog this request could reach; a :all grant
   resolves to it. The result still passes through granted-tools, which
   intersects it with what the resolved membership may use — so this widens
   consent, never authorization.

   membershipId is absent rather than blank: the context resolves by email
   here, and a blank id would be sent as a membership that matches nothing.
   actorId is present only when the contract names one, so a method that grants
   no actor produces a record call-actor-id treats as carrying none — and
   credential-backed tools then fail for want of an actor instead of borrowing
   whichever actor the membership happens to hold."
  [grant available-tool-names]
  (let [granted (if (= :all (:tools grant))
                  (vec available-tool-names)
                  (filterv (set available-tool-names) (:tools grant)))]
    (clj->js (cond-> {:accessToken "authentication-contract"
                      :clientId    "knoxx-authentication-contract"
                      :userEmail   (:user-email grant)
                      :tools       granted}
               (:org-slug grant) (assoc :orgSlug (:org-slug grant))
               (:actor-id grant) (assoc :actorId (:actor-id grant))))))

(defn announce!
  "Log once, at startup, whenever a surface accepts anything but OAuth.

   A silently-open authentication method is a bug even when every guard around
   it holds, and the contract is a file somebody has to think to open."
  [config surface]
  (doseq [method (law/enabled-methods (contract-for config surface) surface)
          :when (not= :oauth-bearer (:auth-method/id method))]
    (.warn js/console
           (str "[knoxx-auth] surface " surface " accepts " (:auth-method/id method)
                " — see contracts/authentication/" mcp-contract-id ".edn"
                (when-let [e (:auth-method/token-env method)] (str " (token from " e ")"))))))
