(ns knoxx.backend.infra.store.protocol
  "Delegates to the contract-runtime store protocol.
   Re-exports all public vars for backward compatibility."
  (:require [open-hax.contract-runtime.store.protocol :as core]))

;; Re-export the protocol functions
(def insert! core/insert!)
(def find-docs core/find-docs)

;; The IStore protocol is defined in open-hax.contract-runtime.store.protocol
;; and should be used directly from there. This namespace provides backward
;; compatibility for callers that use the old namespace.
;; For protocol implementation, use: open-hax.contract-runtime.store.protocol/IStore
