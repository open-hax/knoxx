(ns knoxx.backend.actions.contract
  (:require [malli.core :as m]))

(def ActionKind
  [:enum :invoke/agent :invoke/http :invoke/discord-post :invoke/noop])

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