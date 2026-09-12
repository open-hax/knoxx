(ns knoxx.backend.domain.actor.tools
  "Compatibility entry point for the actor SDK adapter."
  (:require [knoxx.backend.extern.actor-tools :as tools]))
(def create-actors-custom-tools tools/create-tools)
