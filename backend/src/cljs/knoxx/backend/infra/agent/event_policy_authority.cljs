(ns knoxx.backend.infra.agent.event-policy-authority
  "Unforgeable in-process authority for event-carried agent resource policy.

  The token is an object identity, not serializable data. An HTTP or persisted
  agent-spec map can spell the same keys but cannot manufacture this value. A
  trusted trigger action creates the context anew on replay after restart.")

(defonce ^:private authority-token (js/Object.))

(defn authorized-context
  "Build the tool auth context for one server-admitted event policy."
  [resource-policies actor-id role tool-policies]
  (cond-> {:resourcePolicies resource-policies
           ::authority authority-token}
    actor-id (assoc :actorId actor-id)
    role (assoc :roleSlugs [role])
    (seq tool-policies) (assoc :toolPolicies (vec tool-policies))))

(defn authorized?
  "True only for a context minted by `authorized-context` in this process."
  [auth-context]
  (identical? authority-token (::authority auth-context)))
