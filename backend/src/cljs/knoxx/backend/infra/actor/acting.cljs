(ns knoxx.backend.infra.actor.acting
  "The actor a unit of work is currently acting as.

   Distinct from domain.actor.scope, which matches a principal against a
   contract's declared actors. That answers \"may this actor\"; this answers
   \"which actor, right now\".

   Tool credentials are actor-owned, so something has to say which actor a tool
   call belongs to. The agent runtime answers that with agent-context, but it is
   only set on the agent-spawn path and it is a process-global atom — fine for a
   single turn loop, wrong for a concurrent HTTP surface where two requests can
   interleave at any await.

   This is the surface-neutral answer: enter a scope for one unit of work, read
   it from anywhere inside, and it is unreachable outside. Nothing here decides
   *which* actor is permitted — that is law.mcp-oauth's job — only which one is
   currently in effect.

   The distinction that matters: **being in a scope with no actor is not the
   same as being outside a scope.** A caller that has established there is no
   actor is making a claim, and that claim has to be able to win over a
   process-global that happens to hold one.

   Lives in infra because entering and reading ambient request-local state is an
   effect, whatever it is wrapped in. It was briefly in domain to spare
   infra.actor.credentials a domain-to-infra require; the honest fix was to move
   the credential resolver here too, since it performs a database read and was
   never domain either. The raw Node interop still lives in
   extern.async-local-storage; this namespace exchanges CLJS values only."
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
  "Call f with no arguments with this actor in scope for the duration.

   Takes either an actor id or a map of {:actor-id, :org-id}. The org matters
   because actor_id is not unique across orgs: a credential lookup that knows
   only the actor can resolve another tenant's membership and return its secret.
   Carrying the request's org means the lookup is scoped to it.

   A scope is entered even when actor-id is blank, and that is the whole point:
   it records \"this work has *no* actor\" as a positive fact rather than an
   absence. Entering no scope would leave the caller indistinguishable from code
   that never established anything, and readers fall back to the process-global
   agent-context in that case — so an actor-less MCP call could pick up whatever
   actor a concurrent agent turn happened to be running as, and use its
   credentials. That is the exact leak this namespace exists to prevent,
   arriving through the fallback instead of through the store.

   Nested scopes shadow: the innermost wins, including when the innermost has no
   actor."
  [actor-or-map f]
  (let [{:keys [actor-id org-id]} (if (map? actor-or-map)
                                    actor-or-map
                                    {:actor-id actor-or-map})]
    (als/run-with store
                  {:actor-id (normalize-actor-id actor-id)
                   :org-id   (normalize-actor-id org-id)}
                  f)))

(defn in-scope?
  "True when the caller is inside a scope, whether or not it names an actor.

   Readers need this to tell \"no actor, definitively\" from \"nobody said\".
   Only the second may fall back to another source."
  []
  (some? (als/current store)))

(defn current-org-id
  "The org in scope, or nil. Scopes a credential lookup to one tenant."
  []
  (normalize-actor-id (:org-id (als/current store))))

(defn current-actor-id
  "The actor id in scope, or nil.

   nil is ambiguous on its own — no actor, or no scope — so pair it with
   in-scope? whenever the difference decides whether to consult a fallback."
  []
  (normalize-actor-id (:actor-id (als/current store))))
