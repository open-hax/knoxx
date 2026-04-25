(ns knoxx.backend.agents.transcript
  "Session transcript building and message conversion."
  (:require [clojure.string :as str]
            [knoxx.backend.agent-runtime :refer [sync-system-message]]
            [knoxx.backend.agents.content :refer [nonblank assistant-content-parts session-message-text]]
            [knoxx.backend.text :refer [content-part-text]]))

(defn ^:export session->stored-messages
  "Exported simplified variant (no content-parts).  Used by tests and recovery."
  [session]
  (let [messages (when (and session (array? (aget session "messages")))
                   (array-seq (aget session "messages")))]
    (->> messages
         (keep (fn [message]
                 (let [role (some-> (aget message "role") str)
                       content (aget message "content")
                       text (cond
                              (string? content) (nonblank content)
                              (array? content) (->> (array-seq content)
                                                    (map content-part-text)
                                                    (remove str/blank?)
                                                    (str/join "\n\n")
                                                    nonblank)
                              :else (some-> (aget message "text") nonblank))]
                   (when (and (contains? #{"user" "assistant" "system"} role)
                              text)
                     {:role role
                      :content text}))))
         vec)))

(defn- transcript-messages
  "Internal richer variant that preserves assistant content-parts."
  [session]
  (let [messages (when (and session (array? (aget session "messages")))
                   (array-seq (aget session "messages")))]
    (->> messages
         (keep (fn [message]
                 (let [role (some-> (aget message "role") str)
                       text (some-> (session-message-text message) nonblank)
                       ;; Despite the name, assistant-content-parts extracts media parts from any
                       ;; pi message content array. We must persist user-side content parts too,
                       ;; otherwise restored sessions lose multimodal inputs.
                       parts (assistant-content-parts message)]
                   (when (and (contains? #{"user" "assistant" "system"} role)
                              (or text (seq parts)))
                     (cond-> {:role role}
                       text (assoc :content text)
                       (seq parts) (assoc :content-parts parts))))))
         vec)))

(defn- append-message-if-novel
  [messages message]
  (let [items (vec (or messages []))
        last-message (peek items)
        comparable (fn [entry]
                     (select-keys entry [:role :content :content-parts]))]
    (if (= (comparable last-message) (comparable message))
      items
      (conj items message))))

(defn- requested-system-prompt
  [agent-spec]
  (some-> (:system-prompt agent-spec) str str/trim not-empty))

(defn ensure-system-message
  [messages agent-spec]
  (sync-system-message messages (requested-system-prompt agent-spec)))

(defn transcript-before-prompt
  [session user-message agent-spec]
  (-> (transcript-messages session)
      (ensure-system-message agent-spec)
      (append-message-if-novel user-message)))

(defn transcript-after-turn
  [session fallback-messages]
  (let [snapshot (transcript-messages session)]
    (if (seq snapshot)
      snapshot
      (vec fallback-messages))))
