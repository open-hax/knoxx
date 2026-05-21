(ns knoxx.backend.infra.agent.stream
  "Streaming event handling for agent turns."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.agent.reasoning :as reasoning]
            [knoxx.backend.domain.agent.text-delta :as text-delta]
            [knoxx.backend.domain.agent.tool-lifecycle :as tool-lifecycle]
            [knoxx.backend.domain.agent.turn-guards :as turn-guards]
            [knoxx.backend.infra.agent.stream.provider-events :as provider-events]
            [knoxx.backend.infra.agent.tools :refer [tool-call-preview-from-part assistant-tool-call-previews]]
            [knoxx.backend.infra.redis-client :as redis]
            [knoxx.backend.domain.realtime :refer [broadcast-ws-session!]]
            [knoxx.backend.domain.action.run-state :refer [update-run! append-run-event! update-run-tool-receipt!
                                             backfill-run-tool-input-preview! append-limited
                                             append-run-trace-text! apply-run-tool-trace-event!
                                             tool-event-payload]]
            [knoxx.backend.infra.stores.session-store :as session-store]
            [knoxx.backend.domain.text :refer [assistant-message-text assistant-message-reasoning-text]]
            [knoxx.backend.domain.voice.turn-control :as turn-control]
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
        delta (text-delta/diff-appended-text @last* full-text)]
    (when (seq delta)
      (emit-streaming-delta! state kind delta))
    (reset! last* full-text)))

(defn- emit-text-delta-with-think-tags!
  "Routes text deltas that contain <think>...</think> blocks into the reasoning
   stream, leaving the assistant message stream clean."
  [state delta]
  (let [mode* (:think-tag-mode* state)
        routed (reasoning/route-think-delta {:mode @mode*
                                             :last-assistant-text @(:last-assistant-text* state)
                                             :delta delta})]
    (reset! mode* (:mode routed))
    (doseq [{:keys [kind delta]} (:emissions routed)]
      (emit-streaming-delta! state kind delta))))

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

(defn emit-streaming-delta!
  [{:keys [run-id conversation-id session-id started-ms ttft-recorded? chunks reasoning-chunks
           last-assistant-text* last-reasoning-text*] :as state}
   kind delta]
  (let [delta (str (or delta ""))
        delta (suppress-replayed-prefix-delta! state kind delta)
        last* (if (= kind :agent_message) last-assistant-text* last-reasoning-text*)
        delta (text-delta/diff-appended-text @last* delta)]
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
                              :token delta}))))
