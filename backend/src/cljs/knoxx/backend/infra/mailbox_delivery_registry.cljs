(ns knoxx.backend.infra.mailbox-delivery-registry
  "Explicit selection for authorized in-process actor message delivery."
  (:require [knoxx.backend.law.mailbox-store :as law]
            [knoxx.backend.shape.mailbox-delivery :as port]))
(defonce provider* (atom nil))
(defn install! [provider]
  (when (and provider (not (satisfies? port/IMailboxDelivery provider)))
    (law/refuse! 500 "mailbox_delivery_provider_invalid" "Mailbox delivery must implement its named port"))
  (reset! provider* provider))
(defn deliver! [runtime config context request]
  (if-let [provider @provider*] (port/deliver! provider runtime config context request)
    (law/refuse! 503 "mailbox_delivery_unavailable" "Mailbox delivery has not been configured")))
