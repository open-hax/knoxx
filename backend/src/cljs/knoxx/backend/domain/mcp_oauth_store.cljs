(ns knoxx.backend.domain.mcp-oauth-store
  "Pure immutable credential admission, single use and scoped revocation."
  (:require [knoxx.backend.law.mcp-oauth-store :as law]))

(def empty-state {:clients {} :codes {} :tokens {}})

(defn- admit-client [state {:keys [id payload] :as input}]
  (law/validate! [:map {:closed true} [:id law/Text] [:payload law/Client]] input)
  (law/require! (= id (:client_id payload)) :mcp-oauth-client-mismatch)
  (if-let [existing (get-in state [:clients id])]
    (do (law/require! (= existing payload) :mcp-oauth-client-collision) [state true])
    [(assoc-in state [:clients id] payload) true]))

(defn- admit-credential [state kind {:keys [id payload now ttl] :as input}]
  (law/credential! kind input)
  (law/require! (contains? (:clients state) (:clientId payload)) :mcp-oauth-client-missing)
  (if-let [existing (get-in state [kind id])]
    (do (law/require! (= [payload ttl] [(:payload existing) (:ttl existing)])
                      :mcp-oauth-credential-collision)
        ;; Never reactivate a consumed code or extend the first admitted expiry.
        [state true])
    [(assoc-in state [kind id] {:payload payload :ttl ttl :expires-at (+ now (* ttl 1000))
                               :status :active}) true]))

(defn- consume [state {:keys [id now] :as input}]
  (law/validate! [:map {:closed true} [:id law/Digest] [:now :int]] input)
  (let [record (get-in state [:codes id])]
    (if (law/live? record now)
      [(assoc-in state [:codes id :status] :consumed) (select-keys record [:payload :expires-at])]
      [state nil])))

(defn- revoke [state kind {:keys [id membership-id] :as input}]
  (law/validate! [:map {:closed true} [:id law/Digest]
                  [:membership-id {:optional true} law/Text]] input)
  (let [record (get-in state [kind id])]
    (if (and record (= :active (:status record))
             (or (nil? membership-id) (= membership-id (get-in record [:payload :membershipId]))))
      [(assoc-in state [kind id :status] :revoked) true] [state false])))

(defn- exchange [state {:keys [code-id expected token] :as input}]
  (law/validate! [:map {:closed true} [:code-id law/Digest] [:expected law/Code] [:token :map]] input)
  (law/credential! :tokens token)
  (let [record (get-in state [:codes code-id])
        copied [:clientId :membershipId :axxiumPrincipalId :axxiumEntityId :orgSlug :tools :userEmail :actorId]]
    (if-not (law/live? record (:now token)) [state nil]
      (do
        (law/require! (= expected (:payload record)) :mcp-oauth-code-changed)
        (law/require! (= (select-keys expected copied) (select-keys (:payload token) copied))
                      :mcp-oauth-grant-changed)
        (law/require! (not (contains? (:tokens state) (:id token))) :mcp-oauth-credential-collision)
        (let [[issued _] (admit-credential state :tokens token)]
          [(assoc-in issued [:codes code-id :status] :consumed)
           {:accepted? true :expires-at (:expires-at record)}])))))

(defn transition
  "Return [next-state result] for an explicitly named provider command."
  [state operation arguments]
  (case operation
    :oauth/register (admit-client state arguments)
    :oauth/issue-code (admit-credential state :codes arguments)
    :oauth/issue-token (admit-credential state :tokens arguments)
    :oauth/exchange (exchange state arguments)
    :oauth/consume-code (consume state arguments)
    :oauth/revoke-code (revoke state :codes arguments)
    :oauth/revoke-token (revoke state :tokens arguments)
    (throw (ex-info "Unsupported OAuth command" {:code "mcp_oauth_unknown_operation"}))))

(defn read-operation
  "Read only active credentials or safe membership-scoped metadata."
  [state operation {:keys [id now membership-id] :as arguments}]
  (case operation
    :oauth/client
    (do (law/validate! [:map {:closed true} [:id law/Text]] arguments)
        (get-in state [:clients id]))
    (:oauth/code :oauth/token)
    (do (law/validate! [:map {:closed true} [:id law/Digest] [:now :int]] arguments)
        (let [record (get-in state [(if (= operation :oauth/code) :codes :tokens) id])]
          (when (law/live? record now) (:payload record))))
    :oauth/tokens
    (do (law/validate! [:map {:closed true} [:membership-id law/Text] [:now :int]] arguments)
        (->> (:tokens state)
             (keep (fn [[token-id record]]
                     (when (and (law/live? record now)
                                (= membership-id (get-in record [:payload :membershipId])))
                       (assoc (:payload record) :tokenId token-id))))
             (sort-by :tokenId) vec))
    (throw (ex-info "Unsupported OAuth read" {:code "mcp_oauth_unknown_operation"}))))
