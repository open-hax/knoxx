(ns knoxx.backend.domain.agent.runtime
  (:require [clojure.string :as str]
            [knoxx.backend.domain.actor.mailbox :as actor-mailbox]
            [knoxx.backend.domain.agent.agent-context :as agent-context]
            [knoxx.backend.domain.agent.agent-hydration :refer [create-agent-custom-tools]]
            [knoxx.backend.infra.http :as http]
            [knoxx.backend.infra.http :refer [no-content? request-query-string request-forward-headers request-forward-body]]
            [knoxx.backend.infra.redis-client :as redis]
            [knoxx.backend.domain.realtime :refer [broadcast-ws-session!]]
            [knoxx.backend.domain.action.run-state :refer [tool-event-payload append-run-event!]]
            [knoxx.backend.runtime.models :refer [normalize-thinking-level effective-thinking-level models-config allowlisted-model-id? resolve-model-contract]]
            [knoxx.backend.domain.extension-runtime :as ext-runtime]
            [knoxx.backend.domain.sessions.session-store :as session-store]
            [knoxx.backend.infra.tooling :refer [allowed-tool-id-set create-runtime-tools]]
            ["@open-hax/eta-mu-cli" :as eta-mu]
            ["node:fs/promises" :as fs]
            ["node:path" :as path]))

;; Initialize extension runtime at load time
(ext-runtime/init!)

(defonce eta-mu-runtime* (atom nil))
(defonce eta-mu-module-promise* (atom nil))





;; Start the sweep loop once at load time.
;; We could think about this as a woorkflow that is triggered once on a system startup event
(start-agent-session-sweep!)

(defn- js-array-seq
  [value]
  (if (array? value) (array-seq value) []))











(defn- context-policy
  [agent-spec]
  (or (:context-policy agent-spec)
      (:contextPolicy agent-spec)
      (:context agent-spec)
      (get-in agent-spec [:extras :context])
      (get-in agent-spec [:extras :context-policy])))





