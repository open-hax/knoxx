(ns knoxx.backend.domain.social.conversation
  "Derive conversation identifiers from confirmed current-run receipts only."
  (:require [clojure.string :as str]
            [knoxx.backend.law.conversation :as law]))

(defn- post-uri? [value]
  (and (string? value) (boolean (re-matches #"at://did:[^/]+/app\.bsky\.feed\.post/[^/?#]+" value))))

(defn- field [value key] (or (get value key) (get value (keyword key))))

(defn- reference [value]
  (let [uri (field value "uri") cid (field value "cid")]
    (when (and (post-uri? uri) (not (str/blank? cid))) {:id uri :cid cid})))

(defn- confirmed? [status] (contains? #{"confirmed" :confirmed} status))

(defn- completed? [receipt]
  (and (= "completed" (:status receipt)) (not (:is_error receipt))))

(defn- bluesky [receipt]
  (let [d (get-in receipt [:result :details])]
    (cond
      (= "bluesky_publish" (:tool_name receipt))
      (when (and (confirmed? (get-in d [:actionOutcome :status])) (reference d))
        [{:platform "bluesky" :latest (reference d)
          :root (or (reference (:replyRoot d)) (when (str/blank? (:reply_to d)) (reference d)))
          :parent (reference (:replyParent d))}])
      (= "social_request" (:tool_name receipt))
      (when (and (= "bluesky" (:platform d)) (every? #(confirmed? (:status %)) (:actionOutcomes d)))
        (keep (fn [post]
                (when (reference post)
                  {:platform "bluesky" :latest (reference post)
                   :root (or (reference (:replyRoot post)) (when (str/blank? (:reply_to post)) (reference post)))
                   :parent (reference (:replyParent post))})) (:publications d))))))

(defn- discord [receipt]
  (let [d (get-in receipt [:result :details]) api? (= "social_request" (:tool_name receipt))
        body (when api? (:body d)) native? (= "discord_send" (:tool_name receipt))
        confirmed? (if api? (and (= "discord" (:platform d))
                                (= "create_message" (:operation d))
                                (seq (:actionOutcomes d)) (every? #(confirmed? (:status %)) (:actionOutcomes d)))
                       (confirmed? (get-in d [:actionOutcome :status])))
        channel (if api? (field body "channel_id") (:channelId d))
        ids (if api? [(field body "id")] (or (seq (:messageIds d)) [(:messageId d)]))
        parent (if api? (field (field body "message_reference") "message_id") (get-in receipt [:input :reply_to]))]
    (when (and (or api? native?) confirmed? (not (str/blank? channel))
               (seq ids) (every? #(and (string? %) (re-matches #"[0-9]+" %)) ids))
      [(cond-> {:platform "discord" :root {:id channel} :latest {:id (last ids)}
                :channel-id channel :message-ids (vec ids)}
         (not (str/blank? parent)) (assoc :parent {:id parent}))])))

(defn- candidates
  [receipt]
  (when (completed? receipt)
    (map #(assoc % :source-tool (:tool_name receipt) :source-call (or (:id receipt) (:tool_call_id receipt)))
         (concat (bluesky receipt) (discord receipt)))))

(defn- uncertain-publication?
  [platform receipt]
  (let [tool (:tool_name receipt) d (get-in receipt [:result :details])
        receipt-platform (case tool "bluesky_publish" "bluesky" "discord_send" "discord" (:platform d))
        outcomes (if (= "social_request" tool) (:actionOutcomes d) [(:actionOutcome d)])]
    (and (= platform receipt-platform)
         (some #(contains? #{"unknown" "partial" :unknown :partial} (:status %)) outcomes))))

(defn derive-cursor
  "Accept only semantic annotations from the model. Platform references and
   observation time come from trusted server/run data. Missing root refuses
   persistence rather than silently starting a different conversation."
  [run parameters observed-at]
  (law/assert-valid! law/Remember parameters)
  (let [candidate (last (filter #(or (nil? (:platform parameters)) (= (:platform parameters) (:platform %)))
                                (mapcat candidates (:tool_receipts run))))]
    (when-not (and (= "running" (:status run)) candidate (:root candidate)
                   (not-any? #(uncertain-publication? (:platform candidate) %) (:tool_receipts run)))
      (throw (ex-info "Not recorded: this live turn needs a confirmed own message/post with actual root metadata. Send nothing again; inspect the existing receipt."
                      {:code "conversation_confirmation_required" :phase :preflight})))
    (law/assert-valid! law/Cursor
                      (merge (into {} (remove (comp nil? val)) candidate) {:version 1 :actor-id (:actor_id run) :run-id (:run_id run)
                                        :observed-at observed-at :topic (:topic parameters) :state (:state parameters)
                                        :question (or (:question parameters) "") :annotation-tier "provisional"}))))
