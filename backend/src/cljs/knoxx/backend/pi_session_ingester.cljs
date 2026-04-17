(ns knoxx.backend.pi-session-ingester
  (:require [clojure.string :as str]
            ["node:fs/promises" :as fs]
            ["node:path" :as path]
            ["node:crypto" :as crypto]))


(def ^:private PI-SESSIONS-ROOT
  (or (aget (.-env js/process) "PI_SESSIONS_ROOT") "/home/err/.pi/agent/sessions"))

(def ^:private INGEST-STATE-DIR
  (or (aget (.-env js/process) "INGEST_STATE_DIR") "/home/err/.knoxx/pi-ingest-state"))

(def ^:private INGEST-STATE-FILE
  (.join path INGEST-STATE-DIR "ingested-sessions.json"))

(def ^:private MAX-EVENTS-PER-BATCH 200)
(def ^:private MAX-TEXT-LENGTH 12000)
(def ^:private PI-SESSION-PROJECT
  (or (aget (.-env js/process) "PI_SESSION_PROJECT") "knoxx-session"))

(def ^:private SUPPORTED-EVENT-TYPES
  #{"session" "message" "compaction" "model_change"
    "thinking_level_change" "custom_message" "branch_summary"})


(defn- ensure-state-dir
  []
  (.mkdir fs INGEST-STATE-DIR #js {:recursive true}))

(defn- load-ingest-state
  []
  (-> (.readFile fs INGEST-STATE-FILE "utf-8")
      (.then (fn [raw] (js/JSON.parse raw)))
      (.catch (fn [_] #js {:sessions (js/Object.create nil)}))))

(defn- save-ingest-state
  [state]
  (-> (ensure-state-dir)
      (.then (fn [_] (.writeFile fs INGEST-STATE-FILE (js/JSON.stringify state nil 2) "utf-8")))))


(defn- discover-session-files
  [since-ts]
  (let [since-ts (or since-ts 0)]
    (-> (.readdir fs PI-SESSIONS-ROOT)
        (.then
         (fn [dirs]
           (js/Promise.all
            (map
             (fn [dir]
               (let [dir-path (.join path PI-SESSIONS-ROOT dir)]
                 (-> (.readdir fs dir-path)
                     (.then
                      (fn [entries]
                        (js/Promise.all
                         (map
                          (fn [entry]
                            (when (.endsWith entry ".jsonl")
                              (let [file-path (.join path dir-path entry)]
                                (-> (.stat fs file-path)
                                    (.then
                                     (fn [s]
                                       (when (> (.-mtimeMs s) since-ts)
                                         (let [match (.match entry #"^[\dT:-]+_(.+)\.jsonl$")
                                               session-id (if match
                                                            (aget match 1)
                                                            (.replace entry #"\.jsonl$" ""))]
                                           #js {:dir dir
                                                :path file-path
                                                :sessionId session-id
                                                :mtime (.-mtimeMs s)
                                                :size (.-size s)}))))
                                    (.catch (fn [_] nil))))))
                          entries))))
                     (.catch (fn [_] #js [])))))
             dirs))))
        (.then
         (fn [dir-results]
           (let [flat (->> (js/Array.from dir-results)
                           (reduce (fn [acc r] (if (array? r) (into acc (js/Array.from r)) (conj acc r))) [])
                           (filterv some?))]
             (.sort flat (fn [a b] (- (.-mtime a) (.-mtime b)))))))
        (.catch (fn [_] #js [])))))


(defn- truncate-text
  [text max-len]
  (let [max-len (or max-len MAX-TEXT-LENGTH)]
    (if (or (not text) (<= (.-length text) max-len))
      text
      (str (.slice text 0 max-len) "\n... [truncated " (- (.-length text) max-len) " chars]"))))

(defn- extract-text-from-content
  [content]
  (when (array? content)
    (->> (js/Array.from content)
         (filter (fn [c] (and (= (.-type c) "text") (.-text c))))
         (map (fn [c] (.-text c)))
         (clojure.string/join "\n"))))

(defn- extract-tool-calls
  [content]
  (when (array? content)
    (->> (js/Array.from content)
         (filter (fn [c] (= (.-type c) "toolCall"))))))

(defn- extract-thinking
  [content]
  (when (array? content)
    (->> (js/Array.from content)
         (filter (fn [c] (and (= (.-type c) "thinking") (.-text c))))
         (map (fn [c] (.-text c)))
         (clojure.string/join "\n"))))


(defn- cwd-to-project
  [cwd]
  (if (not cwd)
    "pi"
    (let [normalized (-> cwd
                         (.replace #"^/home/[^/]+/devel/" "")
                         (.replace #"^/home/[^/]+/" "")
                         (.replace #"^/" ""))]
      (if (str/blank? normalized) "pi" normalized))))


(defn- make-event
  [session-id kind ts text meta extra]
  (let [evt #js {:schema "openplanner.event.v1"
                 :id (str "pi:" session-id ":" kind ":" (.-id extra))
                 :ts ts
                 :source "pi-session-ingester"
                 :kind (str "pi." kind)
                 :source_ref #js {:project PI-SESSION-PROJECT :session session-id}
                 :text (truncate-text text nil)
                 :meta meta
                 :extra extra}]
    evt))


(defn- map-session-event
  [pi-event session-id cwd]
  (let [id (or (.-id pi-event) "unknown")]
    #js [(-> (make-event session-id "session_start" (.-timestamp pi-event)
                         (str "Pi session started in " cwd)
                         #js {:role "system" :author "pi"
                              :pi_session_version (.-version pi-event)
                              :pi_cwd cwd}
                         #js {:id id
                              :pi_session_id (.-id pi-event)
                              :pi_version (.-version pi-event)
                              :workspace cwd
                              :pi_workspace_project (cwd-to-project cwd)}))]))


(defn- map-model-change-event
  [pi-event session-id]
  #js [(make-event session-id "model_change" (.-timestamp pi-event)
                   (str "Model: " (or (.-provider pi-event) "") "/" (or (.-modelId pi-event) ""))
                   #js {:role "system" :author "pi"}
                   #js {:id (.-id pi-event)
                        :provider (.-provider pi-event)
                        :model_id (.-modelId pi-event)})])


(defn- map-compaction-event
  [pi-event session-id cwd]
  (let [summary (or (.-summary pi-event) "")]
    (if (str/blank? summary)
      #js []
      #js [(make-event session-id "compaction" (.-timestamp pi-event)
                       summary
                       #js {:role "system" :author "pi"}
                       #js {:id (.-id pi-event)
                            :compaction true
                            :pi_workspace_project (cwd-to-project cwd)})])))


(defn- map-custom-message-event
  [pi-event session-id cwd]
  (let [content (.-content pi-event)]
    (if (or (not content) (= (.-display pi-event) false))
      #js []
      #js [(make-event session-id (str "custom." (or (.-customType pi-event) "unknown"))
                       (.-timestamp pi-event)
                       (if (string? content) content (js/JSON.stringify content))
                       #js {:role "system" :author "pi"}
                       #js {:id (.-id pi-event)
                            :custom_type (.-customType pi-event)
                            :pi_workspace_project (cwd-to-project cwd)})])))


(defn- map-user-message
  [pi-event session-id msg cwd]
  (let [text (extract-text-from-content (.-content msg))]
    (if (str/blank? text)
      #js []
      #js [(make-event session-id "message" (.-timestamp pi-event)
                       text
                       #js {:role "user" :author "user"}
                       #js {:id (.-id pi-event)
                            :pi_message_id (.-id pi-event)
                            :pi_parent_id (.-parentId pi-event)
                            :pi_workspace_project (cwd-to-project cwd)})])))


(defn- map-assistant-message
  [pi-event session-id msg cwd]
  (let [text (extract-text-from-content (.-content msg))
        thinking (extract-thinking (.-content msg))
        tool-calls (extract-tool-calls (.-content msg))
        model (or (.-model msg) (.-provider msg) "unknown")
        events #js []]
    ;; Assistant text
    (when (not (str/blank? text))
      (.push events (make-event session-id "message" (.-timestamp pi-event)
                                text
                                #js {:role "assistant" :author "pi" :model model}
                                #js {:id (.-id pi-event)
                                     :pi_message_id (.-id pi-event)
                                     :pi_parent_id (.-parentId pi-event)
                                     :provider (.-provider msg)
                                     :model (.-model msg)
                                     :usage (or (.-usage msg) nil)
                                     :stop_reason (or (.-stopReason msg) nil)
                                     :pi_workspace_project (cwd-to-project cwd)})))
    ;; Thinking
    (when (not (str/blank? thinking))
      (.push events (make-event session-id "reasoning" (.-timestamp pi-event)
                                thinking
                                #js {:role "system" :author "pi" :model model}
                                #js {:id (str (.-id pi-event) "-thinking")
                                     :pi_message_id (.-id pi-event)})))
    ;; Tool calls
    (doseq [tc (js/Array.from tool-calls)]
      (let [tool-name (or (.-name tc) "unknown")
            args-preview (when (.-arguments tc)
                           (truncate-text (js/JSON.stringify (.-arguments tc)) 2000))]
        (.push events (make-event session-id "tool_call" (.-timestamp pi-event)
                                  (truncate-text (str "Tool: " tool-name "\n" (or args-preview "")) nil)
                                  #js {:role "system" :author "pi" :model model}
                                  #js {:id (or (.-id tc) (.-id pi-event))
                                       :pi_message_id (.-id pi-event)
                                       :tool_name tool-name
                                       :tool_call_id (.-id tc)
                                       :tool_arguments_preview args-preview}))))
    events))


(defn- map-pi-event-to-events
  [pi-event session-meta]
  (let [event-type (.-type pi-event)
        session-id (.-sessionId session-meta)
        cwd (.-cwd session-meta)]
    (cond
      (not (contains? SUPPORTED-EVENT-TYPES event-type))
      #js []

      (= event-type "session")
      (map-session-event pi-event session-id cwd)

      (= event-type "model_change")
      (map-model-change-event pi-event session-id)

      (= event-type "thinking_level_change")
      #js []

      (= event-type "compaction")
      (map-compaction-event pi-event session-id cwd)

      (= event-type "custom_message")
      (map-custom-message-event pi-event session-id cwd)

      (= event-type "message")
      (let [msg (or (.-message pi-event) #js {})
            role (.-role msg)]
        (cond
          (= role "user")
          (map-user-message pi-event session-id msg cwd)

          (= role "assistant")
          (map-assistant-message pi-event session-id msg cwd)

          :else #js []))

      :else #js [])))


(defn- parse-session-file
  [file-path]
  (-> (.readFile fs file-path "utf-8")
      (.then
       (fn [raw]
         (let [lines (.split raw "\n")
               events #js []
               session-meta #js {:sessionId "unknown" :cwd "/unknown"}]
           (doseq [line (js/Array.from lines)]
             (let [trimmed (.trim line)]
               (when (not (str/blank? trimmed))
                 (try
                   (let [parsed (js/JSON.parse trimmed)]
                     (when (= (.-type parsed) "session")
                       (aset session-meta "sessionId" (or (.-id parsed) "unknown"))
                       (aset session-meta "cwd" (or (.-cwd parsed) "/unknown")))
                     (.push events parsed))
                   (catch :default _ nil)))))
           #js {:events events :sessionMeta session-meta})))))


(defn- ingest-session-file
  [file-path session-file-meta openplanner-request-fn]
  (-> (parse-session-file file-path)
      (.then
       (fn [{:keys [events session-meta]}]
         ;; Map all pi events to OpenPlanner events
         (let [all-op-events (reduce
                              (fn [acc pi-event]
                                (into (js/Array.from acc)
                                      (js/Array.from (map-pi-event-to-events pi-event session-meta))))
                              #js []
                              (js/Array.from events))]
           (if (= (.-length all-op-events) 0)
             #js {:sessionId (.-sessionId session-meta) :eventsIngested 0 :batches 0}
             ;; Batch and send
             (let [promises #js []
                   events-ingested (atom 0)
                   batches (atom 0)]
               (loop [i 0]
                 (when (< i (.-length all-op-events))
                   (let [batch (.slice all-op-events i (+ i MAX-EVENTS-PER-BATCH))]
                     (.push promises
                            (-> (openplanner-request-fn "POST" "/v1/events" #js {:events batch})
                                (.then (fn [_]
                                         (swap! events-ingested + (.-length batch))
                                         (swap! batches inc)))
                                (.catch (fn [err]
                                          (.error js/console
                                                  (str "[pi-ingester] Batch failed for "
                                                       (.-sessionId session-meta) ":")
                                                  (.-message err)))))))
                   (recur (+ i MAX-EVENTS-PER-BATCH))))
               (-> (js/Promise.all promises)
                   (.then (fn [_]
                            #js {:sessionId (.-sessionId session-meta)
                                 :eventsIngested @events-ingested
                                 :batches @batches}))))))))))


(defn run-pi-session-ingest
  [{:keys [openplanner-request-fn force limit session-dirs]
    :or {force false limit 50}}]
  (when (not openplanner-request-fn)
    (throw (js/Error. "openplannerRequestFn is required")))
  (let [limit (or limit 50)]
    (-> (if force
          (js/Promise.resolve #js {:sessions (js/Object.create nil)})
          (load-ingest-state))
        (.then
         (fn [state]
           (-> (discover-session-files 0)
               (.then
                (fn [all-files]
                  (let [files (if session-dirs
                                (.filter all-files
                                         (fn [f]
                                           (some (fn [d] (.includes (.-dir f) d)) session-dirs)))
                                all-files)
                        new-files (if force
                                    files
                                    (.filter files
                                             (fn [f]
                                               (let [existing (aget (.-sessions state) (.-sessionId f))]
                                                 (or (not existing)
                                                     (< (or (.-mtime existing) 0) (.-mtime f)))))))
                        to-ingest (.slice new-files 0 limit)]
                    (if (= (.-length to-ingest) 0)
                      #js {:ok true
                           :scanned (.-length files)
                           :newSessions 0
                           :ingested 0
                           :totalEvents 0
                           :skipped (- (.-length files) (.-length to-ingest))}
                      (let [results-atom (atom #js [])]
                        (-> (reduce
                             (fn [promise-chain file]
                               (-> promise-chain
                                   (.then
                                    (fn [_]
                                      (-> (ingest-session-file (.-path file) file openplanner-request-fn)
                                          (.then
                                           (fn [result]
                                             (aset (.-sessions state) (.-sessionId file)
                                                   #js {:mtime (.-mtime file)
                                                        :eventCount (.-eventsIngested result)
                                                        :ingestedAt (.toISOString (js/Date.))
                                                        :dir (.-dir file)
                                                        :size (.-size file)})
                                             (.push @results-atom result)))
                                          (.catch
                                           (fn [err]
                                             (.error js/console
                                                     (str "[pi-ingester] Failed: " (.-sessionId file) ":")
                                                     (.-message err))
                                             (.push @results-atom
                                                    #js {:sessionId (.-sessionId file)
                                                         :error (.-message err)
                                                         :eventsIngested 0
                                                         :batches 0}))))))))
                             (js/Promise.resolve nil)
                             (js/Array.from to-ingest))
                            (.then
                             (fn [_]
                               (-> (save-ingest-state state)
                                   (.then
                                    (fn [_]
                                      (let [results @results-atom
                                            total-events (reduce
                                                          (fn [sum r] (+ sum (or (.-eventsIngested r) 0)))
                                                          0
                                                          (js/Array.from results))
                                            errors (reduce
                                                    (fn [cnt r] (if (.-error r) (inc cnt) cnt))
                                                    0
                                                    (js/Array.from results))]
                                        #js {:ok true
                                             :scanned (.-length files)
                                             :newSessions (.-length to-ingest)
                                             :ingested (- (.-length to-ingest) errors)
                                             :totalEvents total-events
                                             :errors errors
                                             :details results})))))))))))))))))

        (.catch
         (fn [err]
           #js {:ok false :error (.-message err)})))



(defn get-pi-ingest-status
  []
  (-> (js/Promise.all #js [(load-ingest-state) (discover-session-files 0)])
      (.then
       (fn [[state all-files]]
         (let [ingested-ids (js/Set. (js/Object.keys (.-sessions state)))
               pending (.filter all-files
                                (fn [f] (not (.has ingested-ids (.-sessionId f)))))
               stale (.filter all-files
                              (fn [f]
                                (let [existing (aget (.-sessions state) (.-sessionId f))]
                                  (and existing (< (or (.-mtime existing) 0) (.-mtime f))))))
               total-ingested (reduce
                               (fn [sum s] (+ sum (or (aget s "eventCount") 0)))
                               0
                               (js/Object.values (.-sessions state)))
               last-ingested (reduce
                              (fn [max-val s]
                                (let [at (aget s "ingestedAt")]
                                  (if (and at (> (.length at) (.length max-val))) at max-val)))
                              ""
                              (js/Object.values (.-sessions state)))
               recent (->> (js/Object.entries (.-sessions state))
                           (sort-by (fn [[_ s]] (aget s "ingestedAt")) #(> %1 %2))
                           (take 10)
                           (mapv (fn [[id s]]
                                   #js {:sessionId id
                                        :eventCount (aget s "eventCount")
                                        :ingestedAt (aget s "ingestedAt")
                                        :dir (aget s "dir")})))]
           #js {:ok true
                :piSessionsRoot PI-SESSIONS-ROOT
                :totalSessionFiles (.-length all-files)
                :ingestedSessions (.-size ingested-ids)
                :pendingSessions (.-length pending)
                :staleSessions (.-length stale)
                :totalIngestedEvents total-ingested
                :lastIngestedAt last-ingested
                :recentIngested (clj->js recent)})))))


(defn list-pi-sessions
  [{:keys [limit offset workspace]
    :or {limit 50 offset 0}}]
  (-> (discover-session-files 0)
      (.then
       (fn [all-files]
         (let [filtered (if workspace
                          (.filter all-files (fn [f] (.includes (.-dir f) workspace)))
                          all-files)
               sorted (.sort filtered (fn [a b] (- (.-mtime b) (.-mtime a))))
               total (.-length sorted)
               page (.slice sorted offset (+ offset limit))]
           (-> (js/Promise.all
                (map
                 (fn [f]
                   (-> (.readFile fs (.-path f) "utf-8")
                       (.then
                        (fn [raw]
                          (let [lines (.split raw "\n")
                                first-line (some (fn [l] (when (not (str/blank? (.trim l))) l)) (js/Array.from lines))]
                            (if (not first-line)
                              #js {:sessionId (.-sessionId f) :workspace (.-dir f)
                                   :lastModified (.toISOString (js/Date. (.-mtime f)))
                                   :fileSize (.-size f) :dir (.-dir f)}
                              (try
                                (let [header (js/JSON.parse (.trim first-line))
                                      msg-count (or (.length (.match raw (js/RegExp. "\"type\":\"message\"" "g"))) 0)
                                      tool-count (or (.length (.match raw (js/RegExp. "\"type\":\"toolCall\"" "g"))) 0)]
                                  #js {:sessionId (or (.-id header) (.-sessionId f))
                                       :workspace (or (.-cwd header) (.-dir f))
                                       :startTime (.-timestamp header)
                                       :lastModified (.toISOString (js/Date. (.-mtime f)))
                                       :messageCount msg-count
                                       :toolCallCount tool-count
                                       :fileSize (.-size f)
                                       :dir (.-dir f)})
                                (catch :default _
                                  #js {:sessionId (.-sessionId f) :workspace (.-dir f)
                                       :lastModified (.toISOString (js/Date. (.-mtime f)))
                                       :fileSize (.-size f) :dir (.-dir f)}))))))
                       (.catch
                        (fn [_]
                          #js {:sessionId (.-sessionId f) :workspace (.-dir f)
                               :lastModified (.toISOString (js/Date. (.-mtime f)))
                               :fileSize (.-size f) :dir (.-dir f)}))))
                 (js/Array.from page)))
               (.then
                (fn [sessions]
                  (let [valid (.filter sessions some?)]
                    #js {:ok true
                         :sessions valid
                         :total total
                         :offset offset
                         :limit limit
                         :has_more (< (+ offset (.-length valid)) total)})))))))))

