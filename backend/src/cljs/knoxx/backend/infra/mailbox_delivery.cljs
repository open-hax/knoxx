(ns knoxx.backend.infra.mailbox-delivery
  "Deliver with current context through existing agent control and event ports."
  (:require [knoxx.backend.domain.event.dispatch :as events]
            [knoxx.backend.infra.agent.service :as agent]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.stores.mongo-session-store :as threads]
            [knoxx.backend.law.mailbox-store :as law]
            [knoxx.backend.shape.mailbox-delivery :as port]))
(defn- ^:async target-thread! [context target]
  (when-not (or (:session-id target) (:conversation-id target))
    (law/refuse! 404 "mailbox_target_not_found" "The target actor has no current conversation"))
  (let [session-id (or (:session-id target) (await (threads/get-conversation-active-session (:conversation-id target))))
        thread (when session-id (await (threads/get-session session-id)))]
    (when-not (and thread (= (authz/ctx-org-id context) (:org_id thread))
                   (or (nil? (:conversation-id target)) (= (:conversation-id target) (:conversation_id thread))))
      (law/refuse! 404 "mailbox_target_not_found" "No current conversation exists in this organization"))
    (when-not (or (authz/ctx-permitted? context "org.events.control") (authz/principal-match? context thread))
      (law/refuse! 403 "mailbox_target_denied" "The target conversation is outside current control authority")) thread))
(defn- ^:async control! [runtime config context {:keys [mode target content metadata mailbox-id]}]
  (authz/ensure-permission! context (if (= mode "steer") "agent.controls.steer" "agent.controls.follow_up"))
  (let [thread (await (target-thread! context target))]
    (await (agent/control-turn! runtime config {:conversation-id (:conversation_id thread) :session-id (:session_id thread)
                                               :run-id (:run_id thread) :message content :kind (if (= mode "steer") "steer" "follow_up")
                                               :metadata (assoc metadata :mailboxId mailbox-id)}))))
(defn- event! [config context {:keys [target content metadata mailbox-id]}]
  (authz/ensure-permission! context "org.events.control")
  (events/dispatch-external! config {:id (str "actor-mailbox-" mailbox-id) :sourceKind "actor" :eventKind "actors.message"
                                   :orgId (authz/ctx-org-id context)
                                   :payload {:orgId (authz/ctx-org-id context) :actorId (:actor-id target)
                                             :conversationId (:conversation-id target) :sessionId (:session-id target)
                                             :content content :mailboxId mailbox-id :metadata metadata}}))
(defrecord LocalMailboxDelivery []
  port/IMailboxDelivery
  (deliver! [_ runtime config context request]
    (law/checked! :mailbox/delivery law/DeliveryRequest request)
    (authz/ensure-permission! context "agent.chat.use")
    (case (:mode request)
      "inbox-only" {:ok true :delivery "inbox-only" :mailbox-id (:mailbox-id request)}
      "event" (event! config context request)
      ("follow-up" "steer") (control! runtime config context request)
      (law/refuse! 400 "mailbox_delivery_mode_invalid" "Unsupported mailbox delivery mode"))))
(defn provider "Use the existing authorized in-process service implementations." [] (->LocalMailboxDelivery))
