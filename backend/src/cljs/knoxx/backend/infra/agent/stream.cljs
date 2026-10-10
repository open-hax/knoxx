(ns knoxx.backend.infra.agent.stream
  "Streaming event handling for agent turns."
  (:require [knoxx.backend.infra.run-event-payload :as run-payload]
            [clojure.string :as str]
            [knoxx.backend.domain.agent.reasoning :as reasoning]
            [knoxx.backend.domain.agent.text-delta :as text-delta]
            [knoxx.backend.domain.agent.tool-lifecycle :as tool-lifecycle]
            [knoxx.backend.domain.agent.turn-guards :as turn-guards]
            [knoxx.backend.infra.agent.stream.provider-events :as provider-events]
            [knoxx.backend.infra.agent.stream.reducer :as reducer]
            [knoxx.backend.infra.agent.stream.sinks :as sinks]
            [knoxx.backend.infra.agent.tools :refer [tool-call-preview-from-part assistant-tool-call-previews]]
            [knoxx.backend.domain.action.run-state :refer [append-limited]]
            [knoxx.backend.domain.voice.turn-control :as turn-control]
            [knoxx.backend.shape.agent :as agent-shape]
            [knoxx.backend.domain.time :refer [now-iso]]))

(defn make-stream-state
  [run-id conversation-id session-id started-at started-ms random-uuid!]
  {:run-id run-id
   :conversation-id conversation-id
   :session-id session-id
   :started-at started-at
   :started-ms started-ms
   :random-uuid! random-uuid!
   :chunks (atom [])
   :reasoning-chunks (atom [])
   :ttft-recorded? (atom false)
   :last-assistant-text* (atom "")
   :last-reasoning-text* (atom "")
   :replay-suppression* (atom {})
   :think-tag-mode* (atom :off)
   :message-chunk-start* (atom {:agent_message 0 :reasoning 0})
   :aborting? (atom false)
   :abort-reason* (atom nil)
   :tool-loop* (atom {:last nil :streak 0 :counts {}})
   :tool-call-ids* (atom tool-lifecycle/empty-tool-call-id-state)
   :seen-tool-lifecycle-events* (atom #{})
   :run-event-sink (sinks/live-run-event-sink)})

(defn- register-tool-call-start!
  "Reserve a per-occurrence unique id for a starting tool call so reused
   provider ids (call_0 every round) stop collapsing receipts and events."
  [state tool-name raw-tool-call-id]
  (let [resolved (tool-lifecycle/resolve-tool-call-start-id
                  @(:tool-call-ids* state)
                  {:tool-name tool-name :tool-call-id raw-tool-call-id})]
    (reset! (:tool-call-ids* state) (:state resolved))
    (:tool-call-id resolved)))

(defn- active-tool-call-id
  "Resolve a raw provider tool-call id to the unique id of its latest start,
   falling back to the trimmed raw id when no start was observed."
  [state tool-name raw-tool-call-id]
  (or (tool-lifecycle/active-tool-call-id @(:tool-call-ids* state)
                                          {:tool-name tool-name :tool-call-id raw-tool-call-id})
      (some-> raw-tool-call-id str str/trim not-empty)))

(declare emit-streaming-delta!)

(defn- emit-progress-text!
  "Diff an explicit cumulative snapshot against this provider message's text;
   append only its suffix or replace its corrected message portion."
  [state kind full-text]
  (let [full-text (str (or full-text ""))
        last* (if (= kind :agent_message)
                (:last-assistant-text* state)
                (:last-reasoning-text* state))
        {:keys [delta corrected?]} (reducer/reconcile-text @last* full-text)]
    (if corrected?
      (let [chunks* (if (= kind :agent_message) (:chunks state) (:reasoning-chunks state))
            start (get @(:message-chunk-start* state) kind 0)
            prefix (apply str (subvec @chunks* 0 start))]
        (swap! chunks* #(conj (subvec % 0 start) full-text))
        (sinks/replace-stream-text! state kind (count prefix) (str prefix full-text)))
      (when (seq delta)
        (emit-streaming-delta! state kind delta :incremental)))
    (reset! last* full-text)))

(defn- emit-text-delta-with-think-tags!
  "Routes text deltas that contain <think>...</think> blocks into the reasoning
   stream, leaving the assistant message stream clean."
  [state delta]
  (let [mode* (:think-tag-mode* state)
        routed (reducer/route-incremental-text {:mode @mode*
                                             :last-assistant-text @(:last-assistant-text* state)
                                             :delta delta})]
    (reset! mode* (:mode routed))
    (doseq [{:keys [kind delta]} (:emissions routed)]
      (emit-streaming-delta! state kind delta :incremental))))

(defn- first-lifecycle-event?
  [state type tool-call-id]
  (if-not (and (string? tool-call-id) (seq tool-call-id))
    true
    (let [event-key (str type ":" tool-call-id)
          seen? (contains? @(:seen-tool-lifecycle-events* state) event-key)]
      (when-not seen?
        (swap! (:seen-tool-lifecycle-events* state) conj event-key))
      (not seen?))))

(defn- suppress-replayed-prefix-delta!
  [{:keys [last-assistant-text* last-reasoning-text* replay-suppression*]} kind delta]
  (let [previous @(if (= kind :agent_message) last-assistant-text* last-reasoning-text*)
        offset (get @replay-suppression* kind)
        result (text-delta/suppress-replayed-prefix-delta previous offset delta)]
    (if-let [next-offset (:replay-offset result)]
      (swap! replay-suppression* assoc kind next-offset)
      (swap! replay-suppression* dissoc kind))
    (:delta result)))

(defn- record-first-token!
  [{:keys [run-id conversation-id session-id started-ms ttft-recorded?] :as state} kind]
  (when (and (= kind :agent_message) (not @ttft-recorded?))
    (reset! ttft-recorded? true)
    (let [ttft-ms (- (.now js/Date) started-ms)
          event (run-payload/tool-event-payload run-id conversation-id session-id "assistant_first_token"
                                               {:status "streaming" :ttft_ms ttft-ms})
          sink (sinks/sink-or-default state)]
      (sinks/update-run-state! sink run-id #(assoc % :ttft_ms ttft-ms))
      (sinks/emit-run-event! sink event)
      (sinks/update-session-record! sink session-id {:op :mark-streaming :active? true}))))

(defn- append-token!
  [{:keys [run-id conversation-id session-id chunks reasoning-chunks
           last-assistant-text* last-reasoning-text*] :as state} kind delta]
  (record-first-token! state kind)
  (swap! (if (= kind :agent_message) chunks reasoning-chunks) conj delta)
  (swap! (if (= kind :agent_message) last-assistant-text* last-reasoning-text*) str delta)
  (sinks/append-trace-text! (sinks/sink-or-default state) run-id kind delta (now-iso))
  (sinks/emit-token-event! (sinks/sink-or-default state)
                         {:run_id run-id :conversation_id conversation-id :session_id session-id
                          :kind (if (= kind :agent_message) "assistant_message" "reasoning")
                          :token delta}))

(defn emit-streaming-delta!
  "Emit literal incremental bytes, or explicitly use the legacy replay adapter.
   Native token events and already-diffed snapshots use :incremental. Providers
   with a declared replay protocol can opt into :replay."
  ([state kind delta]
   (emit-streaming-delta! state kind delta :incremental))
  ([{:keys [last-assistant-text* last-reasoning-text*] :as state}
   kind delta semantics]
  (let [delta (str (or delta ""))
        last* (if (= kind :agent_message) last-assistant-text* last-reasoning-text*)
        delta (if (= semantics :replay)
                (text-delta/diff-appended-text @last* (suppress-replayed-prefix-delta! state kind delta))
                delta)]
    (when (seq delta)
      (append-token! state kind delta)))))
(defn sync-assistant-message!
  [state assistant-message]
  (when (and assistant-message
             (= (aget assistant-message "role") "assistant"))
    (let [text (provider-events/assistant-text-snapshot assistant-message)
          think-split (reasoning/split-think-tags text)
          full-text (if (:hadThinkTags think-split) (:answer think-split) text)
          full-reasoning (let [reasoning-text (provider-events/assistant-reasoning-snapshot assistant-message)]
                           (if (some? reasoning-text) reasoning-text
                               (when (:hadThinkTags think-split) (:reasoning think-split))))
          tool-previews (assistant-tool-call-previews assistant-message)]
      (doseq [{:keys [tool_call_id tool_name input_preview]} tool-previews]
        (sinks/backfill-tool-input-preview! (sinks/sink-or-default state) (:run-id state)
                                            (active-tool-call-id state tool_name tool_call_id)
                                            tool_name input_preview))
      ;; An omitted text channel in a tool-only partial is not a correction.
      ;; Explicit empty snapshots still clear the current provider message.
      (when (some? full-text)
        (emit-progress-text! state :agent_message full-text))
      ;; Providers can omit reasoning from their terminal/partial message even
      ;; after streaming it. Absence is not an authoritative empty correction.
      (when (some? full-reasoning)
        (emit-progress-text! state :reasoning full-reasoning)))))

(defn request-abort!
  [{:keys [run-id conversation-id session-id aborting? abort-reason*] :as state} session reason]
  (let [reason (str (or reason "aborted"))]
    (if @aborting?
      (js/Promise.resolve nil)
      (do
        (reset! aborting? true)
        (reset! abort-reason* reason)
        (let [sink (sinks/sink-or-default state)]
          (sinks/update-session-record! sink session-id {:op :mark-streaming :active? false})
          (let [abort-event (run-payload/tool-event-payload run-id conversation-id session-id "abort_requested"
                                                {:status "aborting"
                                                 :reason reason})]
            (sinks/emit-run-event! sink abort-event)))
        ;; Abort at the provider via the IAgentSession protocol. The previous
        ;; (.abort session) called a method that does not exist on the session
        ;; wrapper record, so it threw inside the provider event handler and was
        ;; silently swallowed — the death-spiral guard never actually stopped a
        ;; runaway turn. Routing through abort! reaches the raw provider session.
        (agent-shape/abort! session)))))

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

(defn- handle-message-start!
  [state event]
  (when (= "assistant" (:message-role event))
    (reset! (:message-chunk-start* state) {:agent_message (count @(:chunks state))
                                         :reasoning (count @(:reasoning-chunks state))})
    (reset! (:last-assistant-text* state) "")
    (reset! (:last-reasoning-text* state) "")
    (reset! (:think-tag-mode* state) :off)
    (reset! (:replay-suppression* state) {})))

(defn- handle-message-update!
  [state event]
  (let [assistant-event-type (:assistant-event-type event)]
    (cond
      (= assistant-event-type "text_delta")
      (if (some? (:text-snapshot event))
        (emit-progress-text! state :agent_message (:text-snapshot event))
        (emit-text-delta-with-think-tags! state (str (or (:delta event) ""))))

      (contains? #{"reasoning_delta" "reasoning" "reasoning_content_delta" "thinking_delta" "thinking"} assistant-event-type)
      (if (some? (:reasoning-snapshot event))
        (emit-progress-text! state :reasoning (:reasoning-snapshot event))
        (emit-streaming-delta! state :reasoning (str (or (:delta event) "")) :incremental))

      (contains? #{"toolcall_delta" "tool_call_delta"} assistant-event-type)
      (sync-assistant-message! state (or (:partial-message event)
                                         (:message event)))

      (contains? #{"toolcall_end" "tool_call_end"} assistant-event-type)
      (do
        (when-let [preview (tool-call-preview-from-part (:tool-call event))]
          (sinks/backfill-tool-input-preview! (sinks/sink-or-default state)
                                              (:run-id state)
                                              (active-tool-call-id state (:tool_name preview) (:tool_call_id preview))
                                              (:tool_name preview)
                                              (:input_preview preview)))
        (sync-assistant-message! state (or (:partial-message event)
                                           (:message event))))

      :else (sync-assistant-message! state (:message event)))))

(defn- handle-message-end!
  [state event]
  (sync-assistant-message! state (:message event)))

(defn- handle-tool-execution-start!
  [state _session event]
  (let [tool-name (:tool-name event)
        tool-call-id (register-tool-call-start! state tool-name (:tool-call-id event))
        event (assoc event :tool-call-id tool-call-id)
        guard (turn-guards/observe-tool-call @(:tool-loop* state)
                                             {:tool-name tool-name
                                              :tool-call-id tool-call-id
                                              :input-preview (:input-preview event)
                                              :aborting? @(:aborting? state)})]
    (reset! (:tool-loop* state) (:state guard))
    (when (:abort? guard)
      (let [spiral-event (run-payload/tool-event-payload (:run-id state) (:conversation-id state) (:session-id state) "death_spiral_detected"
                                             (tool-lifecycle/run-event-extra :death-spiral
                                                                             (merge event
                                                                                    {:count (:count guard)
                                                                                     :streak (:streak guard)})))]
        (sinks/emit-run-event! (sinks/sink-or-default state) spiral-event)
        ((:abort! state) (:reason guard))))
    (let [at (now-iso)
          event (assoc event :at at)
          first-event? (first-lifecycle-event? state "tool_start" tool-call-id)
          tool-event (run-payload/tool-event-payload (:run-id state) (:conversation-id state) (:session-id state) "tool_start"
                                         (tool-lifecycle/run-event-extra :start event))]
      (sinks/update-tool-receipt! (sinks/sink-or-default state) (:run-id state) tool-call-id {:tool_name tool-name}
                                  #(tool-lifecycle/start-receipt % event))
      (when first-event?
        (sinks/apply-tool-trace-event! (sinks/sink-or-default state) (:run-id state) (tool-lifecycle/trace-event :start event))
        (sinks/emit-run-event! (sinks/sink-or-default state) tool-event)))))

(defn- handle-tool-execution-update!
  [state event]
  (let [tool-name (:tool-name event)
        tool-call-id (or (active-tool-call-id state tool-name (:tool-call-id event))
                         (str tool-name "-update"))
        preview (:preview event)
        at (now-iso)
        event (assoc event
                     :tool-call-id tool-call-id
                     :at at
                     :append-preview #(append-limited %1 %2 8))]
    (sinks/update-tool-receipt! (sinks/sink-or-default state) (:run-id state) tool-call-id {:tool_name tool-name}
                                #(tool-lifecycle/update-receipt % event))
    (sinks/apply-tool-trace-event! (sinks/sink-or-default state) (:run-id state) (tool-lifecycle/trace-event :update event))
    (when preview
      (let [tool-event (run-payload/tool-event-payload (:run-id state) (:conversation-id state) (:session-id state) "tool_update"
                                           (tool-lifecycle/run-event-extra :update event))]
        (sinks/emit-run-event! (sinks/sink-or-default state) tool-event)))))

(defn- handle-tool-execution-end!
  [state event]
  (let [tool-name (:tool-name event)
        tool-call-id (or (active-tool-call-id state tool-name (:tool-call-id event))
                         ((:random-uuid! state)))
        at (now-iso)
        event (assoc event
                     :tool-call-id tool-call-id
                     :at at)
        first-event? (first-lifecycle-event? state "tool_end" tool-call-id)
        tool-event (run-payload/tool-event-payload (:run-id state) (:conversation-id state) (:session-id state) "tool_end"
                                       (tool-lifecycle/run-event-extra :end event))]
    (sinks/update-tool-receipt! (sinks/sink-or-default state) (:run-id state) tool-call-id {:tool_name tool-name}
                                #(tool-lifecycle/end-receipt % event))
    (when first-event?
      (sinks/apply-tool-trace-event! (sinks/sink-or-default state) (:run-id state) (tool-lifecycle/trace-event :end event))
      (sinks/emit-run-event! (sinks/sink-or-default state) tool-event))))

(defn- handle-turn-end!
  [state event]
  (let [turn-event (run-payload/tool-event-payload (:run-id state) (:conversation-id state) (:session-id state) "turn_end"
                                       {:status "completed"
                                        :tool_result_count (:tool-result-count event)})]
    (sinks/emit-run-event! (sinks/sink-or-default state) turn-event)))

(defn- handle-agent-end!
  [state _event]
  (sinks/emit-run-event! (sinks/sink-or-default state)
                         (run-payload/tool-event-payload (:run-id state) (:conversation-id state) (:session-id state) "agent_end"
                                             {:status "completed"})))

(defn build-subscribe-handler
  [state session]
  (let [abort! (fn [reason] (request-abort! state session reason))
        state (assoc state :abort! abort!)]
    (fn [provider-event]
      (let [event (provider-events/normalize provider-event)
            event-type (:type event)]
        (case event-type
          "message_start" (handle-message-start! state event)
          "message_update" (handle-message-update! state event)
          "message_end" (handle-message-end! state event)
          "tool_execution_start" (handle-tool-execution-start! state session event)
          "tool_execution_update" (handle-tool-execution-update! state event)
          "tool_execution_end" (handle-tool-execution-end! state event)
          "turn_end" (handle-turn-end! state event)
          "agent_end" (handle-agent-end! state event)
          nil)))))
