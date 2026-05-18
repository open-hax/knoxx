(ns knoxx.backend.policy.protocol
  "Policy store protocol shared by EDN and SQL adapters.

   Contract-shaped values are validated with open-hax.contracts.schema at the
   adapter boundary. EDN is canonical for contract identity; SQL is a projection
   and secret-state adapter."
  (:require [open-hax.contracts.schema :as contract-schema]))

(defn validate-contract!
  [contract-class value]
  (contract-schema/assert! contract-class value))

(defn validate-actor!
  [actor]
  (validate-contract! :actor actor))

(defprotocol PolicyStore
  (list-contracts [store contract-class]
    "Return a promise or value containing contract-shaped maps for contract-class.")
  (get-contract [store contract-class contract-id]
    "Return one contract-shaped map by class/id, or nil.")
  (upsert-contract! [store contract-class contract]
    "Validate and persist a contract-shaped map.")
  (list-actors [store]
    "Return actor contract maps validated by the shared contract schema.")
  (get-actor [store actor-id]
    "Return one actor contract map validated by the shared contract schema, or nil.")
  (upsert-actor! [store actor]
    "Validate and persist one actor contract map."))

(defprotocol ActorCredentialStore
  (list-actor-credentials [store provider]
    "Return active actor credential state rows for provider. Implementations must not expose this through public contract APIs.")
  (get-actor-credential [store actor-id provider]
    "Return actor credential state for provider. Implementations must not expose this through public contract APIs.")
  (upsert-actor-credential! [store actor-id provider credential]
    "Persist mutable actor credential state."))

(defprotocol ActorProjectionStore
  (sync-actor-projections! [store actors]
    "Sync canonical actor contracts into projection storage such as SQL memberships/users."))
