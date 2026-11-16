(ns knoxx.backend.domain.social.action-admission
  "Pure, per-run admission of canonical platform actions and explicit outcomes."
  (:require [clojure.string :as str]))

(defn- nonblank?
  [value]
  (and (string? value) (not (str/blank? value))))

(defn- discord-message-intent
  [tool params channel]
  (let [publish? (= tool "discord_publish")
        text (if publish? (or (:content params) (:text params))
                         (or (:text params) (:content params)))]
    {:platform "discord" :effect :publish :target channel
     :arguments {:text (str/trim (or text ""))
                 :reply-to (when-not publish? (or (:reply_to params) (:replyTo params)))
                 :attachments (vec (or (:attachment_urls params) (:attachmentUrls params) []))}}))

(defn canonical-follow-target
  "Use a stable Bluesky DID as the follow audience across all write aliases."
  [value]
  (when (string? value)
    (re-matches #"did:[a-z0-9]+:[A-Za-z0-9.:%_-]+" (str/trim value))))

(defn native-intent
  "Canonicalize convenience aliases. Generic executors must produce this same
   intent after schema/effect planning, not use their tool name as the key."
  [tool params actor-id]
  (let [tool (str/replace tool "." "_")
        channel (or (:channel_id params) (:channelId params))]
    (case tool
      ("discord_send" "discord_publish") (discord-message-intent tool params channel)
      "discord_react"
      {:platform "discord" :effect :react :target channel
       :arguments {:message-id (or (:message_id params) (:messageId params)) :emoji (:emoji params)}}
      "discord_thread_create"
      {:platform "discord" :effect :thread :target channel
       :arguments {:message-id (or (:message_id params) (:messageId params)) :name (:name params)
                   :auto-archive-duration (or (:auto_archive_duration params) (:autoArchiveDuration params) 1440)}}
      "bluesky_publish"
      {:platform "bluesky" :effect :publish :target actor-id
       :arguments {:text (or (:text params) "") :reply-to (or (:replyTo params) "")
                   :images (vec (or (:images params) []))
                   :image-alts (vec (or (:imageAlts params) []))}}
      "bluesky_repost"
      {:platform "bluesky" :effect :amplify :target actor-id :arguments {:uri (:uri params)}}
      "bluesky_like"
      {:platform "bluesky" :effect :react :target actor-id :arguments {:uri (:uri params)}}
      "bluesky_follow"
      {:platform "bluesky" :effect :curate :target (canonical-follow-target (:actor params))
       :arguments {:collection "app.bsky.graph.follow" :operation :create}}
      "bluesky_chat_send"
      {:platform "bluesky" :effect :conversation :target (:convoId params)
       :arguments {:text (:text params) :reply-to (or (:replyToMessageId params) "")}}
      nil)))

(defn scope
  "A failure pauses the affected effect and audience, not every platform feature."
  [intent]
  (select-keys intent [:platform :effect :target]))

(defn reserve
  "Atomically apply the returned state before execution. Equivalent aliases and
   parallel writes share keys; a definite refusal permits changed arguments."
  [state run-id intent]
  (let [state (if (= run-id (:run-id state)) state {:run-id run-id :attempts {} :blocked {}})
        audience (scope intent)
        prior (get-in state [:attempts intent])
        pending (some #(and (= audience (scope (key %))) (= :in-flight (:status (val %))))
                      (:attempts state))
        attempt-id (inc (or (:next-attempt state) 0))
        reason (cond (get-in state [:blocked audience]) :unresolved-audience
                     prior :duplicate-action
                     pending :action-in-flight)]
    (if reason
      {:allowed? false :reason reason :state state}
      {:allowed? true :reservation {:run-id run-id :attempt-id attempt-id :intent intent}
       :state (-> state (assoc :next-attempt attempt-id)
                  (assoc-in [:attempts intent] {:status :in-flight :attempt-id attempt-id}))})))

(defn reserve-many
  "All members inspect the original state, then reserve together. Distinct
   actions in one request may share an audience; a duplicate inside the request
   or any conflict with earlier work refuses the entire group without mutation."
  [state run-id intents]
  (let [checks (mapv #(reserve state run-id %) intents)
        refused (some #(when-not (:allowed? %) %) checks)
        duplicate? (not= (count intents) (count (set intents)))
        next-state (if (= run-id (:run-id state)) state {:run-id run-id :attempts {} :blocked {}})
        attempt-id (inc (or (:next-attempt next-state) 0))]
    (if (or refused duplicate? (empty? intents) (> (count intents) 200))
      {:allowed? false :reason (cond duplicate? :duplicate-in-batch (empty? intents) :empty-batch
                                     (> (count intents) 200) :batch-too-large :else (:reason refused))
       :state state}
      {:allowed? true :reservation {:run-id run-id :attempt-id attempt-id :intents intents}
       :state (-> next-state (assoc :next-attempt attempt-id)
                  (update :attempts into (map #(vector % {:status :in-flight :attempt-id attempt-id}) intents)))})))

(defn- confirmed?
  [{:keys [platform effect]} details]
  (case [platform effect]
    ["discord" :publish] (and (true? (:sent details)) (nonblank? (:messageId details)))
    ["discord" :react] (true? (:reacted details))
    ["discord" :thread] (and (true? (:created details)) (nonblank? (:threadId details)))
    ["bluesky" :publish] (and (nonblank? (:uri details)) (nonblank? (:cid details)))
    ["bluesky" :amplify] (and (nonblank? (:uri details)) (nonblank? (:cid details)))
    ["bluesky" :react] (and (nonblank? (:uri details)) (nonblank? (:cid details)))
    ["bluesky" :curate] (and (nonblank? (:uri details)) (nonblank? (:cid details)))
    ["bluesky" :conversation] (nonblank? (:id details))
    false))

(defn outcome
  "Only typed response data proves a refusal. A message string, timeout, partial
   chunk delivery or missing confirmation must never become safe-to-retry."
  [intent {:keys [failed? details error-data]}]
  (let [status (or (:http-status error-data) (:status details))
        code (or (:platform-code error-data) (:code details))
        delivered (vec (or (:delivered-message-ids error-data) []))
        state (cond
                (seq delivered) :partial
                (and failed? (= :preflight (:phase error-data))) :not-attempted
                failed? (if (contains? #{400 401 403 404 405 409 413 415 422 429} status)
                          :refused :unknown)
                (confirmed? intent details) :confirmed
                :else :unknown)]
    (cond-> {:status state}
      (integer? status) (assoc :http-status status)
      (or (integer? code) (string? code)) (assoc :platform-code code)
      (seq delivered) (assoc :delivered-message-ids delivered))))

(defn- resolved-alias
  [state intent result paused?]
  (if-let [target (when-not (= :not-attempted (:status result)) (:resolved-target result))]
    (let [alias (assoc intent :target target)]
      (cond-> state
        (not (contains? (:attempts state) alias)) (assoc-in [:attempts alias] result)
        paused? (assoc-in [:blocked (scope alias)] result)))
    state))

(defn settle
  "Complete only the original reservation. A late older result cannot alter a
   newer run. Permission/auth/rate refusals pause this effect for this audience."
  [state {:keys [run-id attempt-id intent]} result]
  (if (and (= run-id (:run-id state))
           (= attempt-id (get-in state [:attempts intent :attempt-id]))
           (= :in-flight (get-in state [:attempts intent :status])))
    (let [paused? (or (contains? #{:unknown :partial} (:status result))
                      (contains? #{401 403 429} (:http-status result)))]
      (-> (cond-> (if (= :not-attempted (:status result))
                    (update state :attempts dissoc intent)
                    (assoc-in state [:attempts intent] result))
            paused? (assoc-in [:blocked (scope intent)] result))
          (resolved-alias intent result paused?)))
    state))

(defn settle-many
  "Keep individual evidence, but an ambiguous or partial group pauses every
   affected scope. A late old group cannot disturb a newer run."
  [state {:keys [run-id attempt-id intents]} outcomes]
  (when-not (= (count intents) (count outcomes))
    (throw (ex-info "Every reserved action requires an actual outcome" {:code "platform_group_outcomes_missing"})))
  (if (not= run-id (:run-id state))
    state
    (let [pending (set (filter #(and (= :in-flight (get-in state [:attempts % :status]))
                                     (= attempt-id (get-in state [:attempts % :attempt-id]))) intents))
          uncertain? (some #(contains? #{:unknown :partial} (:status %)) outcomes)
          settled (reduce (fn [current [intent result]]
                             (settle current {:run-id run-id :attempt-id attempt-id :intent intent} result)) state (map vector intents outcomes))]
      (if uncertain?
        (reduce (fn [current [intent outcome]]
                  (if (contains? pending intent)
                    (-> current (assoc-in [:blocked (scope intent)] {:status :partial})
                        (resolved-alias intent outcome true)) current))
                settled (map vector intents outcomes))
        settled))))
