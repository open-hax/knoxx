(ns knoxx.backend.agent-hydration
  "Agent hydration orchestration: settings, passive RAG/memory hydration,
   message assembly, and tool-suite composition.  All implementation lives
   in vertical domain slices under knoxx.backend.tools.<domain>."
  (:require [clojure.string :as str]
            [knoxx.backend.core-memory :refer [fetch-openplanner-session-rows! filter-authorized-memory-hits! session-visible?]]
            [knoxx.backend.http :refer [openplanner-enabled?]]
            [knoxx.backend.openplanner-memory :refer [openplanner-memory-search!]]
            [knoxx.backend.runtime.defaults :refer [default-settings]]
            [knoxx.backend.text :refer [value->preview-text clip-text]]
            [knoxx.backend.tools.shared :as shared]
            [knoxx.backend.tools.semantic :as semantic]
            [knoxx.backend.tools.discord :as discord]
            [knoxx.backend.tools.event-agents :as event-agents]
            [knoxx.backend.tools.openplanner :as openplanner]
            [knoxx.backend.tools.music :as music]
            [knoxx.backend.tools.voice :as voice]
            [knoxx.backend.tools.multimodal :as multimodal]
            [knoxx.backend.tools.workspace-media :as workspace-media]
            [knoxx.backend.tools.mcp :as mcp]
            [knoxx.backend.tools.contracts :as contracts]))

(defonce settings-state* (atom nil))

(defn ensure-settings!
  [config]
  (when-not @settings-state*
    (reset! settings-state* (default-settings config)))
  @settings-state*)

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
                    (assoc result :elapsedMs (- (.now js/Date) started-ms))))))
     (js/Promise.resolve nil))))

(defn memory-hydration-trigger?
  [message]
  (boolean (re-find #"(?i)\b(previous|earlier|before|remember|last time|prior|session|you said|you did|we talked|we discussed)\b"
                    (or message ""))))

(defn passive-memory-hydration!
  ([config conversation-id message] (passive-memory-hydration! config conversation-id message nil))
  ([config conversation-id message auth-context]
   (if (and (openplanner-enabled? config)
            (memory-hydration-trigger? message))
     (let [started-ms (.now js/Date)]
       (-> (openplanner-memory-search! config {:query message :k 4})
           (.then (fn [result]
                    (-> (filter-authorized-memory-hits! config auth-context (:hits result))
                        (.then (fn [hits]
                                 (assoc result :hits hits
                                               :elapsedMs (- (.now js/Date) started-ms)
                                               :conversationId conversation-id))))))))
     (js/Promise.resolve nil))))


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
      (let [multimodal-parts (mapv (fn [part]
                                     (let [type (:type part)
                                           data (or (:data part) (:url part))
                                           mime-type (:mimeType part)
                                           text (:text part)]
                                       (case type
                                         :text {:type "text" :text (str text)}
                                         :image {:type "image" :data data :mimeType mime-type}
                                         :audio {:type "audio" :data data :mimeType mime-type}
                                         :video {:type "video" :data data :mimeType mime-type}
                                         :document {:type "document" :data data :mimeType mime-type :filename (:filename part)}
                                         {:type "text" :text (str "[Unknown content type: " type "]")})))
                                   content-parts)]
        (clj->js (into base-parts multimodal-parts)))
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
        (.concat (.concat (.concat (.concat (.concat (.concat (.concat (.concat (semantic/create-semantic-custom-tools runtime config auth-context)
                                                                                (discord/create-discord-custom-tools runtime config auth-context))
                                                                       (event-agents/create-event-agent-custom-tools runtime config auth-context))
                                                              (openplanner/create-openplanner-custom-tools runtime config auth-context))
                                                     (music/create-music-custom-tools runtime config auth-context))
                                            (voice/create-voice-synth-custom-tools runtime config auth-context))
                                   (multimodal/create-multimodal-custom-tools runtime config auth-context))
                          (workspace-media/create-workspace-media-custom-tools runtime config auth-context))
                   (mcp/create-mcp-custom-tools runtime config auth-context)))
       (shared/filter-custom-tools-by-allow-set allowed-tool-ids))))

(defn create-agent-custom-tools
  "Dispatch to the appropriate tool suite for the given agent spec."
  ([runtime config] (create-agent-custom-tools runtime config nil nil nil))
  ([runtime config auth-context] (create-agent-custom-tools runtime config auth-context nil nil))
  ([runtime config auth-context agent-spec] (create-agent-custom-tools runtime config auth-context agent-spec nil))
  ([runtime config auth-context agent-spec allowed-tool-ids]
   (case (shared/agent-custom-tool-suite agent-spec)
     :contract-librarian (contracts/create-contract-librarian-tools runtime config auth-context allowed-tool-ids)
     (create-knoxx-custom-tools runtime config auth-context allowed-tool-ids))))
