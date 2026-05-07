(ns knoxx.backend.tools.registry
  (:require [clojure.string :as str]))

(def ^:private tool-meta
  {"read" {:id "read" :label "Read" :description "Read files and retrieved context" :risk-level "low"}
   "write" {:id "write" :label "Write" :description "Create new markdown drafts and artifacts" :risk-level "medium"}
   "edit" {:id "edit" :label "Edit" :description "Revise existing documents and drafts" :risk-level "medium"}
   "bash" {:id "bash" :label "Shell" :description "Run controlled shell commands" :risk-level "high"}
   "websearch" {:id "websearch" :label "Web Search" :description "Search the live web through Proxx websearch" :risk-level "low"}
   "web.read" {:id "web.read" :label "Web Read" :description "Fetch a web link or attachment URL and extract readable content or metadata" :risk-level "low"}
   "canvas" {:id "canvas" :label "Canvas" :description "Open long-form markdown drafting canvas" :risk-level "low"}

   "email.send" {:id "email.send" :label "Email" :description "Send drafts through configured email account" :risk-level "low"}

   "discord.publish" {:id "discord.publish" :label "Discord Publish" :description "Publish updates to Discord" :risk-level "medium"}
   "discord.send" {:id "discord.send" :label "Discord Send" :description "Send Discord messages and replies" :risk-level "medium"}
   "discord.read" {:id "discord.read" :label "Discord Read" :description "Read messages from Discord channels" :risk-level "medium"}
   "discord.channel.messages" {:id "discord.channel.messages" :label "Discord Channel Messages" :description "Fetch messages from a Discord channel" :risk-level "medium"}
   "discord.channel.scroll" {:id "discord.channel.scroll" :label "Discord Channel Scroll" :description "Scroll older messages in a Discord channel" :risk-level "medium"}
   "discord.dm.messages" {:id "discord.dm.messages" :label "Discord DM Messages" :description "Fetch messages from a Discord DM channel" :risk-level "medium"}
   "discord.search" {:id "discord.search" :label "Discord Search" :description "Search messages in Discord channels" :risk-level "medium"}
   "discord.guilds" {:id "discord.guilds" :label "Discord Guilds" :description "List Discord servers the bot is in" :risk-level "medium"}
   "discord.channels" {:id "discord.channels" :label "Discord Channels" :description "List channels in a Discord server" :risk-level "medium"}
   "discord.list.servers" {:id "discord.list.servers" :label "Discord List Servers" :description "List all Discord servers the bot can access" :risk-level "medium"}
   "discord.list.channels" {:id "discord.list.channels" :label "Discord List Channels" :description "List channels across one or all Discord servers" :risk-level "medium"}

   "event_agents.status" {:id "event_agents.status" :label "Event Agent Status" :description "Inspect scheduled event-agent runtime state and configuration" :risk-level "low"}
   "event_agents.dispatch" {:id "event_agents.dispatch" :label "Event Agent Dispatch" :description "Dispatch a structured event into the event-agent runtime" :risk-level "low"}
   "event_agents.run_job" {:id "event_agents.run_job" :label "Event Agent Run Job" :description "Trigger a configured event-agent job immediately" :risk-level "low"}
   "event_agents.upsert_job" {:id "event_agents.upsert_job" :label "Event Agent Upsert Job" :description "Create or update a scheduled event-agent job" :risk-level "high"}
   "schedule_event_agent" {:id "schedule_event_agent" :label "Schedule Event Agent" :description "Create or update a scheduled event-agent job with prompts, tools, triggers, and source config" :risk-level "high"}
   "events.status" {:id "events.status" :label "Events Status" :description "Inspect generic events runtime state and trigger configuration" :risk-level "low"}
   "events.dispatch" {:id "events.dispatch" :label "Events Dispatch" :description "Dispatch a normalized event into the generic events runtime" :risk-level "low"}
   "agents.spawn" {:id "agents.spawn" :label "Agents Spawn" :description "Launch a one-off normal Knoxx agent run through the shared agent runtime" :risk-level "medium"}

   "sandbox_container.create" {:id "sandbox_container.create" :label "Sandbox Create" :description "Create a TTL-bound sandbox container for isolated development work" :risk-level "low"}
   "sandbox_container.status" {:id "sandbox_container.status" :label "Sandbox Status" :description "Inspect sandbox container runtime status and remaining TTL" :risk-level "low"}
   "sandbox_container.exec" {:id "sandbox_container.exec" :label "Sandbox Exec" :description "Execute a shell command inside a sandbox container" :risk-level "low"}
   "sandbox_container.read" {:id "sandbox_container.read" :label "Sandbox Read" :description "Read a text file from the sandbox workdir" :risk-level "low"}
   "sandbox_container.write" {:id "sandbox_container.write" :label "Sandbox Write" :description "Write a text file into the sandbox workdir" :risk-level "low"}
   "sandbox_container.commit" {:id "sandbox_container.commit" :label "Sandbox Commit" :description "Create a git commit inside the sandbox workdir" :risk-level "low"}
   "sandbox_container.destroy" {:id "sandbox_container.destroy" :label "Sandbox Destroy" :description "Destroy a sandbox container and its temporary workspace" :risk-level "low"}

   "bluesky.publish" {:id "bluesky.publish" :label "Bluesky Publish" :description "Publish updates to Bluesky" :risk-level "low"}
   "bluesky.profile" {:id "bluesky.profile" :label "Bluesky Profile" :description "Read a Bluesky profile by handle or DID" :risk-level "low"}
   "bluesky.search" {:id "bluesky.search" :label "Bluesky Search" :description "Search public Bluesky posts or actors" :risk-level "low"}
   "bluesky.author.feed" {:id "bluesky.author.feed" :label "Bluesky Author Feed" :description "Read recent posts from a specific Bluesky author" :risk-level "low"}
   "bluesky.timeline" {:id "bluesky.timeline" :label "Bluesky Timeline" :description "Read the authenticated Bluesky timeline" :risk-level "low"}

   "music.identify_file" {:id "music.identify_file" :label "Music Identify" :description "Identify songs from audio files using AudD API" :risk-level "medium"}
   "music.acoustid_lookup" {:id "music.acoustid_lookup" :label "AcoustID Lookup" :description "Look up audio fingerprints via AcoustID" :risk-level "medium"}
   "music.musicbrainz_recording" {:id "music.musicbrainz_recording" :label "MusicBrainz" :description "Look up recording metadata by MBID" :risk-level "medium"}
   "music.copyright_check" {:id "music.copyright_check" :label "Copyright Check" :description "Check copyright status of audio" :risk-level "medium"}
   "music.generate" {:id "music.generate" :label "Generate Music" :description "Synthesize a WAV file from a JSON music spec using the native Node.js audio engine" :risk-level "medium"}
   "voice.openutau_project" {:id "voice.openutau_project" :label "OpenUtau Project" :description "Create an OpenUtau .ustx singing project for human review and export" :risk-level "low"}
   "voice.tts"        {:id "voice.tts"        :label "Text-to-Speech" :description "Synthesize speech via Voxx Gateway TTS; writes MP3 to workspace"  :risk-level "low"}
   "voice.tts_stream" {:id "voice.tts_stream" :label "TTS Stream"     :description "Return Voxx WS streaming TTS session params for /ws/voice/tts" :risk-level "low"}
   "discord.voice.join"   {:id "discord.voice.join"   :label "Voice Join"   :description "Join a Discord voice channel" :risk-level "low"}
   "discord.voice.leave"  {:id "discord.voice.leave"  :label "Voice Leave"  :description "Leave a Discord voice channel" :risk-level "low"}
   "discord.voice.say"    {:id "discord.voice.say"    :label "Voice Say"    :description "Synthesize speech and play in a Discord voice channel" :risk-level "low"}
   "discord.voice.status" {:id "discord.voice.status" :label "Voice Status" :description "Check Discord voice connection status" :risk-level "low"}
   "discord.voice.connect" {:id "discord.voice.connect" :label "Voice Connect" :description "Join a voice channel and start listening/transcription" :risk-level "low"}
   "discord.voice.listen" {:id "discord.voice.listen" :label "Voice Listen" :description "Listen for user speech and transcribe into agent session" :risk-level "low"}
   "discord.voice.stop_listen" {:id "discord.voice.stop_listen" :label "Voice Stop Listen" :description "Stop listening for voice input" :risk-level "low"}
   "discord.voice.list_members" {:id "discord.voice.list_members" :label "List Voice Members" :description "List members in a voice channel" :risk-level "low"}

   "audio.spectrogram" {:id "audio.spectrogram" :label "Audio Spectrogram" :description "Generate spectrogram from audio" :risk-level "medium"}
   "audio.waveform" {:id "audio.waveform" :label "Audio Waveform" :description "Generate waveform from audio" :risk-level "medium"}

   "multimodal.upload" {:id "multimodal.upload" :label "Multimodal Upload" :description "Upload images, audio, video, and documents for multimodal AI" :risk-level "low"}
   "workspace_media.attach" {:id "workspace_media.attach" :label "Attach Workspace Media" :description "Attach an image, audio file, video, or document from the workspace into the response" :risk-level "low"}

   "semantic_query" {:id "semantic_query" :label "Semantic Query" :description "Query semantic context in the active corpus" :risk-level "low"}
   "semantic_read" {:id "semantic_read" :label "Read Document" :description "Read a document by relative path from the active Knoxx corpus" :risk-level "low"}
   "graph_query" {:id "graph_query" :label "Graph Query" :description "Query the canonical knowledge graph" :risk-level "low"}
   "memory_search" {:id "memory_search" :label "Memory Search" :description "Search prior Knoxx sessions in OpenPlanner" :risk-level "low"}
   "memory_session" {:id "memory_session" :label "Memory Session" :description "Load a specific Knoxx session from OpenPlanner" :risk-level "low"}
   "save_translation" {:id "save_translation" :label "Save Translation" :description "Save translated content to database" :risk-level "low"}
   "create_new_file" {:id "create_new_file" :label "Create New File" :description "Create a new file-backed artifact for the Knoxx canvas editor" :risk-level "low"}

   "contract.write" {:id "contract.write" :label "Contract Write" :description "Create or update a contract by writing EDN text" :risk-level "high"}
   "contract.validate" {:id "contract.validate" :label "Contract Validate" :description "Parse and validate EDN contract text without saving. Returns errors with line hints." :risk-level "low"}

   "nrepl.eval" {:id "nrepl.eval" :label "nREPL Eval" :description "Evaluate CLJ/CLJS in the live shadow-cljs runtime via nREPL (developer-only)" :risk-level "high"}

   "session_mycology" {:id "session_mycology" :label "Session Mycology" :description "Per-turn retrospection with p-scores and skill spore incubation" :risk-level "low"}

   "openplanner.query-graph" {:id "openplanner.query-graph" :label "OpenPlanner Query Graph" :description "Query the epistemic knowledge graph via MCP" :risk-level "medium"}
   "openplanner.search-events" {:id "openplanner.search-events" :label "OpenPlanner Search Events" :description "Search the epistemic event store via MCP" :risk-level "medium"}
   "openplanner.append-fact" {:id "openplanner.append-fact" :label "OpenPlanner Append Fact" :description "Append a Fact to the epistemic kernel" :risk-level "medium"}
   "openplanner.append-obs" {:id "openplanner.append-obs" :label "OpenPlanner Append Obs" :description "Append an Observation to the epistemic kernel" :risk-level "medium"}
   "openplanner.append-inference" {:id "openplanner.append-inference" :label "OpenPlanner Append Inference" :description "Append an Inference to the epistemic kernel" :risk-level "medium"}
   "openplanner.append-attestation" {:id "openplanner.append-attestation" :label "OpenPlanner Append Attestation" :description "Append an Attestation to the epistemic kernel" :risk-level "medium"}
   "openplanner.append-judgment" {:id "openplanner.append-judgment" :label "OpenPlanner Append Judgment" :description "Append a Judgment to the epistemic kernel" :risk-level "medium"}

   "mcp.shoedelussy.write_pattern" {:id "mcp.shoedelussy.write_pattern" :label "Shoedelussy Write Pattern" :description "Write Strudel code to the Shoedelussy MCP scratch buffer and mirror it into a durable project when project_id already exists" :risk-level "low"}
   "mcp.shoedelussy.get_pattern" {:id "mcp.shoedelussy.get_pattern" :label "Shoedelussy Get Pattern" :description "Read the Shoedelussy MCP scratch buffer, or fall back to durable project code when available" :risk-level "low"}
   "mcp.shoedelussy.mutate_pattern" {:id "mcp.shoedelussy.mutate_pattern" :label "Shoedelussy Mutate Pattern" :description "Apply a built-in mutation to the current Shoedelussy scratch or project-backed pattern" :risk-level "low"}
   "mcp.shoedelussy.inject_section" {:id "mcp.shoedelussy.inject_section" :label "Shoedelussy Inject Section" :description "Append or replace a named section in the current Shoedelussy scratch or project-backed pattern" :risk-level "low"}
   "mcp.shoedelussy.create_project" {:id "mcp.shoedelussy.create_project" :label "Shoedelussy Create Project" :description "Create or update a durable Shoedelussy project from inline code or the current scratch pattern" :risk-level "low"}
   "mcp.shoedelussy.set_bpm" {:id "mcp.shoedelussy.set_bpm" :label "Shoedelussy Set BPM" :description "Set BPM metadata and project tempo for a durable Shoedelussy project" :risk-level "low"}
   "mcp.shoedelussy.set_key" {:id "mcp.shoedelussy.set_key" :label "Shoedelussy Set Key" :description "Set the musical key for a durable Shoedelussy project" :risk-level "low"}
   "mcp.shoedelussy.get_state" {:id "mcp.shoedelussy.get_state" :label "Shoedelussy Get State" :description "Read BPM, key, code, and section summary for a durable Shoedelussy project" :risk-level "low"}
   "mcp.shoedelussy.list_projects" {:id "mcp.shoedelussy.list_projects" :label "Shoedelussy List Projects" :description "List durable Shoedelussy projects available through the MCP server" :risk-level "low"}
   "mcp.shoedelussy.load_project" {:id "mcp.shoedelussy.load_project" :label "Shoedelussy Load Project" :description "Load a durable Shoedelussy project by id" :risk-level "low"}
   "mcp.shoedelussy.save_snapshot" {:id "mcp.shoedelussy.save_snapshot" :label "Shoedelussy Save Snapshot" :description "Save a new snapshot/version of a durable Shoedelussy project" :risk-level "low"}
   "mcp.shoedelussy.render_loop" {:id "mcp.shoedelussy.render_loop" :label "Shoedelussy Render Loop" :description "Create a share-backed browser playback/render URL for Strudel code; this is a browser render link, not a server wav/mp3 export" :risk-level "low"}
    "mcp.shoedelussy.render_wav" {:id "mcp.shoedelussy.render_wav" :label "Shoedelussy Render WAV" :description "Create a browser URL that attempts WAV capture/download from the Shoedelussy frontend shell; this is browser-side export, not a headless server renderer" :risk-level "medium"}

    "memory.temp" {:id "memory.temp" :label "Temporary Memory" :description "Read or write short-lived keyed data with a TTL for pipeline steps" :risk-level "low"}})

(defn known-tool-ids
  []
  (->> (keys tool-meta) sort vec))

(defn- sanitized-alias
  [tool-id]
  (some-> tool-id
          str
          (str/replace #"[^A-Za-z0-9_-]" "_")
          (str/replace #"_+" "_")))

(defn normalize-tool-id
  [v]
  (cond
    (keyword? v) (name v)
    (string? v)
    (let [trimmed (str/trim v)]
      (cond
        (contains? tool-meta trimmed) trimmed
        :else (or (some (fn [tool-id]
                          (when (= trimmed (sanitized-alias tool-id))
                            tool-id))
                        (keys tool-meta))
                  trimmed)))
    :else (str v)))

(defn get-tool
  [tool-id]
  (when-let [id (some-> tool-id normalize-tool-id str str/trim not-empty)]
    (or (get tool-meta id)
        {:id id
         :label id
         :description ""})))
