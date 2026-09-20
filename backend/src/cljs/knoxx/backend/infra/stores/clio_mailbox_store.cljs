(ns knoxx.backend.infra.stores.clio-mailbox-store
  "Canonical Clio actor mailboxes with scoped reads and fenced delivery receipts."
  (:require [knoxx.backend.extern.mailbox-store :as host]
            [knoxx.backend.infra.clio-application-store :as clio]
            [knoxx.backend.infra.mailbox-changes :as changes]
            [knoxx.backend.infra.stores.mailbox-store-reference :as reference]
            [knoxx.backend.law.mailbox-store :as law]
            [knoxx.backend.shape.mailbox-store :as mailbox]))

(defn- ^:async command! [{:keys [ledger options]} kind scope payload]
  (let [result (await (host/serialized!
                       (:directory ledger)
                       (fn [] (clio/write! ledger :mailbox/operation [(host/operation options kind scope payload)]))))]
    (if (= kind :claim) (assoc result :durable? true) result)))

(defn- ^:async list-durable! [ledger options scope filters]
  (assoc (await (clio/read! ledger :mailbox/entries [scope filters (host/clock! options)]))
         :durable? true))

(defrecord ClioMailboxStore [ledger options]
  mailbox/IMailboxStore
  (create-entry! [this scope entry] (command! this :create scope entry))
  (register-route! [this scope route] (command! this :register scope route))
  (unregister-route! [this scope conversation] (command! this :unregister scope {:conversation-id conversation}))
  (resolve-route [_ scope actor] (clio/read! ledger :mailbox/route [scope actor (host/clock! options)]))
  (list-entries [_ scope filters] (list-durable! ledger options scope filters))
  (read-message [_ scope id] (clio/read! ledger :mailbox/message [scope id (host/clock! options)]))
  (claim-deliveries! [this scope request] (command! this :claim scope request))
  (mark-delivery! [this scope id status request]
    (command! this :mark scope {:entry-id id :status status :options request}))
  (acknowledge-entry! [this scope id] (command! this :ack scope {:entry-id id})))

(defn open!
  "Open an explicit mailbox ledger; invalid options or clocks create no files."
  [{:keys [directory] :as options}]
  (law/checked! :mailbox/options
                [:map [:directory law/NonBlank] [:clock! {:optional true} fn?]] options)
  (host/clock! options)
  (->ClioMailboxStore
   (clio/open! {:directory directory :stream "knoxx/mailbox" :projection reference/projection
                :reads {:mailbox/route reference/resolve-route :mailbox/entries reference/list-entries
                        :mailbox/message reference/read-message}
                :after-append changes/accepted!
                :writes {:mailbox/operation reference/apply-operation!}}) options))
