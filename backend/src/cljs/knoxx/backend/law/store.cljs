(ns knoxx.backend.law.store
  "Delegates to the contract-runtime store law module.
   Re-exports all public vars for backward compatibility."
  (:require [open-hax.contract-runtime.store.law :as core]))

(def compile-schema-guard core/compile-schema-guard)
