(ns knoxx.backend.shape.agent.provider
  "Provider-facing request/response shapes after Knoxx has normalized an agent
   turn. Provider adapters may add provider-specific keys, so schemas remain
   open and validate only the fields Knoxx depends on."
  (:require [knoxx.backend.shape.agent.message :as message]
            [malli.core :as m]
            [malli.error :as me]))

(def ProviderTool
  [:map {:closed false}
   [:name :string]
   [:description {:optional true} :string]
   [:parameters {:optional true} any?]])

(def ProviderRequest
  [:map {:closed false}
   [:model {:optional true} [:maybe :string]]
   [:messages [:vector message/AgentMessage]]
   [:tools {:optional true} [:vector ProviderTool]]
   [:thinking-level {:optional true} [:maybe :string]]
   [:temperature {:optional true} number?]
   [:max-tokens {:optional true} int?]
   [:stream {:optional true} :boolean]
   [:metadata {:optional true} [:map {:closed false}]]])

(def Usage
  [:map {:closed false}
   [:input-tokens {:optional true} int?]
   [:output-tokens {:optional true} int?]
   [:total-tokens {:optional true} int?]])

(def ProviderResponse
  [:map {:closed false}
   [:answer {:optional true} [:maybe :string]]
   [:reasoning {:optional true} [:maybe :string]]
   [:messages {:optional true} [:vector message/AgentMessage]]
   [:usage {:optional true} Usage]
   [:finish-reason {:optional true} [:maybe :string]]
   [:error {:optional true} [:maybe :string]]])

(defn valid-provider-request?
  [value]
  (m/validate ProviderRequest value))

(defn explain-provider-request
  [value]
  (me/humanize (m/explain ProviderRequest value)))
