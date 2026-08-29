(ns knoxx.backend.law.url
  "Delegates to the katamorph law url module.
   Re-exports all public vars for backward compatibility."
  (:require [katamorph.law.url :as core]))

(def looks-like-url? core/looks-like-url?)
(def media-url? core/media-url?)
(def data-url? core/data-url?)
