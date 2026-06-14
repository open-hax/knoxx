(ns knoxx.frontend.pages.mail.api
  "Mailbox REST calls. CLJS port of listActorMailbox and
   acknowledgeActorMailboxEntry from src/lib/api/runtime.ts, on the
   shared knoxx request helper."
  (:require [knoxx.frontend.lib.api :as api]
            [knoxx.frontend.pages.mail.logic :as logic]))

(defn list-mailbox
  "GET the actor mailbox; resolves to a normalized list response."
  [box status]
  (let [params (js/URLSearchParams.)]
    (.set params "box" box)
    (.set params "limit" "100")
    (when (and status (not= status "all"))
      (.set params "status" status))
    (-> (api/request (str "/api/actors/mailbox?" (.toString params)))
        (.then #(logic/normalize-list-response % box)))))

(defn acknowledge-entry
  "POST an acknowledgement for one mailbox entry."
  [mailbox-id]
  (api/request (str "/api/actors/mailbox/" (js/encodeURIComponent mailbox-id) "/ack")
               {:method "POST"}))
