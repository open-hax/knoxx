(ns knoxx.backend.runtime.contract-validator
  (:require [knoxx.backend.contracts.validator :as impl]))

;; Backwards-compat shim: contract validation semantics now live under
;; knoxx.backend.contracts.*. Keep runtime.* as process/runtime-only.

(def ContractId impl/ContractId)
(def UserSurface impl/UserSurface)
(def PolicyCheck impl/PolicyCheck)
(def AgentSpec impl/AgentSpec)
(def AgentContract impl/AgentContract)
(def ActorContract impl/ActorContract)
(def RoleContract impl/RoleContract)
(def CapabilityContract impl/CapabilityContract)
(def PolicyContract impl/PolicyContract)
(def ModelFamilyContract impl/ModelFamilyContract)
(def ModelContract impl/ModelContract)
(def validate impl/validate)
