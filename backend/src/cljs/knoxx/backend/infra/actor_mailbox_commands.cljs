(ns knoxx.backend.infra.actor-mailbox-commands
  "Shared human/agent mailbox commands with durable claims and honest outcomes."
  (:require [knoxx.backend.domain.actor.mailbox :as data]
            [knoxx.backend.domain.mailbox-address :as address]
            [knoxx.backend.extern.mailbox-store :as host]
            [knoxx.backend.infra.actor-mailbox :as mailbox]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.mailbox-delivery-registry :as delivery]
            [knoxx.backend.law.mailbox-store :as law]))

(defn- ^:async current-context! [runtime context]
  (let [current (await (authz/current-context! runtime context))]
    (authz/ensure-permission! current "agent.chat.use") current))
(defn allowed?
  "Explicit denial wins over administrator defaults at both interfaces."
  [context]
  (and (not= "deny" (authz/ctx-tool-effect context "actors.send-message"))
       (authz/ctx-tool-allowed? context "actors.send-message")))
(defn- require-send! [context]
  (when-not (allowed? context)
    (law/refuse! 403 "mailbox_tool_denied" "Actor message sending is denied by current tool policy")))
(defn- require-mode! [context mode]
  (when-let [permission (get {"event" "org.events.control" "steer" "agent.controls.steer"
                             "follow-up" "agent.controls.follow_up"} mode)]
    (authz/ensure-permission! context permission)))
(defn- ^:async resolved-target! [mailbox request]
  (let [target (address/resolve-target request (:lineage request))]
    (if-let [actor (:actor-id target)]
      (if-let [route (await (mailbox/resolve-actor-session! mailbox actor))]
        (do
          (doseq [field [:conversation-id :session-id]]
            (when (and (get target field) (not= (get target field) (get route field)))
              (law/refuse! 409 "mailbox_route_conflict" "Explicit target differs from the current actor route")))
          (merge target (select-keys route [:conversation-id :session-id :run-id])))
        (if (or (:conversation-id target) (:session-id target))
          (law/refuse! 404 "mailbox_target_not_found" "The named actor has no matching live route") target)) target)))
(defn- raw-entry [mailbox request target mode]
  (let [id (:operation-id request) lineage (:lineage request)
        source (cond-> (data/source-from-context lineage)
                 (get-in mailbox [:scope :actor-id]) (assoc :actor-id (get-in mailbox [:scope :actor-id])))]
    {:id id :kind "actor-message" :source source :target target :delivery-mode mode
     :content (:content request) :preview (:content request) :metadata (or (:metadata request) {})
     :content-ref (cond-> {:mailbox-id id} (:run-id lineage) (assoc :source-run-id (:run-id lineage)))}))
(defn- unconfirmed! [entry cause]
  (throw (ex-info "Delivery happened, but its durable receipt could not be confirmed"
                  {:status 503 :code "mailbox_delivery_unconfirmed" :mailbox-id (:mailbox/id entry)
                   :claim-id (get-in entry [:mailbox/delivery :claim-id])} cause)))
(defn- ^:async record-failure! [mailbox entry cause]
  (try (await (mailbox/mark-failed! mailbox entry cause))
       (catch :default persistence-error
         (throw (ex-info "Delivery failed and its failure receipt could not be persisted"
                         {:status 503 :code "mailbox_failure_unconfirmed" :mailbox-id (:mailbox/id entry)} persistence-error))))
  (throw cause))
(defn- ^:async delivery-context! [runtime context mailbox entry]
  (let [current (await (current-context! runtime context))
        current-scope (:scope (mailbox/context runtime current))
        actual (await (mailbox/read-message! mailbox (:mailbox/id entry)))]
    (require-send! current)
    (require-mode! current (get-in entry [:mailbox/delivery :mode]))
    (when-not (= (select-keys current-scope [:org-id :actor-id])
                 (select-keys (:scope mailbox) [:org-id :actor-id]))
      (law/refuse! 403 "mailbox_context_changed" "Mailbox authority changed before delivery"))
    (when-not (and (= "pending" (:mailbox/status actual))
                   (= (get-in actual [:mailbox/delivery :claim-id])
                      (get-in entry [:mailbox/delivery :claim-id])))
      (law/refuse! 409 "mailbox_claim_stale" "This command no longer owns the current attempt"))
    current))
