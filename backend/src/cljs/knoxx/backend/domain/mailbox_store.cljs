(ns knoxx.backend.domain.mailbox-store
  "Pure mailbox transitions; accepted operations are the sole durable authority."
  (:require [knoxx.backend.law.mailbox-store :as law]))

(defn- due? [now instant] (and instant (not (neg? (compare now instant)))))
(defn- actor-required! [scope]
  (when-not (or (:admin? scope) (:actor-id scope))
    (law/refuse! 403 "mailbox_actor_required" "An explicit actor binding is required")))
(defn- scope! [scope at]
  (law/checked! :mailbox/scope law/Scope scope)
  (law/checked! :mailbox/clock law/Instant at)
  (actor-required! scope))
(defn- key-for [scope id] [(:org-id scope) id])
(defn- projected-entry [entry at]
  (if (and (contains? #{"pending" "failed"} (:mailbox/status entry))
            (due? at (:mailbox/expires-at entry)))
    (assoc entry :mailbox/status "expired") entry))
(defn- entry! [state scope id at]
  (law/checked! :mailbox/id law/NonBlank id)
  (if-let [entry (get-in state [:entries (key-for scope id)])]
    (projected-entry entry at)
    (law/refuse! 404 "mailbox_not_found" "Mailbox entry was not found")))
(defn- sender! [scope entry]
  (when-not (or (:admin? scope) (= (:actor-id scope) (get-in entry [:mailbox/source :actor-id])))
    (law/refuse! 404 "mailbox_not_found" "Mailbox entry was not found")))

(defn resolve-route
  "Read only a current actor route in the caller's tenant."
  [state scope actor at]
  (scope! scope at)
  (law/checked! :mailbox/actor law/NonBlank actor)
  (when-let [route (get-in state [:routes (key-for scope actor)])]
    (when-not (due? at (:expires-at route)) route)))

(defn- own-route! [scope actor]
  (when-not (or (:admin? scope) (= actor (:actor-id scope)))
    (law/refuse! 403 "mailbox_route_denied" "Only this actor may register its route")))

(defn- register [state {:keys [scope at route-expires-at payload]}]
  (law/checked! :mailbox/route law/Route payload)
  (law/checked! :mailbox/route-expiry law/Instant route-expires-at)
  (own-route! scope (:actor-id payload))
  (let [route (assoc (dissoc payload :ttl-seconds) :org-id (:org-id scope) :status "active"
                     :last-seen-at at :expires-at route-expires-at)]
    [(assoc-in state [:routes (key-for scope (:actor-id payload))] route) route]))

(defn- unregister [state {:keys [scope payload]}]
  (let [conversation (:conversation-id payload)
        _ (law/checked! :mailbox/conversation law/NonBlank conversation)
        keys (for [[key route] (:routes state)
                   :when (and (= (:org-id scope) (:org-id route))
                              (= conversation (:conversation-id route))
                              (or (:admin? scope) (= (:actor-id scope) (:actor-id route))))] key)]
    [(update state :routes #(apply dissoc % keys)) {:ok true :retired (count keys)}]))

(defn- create [state {:keys [scope payload at]}]
  (law/checked! :mailbox/entry law/EntryInput payload)
  (sender! scope payload)
  (let [key (key-for scope (:mailbox/id payload))
        original (get-in state [:inputs key])]
    (when (and original (not= original payload))
      (law/refuse! 409 "mailbox_identity_conflict" "Mailbox id is already bound to a different message"))
    (when (and (nil? original) (due? at (:mailbox/expires-at payload)))
      (law/refuse! 409 "mailbox_expired" "A new message must not already be expired"))
    (if original [state (entry! state scope (:mailbox/id payload) at)]
        (let [entry (-> payload (dissoc :mailbox/content)
                        (assoc :mailbox/org-id (:org-id scope) :mailbox/status "pending"
                               :mailbox/created-at at :mailbox/updated-at at :mailbox/durable? true)
                        (assoc-in [:mailbox/delivery :attempts] 0))]
          [(-> state (assoc-in [:inputs key] payload) (assoc-in [:entries key] entry)) entry]))))

(defn list-entries
  "Read a tenant inbox/outbox; non-admin filters cannot select another actor."
  [state scope filters at]
  (scope! scope at)
  (law/checked! :mailbox/filters law/Filters filters)
  (let [filters (if (and (not (:admin? scope)) (nil? (:source-actor-id filters)) (nil? (:target-actor-id filters)))
                  (assoc filters :target-actor-id (:actor-id scope)) filters)]
    (when (and (not (:admin? scope))
               (or (and (:source-actor-id filters) (not= (:actor-id scope) (:source-actor-id filters)))
                   (and (:target-actor-id filters) (not= (:actor-id scope) (:target-actor-id filters)))))
      (law/refuse! 403 "mailbox_scope_denied" "Mailbox filters must name the caller's own actor"))
    (let [paths {:source-actor-id [:mailbox/source :actor-id] :target-actor-id [:mailbox/target :actor-id]
                 :target-session-id [:mailbox/target :session-id] :source-run-id [:mailbox/source :run-id]
                 :status [:mailbox/status]}
          entries (->> (vals (:entries state))
                       (filter #(= (:org-id scope) (:mailbox/org-id %)))
                       (map #(projected-entry % at))
                       (filter #(every? (fn [[key path]]
                                         (or (nil? (get filters key)) (= (get filters key) (get-in % path)))) paths))
                       (sort-by (juxt :mailbox/created-at :mailbox/id)) reverse vec)]
      {:entries (vec (take (or (:limit filters) 50) entries)) :total (count entries)})))

(defn read-message
  "Resolve full immutable content only within sender, recipient or tenant admin authority."
  [state scope id at]
  (scope! scope at)
  (let [entry (entry! state scope id at)]
    (when-not (or (:admin? scope) (= (:actor-id scope) (get-in entry [:mailbox/source :actor-id]))
                   (= (:actor-id scope) (get-in entry [:mailbox/target :actor-id])))
      (law/refuse! 404 "mailbox_not_found" "Mailbox entry was not found"))
    (let [content (get-in state [:inputs (key-for scope id) :mailbox/content])]
      (when-not (string? content)
        (law/refuse! 409 "mailbox_content_unavailable" "This legacy message has no canonical mailbox body"))
      (assoc entry :mailbox/content content))))

(defn- eligible? [entry options at]
  (and (contains? (set (or (:statuses options) ["pending" "failed"])) (:mailbox/status entry))
       (< (get-in entry [:mailbox/delivery :attempts] 0) (or (:max-attempts options) 5))
       (or (nil? (get-in entry [:mailbox/delivery :next-at]))
           (due? at (get-in entry [:mailbox/delivery :next-at])))
       (or (nil? (get-in entry [:mailbox/delivery :lease-until]))
           (due? at (get-in entry [:mailbox/delivery :lease-until])))))

(defn- claim [state {:keys [scope id payload at lease-until next-at]}]
  (law/checked! :mailbox/claim law/ClaimOptions payload)
  (law/checked! :mailbox/lease law/Instant lease-until)
  (law/checked! :mailbox/next-at law/Instant next-at)
  (when (and (not (:admin? scope)) (nil? (:mailbox-id payload)))
    (law/refuse! 403 "mailbox_admin_required" "Bulk delivery claims require tenant administration"))
  (let [entries (if-let [target (:mailbox-id payload)]
                  (let [entry (entry! state scope target at)] (sender! scope entry) [entry])
                  (map #(projected-entry % at) (filter #(= (:org-id scope) (:mailbox/org-id %)) (vals (:entries state)))))
        eligible (->> entries (filter #(eligible? % payload at))
                      (sort-by (juxt :mailbox/created-at :mailbox/id)) (take (or (:limit payload) 25)))
        claimed (mapv #(-> % (assoc :mailbox/status "pending" :mailbox/updated-at at)
                            (update-in [:mailbox/delivery :attempts] inc)
                            (assoc-in [:mailbox/delivery :claim-id] (str id "/" (:mailbox/id %)))
                            (assoc-in [:mailbox/delivery :lease-until] lease-until)
                            (assoc-in [:mailbox/delivery :next-at] next-at)) eligible)]
    [(reduce #(assoc-in %1 [:entries (key-for scope (:mailbox/id %2))] %2) state claimed)
     {:entries claimed :claimed (count claimed)}]))

(defn- mark [state {:keys [scope payload at]}]
  (let [{:keys [entry-id status options]} payload
        _ (law/checked! :mailbox/outcome [:enum "delivered" "failed"] status)
        _ (law/checked! :mailbox/mark law/MarkOptions options)
        entry (entry! state scope entry-id at)
        key (key-for scope entry-id)
        receipt {:status status :options (dissoc options :operation-id)}]
    (sender! scope entry)
    (when-not (= (:claim-id options) (get-in entry [:mailbox/delivery :claim-id]))
      (law/refuse! 409 "mailbox_claim_stale" "Delivery outcome does not own the current claim"))
    (if-let [existing (get-in state [:receipts [key (:claim-id options)]])]
      (if (= receipt existing) [state entry]
          (law/refuse! 409 "mailbox_outcome_conflict" "The delivery claim already has another outcome"))
      (do
        (when-not (= "pending" (:mailbox/status entry))
          (law/refuse! 409 "mailbox_claim_stale" "The message can no longer accept this outcome"))
        (let [updated (cond-> (-> entry (assoc :mailbox/status status :mailbox/updated-at at)
                                  (update :mailbox/delivery dissoc :lease-until))
                        (= status "delivered") (assoc :mailbox/delivered-at at)
                        (:content-ref options) (assoc :mailbox/content-ref (:content-ref options))
                        (:error options) (assoc-in [:mailbox/delivery :last-error] (:error options)))]
          [(-> state (assoc-in [:entries key] updated)
               (assoc-in [:receipts [key (:claim-id options)]] receipt)) updated])))))

(defn- acknowledge [state {:keys [scope payload at]}]
  (let [id (:entry-id payload) entry (entry! state scope id at)]
    (when-not (or (:admin? scope) (= (:actor-id scope) (get-in entry [:mailbox/target :actor-id])))
      (law/refuse! 404 "mailbox_not_found" "Mailbox entry was not found"))
    (when (contains? #{"expired" "superseded"} (:mailbox/status entry))
      (law/refuse! 409 "mailbox_terminal" "This message can no longer be acknowledged"))
    (if (= "acknowledged" (:mailbox/status entry)) [state entry]
        (let [ack (-> entry (assoc :mailbox/status "acknowledged" :mailbox/acknowledged-at at :mailbox/updated-at at)
                       (update :mailbox/delivery dissoc :claim-id :lease-until))]
          [(assoc-in state [:entries (key-for scope id)] ack) ack]))))

(defn transition
  "Replay an accepted finite command, preserving its original idempotent receipt."
  [state {:keys [kind scope id at] :as operation}]
  (law/checked! :mailbox/operation law/Operation operation)
  (scope! scope at)
  (let [key (key-for scope id) request (select-keys operation [:kind :scope :payload])]
    (if-let [existing (get-in state [:operations key])]
      (if (= request (:request existing)) [state (:result existing)]
          (law/refuse! 409 "mailbox_operation_conflict" "Mailbox operation id has different arguments"))
      (let [[next-state result] ((case kind :create create :register register :unregister unregister
                                       :claim claim :mark mark :ack acknowledge) state operation)]
        [(if (= state next-state) state
             (-> next-state (assoc-in [:operations key] {:request request :result result})
                 (update :version (fnil inc 0)))) result]))))
