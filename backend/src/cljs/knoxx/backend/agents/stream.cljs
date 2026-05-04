(ns knoxx.backend.agents.stream
  "Streaming event handling for agent turns."
  (:require [clojure.string :as str]
            [knoxx.backend.agents.content :as content :refer [diff-appended-text preview-text-nonblank tool-result-content-parts]]
            [knoxx.backend.agents.tools :as tools :refer [tool-call-input-preview tool-call-preview-from-part assistant-tool-call-previews]]
            [knoxx.backend.redis-client :as redis]
            [knoxx.backend.realtime :refer [broadcast-ws-session!]]
            [knoxx.backend.run-state :refer [update-run! append-run-event! update-run-tool-receipt!
                                             backfill-run-tool-input-preview! append-limited
                                             append-run-trace-text! apply-run-tool-trace-event!
                                             tool-event-payload]]
            [knoxx.backend.session-store :as session-store]
            [knoxx.backend.text :refer [assistant-message-text assistant-message-reasoning-text]]
            [knoxx.backend.turn-control :as turn-control]
            [knoxx.backend.util.time :refer [now-iso]]))

(def ^:private DEATH_SPIRAL_STREAK_LIMIT 6)
(def ^:private DEATH_SPIRAL_TOTAL_LIMIT 12)

