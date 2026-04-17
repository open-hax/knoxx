(ns knoxx.backend.agent-templates
  "DSL and Library for Agent Runtime Templates.
   Allows defining reusable profiles (roles, models, prompts) 
   that can be instantiated into specific event-agent jobs.
   
   Architecture:
   - Model Profiles: Decouple templates from specific model strings
   - Templates: Reusable agent personas with prompts and policies
   - Instances: Concrete job specs with triggers, filters, and overrides
   - Persistence: Redis (hot) → SQL (cold) write-behind queue"
  (:require [clojure.string :as str]
            [knoxx.backend.runtime-config :as runtime-config]))

;; =============================================================================
;; Model Profiles
;; =============================================================================
;; Decouples the template from specific model strings to allow 
;; global updates (e.g., switching 'local-fast' from gemma2 to gemma4)

(def model-profiles
  {:local-fast   {:model "gemma4:e4b" :thinking-level "off"}
   :local-mid    {:model "gemma4:31b" :thinking-level "off"}
   :local-heavy  {:model "gemma4:31b" :thinking-level "minimal"}
   :cloud-heavy  {:model "glm-5"      :thinking-level "high"}
   :cloud-fast   {:model "glm-5-fast" :thinking-level "off"}
   :cloud-balanced {:model "glm-5"    :thinking-level "minimal"}})

(defn resolve-model-profile [profile-id]
  (or (get model-profiles profile-id)
      {:model (runtime-config/default-model) :thinking-level "off"}))

(defn all-model-profiles []
  (vec (keys model-profiles)))

;; =============================================================================
;; Agent Templates
;; =============================================================================
;; The Library of reusable Agent personas.
;; Each template defines: role, system-prompt, model-profile, tool-policies

(def templates
  {:yap-bot
   {:role "creative_catalyst"
    :system-prompt "You are the Frankie-Infinite-Yap Bot. Be creatively chaotic, musically inclined, and a catalyst for lyrics and rhythms. Prefer vivid, surreal imagery over dry facts."
    :model-profile :local-fast
    :tool-policies [{:toolId "discord.publish" :effect "allow"}
                    {:toolId "discord.send" :effect "allow"}
                    {:toolId "discord.read" :effect "allow"}]
    :thinking-level "off"}

   :sentinel
   {:role "security_monitor"
    :system-prompt "You are a system sentinel. Monitor logs for anomalies, security drift, and critical failures. Be concise and urgent."
    :model-profile :cloud-heavy
    :tool-policies [{:toolId "discord.publish" :effect "allow"}
                    {:toolId "discord.send" :effect "allow"}]
    :thinking-level "high"}

   :summarizer
   {:role "knowledge_synthesizer"
    :system-prompt "You are a professional synthesizer. Extract key themes, action items, and critical signal from noisy conversations."
    :model-profile :local-mid
    :tool-policies [{:toolId "discord.publish" :effect "allow"}
                    {:toolId "discord.send" :effect "allow"}
                    {:toolId "discord.read" :effect "allow"}
                    {:toolId "memory_search" :effect "allow"}]
    :thinking-level "off"}

   :patrol-observer
   {:role "knowledge_worker"
    :system-prompt "Observe configured channels, detect fresh human signals, and queue structured events without speaking publicly."
    :model-profile :local-fast
    :tool-policies [{:toolId "discord.read" :effect "allow"}
                    {:toolId "discord.channel.messages" :effect "allow"}]
    :thinking-level "off"}

   :mention-responder
   {:role "executive"
    :system-prompt "You are Knoxx's targeted event-driven Discord responder. Read the room, use tools when needed, and prefer silence over filler."
    :model-profile :local-fast
    :tool-policies [{:toolId "discord.publish" :effect "allow"}
                    {:toolId "discord.send" :effect "allow"}
                    {:toolId "discord.read" :effect "allow"}
                    {:toolId "memory_search" :effect "allow"}
                    {:toolId "graph_query" :effect "allow"}]
    :thinking-level "off"}})

(defn get-template [template-id]
  (get templates template-id))

(defn all-templates []
  (vec (keys templates)))

(defn resolve-template-spec
  "Resolve a template into a concrete agent spec.
   Merges template defaults + model profile + user overrides.
   Returns a map suitable for :agentSpec in event-agent jobs."
  [template-id overrides]
  (let [template (get-template template-id)]
    (if-not template
      (throw (js/Error. (str "Unknown agent template: " template-id)))
      (let [model-cfg (resolve-model-profile (:model-profile template))]
        (-> template
            (merge model-cfg)
            (merge overrides)
            ;; Ensure thinking-level is explicitly set (prevents "reasoning reset" bug)
            (assoc :thinking-level (or (:thinking-level overrides)
                                       (:thinking-level template)
                                       (:thinking-level model-cfg)
                                       "off")))))))

;; =============================================================================
;; Job Instantiation Helpers
;; =============================================================================
;; Helpers for creating concrete job instances from templates

(defn default-tool-policies []
  [{:toolId "discord.read" :effect "allow"}
   {:toolId "discord.channel.messages" :effect "allow"}
   {:toolId "discord.channel.scroll" :effect "allow"}
   {:toolId "discord.dm.messages" :effect "allow"}
   {:toolId "discord.search" :effect "allow"}
   {:toolId "discord.publish" :effect "allow"}
   {:toolId "discord.send" :effect "allow"}
   {:toolId "websearch" :effect "allow"}
   {:toolId "memory_search" :effect "allow"}
   {:toolId "graph_query" :effect "allow"}])

(defn instantiate-job
  "Create a concrete event-agent job from a template.
   
   Args:
   - template-id: Keyword identifying the template (e.g., :yap-bot)
   - job-id: Unique string ID for this job instance
   - trigger: Map with :kind (\"cron\" or \"event\"), :cadenceMinutes, :eventKinds
   - source: Map with :kind, :mode, :config
   - filters: Map with :channels, :keywords, :repositories
   - overrides: Optional map to override template defaults
   
   Returns a complete job spec ready for persistence."
  [template-id job-id trigger source filters & [overrides]]
  (let [agent-spec (resolve-template-spec template-id overrides)]
    {:id job-id
     :name (or (:name overrides) job-id)
     :enabled true
     :trigger trigger
     :source source
     :filters filters
     :agentSpec agent-spec
     :description (or (:description overrides)
                      (str "Instance of " (name template-id) " template"))
     :templateId (name template-id)}))

(defn normalize-job-for-persistence
  "Ensure a job spec has all required fields for durable storage.
   Adds timestamps and validates structure."
  [job]
  (let [now (.toISOString (js/Date.))]
    (-> job
        (assoc :createdAt (or (:createdAt job) now))
        (assoc :updatedAt now)
        (assoc-in [:agentSpec :thinkingLevel] 
                  (or (get-in job [:agentSpec :thinkingLevel])
                      (get-in job [:agentSpec :thinking-level])
                      "off"))
        ;; Normalize thinking-level key to camelCase for consistency
        (update :agentSpec #(-> %
                                (dissoc :thinking-level)
                                (assoc :thinkingLevel (or (:thinkingLevel %)
                                                          (:thinking-level %)
                                                          "off")))))))
