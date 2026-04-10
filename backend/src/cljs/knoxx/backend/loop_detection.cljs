(ns knoxx.backend.loop-detection
  "Loop detection for agent turns. Tracks tool call signatures and assistant message
   patterns to detect when an agent is stuck in a repetitive cycle."
  (:require [clojure.string :as str]
            [knoxx.backend.realtime :refer [broadcast-ws-session!]]
            [knoxx.backend.runtime-config :refer [now-iso]]))

;; ━━━ State ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

(defonce turn-state* (atom {}))
;; Map of session-id -> {:tool-calls {...} :message-chunks [] :started-ms N :last-progress-ms N :loop-warning-count N}

;; Configurable thresholds
(def ^:dynamic *tool-repetition-threshold* 3)
(def ^:dynamic *message-repetition-threshold* 3)
(def ^:dynamic *max-turn-duration-ms* 300000)  ;; 5 minutes
(def ^:dynamic *no-progress-timeout-ms* 30000)  ;; 30 seconds
(def ^:dynamic *similarity-threshold* 0.85)     ;; For fuzzy message matching
(def ^:dynamic *max-loop-warnings* 2)           ;; Try loop breaker twice before abort

;; ━━━ Tool Call Tracking ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

(defn tool-signature
  "Create a hashable signature for a tool call. Normalizes params to detect
   same tool with same arguments."
  [tool-name params]
  (let [param-str (if (object? params)
                    (js/JSON.stringify params)
                    (str params))]
    [tool-name (hash param-str)]))

(defn record-tool-call!
  "Record a tool call and return detection result.
   Returns {:detected true :count N :signature [...] :should-warn true} if loop detected."
  [session-id tool-name params]
  (let [sig (tool-signature tool-name params)]
    (swap! turn-state*
           (fn [state]
             (let [session-state (get state session-id {:tool-calls {}})
                   tool-calls (:tool-calls session-state)
                   call-count (inc (get tool-calls sig 0))
                   new-state (assoc-in state [session-id :tool-calls sig] call-count)]
               (if (>= call-count *tool-repetition-threshold*)
                 (assoc new-state
                        :detected true
                        :count call-count
                        :signature sig
                        :should-warn true)
                 new-state))))))

(defn get-tool-call-count
  [session-id tool-name params]
  (let [sig (tool-signature tool-name params)]
    (get-in @turn-state* [session-id :tool-calls sig] 0)))

;; ━━━ Message Pattern Detection ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

