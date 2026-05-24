(ns knoxx.backend.infra.agent.hydration
  "Agent hydration orchestration: settings, passive RAG/memory hydration,
   message assembly, and tool-suite composition.  All implementation lives
   in vertical domain slices under knoxx.backend.tools.<domain>."
  (:require [clojure.string :as str]
            [knoxx.backend.infra.core-memory :refer [filter-authorized-memory-hits!]]
            [knoxx.backend.domain.contracts.sources :as sources]
            [knoxx.backend.infra.clients.openplanner :as openplanner-client]
            [knoxx.backend.infra.openplanner.memory :refer [openplanner-memory-search!]]
            [knoxx.backend.infra.defaults :refer [default-settings]]
            [knoxx.backend.domain.text :refer [value->preview-text]]
            [knoxx.backend.domain.tools :as shared]
            [knoxx.backend.infra.openplanner.semantic :as semantic]
            [knoxx.backend.domain.discord.tools :as discord]
            [knoxx.backend.domain.discord.voice-tools :as discord-voice]
            [knoxx.backend.domain.event.tools :as events]
            [knoxx.backend.domain.actor.tools :as actors]
            [knoxx.backend.infra.openplanner.tools :as openplanner]
            [knoxx.backend.domain.music :as music]
            [knoxx.backend.domain.voice.tools :as voice]
            [knoxx.backend.domain.media.blaze :as blaze]
            [knoxx.backend.domain.bluesky.bluesky :as bluesky]
            [knoxx.backend.domain.media.multimodal :as multimodal]
            [knoxx.backend.domain.media.workspace :as workspace-media]
            [knoxx.backend.tools.mcp :as mcp]
            [knoxx.backend.domain.contracts.tools :as contracts]
            [knoxx.backend.domain.nrepl :as nrepl]
            [knoxx.backend.domain.session-mycology :as session-mycology]))

(defonce settings-state* (atom nil))

(defn ensure-settings!
  [config]
  (when-not @settings-state*
    (reset! settings-state* (default-settings config)))
  @settings-state*)


(defn memory-hydration-trigger?
  [message]
  (boolean (re-find #"(?i)\b(previous|earlier|before|remember|last time|prior|session|you said|you did|we talked|we discussed)\b"
                    (or message ""))))

(defn passive-hydration!
  ([runtime config mode message] (passive-hydration! runtime config mode message nil))
  ([runtime config mode message auth-context]
   (if (= mode "rag")
     (let [started-ms (.now js/Date)
           top-k (max 1 (min 4 (or (:retrievalTopK @settings-state*) 3)))]
       (-> (semantic/semantic-search-documents! runtime config {:query message
                                                               :top-k top-k
                                                               :max-snippet-chars 240} auth-context)
           (.then (fn [result]
                    (assoc result :elapsedMs (- (.now js/Date) started-ms))))
           (.catch (fn [err]
                     (.warn js/console "[agent-hydration] passive semantic hydration failed; continuing without corpus context" err)
                     nil))))
     (js/Promise.resolve nil))))

(defn- openplanner-memory-source-options
  [config agent-spec]
  (when-let [source (sources/find-source (sources/source-specs-for-agent config agent-spec)
                                         :source/openplanner-memory)]
    (sources/source-hydration-options source)))

(defn- passive-memory-hydration-options
  [config agent-spec]
  (sources/deep-merge
   (openplanner-memory-source-options config agent-spec)
   (or (:memory-hydration agent-spec)
       (:memoryHydration agent-spec)
       (get-in agent-spec [:memory :passive-hydration])
       (get-in agent-spec [:memory :passiveHydration])
       (get-in agent-spec [:memory_hydration]))))

(defn- passive-memory-hydration-mode
  [opts]
  (let [mode (or (:mode opts) (:source-mode opts) (:sourceMode opts))]
    (some-> (cond
              (keyword? mode) (name mode)
              (some? mode) (str mode)
              :else nil)
            str/trim
            str/lower-case
            not-empty)))

