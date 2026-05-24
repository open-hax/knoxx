(ns knoxx.backend.domain.source.registry
  "Helpers for source resource declarations.

   Driver protocol/spec behavior lives in knoxx.backend.domain.driver.registry.
   Source resources select driver events with :source/listens; they do not define
   emitted event shapes."
  (:require [knoxx.backend.domain.driver.registry :as driver-registry]))

(defn source-listens
  "Return normalized event type keywords selected by a source resource."
  [source]
  (driver-registry/source-listens source))
