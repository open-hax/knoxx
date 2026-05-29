(ns knoxx.backend.infra.agent.stream.sinks
  "Run event sinks for agent stream side effects.

   Policy:
   - Reducer/semantic errors remain fatal to the caller.
   - Best-effort sink effects that broadcast or persist observability events
     should not change stream semantics when they fail asynchronously.
   - Synchronous sink exceptions are not swallowed here; callers/tests should see
     them so wiring bugs fail fast."
  (:require [knoxx.backend.domain.action.run-state :as run-state]
            [knoxx.backend.domain.realtime :as realtime]
            [knoxx.backend.infra.redis-client :as redis]
            [knoxx.backend.infra.stores.session-store :as session-store]))

(defprotocol IRunEventSink
  (emit-run-event! [sink run-event])
  (emit-token-event! [sink token-event])
  (update-run-state! [sink run-id update-fn])
  (update-session-record! [sink session-id update])
  (finalize-run! [sink result])
  (record-run-error! [sink error-event])
  (append-trace-text! [sink run-id kind delta at])
  (update-tool-receipt! [sink run-id receipt-id default-receipt update-fn])
  (apply-tool-trace-event! [sink run-id trace-event])
  (backfill-tool-input-preview! [sink run-id receipt-id tool-name input-preview]))

(defrecord LiveRunEventSink []
  IRunEventSink
  (emit-run-event! [_ run-event]
    (run-state/append-run-event! (:run_id run-event) run-event)
    (realtime/broadcast-ws-session! (:session_id run-event) "events" run-event))

  (emit-token-event! [_ token-event]
    (realtime/broadcast-ws-session! (:session_id token-event) "tokens" token-event))

  (update-run-state! [_ run-id update-fn]
    (run-state/update-run! run-id update-fn))

  (update-session-record! [_ session-id update]
    (case (:op update)
      :mark-streaming (session-store/mark-session-streaming! (redis/get-client) session-id (:active? update))
      nil))

  (finalize-run! [_ result]
    result)

  (record-run-error! [this error-event]
    (emit-run-event! this error-event))

  (append-trace-text! [_ run-id kind delta at]
    (run-state/append-run-trace-text! run-id kind delta at))

  (update-tool-receipt! [_ run-id receipt-id default-receipt update-fn]
    (run-state/update-run-tool-receipt! run-id receipt-id default-receipt update-fn))

  (apply-tool-trace-event! [_ run-id trace-event]
    (run-state/apply-run-tool-trace-event! run-id trace-event))

  (backfill-tool-input-preview! [_ run-id receipt-id tool-name input-preview]
    (run-state/backfill-run-tool-input-preview! run-id receipt-id tool-name input-preview)))

(defn live-run-event-sink
  []
  (->LiveRunEventSink))

(defn sink-or-default
  [state]
  (or (:run-event-sink state)
      (live-run-event-sink)))