(defn normalize-message
  "Normalize a message chunk for comparison. Removes variable parts like timestamps."
  [text]
  (-> text
      (str/replace #"\b\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}[^\s]*\b" "") ;; ISO timestamps
      (str/replace #"\b\d+ms\b" "")      ;; Milliseconds
      (str/replace #"\b\d+s\b" "")       ;; Seconds
      (str/lower-case)
      (str/trim)))

(defn similarity-score
  "Calculate Jaccard similarity between two normalized messages.
   Returns value between 0 and 1."
  [text-a text-b]
  (let [words-a (set (str/split (normalize-message text-a) #"\s+"))
        words-b (set (str/split (normalize-message text-b) #"\s+"))
        intersection (count (clojure.set/intersection words-a words-b))
        union (count (clojure.set/union words-a words-b))]
    (if (zero? union)
      1.0
      (/ intersection union))))

(defn detect-message-loop
  "Check if recent message chunks show repetitive patterns.
   Returns {:detected true :pattern '...'} if loop detected."
  [session-id new-chunk]
  (swap! turn-state*
         (fn [state]
           (let [session-state (get state session-id {:message-chunks []})
                 chunks (:message-chunks session-state)
                 ;; Keep last 10 chunks for comparison
                 recent (take 10 (conj chunks new-chunk))
                 ;; Check similarity with recent chunks
                 similar-count (count (filter #(>= (similarity-score new-chunk %) *similarity-threshold*)
                                               recent))]
             (-> state
                 (assoc-in [session-id :message-chunks] (vec recent))
                 (cond->
                   (>= similar-count *message-repetition-threshold*)
                   (assoc :message-loop-detected true
                          :similar-count similar-count
                          :sample-chunk new-chunk)))))))

;; ━━━ Time-Based Detection ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

(defn start-turn!
  "Initialize turn state for a session."
  [session-id]
  (swap! turn-state*
         assoc session-id
         {:tool-calls {}
          :message-chunks []
          :started-ms (.now js/Date)
          :last-progress-ms (.now js/Date)
          :loop-warning-count 0}))

(defn record-progress!
  "Update last-progress timestamp when tokens arrive."
  [session-id]
  (swap! turn-state*
         assoc-in [session-id :last-progress-ms] (.now js/Date)))

(defn check-timeouts
  "Check for max duration or no-progress timeout.
   Returns {:timeout true :reason '...'} if detected."
  [session-id]
  (let [state (get @turn-state* session-id)
        now (.now js/Date)
        started (:started-ms state)
        last-progress (:last-progress-ms state)]
    (cond
      (and started (> (- now started) *max-turn-duration-ms*))
      {:timeout true
       :reason (str "Turn exceeded maximum duration of " *max-turn-duration-ms* "ms")}

      (and last-progress (> (- now last-progress) *no-progress-timeout-ms*))
      {:timeout true
       :reason (str "No progress for " *no-progress-timeout-ms* "ms")}

      :else nil)))

;; ━━━ Loop Breaker ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

(def loop-breaker-queries
  "Predefined queries to inject when loop detected. Tries to redirect agent."
  ["Let me summarize what we've accomplished and conclude this turn."
   "Please provide your final answer now, summarizing the key findings."
   "I need you to stop and give me the final result immediately."
   "Enough iteration. What is your final conclusion?"])

(defn get-loop-breaker-query
  "Get a loop breaker query based on warning count. Escalates in intensity."
  [warning-count]
  (get loop-breaker-queries (min warning-count (dec (count loop-breaker-queries)))))

;; ━━━ Event Broadcasting ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

(defn broadcast-loop-detected!
  "Broadcast loop_detected event via WebSocket. Includes detection details."
  [session-id conversation-id detection]
  (broadcast-ws-session! session-id "agent:loop_detected"
                         (merge {:timestamp (now-iso)
                                 :session_id session-id
                                 :conversation_id conversation-id}
                                detection)))

(defn broadcast-loop-breaker-injected!
  "Broadcast when a loop breaker query is injected."
  [session-id conversation-id query]
  (broadcast-ws-session! session-id "agent:loop_breaker"
                         {:timestamp (now-iso)
                          :session_id session-id
                          :conversation_id conversation-id
                          :query query}))

;; ━━━ Main Detection Flow ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

(defn check-and-handle-loop
  "Main entry point. Call this when tool starts or message chunk arrives.
   
   Returns:
   - {:status :ok} - no loop detected
   - {:status :warn :query '...'} - loop detected, inject this query
   - {:status :abort :reason '...'} - loop persisted after warnings, abort"
  [session-id conversation-id {:keys [tool-name params message-chunk]}]
  (let [state (get @turn-state* session-id)
        warning-count (:loop-warning-count state 0)]
    
    ;; Record progress
    (record-progress! session-id)
    
    ;; Check tool call repetition
    (when tool-name
      (let [tool-detection (record-tool-call! session-id tool-name params)]
        (when (:detected tool-detection)
          (broadcast-loop-detected! session-id conversation-id
                                    {:type :tool-repetition
                                     :tool_name tool-name
                                     :count (:count tool-detection)}))))
    
    ;; Check message pattern repetition
    (when message-chunk
      (let [msg-detection (detect-message-loop session-id message-chunk)]
        (when (:message-loop-detected msg-detection)
          (broadcast-loop-detected! session-id conversation-id
                                    {:type :message-repetition
                                     :similar_count (:similar-count msg-detection)
                                     :sample (:sample-chunk msg-detection)}))))
    
    ;; Check timeouts
    (let [timeout (check-timeouts session-id)]
      (when timeout
        (broadcast-loop-detected! session-id conversation-id
                                  {:type :timeout
                                   :reason (:reason timeout)})))
    
    ;; Determine action
    (let [has-loop (or (:detected @turn-state*)
                       (:message-loop-detected @turn-state*)
                       (:timeout @turn-state*))]
      (cond
        (and has-loop (< warning-count *max-loop-warnings*))
        ;; Try loop breaker
        (let [query (get-loop-breaker-query warning-count)]
          (swap! turn-state* update-in [session-id :loop-warning-count] inc)
          (broadcast-loop-breaker-injected! session-id conversation-id query)
          {:status :warn :query query})
        
        (and has-loop (>= warning-count *max-loop-warnings*))
        ;; Escalate to abort
        {:status :abort
         :reason (str "Loop persisted after " warning-count " intervention attempts")}
        
        :else
        {:status :ok}))))

(defn end-turn!
  "Clean up turn state when turn ends (completed, failed, or aborted)."
  [session-id]
  (swap! turn-state* dissoc session-id))

;; ━━━ Abort Flag Check ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

(defn abort-requested?
  "Check if abort was requested for this session. The abort endpoint sets this flag."
  [session-id]
  ;; This will be checked by the streaming loop
  ;; The abort endpoint in app_routes.cljs sets this in the agent session
  false)  ;; Placeholder - actual implementation checks agent-session atom
