(ns knoxx.backend.domain.driver.builtin
  "Built-in event driver implementations.

   These are code-level drivers, not resources. Source resources instantiate
   them for specific actors/credentials and select event types with
   :source/listens."
  (:require [knoxx.backend.domain.driver.registry :as registry]))

(def discord-driver
  (registry/make-static-driver
   {:id :driver/discord
    :kind :discord
    :emits [{:event/type :discord.message
             :description "A normalized Discord message observed by a Discord gateway."
             :event/shape {:guild-id :string
                           :channel-id :string
                           :message-id :string
                           :author-id :string
                           :content :string}}
            {:event/type :discord.message.mention
             :description "A Discord message that mentions the source actor/bot identity."
             :event/shape {:guild-id :string
                           :channel-id :string
                           :message-id :string
                           :author-id :string
                           :content :string}}
            {:event/type :discord.message.created
             :description "Raw Discord message creation after gateway normalization."
             :event/shape {:guild-id :string
                           :channel-id :string
                           :message-id :string
                           :author-id :string
                           :content :string}}
            {:event/type :discord.voice.state-update
             :description "Discord voice-state gateway update."
             :event/shape {:guild-id :string
                           :channel-id :string
                           :user-id :string}}
            {:event/type :discord.voice.audio.window
             :description "A captured Discord voice audio window emitted by a voice receiver."
             :event/shape {:guild-id :string
                           :channel-id :string
                           :session-id :string
                           :conversation-id :string
                           :audio-files [:vector :map]}}]}))

(def eta-mu-ingestion-driver
  (registry/make-static-driver
   {:id :driver/eta-mu-ingestion
    :kind :eta-mu.ingestion
    :emits [{:event/type :eta-mu.session.started
             :event/shape {:session-id :string
                           :cwd :string
                           :timestamp :string}}
            {:event/type :eta-mu.session.model-changed
             :event/shape {:session-id :string
                           :provider :string
                           :model-id :string}}
            {:event/type :eta-mu.session.compacted
             :event/shape {:session-id :string
                           :summary :string}}
            {:event/type :eta-mu.session.message
             :event/shape {:session-id :string
                           :role :string
                           :content :string}}
            {:event/type :eta-mu.session.tool-call
             :event/shape {:session-id :string
                           :tool-name :string
                           :status :string}}]}))

(def agents-driver
  (registry/make-static-driver
   {:id :driver/agents
    :kind :agents
    :emits [{:event/type :agent.session.started
             :event/shape {:session-id :string
                           :conversation-id :string
                           :agent-id :string}}
            {:event/type :agent.session.switched
             :event/shape {:session-id :string
                           :conversation-id :string
                           :agent-id :string}}
            {:event/type :agent.turn.started
             :event/shape {:run-id :string
                           :session-id :string
                           :conversation-id :string}}
            {:event/type :agent.turn.completed
             :event/shape {:run-id :string
                           :session-id :string
                           :conversation-id :string}}
            {:event/type :agent.tool.started
             :event/shape {:run-id :string
                           :tool-name :string
                           :tool-call-id :string}}
            {:event/type :agent.tool.completed
             :event/shape {:run-id :string
                           :tool-name :string
                           :tool-call-id :string}}]}))

(def knoxx-driver
  (registry/make-static-driver
   {:id :driver/knoxx
    :kind :knoxx
    :emits [{:event/type :knoxx.runtime.started
             :event/shape {:host :string
                           :port :int}}
            {:event/type :knoxx.runtime.stopping
             :event/shape {:reason :string}}
            {:event/type :knoxx.resource.changed
             :event/shape {:resource-kind :keyword
                           :resource-id :string}}]}))

(def user-driver
  (registry/make-static-driver
   {:id :driver/user
    :kind :user
    :emits [{:event/type :user.created
             :event/shape {:user-id :string}}
            {:event/type :user.updated
             :event/shape {:user-id :string}}
            {:event/type :user.message.sent
             :event/shape {:user-id :string
                           :message-id :string
                           :content :string}}]}))

(def org-driver
  (registry/make-static-driver
   {:id :driver/org
    :kind :org
    :emits [{:event/type :org.created
             :event/shape {:org-id :string}}
            {:event/type :org.updated
             :event/shape {:org-id :string}}
            {:event/type :org.member.added
             :event/shape {:org-id :string
                           :user-id :string}}
            {:event/type :org.member.removed
             :event/shape {:org-id :string
                           :user-id :string}}]}))

(def sessions-driver
  (registry/make-static-driver
   {:id :driver/sessions
    :kind :sessions
    :emits [{:event/type :session.created
             :event/shape {:session-id :string
                           :conversation-id :string}}
            {:event/type :session.resumed
             :event/shape {:session-id :string
                           :conversation-id :string}}
            {:event/type :session.closed
             :event/shape {:session-id :string
                           :conversation-id :string}}]}))

(def translations-driver
  (registry/make-static-driver
   {:id :driver/translations
    :kind :translations
    :emits [{:event/type :translation.segment.created
             :event/shape {:segment-id :string
                           :project :string}}
            {:event/type :translation.segment.reviewed
             :event/shape {:segment-id :string
                           :reviewer-id :string
                           :status :string}}
            {:event/type :translation.batch.completed
             :event/shape {:batch-id :string
                           :project :string}}]}))

(def built-in-drivers
  [discord-driver
   eta-mu-ingestion-driver
   agents-driver
   knoxx-driver
   user-driver
   org-driver
   sessions-driver
   translations-driver])

(defn register-built-in-drivers!
  "Register built-in code-level drivers. Idempotent."
  []
  (registry/register-drivers! built-in-drivers))
