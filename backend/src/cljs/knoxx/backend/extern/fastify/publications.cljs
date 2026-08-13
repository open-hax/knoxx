(ns knoxx.backend.extern.fastify.publications
  "Fastify boundary for the publication projection routes.

  Native request and reply handles are born and die here. The facade this
  adapter calls receives decoded CLJS data and returns CLJS data; it never sees
  a Fastify object. Handlers are awaited rather than chained, so a currently
  synchronous facade stays compatible if it later becomes effectful without
  introducing a `.then`.

  Both routes authorize. The projection reads document titles, garden
  membership, and publication paths off the filesystem, so an unauthenticated
  caller must not be able to enumerate it."
  (:require [knoxx.backend.extern.fastify :as fastify]
            [knoxx.backend.infra.routes.publications :as publications]
            [knoxx.backend.law.publication :as law]
            [knoxx.backend.shape.resource-identity :as resource-identity]))

(def read-permission "org.publications.read")

(def DecodedRequest
  "Contract for the data this adapter hands inward. Named and asserted so a
   changed Fastify parameter shape fails at the boundary rather than flowing on
   into document lookup."
  [:map {:closed true}
   [:params [:map-of :keyword [:maybe :string]]]
   [:method :string]])

(defn decode-request
  "Project the native request onto the CLJS data the facade actually reads, and
   validate it. Everything downstream reads route identity from this map, never
   from the raw handle."
  [request]
  (law/assert-valid!
   :publication/request
   DecodedRequest
   {:params (fastify/request-params request)
    :method (fastify/request-method request)}))

(defn- error-status
  "A projection failure caused by resource data is a 409 — the desired state is
   genuinely contradictory and a retry will not help. An unknown document is a
   404. Anything else is a 500."
  [err]
  (let [data (ex-data err)]
    (cond
      (contains? data :document/id) 404
      (or (contains? data :conflicts)
          (contains? data :conflicting-payloads)
          (contains? data :blockers)) 409
      :else 500)))

(defn- error-body
  [err]
  (cond-> {:error (ex-message err)}
    (some? (ex-data err)) (assoc :detail (ex-data err))))

(defn encode-body
  "Encode keyword values so identity survives JSON.

   `send-json!` serializes with `clj->js`, which renders a keyword using `name`
   — so `:knoxx.docs/translation-pipeline` would reach the CMS as
   `\"translation-pipeline\"`, collapsing distinct namespaces onto one wire id
   and defeating the canonical identity the projection just established. Every
   keyword value is encoded as `namespace/name`, with no EDN leading colon.

   Map keys are left to `clj->js`: unqualified JSON keys are this codebase's
   wire convention, with qualified domain keys rebuilt by explicit adapter
   mapping on the client side."
  [body]
  (resource-identity/encode-wire-values body))

(defn ^:async send-projection!
  "Await an operation and send its CLJS result as JSON, translating a projection
   failure into a status the CMS can act on."
  [reply operation]
  (try
    (fastify/send-json! reply 200 (encode-body (await (operation))))
    (catch :default err
      (fastify/send-json! reply (error-status err) (encode-body (error-body err))))))

(defn- ^:async guarded!
  "Authorize, then run. `ensure-permission!` throws, and `send-projection!`
   turns that into an error response — so an unauthorized caller never reaches
   the filesystem-backed projection."
  [handlers ctx operation]
  (when ctx ((:ensure-permission! handlers) ctx read-permission))
  (await (operation)))

(defn- authorized-route
  "One shape for both routes: enter the request context, authorize, run the
   projection, send it. `operation` receives the decoded request."
  [runtime handlers operation]
  (^:async fn [request reply]
    (let [decoded (decode-request request)]
      (await
       ((:with-request-context! handlers) runtime request reply
        (^:async fn [ctx]
          (await
           (send-projection!
            reply
            #(guarded! handlers ctx (fn [] (operation decoded)))))))))))

(defn register-publication-routes!
  [app runtime config handlers]
  (fastify/route!
   app
   {:method "GET"
    :url "/api/publications/documents"
    :handler (authorized-route runtime handlers
                               (fn [_decoded]
                                 (publications/list-publication-documents! config)))})
  (fastify/route!
   app
   {:method "GET"
    :url "/api/publications/documents/:documentId"
    :handler (authorized-route runtime handlers
                               (fn [decoded]
                                 (publications/publication-document-view!
                                  config (get-in decoded [:params :documentId]))))})
  nil)
