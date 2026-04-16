(ns kms-ingestion.drivers.pi-sessions
  "Driver for ingesting pi coding agent session JSONL files into OpenPlanner.

   Each JSONL file in ~/.pi/agent/sessions/<workspace>/ contains a pi session
   with events like session start, messages, tool calls, reasoning, etc.
   This driver discovers new/modified sessions, parses them, and maps them
   to OpenPlanner event v1 envelopes for ingestion via POST /v1/events.

   The driver stores state as a map of session-id -> {mtime, event-count}
   so incremental scans skip already-ingested sessions."
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [kms-ingestion.drivers.protocol :as protocol])
  (:import
   [java.io BufferedReader FileReader]
   [java.time Instant]))

;; ---------------------------------------------------------------------------
;; Config
;; ---------------------------------------------------------------------------

(def ^:private default-sessions-root
  (or (System/getenv "PI_SESSIONS_ROOT")
      "/home/err/.pi/agent/sessions"))

(def ^:private max-text-length 12000)
(def ^:private max-session-size 20000000) ; 20MB
(def ^:private session-project
  (or (System/getenv "PI_SESSION_PROJECT")
      "knoxx-session"))

(def ^:private supported-event-types
  #{"session" "message" "compaction" "model_change"
    "thinking_level_change" "custom_message" "branch_summary"})

;; ---------------------------------------------------------------------------
;; JSONL Parsing
;; ---------------------------------------------------------------------------

