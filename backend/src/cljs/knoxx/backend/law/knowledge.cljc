(ns knoxx.backend.law.knowledge
  "Explicit actor-owned knowledge contracts; no identity is accepted from tool input."
  (:require [clojure.string :as str]
            [malli.core :as m]))

(def Principal
  "Server-resolved actor and organization, never a caller-supplied owner."
  [:map [:actor-id [:string {:min 1}]] [:org-slug [:string {:min 1}]]
   [:org-id {:optional true} [:maybe :string]]])

(def Scope
  "Versioned ownership carried with original events and their graph projections."
  [:map [:version [:= 1]] [:visibility [:enum "private" "shared"]]
   [:actor_id [:string {:min 1}]] [:org_slug {:optional true} [:maybe :string]]
   [:org_id {:optional true} [:maybe :string]]])

(def RecordParameters
  "An agent may record observations or hypotheses, not silently promote acceptance."
  [:map {:closed true}
   [:text [:string {:min 1 :max 12000}]]
   [:title {:optional true} [:string {:min 1 :max 180}]]
   [:visibility {:optional true} [:enum "private" "shared"]]
   [:tier {:optional true} [:enum "observed" "derived" "provisional"]]
   [:confidence {:optional true} [:double {:min 0 :max 1}]]
   [:evidence {:optional true} [:vector {:max 8} [:string {:min 1 :max 2048}]]]])

(def ReadParameters
  "Read one exact original knowledge identity."
  [:map {:closed true} [:id [:string {:min 1 :max 240}]]])

(def SearchParameters
  "Search candidates are authorized against their original event before projection."
  [:map {:closed true} [:query [:string {:min 1 :max 1000}]]
   [:limit {:optional true} [:int {:min 1 :max 12}]]])

(def LinkParameters
  "Relationships are explicit attributed claims, not merges or semantic equivalence."
  [:map {:closed true}
   [:source [:string {:min 1 :max 240}]] [:target [:string {:min 1 :max 240}]]
   [:relation [:enum "supports" "contradicts" "related_to" "derived_from"]]
   [:text [:string {:min 1 :max 4000}]]
   [:visibility {:optional true} [:enum "private" "shared"]]
   [:evidence {:optional true} [:vector {:max 8} [:string {:min 1 :max 2048}]]]])

(defn assert-valid!
  "Validate a named boundary before performing any effect."
  [schema value]
  (when-not (m/validate schema value)
    (throw (ex-info "Invalid knowledge boundary" {:status 400 :code "knowledge_invalid"})))
  value)

(defn literal-query-pattern
  "Treat URLs, identifiers, and punctuation as literal text rather than regex authority."
  [query]
  (str/replace query #"[\\^$.*+?()\[\]{}|]" (fn [character] (str "\\" character))))

(defn scope-for
  "Stamp a new record from the authenticated principal; default to private."
  [principal visibility]
  (assert-valid! Principal principal)
  (assert-valid! Scope
                 (cond-> {:version 1 :visibility (or visibility "private")
                          :actor_id (:actor-id principal) :org_slug (:org-slug principal)}
                   (:org-id principal) (assoc :org_id (:org-id principal)))))

(defn same-org?
  "Organization IDs win when both exist; otherwise require the declared org slug."
  [principal scope]
  (if (and (seq (:org-id principal)) (seq (:org_id scope)))
    (= (:org-id principal) (:org_id scope))
    (and (not (str/blank? (:org-slug principal)))
         (= (:org-slug principal) (:org_slug scope)))))

(defn visible?
  "Actor privacy is checked before all organization/admin permissions."
  [principal scope]
  (boolean
   (and (m/validate Principal principal)
        (m/validate Scope scope)
        (if (= "shared" (:visibility scope))
          (same-org? principal scope)
          (and (= (:actor-id principal) (:actor_id scope))
               (or (and (not (seq (:org_id scope)))
                        (not (seq (:org_slug scope))))
                   (same-org? principal scope)))))))

(defn link-scope
  "A link cannot publish a private endpoint; each relationship retains its author."
  [principal source-scope target-scope visibility]
  (when-not (and (visible? principal source-scope) (visible? principal target-scope))
    (throw (ex-info "Knowledge resource is outside the acting identity"
                    {:status 403 :code "knowledge_scope_denied"})))
  (let [private-endpoint? (some #(= "private" (:visibility %)) [source-scope target-scope])]
    (when (and private-endpoint? (= visibility "shared"))
      (throw (ex-info "A shared link cannot expose a private endpoint"
                      {:status 403 :code "knowledge_private_endpoint"})))
    (scope-for principal (if private-endpoint? "private" (or visibility "private")))))

(defn assert-link-endpoints!
  "Relationships connect distinct knowledge entries, never other relationship claims."
  [source target]
  (when-not (and (not= (:id source) (:id target))
                 (every? #(and (= "knoxx.knowledge" (:kind %))
                               (= "entry" (get-in % [:extra :knowledge_kind])))
                         [source target]))
    (throw (ex-info "Knowledge links require two distinct entries"
                    {:status 400 :code "knowledge_invalid_endpoints"})))
  [source target])
