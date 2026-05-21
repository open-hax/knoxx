(ns knoxx.backend.infra.agent.session-registry
  "Active in-process agent session registry port and atom-backed implementation.")

(defprotocol IActiveSessionRegistry
  (get-active-session-entry [registry conversation-id])
  (get-active-session [registry conversation-id])
  (put-active-session! [registry conversation-id entry])
  (touch-active-session! [registry conversation-id])
  (remove-active-session! [registry conversation-id])
  (sweep-expired-sessions! [registry now-ms]))

(def default-max-sessions 500)
(def default-inactive-ttl-ms (* 4 60 60 1000))

(defn- evict-oldest-entry
  [entries max-sessions]
  (if (and (pos? max-sessions)
           (> (count entries) max-sessions))
    (let [oldest (apply min-key (comp :last-accessed val) entries)]
      (dissoc entries (key oldest)))
    entries))

(defrecord AtomActiveSessionRegistry [sessions* max-sessions inactive-ttl-ms now-ms]
  IActiveSessionRegistry
  (get-active-session-entry [_ conversation-id]
    (get @sessions* conversation-id))

  (get-active-session [this conversation-id]
    (:session (get-active-session-entry this conversation-id)))

  (put-active-session! [_ conversation-id entry]
    (let [timestamp (or (:last-accessed entry) (now-ms))]
      (swap! sessions*
             (fn [entries]
               (-> entries
                   (assoc conversation-id (assoc entry :last-accessed timestamp))
                   (evict-oldest-entry max-sessions))))
      (get @sessions* conversation-id)))

  (touch-active-session! [_ conversation-id]
    (let [timestamp (now-ms)]
      (swap! sessions*
             (fn [entries]
               (if (contains? entries conversation-id)
                 (assoc-in entries [conversation-id :last-accessed] timestamp)
                 entries)))
      (get @sessions* conversation-id)))

  (remove-active-session! [_ conversation-id]
    (swap! sessions* dissoc conversation-id)
    nil)

  (sweep-expired-sessions! [_ now-ms-value]
    (let [cutoff (- now-ms-value inactive-ttl-ms)
          stale (for [[id entry] @sessions*
                      :when (< (or (:last-accessed entry) 0) cutoff)]
                  id)]
      (when (seq stale)
        (swap! sessions* #(apply dissoc % stale)))
      (vec stale))))

(defn atom-registry
  ([sessions*]
   (atom-registry sessions* {}))
  ([sessions* {:keys [max-sessions inactive-ttl-ms now-ms]
               :or {max-sessions default-max-sessions
                    inactive-ttl-ms default-inactive-ttl-ms}}]
   (->AtomActiveSessionRegistry sessions*
                                max-sessions
                                inactive-ttl-ms
                                (or now-ms #(.now js/Date)))))
