(ns knoxx.frontend.pages.mail.api
  "Mailbox REST calls. CLJS port of listActorMailbox and
   acknowledgeActorMailboxEntry from src/lib/api/runtime.ts, on the
   shared knoxx request helper."
  (:require [knoxx.frontend.lib.api :as api]
            [knoxx.frontend.pages.mail.logic :as logic]))

(defn ^:async list-mailbox
  "GET the actor mailbox; resolves to a normalized list response."
  [box status]
  (let [params (js/URLSearchParams.)]
    (.set params "box" box)
    (.set params "limit" "100")
    (when (and status (not= status "all"))
      (.set params "status" status))
    (logic/normalize-list-response
     (await (api/request (str "/api/actors/mailbox?" (.toString params)))) box)))

(defn acknowledge-entry
  "POST an acknowledgement for one mailbox entry."
  [mailbox-id]
  (api/request (str "/api/actors/mailbox/" (js/encodeURIComponent mailbox-id) "/ack")
               {:method "POST"}))

(defn ^:async read-entry
  "Read canonical full content through the same owned mailbox boundary as agents."
  [mailbox-id]
  (let [response (await (api/request (str "/api/actors/mailbox/" (js/encodeURIComponent mailbox-id))))]
    (logic/assert-message! mailbox-id response)))

(defn ^:async send-message
  "Send a finite command without accepting caller-supplied authority fields."
  [payload]
  (logic/assert-send!
   (await (api/request "/api/actors/messages"
                       {:method "POST" :body (select-keys payload [:operation_id :target :content :mode])}))))

(defn subscribe!
  "Invalidate on scoped mailbox events and reconnect, closing the stream on unmount."
  [changed! status!]
  (if (exists? js/EventSource)
    (let [stream (js/EventSource. "/api/actors/mailbox/changes" #js {:withCredentials true})
          change! (fn [_] (changed!))
          open! (fn [_] (status! "Live") (changed!))
          error! (fn [_] (status! "Reconnecting; refresh is available"))]
      (.addEventListener stream "mailbox-changed" change!)
      (.addEventListener stream "open" open!)
      (.addEventListener stream "error" error!)
      (fn [] (.removeEventListener stream "mailbox-changed" change!)
        (.removeEventListener stream "open" open!) (.removeEventListener stream "error" error!) (.close stream)))
    (do (status! "Live updates unavailable; use Refresh") (fn [] nil))))
