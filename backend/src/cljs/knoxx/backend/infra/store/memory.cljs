(ns knoxx.backend.infra.store.memory
  "Delegates to the contract-runtime store memory module.
   Re-exports all public vars for backward compatibility."
  (:require [open-hax.contract-runtime.store.memory :as core]))

(def memory-collection core/memory-collection)