(defn- parse-jsonl-lines
  "Parse JSONL lines, skipping blanks and parse errors."
  [lines]
  (keep #(try (json/parse-string % true) (catch Exception _ nil)) lines))

(defn- read-jsonl-file
  "Read a JSONL file and return parsed entries."
  [^java.io.File file]
  (with-open [reader (BufferedReader. (FileReader. file))]
    (doall (parse-jsonl-lines (line-seq reader)))))

;; ---------------------------------------------------------------------------
;; Text helpers
;; ---------------------------------------------------------------------------

(defn- truncate-text
  [text]
  (if (and text (> (count text) max-text-length))
    (str (subs text 0 max-text-length) "\n... [truncated " (- (count text) max-text-length) " chars]")
    text))

(defn- extract-text-content
  "Extract text from a pi message content array."
  [content]
  (when (sequential? content)
    (->> content
         (filter #(and (= "text" (:type %)) (:text %)))
         (map :text)
         (str/join "\n"))))

(defn- extract-tool-calls
  [content]
  (when (sequential? content)
    (filter #(= "toolCall" (:type %)) content)))

(defn- extract-thinking
  [content]
  (when (sequential? content)
    (->> content
         (filter #(and (= "thinking" (:type %)) (:text %)))
         (map :text)
         (str/join "\n"))))

(defn- cwd-to-project
  "Map a pi workspace cwd to an OpenPlanner project label."
  [cwd]
  (when cwd
    (let [home (System/getenv "HOME")]
      (cond-> cwd
        (and home (str/starts-with? cwd home))
        (str/replace (str home "/") "")
        true
        (str/replace #"[\\/]+" "--")
        true
        (str/replace #"--$" "")))))

;; ---------------------------------------------------------------------------
;; Event Mapping
;; ---------------------------------------------------------------------------

(defn- make-event
  [id ts kind session-id text meta extra]
  {:schema "openplanner.event.v1"
   :id id
   :ts ts
   :source "pi-session-ingester"
   :kind kind
   :source_ref {:project session-project
                :session session-id}
   :text (truncate-text text)
   :meta meta
   :extra extra})

(defn- map-session-event
  [pi-event session-id cwd]
  [(make-event
    (str "pi:" session-id ":session")
    (:timestamp pi-event)
    "pi.session_start"
    session-id
    (str "Pi session started in " cwd)
    {:role "system" :author "pi"}
    {:pi_session_id (:id pi-event)
     :pi_version (:version pi-event)
     :workspace cwd
     :pi_workspace_project (cwd-to-project cwd)})])

(defn- map-model-change-event
  [pi-event session-id]
  [(make-event
    (str "pi:" session-id ":model:" (:id pi-event))
    (:timestamp pi-event)
    "pi.model_change"
    session-id
    (str "Model: " (:provider pi-event) "/" (:modelId pi-event))
    {:role "system" :author "pi"}
    {:provider (:provider pi-event)
     :model_id (:modelId pi-event)})])

(defn- map-compaction-event
  [pi-event session-id cwd]
  (let [summary (or (:summary pi-event) "")]
    [(make-event
      (str "pi:" session-id ":compaction:" (:id pi-event))
      (:timestamp pi-event)
      "pi.compaction"
      session-id
      summary
      {:role "system" :author "pi"}
      {:compaction true
       :pi_workspace_project (cwd-to-project cwd)})]))

(defn- map-custom-message-event
  [pi-event session-id cwd]
  (let [content (or (:content pi-event) "")]
    [(make-event
      (str "pi:" session-id ":custom:" (:id pi-event))
      (:timestamp pi-event)
      (str "pi.custom." (or (:customType pi-event) "unknown"))
      session-id
      (if (string? content) content (json/generate-string content))
      {:role "system" :author "pi"}
      {:custom_type (:customType pi-event)
       :pi_workspace_project (cwd-to-project cwd)})]))
(defn- map-message-event
  [pi-event session-id cwd]
  (let [msg (:message pi-event)
        content (:content msg)
        role (:role msg)
        session-id-str session-id]
    (cond
      (= "user" role)
      [(make-event
        (str "pi:" session-id-str ":msg:" (:id pi-event))
        (:timestamp pi-event)
        "pi.message"
        session-id-str
        (or (extract-text-content content) "")
        {:role "user" :author "user"}
        {:pi_message_id (:id pi-event)
         :pi_parent_id (:parentId pi-event)
         :pi_workspace_project (cwd-to-project cwd)})]

      (= "assistant" role)
      (let [text-content (extract-text-content content)
            tool-calls (extract-tool-calls content)
            thinking (extract-thinking content)
            model-name (or (:model msg) (:provider msg) "unknown")
            base-meta {:role "assistant" :author "pi" :model model-name}
            base-extra {:pi_message_id (:id pi-event)
                        :pi_parent_id (:parentId pi-event)
                        :provider (:provider msg)
                        :model (:model msg)
                        :usage (:usage msg)
                        :stop_reason (:stopReason msg)
                        :pi_workspace_project (cwd-to-project cwd)}
            text-event (when text-content
                         (make-event
                          (str "pi:" session-id-str ":msg:" (:id pi-event))
                          (:timestamp pi-event)
                          "pi.message"
                          session-id-str
                          text-content
                          base-meta
                          base-extra))
            thinking-event (when thinking
                            (make-event
                             (str "pi:" session-id-str ":thinking:" (:id pi-event))
                             (:timestamp pi-event)
                             "pi.reasoning"
                             session-id-str
                             thinking
                             {:role "system" :author "pi" :model model-name}
                             {:pi_message_id (:id pi-event)
                              :pi_workspace_project (cwd-to-project cwd)}))
            tool-events (when (seq tool-calls)
                         (map-indexed
                          (fn [idx tc]
                            (let [tool-name (or (:name tc) "unknown")
                                  args-preview (if (:arguments tc)
                                                 (let [s (if (string? (:arguments tc))
                                                           (:arguments tc)
                                                           (json/generate-string (:arguments tc)))]
                                                   (subs s 0 (min 500 (count s))))
                                                 "")]
                              (make-event
                               (str "pi:" session-id-str ":tool:" (or (:id tc) (str (:id pi-event) "_" idx)))
                               (:timestamp pi-event)
                               "pi.tool_call"
                               session-id-str
                               (str "Tool: " tool-name "\n" args-preview)
                               {:role "system" :author "pi" :model model-name}
                               {:pi_message_id (:id pi-event)
                                :tool_name tool-name
                                :tool_call_id (:id tc)
                                :tool_arguments_preview args-preview
                                :pi_workspace_project (cwd-to-project cwd)})))
                          tool-calls))]
        (vec (keep identity (concat [text-event thinking-event] tool-events))))

      :else [])))

(defn- map-pi-event
  "Map a single pi event to OpenPlanner events. Returns a vector."
  [pi-event session-id cwd]
  (let [event-type (:type pi-event)]
    (cond
      (not (supported-event-types event-type)) []
      (= "session" event-type) (map-session-event pi-event session-id cwd)
      (= "model_change" event-type) (map-model-change-event pi-event session-id)
      (= "compaction" event-type) (map-compaction-event pi-event session-id cwd)
      (= "custom_message" event-type) (map-custom-message-event pi-event session-id cwd)
      (= "message" event-type) (map-message-event pi-event session-id cwd)
      :else [])))

;; ---------------------------------------------------------------------------
;; Session File Discovery
;; ---------------------------------------------------------------------------

(defn- extract-session-id
  "Extract session ID from pi JSONL filename: <timestamp>_<uuid>.jsonl"
  [filename]
  (if-let [match (re-matches #"[\dT:-]+_(.+)\.jsonl" filename)]
    (second match)
    (str/replace filename #"\.jsonl$" "")))

(defn- discover-session-files
  "Discover JSONL session files under the root directory.
   Returns [{:id path :path ... :mtime ... :size ...}]"
  [root since-ts]
  (let [root-file (io/file root)]
    (when (.exists root-file)
      (let [dirs (filter #(.isDirectory %) (.listFiles root-file))]
        (->> dirs
             (mapcat (fn [dir]
                       (try
                         (->> (.listFiles dir)
                              (filter #(and (.isFile %)
                                            (str/ends-with? (.getName %) ".jsonl")
                                            (> (.lastModified %) since-ts)))
                              (map (fn [file]
                                     (let [name (.getName file)]
                                       {:id (.getAbsolutePath file)
                                        :path (.getAbsolutePath file)
                                        :name name
                                        :session-id (extract-session-id name)
                                        :dir (.getName dir)
                                        :mtime (.lastModified file)
                                        :size (.length file)
                                        :modified-at (Instant/ofEpochMilli (.lastModified file))}))))
                         (catch Exception _ []))))
             (sort-by :mtime))))))

;; ---------------------------------------------------------------------------
;; Driver Implementation
;; ---------------------------------------------------------------------------

(defrecord PiSessionsDriver [root-path state]
  protocol/Driver

  (discover [_ opts]
    (let [existing-state (or (:existing-state opts) {})
          since-ts (if (seq existing-state)
                     (- (apply min (map (fn [[_id row]]
                                         (if-let [mt (:mtime row)]
                                           (if (number? mt) mt
                                             (try (.getTime (java.sql.Timestamp/valueOf (str mt)))
                                                  (catch Exception _ 0)))
                                           0))
                                       existing-state))
                                        60000) ; 60s overlap
                     0)
          files (discover-session-files root-path since-ts)
          ;; Filter out already-ingested (same path + same mtime)
          new-files (filter (fn [f]
                              (let [existing (get existing-state (:id f))]
                                (or (nil? existing)
                                    (let [existing-meta (:metadata existing)]
                                      (not= (:size existing-meta) (:size f))))))
                            files)]
      {:total-files (count files)
       :new-files (count new-files)
       :changed-files 0
       :deleted-files 0
       :unchanged-files (- (count files) (count new-files))
       :files (map (fn [f]
                     (assoc f :content-hash (str (:session-id f) ":" (:mtime f))))
                   new-files)}))

  (extract [_ file-id]
    (try
      (let [file (io/file file-id)
            entries (read-jsonl-file file)
            ;; Extract session metadata from the first "session" event
            session-event (first (filter #(= "session" (:type %)) entries))
            session-id (or (:id session-event)
                           (extract-session-id (.getName file)))
            cwd (or (:cwd session-event) "")
            ;; Map all events
            op-events (vec (mapcat #(map-pi-event % session-id cwd) entries))]
        {:id file-id
         :path file-id
         :content (json/generate-string {:session-id session-id
                                          :cwd cwd
                                          :events op-events})
         :content-hash (str session-id ":" (.lastModified file))})
      (catch Exception e
        {:id file-id
         :path file-id
         :content nil
         :error (.getMessage e)})))

  (extract-batch [_ file-ids]
    (map #(protocol/extract nil %) file-ids))

  (get-state [_]
    @state)

  (set-state [_ new-state]
    (reset! state new-state))

  (close [_]))

(defn create-driver
  "Create a pi-sessions driver from config."
  [config]
  (let [root (or (:root-path config) (:root_path config) default-sessions-root)]
    (->PiSessionsDriver root (atom {}))))
