(ns knoxx.backend.law.url
  "Delegates to the contract-runtime law url module.
   Re-exports all public vars for backward compatibility."
  (:require [open-hax.contract-runtime.law.url :as core]))

(def looks-like-url? core/looks-like-url?)
(def media-url? core/media-url?)
(def data-url? core/data-url?)