(defn- positive-int-or
  [value fallback]
  (let [parsed (js/parseInt value 10)]
    (if (and (number? parsed) (not (js/isNaN parsed)) (pos? parsed))
      parsed
      fallback)))

(defn- passive-memory-hydration-enabled?
  [opts]
  (and (not (false? (:enabled? opts)))
       (not (false? (:enabled opts)))))

(defn- passive-memory-hydration-should-run?
  [message opts]
  (let [mode (passive-memory-hydration-mode opts)]
    (or (contains? #{"always" "eager" "on"} mode)
        (and (not= "off" mode)
             (memory-hydration-trigger? message)))))

(defn passive-memory-hydration!
  ([config conversation-id message] (passive-memory-hydration! config conversation-id message nil nil))
  ([config conversation-id message auth-context] (passive-memory-hydration! config conversation-id message auth-context nil))
  ([config conversation-id message auth-context agent-spec]
   (let [opts (or (passive-memory-hydration-options config agent-spec) {})]
     (if (and (openplanner-client/enabled? (openplanner-client/client config))
              (passive-memory-hydration-enabled? opts)
              (passive-memory-hydration-should-run? message opts))
       (let [started-ms (.now js/Date)
             k (max 1 (min 12 (positive-int-or (or (:k opts) (:top-k opts) (:topK opts)) 6)))]
         (-> (openplanner-memory-search! config {:query message :k k})
             (.then (fn [result]
                      (-> (filter-authorized-memory-hits! config auth-context (:hits result))
                          (.then (fn [hits]
                                   (assoc result :hits hits
                                                 :mode (or (passive-memory-hydration-mode opts) "triggered")
                                                 :elapsedMs (- (.now js/Date) started-ms)
                                                 :conversationId conversation-id))))))
             (.catch (fn [err]
                       (.warn js/console "[agent-hydration] passive OpenPlanner memory hydration failed; continuing without memory context" err)
                       nil)))
       (js/Promise.resolve nil))))))


(defn passive-hydration-text
  [hydration]
  (when (seq (:results hydration))
    (str "Passive semantic hydration from the active Knoxx corpus follows. This context is automatic and may be incomplete. Use semantic_query or semantic_read if more grounding is needed.\n\n"
         (str/join
          "\n\n"
          (map-indexed (fn [idx result]
                         (str (inc idx) ". " (:path result)
                              "\n   relevance: " (.toFixed (js/Number. (:score result)) 2)
                              (when (:indexed result)
                              (str ", indexed chunks: " (:chunkCount result)))
                              "\n   snippet: " (:snippet result)))
                       (:results hydration))))))

(defn passive-memory-hydration-text
  [memory]
  (when (seq (:hits memory))
    (str "Passive conversational memory hydration from OpenPlanner follows. This is prior Knoxx session memory and action history; verify with memory_search or memory_session if precision matters.\n\n"
         (str/join
          "\n\n"
          (map-indexed
           (fn [idx hit]
             (let [metadata (or (:metadata hit) hit)
                   session (or (:session hit) (:session metadata) "unknown-session")
                   role (or (:role hit) (:role metadata) "memory")
                   snippet (or (:snippet hit) (:document hit) (:text hit) "")]
               (str (inc idx) ". session=" session ", role=" role
                    "\n   snippet: " (or (value->preview-text snippet 260) ""))))
           (:hits memory))))))

(defn build-agent-user-message
  [message hydration memory]
  (let [parts (cond-> [(str "User request:\n" message)]
                (passive-hydration-text hydration) (conj (passive-hydration-text hydration))
                (passive-memory-hydration-text memory) (conj (passive-memory-hydration-text memory)))]
    (str/join "\n\n" parts)))

(defn build-agent-multimodal-message
  "Build a multimodal message for models that support images, audio, video, and documents.
   Returns a JavaScript array of content parts suitable for the agent SDK."
  [message content-parts hydration memory]
  (let [text-content (str/join "\n\n"
                               (cond-> [(str "User request:\n" message)]
                                 (passive-hydration-text hydration) (conj (passive-hydration-text hydration))
                                 (passive-memory-hydration-text memory) (conj (passive-memory-hydration-text memory))))
        base-parts [{:type "text" :text text-content}]]
    (if (seq content-parts)
      (clj->js (into base-parts
                     (mapv (fn [part]
                             (let [p        (if (map? part) part (js->clj part :keywordize-keys true))
                                   raw      (or (:data p) (:url p))
                                   strip-fn (fn [s]
                                              (if (and (string? s) (str/starts-with? s "data:"))
                                                (let [i (.indexOf s ",")]
                                                  (if (>= i 0) (.slice s (inc i)) s))
                                                s))
                                   data     (strip-fn raw)
                                   mime     (or (:mimeType p)
                                               (when (and (string? raw) (str/starts-with? raw "data:"))
                                                 (second (re-find #"data:([^;,]+)" raw))))
                                   ptype    (some-> (or (:type p) (aget p "type")) name)]
                               (case ptype
                                 "text"     {:type "text"     :text (str (:text p))}
                                 "image"    {:type "image"    :data data :mimeType mime}
                                 "audio"    {:type "audio"    :data data :mimeType mime}
                                 "video"    {:type "video"    :data data :mimeType mime}
                                 "document" {:type "document" :data data :mimeType mime :filename (:filename p)}
                                 {:type "text" :text (str "[Unknown: " ptype "]")})))
                           content-parts)))
      (clj->js base-parts))))

(defn hydration-sources
  [hydration]
  (if (seq (:results hydration))
    (mapv (fn [result]
            {:title (:name result)
             :url (:path result)
             :section (:snippet result)})
          (:results hydration))
    []))

(defn create-knoxx-custom-tools
  "Compose the full Knoxx tool suite from vertical domain slices."
  ([runtime config] (create-knoxx-custom-tools runtime config nil))
  ([runtime config auth-context]
   (create-knoxx-custom-tools runtime config auth-context nil))
  ([runtime config auth-context allowed-tool-ids]
   (-> (shared/sanitize-custom-tools
        (-> (semantic/create-semantic-custom-tools runtime config auth-context)
            (.concat (discord/create-discord-custom-tools runtime config auth-context))
            (.concat (discord-voice/create-discord-voice-custom-tools runtime config auth-context))
            (.concat (events/create-events-custom-tools runtime config auth-context))
            (.concat (actors/create-actors-custom-tools runtime config auth-context))
            (.concat (openplanner/create-openplanner-custom-tools runtime config auth-context))
            (.concat (music/create-music-custom-tools runtime config auth-context))
            (.concat (voice/create-voice-synth-custom-tools runtime config auth-context))
            (.concat (blaze/create-blaze-custom-tools runtime config auth-context))
            (.concat (bluesky/create-bluesky-custom-tools runtime config auth-context))
            (.concat (multimodal/create-multimodal-custom-tools runtime config auth-context))
            (.concat (workspace-media/create-workspace-media-custom-tools runtime config auth-context))
            (.concat (mcp/create-mcp-custom-tools runtime config auth-context))
            (.concat (session-mycology/create-session-mycology-tools runtime config auth-context))
            (.concat (nrepl/create-nrepl-custom-tools runtime config auth-context))))
       (shared/filter-custom-tools-by-allow-set allowed-tool-ids))))

(defn agent-custom-tool-suite
  "Compatibility wrapper for tests and older call sites."
  [agent-spec]
  (shared/agent-custom-tool-suite agent-spec))

(defn create-agent-custom-tools
  "Dispatch to the appropriate tool suite for the given agent spec."
  ([runtime config] (create-agent-custom-tools runtime config nil nil nil))
  ([runtime config auth-context] (create-agent-custom-tools runtime config auth-context nil nil))
  ([runtime config auth-context agent-spec] (create-agent-custom-tools runtime config auth-context agent-spec nil))
  ([runtime config auth-context agent-spec allowed-tool-ids]
   (case (shared/agent-custom-tool-suite agent-spec)
     :contract-librarian (contracts/create-contract-librarian-tools runtime config auth-context allowed-tool-ids)
     (create-knoxx-custom-tools runtime config auth-context allowed-tool-ids))))
