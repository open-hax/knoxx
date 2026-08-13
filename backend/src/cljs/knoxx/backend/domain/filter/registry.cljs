(ns knoxx.backend.domain.filter.registry
  "Delegates to the katamorph filter registry.
   Re-exports all public vars for backward compatibility."
  (:require [katamorph.filter.registry :as core]))

(def register-filter! core/register-filter!)
(def filter-fn core/filter-fn)
(def filter-ids core/filter-ids)
