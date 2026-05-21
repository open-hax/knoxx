(ns knoxx.backend.infra.agent.message
  (:require [clojure.string :as str]
            [knoxx.backend.domain.agent.reasoning :as reasoning]
            [knoxx.backend.extern.agent-message :as xagent-message]))

(defn stored-session-message->agent-message
  [message]
  (xagent-message/stored-message->agent-message message))

(defn planner-row->stored-session-message
  [row]
  (let [role (some-> (:role row) str)
        text (some-> (:text row) str)]
    (when (and (contains? #{"user" "assistant" "system"} role)
               (not (str/blank? text)))
      {:role role
       :content text})))



(defn- comparable-session-message
  [message]
  {:role (some-> (:role message) str)
   :content (some-> (:content message) str)})

(defn merge-restored-session-messages
  [base-messages overlay-messages]
  (let [base (vec (or base-messages []))
        overlay (vec (or overlay-messages []))
        base* (mapv comparable-session-message base)
        overlay* (mapv comparable-session-message overlay)
        overlap (loop [n (min (count base*) (count overlay*))]
                  (cond
                    (zero? n) 0
                    (= (subvec base* (- (count base*) n))
                       (subvec overlay* 0 n)) n
                    :else (recur (dec n))))]
    (into base (subvec overlay overlap))))

(defn sync-system-message
  [messages system-prompt]
  (let [items (vec (or messages []))
        prompt (some-> system-prompt str str/trim not-empty)]
    (if-not prompt
      items
      (let [system-index (reduce-kv (fn [_ idx entry]
                                      (when (= "system" (some-> (:role entry) str str/lower-case))
                                        (reduced idx)))
                                    nil
                                    items)]
        (if (some? system-index)
          (let [updated (assoc items system-index {:role "system" :content prompt})]
            (->> updated
                 (keep-indexed (fn [idx entry]
                                 (when (or (not= "system" (some-> (:role entry) str str/lower-case))
                                           (= idx system-index))
                                   entry)))
                 vec))
          (into [{:role "system" :content prompt}] items))))))

(defn message-text-size
  [message]
  (+ (count (str (or (:content message) (:summary message) "")))
     (reduce + 0 (map #(count (str (or (:text %) (:filename %) (:url %) "")))
                      (or (:content-parts message) (:contentParts message) [])))))
(defn stored-content-part->agent-part
  [part]
  (xagent-message/content-part->agent-part part))

(defn mime->audio-format
  [mime-type]
  (xagent-message/mime->audio-format mime-type))

(defn split-think-tags
  "Extract a leading <think>...</think> block from assistant text.

   Some Gemma-family models emit thinking traces inline instead of as structured
   reasoning parts. This keeps the assistant answer clean while preserving
   the trace in :reasoning."
  [text]
  (reasoning/split-think-tags text))

(defn content-part-type  [part]
  (cond
    (keyword? (:type part)) (name (:type part))
    (string? (:type part)) (:type part)
    :else nil))
