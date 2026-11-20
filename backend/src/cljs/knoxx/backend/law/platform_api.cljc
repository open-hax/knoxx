(ns knoxx.backend.law.platform-api
  "Boundary contracts for public platform operation discovery."
  (:require [malli.core :as m]
            [malli.error :as me]))

(def Platform [:enum "discord" "bluesky"])

(def FeaturesParameters
  [:map {:closed true}
   [:platform {:optional true :description "Choose Discord or Bluesky, or omit to compare both."} Platform]
   [:query {:optional true :description "A feature intent, such as make a poll, edit a message, continue a thread, or manage lists. Empty returns all feature families."} [:string {:max 256}]]
   [:limit {:optional true} [:int {:min 1 :max 25}]]
   [:offset {:optional true} [:int {:min 0 :max 100000}]]])

(def DescribeParameters
  [:map {:closed true}
   [:platform Platform]
   [:operation {:description "Exact operation ID returned by social.features."} [:string {:min 1 :max 256}]]
   [:reference {:optional true :description "Optional local schema reference: Discord #/components/schemas/Name, or Bluesky nsid#definition / #definition. External URLs are not followed."}
    [:string {:min 1 :max 1024}]]])

(def ReadNumber
  "Finite JSON numbers, including fractional query values."
  [:and number? [:fn {:error/message "Expected a finite JSON number"}
                #(and (= % %) (not= ##Inf %) (not= ##-Inf %))]])

(def ReadValue
  "Simple URL parameter values; objects and credentials are never accepted."
  [:or [:string {:max 8192}] ReadNumber :boolean
   [:vector {:max 100} [:or [:string {:max 8192}] ReadNumber :boolean]]])

(def ReadParameters
  "Use feature-discovery parameter names; transport infers path versus query."
  [:map {:closed true}
   [:platform Platform]
   [:operation [:string {:min 1 :max 256}]]
   [:parameters {:optional true :description "Named parameter values from social.describe, for example [{name: channel_id, value: returned ID}, {name: limit, value: 10}]. Array values become repeated query fields."}
    [:vector {:max 40}
     [:map {:closed true}
      [:name [:string {:min 1 :max 128}]]
      [:value ReadValue]]]]])

(def ReadPlan
  "Internal schema-selected read plan; callers cannot provide headers or origins."
  [:map {:closed true}
   [:platform Platform] [:operation :string] [:method [:= "GET"]]
   [:path-template [:re #"^/(?!/)"]]
   [:path [:map-of :string [:or :string :int]]]
   [:query [:map-of :string ReadValue]]
   [:credential-provider [:enum "discord_bot" "bluesky"]]
   [:service [:enum :discord :bluesky-pds :bluesky-chat :bluesky-ozone]]
   [:proxy [:maybe [:enum "did:web:api.bsky.chat#bsky_chat"
                   "did:plc:ar7c4by46qjdydhdevvrndac#atproto_labeler"]]]
   [:security :any]])

(def DiscordReadCredential
  "An actor-owned bot credential, resolved by the policy database."
  [:map {:closed true} [:token [:string {:min 1}]]])

(def BlueskyReadCredential
  "Actor-owned account authentication; PDS is configured, never supplied by the model."
  [:map {:closed true}
   [:identifier [:string {:min 1}]] [:password [:string {:min 1}]]
   [:pds [:string {:min 1}]] [:scope-key [:string {:min 1}]]])

(defn assert-valid!
  "Reject invalid tool boundaries without echoing submitted values."
  [schema value]
  (when-not (m/validate schema value)
    (throw (ex-info "Invalid platform API parameters"
                    {:code "platform_parameters_invalid"
                     :errors (me/humanize (m/explain schema value))})))
  value)
