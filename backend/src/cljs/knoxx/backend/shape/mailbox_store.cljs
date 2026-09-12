(ns knoxx.backend.shape.mailbox-store
  "Named tenant-scoped actor delivery persistence operations.")

(defprotocol IMailboxStore
  (create-entry! [store scope entry] "Admit an immutable message reference with a caller-stable entry id.")
  (register-route! [store scope route] "Bind the actor's current live conversation in this tenant.")
  (unregister-route! [store scope conversation-id] "Retire only routes still bound to this conversation.")
  (resolve-route [store scope actor-id] "Read this tenant's unexpired actor route.")
  (list-entries [store scope filters] "Read only the caller's inbox/outbox, or authorized tenant administration.")
  (claim-deliveries! [store scope options] "Atomically lease eligible attempts and return their fencing tokens.")
  (mark-delivery! [store scope entry-id status options] "Persist a delivery outcome only for its current claim.")
  (acknowledge-entry! [store scope entry-id] "Acknowledge an existing entry within recipient authority."))
