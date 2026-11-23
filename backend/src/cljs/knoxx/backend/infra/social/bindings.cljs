(ns knoxx.backend.infra.social.bindings
  "Admit social writes only after the acting resource has environment bindings."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.contracts.loader :as contracts]))

(def ^:private native-reads
  #{"discord_read" "discord_channel_messages" "discord_channel_scroll"
    "discord_dm_messages" "discord_search" "discord_guilds" "discord_list_servers"
    "discord_channels" "discord_list_channels" "discord_voice_status"
    "discord_voice_list_members"
    "bluesky_profile" "bluesky_search" "bluesky_author_feed" "bluesky_timeline"
    "bluesky_thread" "bluesky_notifications" "bluesky_followers" "bluesky_follows"
    "bluesky_chat_list" "bluesky_chat_read"})

(defn ensure-write-allowed!
  "Reject writes for a declared actor whose trusted bindings are pending."
  [config actor-id]
  (when (and (seq actor-id)
             (some (fn [{:keys [contract]}]
                     (and (= actor-id (:actor/id contract))
                          (true? (get-in contract [:actor/metadata :social/bindings-pending]))))
                   (contracts/load-all-contracts-sync config)))
    (throw (ex-info "Social writes require resolved environment bindings for the acting identity"
                    {:status 403 :code "social_bindings_pending" :phase :preflight}))))

(defn ensure-native-tool-allowed!
  "Preserve known reads. An unclassified native social tool cannot write while bindings are pending."
  [config actor-id tool-name]
  (when-let [name (some-> tool-name (str/replace "." "_"))]
    (when (and (or (str/starts-with? name "discord_") (str/starts-with? name "bluesky_"))
               (not (contains? native-reads name)))
      (ensure-write-allowed! config actor-id))))