(defn sync-assistant-message!
  [state assistant-message]
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
  (let [assistant-event-type (:assistant-event-type event)]
    (cond
      (= assistant-event-type "text_delta")
      (let [delta (str (or (:delta event) ""))
            last-text @(:last-assistant-text* state)]
        ;; Some providers send cumulative "text so far" in `delta`.
        ;; Heuristic: if the incoming payload already includes what we've emitted,
        ;; treat it as cumulative and diff it.
        (if (and (not (str/blank? last-text))
                 (str/starts-with? delta last-text))
          (emit-progress-text! state :agent_message delta)
          (emit-text-delta-with-think-tags! state delta)))

      (contains? #{"reasoning_delta" "reasoning" "reasoning_content_delta" "thinking_delta" "thinking"} assistant-event-type)
      (let [delta (str (or (:delta event) ""))
            last-reasoning @(:last-reasoning-text* state)]
        (if (and (not (str/blank? last-reasoning))
                 (str/starts-with? delta last-reasoning))
          (emit-progress-text! state :reasoning delta)
          (emit-streaming-delta! state :reasoning delta)))

      (contains? #{"toolcall_delta" "tool_call_delta"} assistant-event-type)
      (sync-assistant-message! state (or (:partial-message event)
                                         (:message event)))

      (contains? #{"toolcall_end" "tool_call_end"} assistant-event-type)
      (do
        (when-let [preview (tool-call-preview-from-part (:tool-call event))]
          (backfill-run-tool-input-preview! (:run-id state)
                                            (:tool_call_id preview)
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
        tool-call-id (or (:tool-call-id event) ((:random-uuid! state)))
        event (assoc event :tool-call-id tool-call-id)
        guard (turn-guards/observe-tool-call @(:tool-loop* state)
                                             {:tool-name tool-name
                                              :tool-call-id tool-call-id
                                              :input-preview (:input-preview event)
                                              :aborting? @(:aborting? state)})]
    (reset! (:tool-loop* state) (:state guard))
    (when (:abort? guard)
      (let [spiral-event (tool-event-payload (:run-id state) (:conversation-id state) (:session-id state) "death_spiral_detected"
                                             (tool-lifecycle/run-event-extra :death-spiral
                                                                             (merge event
                                                                                    {:count (:count guard)
                                                                                     :streak (:streak guard)})))]
        (append-run-event! (:run-id state) spiral-event)
        (broadcast-ws-session! (:session-id state) "events" spiral-event)
        ((:abort! state) (:reason guard))))
    (let [at (now-iso)
          event (assoc event :at at)
          first-event? (first-lifecycle-event? state "tool_start" tool-call-id)
          tool-event (tool-event-payload (:run-id state) (:conversation-id state) (:session-id state) "tool_start"
                                         (tool-lifecycle/run-event-extra :start event))]
      (update-run-tool-receipt! (:run-id state) tool-call-id {:tool_name tool-name}
                                #(tool-lifecycle/start-receipt % event))
      (when first-event?
        (apply-run-tool-trace-event! (:run-id state) (tool-lifecycle/trace-event :start event))
        (append-run-event! (:run-id state) tool-event)
        (broadcast-ws-session! (:session-id state) "events" tool-event)))))

(defn- handle-tool-execution-update!
  [state event]
  (let [tool-name (:tool-name event)
        tool-call-id (or (:tool-call-id event) (str tool-name "-update"))
        preview (:preview event)
        at (now-iso)
        event (assoc event
                     :tool-call-id tool-call-id
                     :at at
                     :append-preview #(append-limited %1 %2 8))]
    (update-run-tool-receipt! (:run-id state) tool-call-id {:tool_name tool-name}
                              #(tool-lifecycle/update-receipt % event))
    (apply-run-tool-trace-event! (:run-id state) (tool-lifecycle/trace-event :update event))
    (when preview
      (let [tool-event (tool-event-payload (:run-id state) (:conversation-id state) (:session-id state) "tool_update"
                                           (tool-lifecycle/run-event-extra :update event))]
        (append-run-event! (:run-id state) tool-event)
        (broadcast-ws-session! (:session-id state) "events" tool-event)))))

(defn- handle-tool-execution-end!
  [state event]
  (let [tool-name (:tool-name event)
        tool-call-id (or (:tool-call-id event) ((:random-uuid! state)))
        at (now-iso)
        event (assoc event
                     :tool-call-id tool-call-id
                     :at at)
        first-event? (first-lifecycle-event? state "tool_end" tool-call-id)
        tool-event (tool-event-payload (:run-id state) (:conversation-id state) (:session-id state) "tool_end"
                                       (tool-lifecycle/run-event-extra :end event))]
    (update-run-tool-receipt! (:run-id state) tool-call-id {:tool_name tool-name}
                              #(tool-lifecycle/end-receipt % event))
    (when first-event?
      (apply-run-tool-trace-event! (:run-id state) (tool-lifecycle/trace-event :end event))
      (append-run-event! (:run-id state) tool-event)
      (broadcast-ws-session! (:session-id state) "events" tool-event))))

(defn- handle-turn-end!
  [state event]
  (let [turn-event (tool-event-payload (:run-id state) (:conversation-id state) (:session-id state) "turn_end"
                                       {:status "completed"
                                        :tool_result_count (:tool-result-count event)})]
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
    (fn [provider-event]
      (let [event (provider-events/normalize provider-event)
            event-type (:type event)]
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
