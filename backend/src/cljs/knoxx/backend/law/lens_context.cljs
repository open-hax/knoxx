(ns knoxx.backend.law.lens-context
  "Malli parameter contract for KnoxxLens intentional cross-channel context
   expansion (`lens_grab_context`). Pure schema declaration only — no I/O."
  (:require [malli.core :as m]))

(def grab-context-params
  [:map
   [:query {:description "Vector + graph search query describing the cross-channel topic, person, project, shared URL, or thread to pull into context."}
    :string]
   [:topK {:optional true
           :description "Number of top semantically similar Discord message anchors to retrieve across all servers and channels (default 6, max 12)."}
    [:int {:min 1 :max 12}]]
   [:windowBefore {:optional true
                   :description "Number of preceding chronological messages to grab before each matched cross-channel message (default 2, max 8)."}
    [:int {:min 0 :max 8}]]
   [:windowAfter {:optional true
                  :description "Number of following chronological messages to grab after each matched cross-channel message (default 2, max 8)."}
    [:int {:min 0 :max 8}]]
   [:currentChannelId {:optional true
                       :description "Open Discord channel snowflake ID from <discord_context> (id=...), used to prioritize cross-channel connections and label edge scope."}
    :string]
   [:currentGuildId {:optional true
                     :description "Open Discord server (guild) snowflake ID from <discord_context> or templateContext, used to label same-server vs cross-server connections."}
    :string]
   [:guildId {:optional true
              :description "Optional Discord server (guild) filter when explicitly narrowing to one server. Omit by default to search across all ingested servers and channels."}
    :string]])

(defn valid-params?
  "True when `params` satisfies `grab-context-params`."
  [params]
  (m/validate grab-context-params params))
