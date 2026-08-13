(ns knoxx.backend.domain.agent.reasoning
  "Delegates to the katamorph agent reasoning module.
   Re-exports all public vars for backward compatibility."
  (:require [katamorph.agent.reasoning :as core]))

(def split-think-tags core/split-think-tags)
(def route-think-delta core/route-think-delta)
