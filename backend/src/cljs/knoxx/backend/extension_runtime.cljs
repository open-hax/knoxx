(ns knoxx.backend.extension-runtime
  "Lightweight extension runtime for Knoxx.

  Bridges the gap between Knoxx's native agent runtime and the eta-mu
  extension event model.  Extensions register event handlers; the
  runtime dispatches events at lifecycle points.

  Supported events:
    session_start       – new conversation/session created
    session_switch      – conversation switched to a different session
    turn_start          – user message received, turn about to begin
    before_agent_start  – right before the agent prompt is sent
    context             – message list about to be sent to the model
    turn_end            – agent turn completed (success or failure)
    session_shutdown    – session ended / cleaned up

  Commands:
    Extensions can register slash-command handlers.  The chat router
    checks commands before enqueueing a normal turn.

  Prompt injection:
    Extensions returning {:systemPrompt ...} from before_agent_start
    cause the system message to be updated.

  Context pruning:
    Extensions returning {:messages [...]} from context cause the
    message list to be replaced before sending to the model."
  (:require [clojure.string :as str]))

;; ---------------------------------------------------------------------------
;; Registry atoms

(defonce extensions* (atom []))
(defonce command-handlers* (atom {}))

;; ---------------------------------------------------------------------------
;; Loading

(defn- load-built-in-extensions
  "Return a vector of built-in extension maps.
   Each map has :name, :events, :commands, :tools keys."
  []
  ;; Built-ins are hard-referenced here to avoid dynamic require issues
  ;; in ClojureScript.  Add new built-ins as they are created.
  [;; session-mycology is loaded as a native tool suite, not a full
   ;; extension, because its event hooks require eta-mu runtime ctx.
   ;; When the full adapter is written, it goes here.
   ])

(defn init!
  "Initialise the extension runtime.  Idempotent."
  []
  (when (empty? @extensions*)
    (reset! extensions* (load-built-in-extensions))
    (js/console.log "[extension-runtime] initialised with" (count @extensions*) "built-in extension(s)")))

;; ---------------------------------------------------------------------------
;; Event dispatch

(defn- event-handlers
  [event-name]
  (->> @extensions*
       (mapcat (fn [ext]
                 (->> (:events ext)
                      (filter #(= event-name (:event %))))))
       (keep :handler)))

(defn dispatch-event
  "Dispatch an event to all registered handlers.

   event-name – keyword or string
   event      – map/JS object passed to handlers
   ctx        – extension context map

   Returns a Promise that resolves to a merged result map.
   Handlers may return promises; results are deep-merged left-to-right.

   Keys that extensions commonly produce:
     :systemPrompt  – string to inject into the system prompt
     :message       – map with :customType, :content, :display to inject
     :messages      – vector of messages to replace the outgoing context"
  [event-name event ctx]
  (let [handlers (event-handlers event-name)]
    (if (empty? handlers)
      (js/Promise.resolve {})
      (-> (reduce (fn [promise handler]
                    (.then promise
                           (fn [acc]
                             (let [result (handler event ctx)]
                               (if (instance? js/Promise result)
                                 (.then result (fn [r] (merge acc (or r {}))))
                                 (js/Promise.resolve (merge acc (or result {}))))))))
                  (js/Promise.resolve {})
                  handlers)
          (.catch (fn [err]
                    (.warn js/console "[extension-runtime] event" event-name "handler failed:" err)
                    {}))))))

;; ---------------------------------------------------------------------------
;; Command registry

(defn register-command!
  "Register a slash-command handler.

   cmd-name   – string without leading slash, e.g. 'mycology'
   handler    – fn [args ctx] -> {:ok true :reply ...} or nil"
  [cmd-name handler]
  (swap! command-handlers* assoc (str/lower-case (str cmd-name)) handler)
  (js/console.log "[extension-runtime] registered command /" cmd-name))

(defn unregister-command!
  [cmd-name]
  (swap! command-handlers* dissoc (str/lower-case (str cmd-name))))

(defn command-names
  []
  (sort (keys @command-handlers*)))

(defn handle-command
  "Try to handle a slash command.  Returns a Promise of
   {:handled true :result ...} or {:handled false}.

   text – full user text, e.g. '/mycology on'
   ctx  – extension context map"
  [text ctx]
  (let [trimmed (str/trim (str text))]
    (if-not (str/starts-with? trimmed "/")
      (js/Promise.resolve #js {:handled false})
      (let [without-slash (subs trimmed 1)
            tokens (str/split without-slash #"\s+")
            cmd (str/lower-case (first tokens))
            args (str/join " " (rest tokens))
            handler (get @command-handlers* cmd)]
        (if handler
          (let [result (handler args ctx)]
            (if (instance? js/Promise result)
              (.then result (fn [r] #js {:handled true :result r}))
              (js/Promise.resolve #js {:handled true :result result})))
          (js/Promise.resolve #js {:handled false}))))))

;; ---------------------------------------------------------------------------
;; Context helpers

(defn build-extension-ctx
  "Build the standard extension context map from Knoxx runtime data.

   Keys mirror eta-mu ctx where possible:
     :cwd            – workspace root
     :model          – {:provider ... :id ...}
     :sessionManager – SDK session manager (optional)
     :hasUI          – false in backend-only mode
     :ui             – nil
     :conversationId – Knoxx conversation id
     :sessionId      – Knoxx session id
     :runId          – current run id
     :authContext    – authz context
     :config         – Knoxx config map"
  [_runtime config & {:keys [conversation-id session-id run-id model-id auth-context]}]
  #js {:cwd (or (:workspace-root config) (.. js/process cwd))
       :model (when model-id
                #js {:provider "proxx" :id model-id})
       :sessionManager nil
       :hasUI false
       :ui nil
       :conversationId conversation-id
       :sessionId session-id
       :runId run-id
       :authContext auth-context
       :config config})

;; ---------------------------------------------------------------------------
;; Prompt injection helpers

(defn apply-before-agent-start-results
  "Apply the merged result of before_agent_start events.

   session-manager – pi SDK SessionManager instance (optional)
   messages        – current message vector (will be modified if :message present)
   system-prompt   – current system prompt string
   result          – merged event result map

   Returns [new-messages new-system-prompt]."
  [session-manager messages system-prompt result]
  (let [messages* (vec messages)
        prompt* (or (:systemPrompt result) system-prompt)
        injected-msg (:message result)]
    [(if injected-msg
       (conj messages* injected-msg)
       messages*)
     prompt*]))

(defn apply-context-results
  "Apply the merged result of context events.

   messages – current message vector
   result   – merged event result map

   Returns new message vector."
  [messages result]
  (if-let [new-messages (:messages result)]
    (vec new-messages)
    (vec messages)))
