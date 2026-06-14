(ns knoxx.backend.domain.resources.namespace-file
  "Delegates to the contract-runtime manifest module.
   Re-exports all public vars for backward compatibility."
  (:require [open-hax.contract-runtime.manifest :as core]))

(def kind-id-keys core/kind-id-keys)
(def kind->id-key core/kind->id-key)
(def namespace-file? core/namespace-file?)
(def qualified-id core/qualified-id)
(def qualified-id-str core/qualified-id-str)
(def entry-kinds core/entry-kinds)
(def facet-kinds core/facet-kinds)
(def anonymous-facets core/anonymous-facets)
(def namespace-file-definitions core/namespace-file-definitions)
