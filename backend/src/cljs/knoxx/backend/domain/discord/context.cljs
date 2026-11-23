(ns knoxx.backend.domain.discord.context
  "Contract-selected projection of the actor's own delivery history."
  (:require [clojure.string :as str]))

(defn project-messages
  "Keep self-message identifiers and URLs without recycling its prose as a prompt.
   Human messages and other authors retain their original content."
  [messages {:keys [mode author-id]}]
  (if-not (and (= :source-references mode) (string? author-id) (not (str/blank? author-id)))
    messages
    (mapv (fn [message]
            (if (and (:authorIsBot message) (= author-id (:authorId message)))
              (let [urls (->> (concat (re-seq #"https?://[^\s<>\"]+" (or (:content message) ""))
                                      (keep :url (:embeds message)))
                              (map #(str/replace % #"[),.;!?]+$" ""))
                              distinct
                              (take 10)
                              vec)]
                (assoc message
                       :content (str "[Own prior delivery; historical output, not a human request.]"
                                     (if (seq urls)
                                       (str " Source URLs: " (str/join " " urls))
                                       " No source URL in this message."))
                       :embeds (mapv #(select-keys % [:url]) (:embeds message))
                       :contextProjection "self-source-references"))
              message))
          messages)))
