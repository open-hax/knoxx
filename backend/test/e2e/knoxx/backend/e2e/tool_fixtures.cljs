(ns knoxx.backend.e2e.tool-fixtures
  "What the e2e sweep knows about each MCP tool.

   Data in a namespace rather than a config file, so a renamed parameter is a
   compile-time neighbour of the schema it mirrors instead of a JSON string
   nobody checks.

   Exactly one of these keys says what the suite does with a tool:

     :args   — call it with these arguments. Cheap and idempotent.
     :needs  — a live handle no fixture can invent (a channel id, a sandbox
               id, an at:// uri). Not called; recorded so the coverage report
               can tell \"nobody wrote a fixture\" from \"a fixture is
               impossible here\".
     :absent — this tool is not on the MCP surface, and why. These entries are
               findings: several came from the first e2e run, which is the
               first time anything had enumerated the served catalog.
     :covered-by — a dedicated e2e namespace exercises it properly. The sweep
               cannot: sandbox and nREPL calls only mean anything as a chain
               threading state the previous call returned, and the Discord
               tools are worth asserting on identity rather than on returning.

   An :args fixture may additionally declare :allowed-error, an exact message
   substring for a dependency failure the harness intentionally cannot satisfy.
   Anything broader would turn a new handler defect into a green sweep.

   :write marks a tool that mutates or publishes; the sweep skips those.

   A tool with no entry at all is reported as uncovered, and mcp-tools-e2e
   asserts coverage never quietly collapses."
  (:require [clojure.string :as str]))

(def ^:private librarian-only
  "contract tools ship in the contract-librarian suite, not create-knoxx-custom-tools")

(def ^:private registry-only
  "named in registry/tools but built by no factory — not on the agent tool surface")

(def ^:private bridged
  "bridged from an external MCP server; present only when MCP_SERVERS names it")

(def ^:private openplanner-not-configured
  "OpenPlanner is not configured")

(def fixtures
  {;; ── corpus and memory ────────────────────────────────────────────────────
   "semantic_query"   {:args {:query "knoxx architecture" :topK 3}
                       :allowed-error openplanner-not-configured}
   "graph_query"      {:args {:query "knoxx" :limit 3}
                       :allowed-error openplanner-not-configured}
   "memory_search"    {:args {:query "mcp" :k 3}
                       :allowed-error openplanner-not-configured}
   "memory_session"   {:needs "a real session id"}
   "websearch"        {:args {:query "model context protocol" :numResults 3}}
   "web_read"         {:args {:url "https://example.com/" :maxChars 500}}
   "push_claim"       {:write true :args {:claim "e2e smoke claim" :probability 0.5}}
   "save_translation" {:needs "a real document_id"}
   "create_new_file"  {:write true :args {:title "e2e"
                                          :path "scratch/e2e.md"
                                          :content "e2e"}}

   ;; ── events ───────────────────────────────────────────────────────────────
   "events_status"       {:args {}}
   "events_dispatch"     {:write true :args {:event_type "manual.note"
                                             :generator_kind "manual"}}
   "triggers_fire"       {:write true :needs "a trigger id whose actions are safe to fire"}
   "agents_spawn"        {:write true :needs "a billable agent run"}
   "actors_send-message" {:write true :needs "a live mailbox"}

   ;; ── contracts: not served over MCP ───────────────────────────────────────
   "contract_list"     {:absent librarian-only}
   "contract_read"     {:absent librarian-only}
   "contract_validate" {:absent librarian-only}
   "contract_write"    {:absent librarian-only}

   ;; ── credential-backed: discord ───────────────────────────────────────────
   "discord_list_servers"     {:covered-by "discord-identity-e2e"}
   "discord_list_channels"    {:args {}}
   "discord_guilds"           {:args {}}
   "discord_channels"         {:needs "a guild id"}
   "discord_channel_messages" {:needs "a channel id"}
   "discord_channel_scroll"   {:needs "a channel id and an oldest seen id"}
   "discord_dm_messages"      {:needs "a user id"}
   "discord_search"           {:needs "a channel or user id to scope the search"}
   "discord_read"             {:needs "a channel id"}
   "discord_react"            {:write true :needs "a message id"}
   "discord_thread_create"    {:write true :needs "a channel id"}
   "discord_send"             {:write true :needs "a channel id"}
   "discord_publish"          {:write true :needs "a channel id"}

   ;; ── credential-backed: bluesky ───────────────────────────────────────────
   "bluesky_profile"       {:args {}}
   "bluesky_search"        {:args {:query "clojure" :kind "posts" :limit 3}}
   "bluesky_timeline"      {:args {:limit 3}}
   "bluesky_author_feed"   {:args {:actor "bsky.app" :limit 3}}
   "bluesky_followers"     {:args {:actor "bsky.app"}}
   "bluesky_follows"       {:args {:actor "bsky.app"}}
   "bluesky_notifications" {:args {}}
   "bluesky_chat_list"     {:args {}}
   "bluesky_thread"        {:needs "an at:// uri"}
   "bluesky_chat_read"     {:needs "a convo id"}
   "bluesky_publish"       {:write true :args {:text "e2e smoke post"}}
   "bluesky_repost"        {:write true :needs "a post uri"}
   "bluesky_like"          {:write true :needs "a post uri"}
   "bluesky_unlike"        {:write true :needs "a liked post uri"}
   "bluesky_follow"        {:write true :needs "an actor"}
   "bluesky_unfollow"      {:write true :needs "a followed actor"}
   "bluesky_delete"        {:write true :needs "a post uri"}
   "bluesky_chat_send"     {:write true :needs "a convo id"}
   "bluesky_chat_react"    {:write true :needs "a message id"}

   ;; ── openplanner epistemic kernel: not served over MCP ────────────────────
   "openplanner_query-graph"        {:absent registry-only}
   "openplanner_search-events"      {:absent registry-only}
   "openplanner_append-fact"        {:absent registry-only}
   "openplanner_append-obs"         {:absent registry-only}
   "openplanner_append-inference"   {:absent registry-only}
   "openplanner_append-attestation" {:absent registry-only}
   "openplanner_append-judgment"    {:absent registry-only}
   "email_send"                     {:absent "an HTTP route (/api/tools/email/send); there is no MCP tool"}

   ;; ── sandbox ──────────────────────────────────────────────────────────────
   "sandbox_container_create"  {:covered-by "sandbox-e2e"}
   "sandbox_container_status"  {:covered-by "sandbox-e2e"}
   "sandbox_container_exec"    {:covered-by "sandbox-e2e"}
   "sandbox_container_read"    {:covered-by "sandbox-e2e"}
   "sandbox_container_write"   {:covered-by "sandbox-e2e"}
   "sandbox_container_commit"  {:covered-by "sandbox-e2e"}
   "sandbox_container_destroy" {:covered-by "sandbox-e2e"}

   ;; ── media and voice ──────────────────────────────────────────────────────
   "music_musicbrainz_recording" {:args {:mbid "b9ad642e-b012-41c7-b72a-42cf4911f9ff"}}
   "music_identify_file"         {:needs "an audio file"}
   "music_acoustid_lookup"       {:needs "an audio fingerprint"}
   "music_copyright_check"       {:needs "an audio file"}
   "music_generate"              {:write true :needs "a music spec and disk"}
   "music_generate_song"         {:write true :needs "a billable generation"}
   "image_generate"              {:write true :needs "a billable generation"}
   "video_generate"              {:write true :needs "a billable generation"}
   "blaze_generate"              {:write true :needs "a billable generation"}
   "voice_tts"                   {:write true :needs "workspace disk"}
   "voice_tts_stream"            {:args {:text "e2e"}}
   "voice_openutau_project"      {:write true :needs "workspace disk"}
   "voice_openutau_render"       {:write true :needs "a .ustx project"}
   "audio_spectrogram"           {:needs "an audio file"}
   "audio_waveform"              {:needs "an audio file"}
   "multimodal_upload"           {:write true :needs "a file to upload"}
   "workspace_media_attach"      {:needs "a workspace media path"}

   ;; ── discord voice ────────────────────────────────────────────────────────
   "discord_voice_status"                {:covered-by "discord-identity-e2e"}
   "discord_voice_join"                  {:covered-by "discord-identity-e2e"}
   "discord_voice_leave"                 {:write true :needs "a live voice connection"}
   "discord_voice_say"                   {:write true :needs "a live voice channel"}
   "discord_voice_connect"               {:write true :needs "a live voice channel"}
   "discord_voice_listen"                {:write true :needs "a live voice connection"}
   "discord_voice_stop_listen"           {:write true :needs "a live voice connection"}
   "discord_voice_list_members"          {:needs "a voice channel id"}
   "discord_voice_agent_event_connect"   {:write true :needs "a live voice channel"}
   "discord_voice_agent_event_listen"    {:write true :needs "a live voice connection"}

   ;; ── shoedelussy: bridged, absent unless configured ───────────────────────
   "mcp_shoedelussy_write_pattern"  {:absent bridged}
   "mcp_shoedelussy_get_pattern"    {:absent bridged}
   "mcp_shoedelussy_mutate_pattern" {:absent bridged}
   "mcp_shoedelussy_inject_section" {:absent bridged}
   "mcp_shoedelussy_create_project" {:absent bridged}
   "mcp_shoedelussy_set_bpm"        {:absent bridged}
   "mcp_shoedelussy_set_key"        {:absent bridged}
   "mcp_shoedelussy_get_state"      {:absent bridged}
   "mcp_shoedelussy_list_projects"  {:absent bridged}
   "mcp_shoedelussy_load_project"   {:absent bridged}
   "mcp_shoedelussy_save_snapshot"  {:absent bridged}
   "mcp_shoedelussy_render_loop"    {:absent bridged}
   "mcp_shoedelussy_render_wav"     {:absent bridged}

   ;; ── developer ────────────────────────────────────────────────────────────
   "nrepl_eval"       {:covered-by "nrepl-e2e"}
   "session_mycology" {:write true :needs "learning records on disk"}})

(defn callable
  "Fixtures the sweep will actually invoke: those with args and nothing else
   standing in the way."
  [include-writes?]
  (into {}
        (filter (fn [[_ fixture]]
                  (and (contains? fixture :args)
                       (not (:needs fixture))
                       (not (:absent fixture))
                       (not (:covered-by fixture))
                       (or include-writes? (not (:write fixture))))))
        fixtures))

(defn allowed-tool-error?
  "True only when `detail` contains this fixture's reviewed dependency error."
  [tool-name detail]
  (when-let [expected (some-> (get fixtures tool-name) :allowed-error)]
    (str/includes? (str detail) (str expected))))

(defn disposition-faults
  "Fixture entries that name more or fewer than one execution disposition."
  []
  (->> fixtures
       (keep (fn [[tool-name fixture]]
               (let [present (filter #(contains? fixture %)
                                     [:args :needs :absent :covered-by])]
                 (when-not (= 1 (count present))
                   [tool-name (vec present)]))))
       (into (sorted-map))))

(defn covered-elsewhere
  "Tool name -> the e2e namespace that exercises it beyond the sweep."
  []
  (into (sorted-map)
        (keep (fn [[tool-name fixture]]
                (when-let [ns-name (:covered-by fixture)] [tool-name ns-name])))
        fixtures))

(defn uncovered
  "Tool names in `tool-names` that no fixture mentions at all."
  [tool-names]
  (vec (sort (remove #(contains? fixtures %) tool-names))))

(defn stale
  "Fixtures naming a tool the server does not expose and does not explain.

   An :absent entry is not stale — it is a recorded reason. Everything else
   that has stopped matching is a rename or a removal nobody propagated."
  [tool-names]
  (let [exposed (set tool-names)]
    (->> fixtures
         (remove (fn [[tool-name fixture]]
                   (or (contains? exposed tool-name) (:absent fixture))))
         (map key)
         sort
         vec)))

(defn wrongly-absent
  "Fixtures marked :absent for a tool the server does in fact expose.

   The reason attached to an :absent entry is a claim about the system. When
   the tool shows up, the claim is stale and worth more than a silent pass."
  [tool-names]
  (let [exposed (set tool-names)]
    (->> fixtures
         (filter (fn [[tool-name fixture]]
                   (and (:absent fixture) (contains? exposed tool-name))))
         (map (fn [[tool-name fixture]]
                (str tool-name " is exposed, but is marked absent: " (:absent fixture))))
         sort
         vec)))

(defn absence-report
  "One line per tool the fixtures say is off the MCP surface, with the reason."
  []
  (->> fixtures
       (keep (fn [[tool-name fixture]]
               (when-let [reason (:absent fixture)]
                 (str "  " tool-name " — " reason))))
       sort
       (str/join "\n")))
