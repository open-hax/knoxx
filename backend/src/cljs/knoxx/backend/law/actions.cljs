(ns knoxx.backend.law.actions
  (:require [malli.core :as m]))

(def ActionKind
  [:enum
   :actions/start-agent-session
   :actions/start-agent
   :actions/agent-send-steer-message
   :actions/agent-queue-follow-up-message
   :actions/stop-session
   :invoke/agent
   :invoke/sub-agent
   :invoke/http
   :invoke/discord-post
   :invoke/noop])

(def ActionContract
  [:map {:closed false}
   [:action/id     :string]
   [:action/kind   ActionKind]
   [:action/label  {:optional true} :string]
   [:action/with   {:optional true} [:map-of :keyword :any]]])

(def StepSpec
  [:map {:closed false}
   [:uses    :string]
   [:with    {:optional true} [:map-of :keyword :any]]])

(def validate-action
  (m/validator ActionContract))

(def explain-action
  (m/explainer ActionContract))
