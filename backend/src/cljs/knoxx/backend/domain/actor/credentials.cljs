(ns knoxx.backend.domain.actor.credentials
  "Resolve per-actor tool credentials from the policy DB.

   Tool credentials are actor-owned state. Do not read API keys from process
   env vars here; missing credentials should be fixed in Admin → Actors."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.agent.agent-context :as agent-context]
            [knoxx.backend.domain.auth.authz :as authz]))

(defn current-actor-id
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

(defn- normalize-credential
  [payload]
  (let [credential (aget payload "credential")]
    (when credential
      (js->clj credential :keywordize-keys true))))

(defn get-credential!
  [runtime provider]
  (let [actor-id (current-actor-id)
        db (authz/policy-db runtime)]
    (cond
      (str/blank? (str actor-id))
      (js/Promise.reject
       (js/Error. (str "No current actor_id is available for " provider " credentials. Start the agent with an actor_id and configure it in Admin → Actors.")))

      (nil? db)
      (js/Promise.reject
       (js/Error. "Actor credentials require the Knoxx policy database."))

      :else
      (-> (.getActorCredential db actor-id provider)
          (.then (fn [result]
                   (if-let [credential (normalize-credential result)]
                     credential
                     (throw (js/Error. (str "No active " provider " credentials configured for actor " actor-id ". Configure them in Admin → Actors."))))))))))

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
