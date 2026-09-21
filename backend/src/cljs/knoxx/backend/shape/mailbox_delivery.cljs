(ns knoxx.backend.shape.mailbox-delivery
  "Named port for authorized actor message effects.")
(defprotocol IMailboxDelivery
  (deliver! [provider runtime config context request] "Deliver one already claimed message with verified authority."))
