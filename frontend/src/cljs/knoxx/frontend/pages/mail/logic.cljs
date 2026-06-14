(ns knoxx.frontend.pages.mail.logic
  "Pure logic for the actor mailbox page. CLJS port of the helpers in
   src/pages/MailPage.tsx and the mailbox normalizers in
   src/lib/api/runtime.ts."
  (:require [clojure.string :as str]))

(defn record-string
  "First value under `ks` that is a non-blank string (returned untrimmed)."
  [m & ks]
  (some (fn [k]
          (let [v (get m k)]
            (when (and (string? v) (not (str/blank? v))) v)))
        ks))

(defn status-tone [status]
  (case status
    "pending" "bg-amber-500/15 text-amber-200 border-amber-500/30"
    "failed" "bg-red-500/15 text-red-200 border-red-500/30"
    "delivered" "bg-emerald-500/15 text-emerald-200 border-emerald-500/30"
    "acknowledged" "bg-sky-500/15 text-sky-200 border-sky-500/30"
    "bg-slate-500/15 text-slate-200 border-slate-500/30"))

(defn format-date
  "Localized date string; em-dash when missing, passthrough when unparseable."
  [value]
  (if (str/blank? value)
    "—"
    (let [date (js/Date. value)]
      (if (js/Number.isNaN (.getTime date))
        value
        (.toLocaleString date)))))

(defn find-string-deep
  "Walks maps/vectors up to depth 4 looking for a non-blank string under
   any of `ks`. Direct keys win over nested values. Returns it trimmed."
  ([value ks] (find-string-deep value ks 0))
  ([value ks depth]
   (when (<= depth 4)
     (cond
       (map? value)
       (or (some (fn [k]
                   (let [v (get value k)]
                     (when (and (string? v) (not (str/blank? v)))
                       (str/trim v))))
                 ks)
           (some #(find-string-deep % ks (inc depth)) (vals value)))

       (sequential? value)
       (some #(find-string-deep % ks (inc depth)) value)

       :else nil))))

(def ^:private run-id-keys
  [:run-id :runId :run_id :source-run-id :target-run-id :sourceRunId :targetRunId])
(def ^:private session-id-keys [:session-id :sessionId :session_id])
(def ^:private conversation-id-keys [:conversation-id :conversationId :conversation_id])

(defn mailbox-links
  "Cross-navigation links derivable from a mailbox entry's references."
  [{:keys [contentRef target source]}]
  (let [run-id (find-string-deep contentRef run-id-keys)
        session-id (some #(find-string-deep % session-id-keys) [contentRef target source])
        conversation-id (some #(find-string-deep % conversation-id-keys) [contentRef target source])
        event-id (find-string-deep contentRef [:event-id :eventId :event_id])
        session (or conversation-id session-id)]
    (cond-> []
      run-id (conj {:label "Open run"
                    :path (str "/agents?tab=audit&run=" (js/encodeURIComponent run-id))
                    :detail run-id})
      session (conj {:label "Open session"
                     :path (str "/agents?tab=audit&session=" (js/encodeURIComponent session))
                     :detail session})
      event-id (conj {:label "Open event"
                      :path (str "/events?eventId=" (js/encodeURIComponent event-id))
                      :detail event-id}))))

(defn unread-count [entries]
  (count (remove #(= "acknowledged" (:status %)) entries)))

;; ── mailbox response normalizers (port of lib/api/runtime.ts) ───────────────

(defn- as-record [v] (if (map? v) v {}))

(defn normalize-entry
  "Normalizes one mailbox entry; nil when it has no id."
  [value]
  (when (map? value)
    (when-let [id (when (string? (:id value)) (:id value))]
      {:id id
       :kind (or (:kind value) "actor-message")
       :status (or (:status value) "pending")
       :source (as-record (:source value))
       :target (as-record (:target value))
       :delivery (as-record (:delivery value))
       :contentRef (as-record (:contentRef value))
       :metadata (as-record (:metadata value))
       :preview (:preview value)
       :lastError (:lastError value)
       :createdAt (:createdAt value)
       :updatedAt (:updatedAt value)
       :deliveredAt (:deliveredAt value)
       :acknowledgedAt (:acknowledgedAt value)
       :expiresAt (:expiresAt value)})))

(defn normalize-list-response
  "Normalizes a mailbox list response, defaulting :box to `fallback-box`."
  [value fallback-box]
  (let [record (as-record value)]
    {:ok (if (boolean? (:ok record)) (:ok record) true)
     :box (if (contains? #{"inbox" "outbox"} (:box record)) (:box record) fallback-box)
     :actor-id (:actorId record)
     :entries (into [] (keep normalize-entry) (:entries record))}))
