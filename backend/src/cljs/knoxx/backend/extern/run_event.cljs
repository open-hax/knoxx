(ns knoxx.backend.extern.run-event
  "Runtime occurrence identity at the Node cryptography boundary."
  (:require ["node:crypto" :as crypto]
            [knoxx.backend.law.run-store :as law]))

(defn new-id
  "Mint one identity when a runtime event is produced, before any sink retries it."
  []
  (law/require! law/NonBlank (str "event_" (crypto/randomUUID))))
