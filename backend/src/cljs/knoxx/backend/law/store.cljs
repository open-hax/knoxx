(ns knoxx.backend.law.store
  "Delegates to the katamorph store law module.
   Re-exports all public vars for backward compatibility."
  (:require [katamorph.store.law :as core]))

(def compile-schema-guard core/compile-schema-guard)