(defn ^:async deliver-claimed!
  "A claim is durable before the effect. An unconfirmed effect is never called delivered."
  [runtime config context mailbox entry content]
  (let [outcome (try
                 (let [current (await (delivery-context! runtime context mailbox entry))]
                   {:result (await (delivery/deliver! runtime config current
                                                      {:mode (get-in entry [:mailbox/delivery :mode])
                                                       :target (:mailbox/target entry) :content content
                                                       :metadata (:mailbox/metadata entry) :mailbox-id (:mailbox/id entry)}))})
                     (catch :default cause {:cause cause}))]
    (if-let [cause (:cause outcome)]
      (if (= "mailbox_claim_stale" (:code (ex-data cause)))
        (throw cause)
        (await (record-failure! mailbox entry cause)))
      (let [persisted (try (await (mailbox/mark-delivered! mailbox entry
                                                        (merge (:mailbox/content-ref entry) {:delivery-result (:result outcome)})))
                           (catch :default cause (unconfirmed! entry cause)))]
        {:ok true :entry persisted :target (:mailbox/target entry) :result (:result outcome)}))))
(defn ^:async send!
  "Admit and deliver a message through the same command for HTTP and agent tools."
  [runtime config context request]
  (law/checked! :mailbox/send law/SendRequest request)
  (let [current (await (current-context! runtime context)) _ (require-send! current)
        mailbox (mailbox/context runtime current)
        mode (case (:mode request) (nil "message") "follow-up" (:mode request))
        _ (require-mode! current mode) target (await (resolved-target! mailbox request))
        _ (when (and (= mode "inbox-only") (nil? (:actor-id target)))
            (law/refuse! 400 "mailbox_actor_target_required" "Inbox delivery requires an explicit actor target"))
        entry (await (mailbox/create-entry! mailbox (raw-entry mailbox request target mode)))]
    (when (= "failed" (:mailbox/status entry))
      (law/refuse! 409 "mailbox_retry_required" "The previous attempt failed; use an explicit retry command"))
    (if (contains? #{"delivered" "acknowledged"} (:mailbox/status entry))
      {:ok true :existing true :entry entry :target (:mailbox/target entry)}
      (let [claim (first (:entries (await (mailbox/claim-entry! mailbox (:mailbox/id entry) (host/new-id!)))))]
        (when-not claim (law/refuse! 409 "mailbox_delivery_pending" "Another delivery attempt already owns this message"))
        (await (deliver-claimed! runtime config current mailbox claim (:content request)))))))
(defn ^:async retry!
  "Claim eligible retries and use canonical full content, preserving each durable outcome."
  [runtime config context options]
  (let [current (await (current-context! runtime context))
        _ (authz/ensure-permission! current "org.events.control") _ (require-send! current)
        mailbox (mailbox/context runtime current)
        ;; Replaying a provider claim receipt does not confer a second right to perform its effect.
        claimed (await (mailbox/retry-eligible! mailbox (assoc options :operation-id (host/new-id!))))
        results (atom [])]
    (doseq [entry (:entries claimed)]
      (let [result (try (let [message (await (mailbox/read-message! mailbox (:mailbox/id entry)))]
                          (await (deliver-claimed! runtime config current mailbox entry (:mailbox/content message))))
                        (catch :default cause {:ok false :mailbox-id (:mailbox/id entry)
                                               :error-code (or (:code (ex-data cause)) "mailbox_retry_failed")}))]
        (swap! results conj result)))
    (assoc claimed :deliveries @results)))
