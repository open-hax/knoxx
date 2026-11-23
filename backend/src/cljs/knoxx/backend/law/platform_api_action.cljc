(ns knoxx.backend.law.platform-api-action
  "Named contracts for planned API actions and their publication projections."
  (:require [knoxx.backend.law.platform-action :as action]
            [knoxx.backend.law.platform-api-effects :as effects]
            [malli.core :as m]))

(def registry
  (merge action/registry effects/registry
         {::descriptor [:map {:closed true}
                         [:operation [:string {:min 1}]]
                         [:intent [:ref action/Intent]] [:effect [:ref ::effects/effect]]
                         [:kind [:enum :discord-message :discord-thread :discord-reaction :bluesky-record :bluesky-chat :operation]]
                         [:response-path [:vector [:or :string :int]]]
                         [:payload :any] [:record :any]
                         [:guard-params [:map {:closed true}
                                          [:text :string] [:source-text :string]
                                          [:channel_id {:optional true} :string]
                                          [:replyTo {:optional true} :string]]]]
          ::descriptors [:vector {:max 200} [:ref ::descriptor]]}))

(def Descriptors ::descriptors)
(def descriptors-valid? (m/validator [:schema {:registry registry} Descriptors]))

(defn assert-descriptors!
  "Refuse malformed internal action data without exposing a private payload."
  [descriptors]
  (when-not (descriptors-valid? descriptors)
    (throw (ex-info "Invalid planned API action descriptors" {:code "platform_api_actions_invalid" :phase :preflight})))
  descriptors)

(def ^:private admitted-bluesky-create-effects
  {"app.bsky.feed.post" :publish
   "app.bsky.feed.like" :react
   "app.bsky.feed.repost" :amplify
   "app.bsky.graph.follow" :curate})

(defn admitted-social-write?
  "The deployed social contracts can publish, reply, react, repost or follow.
   New write classes need their own explicit execution policy."
  [{:keys [operation intent effect]}]
  (case (:platform intent)
    "discord" (contains? #{["create_message" :publish] ["add_my_message_reaction" :react]}
                         [operation (:effect effect)])
    "bluesky" (and (contains? #{"com.atproto.repo.createRecord" "com.atproto.repo.applyWrites"} operation)
                   (= :create (:mutation effect))
                   (= (:effect effect) (get admitted-bluesky-create-effects (:collection effect))))
    false))
