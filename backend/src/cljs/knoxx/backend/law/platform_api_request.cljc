(ns knoxx.backend.law.platform-api-request
  "Named JSON request boundaries. No actor, origin, header or credential inputs."
  (:require [malli.core :as m]))

(def registry
  {::json [:or :nil :boolean :string :double :int
           [:vector [:ref ::json]]
           [:map-of :string [:ref ::json]]]
   ::parameter-value [:or :string :double :int :boolean
                      [:vector {:max 100} [:or :string :double :int :boolean]]]
   ::parameter [:map {:closed true}
                [:name [:string {:min 1 :max 128}]]
                [:value [:ref ::parameter-value]]]
   ::input [:map {:closed true}
            [:platform [:enum "discord" "bluesky"]]
            [:operation [:string {:min 1 :max 256}]]
            [:parameters {:optional true} [:vector {:max 40} [:ref ::parameter]]]
            [:body {:optional true} [:ref ::json]]]
   ::plan [:map {:closed true}
           [:platform [:enum "discord" "bluesky"]] [:operation :string]
           [:method [:enum "GET" "POST" "PUT" "PATCH" "DELETE"]]
           [:path-template [:re "^/(?!/)"]]
           [:path [:map-of :string [:or :string :int]]]
           [:query [:map-of :string [:ref ::parameter-value]]]
           [:credential-provider [:enum "discord_bot" "bluesky"]]
           [:service [:enum :discord :bluesky-pds :bluesky-chat :bluesky-ozone]]
           [:proxy [:maybe [:enum "did:web:api.bsky.chat#bsky_chat"
                           "did:plc:ar7c4by46qjdydhdevvrndac#atproto_labeler"]]]
           [:security :any]
           [:body {:optional true} [:ref ::json]]
           [:empty-success-statuses [:vector [:int {:min 200 :max 299}]]]
           [:account-repository {:optional true} [:string {:min 1}]]]
   ::response [:map {:closed true}
               [:ok :boolean] [:status [:int {:min 100 :max 599}]]
               [:body [:ref ::json]] [:empty-body? :boolean]
               [:rate-limit [:map-of :string :string]]
               [:authenticated-did {:optional true} [:string {:min 1}]]
               [:redacted-fields :int]]})

(def Input ::input)
(def Plan ::plan)
(def Response ::response)
(def InputSchema
  "Inline named input at the MCP envelope; recursive body refs stay in the registry."
  [:schema {:registry registry} (get registry Input)])

(def validators
  "Executable named schemas retain the EDN registry as their source."
  (into {} (map (fn [[key _]] [key (m/validator [:schema {:registry registry} key])])) registry))

(defn- bounded-json?
  [value]
  (loop [pending [[value 0]] nodes 0]
    (if-let [[item depth] (peek pending)]
      (and (< nodes 50000) (<= depth 32)
           (cond
             (number? item) (and (= item item) (not= item ##Inf) (not= item ##-Inf)
                                 (recur (pop pending) (inc nodes)))
             (string? item) (and (<= (count item) 1048576) (recur (pop pending) (inc nodes)))
             (map? item) (and (every? #(and (string? %) (<= (count %) 1024)) (keys item))
                              (recur (into (pop pending) (map #(vector % (inc depth))) (vals item)) (inc nodes)))
             (vector? item) (recur (into (pop pending) (map #(vector % (inc depth))) item) (inc nodes))
             (or (nil? item) (boolean? item)) (recur (pop pending) (inc nodes))
             :else false))
      true)))

(defn assert-valid!
  "Reject unsafe shapes without returning private submitted values in errors."
  [schema-key value]
  (when-not (and (or (not (contains? value :body)) (bounded-json? (:body value)))
                 (if-let [valid? (get validators schema-key)] (valid? value) false)
                 (every? #(bounded-json? (:value %)) (:parameters value))
                 (every? bounded-json? (vals (:query value))))
    (throw (ex-info "Invalid JSON platform request boundary"
                    {:code "platform_json_request_invalid" :schema schema-key :phase :preflight})))
  value)
