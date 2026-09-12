(ns knoxx.backend.infra.stores.mailbox-store
  "Explicit selection of the actor mailbox persistence protocol."
  (:require [knoxx.backend.law.mailbox-store :as law]
            [knoxx.backend.shape.mailbox-store :as mailbox]))

(defonce provider* (atom nil))
(defn install! "Select a provider, or clear the selection during orderly teardown." [provider]
  (when-not (or (nil? provider) (satisfies? mailbox/IMailboxStore provider))
    (law/refuse! 500 "mailbox_provider_invalid" "Mailbox provider does not implement its service protocol"))
  (reset! provider* provider))
(defn selected! "Resolve selected persistence; missing durability is never simulated." []
  (or @provider* (law/refuse! 503 "mailbox_provider_unavailable" "Mailbox persistence has not been configured")))