(defn make-stream-state
  [run-id conversation-id session-id started-at started-ms node-crypto]
  {:run-id run-id
   :conversation-id conversation-id
   :session-id session-id
   :started-at started-at
   :started-ms started-ms
   :node-crypto node-crypto
   :chunks (atom [])
   :reasoning-chunks (atom [])
   :ttft-recorded? (atom false)
   :last-assistant-text* (atom "")
   :last-reasoning-text* (atom "")
   :think-tag-mode* (atom :off)
   :aborting? (atom false)
   :abort-reason* (atom nil)
   :tool-loop* (atom {:last nil :streak 0 :counts {}})
   :seen-tool-lifecycle-events* (atom #{})})

(declare emit-streaming-delta!)

(defn- emit-progress-text!
  "Emit appended delta for a *cumulative* text value.

   Some providers misuse `*_delta` to carry the full message-so-far (cumulative)
   instead of an incremental token. If we treat that as an incremental delta we
   get duplicated leading tokens. This helper diffs against our last seen text,
   emits only the appended portion, and then resets our last-text atom to the
   provided cumulative value."
  [state kind full-text]
  (let [full-text (str (or full-text ""))
        last* (if (= kind :agent_message)
                (:last-assistant-text* state)
                (:last-reasoning-text* state))
        delta (diff-appended-text @last* full-text)]
    (when (seq delta)
      (emit-streaming-delta! state kind delta))
    (reset! last* full-text)))

(defn- emit-text-delta-with-think-tags!
  "Routes text deltas that contain <think>...</think> blocks into the reasoning
   stream, leaving the assistant message stream clean.

   This is a pragmatic fix for Gemma-family models that sometimes emit thinking
   traces inline even when the provider does not supply structured reasoning
   events." 
  [state delta]
  (let [delta (str (or delta ""))
        mode* (:think-tag-mode* state)
        last-text @(:last-assistant-text* state)]
    (cond
      (str/blank? delta) nil

      ;; Only opt-in when we see an explicit <think> tag early, before we've
      ;; emitted substantive assistant text.
      (= @mode* :off)
      (let [idx (.indexOf delta "<think>")]
        (if (and (>= idx 0)
                 (str/blank? (str last-text))
                 (< idx 64))
          (do
            (reset! mode* :thinking)
            (let [before (subs delta 0 idx)
                  after (subs delta (+ idx (count "<think>")))]
              (when (seq (str/trim before))
                (emit-streaming-delta! state :agent_message before))
              (when (seq after)
                (emit-text-delta-with-think-tags! state after))))
          (emit-streaming-delta! state :agent_message delta)))

      (= @mode* :thinking)
      (let [idx (.indexOf delta "</think>")]
(if (>= idx 0)
           (let [thinking (subs delta 0 idx)
                 after (subs delta (+ idx (count "</think>")))]
             (when (seq thinking)
               (emit-streaming-delta! state :reasoning thinking))
             (reset! mode* :done)
             (when (seq after)
               (emit-streaming-delta! state :agent_message after)))
           (emit-streaming-delta! state :reasoning delta)))

      :else
      (emit-streaming-delta! state :agent_message delta))))

(defn- first-lifecycle-event?
  [state type tool-call-id]
  (if-not (and (string? tool-call-id) (seq tool-call-id))
    true
    (let [event-key (str type ":" tool-call-id)
          seen? (contains? @(:seen-tool-lifecycle-events* state) event-key)]
      (when-not seen?
        (swap! (:seen-tool-lifecycle-events* state) conj event-key))
      (not seen?))))

(defn emit-streaming-delta!
  [{:keys [run-id conversation-id session-id started-ms ttft-recorded? chunks reasoning-chunks
           last-assistant-text* last-reasoning-text*]}
   kind delta]
  (when (seq delta)
    (when (and (= kind :agent_message)
               (not @ttft-recorded?))
      (reset! ttft-recorded? true)
      (let [ttft-ms (- (.now js/Date) started-ms)
            ttft-event (tool-event-payload run-id conversation-id session-id "assistant_first_token"
                                           {:status "streaming"
                                            :ttft_ms ttft-ms})]
        (update-run! run-id #(assoc % :ttft_ms ttft-ms))
        (append-run-event! run-id ttft-event)
        (broadcast-ws-session! session-id "events" ttft-event)
        (session-store/mark-session-streaming! (redis/get-client) session-id true)))

    ;; IMPORTANT: last-* atoms are treated as *cumulative text so far*.
    ;; emit-progress-text! relies on this to diff cumulative provider payloads.
    (if (= kind :agent_message)
      (do
        (swap! chunks conj delta)
        (swap! last-assistant-text* str delta))
      (do
        (swap! reasoning-chunks conj delta)
        (swap! last-reasoning-text* str delta)))

    (append-run-trace-text! run-id kind delta (now-iso))
    (broadcast-ws-session! session-id "tokens"
                           {:run_id run-id
                            :conversation_id conversation-id
                            :session_id session-id
                            :kind (if (= kind :agent_message) "assistant_message" "reasoning")
                            :token delta})))

(defn sync-assistant-message!
  [{:keys [last-assistant-text* last-reasoning-text*] :as state}
   assistant-message]
  (when (and assistant-message
             (= (aget assistant-message "role") "assistant"))
    (let [full-text (assistant-message-text assistant-message)
          full-reasoning (assistant-message-reasoning-text assistant-message)
          tool-previews (assistant-tool-call-previews assistant-message)]
      (doseq [{:keys [tool_call_id tool_name input_preview]} tool-previews]
        (backfill-run-tool-input-preview! (:run-id state) tool_call_id tool_name input_preview))
      ;; Treat message sync as a cumulative "authoritative" snapshot: diff and reset.
      ;; This avoids duplicating the appended delta into :last-*-text* when the
      ;; terminal message arrives immediately after streaming deltas.
      (emit-progress-text! state :agent_message full-text)
      (emit-progress-text! state :reasoning full-reasoning))))

(defn request-abort!
  [{:keys [run-id conversation-id session-id aborting? abort-reason*]} session reason]
  (let [reason (str (or reason "aborted"))]
    (if @aborting?
      (js/Promise.resolve nil)
      (do
        (reset! aborting? true)
        (reset! abort-reason* reason)
        (session-store/mark-session-streaming! (redis/get-client) session-id false)
        (let [abort-event (tool-event-payload run-id conversation-id session-id "abort_requested"
                                              {:status "aborting"
                                               :reason reason})]
          (append-run-event! run-id abort-event)
          (broadcast-ws-session! session-id "events" abort-event))
        (.abort session)))))

(defn register-active-turn!
  ([state abort!] (register-active-turn! state abort! nil))
  ([{:keys [run-id conversation-id started-at] :as state} abort! agent-spec]
   (turn-control/register-active-turn!
    conversation-id
    {:run_id run-id
     :session_id (:session-id state)
     :started_at started-at
     :agent_spec agent-spec
     :abort! abort!})))

;; ─── Event handlers ─────────────────────────────────────────────────────────

(defn- handle-message-update!
  [state event]
  (let [assistant-event (aget event "assistantMessageEvent")
        assistant-event-type (aget assistant-event "type")]
    (cond
      (= assistant-event-type "text_delta")
      (let [delta (or (aget assistant-event "delta") "")
            delta (str (or delta ""))
            last-text @(:last-assistant-text* state)]
        ;; Some providers send cumulative "text so far" in `delta`.
        ;; Heuristic: if the incoming payload already includes what we've emitted,
        ;; treat it as cumulative and diff it.
        (if (and (not (str/blank? last-text))
                 (str/starts-with? delta last-text))
          (emit-progress-text! state :agent_message delta)
          (emit-text-delta-with-think-tags! state delta)))

      (contains? #{"reasoning_delta" "reasoning" "reasoning_content_delta" "thinking_delta" "thinking"} assistant-event-type)
      (let [delta (or (aget assistant-event "delta")
                      (aget assistant-event "text")
                      (aget assistant-event "reasoning")
                      (aget assistant-event "thinking")
                      "")
            delta (str (or delta ""))
            last-reasoning @(:last-reasoning-text* state)]
        (if (and (not (str/blank? last-reasoning))
                 (str/starts-with? delta last-reasoning))
          (emit-progress-text! state :reasoning delta)
          (emit-streaming-delta! state :reasoning delta)))

      (contains? #{"toolcall_delta" "tool_call_delta"} assistant-event-type)
      (sync-assistant-message! state (or (aget assistant-event "partial")
                                         (aget event "message")))

      (contains? #{"toolcall_end" "tool_call_end"} assistant-event-type)
      (do
        (when-let [preview (tool-call-preview-from-part (aget assistant-event "toolCall"))]
          (backfill-run-tool-input-preview! (:run-id state)
                                            (:tool_call_id preview)
                                            (:tool_name preview)
                                            (:input_preview preview)))
        (sync-assistant-message! state (or (aget assistant-event "partial")
                                           (aget event "message"))))

      :else (sync-assistant-message! state (aget event "message")))))

(defn- handle-message-end!
  [state event]
  (sync-assistant-message! state (aget event "message")))

(defn- handle-tool-execution-start!
  [state _session event]
  (let [tool-name (or (aget event "toolName") "tool")
        tool-call-id (or (aget event "toolCallId") (.randomUUID (:node-crypto state)))
        raw-args (or (aget event "params")
                     (aget event "toolArgs")
                     (aget event "args")
                     (aget event "arguments")
                     (aget event "input")
                     (aget event "parameters"))
        input-raw (cond
                    (nil? raw-args) nil
                    (= raw-args js/undefined) nil
                    (string? raw-args) raw-args
                    :else (try
                            (js->clj raw-args :keywordize-keys true)
                            (catch :default _ (str raw-args))))
        input-preview (or (tool-call-input-preview tool-name (aget event "params"))
                          (tool-call-input-preview tool-name (aget event "toolArgs"))
                          (tool-call-input-preview tool-name (aget event "args"))
                          (tool-call-input-preview tool-name (aget event "arguments"))
                          (tool-call-input-preview tool-name (aget event "input"))
                          (tool-call-input-preview tool-name (aget event "parameters"))
                          (tool-call-input-preview tool-name raw-args))
        signature (str tool-name "::" (or input-preview ""))
        {:keys [last streak counts]} @(:tool-loop* state)
        next-total (inc (get counts signature 0))
        next-counts (assoc counts signature next-total)
        next-streak (if (= signature last) (inc streak) 1)]
    (swap! (:tool-loop* state) assoc :last signature :streak next-streak :counts next-counts)
    (when (and (not @(:aborting? state))
               (or (>= next-streak DEATH_SPIRAL_STREAK_LIMIT)
                   (>= next-total DEATH_SPIRAL_TOTAL_LIMIT)))
      (let [reason (str "death_spiral_detected: tool '" tool-name "' repeated " next-total "x (streak " next-streak ")")
            spiral-event (tool-event-payload (:run-id state) (:conversation-id state) (:session-id state) "death_spiral_detected"
                                             {:status "failed"
                                              :tool_name tool-name
                                              :tool_call_id tool-call-id
                                              :count next-total
                                              :streak next-streak})]
        (append-run-event! (:run-id state) spiral-event)
        (broadcast-ws-session! (:session-id state) "events" spiral-event)
        ((:abort! state) reason)))
    (let [first-event? (first-lifecycle-event? state "tool_start" tool-call-id)
          tool-event (tool-event-payload (:run-id state) (:conversation-id state) (:session-id state) "tool_start"
                                         {:status "running"
                                          :tool_name tool-name
                                          :tool_call_id tool-call-id
                                          :preview input-preview})]
      (update-run-tool-receipt! (:run-id state) tool-call-id {:tool_name tool-name}
                                (fn [receipt]
                                  (cond-> (merge receipt {:tool_name tool-name
                                                          :status "running"
                                                          :started_at (or (:started_at receipt) (now-iso))})
                                    (some? input-raw) (assoc :input input-raw)
                                    input-preview (assoc :input_preview input-preview))))
      (when first-event?
        (apply-run-tool-trace-event! (:run-id state) {:type "tool_start"
                                                      :tool_name tool-name
                                                      :tool_call_id tool-call-id
                                                      :preview input-preview
                                                      :at (now-iso)})
        (append-run-event! (:run-id state) tool-event)
        (broadcast-ws-session! (:session-id state) "events" tool-event)))))

(defn- handle-tool-execution-update!
  [state event]
  (let [tool-name (or (aget event "toolName") "tool")
        tool-call-id (or (aget event "toolCallId") (str tool-name "-update"))
        preview (or (preview-text-nonblank (aget event "delta") 400)
                    (preview-text-nonblank (aget event "update") 400)
                    (preview-text-nonblank (aget event "message") 400)
                    (preview-text-nonblank (aget event "statusMessage") 400))]
    (update-run-tool-receipt! (:run-id state) tool-call-id {:tool_name tool-name}
                              (fn [receipt]
                                (cond-> (merge receipt {:tool_name tool-name
                                                        :status "running"})
                                  preview (update :updates #(append-limited % preview 8)))))
    (apply-run-tool-trace-event! (:run-id state) {:type "tool_update"
                                                  :tool_name tool-name
                                                  :tool_call_id tool-call-id
                                                  :preview preview
                                                  :at (now-iso)})
    (when preview
      (let [tool-event (tool-event-payload (:run-id state) (:conversation-id state) (:session-id state) "tool_update"
                                           {:status "running"
                                            :tool_name tool-name
                                            :tool_call_id tool-call-id
                                            :preview preview})]
        (append-run-event! (:run-id state) tool-event)
        (broadcast-ws-session! (:session-id state) "events" tool-event)))))

(defn- handle-tool-execution-end!
  [state event]
  (let [tool-name (or (aget event "toolName") "tool")
        tool-call-id (or (aget event "toolCallId") (.randomUUID (:node-crypto state)))
        is-error (boolean (aget event "isError"))
        raw-result (or (aget event "result")
                       (aget event "toolResult")
                       (aget event "output"))
        result-raw (cond
                     (nil? raw-result) nil
                     (= raw-result js/undefined) nil
                     (string? raw-result) raw-result
                     :else (try
                             (js->clj raw-result :keywordize-keys true)
                             (catch :default _ (str raw-result))))
        content-parts (tool-result-content-parts raw-result)
        result-preview (or (preview-text-nonblank (aget event "result") 20000)
                           (preview-text-nonblank (aget event "toolResult") 20000)
                           (preview-text-nonblank (aget event "output") 20000))
        first-event? (first-lifecycle-event? state "tool_end" tool-call-id)
        tool-event (tool-event-payload (:run-id state) (:conversation-id state) (:session-id state) "tool_end"
                                       {:status (if is-error "failed" "completed")
                                        :tool_name tool-name
                                        :tool_call_id tool-call-id
                                        :is_error is-error
                                        :preview result-preview})]
    (update-run-tool-receipt! (:run-id state) tool-call-id {:tool_name tool-name}
                              (fn [receipt]
                                (cond-> (merge receipt {:tool_name tool-name
                                                        :status (if is-error "failed" "completed")
                                                        :ended_at (now-iso)
                                                        :is_error is-error})
                                  (some? result-raw) (assoc :result result-raw)
                                  result-preview (assoc :result_preview result-preview)
                                  (seq content-parts) (assoc :content_parts content-parts))))
    (when first-event?
      (apply-run-tool-trace-event! (:run-id state) {:type "tool_end"
                                                    :tool_name tool-name
                                                    :tool_call_id tool-call-id
                                                    :preview result-preview
                                                    :is_error is-error
                                                    :at (now-iso)})
      (append-run-event! (:run-id state) tool-event)
      (broadcast-ws-session! (:session-id state) "events" tool-event))))

(defn- handle-turn-end!
  [state event]
  (let [tool-results (or (aget event "toolResults") #js [])
        turn-event (tool-event-payload (:run-id state) (:conversation-id state) (:session-id state) "turn_end"
                                       {:status "completed"
                                        :tool_result_count (or (.-length tool-results) 0)})]
    (append-run-event! (:run-id state) turn-event)
    (broadcast-ws-session! (:session-id state) "events" turn-event)))

(defn- handle-agent-end!
  [state _event]
  (broadcast-ws-session! (:session-id state) "events"
                         (tool-event-payload (:run-id state) (:conversation-id state) (:session-id state) "agent_end"
                                             {:status "completed"})))

(defn build-subscribe-handler
  [state session]
  (let [abort! (fn [reason] (request-abort! state session reason))
        state (assoc state :abort! abort!)]
    (fn [event]
      (let [event-type (aget event "type")]
        (cond
          (= event-type "message_update")
          (handle-message-update! state event)

          (= event-type "message_end")
          (handle-message-end! state event)

          (= event-type "tool_execution_start")
          (handle-tool-execution-start! state session event)

          (= event-type "tool_execution_update")
          (handle-tool-execution-update! state event)

          (= event-type "tool_execution_end")
          (handle-tool-execution-end! state event)

          (= event-type "turn_end")
          (handle-turn-end! state event)

          (= event-type "agent_end")
          (handle-agent-end! state event))))))
