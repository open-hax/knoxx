(ns knoxx.backend.domain.registry.resource
  "Generic registry protocol: delegates to the contract-runtime resource registry.

   This is a thin wrapper that provides backward compatibility for existing
   Knoxx callers. The actual implementation lives in the extracted
   contract-runtime package.

   The config map must contain :contract-runtime/deps (see
   knoxx.backend.contract-runtime-deps/build-deps)."
  (:require [open-hax.contract-runtime.registry.resource :as core-registry]))

(def registry-id core-registry/registry-id)
(def registry-resource-kind core-registry/registry-resource-kind)
(def registered-resource-ids core-registry/registered-resource-ids)
(def registry-resource core-registry/registry-resource)
(def registry-catalog core-registry/registry-catalog)

(def registry-specs core-registry/registry-specs)
(def make-registry core-registry/make-registry)
(def registries-by-kind core-registry/registries-by-kind)
(def actions-registry core-registry/actions-registry)
(def rules-registry core-registry/rules-registry)
(def triggers-registry core-registry/triggers-registry)
(def actors-registry core-registry/actors-registry)
(def users-registry core-registry/users-registry)
(def agents-registry core-registry/agents-registry)
(def capabilities-registry core-registry/capabilities-registry)
(def roles-registry core-registry/roles-registry)
(def workflows-registry core-registry/workflows-registry)
(def schedules-registry core-registry/schedules-registry)
(def sources-registry core-registry/sources-registry)
(def registry core-registry/registry)
(def catalog core-registry/catalog)
