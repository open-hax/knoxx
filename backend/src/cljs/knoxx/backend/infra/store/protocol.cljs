(ns knoxx.backend.infra.store.protocol
  "Delegates to the katamorph store protocol.
   Re-exports all public vars for backward compatibility."
  (:require [katamorph.store.protocol :as core]))

;; Re-export the protocol functions
(def insert! core/insert!)
(def find-docs core/find-docs)

;; The IStore protocol is defined in katamorph.store.protocol
;; and should be used directly from there. This namespace provides backward
;; compatibility for callers that use the old namespace.
;; For protocol implementation, use: katamorph.store.protocol/IStore
