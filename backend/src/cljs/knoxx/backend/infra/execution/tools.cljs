(ns knoxx.backend.infra.execution.tools
  "Replace SDK coding execution while keeping its schemas and tool semantics."
  (:require [knoxx.backend.extern.eta-mu :as sdk]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.execution.client :as client]
            [knoxx.backend.law.execution :as law]))

(defn create-tools
  "An isolated deployment always overrides allowed coding names; failure stays closed."
  [_runtime config auth-context]
  (when (law/isolated? config)
    (let [names (filterv #(or (nil? auth-context) (authz/ctx-tool-allowed? auth-context %))
                          (sort law/coding-tools))]
      (when (seq names)
        (law/require-client! config)
        (sdk/isolated-coding-tools (:workspace-root config) names
                                  (partial client/execute! config auth-context))))))
