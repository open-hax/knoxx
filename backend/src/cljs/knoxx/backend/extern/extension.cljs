(ns knoxx.backend.extern.extension
  "Extension runtime event boundary helpers.

   Owns JS object construction for eta-mu/OpenCode-style extension lifecycle
   events so agent session orchestration can pass CLJS maps at call sites."
  (:require [knoxx.backend.extern.js :as xjs]))

(defn event-payload
  [m]
  (xjs/object m))

(defn empty-event-payload
  []
  (xjs/empty-object))
