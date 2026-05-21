(ns knoxx.backend.domain.actor.scope
  (:require [clojure.string :as str]))

;; Contract-scoped actor / principal matching.

(def wildcard-actor :*)
(def legacy-chat-actor-id "chat_primary")

(defn normalize-actor-claim
  [value]
  (cond
    (= value wildcard-actor) wildcard-actor
    (keyword? value) (let [normalized (some-> value name str/trim not-empty)]
                       (when normalized
                         (if (= normalized "*")
                           wildcard-actor
                           normalized)))
    (string? value) (let [normalized (some-> value str str/trim not-empty)]
                      (when normalized
                        (if (= normalized "*")
                          wildcard-actor
                          normalized)))
    (nil? value) nil
    :else (normalize-actor-claim (str value))))

(defn normalize-actor-claims
  [value]
  (let [items (cond
                (set? value) value
                (sequential? value) value
                (nil? value) []
                :else [value])]
    (into #{} (keep normalize-actor-claim) items)))

(defn normalized-contract-actors
  ([contract]
   (normalized-contract-actors contract nil))
  ([contract missing-fallback-actor-id]
   (let [declared (normalize-actor-claims (:contract/actors contract))
         legacy (normalize-actor-claim (:contract/actor contract))
         merged (cond-> declared
                  legacy (conj legacy)
                  (= "knoxx_default" (some-> (:contract/id contract) str str/trim)) (conj wildcard-actor))]
     (cond
       (seq merged) merged
       :else (let [fallback (normalize-actor-claim missing-fallback-actor-id)]
               (if fallback
                 #{fallback}
                 #{}))))))

(defn agent-role-claims
  [contract]
  (let [legacy-roles (or (:actor/roles contract) [])
        agent-roles (or (get-in contract [:agent :roles]) [])
        agent-role (get-in contract [:agent :role])]
    (->> (concat legacy-roles
                 agent-roles
                 (cond
                   (sequential? agent-role) agent-role
                   agent-role [agent-role]
                   :else []))
         distinct
         vec)))

(defn normalize-agent-contract
  [contract]
  (if-not (map? contract)
    contract
    (let [actors (normalized-contract-actors contract)]
      (cond-> (dissoc contract :contract/actor)
        (seq actors) (assoc :contract/actors actors)))))

(defn actor-allowed?
  [actors actor-id]
  (let [claims (normalize-actor-claims actors)
        wanted (normalize-actor-claim actor-id)]
    (or (contains? claims wildcard-actor)
        (and wanted (contains? claims wanted)))))

(defn effective-actor-id
  [actors requested-actor-id default-actor-id]
  (let [claims (normalize-actor-claims actors)
        requested (normalize-actor-claim requested-actor-id)
        fallback (normalize-actor-claim default-actor-id)
        concrete-claims (->> claims
                             (remove #{wildcard-actor})
                             sort
                             vec)]
    (cond
      (and requested (actor-allowed? claims requested)) requested
      (and fallback (actor-allowed? claims fallback)) fallback
      (seq concrete-claims) (first concrete-claims)
      :else fallback)))

(defn actor-claims->wire
  [actors]
  (->> (normalize-actor-claims actors)
       (map (fn [claim]
              (if (= claim wildcard-actor)
                "*"
                claim)))
       sort
       vec))
