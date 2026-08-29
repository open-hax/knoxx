(ns knoxx.backend.infra.store.memory
  "Delegates to the katamorph store memory module.
   Re-exports all public vars for backward compatibility."
  (:require [katamorph.store.memory :as core]))

(def memory-collection core/memory-collection)
