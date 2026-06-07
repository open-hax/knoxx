(ns knoxx.backend.infra.system-instance
  "Identity of the current booted system instance (this Node process).

   The id is minted once per process via defonce: it survives shadow-cljs
   hot reloads but changes on every real restart. Stores stamp it into
   session/run documents (:system_instance_id) so any reader of the shared
   store — this process, a future Knoxx instance, or another protocol
   client — can distinguish work owned by a live instance from work
   orphaned by an instance that no longer exists.

   Single-writer assumption: one Knoxx backend process owns the agent
   runtime at a time. If multiple instances ever share the store, ownership
   checks must gain a liveness/heartbeat dimension instead of plain
   id equality."
  (:require [knoxx.backend.extern.agent-turn-node :as xturn-node]))

(defonce ^:private instance-id*
  (xturn-node/random-uuid!))

(defn current-id
  "UUID identifying the current system instance."
  []
  instance-id*)

(defn owned-by-current-instance?
  "True when the document's :system_instance_id was stamped by this instance.
   Documents without the field (legacy writers) are never owned."
  [doc]
  (= (str (or (:system_instance_id doc) "")) instance-id*))
