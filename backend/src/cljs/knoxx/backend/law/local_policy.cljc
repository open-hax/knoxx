(ns knoxx.backend.law.local-policy
  "Admission contracts for the local policy directory, never a database query language."
  (:require [clojure.string :as str]
            [malli.core :as m]))

(def NonBlank [:and :string [:fn #(not (str/blank? %))]])
(def ToolPolicy
  [:map {:closed true} [:tool-id NonBlank] [:effect [:enum "allow" "deny"]]
   [:constraints :map]])
(def Actor
  [:map {:closed true} [:principal :map] [:user-id NonBlank] [:membership-id NonBlank]])
(def Command
  [:map {:closed true} [:id NonBlank] [:at NonBlank] [:actor [:maybe Actor]]
   [:operation :qualified-keyword] [:args [:vector :any]]])

(defn refuse!
  "Raise a visible policy refusal, preserving the transport status."
  [status code message]
  (throw (ex-info message {:status status :code code})))

(defn assert-schema!
  "Validate boundary data before it can change a replayable directory."
  [schema value]
  (when-not (m/validate schema value)
    (refuse! 400 "local_policy_invalid" "Invalid local policy request"))
  value)

(defn nonblank!
  "Require a nonblank scalar at a named policy boundary."
  [value]
  (assert-schema! NonBlank value))

(defn status!
  "Accept only declared account, membership, organization and credential states."
  [value]
  (assert-schema! [:enum "active" "inactive" "suspended" "disabled" "revoked" "pending"] value))

(defn slug
  "Normalize ordinary directory slugs; an empty result is a visible error."
  [value]
  (nonblank! (-> (nonblank! value) str/trim str/lower-case
                (str/replace #"[^a-z0-9]+" "-")
                (str/replace #"(^-+|-+$)" ""))))

(defn tool-policies
  "Normalize supported wire spellings and reject malformed or duplicate tool grants."
  [items]
  (let [result (mapv (fn [item]
                       (assert-schema!
                        ToolPolicy
                        (if (string? item)
                          {:tool-id item :effect "allow" :constraints {}}
                          {:tool-id (or (:tool-id item) (:toolId item) (:tool_id item) (:id item))
                           :effect (or (:effect item) "allow") :constraints (or (:constraints item) {})})))
                     (or items []))]
    (when-not (= (count result) (count (set (map :tool-id result))))
      (refuse! 400 "local_policy_duplicate_tool" "A tool may appear only once in a policy assignment"))
    (vec (sort-by :tool-id result))))

(defn present!
  "Resolve an exact directory object; absence cannot become an upsert."
  [state collection id]
  (or (get-in state [collection (nonblank! id)])
      (refuse! 404 "local_policy_not_found" "Policy directory object does not exist")))

(defn unique!
  "Require a unique semantic key before creating an object."
  [items predicate]
  (when (some predicate items)
    (refuse! 409 "local_policy_conflict" "A policy object already owns this identity")))
