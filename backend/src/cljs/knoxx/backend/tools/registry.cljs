(ns knoxx.backend.tools.registry
  (:require [clojure.string :as str]))

(def ^:private tool-meta
  {"read" {:id "read" :label "Read" :description "Read files and retrieved context"}
   "write" {:id "write" :label "Write" :description "Create new markdown drafts and artifacts"}
   "edit" {:id "edit" :label "Edit" :description "Revise existing documents and drafts"}
   "bash" {:id "bash" :label "Shell" :description "Run controlled shell commands"}
   "websearch" {:id "websearch" :label "Web Search" :description "Search the live web through Proxx websearch"}
   "canvas" {:id "canvas" :label "Canvas" :description "Open long-form markdown drafting canvas"}

   "email.send" {:id "email.send" :label "Email" :description "Send drafts through configured email account"}

   "discord.publish" {:id "discord.publish" :label "Discord Publish" :description "Publish updates to Discord"}
   "discord.send" {:id "discord.send" :label "Discord Send" :description "Send Discord messages and replies"}
   "discord.read" {:id "discord.read" :label "Discord Read" :description "Read messages from Discord channels"}
   "discord.channel.messages" {:id "discord.channel.messages" :label "Discord Channel Messages" :description "Fetch messages from a Discord channel"}
   "discord.channel.scroll" {:id "discord.channel.scroll" :label "Discord Channel Scroll" :description "Scroll older messages in a Discord channel"}
   "discord.dm.messages" {:id "discord.dm.messages" :label "Discord DM Messages" :description "Fetch messages from a Discord DM channel"}
   "discord.search" {:id "discord.search" :label "Discord Search" :description "Search messages in Discord channels"}
   "discord.guilds" {:id "discord.guilds" :label "Discord Guilds" :description "List Discord servers the bot is in"}
   "discord.channels" {:id "discord.channels" :label "Discord Channels" :description "List channels in a Discord server"}
   "discord.list.servers" {:id "discord.list.servers" :label "Discord List Servers" :description "List all Discord servers the bot can access"}
   "discord.list.channels" {:id "discord.list.channels" :label "Discord List Channels" :description "List channels across one or all Discord servers"}

   "event_agents.status" {:id "event_agents.status" :label "Event Agent Status" :description "Inspect scheduled event-agent runtime state and configuration"}
   "event_agents.dispatch" {:id "event_agents.dispatch" :label "Event Agent Dispatch" :description "Dispatch a structured event into the event-agent runtime"}
   "event_agents.run_job" {:id "event_agents.run_job" :label "Event Agent Run Job" :description "Trigger a configured event-agent job immediately"}
   "event_agents.upsert_job" {:id "event_agents.upsert_job" :label "Event Agent Upsert Job" :description "Create or update a scheduled event-agent job"}
   "schedule_event_agent" {:id "schedule_event_agent" :label "Schedule Event Agent" :description "Create or update a scheduled event-agent job with prompts, tools, triggers, and source config"}

   "bluesky.publish" {:id "bluesky.publish" :label "Bluesky" :description "Publish updates to Bluesky"}

   "music.identify_file" {:id "music.identify_file" :label "Music Identify" :description "Identify songs from audio files using AudD API"}
   "music.acoustid_lookup" {:id "music.acoustid_lookup" :label "AcoustID Lookup" :description "Look up audio fingerprints via AcoustID"}
   "music.musicbrainz_recording" {:id "music.musicbrainz_recording" :label "MusicBrainz" :description "Look up recording metadata by MBID"}
   "music.copyright_check" {:id "music.copyright_check" :label "Copyright Check" :description "Check copyright status of audio"}

   "audio.spectrogram" {:id "audio.spectrogram" :label "Audio Spectrogram" :description "Generate spectrogram from audio"}
   "audio.waveform" {:id "audio.waveform" :label "Audio Waveform" :description "Generate waveform from audio"}

   "multimodal.upload" {:id "multimodal.upload" :label "Multimodal Upload" :description "Upload images, audio, video, and documents for multimodal AI"}

   "semantic_query" {:id "semantic_query" :label "Semantic Query" :description "Query semantic context in the active corpus"}
   "graph_query" {:id "graph_query" :label "Graph Query" :description "Query the canonical knowledge graph"}
   "memory_search" {:id "memory_search" :label "Memory Search" :description "Search prior Knoxx sessions in OpenPlanner"}
   "memory_session" {:id "memory_session" :label "Memory Session" :description "Load a specific Knoxx session from OpenPlanner"}
   "save_translation" {:id "save_translation" :label "Save Translation" :description "Save translated content to database"}

   "contract.write" {:id "contract.write" :label "Contract Write" :description "Create or update a contract by writing EDN text"}

   "openplanner.query-graph" {:id "openplanner.query-graph" :label "OpenPlanner Query Graph" :description "Query the epistemic knowledge graph via MCP"}
   "openplanner.search-events" {:id "openplanner.search-events" :label "OpenPlanner Search Events" :description "Search the epistemic event store via MCP"}
   "openplanner.append-fact" {:id "openplanner.append-fact" :label "OpenPlanner Append Fact" :description "Append a Fact to the epistemic kernel"}
   "openplanner.append-obs" {:id "openplanner.append-obs" :label "OpenPlanner Append Obs" :description "Append an Observation to the epistemic kernel"}
   "openplanner.append-inference" {:id "openplanner.append-inference" :label "OpenPlanner Append Inference" :description "Append an Inference to the epistemic kernel"}
   "openplanner.append-attestation" {:id "openplanner.append-attestation" :label "OpenPlanner Append Attestation" :description "Append an Attestation to the epistemic kernel"}
   "openplanner.append-judgment" {:id "openplanner.append-judgment" :label "OpenPlanner Append Judgment" :description "Append a Judgment to the epistemic kernel"}})

(defn get-tool
  [tool-id]
  (when-let [id (some-> tool-id str str/trim not-empty)]
    (or (get tool-meta id)
        {:id id
         :label id
         :description ""})))

(defn known-tool-ids
  []
  (->> (keys tool-meta) sort vec))

(defn normalize-tool-id
  [v]
  (cond
    (keyword? v) (name v)
    (string? v) v
    :else (str v)))
