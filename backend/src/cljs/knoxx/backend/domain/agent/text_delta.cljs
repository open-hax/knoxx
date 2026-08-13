(ns knoxx.backend.domain.agent.text-delta
  "Delegates to the katamorph agent text-delta module.
   Re-exports all public vars for backward compatibility."
  (:require [katamorph.agent.text-delta :as core]))

(def diff-appended-text core/diff-appended-text)
(def suppress-replayed-prefix-delta core/suppress-replayed-prefix-delta)
