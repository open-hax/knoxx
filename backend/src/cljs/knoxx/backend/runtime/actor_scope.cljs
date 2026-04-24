(ns knoxx.backend.runtime.actor-scope
  (:require [knoxx.backend.contracts.actor-scope :as impl]))

;; Backwards-compat shim: contract actor-scope semantics now live under
;; knoxx.backend.contracts.*. Keep runtime.* as process/runtime-only.

(def wildcard-actor impl/wildcard-actor)
(def legacy-chat-actor-id impl/legacy-chat-actor-id)
(def normalize-actor-claim impl/normalize-actor-claim)
(def normalize-actor-claims impl/normalize-actor-claims)
(def normalized-contract-actors impl/normalized-contract-actors)
(def agent-role-claims impl/agent-role-claims)
(def normalize-agent-contract impl/normalize-agent-contract)
(def actor-allowed? impl/actor-allowed?)
(def effective-actor-id impl/effective-actor-id)
(def actor-claims->wire impl/actor-claims->wire)
