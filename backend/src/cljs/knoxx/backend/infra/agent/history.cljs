(ns knoxx.backend.infra.agent.history
  "Agent history/transcript ports and context pruning helpers."
  (:require [clojure.string :as str]
            [knoxx.backend.extern.eta-mu :as eta-mu-extern]
            [knoxx.backend.infra.agent.message :as msg]
            [knoxx.backend.infra.stores.message-source :refer [fetch-messages!]]))

(defprotocol IMessageHistory
  (load-history [history request])
  (append-message! [history message]))

(defprotocol ITranscriptCodec
  (stored->provider-messages [codec stored-messages context-policy])
  (provider->stored-message [codec provider-message]))

(defrecord MessageSourceHistory [message-source]
  IMessageHistory
  (load-history [_ {:keys [conversation-id]}]
    (fetch-messages! message-source conversation-id))

  (append-message! [_ _message]
    (js/Promise.resolve nil)))

(defn message-source-history
  [message-source]
  (->MessageSourceHistory message-source))

(defn- positive-int-value
  [v]
  (when (integer? v) (max v 0)))

(defn- context-policy
  [agent-spec]
  (or (:context-policy agent-spec)
      (:contextPolicy agent-spec)
      (:context agent-spec)
      (get-in agent-spec [:extras :context])
      (get-in agent-spec [:extras :context-policy])))

(defn prune-session-messages
  [agent-spec messages]
  (let [items (vec (or messages []))
        policy (context-policy agent-spec)]
    (if-not policy
      items
      (let [max-messages (positive-int-value (or (:max-messages policy) (:maxMessages policy) (:max_messages policy)))
            max-chars (positive-int-value (or (:max-chars policy) (:maxChars policy) (:max_chars policy)))
            preserve-system? (not= false (or (:preserve-system policy) (:preserveSystem policy) (:preserve_system policy)))
            system-messages (if preserve-system?
                              (filterv #(= "system" (some-> (:role %) str str/lower-case)) items)
                              [])
            body-messages (if preserve-system?
                            (remove #(= "system" (some-> (:role %) str str/lower-case)) items)
                            items)
            by-count (if max-messages (take-last max-messages (vec body-messages)) (vec body-messages))
            by-chars (if max-chars
                       (loop [remaining (reverse by-count) total 0 kept '()]
                         (if-let [message (first remaining)]
                           (let [size (msg/message-text-size message)]
                             (if (and (seq kept) (> (+ total size) max-chars))
                               (vec kept)
                               (recur (rest remaining) (+ total size) (conj kept message))))
                           (vec kept)))
                       (vec by-count))]
        (vec (concat system-messages by-chars))))))

(defrecord DefaultTranscriptCodec []
  ITranscriptCodec
  (stored->provider-messages [_ stored-messages agent-spec]
    (->> (prune-session-messages agent-spec stored-messages)
         (keep msg/stored-session-message->agent-message)
         vec))

  (provider->stored-message [_ provider-message]
    (msg/planner-row->stored-session-message provider-message)))

(def default-transcript-codec
  (->DefaultTranscriptCodec))

(defn ^:async rehydrate-session-manager!
  [message-source session-manager conversation-id agent-spec]
  (let [history (message-source-history message-source)
        messages (await (load-history history {:conversation-id conversation-id}))
        merged-messages (-> (vec (or messages []))
                            (msg/sync-system-message (:system-prompt agent-spec))
                            (#(prune-session-messages agent-spec %)))]
    (doseq [message merged-messages]
      (when-let [agent-message (msg/stored-session-message->agent-message message)]
        (eta-mu-extern/append-message! session-manager agent-message)))
    {:session-manager session-manager
     :restored (boolean (seq merged-messages))}))