(defn prune-session-messages
  "Apply an agent contract context policy to stored transcript messages.

   Supported contract shape:
   :context {:max-messages 40
             :max-chars 80000
             :preserve-system true}

   This is intentionally a deterministic sliding-window prune. Summary-based
   compression can be layered later, but this prevents unbounded sticky sessions."
  [agent-spec messages]
  (let [items (vec (or messages []))
        policy (context-policy agent-spec)]
    (if-not policy
      items
      (let [max-messages (positive-int-value (or (:max-messages policy)
                                                 (:maxMessages policy)
                                                 (:max_messages policy)))
            max-chars (positive-int-value (or (:max-chars policy)
                                              (:maxChars policy)
                                              (:max_chars policy)))
            preserve-system? (not= false (or (:preserve-system policy)
                                             (:preserveSystem policy)
                                             (:preserve_system policy)))
            system-messages (if preserve-system?
                              (filterv #(= "system" (some-> (:role %) str str/lower-case)) items)
                              [])
            body-messages (if preserve-system?
                            (remove #(= "system" (some-> (:role %) str str/lower-case)) items)
                            items)
            by-count (if max-messages
                       (take-last max-messages (vec body-messages))
                       (vec body-messages))
            by-chars (if max-chars
                       (loop [remaining (reverse by-count)
                              total 0
                              kept '()]
                         (if-let [message (first remaining)]
                           (let [size (message-text-size message)]
                             (if (and (seq kept) (> (+ total size) max-chars))
                               (vec kept)
                               (recur (rest remaining) (+ total size) (conj kept message))))
                           (vec kept)))
                       (vec by-count))]
        (vec (concat system-messages by-chars))))))







(defn- effective-tool-auth-context
  [auth-context allowed-tool-ids]
  (if-not auth-context
    nil
    (assoc auth-context
           :toolPolicies (mapv (fn [tool-id]
                                 {:toolId tool-id :effect "allow"})
                               (sort (vec allowed-tool-ids))))))

(defn- restore-agent-context!
  [previous]
  (if previous
    (agent-context/set-context! previous)
    (agent-context/clear-context!)))

(defn- wrap-tool-execute-with-agent-context!
  [tool context]
  (let [execute (and tool (aget tool "execute"))]
    (when (fn? execute)
      (aset tool "execute"
            (fn [& args]
              (let [previous (agent-context/get-context)]
                (agent-context/set-context! context)
                (try
                  (let [result (apply execute args)]
                    (if (and result (fn? (aget result "finally")))
                      (.finally result (fn [] (restore-agent-context! previous)))
                      (do
                        (restore-agent-context! previous)
                        result)))
                  (catch :default err
                    (restore-agent-context! previous)
                    (throw err))))))))
  tool)

(defn- wrap-custom-tools-with-agent-context!
  [custom-tools context]
  (when custom-tools
    (doseq [tool (if (array? custom-tools) (array-seq custom-tools) [])]
      (wrap-tool-execute-with-agent-context! tool context)))
  custom-tools)

(defn- tool-runtime-name
  "Return the eta-mu runtime name for a built-in/custom tool entry.
   Built-ins are strings after eta-mu 0.70; custom tools are JS objects and may
   have sanitized names such as discord_send with originalName=discord.send."
  [tool]
  (cond
    (string? tool) (some-> tool str str/trim not-empty)
    :else (or (some-> tool (aget "name") str str/trim not-empty)
              (some-> tool (aget "id") str str/trim not-empty)
              (some-> tool (aget "label") str str/trim not-empty))))

(defn- enabled-tool-name-allowlist
  [builtin-tools custom-tools]
  (->> (concat (or builtin-tools [])
               (if (array? custom-tools) (array-seq custom-tools) []))
       (keep tool-runtime-name)
       distinct
       vec))

(defn- path-resolve
  [^js node-path & parts]
  (case (count parts)
    0 (.resolve node-path)
    1 (.resolve node-path (nth parts 0))
    2 (.resolve node-path (nth parts 0) (nth parts 1))
    3 (.resolve node-path (nth parts 0) (nth parts 1) (nth parts 2))
    4 (.resolve node-path (nth parts 0) (nth parts 1) (nth parts 2) (nth parts 3))
    5 (.resolve node-path (nth parts 0) (nth parts 1) (nth parts 2) (nth parts 3) (nth parts 4))
    6 (.resolve node-path (nth parts 0) (nth parts 1) (nth parts 2) (nth parts 3) (nth parts 4) (nth parts 5))
    7 (.resolve node-path (nth parts 0) (nth parts 1) (nth parts 2) (nth parts 3) (nth parts 4) (nth parts 5) (nth parts 6))
    (.resolve node-path (nth parts 0) (nth parts 1) (nth parts 2) (nth parts 3) (nth parts 4) (nth parts 5) (nth parts 6) (nth parts 7))))

(defn- path-relative
  [^js node-path from to]
  (.relative node-path from to))

(defn- path-is-absolute?
  [^js node-path value]
  (.isAbsolute node-path value))

(defn- configured-extra-root-records
  [node-path config]
  (let [music-root (some-> (:music-library-root config) str str/trim not-empty)
        extra-roots (->> (or (:extra-workspace-roots config) [])
                         (map (fn [raw]
                                (some-> raw str str/trim not-empty)))
                         (remove nil?))]
    (->> (concat
          (when music-root
            [{:alias "Music"
              :root (path-resolve node-path music-root)}])
          (map (fn [raw-root]
                 {:alias nil
                  :root (path-resolve node-path raw-root)})
               extra-roots))
         (reduce (fn [acc entry]
                   (if (some #(= (:root %) (:root entry)) acc)
                     acc
                     (conj acc entry)))
                 [])
         vec)))



(defn- allowed-root-records
  [node-path config]
  (vec (cons {:alias nil
              :root (path-resolve node-path (:workspace-root config))}
             (configured-extra-root-records node-path config))))

(defn- root-relative-path
  [node-path root candidate]
  (let [rel (path-relative node-path root candidate)]
    (when-not (or (str/starts-with? rel "..")
                  (path-is-absolute? node-path rel))
      rel)))

(defn resolve-workspace-path
  [_runtime config raw-path]
  (let [node-path path
        requested (some-> raw-path str str/trim not-empty)
        roots (allowed-root-records node-path config)
        music-root (some #(when (= "Music" (:alias %)) %) roots)
        candidate (cond
                    (path-is-absolute? node-path (or requested ""))
                    (path-resolve node-path requested)

                    (and requested
                         music-root
                         (or (= requested "Music")
                             (str/starts-with? requested "Music/")))
                    (let [suffix (subs requested (min (count requested) (count "Music/")))]
                      (path-resolve node-path (:root music-root) suffix))

                    :else
                    (path-resolve node-path (:workspace-root config) (or requested "")))
        matched-root (some (fn [root-record]
                             (when (root-relative-path node-path (:root root-record) candidate)
                               root-record))
                           roots)]
    (when-not matched-root
      (throw (js/Error. "Path escapes allowed workspace roots")))
    candidate))








(defn- register-actor-live-route!
  [runtime conversation-id session-id agent-spec]
  (when-let [actor-id (some-> (:actor-id agent-spec) str str/trim not-empty)]
    (-> (actor-mailbox/register-live-session!
         runtime
         {:actor-id actor-id
          :conversation-id conversation-id
          :session-id session-id
          :contract-id (some-> (:contract-id agent-spec) str str/trim not-empty)
          :source {:registeredBy "agent-runtime"
                   :contractId (:contract-id agent-spec)}})
        (.catch (fn [err]
                  (.warn js/console "[actor-mailbox] failed to register live actor route" (.-message err)))))))







(defn queue-agent-control!
  [_runtime _config {:keys [conversation-id session-id run-id message kind metadata]}]
  (cond
    (str/blank? conversation-id)
    (js/Promise.reject (js/Error. "conversation_id is required for live controls"))

    (str/blank? message)
    (js/Promise.reject (js/Error. "message is required for live controls"))

    :else
    (if-let [session (active-agent-session conversation-id)]
      (if-not (true? (aget session "isStreaming"))
        (js/Promise.reject (js/Error. "No active running turn is available for live controls"))
        (let [preview (if (> (count message) 240)
                        (str (subs message 0 240) "…")
                        message)
              event-type (if (= kind "follow_up") "follow_up_queued" "steer_queued")
              failure-type (if (= kind "follow_up") "follow_up_failed" "steer_failed")
              metadata (or metadata {})
              invoke (if (= kind "follow_up")
                       #(.followUp session message)
                       #(.steer session message))]
          (-> (invoke)
              (.then (fn []
                       (let [event (tool-event-payload run-id conversation-id session-id event-type
                                                       {:status "queued"
                                                        :preview preview
                                                        :metadata metadata})]
                         (when run-id
                           (append-run-event! run-id event))
                         (broadcast-ws-session! session-id "events" event)
                         {:ok true
                          :conversation_id conversation-id
                          :session_id session-id
                          :run_id run-id
                          :kind kind})))
              (.catch (fn [err]
                        (let [event (tool-event-payload run-id conversation-id session-id failure-type
                                                        {:status "failed"
                                                         :error (str err)
                                                         :preview preview
                                                         :metadata metadata})]
                          (when run-id
                            (append-run-event! run-id event))
                          (broadcast-ws-session! session-id "events" event))
                        (throw err))))))
      (js/Promise.reject (js/Error. "Conversation is not active in the agent runtime")))))
