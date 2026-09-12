(ns knoxx.backend.law.mailbox-changes
  "Structural contract for private-content-free mailbox invalidations."
  (:require [knoxx.backend.law.mailbox-store :as mailbox]))

(def Change
  [:map {:closed true} [:org-id mailbox/NonBlank] [:actor-ids [:vector mailbox/NonBlank]]])

(defn checked! "Validate the only metadata a mailbox observer may receive." [change]
  (mailbox/checked! :mailbox/change Change change))
