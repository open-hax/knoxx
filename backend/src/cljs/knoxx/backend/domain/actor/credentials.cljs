(ns knoxx.backend.domain.actor.credentials
  "Resolve per-actor tool credentials from the policy DB.

   Tool credentials are actor-owned state. Do not read API keys from process
   env vars here; missing credentials should be fixed in Admin → Actors."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.actor.acting :as actor-acting]
            [knoxx.backend.domain.agent.agent-context :as agent-context]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.db.policy :as policy-db]))

(defn- agent-context-actor-id
  []
  (let [ctx (or (agent-context/get-context) {})
        spec (:agent-spec ctx)]
    (some-> (or (:actor-id spec)
                (:actor_id spec)
                (:actorId spec)
                (:actor-id ctx)
                (:actor_id ctx)
                (:actorId ctx))
            str
            str/trim
            not-empty)))

(defn current-actor-id
  "The actor whose credentials this call may read, or nil.

   Two sources, and an actor scope is not merely preferred — inside one it is
   the only answer, even when it names no actor. agent-context is a
   process-global atom, so on a concurrent surface it may hold an unrelated
   agent turn's actor; falling back to it from inside a scope that established
   there is no actor would let an actor-less call read that turn's credentials.
   That is the leak the scope exists to prevent, so `or` is the wrong combinator
   here: absence inside a scope is an answer, not a gap.

   Outside any scope nobody has said anything, so the agent-spawn path's
   agent-context is consulted and keeps working unchanged."
  []
  (if (actor-acting/in-scope?)
    (actor-acting/current-actor-id)
    (agent-context-actor-id)))

(defn- normalize-credential
  [payload]
  (:credential payload))

(defn ^:async get-credential!
  [runtime provider]
  (let [actor-id (current-actor-id)
        db (authz/policy-db runtime)]
    (cond
      (str/blank? (str actor-id))
      (throw
       (js/Error. (str "No current actor_id is available for " provider " credentials. Start the agent with an actor_id and configure it in Admin → Actors.")))

      (nil? db)
      (throw
       (js/Error. "Actor credentials require the Knoxx policy database."))

      :else
      (let [result (await (policy-db/get-actor-credential! db actor-id provider))]
        (if-let [credential (normalize-credential result)]
          credential
          (throw (js/Error. (str "No active " provider " credentials configured for actor " actor-id ". Configure them in Admin → Actors."))))))))

(defn secret-value
  [credential & keys]
  (let [secrets (:secretJson credential)]
    (some (fn [k]
            (some-> (or (get secrets k)
                        (get secrets (keyword k))
                        (get secrets (name k)))
                    str
                    str/trim
                    not-empty))
          keys)))
