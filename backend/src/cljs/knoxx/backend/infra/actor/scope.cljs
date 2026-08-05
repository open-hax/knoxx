(ns knoxx.backend.infra.actor.scope
  "The actor a unit of work is running as, scoped to that work.

   Tool credentials are actor-owned, so something has to say which actor a tool
   call belongs to. The agent runtime answers that with agent-context, but it is
   only set on the agent-spawn path and it is a process-global atom — fine for a
   single turn loop, wrong for a concurrent HTTP surface where two requests can
   interleave at any await.

   This is the surface-neutral answer: enter a scope for one unit of work, read
   it from anywhere inside, and it is unreachable outside. Nothing here decides
   *which* actor is permitted — that is law.mcp-oauth's job — only which one is
   currently in effect."
  (:require [clojure.string :as str]
            [knoxx.backend.extern.async-local-storage :as als]))

(defonce ^:private store (als/create-store))

(defn normalize-actor-id
  "An actor id as a non-blank trimmed string, or nil.

   nil rather than \"\" on purpose: a blank actor id must never read as an
   actor, and returning the empty string invites a caller to pass it on."
  [value]
  (some-> value str str/trim not-empty))

(defn run-as!
  "Call f with no arguments with actor-id in scope for the duration.

   A blank actor-id enters no scope at all rather than an empty one, so a caller
   that could not resolve an actor cannot accidentally establish one — the
   failure surfaces where the credential is read, with a message about the
   missing actor, instead of as a lookup for actor \"\"."
  [actor-id f]
  (if-let [actor (normalize-actor-id actor-id)]
    (als/run-with store {:actor-id actor} f)
    (f)))

(defn current-actor-id
  "The actor id in scope, or nil when there is none."
  []
  (normalize-actor-id (:actor-id (als/current store))))
