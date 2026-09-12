(ns knoxx.backend.extern.mailbox-changes
  "Credential-bound SSE invalidations for actor mailbox views."
  (:require [knoxx.backend.domain.mailbox-changes :as visibility]
            [knoxx.backend.extern.fastify :as fastify]
            [knoxx.backend.infra.actor-mailbox :as mailbox]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.mailbox-changes :as changes]))

(defn- close! [{:keys [closed? unsubscribe* interval* pending* raw]}]
  (when (compare-and-set! closed? false true)
    (when-let [unsubscribe @unsubscribe*] (unsubscribe))
    (when-let [interval @interval*] (js/clearInterval interval))
    (reset! pending* [])
    (.end raw)))

(defn- ^:async current-scope! [{:keys [runtime context scope]}]
  (let [current (await (authz/current-context! runtime context))]
    (authz/ensure-permission! current "agent.chat.use")
    (let [next-scope (:scope (mailbox/context runtime current))]
      (when-not (= (select-keys scope [:org-id :actor-id])
                   (select-keys next-scope [:org-id :actor-id]))
        (throw (ex-info "Mailbox stream authority changed" {:status 403})))
      next-scope)))

(defn- ^:async drain! [{:keys [busy? closed? pending* raw] :as state}]
  (when (compare-and-set! busy? false true)
    (try
      (loop []
        (when (and (not @closed?) (seq @pending*))
          (let [batch @pending*]
            (reset! pending* [])
            (let [current (await (current-scope! state))]
              (when (and (not @closed?) (some #(visibility/visible? current %) batch))
                (when-not (.write raw "event: mailbox-changed\ndata: {}\n\n") (close! state))))
            (recur))))
      (catch :default error
        (when-not (contains? #{401 403} (fastify/error-status error))
          (fastify/log-unclassified-failure! "mailbox-changes" error))
        (close! state))
      (finally (reset! busy? false)))))

(defn- enqueue! [{:keys [scope closed? pending*] :as state} change]
  (when (and (not @closed?) (visibility/visible? scope change))
    (swap! pending* #(vec (take-last 64 (conj % change))))
    (drain! state)))

(defn- initial-change [scope]
  {:org-id (:org-id scope) :actor-ids (if-let [actor (:actor-id scope)] [actor] [])})

(defn open-stream!
  "Open after authentication and refresh authority before every emitted frame.
  Return undefined so Fastify does not send another reply after hijacking."
  [runtime context reply]
  (authz/ensure-permission! context "agent.chat.use")
  (let [scope (:scope (mailbox/context runtime context)) raw (.-raw reply)
        state {:runtime runtime :context context :scope scope :raw raw :closed? (atom false)
               :busy? (atom false) :pending* (atom []) :unsubscribe* (atom nil) :interval* (atom nil)}]
    (.hijack reply)
    (.writeHead raw 200 #js {"Content-Type" "text/event-stream" "Cache-Control" "no-cache, no-transform"
                            "Connection" "keep-alive" "X-Accel-Buffering" "no"})
    (.flushHeaders raw)
    (.once raw "close" #(close! state))
    (.once raw "error" #(close! state))
    (reset! (:unsubscribe* state) (changes/subscribe! #(enqueue! state %)))
    ;; Reconnect/heartbeat reconciles other-process appends; process-local writes invalidate immediately.
    (reset! (:interval* state) (js/setInterval #(enqueue! state (initial-change scope)) 15000))
    (enqueue! state (initial-change scope))
    js/undefined))
