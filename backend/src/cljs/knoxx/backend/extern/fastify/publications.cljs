(ns knoxx.backend.extern.fastify.publications
  "Fastify boundary for the publication projection routes.

  Native request and reply handles are born and die here. The handler this
  adapter calls receives decoded CLJS data and returns CLJS data; it never
  sees a Fastify object. Handlers are awaited rather than chained, so a
  currently-synchronous handler stays compatible if it later becomes
  effectful without introducing a `.then`."
  (:require [knoxx.backend.extern.fastify :as fastify]
            [knoxx.backend.infra.routes.publications :as publications]))

(defn decode-request
  "Project the native request onto the CLJS data the handlers actually read.
   Everything downstream reads route identity from this map, never from the
   raw handle."
  [request]
  {:params (fastify/request-params request)
   :method (fastify/request-method request)})

(defn- error-status
  "A projection failure caused by resource data is a 409 — the desired state
   is genuinely contradictory and a retry will not help. An unknown document
   is a 404. Anything else is a 500."
  [err]
  (let [data (ex-data err)]
    (cond
      (contains? data :document/id) 404
      (or (contains? data :conflicts)
          (contains? data :conflicting-payloads)) 409
      :else 500)))

(defn- error-body
  [err]
  (cond-> {:error (ex-message err)}
    (some? (ex-data err)) (assoc :detail (ex-data err))))

(defn ^:async send-projection!
  "Await a handler and send its CLJS result as JSON, translating a projection
   failure into a status the CMS can act on."
  [reply handler]
  (try
    (fastify/send-json! reply 200 (await (handler)))
    (catch :default err
      (fastify/send-json! reply (error-status err) (error-body err)))))

(defn register-publication-routes!
  [app config]
  (fastify/route!
   app
   {:method "GET"
    :url "/api/publications/documents"
    :handler (^:async fn [_request reply]
               (await
                (send-projection!
                 reply
                 #(publications/list-publication-documents! config))))})
  (fastify/route!
   app
   {:method "GET"
    :url "/api/publications/documents/:documentId"
    :handler (^:async fn [request reply]
               (let [request (decode-request request)
                     document-id (get-in request [:params :documentId])]
                 (await
                  (send-projection!
                   reply
                   #(publications/publication-document-view! config document-id)))))})
  nil)
