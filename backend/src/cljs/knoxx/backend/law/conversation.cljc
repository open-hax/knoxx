(ns knoxx.backend.law.conversation
  "Receipt-derived private conversation snapshots; semantic notes remain proposals."
  (:require [malli.core :as m]))

(def registry
  {::nonblank [:string {:min 1 :max 2048}]
   ::platform [:enum "discord" "bluesky"]
   ::remember [:map {:closed true}
               [:platform {:optional true} [:ref ::platform]]
               [:topic [:string {:min 1 :max 180}]]
               [:state [:enum "open" "closed"]]
               [:question {:optional true} [:string {:max 1000}]]]
   ::list [:map {:closed true}
           [:platform {:optional true} [:ref ::platform]]
           [:root {:optional true} [:ref ::nonblank]]
           [:limit {:optional true} [:int {:min 1 :max 25}]]
           [:offset {:optional true} [:int {:min 0 :max 100000}]]]
   ::reference [:map {:closed true} [:id [:ref ::nonblank]] [:cid {:optional true} [:ref ::nonblank]]]
   ::cursor [:map {:closed true}
             [:version [:= 1]] [:actor-id [:ref ::nonblank]] [:run-id [:ref ::nonblank]]
             [:platform [:ref ::platform]] [:root [:ref ::reference]] [:latest [:ref ::reference]]
             [:parent {:optional true} [:ref ::reference]]
             [:channel-id {:optional true} [:ref ::nonblank]]
             [:message-ids {:optional true} [:vector {:min 1 :max 100} [:ref ::nonblank]]]
             [:observed-at [:string {:min 20 :max 40}]]
             [:source-tool [:ref ::nonblank]] [:source-call [:ref ::nonblank]]
             [:topic [:string {:min 1 :max 180}]] [:state [:enum "open" "closed"]]
             [:question [:string {:max 1000}]]
             [:annotation-tier [:= "provisional"]]]})

(def Remember ::remember)
(def ListParameters ::list)
(def Cursor ::cursor)
(def validators (into {} (map (fn [[key _]] [key (m/validator [:schema {:registry registry} key])])) registry))

(defn assert-valid!
  "Reject invented cursor fields at the caller boundary without echoing content."
  [schema value]
  (when-not (if-let [valid? (get validators schema)] (valid? value) false)
    (throw (ex-info "Invalid conversation boundary" {:code "conversation_invalid" :schema schema :phase :preflight})))
  value)

(defn input-schema
  "Inline the named root for MCP field visibility; the registry stays plain data."
  [schema]
  [:schema {:registry registry} (get registry schema)])
