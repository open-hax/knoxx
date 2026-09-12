(ns knoxx.backend.infra.actor-mailbox
  "Verified actor mailbox commands over the selected named persistence provider."
  (:require [knoxx.backend.domain.actor.mailbox :as data]
            [knoxx.backend.extern.mailbox-store :as host]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.stores.mailbox-store :as registry]
            [knoxx.backend.law.mailbox-store :as law]
            [knoxx.backend.shape.mailbox-store :as store]))
(def normalize-status data/normalize-status)
(def normalize-delivery-mode data/normalize-delivery-mode)
(def source-from-context data/source-from-context)
(def mailbox-event-id data/mailbox-event-id)
(def retry-request-event data/retry-request-event)
(defn context
  "Bind only current, verified organization and actor authority to one command chain."
  [_runtime auth-context]
  (when-not (and (authz/ctx-org-id auth-context) (authz/ctx-user-id auth-context) (authz/ctx-membership-id auth-context))
    (law/refuse! 401 "mailbox_identity_required" "An authenticated membership is required"))
  (let [scope {:org-id (authz/ctx-org-id auth-context) :actor-id (some-> (authz/ctx-actor-binding auth-context) str)
               :admin? (authz/ctx-permitted? auth-context "org.events.control")}]
    (law/checked! :mailbox/scope law/Scope scope) {:scope scope :store (registry/selected!)}))
(defn- provider! [{:keys [scope store]}]
  (law/checked! :mailbox/scope law/Scope scope)
  (when-not (satisfies? store/IMailboxStore store)
    (law/refuse! 503 "mailbox_provider_unavailable" "Mailbox persistence has not been configured")) store)
(defn register-live-session!
  "Register an actor route only within the current organization and actor authority."
  [{:keys [scope] :as context} route] (store/register-route! (provider! context) scope route))
(defn unregister-live-session! [{:keys [scope] :as context} conversation-id]
  (store/unregister-route! (provider! context) scope conversation-id))
(defn resolve-actor-session! [{:keys [scope] :as context} actor-id]
  (store/resolve-route (provider! context) scope actor-id))
(defn create-entry!
  "Durably admit the reference before attempting delivery. Provider owns all status fields."
  [{:keys [scope] :as context} raw-entry]
  (let [provider (provider! context) raw-entry (assoc raw-entry :id (or (:id raw-entry) (host/new-id!)))
        raw-entry (if (:admin? scope) raw-entry (assoc-in raw-entry [:source :actor-id] (:actor-id scope)))
        entry (-> (data/mailbox-entry raw-entry) (dissoc :mailbox/status) (update :mailbox/delivery select-keys [:mode]))
        entry (cond-> entry (contains? raw-entry :content) (assoc :mailbox/content (:content raw-entry)))]
    (store/create-entry! provider scope entry)))
(defn claim-entry! "Lease one sender-owned message before its external delivery effect."
  [{:keys [scope] :as context} entry-id operation-id]
  (store/claim-deliveries! (provider! context) scope (cond-> {:mailbox-id entry-id} operation-id (assoc :operation-id operation-id))))
(defn mark-delivery! [{:keys [scope] :as context} id status options]
  (store/mark-delivery! (provider! context) scope id status options))
(defn mark-delivered! [context entry content-ref]
  (mark-delivery! context (:mailbox/id entry) "delivered" {:claim-id (get-in entry [:mailbox/delivery :claim-id]) :content-ref content-ref}))
(defn mark-failed! [context entry error]
  (mark-delivery! context (:mailbox/id entry) "failed" {:claim-id (get-in entry [:mailbox/delivery :claim-id]) :error (str error)}))
(defn list-entries! [{:keys [scope] :as context} filters] (store/list-entries (provider! context) scope filters))
(defn read-message! [{:keys [scope] :as context} id] (store/read-message (provider! context) scope id))
(defn acknowledge-entry! [{:keys [scope] :as context} id] (store/acknowledge-entry! (provider! context) scope id))
(defn retry-eligible! [{:keys [scope] :as context} options] (store/claim-deliveries! (provider! context) scope options))
