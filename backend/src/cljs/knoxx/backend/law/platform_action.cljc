(ns knoxx.backend.law.platform-action
  "Named, data-only contracts for platform action admission."
  (:require [malli.core :as m]))

(def registry
  {::attempt-id [:int {:min 1}]
   ::effect [:enum :publish :react :thread :edit :delete :curate :profile :moderate :account
             :credential :binary :subscription :amplify :conversation :presence]
   ::scope [:map {:closed true}
            [:platform [:enum "discord" "bluesky"]]
            [:effect [:ref ::effect]]
            [:target [:string {:min 1}]]]
   ::intent [:map {:closed true}
             [:platform [:enum "discord" "bluesky"]]
             [:effect [:ref ::effect]]
             [:target [:string {:min 1}]]
             [:arguments [:map-of :keyword :any]]]
   ::outcome [:map {:closed true}
              [:status [:enum :not-attempted :confirmed :refused :unknown :partial]]
              [:http-status {:optional true} [:int {:min 100 :max 599}]]
              [:platform-code {:optional true} [:or :int :string]]
              [:resolved-target {:optional true} [:string {:min 1}]]
              [:delivered-message-ids {:optional true} [:vector [:string {:min 1}]]]]
   ::reservation [:map {:closed true}
                  [:run-id [:string {:min 1}]] [:attempt-id [:ref ::attempt-id]] [:intent [:ref ::intent]]]
   ::intents [:vector {:min 1 :max 200} [:ref ::intent]]
   ::group-reservation [:map {:closed true}
                        [:run-id [:string {:min 1}]]
                        [:attempt-id [:ref ::attempt-id]] [:intents [:ref ::intents]]]
   ::context [:map {:closed true}
              [:conversation-id [:string {:min 1}]] [:actor-id [:string {:min 1}]]]})

(def Intent ::intent)
(def Outcome ::outcome)
(def Reservation ::reservation)
(def GroupReservation ::group-reservation)
(def Intents ::intents)
(def Context ::context)

(def validators
  (into {} (map (fn [[key _]]
                  [key (m/validator [:schema {:registry registry} key])])) registry))

(defn assert-valid!
  "Boundary failures name the contract without echoing private payload values."
  [schema-key value]
  (when-not (if-let [valid? (get validators schema-key)] (valid? value) false)
    (throw (ex-info "Invalid platform action boundary"
                    {:code "platform_action_invalid" :schema schema-key :phase :preflight})))
  value)
