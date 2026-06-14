(ns knoxx.backend.domain.filter.registry
  "Delegates to the contract-runtime filter registry.
   Re-exports all public vars for backward compatibility."
  (:require [open-hax.contract-runtime.filter.registry :as core]))

(def register-filter! core/register-filter!)
(def filter-fn core/filter-fn)
(def filter-ids core/filter-ids)
