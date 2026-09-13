(ns knoxx.backend.shape.local-policy
  "Structure-only administrative response morphisms over already resolved data.")

(defn membership
  "Preserve the established camelCase membership wire."
  [member org roles]
  (when member
    {:id (:id member) :userId (:user-id member) :orgId (:org-id member)
     :actorId (:actor-id member) :orgName (:name org) :orgSlug (:slug org)
     :status (:status member) :isDefault (:is-default member)
     :createdAt (:created-at member) :updatedAt (:updated-at member)
     :roles roles :toolPolicies (or (:tool-policies member) [])}))

(defn credential
  "Separate secret retrieval from safe directory summaries."
  [record include-secret?]
  (when record
    (cond-> {:id (:id record) :userId (:user-id record) :orgId (:org-id record)
             :principalId (:principal-id record)
             :actorId (:actor-id record) :provider (:provider record) :kind (:kind record)
             :accountIdentifier (:account-identifier record) :status (:status record)
             :configuredFields (vec (sort (map name (keys (:secret-json record)))))
             :createdAt (:created-at record) :updatedAt (:updated-at record)}
      include-secret? (assoc :secretJson (:secret-json record)))))

(defn user
  "Encode directory metadata and already scoped public member/credential responses."
  [row memberships credentials]
  {:id (:id row) :email (:email row) :username (:username row) :displayName (:display-name row)
   :authProvider (:auth-provider row) :externalSubject (:external-subject row)
   :principalId (:principal-id row) :identityBound (boolean (:principal-id row))
   :identityEnrollmentRequired (nil? (:principal-id row)) :status (:status row)
   :createdAt (:created-at row) :updatedAt (:updated-at row)
   :credentials credentials :memberships memberships})

(defn org
  "Attach already computed directory counts."
  [row counts]
  (when row (merge row counts)))
