(ns knoxx.backend.tools.registry
  (:require [clojure.string :as str]))

(def ^:private tool-meta
  {"read" {:id "read" :label "Read" :description "Read files and retrieved context"}
   "write" {:id "write" :label "Write" :description "Create new markdown drafts and artifacts"}
   "edit" {:id "edit" :label "Edit" :description "Revise existing documents and drafts"}
   "bash" {:id "bash" :label "Shell" :description "Run controlled shell commands"}
   "websearch" {:id "websearch" :label "Web Search" :description "Search the live web through Proxx websearch"}
   "web.read" {:id "web.read" :label "Web Read" :description "Fetch a web link or attachment URL and extract readable content or metadata"}
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
   "voice.openutau_project" {:id "voice.openutau_project" :label "OpenUtau Project" :description "Create an OpenUtau .ustx singing project for human review and export"}

   "audio.spectrogram" {:id "audio.spectrogram" :label "Audio Spectrogram" :description "Generate spectrogram from audio"}
   "audio.waveform" {:id "audio.waveform" :label "Audio Waveform" :description "Generate waveform from audio"}

   "multimodal.upload" {:id "multimodal.upload" :label "Multimodal Upload" :description "Upload images, audio, video, and documents for multimodal AI"}
   "workspace_media.attach" {:id "workspace_media.attach" :label "Attach Workspace Media" :description "Attach an image, audio file, video, or document from the workspace into the response"}

   "semantic_query" {:id "semantic_query" :label "Semantic Query" :description "Query semantic context in the active corpus"}
   "semantic_read" {:id "semantic_read" :label "Read Document" :description "Read a document by relative path from the active Knoxx corpus"}
   "graph_query" {:id "graph_query" :label "Graph Query" :description "Query the canonical knowledge graph"}
   "memory_search" {:id "memory_search" :label "Memory Search" :description "Search prior Knoxx sessions in OpenPlanner"}
   "memory_session" {:id "memory_session" :label "Memory Session" :description "Load a specific Knoxx session from OpenPlanner"}
   "save_translation" {:id "save_translation" :label "Save Translation" :description "Save translated content to database"}
   "create_new_file" {:id "create_new_file" :label "Create New File" :description "Create a new file-backed artifact for the Knoxx canvas editor"}

   "contract.write" {:id "contract.write" :label "Contract Write" :description "Create or update a contract by writing EDN text"}

   "openplanner.query-graph" {:id "openplanner.query-graph" :label "OpenPlanner Query Graph" :description "Query the epistemic knowledge graph via MCP"}
   "openplanner.search-events" {:id "openplanner.search-events" :label "OpenPlanner Search Events" :description "Search the epistemic event store via MCP"}
   "openplanner.append-fact" {:id "openplanner.append-fact" :label "OpenPlanner Append Fact" :description "Append a Fact to the epistemic kernel"}
   "openplanner.append-obs" {:id "openplanner.append-obs" :label "OpenPlanner Append Obs" :description "Append an Observation to the epistemic kernel"}
   "openplanner.append-inference" {:id "openplanner.append-inference" :label "OpenPlanner Append Inference" :description "Append an Inference to the epistemic kernel"}
   "openplanner.append-attestation" {:id "openplanner.append-attestation" :label "OpenPlanner Append Attestation" :description "Append an Attestation to the epistemic kernel"}
   "openplanner.append-judgment" {:id "openplanner.append-judgment" :label "OpenPlanner Append Judgment" :description "Append a Judgment to the epistemic kernel"}

   "mcp.shoedelussy.write_pattern" {:id "mcp.shoedelussy.write_pattern" :label "Shoedelussy Write Pattern" :description "Write the current Shoedelussy Strudel pattern buffer"}
   "mcp.shoedelussy.get_pattern" {:id "mcp.shoedelussy.get_pattern" :label "Shoedelussy Get Pattern" :description "Read the current Shoedelussy Strudel pattern buffer"}
   "mcp.shoedelussy.mutate_pattern" {:id "mcp.shoedelussy.mutate_pattern" :label "Shoedelussy Mutate Pattern" :description "Apply a built-in mutation to the current Shoedelussy pattern"}
   "mcp.shoedelussy.inject_section" {:id "mcp.shoedelussy.inject_section" :label "Shoedelussy Inject Section" :description "Append or replace a named section in the current Shoedelussy pattern"}
   "mcp.shoedelussy.set_bpm" {:id "mcp.shoedelussy.set_bpm" :label "Shoedelussy Set BPM" :description "Set BPM metadata and project tempo in Shoedelussy"}
   "mcp.shoedelussy.set_key" {:id "mcp.shoedelussy.set_key" :label "Shoedelussy Set Key" :description "Set the musical key for a Shoedelussy project"}
   "mcp.shoedelussy.get_state" {:id "mcp.shoedelussy.get_state" :label "Shoedelussy Get State" :description "Read BPM, key, code, and section summary for a Shoedelussy project"}
   "mcp.shoedelussy.list_projects" {:id "mcp.shoedelussy.list_projects" :label "Shoedelussy List Projects" :description "List Shoedelussy projects available through the MCP server"}
   "mcp.shoedelussy.load_project" {:id "mcp.shoedelussy.load_project" :label "Shoedelussy Load Project" :description "Load a Shoedelussy project by id"}
   "mcp.shoedelussy.save_snapshot" {:id "mcp.shoedelussy.save_snapshot" :label "Shoedelussy Save Snapshot" :description "Save a new snapshot/version of a Shoedelussy project"}})

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
