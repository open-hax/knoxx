(ns knoxx.backend.infra.stores.clio-mcp-oauth
  "Hash-only canonical OAuth facts with atomic code exchange and explicit revocation."
  (:require [knoxx.backend.domain.mcp-oauth-store :as domain]
            [knoxx.backend.extern.mcp-oauth-store :as host]
            [knoxx.backend.infra.clio-application-store :as clio]
            [knoxx.backend.law.mcp-oauth-store :as law]
            [knoxx.backend.shape.mcp-oauth-store :as protocol]))

(def ^:private reads #{:oauth/client :oauth/code :oauth/token :oauth/tokens})
(def ^:private writes #{:oauth/register :oauth/issue-code :oauth/issue-token :oauth/exchange
                       :oauth/consume-code :oauth/revoke-code :oauth/revoke-token})

(defn- projection []
  (let [state (atom domain/empty-state)] {:store state :snapshot #(deref state)}))

(defn- read-method [operation]
  (fn [state arguments] (domain/read-operation @state operation arguments)))

(defn- write-method [operation]
  (fn [state arguments]
    (let [[next-state result] (domain/transition @state operation arguments)]
      (reset! state next-state) result)))

(defn- before-append! [{:operation/keys [method args result]}]
  ;; This is a preparation-time clock check, before Clio obtains its append lock.
  ;; Replay must use the accepted clock and never reevaluate historic expiry.
  (let [input (first args)
        expiry (case method
                 (:oauth/issue-code :oauth/issue-token) (+ (:now input) (* 1000 (:ttl input)))
                 (:oauth/exchange :oauth/consume-code) (:expires-at result)
                 nil)]
    (when expiry
      (law/require! (> expiry (host/now-ms)) :mcp-oauth-expired-before-admission))))

(defrecord ClioMcpOAuthStore [engine]
  protocol/IMcpOAuthStore
  (oauth-read! [_ operation arguments] (clio/read! engine operation [arguments]))
  (oauth-admit! [_ operation arguments] (clio/write! engine operation [arguments])))

(defn open!
  "Open the explicit OAuth directory; malformed history is fatal, with no fallback."
  [{:keys [directory]}]
  (->ClioMcpOAuthStore
   (clio/open! {:directory directory :stream "knoxx/mcp-oauth" :projection projection
                :reads (into {} (map #(vector % (read-method %))) reads)
                :writes (into {} (map #(vector % (write-method %))) writes)
                :before-append before-append!})))
