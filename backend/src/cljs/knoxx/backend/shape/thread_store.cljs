(ns knoxx.backend.shape.thread-store
  "Conversation persistence is separate from per-turn run persistence.")

(defprotocol IThreadStore
  (read-thread [store thread-id] "Read a visible conversation or nil.")
  (conversation-thread [store conversation-id] "Read the unique visible conversation binding or nil.")
  (put-thread! [store thread] "Admit conversation fields and return persisted state.")
  (patch-thread! [store thread-id patch] "Atomically merge fields into current conversation state.")
  (rewind-thread! [store thread-id turns] "Atomically remove the final user turns, or return nil.")
  (delete-thread! [store thread-id] "Idempotently remove current conversation state.")
  (active-threads [store] "List visible running, queued, and waiting-input conversations."))
