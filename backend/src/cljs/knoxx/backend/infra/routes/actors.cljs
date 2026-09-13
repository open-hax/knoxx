(ns knoxx.backend.infra.routes.actors
  "HTTP adapters for the same verified, durable mailbox commands used by agent tools."
  (:require [knoxx.backend.extern.actor-mailbox :as wire]
            [knoxx.backend.extern.mailbox-changes :as changes]
            [knoxx.backend.infra.actor-mailbox :as mailbox]
            [knoxx.backend.infra.actor-mailbox-commands :as commands]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.law.mailbox-store :as law]
            [knoxx.backend.macros :refer-macros [defroute]]))
(defn- actor-id! [ctx]
  (or (authz/ctx-actor-binding ctx) (law/refuse! 403 "mailbox_actor_required" "An explicit actor binding is required")))
(defroute actor-mailbox-list-route!
  [] "GET" "/api/admin/config/actors/mailbox" [session-guard]
  (try (ensure-permission! ctx "org.events.control")
       (let [result (await (mailbox/list-entries! (mailbox/context runtime ctx) (wire/request-filters request)))]
         (json-response! reply 200 (assoc (wire/result-wire result) :ok true
                                        :capabilities (assoc (commands/interface-capabilities ctx) :acknowledge true))))
       (catch :default err (error-response! reply err))))
(defroute actor-mailbox-ack-route!
  [] "POST" "/api/admin/config/actors/mailbox/:mailboxId/ack" [session-guard]
  (try (ensure-permission! ctx "org.events.control")
       (let [entry (await (mailbox/acknowledge-entry! (mailbox/context runtime ctx) (wire/request-id request)))]
         (json-response! reply 200 {:ok true :entry (wire/entry-wire entry)}))
       (catch :default err (error-response! reply err))))
(defroute actor-mailbox-retry-route!
  [] "POST" "/api/admin/config/actors/mailbox/retry" [session-guard]
  (try (ensure-permission! ctx "org.events.control")
       (let [{:keys [dispatch? options]} (wire/request-retry request)
             result (if dispatch? (await (commands/retry! runtime config ctx options))
                      (await (mailbox/retry-eligible! (mailbox/context runtime ctx) options)))]
         (json-response! reply 202 (assoc (wire/result-wire result) :ok true :retry_event_count (count (:deliveries result)))))
       (catch :default err (error-response! reply err))))
(defroute actor-mailbox-self-list-route!
  [] "GET" "/api/actors/mailbox" [session-guard]
  (try (ensure-permission! ctx "agent.chat.use")
       (let [actor-id (actor-id! ctx) box (wire/request-box request)
             filters (assoc (select-keys (wire/request-filters request) [:status :limit])
                            (if (= box "outbox") :source-actor-id :target-actor-id) actor-id)
             result (await (mailbox/list-entries! (mailbox/context runtime ctx) filters))]
         (json-response! reply 200 (assoc (wire/result-wire result) :ok true :box box :actorId actor-id
                                        :capabilities (commands/interface-capabilities ctx))))
       (catch :default err (error-response! reply err))))
(defroute actor-mailbox-self-read-route!
  [] "GET" "/api/actors/mailbox/:mailboxId" [session-guard]
  (try (ensure-permission! ctx "agent.chat.use")
       (let [entry (await (mailbox/read-message! (mailbox/context runtime ctx) (wire/request-id request)))]
         (json-response! reply 200 {:ok true :entry (wire/entry-wire entry)}))
       (catch :default err (error-response! reply err))))
(defroute actor-mailbox-self-ack-route!
  [] "POST" "/api/actors/mailbox/:mailboxId/ack" [session-guard]
  (try (ensure-permission! ctx "agent.chat.use") (actor-id! ctx)
       (let [context (update (mailbox/context runtime ctx) :scope assoc :admin? false)
             entry (await (mailbox/acknowledge-entry! context (wire/request-id request)))]
         (json-response! reply 200 {:ok true :entry (wire/entry-wire entry)}))
       (catch :default err (error-response! reply err))))
(defroute actor-mailbox-send-route!
  [] "POST" "/api/actors/messages" [session-guard]
  (try (let [result (await (commands/send! runtime config ctx (wire/request-send request)))]
         (json-response! reply 200 (wire/command-wire result)))
       (catch :default err (error-response! reply err))))
(defroute actor-mailbox-changes-route!
  [] "GET" "/api/actors/mailbox/events/stream" [session-guard]
  (try (changes/open-stream! runtime ctx reply)
       (catch :default err (error-response! reply err))))
(defn register-actor-routes!
  "Register the mailbox reader, sender, acknowledgement and operator retry adapters."
  [app runtime config deps]
  (doseq [register! [actor-mailbox-list-route! actor-mailbox-ack-route! actor-mailbox-retry-route!
                    actor-mailbox-self-list-route! actor-mailbox-self-read-route! actor-mailbox-self-ack-route!
                    actor-mailbox-send-route! actor-mailbox-changes-route!]]
    (register! app runtime config deps)) nil)
