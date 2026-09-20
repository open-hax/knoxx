(ns knoxx.backend.shape.agent-chat-input
  "Pure normalization of requested agent chat fields and tool policy lists."
  (:require [clojure.string :as str]))

(defn compact-agent-spec-overrides
  "Remove absent or empty overrides while preserving explicit values."
  [agent-spec]
  (into {}
        (remove (fn [[_ value]]
                  (or (nil? value)
                      (and (string? value) (str/blank? value))
                      (and (sequential? value) (empty? value)))))
        agent-spec))

(defn requested-role
  "Read the requested role from the normalized command and authority fields."
  [parsed]
  (or (get-in parsed [:agent-spec :role])
      (some-> (:auth-context parsed) :role str str/trim not-empty)
      (some->> (get-in parsed [:auth-context :roleSlugs]) seq first str str/trim not-empty)))

(defn allow-policy?
  "Recognize an explicit allow effect in a tool policy."
  [policy]
  (= "allow" (some-> (:effect policy) str str/lower-case)))

(defn tool-policy-id
  "Normalize the existing tool identifier spellings."
  [policy]
  (some-> (or (:toolId policy) (:tool-id policy)) str str/trim not-empty))

(defn ctx-tool-policies
  "Return the existing context tool policy list as a vector."
  [ctx]
  (vec (or (:toolPolicies ctx)
           (:tool-policies ctx)
           [])))

(defn parsed-auth-tool-policies
  "Return the command authority tool policy list as a vector."
  [parsed]
  (vec (or (get-in parsed [:auth-context :toolPolicies])
           (get-in parsed [:auth-context :tool-policies])
           [])))

(defn requested-tool-policies
  "Prefer explicit agent policy requests over authority-context defaults."
  [parsed]
  (let [from-spec (vec (or (get-in parsed [:agent-spec :tool-policies]) []))
        from-auth (parsed-auth-tool-policies parsed)]
    (cond
      (seq from-spec) from-spec
      (seq from-auth) from-auth
      :else [])))

(defn auth-context-with-actor
  "Attach a nonblank requested actor without discarding existing authority."
  [ctx actor-id]
  (if-let [actor-id* (some-> actor-id str str/trim not-empty)]
    (assoc (or ctx {}) :actorId actor-id*)
    ctx))
