(ns knoxx.backend.law.control
  "Pure invariants for triggered-agent control/job data.

   This namespace deliberately avoids Redis, env access, Discord defaults, and
   contract loading. It normalizes plain maps at the runtime-control boundary."
  (:require [clojure.string :as str]
            [knoxx.backend.shape.parse :refer [parse-positive-int]]))

(def trigger-kind-options
  ["cron" "event"])

(def trigger-kinds
  (set trigger-kind-options))

(def source-kinds
  ["discord" "github" "cron" "manual"])

(def max-messages-limit 100)
(def max-message-chars-limit 8000)

(defn clamp-max-messages
  [value fallback]
  (let [n (or (parse-positive-int value)
              (parse-positive-int fallback)
              25)]
    (max 1 (min max-messages-limit n))))

(defn clamp-max-message-chars
  [value fallback]
  (let [n (or (parse-positive-int value)
              (parse-positive-int fallback)
              1200)]
    (max 120 (min max-message-chars-limit n))))

(defn clamp-source-config
  [source-config default-config]
  (let [cfg (or source-config {})]
    (cond-> (assoc cfg :maxMessages (clamp-max-messages (:maxMessages cfg)
                                                        (:maxMessages (or default-config {}))))
      (parse-positive-int (or (:maxTotalMessages cfg) (:max-total-messages cfg)))
      (assoc :maxTotalMessages (clamp-max-messages (or (:maxTotalMessages cfg)
                                                       (:max-total-messages cfg))
                                                   (:maxMessages cfg)))

      (parse-positive-int (or (:maxMessageChars cfg) (:max-message-chars cfg) (:max_message_chars cfg)))
      (assoc :maxMessageChars (clamp-max-message-chars (or (:maxMessageChars cfg)
                                                           (:max-message-chars cfg)
                                                           (:max_message_chars cfg))
                                                       (:maxMessageChars (or default-config {}))))

      (contains? cfg :stickySession)
      (assoc :stickySession (boolean (:stickySession cfg)))

      (parse-positive-int (:sessionMaxMessages cfg))
      (assoc :sessionMaxMessages (parse-positive-int (:sessionMaxMessages cfg))))))

(defn normalize-tool-policy-entry
  [policy]
  (let [tool-id (some-> (or (:toolId policy)
                            (:tool-id policy)
                            (:tool_id policy))
                        str
                        str/trim
                        not-empty)
        effect (some-> (or (:effect policy) "allow")
                       str
                       str/trim
                       str/lower-case
                       not-empty)]
    (when tool-id
      {:toolId tool-id
       :effect (if (#{"allow" "deny"} effect) effect "allow")})))

(defn normalize-tool-policy-list
  [policies]
  (->> (or policies [])
       (keep normalize-tool-policy-entry)
       vec))
