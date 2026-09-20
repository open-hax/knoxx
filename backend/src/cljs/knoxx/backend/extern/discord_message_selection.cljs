(ns knoxx.backend.extern.discord-message-selection
  "Clock-based Discord history selection and OpenPlanner label reads."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.discord.messages :as messages]
            [knoxx.backend.infra.clients.openplanner :as openplanner-client]))

(defn parse-hours
  "Discord boundary operation: parse-hours."
  [value default-hours]
  (let [n (js/Number value)]
    (if (and (not (js/isNaN n)) (pos? n)) n default-hours)))

(defn- timestamp-ms
  [value]
  (let [ms (.parse js/Date (str (or value "")))]
    (when-not (js/isNaN ms) ms)))

(defn- within-hours?
  [hours message]
  (if-let [ms (timestamp-ms (:timestamp message))]
    (>= ms (- (.now js/Date) (* hours 60 60 1000)))
    true))

(defn- chronological-discord-messages
  [messages]
  (vec (sort-by (fn [message] (or (timestamp-ms (:timestamp message)) 0)) messages)))

(defn- good-first-then-not-bad
  [messages]
  (let [non-bad (messages/drop-bad-discord-messages messages)
        good (chronological-discord-messages (filter #(= "good" (messages/discord-message-quality %)) non-bad))
        not-bad (chronological-discord-messages (remove #(= "good" (messages/discord-message-quality %)) non-bad))]
    (vec (concat good not-bad))))

(defn ^:async attach-openplanner-labels!
  "Discord boundary operation: attach-openplanner-labels!."
  [config messages]
  (let [client (openplanner-client/client config)]
    (if (or (empty? messages) (not (openplanner-client/enabled? client)))
      (good-first-then-not-bad messages)
      (try
        (let [ids (mapv messages/discord-record-id messages)
              response (await (openplanner-client/record-labels! client ids))
              labels (:labels response)]
          (->> messages
               (mapv (fn [message]
                       (assoc message :openplannerLabels
                              (messages/label-for-record-id labels (messages/discord-record-id message)))))
               good-first-then-not-bad))
        (catch :default error
          (.warn js/console "[discord-tools] OpenPlanner label lookup failed; failing closed to avoid surfacing crossed/bad messages" error)
          [])))))

(defn discord-search-result
  "Discord boundary operation: discord-search-result." [scope timeframe-hours user-id query limit result labelled]
  (let [needle (some-> query str str/lower-case)
        author-id (some-> user-id str not-empty)
        filtered (->> labelled
                      (filter #(within-hours? timeframe-hours %))
                      (filter (fn [message]
                                (and (or (str/blank? (str (or needle "")))
                                         (str/includes? (str/lower-case (str (:content message))) needle))
                                     (or (nil? author-id)
                                         (= author-id (:authorId message))))))
                      good-first-then-not-bad
                      (take (or limit 50))
                      vec)]
    {:messages filtered
     :count (count filtered)
     :scope scope
     :channelId (:channelId result)
     :dmChannelId (:dmChannelId result)
     :source "client_side_filter_openplanner_labels"
     :qualityOrder "good_chronological_then_not_bad_chronological"
     :sinceHours timeframe-hours}))

